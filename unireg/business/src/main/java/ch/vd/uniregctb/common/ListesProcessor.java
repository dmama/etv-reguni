package ch.vd.uniregctb.common;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;

/**
 * Classe de base des processeurs de traitement des listes (nominatives, pour les acomptes...)
 */
public abstract class ListesProcessor<R extends ListesResults<R>, T extends ListesThread<R>> {

	/**
	 * Interface à implémenter pour customiser la génération des listes
	 *
	 * @param <R> classe des Results
	 * @param <T> classe des Threads
	 */
	protected static interface Customizer<R extends ListesResults<R>, T extends ListesThread<R>> {

		/**
		 * Renvoie un itérateur des identifiants de contribuables à lister
		 * @param session session hibernate si nécessaire
		 */
		Iterator<Long> getIdIterator(Session session);

		/**
		 * Crée une nouvelle instance de résultats
		 */
		R createResults (RegDate dateTraitement);

		/**
		 * Crée une nouvelle instance de thread spécialisé
		 */
		T createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, StatusManager status, AtomicInteger compteur, HibernateTemplate hibernateTemplate);
	}

	/**
	 * Pour compléter la phrase "Décompte du nombre de xxxx :"
	 * @return
	 */
	protected String getDenominationContribuablesComptes() {
		return "contribuables";
	}

	/**
	 * Appelable par les classes dérivées pour faire le boulot de lancement des threads et de
	 * partage des traitements entre ceux-ci...
	 */
	protected R doRun(final RegDate dateTraitement, final int nbThreads, final StatusManager status,
						final HibernateTemplate hibernateTemplate, final Customizer<R, T> customizer) {

		Assert.notNull(status);

		final R results = customizer.createResults(dateTraitement);

		// les identifiants des contribuables à lister sont d'abord répartis en groupes de
		// 200 (voir constante "tailleLot" plus bas), et ces lots sont alors traités sur
		// 20 threads (voir constante "nbThreads" plus bas également)

		hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				final Iterator<Long> idIterator = customizer.getIdIterator(session);
				final int tailleLot = 200;
				final AtomicInteger compteur = new AtomicInteger(0);

				final LinkedBlockingQueue<List<Long>> queue = new LinkedBlockingQueue<List<Long>>();
				final List<T> threads = new ArrayList<T>(nbThreads);
				for (int i = 0; i < nbThreads; i++) {
					final T thread = customizer.createThread(queue, dateTraitement, status, compteur, hibernateTemplate);
					threads.add(thread);
					thread.setName(String.format("%s-%d", thread.getClass().getSimpleName(), i));
					thread.start();
				}

				// on bourre dans la queue des lots de "tailleLot" identifiants
				int size = 0;
				while (idIterator.hasNext() && !status.interrupted()) {
					final List<Long> list = new ArrayList<Long>(tailleLot);
					while (idIterator.hasNext() && !status.interrupted() && list.size() < tailleLot) {
						list.add(idIterator.next());
					}
					queue.add(list);
					size += list.size();
					status.setMessage(String.format("Décompte du nombre de %s : %d", getDenominationContribuablesComptes(), size));
				}

				// on dit à tout le monde que la queue est remplie (donc la prochaine
				// fois qu'ils voient qu'il n'y a plus rien dans la queue, c'est que tout
				// est fini)
				for (T t : threads) {
					t.onQueueFilled();
				}

				status.setMessage(String.format("Traitement des %d %s", size, getDenominationContribuablesComptes()));

				// attente "active" de la fin des threads
				while (threads.size() > 0) {

					// tous les threads terminés sont enlevés de la liste
					final Iterator<T> iterator = threads.iterator();
					while (iterator.hasNext()) {
						final T thread = iterator.next();
						if (!thread.isAlive()) {
							iterator.remove();
							results.addAll(thread.getResults());
						}
					}

					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {

						// le thread a été interrompu : collecte des résultats courants
						for (T thread : threads) {
							try {
								thread.join();
							}
							catch (InterruptedException ie) {
								// pas bien grave... il est déjà mort...
							}
							finally {
								results.addAll(thread.getResults());
							}
						}

						break;
					}

					final int valeurCompteur = compteur.intValue();
					if (status.interrupted()) {
						final String message = String.format("Interruption en cours... (%d de %d)", valeurCompteur, size);
						status.setMessage(message);
					}
					else {
						final String message = String.format("Analyse des %s : %d de %d", getDenominationContribuablesComptes(), valeurCompteur, size);
						status.setMessage(message, size == 0 ? 0 : valeurCompteur * 100 / size);
					}
				}

				return null;
			}
		});

		results.sort();
		results.setInterrompu(status.interrupted());
		results.end();
		return results;
	}

}

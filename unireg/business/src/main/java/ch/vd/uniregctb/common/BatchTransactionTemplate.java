package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Classe utilitaire qui permet de découper le processing d'une collection d'éléments en lots (batchs). Chaque lot est exécuté dans une
 * transaction propre.
 * <p>
 * En cas d'exception levée durant le traitement d'un lot, le comportement est soit une reprise automatique soit pas de reprise (voir
 * {@link Behavior} pour plus de détails).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class BatchTransactionTemplate<E> {



	private final Logger LOGGER = Logger.getLogger(BatchTransactionTemplate.class);

	public enum Behavior {
		/**
		 * En cas d'exception durant le processing d'un batch, considère le batch comme perdu (en erreur, transaction rollée-back) dans son
		 * ensemble et continue avec les batchs restants.
		 */
		SANS_REPRISE,
		/**
		 * En cas d'exception durant le processing d'un batch, reprend le processing du batch élément par élément, chacun dans sa propre
		 * transaction. A la fin, seuls les éléments posant problèmes n'auront pas été processés.
		 */
		REPRISE_AUTOMATIQUE
	}

	private final PlatformTransactionManager transactionManager;
	private final BatchIterator<E> iterator;
	private final Behavior behavior;
	private final StatusManager statusManager;
	private final HibernateTemplate hibernateTemplate;

	private boolean readonly;

	/**
	 * @param iterator
	 *            un itérateur qui retourne les éléments à processer
	 * @param batchSize
	 *            la taille maximale des batches
	 * @param behavior
	 *            le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager
	 *            le transaction manager Spring
	 * @param hibernateTemplate
	 * 			  le bean hibernateTemplate
	 */
	public BatchTransactionTemplate(Iterator<E> iterator, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager,
			StatusManager statusManager, HibernateTemplate hibernateTemplate) {
		this.iterator = new BatchIterator<E>(iterator, batchSize);
		this.behavior = behavior;
		this.transactionManager = transactionManager;
		this.statusManager = statusManager;
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * @param list
	 *            la liste des éléments à processer
	 * @param batchSize
	 *            la taille maximale des batches
	 * @param behavior
	 *            le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager
	 *            le transaction manager Spring
	 * @param hibernateTemplate
	 * 			  le bean hibernateTemplate
	 */
	public BatchTransactionTemplate(List<E> list, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager,
			StatusManager statusManager, HibernateTemplate hibernateTemplate) {
		this.iterator = new BatchIterator<E>(list, batchSize);
		this.behavior = behavior;
		this.transactionManager = transactionManager;
		this.statusManager = statusManager;
		this.hibernateTemplate = hibernateTemplate;
	}

	public static abstract class BatchCallback<E> {

		/**
		 * Le pourcentage de progression du traitement complet des éléments.
		 */
		protected int percent;

		/**
		 * Cette méthode est appelée pour chaque lot avant l'ouverture de la transaction.
		 */
		public void beforeTransaction() {
		}

		/**
		 * Cette méthode est appelée pour chaque lot. La transaction est ouverte et sera committée à moins qu'une exception soit levée.
		 *
		 * @param batch
		 *            le lot à processer
		 * @throws Exception
		 *             si le batch doit être rollé-back
		 * @return true si le prochain lot doit être traiter, false si le batch doit être interrompu
		 */
		public abstract boolean doInTransaction(List<E> batch) throws Exception;

		/**
		 * Cette méthode est appelée après que la transaction ait été committée. Elle n'est pas appelée si la transaction a été rollée-back.
		 */
		public void afterTransactionCommit() {
		}

		/**
		 * Cette méthode est appelée après que la transaction ait été rollée-back. Elle n'est pas appelée si la transaction a été committée.
		 *
		 * @param e
		 *            l'exception à la source du rollback de la transaction
		 * @param willRetry
		 *            <code>true</code> si le batch qui vient d'être rollé-back va être repris élément par élément; <code>false</code>
		 *            autrement.
		 */
		public void afterTransactionRollback(Exception e, boolean willRetry) {
		}
	}

	/**
	 * Exécute le traitement par batchs dans autant de transactions séparées.
	 */
	public void execute(final BatchCallback<E> action) throws TransactionException {

		final boolean reprise = (behavior == Behavior.REPRISE_AUTOMATIQUE);
		boolean processNextBatch = true;

		while (iterator.hasNext() && processNextBatch) {

			final List<E> batch = asList(iterator.next());
			action.percent = iterator.getPercent();

			if (statusManager != null && statusManager.interrupted()) {
				LOGGER.debug("le batch a été interrompu.");
				break;
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("première exécution du batch");
			}

			// exécute le batch en un bloc
			final ExecuteInTransactionResult ret = executeInTransaction(action, batch, reprise);
			processNextBatch = ret.processNextBatch;

			if (!ret.committed && reprise) {

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("reprise du batch");
				}

				// re-exécute le batch élément par élément
				for (E e : batch) {
					if (statusManager != null && statusManager.interrupted()) {
						break;
					}

					final List<E> subbatch = new ArrayList<E>(1);
					subbatch.add(e);

					final ExecuteInTransactionResult retReprise = executeInTransaction(action, subbatch, false);
					if (!retReprise.processNextBatch) {
						break;
					}
				}
			}
		}
	}


	/**
	 * Une classe pour stocker le retour de la méthode {@link BatchTransactionTemplate#executeInTransaction(BatchCallback, List, boolean)}
	 *
	 * @author xsifnr
	 *
	 */
	private class ExecuteInTransactionResult {

		/**
		 * true si la transaction a été committée
		 */
		private boolean committed;


		/**
		 * true si le prochain batch doit être traité
		 */
		private boolean processNextBatch;
	}

	/**
	 * Appelle la méthode doInTransaction sur l'action
	 * @return la valeur booléenne retournée par la méthode doInTransaction
	 */
	private boolean doInTransaction(BatchCallback<E> action, List<E> batch) {
		try {
			return action.doInTransaction(batch);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Exécute un batch dans une transaction.
	 *
	 * @return <code>true</code> si le processus s'est bien déroulé et que la transaction est committée; <code>false</code> si la
	 *         transaction a été rollée-back.
	 */
	private ExecuteInTransactionResult executeInTransaction(final BatchCallback<E> action, final List<E> batch, boolean willRetry) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Execution en transaction du batch = " + batch);
		}

		action.beforeTransaction();

		final ExecuteInTransactionResult r = new ExecuteInTransactionResult();
		r.committed = true;
		r.processNextBatch = true;

		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(readonly);
			r.processNextBatch = (Boolean) template.execute(new TransactionCallback() {
				public Boolean doInTransaction(TransactionStatus status) {
					return executeWithNewSession(batch, action);
				}
			});
		}
		catch (Exception e) {
			r.committed = false;
			action.afterTransactionRollback(e, willRetry);

			if (LOGGER.isEnabledFor(Level.WARN)) {
				LOGGER.warn("Rollback du batch", e);
			}
		}

		if (r.committed) {
			action.afterTransactionCommit();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Commit du batch");
			}
		}

		return r;
	}

	/**
	 * Exécute un batch dans une nouvelle session
	 */
	private Boolean executeWithNewSession(final List<E> batch, final BatchCallback<E> action) {
		return (Boolean) hibernateTemplate.executeWithNewSession(new HibernateCallback() {
			public Boolean doInHibernate(Session session) {
				return Boolean.valueOf(BatchTransactionTemplate.this.doInTransaction(action, batch));
			}
		});
	}

	/**
	 * Extrait les éléments de l'itérateur et stocke-les dans une liste.
	 */
	private static <T> List<T> asList(Iterator<T> iter) {
		final List<T> batch = new ArrayList<T>(100);
		while (iter.hasNext()) {
			batch.add(iter.next());
		}
		return batch;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
}

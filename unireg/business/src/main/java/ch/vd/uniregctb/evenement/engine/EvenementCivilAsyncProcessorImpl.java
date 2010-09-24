package ch.vd.uniregctb.evenement.engine;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCriteria;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Processeur asynchrone de traitement des événements civils
 */
public class EvenementCivilAsyncProcessorImpl implements EvenementCivilAsyncProcessor, InitializingBean, DisposableBean {

	public static final Logger LOGGER = Logger.getLogger(EvenementCivilAsyncProcessorImpl.class);

	/**
	 * La queue qui renvoie les éléments dans un ordre particulier (qui correspond ici
	 * à leur ordre naturel)
	 */
	private final PriorityBlockingQueue<EvtData> queue = new PriorityBlockingQueue<EvtData>();

	/**
	 * Nombre d'événements postés dans la queue depuis le démarrage de l'application
	 */
	private final AtomicInteger nombreEvenementsPostes = new AtomicInteger(0);

	/**
	 * Nombre d'événements reçus sur la queue et traités depuis le démarrage de l'application (ce nombre est donc
	 * toujours inférieur ou égal au nombre d'événements postés ({@link #nombreEvenementsPostes})
	 */
	private final AtomicInteger nombreEvenementsTraites = new AtomicInteger(0);

	/**
	 * Sera mis à <code>true</code> dans la phase d'extinction du service
	 */
	private boolean dying = false;

	/**
	 * Moteur de traitement des événements civils qui sortent de la queue
	 */
	private QueueListener queueListener;

	/**
	 * Délai, en secondes, de latence pour s'assurer que les événements dans la queue
	 * sont bien triés dans le bon ordre (la valeur 0 est invalide, elle doit être paramétrée depuis l'extérieur)
	 */
	private int delaiPriseEnCompte = 0;

	private boolean fetchAwaitingEventsOnStart = true;

	private EvenementCivilDAO evenementCivilDAO;

	private PlatformTransactionManager transactionManager;

	private HibernateTemplate hibernateTemplate;

	private EvenementCivilProcessor evenementCivilProcessor;

	/**
	 * Données placées dans la queue
	 */
	private static final class EvtData implements Comparable<EvtData> {
		public final long evtId;
		public final long timestamp;

		private EvtData(long evtId, long timestamp) {
			this.evtId = evtId;
			this.timestamp = timestamp;
		}

		public int compareTo(EvtData o) {
			// l'ordre naturel correspond à l'ordre des identifiants d'événement
			final long diff = evtId - o.evtId;
			return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
		}
	}

	/**
	 * Listener de la queue, traitement des événements civils
	 */
	private final class QueueListener extends Thread {

		private boolean stopping = false;

		private Lock traitementEnCours = new ReentrantLock();

		private QueueListener() {
			super("EvtCivil");
		}

		@Override
		public void run() {

			LOGGER.info("Démarrage du thread de traitement des événements civils reçus");

			try {
				while (!stopping) {
					final EvtData data = queue.poll(5, TimeUnit.SECONDS);
					if (data != null && !stopping) {

						traitementEnCours.lock();
						try {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("Nouvel événement à traiter : %d", data.evtId));
							}

							// deux possibilités :
							// 1. l'événement est là depuis assez longtemps pour que les événements qui
							//    sont arrivés en même temps que lui soient tous là aussi (donc ils sont dans le bon ordre, voir {@link PriorityQueue})
							// 2. l'événement est trop récent : on ne peut pas encore être sûr que tous les événements soient bien arrivés et donc on attends encore
							final long now = System.currentTimeMillis();
							final long seuilPriseEnCompteEvenement = data.timestamp + delaiPriseEnCompte * 1000L;
							if (now >= seuilPriseEnCompteEvenement) {

								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(String.format("Lancement du traitement de l'événement %d", data.evtId));
								}

								// on traite l'événement!
								try {
									final long debut = System.nanoTime();

									evenementCivilProcessor.traiteEvenementCivil(data.evtId, true);

									final long fin = System.nanoTime();

									if (LOGGER.isInfoEnabled()) {
										LOGGER.info(String.format("Evenement civil %d traité en %d ms", data.evtId, (fin - debut) / 1000000L));
									}
								}
								catch (Exception e) {
									// afin de ne pas faire sauter le thread en cas de problème lors du traitement de l'événement
									LOGGER.error(String.format("Exception reçue lors du traitement de l'événement civil %d", data.evtId), e);
								}
								finally {
									nombreEvenementsTraites.incrementAndGet();
								}
							}
							else {

								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(String.format("Encore trop tôt après la réception de l'événement %d (on attend encore %d ms)", data.evtId, seuilPriseEnCompteEvenement - now));
								}

								// on se revoit un peu plus tard!
								queue.add(data);

								if (!stopping) {
									// on attend un peu pour ne pas partir dans une boucle inutilement rapide
									Thread.sleep(seuilPriseEnCompteEvenement - now);
								}
							}
						}
						finally {
							traitementEnCours.unlock();
						}
					}
				}

				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("Arrêt du thread de traitement des événements civils reçus");
				}
			}
			catch (InterruptedException e) {
				LOGGER.error("Demande d'interruption du thread de traitement des événements civils reçus", e);
			}
		}

		public void requestStop() {
			stopping = true;
		}
	}

	public void setDelaiPriseEnCompte(int delaiPriseEnCompte) {
		if (delaiPriseEnCompte <= 0) {
			throw new IllegalArgumentException("Le délai doit être strictement positif");
		}
		if (delaiPriseEnCompte != this.delaiPriseEnCompte) {
			this.delaiPriseEnCompte = delaiPriseEnCompte;
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("Le délai de prise en compte des événements civils à l'arrivée est de %d seconde%s.", delaiPriseEnCompte, delaiPriseEnCompte > 1 ? "s" : ""));
			}
		}
	}

	public void setEvenementCivilDAO(EvenementCivilDAO evenementCivilDAO) {
		this.evenementCivilDAO = evenementCivilDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	public void setFetchAwaitingEventsOnStart(boolean fetchAwaitingEventsOnStart) {
		this.fetchAwaitingEventsOnStart = fetchAwaitingEventsOnStart;
	}

	public void afterPropertiesSet() throws Exception {

		// vérification de la plage de valeurs autorisées
		setDelaiPriseEnCompte(delaiPriseEnCompte);

		// deux choses : on récupère au démarrage tous les événements "a traiter" de la base de données
		// et on les programme pour un re-traitement, puis on démarre le service qui écoute sur la queue
		// et qui traite effectivement les événements
		if (fetchAwaitingEventsOnStart) {

			final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
			transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			transactionTemplate.setReadOnly(true);
			transactionTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					hibernateTemplate.executeWithNewSession(new HibernateCallback() {
						public Object doInHibernate(Session session) throws HibernateException, SQLException {

							LOGGER.info("Recherche des événements civils dans l'état 'A_TRAITER'");

							final EvenementCriteria criteres = new EvenementCriteria();
							criteres.setEtat(EtatEvenementCivil.A_TRAITER);
							final List<EvenementCivilData> evts = evenementCivilDAO.find(criteres, null);
							if (evts != null && evts.size() > 0) {

								LOGGER.info(String.format("Trouvé %d événements civils 'A_TRAITER'", evts.size()));

								for (EvenementCivilData evt : evts) {
									postEvenementCivil(evt.getId(), System.currentTimeMillis());
								}
							}
							else {
								LOGGER.info("Aucun événement civil 'A_TRAITER' trouvé");
							}
							return null;
						}
					});
					return null;
				}
			});
		}

		// on peut maintenant démarrer le listener de la queue
		queueListener = new QueueListener();
		queueListener.start();
	}

	public void destroy() throws Exception {

		// c'est la fin... on arrête tout!
		dying = true;

		// le listener aussi
		queueListener.requestStop();
		queueListener.join();
		queueListener = null;
	}

	public void postEvenementCivil(long evtId, long timestamp) {
		if (!dying) {
			queue.add(new EvtData(evtId, timestamp));
			nombreEvenementsPostes.incrementAndGet();
		}
	}

	public int getQueueSize() {
		return queue.size();
	}

	public int getDelaiPriseEnCompte() {
		return delaiPriseEnCompte;
	}

	public int getNombreEvenementsRecus() {
		return nombreEvenementsPostes.intValue();
	}

	public int getNombreEvenementsTraites() {
		return nombreEvenementsTraites.intValue();
	}

	/**
	 * Méthode qui attend tant qu'il y a encore des événements civils à traiter.<p/>
	 * Elle n'empêche en aucun cas que d'autres événements civils soient postés, l'attente peut donc s'étendre indéfiniment.<p/>
	 * A priori utilisée dans un contexte de tests pour synchroniser le traitement.
	 * @throws InterruptedException si l'attente a été interrompue par un autre thread
	 */
	public void sync() throws InterruptedException {

		LOGGER.trace("Attente que tous les événements civils soient traités");
		while (queue.size() > 0) {
			Thread.sleep(100);
		}

		// on attends encore un peu pour être certain que le listener a commencé son traitement
		Thread.sleep(10);

		// une fois que la queue est vide, on vérifie qu'il n'y a pas de traitement en cours...
		final Lock traitementEnCours = queueListener.traitementEnCours;
		traitementEnCours.lock();
		traitementEnCours.unlock();
	}
}

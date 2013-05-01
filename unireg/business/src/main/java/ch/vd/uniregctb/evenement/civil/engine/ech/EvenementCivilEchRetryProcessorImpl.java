package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchRethrower;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class EvenementCivilEchRetryProcessorImpl implements EvenementCivilEchRetryProcessor {
	
	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchRetryProcessorImpl.class);
	
	private EvenementCivilNotificationQueue notificationQueue;
	private EvenementCivilEchDAO evtCivilDAO;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchProcessor processor;
	private EvenementCivilEchRethrower rethrower;

	@SuppressWarnings("UnusedDeclaration")
	public void setNotificationQueue(EvenementCivilNotificationQueue notificationQueue) {
		this.notificationQueue = notificationQueue;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setEvtCivilDAO(EvenementCivilEchDAO evtCivilDAO) {
		this.evtCivilDAO = evtCivilDAO;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setProcessor(EvenementCivilEchProcessor processor) {
		this.processor = processor;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setRethrower(EvenementCivilEchRethrower rethrower) {
		this.rethrower = rethrower;
	}

	@Override
	public void retraiteEvenements(@Nullable StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}

		// dans un premier temps, on essaie de relancer les événements civils "à traiter"
		rethrower.fetchAndRethrowEvents();
		
		// ensuite, allons rechercher les numéros d'individus qui sont concernés (ce sont ces numéros qu'il faudra poster dans la queue)
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		final Set<Long> individus = template.execute(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {
				return evtCivilDAO.getIndividusConcernesParEvenementsPourRetry();
			}
		});
		
		if (individus != null && individus.size() > 0) {
			final Set<Long> remaining = new HashSet<>(individus);
			final MutableBoolean processorStopping = new MutableBoolean(false);
			final EvenementCivilEchProcessor.ListenerHandle handle = processor.registerListener(new EvenementCivilEchProcessor.Listener() {
				@Override
				public void onIndividuTraite(long noIndividu) {
					remaining.remove(noIndividu);
				}

				@Override
				public void onStop() {
					processorStopping.setValue(true);
				}
			});

			try {
				// on peut maintenant tout envoyer dans la queue
				notificationQueue.postAll(individus);
				
				// et on attend la fin
				final String msg = "Relance des événements civils e-CH en erreur";
				final int initialSize = individus.size();
				while (remaining.size() > 0 && !status.interrupted() && !processorStopping.booleanValue()) {
					final int progress = getProgress(initialSize, remaining.size());
					status.setMessage(msg, progress);
					
					try {
						Thread.sleep(500L);
					}
					catch (InterruptedException e) {
						LOGGER.error("Thread interrompu pendant le traitement...", e);
						break;
					}
				}

				// histoire que ce soit le message de fin qui reste après la fin
				status.setMessage(msg, getProgress(initialSize, remaining.size()));
			}
			finally {
				processor.unregisterListener(handle);
			}
		}
	}
	
	private static int getProgress(int initialSize, int remainingSize) {
		return (initialSize - remainingSize) * 100 / initialSize;
	}
}

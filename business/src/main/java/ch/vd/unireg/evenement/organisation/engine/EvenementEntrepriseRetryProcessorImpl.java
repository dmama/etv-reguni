package ch.vd.unireg.evenement.organisation.engine;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseDAO;
import ch.vd.unireg.evenement.organisation.engine.processor.EvenementEntrepriseProcessor;

public class EvenementEntrepriseRetryProcessorImpl implements EvenementEntrepriseRetryProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseRetryProcessorImpl.class);
	
	private EvenementEntrepriseNotificationQueue notificationQueue;
	private EvenementEntrepriseDAO evtEntrepriseDAO;
	private PlatformTransactionManager transactionManager;
	private EvenementEntrepriseProcessor processor;

	@SuppressWarnings("UnusedDeclaration")
	public void setNotificationQueue(EvenementEntrepriseNotificationQueue notificationQueue) {
		this.notificationQueue = notificationQueue;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setEvtEntrepriseDAO(EvenementEntrepriseDAO evtEntrepriseDAO) {
		this.evtEntrepriseDAO = evtEntrepriseDAO;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setProcessor(EvenementEntrepriseProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void retraiteEvenements(@Nullable StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}

		// ensuite, allons rechercher les numéros d'entreprises qui sont concernés (ce sont ces numéros qu'il faudra poster dans la queue)
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		final Set<Long> nosEntreprisesCiviles = template.execute(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {
				return evtEntrepriseDAO.getNosEntreprisesCivilesConcerneesParEvenementsPourRetry();
			}
		});
		
		if (nosEntreprisesCiviles != null && nosEntreprisesCiviles.size() > 0) {
			final Set<Long> remaining = new HashSet<>(nosEntreprisesCiviles);
			final MutableBoolean processorStopping = new MutableBoolean(false);
			final EvenementEntrepriseProcessor.ListenerHandle handle = processor.registerListener(new EvenementEntrepriseProcessor.Listener() {

				@Override
				public void onEntrepriseTraitee(long noEntrepriseCivile) {
					remaining.remove(noEntrepriseCivile);
				}

				@Override
				public void onStop() {
					processorStopping.setValue(true);
				}
			});

			try {
				// on peut maintenant tout envoyer dans la queue
				notificationQueue.postAll(nosEntreprisesCiviles);
				
				// et on attend la fin
				final String msg = "Relance des événements entreprise en erreur";
				final int initialSize = nosEntreprisesCiviles.size();
				while (remaining.size() > 0 && !status.isInterrupted() && !processorStopping.booleanValue()) {
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
				handle.unregister();
			}
		}
	}
	
	private static int getProgress(int initialSize, int remainingSize) {
		return (initialSize - remainingSize) * 100 / initialSize;
	}
}

package ch.vd.uniregctb.evenement.organisation.engine;

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

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessor;

public class EvenementOrganisationRetryProcessorImpl implements EvenementOrganisationRetryProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationRetryProcessorImpl.class);
	
	private EvenementOrganisationNotificationQueue notificationQueue;
	private EvenementOrganisationDAO evtOrganisationDAO;
	private PlatformTransactionManager transactionManager;
	private EvenementOrganisationProcessor processor;

	@SuppressWarnings("UnusedDeclaration")
	public void setNotificationQueue(EvenementOrganisationNotificationQueue notificationQueue) {
		this.notificationQueue = notificationQueue;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setEvtOrganisationDAO(EvenementOrganisationDAO evtOrganisationDAO) {
		this.evtOrganisationDAO = evtOrganisationDAO;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setProcessor(EvenementOrganisationProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void retraiteEvenements(@Nullable StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}

		// ensuite, allons rechercher les numéros d'organisations qui sont concernés (ce sont ces numéros qu'il faudra poster dans la queue)
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		final Set<Long> organisations = template.execute(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {
				return evtOrganisationDAO.getOrganisationsConcerneesParEvenementsPourRetry();
			}
		});
		
		if (organisations != null && organisations.size() > 0) {
			final Set<Long> remaining = new HashSet<>(organisations);
			final MutableBoolean processorStopping = new MutableBoolean(false);
			final EvenementOrganisationProcessor.ListenerHandle handle = processor.registerListener(new EvenementOrganisationProcessor.Listener() {

				@Override
				public void onOrganisationTraite(long noOrganisation) {
					remaining.remove(noOrganisation);
				}

				@Override
				public void onStop() {
					processorStopping.setValue(true);
				}
			});

			try {
				// on peut maintenant tout envoyer dans la queue
				notificationQueue.postAll(organisations);
				
				// et on attend la fin
				final String msg = "Relance des événements organisation en erreur";
				final int initialSize = organisations.size();
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
				handle.unregister();
			}
		}
	}
	
	private static int getProgress(int initialSize, int remainingSize) {
		return (initialSize - remainingSize) * 100 / initialSize;
	}
}

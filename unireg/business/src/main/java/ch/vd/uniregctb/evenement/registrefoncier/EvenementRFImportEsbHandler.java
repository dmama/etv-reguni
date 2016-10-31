package ch.vd.uniregctb.evenement.registrefoncier;

import javax.transaction.Status;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.registrefoncier.TraiterImportsRFJob;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;
import ch.vd.uniregctb.transaction.TxSyncManager;

/**
 * Handler qui reçoit les événements de mutation sur les immeubles de la part du registre foncier (Capitastra)
 */
public class EvenementRFImportEsbHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementRFImportEsbHandler.class);

	private TxSyncManager txSyncManager;
	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;

	public void setTxSyncManager(TxSyncManager txSyncManager) {
		this.txSyncManager = txSyncManager;
	}

	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	public void setEvenementRFImportDAO(EvenementRFImportDAO evenementRFImportDAO) {
		this.evenementRFImportDAO = evenementRFImportDAO;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		final String businessId = message.getBusinessId();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Réception d'un message JMS d'un import RF des immeubles {businessId='%s'}", businessId));
		}

		AuthenticationHelper.pushPrincipal("JMS-ImportRF");
		try {
			final String dataUrl = message.getAttachmentRef("data");

			// on crée et insère l'événement en base
			final EvenementRFImport event = new EvenementRFImport();
			event.setDateEvenement(RegDate.get());  // TODO (msi) on devrait obtenir cette information depuis le message
			event.setEtat(EtatEvenementRF.A_TRAITER);
			event.setFileUrl(dataUrl);
			final Long eventId = evenementRFImportDAO.save(event).getId();

			// on provoque le démarrage du batch de traitement à la fermeture de la transaction
			txSyncManager.registerAfterCompletion(status -> {
				if (status == Status.STATUS_COMMITTED) {
					startBatch(eventId);
				}
			});
		}
		catch (Exception e) {
			// boom technique (bug ou problème avec la DB) -> départ dans la DLQ
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void startBatch(@NotNull Long eventId) {
		AuthenticationHelper.pushPrincipal("JMS-ImportRF");
		try {
			final HashMap<String, Object> params = new HashMap<>();
			params.put(TraiterImportsRFJob.ID, eventId);
			batchScheduler.startJob(TraiterImportsRFJob.NAME, params);
		}
		catch (JobAlreadyStartedException | SchedulerException e) {
			LOGGER.error("Le job n'a pas pu être démarré pour la raison suivante :", e);
			throw new RuntimeException(e);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
}

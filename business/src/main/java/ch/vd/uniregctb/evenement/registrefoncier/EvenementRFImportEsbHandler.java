package ch.vd.uniregctb.evenement.registrefoncier;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierImportService;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;
import ch.vd.uniregctb.transaction.TxSyncManager;

/**
 * Handler qui reçoit les événements de mutation sur les immeubles de la part du registre foncier (Capitastra)
 */
public class EvenementRFImportEsbHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementRFImportEsbHandler.class);

	private TxSyncManager txSyncManager;
	private EvenementRFImportDAO evenementRFImportDAO;
	private RegistreFoncierImportService serviceImportRF;

	public void setTxSyncManager(TxSyncManager txSyncManager) {
		this.txSyncManager = txSyncManager;
	}

	public void setEvenementRFImportDAO(EvenementRFImportDAO evenementRFImportDAO) {
		this.evenementRFImportDAO = evenementRFImportDAO;
	}

	public void setServiceImportRF(RegistreFoncierImportService serviceImportRF) {
		this.serviceImportRF = serviceImportRF;
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
			final String dateAsString = message.getHeader("dateValeur");
			final String typeAsString = message.getHeader("typeImport");

			final RegDate dateValeur = RegDateHelper.indexStringToDate(dateAsString);
			if (dateValeur == null) {
				throw new IllegalArgumentException("La date de valeur de l'import n'est pas renseignée ou invalide (" + dateAsString + ")");
			}

			final TypeImportRF type = parseTypeImport(typeAsString);

			// on crée et insère l'événement en base
			final EvenementRFImport event = new EvenementRFImport();
			event.setType(type);
			event.setDateEvenement(dateValeur);
			event.setEtat(EtatEvenementRF.A_TRAITER);
			event.setFileUrl(dataUrl);
			final Long eventId = evenementRFImportDAO.save(event).getId();

			// on provoque le démarrage du batch de traitement à la fermeture de la transaction
			txSyncManager.registerAfterCompletion(status -> {
				if (status == TransactionSynchronization.STATUS_COMMITTED) {
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

	@NotNull
	private static TypeImportRF parseTypeImport(String typeAsString) {
		if (StringUtils.isBlank(typeAsString) || typeAsString.equals("PRINCIPAL")) {
			return TypeImportRF.PRINCIPAL;
		}
		else if (typeAsString.equals("SERVITUDES")) {
			return TypeImportRF.SERVITUDES;
		}
		else {
			throw new IllegalArgumentException("Type d'import inconnu = [" + typeAsString + "]");
		}
	}

	private void startBatch(@NotNull Long eventId) {
		AuthenticationHelper.pushPrincipal("JMS-ImportRF");
		try {
			serviceImportRF.startImport(eventId);
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

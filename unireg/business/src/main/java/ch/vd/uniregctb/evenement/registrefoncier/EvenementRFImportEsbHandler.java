package ch.vd.uniregctb.evenement.registrefoncier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.jms.EsbMessageHandler;

/**
 * Handler qui reçoit les événements de mutation sur les immeubles de la part du registre foncier (Capitastra)
 */
public class EvenementRFImportEsbHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementRFImportEsbHandler.class);

	private EvenementRFImportDAO evenementRFImportDAO;

	public void setEvenementRFImportDAO(EvenementRFImportDAO evenementRFImportDAO) {
		this.evenementRFImportDAO = evenementRFImportDAO;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		final String businessId = message.getBusinessId();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Réception d'un message JMS d'un import RF des immeubles {businessId='%s'}", businessId));
		}

		try {
			final String dataUrl = message.getAttachmentRef("data");

			// on crée et insère l'événement en base
			EvenementRFImport event = new EvenementRFImport();
			event.setDateEvenement(RegDate.get());  // TODO (msi) on devrait obtenir cette information depuis le message
			event.setEtat(EtatEvenementRF.A_TRAITER);
			event.setFileUrl(dataUrl);
			evenementRFImportDAO.save(event);

			// TODO (msi) provoquer le démarrage du batch de traitement à la fermeture de la transaction
		}
		catch (Exception e) {
			// boom technique (bug ou problème avec la DB) -> départ dans la DLQ
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}
}

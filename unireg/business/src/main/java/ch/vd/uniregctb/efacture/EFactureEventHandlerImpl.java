package ch.vd.uniregctb.efacture;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;

public class EFactureEventHandlerImpl implements EFactureEventHandler {

	private final Logger LOGGER = Logger.getLogger(EFactureEventHandlerImpl.class);

	@Override
	public void handle(EFactureEvent event) {
		if (event instanceof DemandeValidationInscription) {
			final DemandeValidationInscription valid = (DemandeValidationInscription) event;
			if (valid.getAction() == DemandeValidationInscription.Action.DESINSCRIPTION) {
				LOGGER.info(String.format("Reçu demande de désinscription e-Facture du contribuable %s au %s", FormatNumeroHelper.numeroCTBToDisplay(valid.getCtbId()), RegDateHelper.dateToDisplayString(valid.getDateDemande())));
			}
			else {
				// TODO e-facture à faire
				LOGGER.info(String.format("Reçu demande d'inscription e-Facture du contribuable %s au %s", FormatNumeroHelper.numeroCTBToDisplay(valid.getCtbId()), RegDateHelper.dateToDisplayString(valid.getDateDemande())));
			}
		}
		else {
			throw new IllegalArgumentException("Type d'événement non supporté : " + event.getClass());
		}
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("eVD-0025-1-0.xsd");
	}
}

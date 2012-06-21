package ch.vd.uniregctb.efacture;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.type.TypeDocument;

public class EFactureEventHandlerImpl implements EFactureEventHandler {

	private final Logger LOGGER = Logger.getLogger(EFactureEventHandlerImpl.class);

	private EFactureService eFactureService;
	private EFactureMessageSender sender;

	public void seteFactureService(EFactureService eFactureService) {
		this.eFactureService = eFactureService;
	}

	public void setSender(EFactureMessageSender sender) {
		this.sender = sender;
	}

	@Override
	public void handle(EFactureEvent event) {
		if (event instanceof DemandeValidationInscription) {
			final DemandeValidationInscription valid = (DemandeValidationInscription) event;
			if (valid.getAction() == DemandeValidationInscription.Action.DESINSCRIPTION) {
				LOGGER.info(String.format("Reçu demande de désinscription e-Facture du contribuable %s au %s", FormatNumeroHelper.numeroCTBToDisplay(valid.getCtbId()), RegDateHelper.dateToDisplayString(valid.getDateDemande())));
			}
			else {
				final DemandeValidationInscription inscription = (DemandeValidationInscription) event;
				LOGGER.info(String.format("Reçu demande d'inscription e-Facture du contribuable %s/%s au %s", FormatNumeroHelper.numeroCTBToDisplay(inscription.getCtbId()), FormatNumeroHelper.formatNumAVS(inscription.getNoAvs()), RegDateHelper.dateToDisplayString(inscription.getDateDemande())));
				try {
					TypeRefusEFacture typeRefus = inscription.performBasicValidation();
					if (typeRefus != null) {
						sender.envoieRefusDemandeInscription(inscription.getIdDemande(), typeRefus);
						return;
					}

					// Vérification de la demandes en cours
					if (eFactureService.getDemandeInscritpionEnCoursDeTraitement(inscription.getCtbId()) != null) {
						sender.envoieRefusDemandeInscription(inscription.getIdDemande(), TypeRefusEFacture.AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT);
						return;
					}

					// identifie le contribuable (no avs et no ctb doivent matché, le ctb doit avoir une adresse courrier)
					typeRefus = eFactureService.identifieContribuablePourInscription(inscription.getCtbId(), inscription.getNoAvs());
					if (typeRefus != null) {
						sender.envoieRefusDemandeInscription(inscription.getIdDemande(), typeRefus);
						return;
					}

					//Stocke l’adresse de courrier électronique de la demande
					eFactureService.updateEmailContribuable(inscription.getCtbId(), inscription.getEmail());

					// valide l'etat du contribuable et envoye le courrier adéquat
					if (eFactureService.valideEtatContribuablePourInscription(inscription.getCtbId())) {
						String archivageId = eFactureService.imprimerDocumentEfacture(inscription.getCtbId(), TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, inscription.getDateDemande());
						sender.envoieMiseEnAttenteDemandeInscription(inscription.getIdDemande(), TypeAttenteEFacture.EN_ATTENTE_SIGNATURE, archivageId, false);
					} else {
						String archivageId = eFactureService.imprimerDocumentEfacture(inscription.getCtbId(), TypeDocument.E_FACTURE_ATTENTE_CONTACT, inscription.getDateDemande());
						sender.envoieMiseEnAttenteDemandeInscription(inscription.getIdDemande(), TypeAttenteEFacture.EN_ATTENTE_CONTACT, archivageId, false);
					}

				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
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

package ch.vd.uniregctb.efacture;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
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
	public void handle(Demande demande) throws Exception {
		if (demande.getAction() == Demande.Action.DESINSCRIPTION) {
			LOGGER.info(String.format("Reçu demande de désinscription e-Facture du contribuable %s au %s", FormatNumeroHelper.numeroCTBToDisplay(demande.getCtbId()), RegDateHelper.dateToDisplayString(demande.getDateDemande())));
		}
		else {
			LOGGER.info(String.format("Reçu demande d'inscription e-Facture du contribuable %s/%s au %s", FormatNumeroHelper.numeroCTBToDisplay(demande.getCtbId()), FormatNumeroHelper.formatNumAVS(
					demande.getNoAvs()), RegDateHelper.dateToDisplayString(demande.getDateDemande())));

			TypeRefusDemande typeRefus = demande.performBasicValidation();
			if (typeRefus != null) {
				sender.envoieRefusDemandeInscription(demande.getIdDemande(), typeRefus, "", false);
				return;
			}

			// Vérification de la demandes en cours
			if (eFactureService.getDemandeEnAttente(demande.getCtbId()) != null) {
				sender.envoieRefusDemandeInscription(demande.getIdDemande(), TypeRefusDemande.AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT, "", false);
				return;
			}

			// identifie le contribuable (no avs et no ctb doivent matché, le ctb doit avoir une adresse courrier)
			typeRefus = eFactureService.identifieContribuablePourInscription(demande.getCtbId(), demande.getNoAvs());
			if (typeRefus != null) {
				sender.envoieRefusDemandeInscription(demande.getIdDemande(), typeRefus, "", false);
				return;
			}

			//Stocke l’adresse de courrier électronique de la demande
			eFactureService.updateEmailContribuable(demande.getCtbId(), demande.getEmail());

			// valide l'etat du contribuable et envoye le courrier adéquat
			if (eFactureService.valideEtatContribuablePourInscription(demande.getCtbId())) {
				String archivageId = eFactureService.imprimerDocumentEfacture(demande.getCtbId(), TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, demande.getDateDemande());
				sender.envoieMiseEnAttenteDemandeInscription(demande.getIdDemande(), TypeAttenteDemande.EN_ATTENTE_SIGNATURE, TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), archivageId, false);
			} else {
				String archivageId = eFactureService.imprimerDocumentEfacture(demande.getCtbId(), TypeDocument.E_FACTURE_ATTENTE_CONTACT, demande.getDateDemande());
				sender.envoieMiseEnAttenteDemandeInscription(demande.getIdDemande(), TypeAttenteDemande.EN_ATTENTE_CONTACT, TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), archivageId, false);
			}

		}
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("eVD-0025-1-0.xsd");
	}
}

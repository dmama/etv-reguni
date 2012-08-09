package ch.vd.uniregctb.efacture;

import org.apache.commons.lang.StringUtils;
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
			LOGGER.info(String.format("Reçu demande de désinscription e-Facture (%s) du contribuable %s au %s", demande.getIdDemande(),
			                          FormatNumeroHelper.numeroCTBToDisplay(demande.getCtbId()), RegDateHelper.dateToDisplayString(demande.getDateDemande())));
		}
		else {
			LOGGER.info(String.format("Reçu demande d'inscription e-Facture (%s) du contribuable %s/%s au %s", demande.getIdDemande(), FormatNumeroHelper.numeroCTBToDisplay(demande.getCtbId()),
			                          FormatNumeroHelper.formatNumAVS(demande.getNoAvs()), RegDateHelper.dateToDisplayString(demande.getDateDemande())));

			TypeRefusDemande typeRefus = demande.performBasicValidation();
			if (typeRefus != null) {
				refuseDemande(demande, typeRefus);
				return;
			}

			// Vérification de la demandes en cours
			if (eFactureService.getDemandeEnAttente(demande.getCtbId()) != null) {
				refuseDemande(demande, TypeRefusDemande.AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT);
				return;
			}

			// identifie le contribuable (no avs et no ctb doivent matché, le ctb doit avoir une adresse courrier)
			typeRefus = eFactureService.identifieContribuablePourInscription(demande.getCtbId(), demande.getNoAvs());
			if (typeRefus != null) {
				refuseDemande(demande, typeRefus);
				return;
			}

			//Stocke l’adresse de courrier électronique de la demande
			eFactureService.updateEmailContribuable(demande.getCtbId(), demande.getEmail());

			// valide l'etat du contribuable et envoye le courrier adéquat
			final TypeAttenteDemande etatFinal;
			if (eFactureService.valideEtatFiscalContribuablePourInscription(demande.getCtbId())) {
				etatFinal = TypeAttenteDemande.EN_ATTENTE_SIGNATURE;
				final String archivageId = eFactureService.imprimerDocumentEfacture(demande.getCtbId(), TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, demande.getDateDemande());
				sender.envoieMiseEnAttenteDemandeInscription(demande.getIdDemande(), etatFinal, etatFinal.getDescription(), archivageId, false);
			}
			else {
				etatFinal = TypeAttenteDemande.EN_ATTENTE_CONTACT;
				final String archivageId = eFactureService.imprimerDocumentEfacture(demande.getCtbId(), TypeDocument.E_FACTURE_ATTENTE_CONTACT, demande.getDateDemande());
				final String description = String.format("%s Assujettissement incohérent avec la e-facture.", etatFinal.getDescription());
				sender.envoieMiseEnAttenteDemandeInscription(demande.getIdDemande(), etatFinal, description, archivageId, false);
			}

			LOGGER.info(String.format("Demande d'inscription passée à l'état %s", etatFinal));
		}
	}

	private void refuseDemande(Demande demande, TypeRefusDemande refus) throws EvenementEfactureException {
		sender.envoieRefusDemandeInscription(demande.getIdDemande(), refus, StringUtils.EMPTY, false);
		LOGGER.info(String.format("Demande d'inscription refusée : %s", refus));
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("eVD-0025-1-0.xsd");
	}
}

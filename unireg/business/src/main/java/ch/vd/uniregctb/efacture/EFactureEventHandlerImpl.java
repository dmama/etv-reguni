package ch.vd.uniregctb.efacture;

import org.apache.commons.validator.EmailValidator;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;

public class EFactureEventHandlerImpl implements EFactureEventHandler {

	private final Logger LOGGER = Logger.getLogger(EFactureEventHandlerImpl.class);
	private EFactureMessageSender sender;

	@Override
	public void handle(EFactureEvent event) {
		if (event instanceof DemandeValidationInscription) {
			final DemandeValidationInscription valid = (DemandeValidationInscription) event;
			if (valid.getAction() == DemandeValidationInscription.Action.DESINSCRIPTION) {
				LOGGER.info(String.format("Reçu demande de désinscription e-Facture du contribuable %s au %s", FormatNumeroHelper.numeroCTBToDisplay(valid.getCtbId()), RegDateHelper.dateToDisplayString(valid.getDateDemande())));
			}
			else {
				// TODO e-facture à faire
				final DemandeValidationInscription inscription = (DemandeValidationInscription) event;
				LOGGER.info(String.format("Reçu demande d'inscription e-Facture du contribuable %s/%s au %s", FormatNumeroHelper.numeroCTBToDisplay(inscription.getCtbId()), FormatNumeroHelper.formatNumAVS(inscription.getNoAvs()), RegDateHelper.dateToDisplayString(inscription.getDateDemande())));
				try {
					if (!performBasicChecks(inscription)) {
						return;
					}

					// 3.1.3. Vérifier demandes en cours

					// 3.1.4. Identifier le contribuable

					// 3.1.5. Stocker l’adresse de courrier électronique de la demande

					// 3.1.6. Vérifier l’existence d’une adresse courrier

					// 3.1.7. Vérifier l’historique e-facture

					// 3.1.8. Vérifier de l’état du contribuable

					// 3.1.9. Envoi des courriers

					// 3.1.10. L’historique des états
					   // ---> TODO Rien, si j'ai bien compris, l'historisation des états est géré par l'e-Facture

					// 3.1.11. Archiver les documents de correspondance
						// ---> On renvoie la clé d'archivage a e-facture qui la stocke

				} catch (EvenementEfactureException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
		else {
			throw new IllegalArgumentException("Type d'événement non supporté : " + event.getClass());
		}
	}

	/**
	 * @param inscription l'inscription dont on doit verifier les données de base: n° avs, email, date de la demande
	 *
	 * @return true si les verifications de base sont ok; false sinon
	 *
	 * @throws EvenementEfactureException s'il y a un problème avec l'envoi d'un message d'erreur
	 */
	private boolean performBasicChecks(DemandeValidationInscription inscription) throws EvenementEfactureException {
		// Check Numéro AVS à 13 chiffres
		if (!AvsHelper.isValidNouveauNumAVS(inscription.getNoAvs())) {
			sender.envoieRefusDemandeInscription(inscription.getIdDemande(), TypeRefusEFacture.NUMERO_AVS_INVALIDE);
			return false;
		}
		// Check Adresse de courrier électronique
		if (!EmailValidator.getInstance().isValid(inscription.getEmail())) {
			sender.envoieRefusDemandeInscription(inscription.getIdDemande(), TypeRefusEFacture.EMAIL_INVALIDE);
			return false;
		}
		// Check Date et heure de la demande
		// TODO A DISCUTER: Les specs parlent d'heure de la demande or RegDate n'a pas d'heure
		if (inscription.getDateDemande() == null) {
			sender.envoieRefusDemandeInscription(inscription.getIdDemande(), TypeRefusEFacture.DATE_DEMANDE_ABSENTE);
			return false;
		}

		return true;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("eVD-0025-1-0.xsd");
	}
}

package ch.vd.uniregctb.evenement.organisation.interne.passeplat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * Detection d'événements à propager sans autre effet.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class PassePlatStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(PassePlatStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException {

		// On ne s'occupe que d'entités déjà connues
		// TODO: Retrouver aussi les entreprises n'ayant pas d'id cantonal.
		if (entreprise == null) {
			return null;
		}

		TypeEvenementOrganisation evtType = event.getType();
		switch (evtType) {
		case FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE);
		case FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS:
			LOGGER.info("Evénement de type [{}] -> Propagation + à vérifier.", evtType);
			return new PassePlatAVerifier(event, organisation, entreprise, context, options, TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS);
		case FOSC_SUSPENSION_FAILLITE:
			LOGGER.info("Evénement de type [{}] -> Propagation + à vérifier.", evtType);
			return new PassePlatAVerifier(event, organisation, entreprise, context, options, TypeInformationComplementaire.SUSPENSION_FAILLITE);
		case FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.ETAT_COLLOCATION_INVENTAIRE_FAILLITE);
		case FOSC_CLOTURE_DE_LA_FAILLITE:
			LOGGER.info("Evénement de type [{}] -> Propagation + à vérifier.", evtType);
			return new PassePlatAVerifier(event, organisation, entreprise, context, options, TypeInformationComplementaire.CLOTURE_FAILLITE);
		case FOSC_REVOCATION_DE_LA_FAILLITE:
			LOGGER.info("Evénement de type [{}] -> Propagation + à vérifier.", evtType);
			return new PassePlatAVerifier(event, organisation, entreprise, context, options, TypeInformationComplementaire.REVOCATION_FAILLITE);
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE);
		case FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.SURSIS_CONCORDATAIRE_PROVISOIRE);
		case FOSC_SURSIS_CONCORDATAIRE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.SURSIS_CONCORDATAIRE);
		case FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT);
		case FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.AUDIENCE_LIQUIDATION_ABANDON_ACTIF);
		case FOSC_PROLONGATION_SURSIS_CONCORDATAIRE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.PROLONGATION_SURSIS_CONCORDATAIRE);
		case FOSC_ANNULATION_SURSIS_CONCORDATAIRE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.ANNULATION_SURSIS_CONCORDATAIRE);
		case FOSC_HOMOLOGATION_DU_CONCORDAT:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.HOMOLOGATION_CONCORDAT);
		case FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF);
		case FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT);
		case FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.CONCORDAT_BANQUE_CAISSE_EPARGNE);
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE:
			LOGGER.info("Evénement de type [{}] -> Propagation.", evtType);
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE);
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER:
			LOGGER.info("Evénement de type [{}] -> Propagation + à vérifier.", evtType);
			return new PassePlatAVerifier(event, organisation, entreprise, context, options, TypeInformationComplementaire.APPEL_CREANCIERS_TRANSFERT_HS);
		default:
			LOGGER.info("Evénement de type [{}] -> Pas de propagation.", evtType);
			return null;
		}
	}
}

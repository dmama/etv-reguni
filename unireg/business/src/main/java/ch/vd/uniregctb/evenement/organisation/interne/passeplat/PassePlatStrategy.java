package ch.vd.uniregctb.evenement.organisation.interne.passeplat;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

import static ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * Detection d'événements à propager sans autre effet.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class PassePlatStrategy extends AbstractOrganisationStrategy {

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

		switch (event.getType()) {
		case FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE);
		case FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.ETAT_COLLOCATION_INVENTAIRE_FAILLITE);
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE);
		case FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.SURSIS_CONCORDATAIRE_PROVISOIRE);
		case FOSC_SURSIS_CONCORDATAIRE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.SURSIS_CONCORDATAIRE);
		case FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT);
		case FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.AUDIENCE_LIQUIDATION_ABANDON_ACTIF);
		case FOSC_PROLONGATION_SURSIS_CONCORDATAIRE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.PROLONGATION_SURSIS_CONCORDATAIRE);
		case FOSC_ANNULATION_SURSIS_CONCORDATAIRE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.ANNULATION_SURSIS_CONCORDATAIRE);
		case FOSC_HOMOLOGATION_DU_CONCORDAT:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.HOMOLOGATION_CONCORDAT);
		case FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF);
		case FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT);
		case FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.CONCORDAT_BANQUE_CAISSE_EPARGNE);
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE:
			return new PassePlat(event, organisation, entreprise, context, options, TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE);
		default:
			return null;
		}
	}
}

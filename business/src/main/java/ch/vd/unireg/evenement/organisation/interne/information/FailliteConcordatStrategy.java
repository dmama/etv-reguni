package ch.vd.unireg.evenement.organisation.interne.information;

import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * Detection de changement situation de faillite et concordat à propager sans autre effet. Ces changements
 * sont détecté par le type de l'événement uniquement et n'ont pas d'existence (au 2015-11-04) dans les données RCEnt.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class FailliteConcordatStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public FailliteConcordatStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		TypeEvenementEntreprise evtType = event.getType();
		switch (evtType) {
		case FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE);
		case FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation + à vérifier.", evtType));
			return new InformationComplementaireAVerifier(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS);
		case FOSC_SUSPENSION_FAILLITE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation + à vérifier.", evtType));
			return new InformationComplementaireAVerifier(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.SUSPENSION_FAILLITE);
		case FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.ETAT_COLLOCATION_INVENTAIRE_FAILLITE);
		case FOSC_CLOTURE_DE_LA_FAILLITE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation + à vérifier.", evtType));
			return new InformationComplementaireAVerifier(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.CLOTURE_FAILLITE);
		case FOSC_REVOCATION_DE_LA_FAILLITE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation + à vérifier.", evtType));
			return new InformationComplementaireAVerifier(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.REVOCATION_FAILLITE);
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE);
		case FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.SURSIS_CONCORDATAIRE_PROVISOIRE);
		case FOSC_SURSIS_CONCORDATAIRE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.SURSIS_CONCORDATAIRE);
		case FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT);
		case FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.AUDIENCE_LIQUIDATION_ABANDON_ACTIF);
		case FOSC_PROLONGATION_SURSIS_CONCORDATAIRE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.PROLONGATION_SURSIS_CONCORDATAIRE);
		case FOSC_ANNULATION_SURSIS_CONCORDATAIRE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.ANNULATION_SURSIS_CONCORDATAIRE);
		case FOSC_HOMOLOGATION_DU_CONCORDAT:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.HOMOLOGATION_CONCORDAT);
		case FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF);
		case FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT);
		case FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.CONCORDAT_BANQUE_CAISSE_EPARGNE);
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation.", evtType));
			return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE);
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Propagation + à vérifier.", evtType));
			return new InformationComplementaireAVerifier(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.APPEL_CREANCIERS_TRANSFERT_HS);
		default:
			Audit.info(event.getId(), String.format("Evénement de type [%s] -> Pas de propagation.", evtType));
			return null;
		}
	}
}

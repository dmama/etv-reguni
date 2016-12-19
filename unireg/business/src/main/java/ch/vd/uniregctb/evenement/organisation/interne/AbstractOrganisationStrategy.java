package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateEntreprise;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Classe regroupant des méthodes communes. Certaines sont clairement des paliatifs en attendant une meilleure
 * solution.
 *
 * @author Raphaël Marmier, 2015-10-02
 */
public abstract class AbstractOrganisationStrategy implements EvenementOrganisationTranslationStrategy {

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntreprise} est
	 * pertinente.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public abstract EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException;


	/**
	 * Est-ce que cette organisation existant d'aujourd'hui existait déjà hier, selon RCEnt?
	 *
	 * @param organisation Une organisation existant pour la date fournie
	 * @param date La date "aujourd'hui"
	 * @return Vrai si existait hier
	 */
	protected static boolean isExisting(Organisation organisation, RegDate date) throws EvenementOrganisationException {
		String nom = organisation.getNom(date);
		if (nom == null) {
			throw new EvenementOrganisationException(
					String.format("Entreprise %s inexistante au %s ne peut être utilisée pour savoir si elle existe déjà à cette date. Ne devrait jamais arriver en production.",
					              organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(date)));
		}
		return organisation.getNom(date.getOneDayBefore()) != null;
	}

	protected static boolean isCreation(TypeEvenementOrganisation type, Organisation organisation, RegDate date) throws EvenementOrganisationException {
		switch (type) {
		case FOSC_NOUVELLE_ENTREPRISE:
			return nouveauAuRc(organisation, date);
		case FOSC_NOUVELLE_SUCCURSALE:
			return nouveauAuRc(organisation, date);
		case FOSC_DISSOLUTION_ENTREPRISE:
			return false;
		case FOSC_RADIATION_ENTREPRISE:
			return false;
		case FOSC_RADIATION_SUCCURSALE:
			return false;
		case FOSC_REVOCATION_DISSOLUTION_ENTREPRISE:
			return false;
		case FOSC_REINSCRIPTION_ENTREPRISE:
			return false;
		case FOSC_AUTRE_MUTATION:
			return false;
		case IMPORTATION_ENTREPRISE:
			return false;
		case FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE:
			return false;
		case FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS:
			return false;
		case FOSC_SUSPENSION_FAILLITE:
			return false;
		case FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE:
			return false;
		case FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE:
			return false;
		case FOSC_CLOTURE_DE_LA_FAILLITE:
			return false;
		case FOSC_REVOCATION_DE_LA_FAILLITE:
			return false;
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE:
			return false;
		case FOSC_ETAT_DES_CHARGES_DANS_FAILLITE:
			return false;
		case FOSC_COMMUNICATION_DANS_FAILLITE:
			return false;
		case FOSC_DEMANDE_SURSIS_CONCORDATAIRE:
			return false;
		case FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE:
			return false;
		case FOSC_SURSIS_CONCORDATAIRE:
			return false;
		case FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT:
			return false;
		case FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF:
			return false;
		case FOSC_PROLONGATION_SURSIS_CONCORDATAIRE:
			return false;
		case FOSC_ANNULATION_SURSIS_CONCORDATAIRE:
			return false;
		case FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS:
			return false;
		case FOSC_HOMOLOGATION_DU_CONCORDAT:
			return false;
		case FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT:
			return false;
		case FOSC_REVOCATION_DU_CONCORDAT:
			return false;
		case FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			return false;
		case FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			return false;
		case FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE:
			return false;
		case FOSC_COMMUNICATION_DANS_LE_CONCORDAT:
			return false;
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE:
			return false;
		case FOSC_COMMANDEMENT_DE_PAYER:
			return false;
		case FOSC_PROCES_VERBAL_SEQUESTRE:
			return false;
		case FOSC_PROCES_VERBAL_SAISIE:
			return false;
		case FOSC_COMMUNICATION_DANS_LA_POURSUITE:
			return false;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION:
			return false;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION:
			return false;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL:
			return false;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL:
			return false;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER:
			return false;
		case IDE_NOUVELLE_INSCRIPTION:
			return !organisation.isConnueInscriteAuRC(date) || nouveauAuRc(organisation, date);
		case IDE_MUTATION:
			return false;
		case IDE_RADIATION:
			return false;
		case IDE_REACTIVATION:
			return false;
		case IDE_ANNULATION:
			return false;
		case RCPERS_DECES:
			return false;
		case RCPERS_ANNULATION_DECES:
			return false;
		case RCPERS_DEPART:
			return false;
		case RCPERS_ANNULATION_DEPART:
			return false;
		case RCPERS_CORRECTION_DONNEES:
			return false;
		case REE_NOUVELLE_INSCRIPTION:
			return !organisation.isConnueInscriteAuRC(date) || nouveauAuRc(organisation, date);
		case REE_MUTATION:
			return false;
		case REE_SUPPRESSION:
			return false;
		case REE_RADIATION:
			return false;
		case REE_TRANSFERT_ETABLISSEMENT:
			return false;
		case REE_REACTIVATION:
			return false;
		default:
			throw new IllegalArgumentException("TypeEvenementOrganisation non supporté. Impossible de déterminer la création d'entreprise: " + type.name());
		}
	}

	/**
	 * @return Vrai si ... voir le code.
	 */
	private static boolean nouveauAuRc(Organisation organisation, RegDate date) throws EvenementOrganisationException {
		if (organisation.isConnueInscriteAuRC(date)) {
			final SiteOrganisation sitePrincipal = organisation.getSitePrincipal(date).getPayload();
			/* Les données avec lesquelles on travaille */
			final InscriptionRC inscriptionRC = sitePrincipal.getDonneesRC().getInscription(date);
			final RegDate dateInscriptionCh = inscriptionRC != null ? inscriptionRC.getDateInscriptionCH() : null;
			final RegDate dateInscriptionVd = inscriptionRC != null ? inscriptionRC.getDateInscriptionVD() : null;

			/* On travaille selon le postulat que toute date d'inscription éloignée de plus d'un certain nombre de jour (seuil) de la date d'événement
			   indique que l'entreprise est pré-existante et qu'il n'y a donc pas de création à la date fournie. */
			final RegDate newnessThresholdDate = date.addDays( - OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);

			/*
			    NOTE: Nous devons tenir compte du fait que la date d'inscription au RC CH peut être nulle
			          lorsque la date d'inscription au RC VD est renseignée par RCEnt. Problème RCEnt.
			 */

			/* Si la date d'inscription au RC est antérieure à la date de seuil, on considère que l'entreprise
			   est pré-existante à la date de l'événement */
			if (dateInscriptionCh != null && dateInscriptionCh.isBefore(newnessThresholdDate)) {
				return false;
			}

			/* Idem pour la date Vd */
			if (dateInscriptionVd != null && dateInscriptionVd.isBefore(newnessThresholdDate)) {
				return false;
			}

			/* Nous sommes dans le délai pour une création hors canton. L'erreur est limité à quelque jours s'il ne s'agit en vérité pas d'une création. */
			if (dateInscriptionVd == null && dateInscriptionCh != null){
				return true;
			}
			/* Impossible de savoir avec certitude. La date au RC CH est peut-être très ancienne. */
			else if (dateInscriptionVd != null && dateInscriptionCh == null) {
				throw new EvenementOrganisationException("Date d'inscription au RC CH introuvable alors qu'on a une date au RC VD!");
			}
			/* Les deux dates sont nulles, les données de RCEnt sont invalides. */
			else if (dateInscriptionVd == null) {
				throw new PasDeDateInscriptionRCVD();
			}
			/* Création si identique. */
			else {
				return dateInscriptionVd == dateInscriptionCh;
			}
		}
		return false;
	}

	protected InformationDeDateEtDeCreation extraireInformationDeDateEtDeCreation(EvenementOrganisation event, Organisation organisation) throws EvenementOrganisationException {
		final RegDate dateEvenement = event.getDateEvenement();

		SiteOrganisation sitePrincipal = organisation.getSitePrincipal(dateEvenement).getPayload();
		final Domicile siege = sitePrincipal.getDomicile(dateEvenement);
		final boolean isVaudoise = siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		final boolean isApm = CategorieEntrepriseHelper.map(organisation.getFormeLegale(dateEvenement)) == CategorieEntreprise.APM; // SIFISC-22478 - Ne pas appliquer la règle jour + 1 pour les APM.
		final boolean inscritAuRC = organisation.isInscriteAuRC(dateEvenement);
		final RegDate dateInscriptionRCVd;
		final RegDate dateInscriptionRC;
		final RegDate dateDeCreation;
		final RegDate dateOuvertureFiscale;
		final boolean isCreation;
		if (inscritAuRC) {
			dateInscriptionRCVd = sitePrincipal.getDateInscriptionRCVd(dateEvenement);
			if (isVaudoise && dateInscriptionRCVd == null) {
				throw new PasDeDateInscriptionRCVD();
			}
			dateInscriptionRC = sitePrincipal.getDateInscriptionRC(dateEvenement);
			isCreation = isCreation(event.getType(), organisation,
			                        dateEvenement); // On ne peut pas l'appeler avant car on doit d'abord s'assurer que l'inscription RC VD existe si on est inscrit au RC et vaudois.
			if (isCreation) {
				if (isVaudoise) {
					dateDeCreation = dateInscriptionRCVd;
					dateOuvertureFiscale = dateInscriptionRCVd.getOneDayAfter();
				}
				else {
					dateDeCreation = dateInscriptionRC;
					dateOuvertureFiscale = dateInscriptionRC;
				}
			}
			else { // Une arrivée
				dateDeCreation = dateInscriptionRCVd;
				dateOuvertureFiscale = dateInscriptionRCVd;
			}
		}
		else {
			isCreation = isCreation(event.getType(), organisation, dateEvenement);
			if (isCreation && isVaudoise && !isApm) { // SIFISC-22478 - Ne pas appliquer la règle jour + 1 pour les APM.
				dateDeCreation = dateEvenement;
				dateOuvertureFiscale = dateEvenement.getOneDayAfter();
			}
			else {
				dateDeCreation = dateEvenement;
				dateOuvertureFiscale = dateEvenement;
			}
		}
		return new InformationDeDateEtDeCreation(dateDeCreation, dateOuvertureFiscale, isCreation);
	}

	protected static class PasDeDateInscriptionRCVD extends EvenementOrganisationException {

		public PasDeDateInscriptionRCVD() {
			super("Date d'inscription au registre vaudois du commerce introuvable pour l'établissement principal vaudois.");
		}
	}

	protected static class InformationDeDateEtDeCreation {
		RegDate dateDeCreation;
		RegDate dateOuvertureFiscale;
		boolean isCreation;

		public InformationDeDateEtDeCreation(RegDate dateDeCreation, RegDate dateOuvertureFiscale, boolean isCreation) {
			this.dateDeCreation = dateDeCreation;
			this.dateOuvertureFiscale = dateOuvertureFiscale;
			this.isCreation = isCreation;
		}

		public RegDate getDateDeCreation() {
			return dateDeCreation;
		}

		public RegDate getDateOuvertureFiscale() {
			return dateOuvertureFiscale;
		}

		public boolean isCreation() {
			return isCreation;
		}
	}
}

package ch.vd.unireg.evenement.entreprise.interne;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.engine.translator.EvenementEntrepriseTranslationStrategy;
import ch.vd.unireg.evenement.entreprise.interne.creation.CreateEntreprise;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseHelper;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

/**
 * Classe regroupant des méthodes communes. Certaines sont clairement des paliatifs en attendant une meilleure
 * solution.
 *
 * @author Raphaël Marmier, 2015-10-02
 */
public abstract class AbstractEntrepriseStrategy implements EvenementEntrepriseTranslationStrategy {


	protected final EvenementEntrepriseContext context;
	protected final EvenementEntrepriseOptions options;

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public AbstractEntrepriseStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		this.context = context;
		this.options = options;
	}

	public EvenementEntrepriseContext getContext() {
		return context;
	}

	public EvenementEntrepriseOptions getOptions() {
		return options;
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntreprise} est
	 * pertinente.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public abstract EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event,
	                                                          final EntrepriseCivile entrepriseCivile,
	                                                          Entreprise entreprise) throws EvenementEntrepriseException;


	/**
	 * Est-ce que cette entreprise civile existant d'aujourd'hui existait déjà hier, selon RCEnt?
	 *
	 * @param entrepriseCivile une entreprise civile existant pour la date fournie
	 * @param date La date "aujourd'hui"
	 * @return Vrai si existait hier
	 */
	protected static boolean isExisting(EntrepriseCivile entrepriseCivile, RegDate date) throws EvenementEntrepriseException {
		String nom = entrepriseCivile.getNom(date);
		if (nom == null) {
			throw new EvenementEntrepriseException(
					String.format("Entreprise %s inexistante au %s ne peut être utilisée pour savoir si elle existe déjà à cette date. Ne devrait jamais arriver en production.",
					              entrepriseCivile.getNumeroEntreprise(), RegDateHelper.dateToDisplayString(date)));
		}
		return entrepriseCivile.getNom(date.getOneDayBefore()) != null;
	}

	protected static boolean isCreation(TypeEvenementEntreprise type, EntrepriseCivile entrepriseCivile, RegDate date) throws EvenementEntrepriseException {
		switch (type) {
		case FOSC_NOUVELLE_ENTREPRISE:
			return nouveauAuRc(entrepriseCivile, date);
		case FOSC_NOUVELLE_SUCCURSALE:
			return nouveauAuRc(entrepriseCivile, date);
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
			return !entrepriseCivile.isConnueInscriteAuRC(date) || nouveauAuRc(entrepriseCivile, date);
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
			return !entrepriseCivile.isConnueInscriteAuRC(date) || nouveauAuRc(entrepriseCivile, date);
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
			throw new IllegalArgumentException("TypeEvenementEntreprise non supporté. Impossible de déterminer la création d'entreprise: " + type.name());
		}
	}

	/**
	 * @return Vrai si ... voir le code.
	 */
	private static boolean nouveauAuRc(EntrepriseCivile entrepriseCivile, RegDate date) throws EvenementEntrepriseException {
		if (entrepriseCivile.isConnueInscriteAuRC(date)) {
			final EtablissementCivil etablissementPrincipal = entrepriseCivile.getEtablissementPrincipal(date).getPayload();
			/* Les données avec lesquelles on travaille */
			final InscriptionRC inscriptionRC = etablissementPrincipal.getDonneesRC().getInscription(date);
			final RegDate dateInscriptionCh = inscriptionRC != null ? inscriptionRC.getDateInscriptionCH() : null;
			final RegDate dateInscriptionVd = inscriptionRC != null ? inscriptionRC.getDateInscriptionVD() : null;

			/* On travaille selon le postulat que toute date d'inscription éloignée de plus d'un certain nombre de jour (seuil) de la date d'événement
			   indique que l'entreprise est pré-existante et qu'il n'y a donc pas de création à la date fournie. */
			final RegDate newnessThresholdDate = date.addDays( - EntrepriseHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);

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
				throw new EvenementEntrepriseException("Date d'inscription au RC CH introuvable alors qu'on a une date au RC VD!");
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

	protected static InformationDeDateEtDeCreation extraireInformationDeDateEtDeCreation(EvenementEntreprise event, EntrepriseCivile entrepriseCivile) throws EvenementEntrepriseException {
		final RegDate dateEvenement = event.getDateEvenement();

		EtablissementCivil etablissementPrincipal = entrepriseCivile.getEtablissementPrincipal(dateEvenement).getPayload();
		final Domicile siege = etablissementPrincipal.getDomicile(dateEvenement);
		final boolean isVaudoise = siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		final boolean inscritAuRC = entrepriseCivile.isInscriteAuRC(dateEvenement);
		final RegDate dateInscriptionRCVd;
		final RegDate dateInscriptionRC;
		final RegDate dateDeCreation;
		final RegDate dateOuvertureFiscale;
		final boolean isCreation;
		if (inscritAuRC) {
			dateInscriptionRCVd = etablissementPrincipal.getDateInscriptionRCVd(dateEvenement);
			if (isVaudoise && dateInscriptionRCVd == null) {
				throw new PasDeDateInscriptionRCVD();
			}
			dateInscriptionRC = etablissementPrincipal.getDateInscriptionRC(dateEvenement);
			isCreation = isCreation(event.getType(), entrepriseCivile, dateEvenement); // On ne peut pas l'appeler avant car on doit d'abord s'assurer que l'inscription RC VD existe si on est inscrit au RC et vaudois.
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
			isCreation = isCreation(event.getType(), entrepriseCivile, dateEvenement);
			dateDeCreation = dateEvenement;
			dateOuvertureFiscale = dateEvenement;
		}
		return new InformationDeDateEtDeCreation(dateDeCreation, dateOuvertureFiscale, isCreation);
	}

	protected static class PasDeDateInscriptionRCVD extends EvenementEntrepriseException {

		public PasDeDateInscriptionRCVD() {
			super("Date d'inscription au registre vaudois du commerce introuvable pour l'établissement principal vaudois.");
		}
	}

	protected static class InformationDeDateEtDeCreation {
		final RegDate dateDeCreation;
		final RegDate dateOuvertureFiscale;
		final boolean isCreation;

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

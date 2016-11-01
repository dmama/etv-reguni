package ch.vd.uniregctb.tiers.manager;

import java.util.Map;

/**
 * Les niveaux d'autorisation <b>en écriture</b> sur les diverses ressources gérées par Unireg pour un utilisateur et un contribuable donné.
 */
@SuppressWarnings("UnusedDeclaration")
public class Autorisations {

	/**
	 * Si <b>vrai</b>, l'édition des données fiscales est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des données fiscales est interdite.
	 */
	private final boolean donneesFiscales;
	private final boolean forsPrincipaux;
	private final boolean forsSecondaires;
	private final boolean forsAutresElementsImposables;
	private final boolean forsAutresImpots;
	private final boolean declarationImpots;
	private final boolean questionnairesSNC;
	private final boolean identificationEntreprise;
	private final boolean decisionsAci;

	private final boolean bouclements;
	private final boolean etatsPM;
	private final boolean regimesFiscaux;
	private final boolean allegementsFiscaux;
	private final boolean flagsPM;
	private final boolean autresDocumentsFiscaux;

	/**
	 * Si <b>vrai</b>, l'édition des adresses est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des adresses est interdite.
	 */
	private final boolean adresses;
	private final boolean adressesDomicile;
	private final boolean adressesCourrier;
	private final boolean adressesRepresentation;
	private final boolean adressesPoursuite;

	/**
	 * Si <b>vrai</b>, l'édition des compléments est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des complément est interdite.
	 */
	private final boolean complements;
	private final boolean complementsCommunications;
	private final boolean complementsCoordonneesFinancieres;

	/**
	 * Si <b>vrai</b>, l'édition des rapports-entre-tiers est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des rapports-entre-tiers est interdite.
	 */
	private final boolean rapports;
	private final boolean rapportsEtablissements;
	private final boolean rapportsDePrestations;
	private final boolean rapportsDeTravail;
	private final boolean autresRapports;

	private final boolean donneesCiviles;
	private final boolean debiteurs;
	private final boolean mouvements;
	private final boolean situationsFamille;
	private final boolean etiquettes;

	/**
	 * Si <b>vrai</b>, l'édition des mandats est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des mandats est interdite.
	 */
	private final boolean mandats;
	private final boolean mandatsGeneraux;
	private final boolean mandatsSpeciaux;
	private final boolean mandatsTiers;

	private final boolean remarques;

	public Autorisations() {
		this.donneesFiscales = false;
		this.forsPrincipaux = false;
		this.forsSecondaires = false;
		this.forsAutresElementsImposables = false;
		this.forsAutresImpots = false;
		this.declarationImpots = false;
		this.questionnairesSNC = false;
		this.identificationEntreprise = false;
		this.decisionsAci = false;

		this.bouclements = false;
		this.etatsPM = false;
		this.regimesFiscaux = false;
		this.allegementsFiscaux = false;
		this.flagsPM = false;
		this.autresDocumentsFiscaux = false;

		this.adresses = false;
		this.adressesDomicile = false;
		this.adressesCourrier = false;
		this.adressesRepresentation = false;
		this.adressesPoursuite = false;

		this.complements = false;
		this.complementsCommunications = false;
		this.complementsCoordonneesFinancieres = false;

		this.rapports = false;
		this.rapportsEtablissements = false;
		this.rapportsDePrestations = false;
		this.rapportsDeTravail = false;
		this.autresRapports = false;

		this.donneesCiviles = false;
		this.debiteurs = false;
		this.mouvements = false;
		this.situationsFamille = false;
		this.etiquettes = false;

		this.mandats = false;
		this.mandatsGeneraux = false;
		this.mandatsSpeciaux = false;
		this.mandatsTiers = false;

		this.remarques = false;
	}

	public Autorisations(Map<String, Boolean> map) {
		this.donneesFiscales = isAllowed(map, AutorisationManagerImpl.MODIF_FISCAL);
		this.forsPrincipaux = isAllowed(map, AutorisationManagerImpl.FISCAL_FOR_PRINC);
		this.forsSecondaires = isAllowed(map, AutorisationManagerImpl.FISCAL_FOR_SEC);
		this.forsAutresElementsImposables = isAllowed(map, AutorisationManagerImpl.FISCAL_FOR_AUTRE);
		this.forsAutresImpots = isAllowed(map, AutorisationManagerImpl.FISCAL_FOR_AUTRE);
		this.declarationImpots = isAllowed(map, AutorisationManagerImpl.MODIF_DI);
		this.questionnairesSNC = isAllowed(map, AutorisationManagerImpl.MODIF_QSNC);
		this.identificationEntreprise = isAllowed(map, AutorisationManagerImpl.MODIF_IDE);
		this.decisionsAci = isAllowed(map,AutorisationManagerImpl.FISCAL_DECISION_ACI);

		this.bouclements = isAllowed(map, AutorisationManagerImpl.MODIF_BOUCLEMENTS);
		this.etatsPM = isAllowed(map, AutorisationManagerImpl.MODIF_ETATS_PM);
		this.regimesFiscaux = isAllowed(map, AutorisationManagerImpl.MODIF_REGIMES_FISCAUX);
		this.allegementsFiscaux = isAllowed(map, AutorisationManagerImpl.MODIF_ALLEGEMENTS_FISCAUX);
		this.flagsPM = isAllowed(map, AutorisationManagerImpl.MODIF_FLAGS_PM);
		this.autresDocumentsFiscaux = isAllowed(map, AutorisationManagerImpl.MODIF_AUTRES_DOCS_FISCAUX);

		this.adresses = isAllowed(map, AutorisationManagerImpl.MODIF_ADRESSE);
		this.adressesDomicile = isAllowed(map, AutorisationManagerImpl.ADR_D);
		this.adressesCourrier = isAllowed(map, AutorisationManagerImpl.ADR_C);
		this.adressesRepresentation = isAllowed(map, AutorisationManagerImpl.ADR_B);
		this.adressesPoursuite = isAllowed(map, AutorisationManagerImpl.ADR_P);

		this.complements = isAllowed(map, AutorisationManagerImpl.MODIF_COMPLEMENT);
		this.complementsCommunications = isAllowed(map, AutorisationManagerImpl.COMPLEMENT_COMMUNICATION);
		this.complementsCoordonneesFinancieres = isAllowed(map, AutorisationManagerImpl.COMPLEMENT_COOR_FIN);

		this.rapports = isAllowed(map, AutorisationManagerImpl.MODIF_DOSSIER);
		this.rapportsEtablissements = isAllowed(map, AutorisationManagerImpl.MODIF_ETABLISSEMENT);
		this.rapportsDePrestations = isAllowed(map, AutorisationManagerImpl.MODIF_RAPPORT);
		this.rapportsDeTravail = isAllowed(map, AutorisationManagerImpl.DOSSIER_TRAVAIL);
		this.autresRapports = isAllowed(map, AutorisationManagerImpl.DOSSIER_NO_TRAVAIL);

		this.donneesCiviles = isAllowed(map, AutorisationManagerImpl.MODIF_CIVIL);
		this.debiteurs = isAllowed(map, AutorisationManagerImpl.MODIF_DEBITEUR);
		this.mouvements = isAllowed(map, AutorisationManagerImpl.MODIF_MOUVEMENT);
		this.situationsFamille = isAllowed(map, AutorisationManagerImpl.FISCAL_SIT_FAMILLLE);
		this.etiquettes = isAllowed(map, AutorisationManagerImpl.MODIF_ETIQUETTES);

		this.mandats = isAllowed(map, AutorisationManagerImpl.MODIF_MANDATS);
		this.mandatsGeneraux = isAllowed(map, AutorisationManagerImpl.MODIF_MANDATS_GENERAUX);
		this.mandatsSpeciaux = isAllowed(map, AutorisationManagerImpl.MODIF_MANDATS_SPECIAUX);
		this.mandatsTiers = isAllowed(map, AutorisationManagerImpl.MODIF_MANDATS_TIERS);

		this.remarques = isAllowed(map, AutorisationManagerImpl.MODIF_REMARQUES);
	}

	private static boolean isAllowed(Map<String, Boolean> map, String key) {
		final Boolean b = map.get(key);
		return b != null && b;
	}

	/**
	 * @return vrai si au moins une donnée est éditable; faux si ce n'est pas le cas
	 */
	public boolean isEditable() {
		return (donneesFiscales && (forsPrincipaux || forsSecondaires || forsAutresElementsImposables || decisionsAci || forsAutresImpots || identificationEntreprise))
				|| (adresses && (adressesDomicile || adressesCourrier || adressesRepresentation || adressesPoursuite))
				|| (complements && (complementsCommunications || complementsCoordonneesFinancieres))
				|| (rapports && (rapportsEtablissements || rapportsDePrestations || rapportsDeTravail || autresRapports))
				|| (mandats && (mandatsGeneraux || mandatsSpeciaux || mandatsTiers))
				|| declarationImpots || questionnairesSNC || bouclements || donneesCiviles || debiteurs || mouvements || situationsFamille || etiquettes
				|| etatsPM || regimesFiscaux || allegementsFiscaux || flagsPM || autresDocumentsFiscaux || remarques;
	}

	public boolean isDonneesFiscales() {
		return donneesFiscales;
	}

	public boolean isForsPrincipaux() {
		return forsPrincipaux && donneesFiscales;
	}

	public boolean isForsSecondaires() {
		return forsSecondaires && donneesFiscales;
	}

	public boolean isForsAutresElementsImposables() {
		return forsAutresElementsImposables && donneesFiscales;
	}

	public boolean isForsAutresImpots() {
		return forsAutresImpots && donneesFiscales;
	}

	public boolean isDeclarationImpots() {
		return declarationImpots;
	}

	public boolean isQuestionnairesSNC() {
		return questionnairesSNC;
	}

	public boolean isBouclements() {
		return bouclements;
	}

	public boolean isAdresses() {
		return adresses;
	}

	public boolean isAdressesDomicile() {
		return adressesDomicile && adresses;
	}

	public boolean isAdressesCourrier() {
		return adressesCourrier && adresses;
	}

	public boolean isAdressesRepresentation() {
		return adressesRepresentation && adresses;
	}

	public boolean isAdressesPoursuite() {
		return adressesPoursuite && adresses;
	}

	public boolean isComplements() {
		return complements;
	}

	public boolean isComplementsCommunications() {
		return complementsCommunications && complements;
	}

	public boolean isComplementsCoordonneesFinancieres() {
		return complementsCoordonneesFinancieres && complements;
	}

	public boolean isRapports() {
		return rapports;
	}

	public boolean isRapportsEtablissements() {
		return rapportsEtablissements;
	}

	public boolean isRapportsDePrestations() {
		return rapportsDePrestations && rapports;
	}

	public boolean isRapportsDeTravail() {
		return rapportsDeTravail && rapports;
	}

	public boolean isAutresRapports() {
		return autresRapports && rapports;
	}

	public boolean isDonneesCiviles() {
		return donneesCiviles;
	}

	public boolean isDebiteurs() {
		return debiteurs;
	}

	public boolean isMouvements() {
		return mouvements;
	}

	public boolean isSituationsFamille() {
		return situationsFamille;
	}

	public boolean isEtiquettes() {
		return etiquettes;
	}

	public boolean isIdentificationEntreprise(){
		return identificationEntreprise;
	}

	public boolean isDecisionsAci() {
		return decisionsAci;
	}

	public boolean isEtatsPM() {
		return etatsPM;
	}

	public boolean isRegimesFiscaux() {
		return regimesFiscaux;
	}

	public boolean isAllegementsFiscaux() {
		return allegementsFiscaux;
	}

	public boolean isFlagsPM() {
		return flagsPM;
	}

	public boolean isAutresDocumentsFiscaux() {
		return autresDocumentsFiscaux;
	}

	public boolean isMandats() {
		return mandats;
	}

	public boolean isMandatsGeneraux() {
		return mandatsGeneraux && mandats;
	}

	public boolean isMandatsSpeciaux() {
		return mandatsSpeciaux && mandats;
	}

	public boolean isMandatsTiers() {
		return mandatsTiers && mandats;
	}

	public boolean isRemarques() {
		return remarques;
	}

	@Override
	public String toString() {
		return "Autorisations{" +
				"donneesFiscales=" + donneesFiscales +
				", identificationEntreprise=" + identificationEntreprise +
				", forsPrincipaux=" + forsPrincipaux +
				", bouclements=" + bouclements +
				", etatsPM=" + etatsPM +
				", regimesFiscaux=" + regimesFiscaux +
				", allegementsFiscaux=" + allegementsFiscaux +
				", flagsPM=" + flagsPM +
				", autresDocumentsFiscaux=" + autresDocumentsFiscaux +
				", decisionsAci=" + decisionsAci +
				", forsSecondaires=" + forsSecondaires +
				", forsAutresElementsImposables=" + forsAutresElementsImposables +
				", forsAutresImpots=" + forsAutresImpots +
				", declarationImpots=" + declarationImpots +
				", questionnairesSNC=" + questionnairesSNC +
				", adresses=" + adresses +
				", adressesDomicile=" + adressesDomicile +
				", adressesCourrier=" + adressesCourrier +
				", adressesRepresentation=" + adressesRepresentation +
				", adressesPoursuite=" + adressesPoursuite +
				", complements=" + complements +
				", complementsCommunications=" + complementsCommunications +
				", complementsCoordonneesFinancieres=" + complementsCoordonneesFinancieres +
				", rapports=" + rapports +
				", rapportsEtablissements=" + rapportsEtablissements +
				", rapportsDePrestations=" + rapportsDePrestations +
				", rapportsDeTravail=" + rapportsDeTravail +
				", autresRapports=" + autresRapports +
				", donneesCiviles=" + donneesCiviles +
				", debiteurs=" + debiteurs +
				", mouvements=" + mouvements +
				", situationsFamille=" + situationsFamille +
				", remarques=" + remarques +
				'}';
	}
}

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
	private final boolean rapportsDePrestations;
	private final boolean rapportsDeTravail;
	private final boolean autresRapports;

	private final boolean donneesCiviles;
	private final boolean debiteurs;
	private final boolean mouvements;
	private final boolean situationsFamille;

	public Autorisations() {
		this.donneesFiscales = false;
		this.forsPrincipaux = false;
		this.forsSecondaires = false;
		this.forsAutresElementsImposables = false;
		this.forsAutresImpots = false;
		this.declarationImpots = false;

		this.adresses = false;
		this.adressesDomicile = false;
		this.adressesCourrier = false;
		this.adressesRepresentation = false;
		this.adressesPoursuite = false;

		this.complements = false;
		this.complementsCommunications = false;
		this.complementsCoordonneesFinancieres = false;

		this.rapports = false;
		this.rapportsDePrestations = false;
		this.rapportsDeTravail = false;
		this.autresRapports = false;

		this.donneesCiviles = false;
		this.debiteurs = false;
		this.mouvements = false;
		this.situationsFamille = false;
	}

	public Autorisations(Map<String, Boolean> map) {
		this.donneesFiscales = isAllowed(map, AutorisationManagerImpl.MODIF_FISCAL);
		this.forsPrincipaux = isAllowed(map, AutorisationManagerImpl.FISCAL_FOR_PRINC);
		this.forsSecondaires = isAllowed(map, AutorisationManagerImpl.FISCAL_FOR_SEC);
		this.forsAutresElementsImposables = isAllowed(map, AutorisationManagerImpl.FISCAL_FOR_AUTRE);
		this.forsAutresImpots = isAllowed(map, AutorisationManagerImpl.FISCAL_FOR_AUTRE);
		this.declarationImpots = isAllowed(map, AutorisationManagerImpl.MODIF_DI);

		this.adresses = isAllowed(map, AutorisationManagerImpl.MODIF_ADRESSE);
		this.adressesDomicile = isAllowed(map, AutorisationManagerImpl.ADR_D);
		this.adressesCourrier = isAllowed(map, AutorisationManagerImpl.ADR_C);
		this.adressesRepresentation = isAllowed(map, AutorisationManagerImpl.ADR_B);
		this.adressesPoursuite = isAllowed(map, AutorisationManagerImpl.ADR_P);

		this.complements = isAllowed(map, AutorisationManagerImpl.MODIF_COMPLEMENT);
		this.complementsCommunications = isAllowed(map, AutorisationManagerImpl.COMPLEMENT_COMMUNICATION);
		this.complementsCoordonneesFinancieres = isAllowed(map, AutorisationManagerImpl.COMPLEMENT_COOR_FIN);

		this.rapports = isAllowed(map, AutorisationManagerImpl.MODIF_DOSSIER);
		this.rapportsDePrestations = isAllowed(map, AutorisationManagerImpl.MODIF_RAPPORT);
		this.rapportsDeTravail = isAllowed(map, AutorisationManagerImpl.DOSSIER_TRAVAIL);
		this.autresRapports = isAllowed(map, AutorisationManagerImpl.DOSSIER_NO_TRAVAIL);

		this.donneesCiviles = isAllowed(map, AutorisationManagerImpl.MODIF_CIVIL);
		this.debiteurs = isAllowed(map, AutorisationManagerImpl.MODIF_DEBITEUR);
		this.mouvements = isAllowed(map, AutorisationManagerImpl.MODIF_MOUVEMENT);
		this.situationsFamille = isAllowed(map, AutorisationManagerImpl.FISCAL_SIT_FAMILLLE);
	}

	private static boolean isAllowed(Map<String, Boolean> map, String key) {
		final Boolean b = map.get(key);
		return b != null && b;
	}

	/**
	 * @return vrai si au moins une donnée est éditable; faux si ce n'est pas le cas
	 */
	public boolean isEditable() {
		return donneesFiscales || forsPrincipaux || forsSecondaires || forsAutresElementsImposables || forsAutresImpots || declarationImpots || adresses || adressesDomicile || adressesCourrier ||
				adressesRepresentation || adressesPoursuite || complements || complementsCommunications || complementsCoordonneesFinancieres || rapports || rapportsDePrestations ||
				rapportsDeTravail || autresRapports || donneesCiviles || debiteurs || mouvements || situationsFamille;
	}

	public boolean isDonneesFiscales() {
		return donneesFiscales;
	}

	public boolean isForsPrincipaux() {
		return forsPrincipaux;
	}

	public boolean isForsSecondaires() {
		return forsSecondaires;
	}

	public boolean isForsAutresElementsImposables() {
		return forsAutresElementsImposables;
	}

	public boolean isForsAutresImpots() {
		return forsAutresImpots;
	}

	public boolean isDeclarationImpots() {
		return declarationImpots;
	}

	public boolean isAdresses() {
		return adresses;
	}

	public boolean isAdressesDomicile() {
		return adressesDomicile;
	}

	public boolean isAdressesCourrier() {
		return adressesCourrier;
	}

	public boolean isAdressesRepresentation() {
		return adressesRepresentation;
	}

	public boolean isAdressesPoursuite() {
		return adressesPoursuite;
	}

	public boolean isComplements() {
		return complements;
	}

	public boolean isComplementsCommunications() {
		return complementsCommunications;
	}

	public boolean isComplementsCoordonneesFinancieres() {
		return complementsCoordonneesFinancieres;
	}

	public boolean isRapports() {
		return rapports;
	}

	public boolean isRapportsDePrestations() {
		return rapportsDePrestations;
	}

	public boolean isRapportsDeTravail() {
		return rapportsDeTravail;
	}

	public boolean isAutresRapports() {
		return autresRapports;
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

	@Override
	public String toString() {
		return "Autorisations{" +
				"donneesFiscales=" + donneesFiscales +
				", forsPrincipaux=" + forsPrincipaux +
				", forsSecondaires=" + forsSecondaires +
				", forsAutresElementsImposables=" + forsAutresElementsImposables +
				", forsAutresImpots=" + forsAutresImpots +
				", declarationImpots=" + declarationImpots +
				", adresses=" + adresses +
				", adressesDomicile=" + adressesDomicile +
				", adressesCourrier=" + adressesCourrier +
				", adressesRepresentation=" + adressesRepresentation +
				", adressesPoursuite=" + adressesPoursuite +
				", complements=" + complements +
				", complementsCommunications=" + complementsCommunications +
				", complementsCoordonneesFinancieres=" + complementsCoordonneesFinancieres +
				", rapports=" + rapports +
				", rapportsDePrestations=" + rapportsDePrestations +
				", rapportsDeTravail=" + rapportsDeTravail +
				", autresRapports=" + autresRapports +
				", donneesCiviles=" + donneesCiviles +
				", debiteurs=" + debiteurs +
				", mouvements=" + mouvements +
				", situationsFamille=" + situationsFamille +
				'}';
	}
}

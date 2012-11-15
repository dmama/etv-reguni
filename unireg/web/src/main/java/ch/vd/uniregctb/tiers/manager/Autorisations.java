package ch.vd.uniregctb.tiers.manager;

import java.util.Map;

import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;

/**
 * Les niveaux d'autorisation <b>en écriture</b> sur les diverses ressources gérées par Unireg pour un utilisateur et un contribuable donné.
 */
@SuppressWarnings("UnusedDeclaration")
public class Autorisations {

	/**
	 * Si <b>vrai</b>, l'édition des données fiscales est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des données fiscales est interdite.
	 */
	private boolean donneesFiscales;
	private boolean forsPrincipaux;
	private boolean forsSecondaires;
	private boolean forsAutresElementsImposables;
	private boolean forsAutresImpots;
	private boolean declarationImpots;

	/**
	 * Si <b>vrai</b>, l'édition des adresses est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des adresses est interdite.
	 */
	private boolean adresses;
	private boolean adressesDomicile;
	private boolean adressesCourrier;
	private boolean adressesRepresentation;
	private boolean adressesPoursuite;

	/**
	 * Si <b>vrai</b>, l'édition des compléments est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des complément est interdite.
	 */
	private boolean complements;
	private boolean complementsCommunications;
	private boolean complementsCoordonneesFinancieres;

	/**
	 * Si <b>vrai</b>, l'édition des rapports-entre-tiers est autorisée selon les détails des booléens qui suivent. Si <b>faux</b>, l'édition des rapports-entre-tiers est interdite.
	 */
	private boolean rapports;
	private boolean rapportsDePrestations;
	private boolean rapportsDeTravail;
	private boolean autresRapports;

	private boolean donneesCiviles;
	private boolean debiteurs;
	private boolean mouvements;
	private boolean situationsFamille;

	public Autorisations() {
	}

	public Autorisations(Map<String, Boolean> map) {
		this.donneesFiscales = isAllowed(map, TiersVisuView.MODIF_FISCAL);
		this.forsPrincipaux = isAllowed(map, TiersEditView.FISCAL_FOR_PRINC);
		this.forsSecondaires = isAllowed(map, TiersEditView.FISCAL_FOR_SEC);
		this.forsAutresElementsImposables = isAllowed(map, TiersEditView.FISCAL_FOR_AUTRE);
		this.forsAutresImpots = isAllowed(map, TiersEditView.FISCAL_FOR_AUTRE);
		this.declarationImpots = isAllowed(map, TiersVisuView.MODIF_DI);

		this.adresses = isAllowed(map, TiersVisuView.MODIF_ADRESSE);
		this.adressesDomicile = isAllowed(map, TiersEditView.ADR_D);
		this.adressesCourrier = isAllowed(map, TiersEditView.ADR_C);
		this.adressesRepresentation = isAllowed(map, TiersEditView.ADR_B);
		this.adressesPoursuite = isAllowed(map, TiersEditView.ADR_P);

		this.complements = isAllowed(map, TiersVisuView.MODIF_COMPLEMENT);
		this.complementsCommunications = isAllowed(map, TiersEditView.COMPLEMENT_COMMUNICATION);
		this.complementsCoordonneesFinancieres = isAllowed(map, TiersEditView.COMPLEMENT_COOR_FIN);

		this.rapports = isAllowed(map, TiersVisuView.MODIF_DOSSIER);
		this.rapportsDePrestations = isAllowed(map, TiersVisuView.MODIF_RAPPORT);
		this.rapportsDeTravail = isAllowed(map, TiersEditView.DOSSIER_TRAVAIL);
		this.autresRapports = isAllowed(map, TiersEditView.DOSSIER_NO_TRAVAIL);

		this.donneesCiviles = isAllowed(map, TiersVisuView.MODIF_CIVIL);
		this.debiteurs = isAllowed(map, TiersVisuView.MODIF_DEBITEUR);
		this.mouvements = isAllowed(map, TiersVisuView.MODIF_MOUVEMENT);
		this.situationsFamille = isAllowed(map, TiersEditView.FISCAL_SIT_FAMILLLE);
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
				adressesRepresentation || adressesPoursuite || complements || complementsCommunications || complementsCoordonneesFinancieres || rapports || rapportsDePrestations || rapportsDeTravail ||
				autresRapports || donneesCiviles || debiteurs || mouvements || situationsFamille;
	}

	public boolean isDonneesFiscales() {
		return donneesFiscales;
	}

	public void setDonneesFiscales(boolean donneesFiscales) {
		this.donneesFiscales = donneesFiscales;
	}

	public boolean isForsPrincipaux() {
		return forsPrincipaux;
	}

	public void setForsPrincipaux(boolean forsPrincipaux) {
		this.forsPrincipaux = forsPrincipaux;
	}

	public boolean isForsSecondaires() {
		return forsSecondaires;
	}

	public void setForsSecondaires(boolean forsSecondaires) {
		this.forsSecondaires = forsSecondaires;
	}

	public boolean isForsAutresElementsImposables() {
		return forsAutresElementsImposables;
	}

	public void setForsAutresElementsImposables(boolean forsAutresElementsImposables) {
		this.forsAutresElementsImposables = forsAutresElementsImposables;
	}

	public boolean isForsAutresImpots() {
		return forsAutresImpots;
	}

	public void setForsAutresImpots(boolean forsAutresImpots) {
		this.forsAutresImpots = forsAutresImpots;
	}

	public boolean isDeclarationImpots() {
		return declarationImpots;
	}

	public void setDeclarationImpots(boolean declarationImpots) {
		this.declarationImpots = declarationImpots;
	}

	public boolean isAdresses() {
		return adresses;
	}

	public void setAdresses(boolean adresses) {
		this.adresses = adresses;
	}

	public boolean isAdressesDomicile() {
		return adressesDomicile;
	}

	public void setAdressesDomicile(boolean adressesDomicile) {
		this.adressesDomicile = adressesDomicile;
	}

	public boolean isAdressesCourrier() {
		return adressesCourrier;
	}

	public void setAdressesCourrier(boolean adressesCourrier) {
		this.adressesCourrier = adressesCourrier;
	}

	public boolean isAdressesRepresentation() {
		return adressesRepresentation;
	}

	public void setAdressesRepresentation(boolean adressesRepresentation) {
		this.adressesRepresentation = adressesRepresentation;
	}

	public boolean isAdressesPoursuite() {
		return adressesPoursuite;
	}

	public void setAdressesPoursuite(boolean adressesPoursuite) {
		this.adressesPoursuite = adressesPoursuite;
	}

	public boolean isComplements() {
		return complements;
	}

	public void setComplements(boolean complements) {
		this.complements = complements;
	}

	public boolean isComplementsCommunications() {
		return complementsCommunications;
	}

	public void setComplementsCommunications(boolean complementsCommunications) {
		this.complementsCommunications = complementsCommunications;
	}

	public boolean isComplementsCoordonneesFinancieres() {
		return complementsCoordonneesFinancieres;
	}

	public void setComplementsCoordonneesFinancieres(boolean complementsCoordonneesFinancieres) {
		this.complementsCoordonneesFinancieres = complementsCoordonneesFinancieres;
	}

	public boolean isRapports() {
		return rapports;
	}

	public void setRapports(boolean rapports) {
		this.rapports = rapports;
	}

	public boolean isRapportsDePrestations() {
		return rapportsDePrestations;
	}

	public void setRapportsDePrestations(boolean rapportsDePrestations) {
		this.rapportsDePrestations = rapportsDePrestations;
	}

	public boolean isRapportsDeTravail() {
		return rapportsDeTravail;
	}

	public void setRapportsDeTravail(boolean rapportsDeTravail) {
		this.rapportsDeTravail = rapportsDeTravail;
	}

	public boolean isAutresRapports() {
		return autresRapports;
	}

	public void setAutresRapports(boolean autresRapports) {
		this.autresRapports = autresRapports;
	}

	public boolean isDonneesCiviles() {
		return donneesCiviles;
	}

	public void setDonneesCiviles(boolean donneesCiviles) {
		this.donneesCiviles = donneesCiviles;
	}

	public boolean isDebiteurs() {
		return debiteurs;
	}

	public void setDebiteurs(boolean debiteurs) {
		this.debiteurs = debiteurs;
	}

	public boolean isMouvements() {
		return mouvements;
	}

	public void setMouvements(boolean mouvements) {
		this.mouvements = mouvements;
	}

	public boolean isSituationsFamille() {
		return situationsFamille;
	}

	public void setSituationsFamille(boolean situationsFamille) {
		this.situationsFamille = situationsFamille;
	}
}

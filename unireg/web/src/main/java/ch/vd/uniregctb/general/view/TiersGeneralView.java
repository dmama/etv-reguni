package ch.vd.uniregctb.general.view;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

/**
 * TiersGeneralView
 *
 * @author xcifde
 *
 */
public class TiersGeneralView {

	public enum TypeTiers {
		HOMME,
		FEMME,
		SEXE_INCONNU,
		/** Ménage commun mixte */
		MC_MIXTE,
		/** Ménage commun avec homme seul (marié seul ou sexe conjoint inconnu) */
		MC_HOMME_SEUL,
		/** Ménage commun avec femme seule (mariée seule ou sexe conjoint inconnu) */
		MC_FEMME_SEULE,
		/** Ménage commun avec deux hommes (pacs) */
		MC_HOMME_HOMME,
		/** Ménage commun avec deux femmes (pacs) */
		MC_FEMME_FEMME,
		/** Ménage commun sans information de sexe */
		MC_SEXE_INCONNU,
		ENTREPRISE,
		ETABLISSEMENT,
		AUTRE_COMM,
		COLLECT_ADMIN,
		DEBITEUR
	}

	public TiersGeneralView() {
	}

	public TiersGeneralView(Long numero) {
		this.numero = numero;
	}

	private RoleView role;

	private Long numero;

	private TypeTiers type;

	private AdresseEnvoiDetaillee adresseEnvoi;

	private Exception adresseEnvoiException;

	private CategorieImpotSource categorie;

	private ModeCommunication modeCommunication;

	private PeriodiciteDecompte periodicite;

	private PeriodeDecompte periode;

	private String personneContact;

	private String numeroTelephone;

	private RegDate dateNaissance;

	private String numeroAssureSocial;

	private String ancienNumeroAVS;

	private String natureTiers;

	private String nomCommuneGestion;

	private boolean annule;

	private Date annulationDate;

	private ValidationResults validationResults;

	public RoleView getRole() {
		return role;
	}

	public void setRole(RoleView role) {
		this.role = role;
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public TypeTiers getType() {
		return type;
	}

	public void setType(TypeTiers type) {
		this.type = type;
	}

	public AdresseEnvoiDetaillee getAdresseEnvoi() {
		return adresseEnvoi;
	}

	public void setAdresseEnvoi(AdresseEnvoiDetaillee adresseEnvoi) {
		this.adresseEnvoi = adresseEnvoi;
	}

	public CategorieImpotSource getCategorie() {
		return categorie;
	}

	public void setCategorie(CategorieImpotSource categorie) {
		this.categorie = categorie;
	}

	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	public void setModeCommunication(ModeCommunication modeCommunication) {
		this.modeCommunication = modeCommunication;
	}

	public PeriodiciteDecompte getPeriodicite() {
		return periodicite;
	}

	public void setPeriodicite(PeriodiciteDecompte periodicite) {
		this.periodicite = periodicite;
	}

	public PeriodeDecompte getPeriode() {
		return periode;
	}

	public void setPeriode(PeriodeDecompte periode) {
		this.periode = periode;
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	public String getNumeroTelephone() {
		return numeroTelephone;
	}

	public void setNumeroTelephone(String numeroTelephone) {
		this.numeroTelephone = numeroTelephone;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
	}

	public String getAncienNumeroAVS() {
		return ancienNumeroAVS;
	}

	public void setAncienNumeroAVS(String ancienNumeroAVS) {
		this.ancienNumeroAVS = ancienNumeroAVS;
	}

	public String getNatureTiers() {
		return natureTiers;
	}

	public void setNatureTiers(String natureTiers) {
		this.natureTiers = natureTiers;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public Exception getAdresseEnvoiException() {
		return adresseEnvoiException;
	}

	public void setAdresseEnvoiException(Exception adresseEnvoiException) {
		this.adresseEnvoiException = adresseEnvoiException;
	}

	public ValidationResults getValidationResults() {
		return validationResults;
	}

	public void setValidationResults(ValidationResults validationResults) {
		this.validationResults = validationResults;
	}

	public String getNomCommuneGestion() {
		return nomCommuneGestion;
	}

	public void setNomCommuneGestion(String nomCommuneGestion) {
		this.nomCommuneGestion = nomCommuneGestion;
	}

	/**
	 * Méthode nécessaire pour éviter l'utilisation de séparateurs des miliers lors
	 * d'un "bind" spring (voir vers.jsp)
	 * @return la représentation en base 10 du numéro de contribuable, sans aucun séparateur
	 */
	public String getNumeroAsString() {
		return numero == null ? null : numero.toString();
	}

	public Date getAnnulationDate() {
		return annulationDate;
	}

	public void setAnnulationDate(Date annulationDate) {
		this.annulationDate = annulationDate;
	}
}

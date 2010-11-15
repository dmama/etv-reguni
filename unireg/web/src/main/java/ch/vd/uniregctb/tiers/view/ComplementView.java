package ch.vd.uniregctb.tiers.view;

/**
 * Form backing-object de l'onglet "complément" associé à un tiers.
 */
public class ComplementView {

	private String personneContact;
	private String complementNom;
	private String numeroTelephonePrive;
	private String numeroTelephonePortable;
	private String numeroTelephoneProfessionnel;
	private String numeroTelecopie;
	private String adresseCourrierElectronique;

	private String numeroCompteBancaire;
	private String nomInstitutionCompteBancaire;
	private String ibanValidationMessage;
	private String titulaireCompteBancaire;
	private String adresseBicSwift;
	private Boolean blocageRemboursementAutomatique;

	private Long ancienNumeroSourcier;

	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	public String getComplementNom() {
		return complementNom;
	}

	public void setComplementNom(String complementNom) {
		this.complementNom = complementNom;
	}

	public String getNumeroTelephonePrive() {
		return numeroTelephonePrive;
	}

	public void setNumeroTelephonePrive(String numeroTelephonePrive) {
		this.numeroTelephonePrive = numeroTelephonePrive;
	}

	public String getNumeroTelephonePortable() {
		return numeroTelephonePortable;
	}

	public void setNumeroTelephonePortable(String numeroTelephonePortable) {
		this.numeroTelephonePortable = numeroTelephonePortable;
	}

	public String getNumeroTelephoneProfessionnel() {
		return numeroTelephoneProfessionnel;
	}

	public void setNumeroTelephoneProfessionnel(String numeroTelephoneProfessionnel) {
		this.numeroTelephoneProfessionnel = numeroTelephoneProfessionnel;
	}

	public String getNumeroTelecopie() {
		return numeroTelecopie;
	}

	public void setNumeroTelecopie(String numeroTelecopie) {
		this.numeroTelecopie = numeroTelecopie;
	}

	public String getAdresseCourrierElectronique() {
		return adresseCourrierElectronique;
	}

	public void setAdresseCourrierElectronique(String adresseCourrierElectronique) {
		this.adresseCourrierElectronique = adresseCourrierElectronique;
	}

	public String getNumeroCompteBancaire() {
		return numeroCompteBancaire;
	}

	public void setNumeroCompteBancaire(String numeroCompteBancaire) {
		this.numeroCompteBancaire = numeroCompteBancaire;
	}

	public String getNomInstitutionCompteBancaire() {
		return nomInstitutionCompteBancaire;
	}

	public void setNomInstitutionCompteBancaire(String nomInstitutionCompteBancaire) {
		this.nomInstitutionCompteBancaire = nomInstitutionCompteBancaire;
	}

	public String getIbanValidationMessage() {
		return ibanValidationMessage;
	}

	public void setIbanValidationMessage(String ibanValidationMessage) {
		this.ibanValidationMessage = ibanValidationMessage;
	}

	public String getTitulaireCompteBancaire() {
		return titulaireCompteBancaire;
	}

	public void setTitulaireCompteBancaire(String titulaireCompteBancaire) {
		this.titulaireCompteBancaire = titulaireCompteBancaire;
	}

	public String getAdresseBicSwift() {
		return adresseBicSwift;
	}

	public void setAdresseBicSwift(String adresseBicSwift) {
		this.adresseBicSwift = adresseBicSwift;
	}

	public Boolean getBlocageRemboursementAutomatique() {
		return blocageRemboursementAutomatique;
	}

	public void setBlocageRemboursementAutomatique(Boolean blocageRemboursementAutomatique) {
		this.blocageRemboursementAutomatique = blocageRemboursementAutomatique;
	}

	public Long getAncienNumeroSourcier() {
		return ancienNumeroSourcier;
	}

	public void setAncienNumeroSourcier(Long ancienNumeroSourcier) {
		this.ancienNumeroSourcier = ancienNumeroSourcier;
	}
}

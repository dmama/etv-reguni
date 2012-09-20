package ch.vd.uniregctb.tiers.view;

public class CompteBancaireView {
	private Long numeroTiersTitulaire;
	private String titulaireCompteBancaire;
	private String numeroCCP;
	private String numeroCompteBancaire;
	private String nomInstitutionCompteBancaire;
	private String iban;
	private String ibanValidationMessage;
	private String adresseBicSwift;

	public CompteBancaireView() {
	}

	public CompteBancaireView(Long numeroTiersTitulaire, String titulaireCompte, String numeroCCP, String numeroCompteBancaire, String nomInstitutionCompteBancaire, String iban,
	                          String ibanValidationMessage, String adresseBicSwift) {
		this.numeroTiersTitulaire = numeroTiersTitulaire;
		this.titulaireCompteBancaire = titulaireCompte;
		this.numeroCCP = numeroCCP;
		this.numeroCompteBancaire = numeroCompteBancaire;
		this.nomInstitutionCompteBancaire = nomInstitutionCompteBancaire;
		this.iban = iban;
		this.ibanValidationMessage = ibanValidationMessage;
		this.adresseBicSwift = adresseBicSwift;
	}

	public Long getNumeroTiersTitulaire() {
		return numeroTiersTitulaire;
	}

	public void setNumeroTiersTitulaire(Long numeroTiersTitulaire) {
		this.numeroTiersTitulaire = numeroTiersTitulaire;
	}

	public String getTitulaireCompteBancaire() {
		return titulaireCompteBancaire;
	}

	public void setTitulaireCompteBancaire(String titulaireCompteBancaire) {
		this.titulaireCompteBancaire = titulaireCompteBancaire;
	}

	public String getNumeroCCP() {
		return numeroCCP;
	}

	public void setNumeroCCP(String numeroCCP) {
		this.numeroCCP = numeroCCP;
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

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public String getIbanValidationMessage() {
		return ibanValidationMessage;
	}

	public void setIbanValidationMessage(String ibanValidationMessage) {
		this.ibanValidationMessage = ibanValidationMessage;
	}

	public String getAdresseBicSwift() {
		return adresseBicSwift;
	}

	public void setAdresseBicSwift(String adresseBicSwift) {
		this.adresseBicSwift = adresseBicSwift;
	}
}

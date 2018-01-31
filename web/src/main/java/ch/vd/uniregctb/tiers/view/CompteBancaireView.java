package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.iban.IbanHelper;

@SuppressWarnings("UnusedDeclaration")
public class CompteBancaireView implements DateRange {
	private RegDate dateDebut;
	private RegDate dateFin;
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

	public CompteBancaireView(RegDate dateDebut, RegDate dateFin, Long numeroTiersTitulaire, String titulaireCompte, String numeroCCP, String numeroCompteBancaire, String nomInstitutionCompteBancaire,
	                          String iban, String ibanValidationMessage, String adresseBicSwift) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.numeroTiersTitulaire = numeroTiersTitulaire;
		this.titulaireCompteBancaire = titulaireCompte;
		this.numeroCCP = numeroCCP;
		this.numeroCompteBancaire = numeroCompteBancaire;
		this.nomInstitutionCompteBancaire = nomInstitutionCompteBancaire;
		this.iban = IbanHelper.normalize(iban);
		this.ibanValidationMessage = ibanValidationMessage;
		this.adresseBicSwift = adresseBicSwift;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
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
		return IbanHelper.toDisplayString(iban);
	}

	public void setIban(String iban) {
		this.iban = IbanHelper.normalize(iban);
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

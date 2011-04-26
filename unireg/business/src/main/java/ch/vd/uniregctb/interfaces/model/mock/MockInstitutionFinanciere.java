package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;

public class MockInstitutionFinanciere implements InstitutionFinanciere {

	private Integer code;
	private String adresse1;
	private String adresse2;
	private String adresse3;
	private String noClearing;
	private String noCompte;
	private String noIdentificationDTA;
	private String nomInstitutionFinanciere;

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getAdresse1() {
		return adresse1;
	}

	public void setAdresse1(String adresse1) {
		this.adresse1 = adresse1;
	}

	public String getAdresse2() {
		return adresse2;
	}

	public void setAdresse2(String adresse2) {
		this.adresse2 = adresse2;
	}

	public String getAdresse3() {
		return adresse3;
	}

	public void setAdresse3(String adresse3) {
		this.adresse3 = adresse3;
	}

	public String getNoClearing() {
		return noClearing;
	}

	public void setNoClearing(String noClearing) {
		this.noClearing = noClearing;
	}

	public String getNoCompte() {
		return noCompte;
	}

	public void setNoCompte(String noCompte) {
		this.noCompte = noCompte;
	}

	public String getNoIdentificationDTA() {
		return noIdentificationDTA;
	}

	public void setNoIdentificationDTA(String noIdentificationDTA) {
		this.noIdentificationDTA = noIdentificationDTA;
	}

	public String getNomInstitutionFinanciere() {
		return nomInstitutionFinanciere;
	}

	public void setNomInstitutionFinanciere(String nomInstitutionFinanciere) {
		this.nomInstitutionFinanciere = nomInstitutionFinanciere;
	}
}

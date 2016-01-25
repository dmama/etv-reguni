package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;

public class MockInstitutionFinanciere implements InstitutionFinanciere {

	public static final MockInstitutionFinanciere Postfinance = new MockInstitutionFinanciere(6081, null, "Postfach", "8759 Netstal", "9110", null, null, "Postfinance");

	private Integer code;
	private String adresse1;
	private String adresse2;
	private String adresse3;
	private String noClearing;
	private String noCompte;
	private String noIdentificationDTA;
	private String nomInstitutionFinanciere;

	public MockInstitutionFinanciere() {
	}

	public MockInstitutionFinanciere(Integer code, String adresse1, String adresse2, String adresse3, String noClearing, String noCompte, String noIdentificationDTA, String nomInstitutionFinanciere) {
		this.code = code;
		this.adresse1 = adresse1;
		this.adresse2 = adresse2;
		this.adresse3 = adresse3;
		this.noClearing = noClearing;
		this.noCompte = noCompte;
		this.noIdentificationDTA = noIdentificationDTA;
		this.nomInstitutionFinanciere = nomInstitutionFinanciere;
		DefaultMockServiceInfrastructureService.addInstitutionFinanciere(this);
	}

	@Override
	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	@Override
	public String getAdresse1() {
		return adresse1;
	}

	public void setAdresse1(String adresse1) {
		this.adresse1 = adresse1;
	}

	@Override
	public String getAdresse2() {
		return adresse2;
	}

	public void setAdresse2(String adresse2) {
		this.adresse2 = adresse2;
	}

	@Override
	public String getAdresse3() {
		return adresse3;
	}

	public void setAdresse3(String adresse3) {
		this.adresse3 = adresse3;
	}

	@Override
	public String getNoClearing() {
		return noClearing;
	}

	public void setNoClearing(String noClearing) {
		this.noClearing = noClearing;
	}

	@Override
	public String getNoCompte() {
		return noCompte;
	}

	public void setNoCompte(String noCompte) {
		this.noCompte = noCompte;
	}

	@Override
	public String getNoIdentificationDTA() {
		return noIdentificationDTA;
	}

	public void setNoIdentificationDTA(String noIdentificationDTA) {
		this.noIdentificationDTA = noIdentificationDTA;
	}

	@Override
	public String getNomInstitutionFinanciere() {
		return nomInstitutionFinanciere;
	}

	public void setNomInstitutionFinanciere(String nomInstitutionFinanciere) {
		this.nomInstitutionFinanciere = nomInstitutionFinanciere;
	}
}

package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;

public class InstitutionFinanciereWrapper implements InstitutionFinanciere, Serializable {

	private static final long serialVersionUID = 255343809870190510L;
	
	private final String adresse1;
	private final String adresse2;
	private final String adresse3;
	private final Integer code;
	private final String noClearing;
	private final String noCompte;
	private final String noIdentificationDTA;
	private final String nomInstitutionFinanciere;

	public InstitutionFinanciereWrapper(ch.vd.registre.common.model.InstitutionFinanciere target) {
		this.adresse1 = target.getAdresse1();
		this.adresse2 = target.getAdresse2();
		this.adresse3 = target.getAdresse3();
		this.code = target.getCode();
		this.noClearing = target.getNoClearing();
		this.noCompte = target.getNoCompte();
		this.noIdentificationDTA = target.getNoIdentificationDTA();
		this.nomInstitutionFinanciere = target.getNomInstitutionFinanciere();
	}

	public static InstitutionFinanciere get(ch.vd.registre.common.model.InstitutionFinanciere institutionFinanciere) {
		if (institutionFinanciere == null) {
			return null;
		}
		return new InstitutionFinanciereWrapper(institutionFinanciere);
	}

	public String getAdresse1() {
		return adresse1;
	}

	public String getAdresse2() {
		return adresse2;
	}

	public String getAdresse3() {
		return adresse3;
	}

	public Integer getCode() {
		return code;
	}

	public String getNoClearing() {
		return noClearing;
	}

	public String getNoCompte() {
		return noCompte;
	}

	public String getNoIdentificationDTA() {
		return noIdentificationDTA;
	}

	public String getNomInstitutionFinanciere() {
		return nomInstitutionFinanciere;
	}
}

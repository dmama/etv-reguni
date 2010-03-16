package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;

public class InstitutionFinanciereWrapper implements InstitutionFinanciere {

	private final ch.vd.registre.common.model.InstitutionFinanciere target;

	public InstitutionFinanciereWrapper(ch.vd.registre.common.model.InstitutionFinanciere target) {
		this.target = target;
	}

	public static InstitutionFinanciere get(ch.vd.registre.common.model.InstitutionFinanciere institutionFinanciere) {
		if (institutionFinanciere == null) {
			return null;
		}
		return new InstitutionFinanciereWrapper(institutionFinanciere);
	}

	public String getAdresse1() {
		return target.getAdresse1();
	}

	public String getAdresse2() {
		return target.getAdresse2();
	}

	public String getAdresse3() {
		return target.getAdresse3();
	}

	public Integer getCode() {
		return target.getCode();
	}

	public String getNoClearing() {
		return target.getNoClearing();
	}

	public String getNoCompte() {
		return target.getNoCompte();
	}

	public String getNoIdentificationDTA() {
		return target.getNoIdentificationDTA();
	}

	public String getNomInstitutionFinanciere() {
		return target.getNomInstitutionFinanciere();
	}
}

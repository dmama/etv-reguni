package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public class InstitutionFinanciereImpl implements InstitutionFinanciere, Serializable {

	private static final long serialVersionUID = 255343809870190510L;
	
	private final String adresse1;
	private final String adresse2;
	private final String adresse3;
	private final Integer code;
	private final String noClearing;
	private final String noCompte;
	private final String noIdentificationDTA;
	private final String nomInstitutionFinanciere;

	public InstitutionFinanciereImpl(ch.vd.registre.common.model.InstitutionFinanciere target) {
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
		return new InstitutionFinanciereImpl(institutionFinanciere);
	}

	@Override
	public String getAdresse1() {
		return adresse1;
	}

	@Override
	public String getAdresse2() {
		return adresse2;
	}

	@Override
	public String getAdresse3() {
		return adresse3;
	}

	@Override
	public Integer getCode() {
		return code;
	}

	@Override
	public String getNoClearing() {
		return noClearing;
	}

	@Override
	public String getNoCompte() {
		return noCompte;
	}

	@Override
	public String getNoIdentificationDTA() {
		return noIdentificationDTA;
	}

	@Override
	public String getNomInstitutionFinanciere() {
		return nomInstitutionFinanciere;
	}
}

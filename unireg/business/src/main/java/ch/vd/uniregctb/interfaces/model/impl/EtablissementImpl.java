package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Etablissement;

public class EtablissementImpl implements Etablissement {

	private final RegDate dateDebut;
	private final RegDate dateFin;

	private final ch.vd.registre.pm.model.Etablissement target;

	public static EtablissementImpl get(ch.vd.registre.pm.model.Etablissement target) {
		if (target == null) {
			return null;
		}
		return new EtablissementImpl(target);
	}

	private EtablissementImpl(ch.vd.registre.pm.model.Etablissement target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
	}

	@Override
	public long getNoEtablissement() {
		return target.getNoEtablissement();
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String getRaisonSociale1() {
		return target.getRaisonSociale1();
	}

	@Override
	public String getRaisonSociale2() {
		return target.getRaisonSociale2();
	}

	@Override
	public String getRaisonSociale3() {
		return target.getRaisonSociale3();
	}

	@Override
	public String getEnseigne() {
		return target.getEnseigne();
	}

	@Override
	public String getChez() {
		return target.getChez();
	}

	@Override
	public String getRue() {
		return target.getRue();
	}

	@Override
	public String getNoTelephone() {
		return target.getNoTelephone();
	}

	@Override
	public String getNoFax() {
		return target.getNoFax();
	}

	@Override
	public String getNoPolice() {
		return target.getNoPolice();
	}

	@Override
	public String getCCP() {
		return target.getCCP();
	}

	@Override
	public String getCompteBancaire() {
		return target.getCompteBancaire();
	}

	@Override
	public String getIBAN() {
		return target.getIBAN();
	}

	@Override
	public String getBicSwift() {
		return target.getBicSwift();
	}

	@Override
	public String getNomInstitutionFinanciere() {
		return target.getNomInstitutionFinanciere();
	}

	@Override
	public Long getNoOrdreLocalitePostale() {
		return target.getNoOrdreLocalitePostale();
	}

	@Override
	public Long getNoRue() {
		return target.getNoRue();
	}
}

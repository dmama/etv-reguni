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

	public long getNoEtablissement() {
		return target.getNoEtablissement();
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public String getRaisonSociale1() {
		return target.getRaisonSociale1();
	}

	public String getRaisonSociale2() {
		return target.getRaisonSociale2();
	}

	public String getRaisonSociale3() {
		return target.getRaisonSociale3();
	}

	public String getEnseigne() {
		return target.getEnseigne();
	}

	public String getChez() {
		return target.getChez();
	}

	public String getRue() {
		return target.getRue();
	}

	public String getNoTelephone() {
		return target.getNoTelephone();
	}

	public String getNoFax() {
		return target.getNoFax();
	}

	public String getNoPolice() {
		return target.getNoPolice();
	}

	public String getCCP() {
		return target.getCCP();
	}

	public String getCompteBancaire() {
		return target.getCompteBancaire();
	}

	public String getIBAN() {
		return target.getIBAN();
	}

	public String getBicSwift() {
		return target.getBicSwift();
	}

	public String getNomInstitutionFinanciere() {
		return target.getNomInstitutionFinanciere();
	}

	public Long getNoOrdreLocalitePostale() {
		return target.getNoOrdreLocalitePostale();
	}

	public Long getNoRue() {
		return target.getNoRue();
	}
}

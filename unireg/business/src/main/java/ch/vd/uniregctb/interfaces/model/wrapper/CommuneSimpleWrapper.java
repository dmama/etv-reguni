package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.infrastructure.model.EnumCanton;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;

public class CommuneSimpleWrapper extends EntiteOFSWrapper implements CommuneSimple {

	private final ch.vd.infrastructure.model.CommuneSimple target;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public static CommuneSimpleWrapper get(ch.vd.infrastructure.model.CommuneSimple target) {
		if (target == null) {
			return null;
		}
		return new CommuneSimpleWrapper(target);
	}

	private CommuneSimpleWrapper(ch.vd.infrastructure.model.CommuneSimple target) {
		super(target);
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getNoACI() {
		return target.getNoACI();
	}

	public String getNoCantonal() {
		return target.getNoCantonal();
	}

	public int getNoOFSEtendu() {
		if (isFraction()) {
			return target.getNoTechnique();
		}
		else {
			return target.getNoOFS();
		}
	}

	@Override
	public int getNoOFS() {
		return getNoOFSEtendu();
	}

	public String getNomAbrege() {
		return target.getNomAbrege();
	}

	public int getNumTechMere() {
		return target.getNumTechMere();
	}

	public String getSigleCanton() {
		return target.getSigleCanton();
	}

	public boolean isVaudoise() {
		return EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
	}

	public boolean isFraction() {
		return target.isFraction();
	}

	public boolean isPrincipale() {
		return target.isPrincipale();
	}

	public boolean isValide() {
		return target.isValide();
	}
}

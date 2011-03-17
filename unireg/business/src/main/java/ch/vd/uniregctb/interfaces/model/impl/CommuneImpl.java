package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.infrastructure.model.EnumCanton;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;

public class CommuneImpl extends EntiteOFSImpl implements Commune, Serializable {

	private static final long serialVersionUID = 8537916537832562224L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String noACI;
	private final String noCantonal;
	private final int noOFSEtendu;
	private final String nomAbrege;
	private final int numTechMere;
	private final boolean vaudoise;
	private final boolean fraction;
	private final boolean principale;
	private final boolean valide;
	private final String sigleCanton;

	public static CommuneImpl get(ch.vd.infrastructure.model.Commune target) {
		if (target == null) {
			return null;
		}
		return new CommuneImpl(target);
	}

	public static CommuneImpl get(ch.vd.infrastructure.model.CommuneSimple target) {
		if (target == null) {
			return null;
		}
		return new CommuneImpl(target);
	}

	protected CommuneImpl(ch.vd.infrastructure.model.CommuneSimple target) {
		super(target);
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noACI = target.getNoACI();
		this.noCantonal = target.getNoCantonal();
		this.noOFSEtendu = (target.isFraction() ? target.getNoTechnique() : target.getNoOFS());
		this.nomAbrege = target.getNomAbrege();
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getNumTechMere();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.valide = target.isValide();
	}

	protected CommuneImpl(ch.vd.infrastructure.model.Commune target) {
		super(target);
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noACI = target.getNoACI();
		this.noCantonal = target.getNoCantonal();
		this.noOFSEtendu = (target.isFraction() ? target.getNoTechnique() : target.getNoOFS());
		this.nomAbrege = target.getNomAbrege();
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getNumTechMere();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.valide = target.isValide();
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getNoACI() {
		return noACI;
	}

	public String getNoCantonal() {
		return noCantonal;
	}

	public int getNoOFSEtendu() {
		return noOFSEtendu;
	}

	@Override
	public int getNoOFS() {
		return getNoOFSEtendu();
	}

	public String getNomAbrege() {
		return nomAbrege;
	}

	public int getNumTechMere() {
		return numTechMere;
	}

	public String getSigleCanton() {
		return sigleCanton;
	}

	public boolean isVaudoise() {
		return vaudoise;
	}

	public boolean isFraction() {
		return fraction;
	}

	public boolean isPrincipale() {
		return principale;
	}

	public boolean isValide() {
		return valide;
	}
}

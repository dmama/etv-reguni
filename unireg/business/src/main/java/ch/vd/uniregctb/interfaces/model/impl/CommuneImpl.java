package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorDate;
import ch.vd.infrastructure.model.EnumCanton;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;

public class CommuneImpl extends EntiteOFSImpl implements Commune, Serializable {

	private static final long serialVersionUID = 8537916537832562224L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOFSEtendu;
	private final int numTechMere;
	private final boolean vaudoise;
	private final boolean fraction;
	private final boolean principale;
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

	public static Commune get(CommuneFiscale target) {
		if (target == null) {
			return null;
		}
		return new CommuneImpl(target);
	}

	protected CommuneImpl(ch.vd.infrastructure.model.CommuneSimple target) {
		super(target);
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noOFSEtendu = (target.isFraction() ? target.getNoTechnique() : target.getNoOFS());
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getNumTechMere();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
	}

	protected CommuneImpl(ch.vd.infrastructure.model.Commune target) {
		super(target);
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noOFSEtendu = (target.isFraction() ? target.getNoTechnique() : target.getNoOFS());
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getNumTechMere();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
	}

	protected CommuneImpl(CommuneFiscale target) {
		super(target.getNoOfs(), toUpperCase(target.getNomAbrege()), target.getNomAbrege(), null);
		this.dateDebut = fidor2reg(target.getDateDebutValidite());
		this.dateFin = fidor2reg(target.getDateFinValidite());
		this.noOFSEtendu = (target.getParentOfsId() == null ? target.getNoOfs() : target.getNoTechnique());
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getParentOfsId() == null ? 0 : target.getParentOfsId();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
		this.fraction = (target.getParentOfsId() != null);
		this.principale = (target.getFractions() != null && !target.getFractions().isEmpty());
	}

	private static String toUpperCase(String string) {
		return string == null ? null : string.toUpperCase();
	}

	private static RegDate fidor2reg(FidorDate date) {
		return date == null ? null : RegDate.get(date.getYear(), date.getMonth(), date.getDay());
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public int getNoOFSEtendu() {
		return noOFSEtendu;
	}

	@Override
	public int getNoOFS() {
		return getNoOFSEtendu();
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
}

package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorDate;
import ch.vd.infrastructure.model.EnumCanton;
import ch.vd.registre.base.date.RegDate;

public class CommuneImpl extends EntiteOFSImpl implements Commune, Serializable {

	private static final long serialVersionUID = 4126668258450436037L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOFSEtendu;
	private final int numTechMere;
	private final boolean vaudoise;
	private final boolean fraction;
	private final boolean principale;
	private final String sigleCanton;
	private final int numTechnique;
	private final District district;

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
		this.numTechnique = target.getNoTechnique();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(sigleCanton);
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.district = null;
	}

	protected CommuneImpl(ch.vd.infrastructure.model.Commune target) {
		super(target);
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noOFSEtendu = (target.isFraction() ? target.getNoTechnique() : target.getNoOFS());
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getNumTechMere();
		this.numTechnique = target.getNoTechnique();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(sigleCanton);
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.district = null;
	}

	protected CommuneImpl(CommuneFiscale target) {
		super(target.getNoOfs(), toUpperCase(target.getNomOfficiel()), target.getNomOfficiel(), null);
		this.dateDebut = fidor2reg(target.getDateDebutValidite());
		this.dateFin = fidor2reg(target.getDateFinValidite());
		this.noOFSEtendu = (target.getNoOFSFaitiere() == null ? target.getNoOfs() : target.getNoTechnique());
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getNoOFSFaitiere() == null ? 0 : target.getNoOFSFaitiere();
		this.numTechnique = target.getNoTechnique();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(sigleCanton);
		this.fraction = (target.getNoOFSFaitiere() != null);
		this.principale = (target.getFractions() != null && !target.getFractions().isEmpty());
		this.district = DistrictImpl.get(target.getDistrict());
	}

	private static String toUpperCase(String string) {
		return string == null ? null : string.toUpperCase();
	}

	private static RegDate fidor2reg(FidorDate date) {
		return date == null ? null : RegDate.get(date.getYear(), date.getMonth(), date.getDay());
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public int getNoOFSEtendu() {
		return noOFSEtendu;
	}

	@Override
	public int getNoOFS() {
		return noOFSEtendu;
	}

	@Override
	public int getNumTechMere() {
		return numTechMere;
	}

	@Override
	public int getNumeroTechnique() {
		return numTechnique;
	}

	@Override
	public String getSigleCanton() {
		return sigleCanton;
	}

	@Override
	public boolean isVaudoise() {
		return vaudoise;
	}

	@Override
	public boolean isFraction() {
		return fraction;
	}

	@Override
	public boolean isPrincipale() {
		return principale;
	}

	@Override
	public District getDistrict() {
		return district;
	}
}

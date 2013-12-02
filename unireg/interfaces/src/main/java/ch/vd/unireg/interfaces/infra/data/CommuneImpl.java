package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import ch.vd.infrastructure.model.EnumCanton;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.XmlUtils;

public class CommuneImpl extends EntiteOFSImpl implements Commune, Serializable {

	private static final long serialVersionUID = -5224553666590025447L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOfsCommuneMere;
	private final boolean vaudoise;
	private final boolean fraction;
	private final boolean principale;
	private final String sigleCanton;
	private final Integer codeDistrict;
	private final Integer codeRegion;

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

	public static Commune get(ch.vd.evd0012.v1.CommuneFiscale target) {
		if (target == null) {
			return null;
		}
		return new CommuneImpl(target);
	}

	protected CommuneImpl(ch.vd.infrastructure.model.CommuneSimple target) {
		super((target.isFraction() ? target.getNoTechnique() : target.getNoOFS()), target.getNomMinuscule(), target.getNomMinuscule(), target.getSigleOFS());
		this.dateDebut = RegDateHelper.get(target.getDateDebutValidite());
		this.dateFin = RegDateHelper.get(target.getDateFinValidite());
		this.sigleCanton = target.getSigleCanton();
		this.noOfsCommuneMere = target.getNumTechMere();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(sigleCanton);
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.codeDistrict = null;
		this.codeRegion = null;
	}

	protected CommuneImpl(ch.vd.infrastructure.model.Commune target) {
		super((target.isFraction() ? target.getNoTechnique() : target.getNoOFS()), target.getNomMinuscule(), target.getNomMinuscule(), target.getSigleOFS());
		this.dateDebut = RegDateHelper.get(target.getDateDebutValidite());
		this.dateFin = RegDateHelper.get(target.getDateFinValidite());
		this.sigleCanton = target.getSigleCanton();
		this.noOfsCommuneMere = target.getNumTechMere();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(sigleCanton);
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.codeDistrict = null;
		this.codeRegion = null;
	}

	public CommuneImpl(ch.vd.evd0012.v1.CommuneFiscale target) {
		super(target.getNumeroOfs(), target.getNomCourt(), target.getNomOfficiel(), null);
		this.dateDebut = XmlUtils.xmlcal2regdate(target.getDateDebutValidite());
		this.dateFin = XmlUtils.xmlcal2regdate(target.getDateFinValidite());
		this.sigleCanton = target.getSigleCanton();
		this.noOfsCommuneMere = target.isEstUneFractionDeCommune() ? link2OfsId(target.getCommuneFaitiereLink()) : -1;
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(sigleCanton);
		this.fraction = target.isEstUneFractionDeCommune();
		this.principale = target.isEstUneCommuneFaitiere();
		this.codeDistrict = link2OfsId(target.getDistrictFiscalLink());
		this.codeRegion = link2OfsId(target.getRegionFiscaleLink());
	}

	public static Integer link2OfsId(String link) {
		if (StringUtils.isBlank(link)) {
			return null;
		}
		// e.g. "districtFiscal/4" => 4
		// e.g. "regionFiscale/1" => 1
		// e.g. "communeFiscale/5871I19600101" => 5871

		int slash = link.indexOf('/');
		if (slash >= 0) {
			link = link.substring(slash + 1);
		}

		int i = link.indexOf('I');
		if (i >= 0) {
			link = link.substring(0, i);
		}

		return Integer.parseInt(link);
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
	public int getOfsCommuneMere() {
		return noOfsCommuneMere;
	}

	@Override
	public String getSigleCanton() {
		return sigleCanton;
	}

	@Override
	public String getNomOfficielAvecCanton() {
		final String canton = String.format("(%s)", sigleCanton);
		final String nomOfficiel = getNomOfficiel();
		if (nomOfficiel.endsWith(sigleCanton) || nomOfficiel.endsWith(canton)) {
			return nomOfficiel;
		}
		else {
			return String.format("%s %s", nomOfficiel, canton);
		}
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
	public Integer getCodeDistrict() {
		return codeDistrict;
	}

	@Override
	public Integer getCodeRegion() {
		return codeRegion;
	}

	@Override
	protected String getMemberString() {
		return String.format("%s, dateDebut=%s, dateFin=%s, mere=%s, vaudoise=%b, fraction=%b, principale=%b, canton=%s, district=%s, region=%s",
		                     super.getMemberString(), dateDebut, dateFin, noOfsCommuneMere <= 0 ? null : noOfsCommuneMere, vaudoise, fraction,
		                     principale, buildQuotedString(sigleCanton), codeDistrict, codeRegion);
	}
}

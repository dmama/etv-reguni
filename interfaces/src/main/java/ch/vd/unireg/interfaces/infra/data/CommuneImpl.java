package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.infrastructure.model.rest.CommuneSimple;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;

public class CommuneImpl extends EntiteOFSImpl implements Commune, Serializable {

	private static final long serialVersionUID = 5489780767000047569L;

	private static final RegDate REFINF_ORIGINE_COMMUNES = RegDate.get(1960, 1, 1);

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOfsCommuneMere;
	private final boolean vaudoise;
	private final boolean fraction;
	private final boolean principale;
	private final String sigleCanton;
	private final Integer codeDistrict;
	private final Integer codeRegion;

	public static CommuneImpl get(CommuneSimple target) {
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

	protected CommuneImpl(CommuneSimple target) {
		super((target.isFraction() ? target.getNoTechnique() : target.getNoOFS()), target.getNomMinuscule(), target.getNomMinuscule(), target.getSigleOFS());
		this.dateDebut = XmlUtils.xmlcal2regdate(target.getDateDebutValidite());
		this.dateFin = XmlUtils.xmlcal2regdate(target.getDateFinValidite());
		this.sigleCanton = target.getSigleCanton();
		this.noOfsCommuneMere = target.getNumTechMere();
		this.vaudoise = ServiceInfrastructureRaw.SIGLE_CANTON_VD.equals(sigleCanton);
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.codeDistrict = null;
		this.codeRegion = null;
	}

	public CommuneImpl(ch.vd.evd0012.v1.CommuneFiscale target) {
		super(target.getNumeroOfs(), target.getNomCourt(), target.getNomOfficiel(), null);
		this.dateDebut = getDateDebutValiditeCommune(target);
		this.dateFin = XmlUtils.xmlcal2regdate(target.getDateFinValidite());
		this.sigleCanton = target.getSigleCanton();
		this.noOfsCommuneMere = target.isEstUneFractionDeCommune() ? link2OfsId(target.getCommuneFaitiereLink()) : -1;
		this.vaudoise = ServiceInfrastructureRaw.SIGLE_CANTON_VD.equals(sigleCanton);
		this.fraction = target.isEstUneFractionDeCommune();
		this.principale = target.isEstUneCommuneFaitiere();
		this.codeDistrict = link2OfsId(target.getDistrictFiscalLink());
		this.codeRegion = link2OfsId(target.getRegionFiscaleLink());
	}

	@Nullable
	private static RegDate getDateDebutValiditeCommune(ch.vd.evd0012.v1.CommuneFiscale target) {
		final RegDate brutto = XmlUtils.xmlcal2regdate(target.getDateDebutValidite());

		// RefInf nous dit que toutes les communes commencent au plus tôt le 01.01.1960 (parce que c'est ainsi qu'est fait le fichier OFS)
		// Pour les besoins des PM (des fors suisses sont beaucoup plus vieux que ça...) on va supposer qu'une commune créée le 01.01.1960
		// est en fait valide depuis les origines...
		if (REFINF_ORIGINE_COMMUNES == brutto) {
			return null;
		}

		return brutto;
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

	@Override
	public RegDate getDateDebut() {
		return getDateDebutValidite();
	}

	@Override
	public RegDate getDateFin() {
		return getDateFinValidite();
	}


}

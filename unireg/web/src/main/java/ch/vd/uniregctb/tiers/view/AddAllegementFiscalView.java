package ch.vd.uniregctb.tiers.view;

import java.math.BigDecimal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCantonCommune;
import ch.vd.uniregctb.tiers.AllegementFiscalConfederation;

public class AddAllegementFiscalView {

	private long pmId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private AllegementFiscal.TypeCollectivite typeCollectivite;
	private AllegementFiscal.TypeImpot typeImpot;
	private BigDecimal pourcentageAllegement;
	private Integer noOfsCommune;
	private AllegementFiscalConfederation.Type typeIFD;
	private AllegementFiscalCantonCommune.Type typeICC;
	private PourcentageMontant flagPourcentageMontant;

	public enum PourcentageMontant {
		POURCENTAGE,
		MONTANT
	}

	public AddAllegementFiscalView() {
	}

	public AddAllegementFiscalView(long pmId) {
		this.pmId = pmId;
		this.flagPourcentageMontant = PourcentageMontant.POURCENTAGE;
	}

	public long getPmId() {
		return pmId;
	}

	public void setPmId(long pmId) {
		this.pmId = pmId;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public AllegementFiscal.TypeCollectivite getTypeCollectivite() {
		return typeCollectivite;
	}

	public void setTypeCollectivite(AllegementFiscal.TypeCollectivite typeCollectivite) {
		this.typeCollectivite = typeCollectivite;
	}

	public AllegementFiscal.TypeImpot getTypeImpot() {
		return typeImpot;
	}

	public void setTypeImpot(AllegementFiscal.TypeImpot typeImpot) {
		this.typeImpot = typeImpot;
	}

	public BigDecimal getPourcentageAllegement() {
		return pourcentageAllegement;
	}

	public void setPourcentageAllegement(BigDecimal pourcentageAllegement) {
		this.pourcentageAllegement = pourcentageAllegement;
	}

	public Integer getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(Integer noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}

	public AllegementFiscalConfederation.Type getTypeIFD() {
		return typeIFD;
	}

	public void setTypeIFD(AllegementFiscalConfederation.Type typeIFD) {
		this.typeIFD = typeIFD;
	}

	public AllegementFiscalCantonCommune.Type getTypeICC() {
		return typeICC;
	}

	public void setTypeICC(AllegementFiscalCantonCommune.Type typeICC) {
		this.typeICC = typeICC;
	}

	public PourcentageMontant getFlagPourcentageMontant() {
		return flagPourcentageMontant;
	}

	public void setFlagPourcentageMontant(PourcentageMontant flagPourcentageMontant) {
		this.flagPourcentageMontant = flagPourcentageMontant;
	}
}

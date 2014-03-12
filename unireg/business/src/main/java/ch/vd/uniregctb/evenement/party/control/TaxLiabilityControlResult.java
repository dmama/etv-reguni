package ch.vd.uniregctb.evenement.party.control;

public class TaxLiabilityControlResult {

	private Long idTiersAssujetti;
	private TaxLiabilityControlEchec echec;

	public Long getIdTiersAssujetti() {
		return idTiersAssujetti;
	}

	public void setIdTiersAssujetti(Long idTiersAssujetti) {
		this.idTiersAssujetti = idTiersAssujetti;
	}

	public TaxLiabilityControlEchec getEchec() {
		return echec;
	}

	public void setEchec(TaxLiabilityControlEchec echec) {
		this.echec = echec;
	}
}

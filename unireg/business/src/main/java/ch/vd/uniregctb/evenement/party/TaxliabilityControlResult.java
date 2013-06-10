package ch.vd.uniregctb.evenement.party;

public class TaxliabilityControlResult {
	private Long idTiersAssujetti;
	private TaxliabilityControlEchec echec;

	public Long getIdTiersAssujetti() {
		return idTiersAssujetti;
	}

	public void setIdTiersAssujetti(Long idTiersAssujetti) {
		this.idTiersAssujetti = idTiersAssujetti;
	}

	public TaxliabilityControlEchec getEchec() {
		return echec;
	}

	public void setEchec(TaxliabilityControlEchec echec) {
		this.echec = echec;
	}
}

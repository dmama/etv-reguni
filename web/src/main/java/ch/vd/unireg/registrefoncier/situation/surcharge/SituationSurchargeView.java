package ch.vd.unireg.registrefoncier.situation.surcharge;

import ch.vd.unireg.registrefoncier.SituationRF;

/**
 * Données pour l'établissement d'une surcharge de commune sur une situation.
 */
public class SituationSurchargeView {
	private long situationId;
	private Integer noOfsSurcharge;

	public SituationSurchargeView() {
	}

	public SituationSurchargeView(SituationRF situation) {
		this.situationId = situation.getId();
		this.noOfsSurcharge = situation.getNoOfsCommuneSurchargee();
	}

	public long getSituationId() {
		return situationId;
	}

	public void setSituationId(long situationId) {
		this.situationId = situationId;
	}

	public Integer getNoOfsSurcharge() {
		return noOfsSurcharge;
	}

	public void setNoOfsSurcharge(Integer noOfsSurcharge) {
		this.noOfsSurcharge = noOfsSurcharge;
	}
}

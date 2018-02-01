package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.RegDate;

public class ChangeDateExerciceCommercialView {

	private Long pmId;
	private RegDate ancienneDate;
	private RegDate nouvelleDate;

	public ChangeDateExerciceCommercialView() {
	}

	public ChangeDateExerciceCommercialView(Long pmId) {
		this.pmId = pmId;
	}

	public Long getPmId() {
		return pmId;
	}

	public void setPmId(Long pmId) {
		this.pmId = pmId;
	}

	public RegDate getAncienneDate() {
		return ancienneDate;
	}

	public void setAncienneDate(RegDate ancienneDate) {
		this.ancienneDate = ancienneDate;
	}

	public RegDate getNouvelleDate() {
		return nouvelleDate;
	}

	public void setNouvelleDate(RegDate nouvelleDate) {
		this.nouvelleDate = nouvelleDate;
	}
}

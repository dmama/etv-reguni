package ch.vd.uniregctb.activation.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.view.TiersGeneralView;

public class TiersReactivationRecapView {

	private TiersGeneralView tiers;

	private RegDate dateReactivation;

	public TiersGeneralView getTiers() {
		return tiers;
	}

	public void setTiers(TiersGeneralView tiers) {
		this.tiers = tiers;
	}

	public RegDate getDateReactivation() {
		return dateReactivation;
	}

	public void setDateReactivation(RegDate dateReactivation) {
		this.dateReactivation = dateReactivation;
	}

}

package ch.vd.unireg.activation.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.general.view.TiersGeneralView;

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

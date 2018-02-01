package ch.vd.unireg.rt.view;

import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.tiers.view.TiersCriteriaView;

public class DebiteurListView extends TiersCriteriaView {

	private static final long serialVersionUID = -7506204082722646397L;

	private TiersGeneralView sourcier;

	public TiersGeneralView getSourcier() {
		return sourcier;
	}

	public void setSourcier(TiersGeneralView sourcier) {
		this.sourcier = sourcier;
	}


}

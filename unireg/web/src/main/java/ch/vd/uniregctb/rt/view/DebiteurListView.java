package ch.vd.uniregctb.rt.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class DebiteurListView extends TiersCriteriaView {

	private static final long serialVersionUID = 1L;

	private TiersGeneralView sourcier;

	public TiersGeneralView getSourcier() {
		return sourcier;
	}

	public void setSourcier(TiersGeneralView sourcier) {
		this.sourcier = sourcier;
	}


}

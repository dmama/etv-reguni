package ch.vd.uniregctb.rapport.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class RapportListView  extends TiersCriteriaView{

	/**
	 *
	 */
	private static final long serialVersionUID = 1424635911204855501L;

	private TiersGeneralView tiers;

	//private String natureTiers;
	
	private boolean isAllowed;

	public boolean isAllowed() {
		return isAllowed;
	}

	public void setAllowed(boolean isAllowed) {
		this.isAllowed = isAllowed;
	}

	public TiersGeneralView getTiers() {
		return tiers;
	}

	public void setTiers(TiersGeneralView tiers) {
		this.tiers = tiers;
	}

	/*public String getNatureTiers() {
		return natureTiers;
	}

	public void setNatureTiers(String natureTiers) {
		this.natureTiers = natureTiers;
	}*/

}

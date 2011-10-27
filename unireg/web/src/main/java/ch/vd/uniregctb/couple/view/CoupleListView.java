package ch.vd.uniregctb.couple.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class CoupleListView extends TiersCriteriaView {

	/**
	 * Serial ID
	 */
	private static final long serialVersionUID = 5427261326083899967L;

	private TiersGeneralView premierePersonne;

	private TiersGeneralView secondePersonne;

	private String page;
	
	private boolean isAllowed;

	private boolean conjointInconnu;
	
	public TiersGeneralView getPremierePersonne() {
		return premierePersonne;
	}

	public void setPremierePersonne(TiersGeneralView premierePersonne) {
		this.premierePersonne = premierePersonne;
	}

	public TiersGeneralView getSecondePersonne() {
		return secondePersonne;
	}

	public void setSecondePersonne(TiersGeneralView secondePersonne) {
		this.secondePersonne = secondePersonne;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public boolean isAllowed() {
		return isAllowed;
	}

	public void setAllowed(boolean isAllowed) {
		this.isAllowed = isAllowed;
	}

	public boolean isConjointInconnu() {
		return conjointInconnu;
	}

	public void setConjointInconnu(boolean conjointInconnu) {
		this.conjointInconnu = conjointInconnu;
	}

	@Override
	public boolean isEmpty() {
		return !conjointInconnu && super.isEmpty();
	}
}

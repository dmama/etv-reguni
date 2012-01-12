package ch.vd.uniregctb.lr.view;

import java.util.List;

import ch.vd.uniregctb.general.view.TiersGeneralView;

public class ListeRecapListView  {

	private TiersGeneralView dpi;

	private List<ListeRecapDetailView> lrs;

	public TiersGeneralView getDpi() {
		return dpi;
	}

	public void setDpi(TiersGeneralView dpi) {
		this.dpi = dpi;
	}

	public List<ListeRecapDetailView> getLrs() {
		return lrs;
	}

	public void setLrs(List<ListeRecapDetailView> lrs) {
		this.lrs = lrs;
	}



}

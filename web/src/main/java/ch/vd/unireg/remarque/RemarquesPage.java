package ch.vd.uniregctb.remarque;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class RemarquesPage {

	private Long tiersId;
	private List<RemarqueView> remarques;
	private boolean showHisto;
	private int page;
	private int totalCount;

	public RemarquesPage(Long tiersId, List<RemarqueView> remarques, boolean showHisto, int page, int totalCount) {
		this.tiersId = tiersId;
		this.remarques = remarques;
		this.showHisto = showHisto;
		this.page = page;
		this.totalCount = totalCount;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public List<RemarqueView> getRemarques() {
		return remarques;
	}

	public void setRemarques(List<RemarqueView> remarques) {
		this.remarques = remarques;
	}

	public boolean isShowHisto() {
		return showHisto;
	}

	public void setShowHisto(boolean showHisto) {
		this.showHisto = showHisto;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
}

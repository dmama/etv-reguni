package ch.vd.uniregctb.rf;

import java.util.List;

public class ImmeublesPage {

	List<ImmeubleView> immeubles;
	int page;
	int totalCount;

	public ImmeublesPage(List<ImmeubleView> immeubles, int page, int totalCount) {
		this.immeubles = immeubles;
		this.page = page;
		this.totalCount = totalCount;
	}

	public List<ImmeubleView> getImmeubles() {
		return immeubles;
	}

	public void setImmeubles(List<ImmeubleView> immeubles) {
		this.immeubles = immeubles;
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

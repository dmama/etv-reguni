package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.Collection;

public class TopList<T> extends ArrayList<T> {

	private int totalHits;

	public TopList(int initialCapacity) {
		super(initialCapacity);
	}

	public TopList() {
	}

	public TopList(Collection<? extends T> c) {
		super(c);
	}

	public int getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(int totalHits) {
		this.totalHits = totalHits;
	}
}

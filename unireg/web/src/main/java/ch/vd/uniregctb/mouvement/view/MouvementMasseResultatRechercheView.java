package ch.vd.uniregctb.mouvement.view;

import java.util.Collections;
import java.util.List;

public class MouvementMasseResultatRechercheView {

	private final List<MouvementDetailView> results;
	private final int size;

	public MouvementMasseResultatRechercheView(List<MouvementDetailView> results, int size) {
		this.results = results != null ? Collections.unmodifiableList(results) : Collections.<MouvementDetailView>emptyList();
		this.size = size;
	}

	public List<MouvementDetailView> getResults() {
		return results;
	}

	public int getResultSize() {
		return size;
	}
}

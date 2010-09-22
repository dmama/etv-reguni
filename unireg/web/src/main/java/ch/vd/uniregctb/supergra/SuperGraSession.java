package ch.vd.uniregctb.supergra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.uniregctb.supergra.delta.Delta;

/**
 * Données stockées dans la session Web pour le mode d'édition SuperGra.
 */
public class SuperGraSession {

	/**
	 * Les deltas (= changements d'attributs, ajout d'entités, ...) déjà enregistrés mais non encore commités
	 */
	private List<Delta> deltas = new ArrayList<Delta>();

	/**
	 * Les états des tiers modifiées dans la session courante.
	 */
	private List<TiersState> tiersStates = new ArrayList<TiersState>();

	private final Options options = new Options();

	public List<Delta> getDeltas() {
		return Collections.unmodifiableList(deltas);
	}

	public int deltaSize() {
		return deltas.size();
	}

	public void addDelta(Delta delta) {
		if (this.deltas == null) {
			this.deltas = new ArrayList<Delta>();
		}
		this.deltas.add(delta);
	}

	public void addDeltas(List<? extends Delta> deltas) {
		if (this.deltas == null) {
			this.deltas = new ArrayList<Delta>();
		}
		this.deltas.addAll(deltas);
	}

	public Delta removeDelta(int index) {
		return deltas.remove(index);
	}

	public void clearDeltas() {
		deltas.clear();
	}

	public List<TiersState> getTiersStates() {
		return tiersStates;
	}

	public void setTiersStates(List<TiersState> tiersStates) {
		this.tiersStates = tiersStates;
	}

	public Options getOptions() {
		return options;
	}
}

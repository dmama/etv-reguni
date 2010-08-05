package ch.vd.uniregctb.supergra;

import java.util.ArrayList;
import java.util.List;

/**
 * Données stockées dans la session Web pour le mode d'édition SuperGra.
 */
public class SuperGraSession {

	/**
	 * Les deltas (= changements d'attributs, ajout d'entités, ...) déjà enregistrés mais non encore commités
	 */
	private List<Delta> deltas = new ArrayList<Delta>();

	public List<Delta> getDeltas() {
		return deltas;
	}

	public void setDeltas(List<Delta> deltas) {
		this.deltas = deltas;
	}

	public void addDelta(Delta delta) {
		if (this.deltas == null) {
			this.deltas = new ArrayList<Delta>();
		}
		this.deltas.add(delta);
	}

	public void addDeltas(List<Delta> deltas) {
		if (this.deltas == null) {
			this.deltas = new ArrayList<Delta>();
		}
		this.deltas.addAll(deltas);
	}
}

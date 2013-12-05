package ch.vd.uniregctb.mouvement.view;

import java.util.List;

/**
 * Vue pour la listes des mouvements d'un contribuable
 *
 * @author xcifde
 *
 */
public class MouvementListView {

	private ContribuableView contribuable;

	private List<MouvementDetailView> mouvements;

	public ContribuableView getContribuable() {
		return contribuable;
	}

	public void setContribuable(ContribuableView contribuable) {
		this.contribuable = contribuable;
	}

	public List<MouvementDetailView> getMouvements() {
		return mouvements;
	}

	public void setMouvements(List<MouvementDetailView> mouvements) {
		this.mouvements = mouvements;
	}

}

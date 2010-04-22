package ch.vd.uniregctb.mouvement.view;

import java.util.List;

import ch.vd.uniregctb.general.view.TiersGeneralView;

/**
 * Vue pour la listes des mouvements d'un contribuable
 *
 * @author xcifde
 *
 */
public class MouvementListView {

	private TiersGeneralView contribuable;

	private List<MouvementDetailView> mouvements;

	public TiersGeneralView getContribuable() {
		return contribuable;
	}

	public void setContribuable(TiersGeneralView contribuable) {
		this.contribuable = contribuable;
	}

	public List<MouvementDetailView> getMouvements() {
		return mouvements;
	}

	public void setMouvements(List<MouvementDetailView> mouvements) {
		this.mouvements = mouvements;
	}

}

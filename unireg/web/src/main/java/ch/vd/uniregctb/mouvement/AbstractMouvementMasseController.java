package ch.vd.uniregctb.mouvement;

import ch.vd.uniregctb.mouvement.manager.MouvementMasseManager;

public abstract class AbstractMouvementMasseController extends AbstractMouvementController {

	private MouvementMasseManager mouvementManager;

	public void setMouvementManager(MouvementMasseManager mouvementManager) {
		this.mouvementManager = mouvementManager;
	}

	protected MouvementMasseManager getMouvementManager() {
		return mouvementManager;
	}
}

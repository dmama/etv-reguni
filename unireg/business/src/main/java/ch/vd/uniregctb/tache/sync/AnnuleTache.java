package ch.vd.uniregctb.tache.sync;

import ch.vd.uniregctb.tiers.Tache;

/**
 * Action permettant d'annuler une tâche devenue obsolète.
 */
public class AnnuleTache extends SynchronizeAction {
	public final Tache tache;

	public AnnuleTache(Tache tache) {
		this.tache = tache;
	}

	@Override
	public void execute(Context context) {
		tache.setAnnule(true);
	}
}

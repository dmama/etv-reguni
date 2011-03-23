package ch.vd.uniregctb.tache.sync;

/**
 * Action pouvant être entreprise pour synchroniser des données d'un contribuable par rapport à d'autres données.
 */
public abstract class SynchronizeAction {
	public abstract void execute(Context context);
}

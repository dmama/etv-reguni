package ch.vd.uniregctb.tache.sync;

/**
 * Action pouvant être entreprise pour synchroniser des données d'un contribuable par rapport à d'autres données.
 */
public abstract class SynchronizeAction {
	public abstract void execute(Context context);

	/**
	 * @return <b>vrai</b> si l'exécution de cette action va modifier directement une entité Hibernate (sans prendre en compte les tâches elles-mêmes); <b>faux</b> si uniquement des tâches seront
	 *         modifiées.
	 */
	public abstract boolean willChangeEntity();

	@Override
	public abstract String toString();
}

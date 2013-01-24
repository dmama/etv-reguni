package ch.vd.uniregctb.data;

public interface DataEventService {

	/**
	 * Enregistre un listener.
	 *
	 * @param listener le listener à enregistrer
	 */
	void register(DataEventListener listener);

	/**
	 * Notifie à tous les listeners qu'un tiers à été changé dans la base de données.
	 *
	 * @param id l'id du tiers changé
	 */
	void onTiersChange(long id);

	/**
	 * Notifie à tous les listeners qu'un individu à été changé dans le registre civil.
	 *
	 * @param id le numéro de l'individu changé
	 */
	void onIndividuChange(long id);

	/**
	 * Notifie à tous les listeners qu'une personne morale à été changée dans le registre PM.
	 *
	 * @param id le numéro de la personne morale changée
	 */
	void onPersonneMoraleChange(long id);

	/**
	 * Notifie à tous les listeners qu'un droit d'accès à été changé sur une personne physique.
	 *
	 * @param ppId l'id de la personne physique concernée
	 */
	void onDroitAccessChange(long ppId);

	/**
	 * Notifie à tous les listeners que la base de données a été (re)chargée complétement à partir d'une opération SQL.
	 */
	void onLoadDatabase();

	/**
	 * Notifie à tous les listeners que la base de données a été vidée complétement à partir d'une opération SQL.
	 */
	void onTruncateDatabase();
}

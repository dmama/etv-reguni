package ch.vd.uniregctb.database;

/**
 * Interface de notification de changements sur la base de données Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface DatabaseListener {

	/**
	 * Cette méthode est appelée lorsqu'un tiers va être ajouté/modifié dans la base de données.
	 * <p>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param id
	 *            le numéro du tiers
	 */
	void onTiersChange(long id);

	/**
	 * Cette méthode est appelée lorsqu'un droit d'accès va être ajouté/modifié dans la base de données.
	 * <p>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param id
	 *            le numéro du tiers concerné par le droit d'accès
	 */
	void onDroitAccessChange(long tiersId);

	/**
	 * Cette méthode est appelée lorsque la base de données va être entièrement vidée (avant le chargement d'un script DBunit, par exemple).
	 * <p>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 */
	void onTruncateDatabase();

	/**
	 * Cette méthode est appelée après que la base de données ait été chargée (après le chargement d'un script DBunit, par exemple).
	 * <p>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 */
	void onLoadDatabase();
}

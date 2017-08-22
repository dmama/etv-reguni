package ch.vd.uniregctb.data;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Interface de notification de changements sur les données Unireg, quand la source du changement
 * est dans les données fiscales
 */
public interface FiscalDataEventListener {

	/**
	 * Cette méthode est appelée lorsqu'un tiers va être ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param id le numéro du tiers
	 */
	void onTiersChange(long id);

	/**
	 * Cette méthode est appelée lorsqu'un droit d'accès va être ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param tiersId le numéro du tiers concerné par le droit d'accès
	 */
	void onDroitAccessChange(long tiersId);

	/**
	 * Cette méthode est appelée lorsqu'un rapport entre tiers va être ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param type type du rapport entre tiers concerné
	 * @param sujetId l'id du sujet du rapport entre tiers concerné
	 * @param objetId l'id de l'objet du rapport entre tiers concerné
	 */
	void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId);

	/**
	 * Cette méthode est appelée lorsqu'un immeuble RF va être ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param immeubleId l'id technique Unireg de l'immeuble.
	 */
	void onImmeubleChange(long immeubleId);

	/**
	 * Cette méthode est appelée lorsqu'un bâtiment RF va être ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param batimentId l'id technique Unireg de l'immeuble.
	 */
	void onBatimentChange(long batimentId);

	/**
	 * Cette méthode est appelée lorsqu'une communauté de propriétaires RF va être ajoutée/modifiée dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param communauteId l'id technique Unireg de la communauté
	 */
	void onCommunauteChange(long communauteId);

	/**
	 * Cette méthode est appelée lorsque la base de données va être entièrement vidée (avant le chargement d'un script DBunit, par exemple).
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 */
	void onTruncateDatabase();

	/**
	 * Cette méthode est appelée après que la base de données ait été chargée (après le chargement d'un script DBunit, par exemple).
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 */
	void onLoadDatabase();

}

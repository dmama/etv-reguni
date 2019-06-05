package ch.vd.unireg.data;

import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Interface du service de notification de changements sur les données Unireg.
 */
public interface FiscalDataEventNotifier {

	/**
	 * Notifie qu'un tiers à été changé dans la base de données.
	 *
	 * @param id l'id du tiers changé
	 */
	void notifyTiersChange(long id);

	/**
	 * Notifie qu'un droit d'accès à été changé sur un tiers.
	 *
	 * @param id l'id du tiers concerné
	 */
	void notifyDroitAccessChange(long id);

	/**
	 * Notifie qu'un immeuble RF a été ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param immeubleId l'id technique Unireg de l'immeuble.
	 */
	void notifyImmeubleChange(long immeubleId);

	/**
	 * Notifie qu'un bâtiment RF a été ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param batimentId l'id technique Unireg de l'immeuble.
	 */
	void notifyBatimentChange(long batimentId);

	/**
	 * Notifie qu'une communauté de propriétaires RF a été ajoutée/modifiée dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param communauteId l'id technique Unireg de la communauté
	 */
	void notifyCommunauteChange(long communauteId);

	/**
	 * Notifie qu'un rapport entre tiers a été modifié entre les tiers donnés
	 *
	 * @param type type du rapport entre tiers concerné
	 * @param sujetId l'id du sujet du rapport entre tiers concerné
	 * @param objetId l'id de l'objet du rapport entre tiers concerné
	 */
	void notifyRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId);

	/**
	 * Notifie que la base de données a été (re)chargée complétement à partir d'une opération SQL.
	 */
	void notifyLoadDatabase();

	/**
	 * Notifie que la base de données a été vidée complétement à partir d'une opération SQL.
	 */
	void notifyTruncateDatabase();
}

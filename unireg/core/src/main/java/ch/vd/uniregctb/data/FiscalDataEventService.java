package ch.vd.uniregctb.data;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Interface du service de notification de changements sur les données Unireg, quand la source du changement
 * est dans les données fiscales
 */
public interface FiscalDataEventService {

	/**
	 * Enregistre un listener.
	 *
	 * @param listener le listener à enregistrer
	 */
	void register(FiscalDataEventListener listener);

	/**
	 * Notifie à tous les listeners qu'un tiers à été changé dans la base de données.
	 *
	 * @param id l'id du tiers changé
	 */
	void onTiersChange(long id);

	/**
	 * Notifie à tous les listeners qu'un droit d'accès à été changé sur une personne physique.
	 *
	 * @param ppId l'id de la personne physique concernée
	 */
	void onDroitAccessChange(long ppId);

	/**
	 * Notifie à tous les listeners qu'un rapport entre tiers a été modifié entre les tiers donnés
	 *
	 * @param type type du rapport entre tiers concerné
	 * @param sujetId l'id du sujet du rapport entre tiers concerné
	 * @param objetId l'id de l'objet du rapport entre tiers concerné
	 */
	void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId);

	/**
	 * Notifie à tous les listeners que la base de données a été (re)chargée complétement à partir d'une opération SQL.
	 */
	void onLoadDatabase();

	/**
	 * Notifie à tous les listeners que la base de données a été vidée complétement à partir d'une opération SQL.
	 */
	void onTruncateDatabase();
}
package ch.vd.uniregctb.data;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

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
	 * Notifie à tous les listeners qu'une organisation à été changé dans le registre des entreprises.
	 *
	 * @param id le numéro de l'organisation changée
	 */
	void onOrganisationChange(long id);

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

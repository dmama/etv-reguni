package ch.vd.uniregctb.data;

/**
 * Interface du service de notification de changements sur les données Unireg, quand la source du changement
 * provient des données civiles
 */
public interface CivilDataEventService {

	/**
	 * Enregistre un listener.
	 *
	 * @param listener le listener à enregistrer
	 */
	void register(CivilDataEventListener listener);

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

}

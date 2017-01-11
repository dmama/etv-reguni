package ch.vd.uniregctb.data;

/**
 * Interface du service de notification de changements sur les données Unireg, quand la source du changement
 * provient des sources de données (civiles)
 */
public interface SourceDataEventService {

	/**
	 * Enregistre un listener "source".
	 *
	 * @param listener le listener à enregistrer
	 */
	void register(SourceDataEventListener listener);

	/**
	 * Notifie à tous les listeners "source" qu'un individu à été changé dans le registre civil.
	 *
	 * @param id le numéro de l'individu changé
	 */
	void onIndividuChange(long id);

	/**
	 * Notifie à tous les listeners "source" qu'une organisation à été changé dans le registre des entreprises.
	 *
	 * @param id le numéro de l'organisation changée
	 */
	void onOrganisationChange(long id);

}

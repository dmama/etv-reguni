package ch.vd.unireg.data;

/**
 * Interface de notification de changements sur des données civiles externes.
 */
public interface CivilDataEventNotifier {
	/**
	 * Notifie qu'un individu à été changé dans le registre civil.
	 *
	 * @param id le numéro de l'individu changé
	 */
	void notifyIndividuChange(long id);

	/**
	 * Notifie qu'une entreprise à été changé dans le registre des entreprises.
	 *
	 * @param id le numéro de l'entreprise changée
	 */
	void notifyEntrepriseChange(long id);
}

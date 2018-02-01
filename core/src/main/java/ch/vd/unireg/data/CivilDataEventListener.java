package ch.vd.unireg.data;

/**
 * Interface de notification de changements sur les données Unireg, quand la source du changement
 * provient des données civiles
 */
public interface CivilDataEventListener {

	/**
	 * Cette méthode est appelée lorsqu'une organisation a été ajouté/modifié dans le registre des entreprises.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param id le numéro d'individu
	 */
	void onOrganisationChange(long id);

	/**
	 * Cette méthode est appelée lorsqu'un individu associé à un tiers a été ajouté/modifié dans le registre civil.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param id le numéro d'individu
	 */
	void onIndividuChange(long id);

}

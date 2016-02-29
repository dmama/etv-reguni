package ch.vd.uniregctb.migration.pm;

public interface Worker {

	/**
	 * Appelé par le {@link Feeder} pour chaque graphe chargé
	 * @param graphe graphe d'objets à traiter
	 * @throws Exception en cas de problème
	 */
	void onGraphe(Graphe graphe) throws Exception;

	/**
	 * Appelé quand tous les graphes ont été soumis
	 */
	void feedingOver() throws Exception;
}

package ch.vd.uniregctb.evenement.civil.engine.ech;

/**
 * Interface de lecture des paramètres utilisés dans le traitement des événements civils e-CH
 */
public interface EvenementCivilEchStrategyParameters {

	/**
	 * @return le nombre maximal de jours autorisés entre la date de fin d'une adresse de résidence et la date de l'événement civil de départ correspondant (toujours positif ou nul)
	 */
	int getDecalageMaxPourDepart();
}

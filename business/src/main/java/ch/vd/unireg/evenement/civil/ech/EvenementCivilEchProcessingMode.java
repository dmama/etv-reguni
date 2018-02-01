package ch.vd.unireg.evenement.civil.ech;

/**
 * Mode de processing d'un événement civil ECH
 */
public enum EvenementCivilEchProcessingMode {

	/**
	 * A traiter sur la voie à trafic léger sans aucun délai (à utiliser pour les relances d'événements
	 * demandées par un opérateur UNIREG)
	 */
	IMMEDIATE,

	/**
	 * A traiter sur la voie à trafic léger avec délai normal (à utiliser pour les arrivées d'événements
	 * à faible trafic générés par l'opérateur du référenciel civil)
	 */
	MANUAL,

	/**
	 * A traiter sur la voie à fort trafic avec délai normal (à utiliser pour les arrivées ou les relances
	 * d'événements en gros volumes)
	 */
	BATCH
}

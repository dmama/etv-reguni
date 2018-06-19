package ch.vd.unireg.evenement.organisation;

/**
 * Mode de processing d'un événement entreprise
 */
public enum EvenementEntrepriseProcessingMode {

	/**
	 * A traiter sur la voie à trafic léger sans aucun délai (à utiliser pour les relances d'événements
	 * demandées par un opérateur UNIREG, lorsque l'entreprise n'est pas déjà en attente de traitement.)
	 */
	IMMEDIATE,

	/**
	 * A traiter sur la voie à trafic léger avec délai normal (à utiliser pour les relances d'événements
	 * demandées par un opérateur UNIREG, lorsque l'entreprise est déjà en attente de traitement.)
	 */
	PRIORITY,

	/**
	 * A traiter sur la voie normale avec délai normal (à utiliser pour les arrivées ou les relances
	 * d'événements en gros volumes)
	 */
	BULK
}

package ch.vd.uniregctb.type;

/**
 * Les différents groupes de flags d'entreprises... qui peuvent contenir des flags mutuellement
 * exclusifs ou pas
 */
public enum GroupeFlagsEntreprise {

	/**
	 * Groupe des flags mutuellements exclusifs autour des sociétés immobilières, des sociétés
	 * de service et de l'utilité publique
	 */
	SI_SERVICE_UTILITE_PUBLIQUE(true),

	/**
	 * Groupe des flags complèments libres (pas de contrôle de chevauchement du tout, en tout cas entre entités de types différents)
	 */
	LIBRE(false);

	private final boolean flagsMutuellementExclusifs;

	GroupeFlagsEntreprise(boolean flagsMutuellementExclusifs) {
		this.flagsMutuellementExclusifs = flagsMutuellementExclusifs;
	}

	public boolean isFlagsMutuellementExclusifs() {
		return flagsMutuellementExclusifs;
	}
}

package ch.vd.unireg.registrefoncier.immeuble.graph;

/**
 * Elément qui représente un ayant-droit dans le graphe des liens entre immeubles et ayants-droits.
 */
public class AyantDroit {
	/**
	 * La clé d'identification unique dans le graphe de l'ayant-droit
	 */
	protected final String key;
	protected final String type;
	protected final String label;

	public AyantDroit(String key, String type, String label) {
		this.key = key;
		this.type = type;
		this.label = label;
	}

	public String toDot() {
		return key + " [shape=oval, label=\"" + type + "\n" + label + "\", style=filled, color=lightblue1]";
	}
}

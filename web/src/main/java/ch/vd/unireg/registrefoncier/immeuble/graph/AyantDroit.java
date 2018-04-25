package ch.vd.unireg.registrefoncier.immeuble.graph;

/**
 * Elément qui représente un ayant-droit dans le graphe des liens entre immeubles et ayants-droits.
 */
public class AyantDroit {
	protected final String type;
	protected final String name;
	protected final String label;

	public AyantDroit(String type, String name, String label) {
		this.type = type;
		this.name = name;
		this.label = label;
	}

	public String toDot() {
		return name + " [shape=oval, label=\"" + type + "\n" + label + "\", style=filled, color=lightblue1]";
	}
}

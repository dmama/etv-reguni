package ch.vd.unireg.registrefoncier.immeuble.graph;

/**
 * Elément qui représente un droit dans le graphe des liens entre immeubles et ayants-droits.
 */
public class Droit {
	private final Long id;
	private final String color;
	private final String style;
	private final String sourceKey;
	private final String destinationKey;
	private final String label;

	public Droit(Long id, String sourceKey, String destinationKey, String label, String color, String style) {
		this.id = id;
		this.color = color;
		this.sourceKey = sourceKey;
		this.destinationKey = destinationKey;
		this.label = label;
		this.style = style;
	}

	public String toDot() {
		return sourceKey + " -> " + destinationKey + " [id=\"link" + id + "\", label=\"" + label + "\", color=" + color + ", style=" + style + "]";
	}
}

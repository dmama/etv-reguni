package ch.vd.unireg.registrefoncier.immeuble.graph;

/**
 * Elément qui représente un droit dans le graphe des liens entre immeubles et ayants-droits.
 */
public class Droit {
	private final Long id;
	private final String color;
	private final String style;
	private final String source;
	private final String destination;
	private final String label;

	public Droit(Long id, String source, String destination, String label, String color, String style) {
		this.id = id;
		this.color = color;
		this.source = source;
		this.destination = destination;
		this.label = label;
		this.style = style;
	}

	public String toDot() {
		return source + " -> " + destination + " [id=\"link" + id + "\", label=\"" + label + "\", color=" + color + ", style=" + style + "]";
	}
}

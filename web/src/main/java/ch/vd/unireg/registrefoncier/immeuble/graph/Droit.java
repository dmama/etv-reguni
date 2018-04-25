package ch.vd.unireg.registrefoncier.immeuble.graph;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.registrefoncier.DroitRF;

/**
 * Elément qui représente un droit dans le graphe des liens entre immeubles et ayants-droits.
 */
public class Droit {
	private final String key;
	private final String color;
	private final String style;
	private final String sourceKey;
	private final String destinationKey;
	private final String label;

	public Droit(String key, String sourceKey, String destinationKey, String label, String color, String style) {
		this.key = key;
		this.color = color;
		this.sourceKey = sourceKey;
		this.destinationKey = destinationKey;
		this.label = label;
		this.style = style;
	}

	/**
	 * Construit la clé d'identification dans le graphe du droit spécifié.
	 *
	 * @param droit un droit
	 * @return la clé d'identification unique du droit
	 */
	public static String buildKey(@NotNull DroitRF droit) {
		return "link" + droit.getId();
	}

	/**
	 * Parse la clé spécifiée pour en extraire l'id du droit.
	 *
	 * @param key une clé
	 * @return l'id du droit <b>si</b> la clé est celle d'un droit; ou <b>null</b> s'il s'agit d'une autre clé.
	 */
	@Nullable
	public static Long parseKey(@NotNull String key) {
		if (key.startsWith("link")) {
			return Long.valueOf(key.substring(4));
		}
		else {
			return null;    // pas un lien
		}
	}

	public String toDot() {
		return sourceKey + " -> " + destinationKey + " [id=\"" + key + "\", label=\"" + label + "\", color=" + color + ", style=" + style + "]";
	}
}

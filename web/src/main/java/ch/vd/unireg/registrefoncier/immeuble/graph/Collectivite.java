package ch.vd.unireg.registrefoncier.immeuble.graph;

/**
 * Elément qui représente une collectivité dans le graphe des liens entre immeubles et ayants-droits.
 */
public class Collectivite extends AyantDroit {
	public Collectivite(String name, String label) {
		super("COLL", name, label);
	}
}

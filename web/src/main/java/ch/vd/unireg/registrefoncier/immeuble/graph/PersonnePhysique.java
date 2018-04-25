package ch.vd.unireg.registrefoncier.immeuble.graph;

/**
 * Elément qui représente une personne physique dans le graphe des liens entre immeubles et ayants-droits.
 */
public class PersonnePhysique extends AyantDroit {
	public PersonnePhysique(String name, String label) {
		super("PP", name, label);
	}
}

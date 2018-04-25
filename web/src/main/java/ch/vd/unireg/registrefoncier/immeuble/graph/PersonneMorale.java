package ch.vd.unireg.registrefoncier.immeuble.graph;

/**
 * Elément qui représente une personne morale dans le graphe des liens entre immeubles et ayants-droits.
 */
public class PersonneMorale extends AyantDroit {
	public PersonneMorale(String name, String label) {
		super("PM", name, label);
	}
}

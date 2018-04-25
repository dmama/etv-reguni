package ch.vd.unireg.registrefoncier.immeuble.graph;

/**
 * Elément qui représente une personne morale dans le graphe des liens entre immeubles et ayants-droits.
 */
public class PersonneMorale extends AyantDroit {
	public PersonneMorale(String key, String label) {
		super(key, "PM", label);
	}
}

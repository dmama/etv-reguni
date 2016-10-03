package ch.vd.uniregctb.evenement.organisation;

import java.util.Comparator;

import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Comparateur pour ordonner les evenements dans leur ordre de traitement:
 *  1. Les plus anciens en premier
 *  2. Puis ceux ayant une origine de plus haute pondération (SIFISC-21128: faire passer les événements FOSC avant les autres)
 *  3. Puis par identifiant croissant
 */
public class EvenementOrganisationComparator implements Comparator<EvenementOrganisation> {

	@Override
	public int compare(EvenementOrganisation o1, EvenementOrganisation o2) {
		int comp = o1.getDateEvenement().compareTo(o2.getDateEvenement());
		if (comp == 0) {
			/* SIFISC-21128: Faire passer devant les événements FOSC datés du même jour pour éviter que les arrivées HC ne partent en erreur. */
			final TypeEvenementOrganisation.Origine origine2 = o2.getType().getOrigine();
			comp = origine2.getPonderation().compareTo(o1.getType().getOrigine().getPonderation());
			if (comp == 0) {
				comp = (o1.getId() < o2.getId() ? -1 : (o1.getId() > o2.getId() ? 1 : 0));
			}
		}
		return comp;
	}
}

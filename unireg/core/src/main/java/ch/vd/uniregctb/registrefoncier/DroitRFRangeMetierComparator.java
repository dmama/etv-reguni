package ch.vd.uniregctb.registrefoncier;

import java.util.Comparator;

import ch.vd.registre.base.date.DateRangeComparator;

/**
 * Comparateur de droits RF qui utilise les critères suivants pour les ordonner les valeurs :
 * <ul>
 *     <li>les dates métier</li>
 *     <li>le type de droit (droit de propriété / servitude)</li>
 *     <li>le nombre d'ayants-droits</li>
 *     <li>le nombre d'immeubles</li>
 * </ul>
 */
public class DroitRFRangeMetierComparator implements Comparator<DroitRF> {
	@Override
	public int compare(DroitRF o1, DroitRF o2) {
		int c = DateRangeComparator.compareRanges(o1.getRangeMetier(), o2.getRangeMetier());
		if (c != 0) {
			return c;
		}
		c = o1.getTypeDroit().compareTo(o2.getTypeDroit());
		if (c != 0) {
			return c;
		}
		// TODO (msi) peut-être faire un comparaison plus fine...
		c = Integer.compare(o1.getAyantDroitList().size(), o2.getAyantDroitList().size());
		if (c != 0) {
			return c;
		}
		c = Integer.compare(o1.getImmeubleList().size(), o2.getImmeubleList().size());
		if (c != 0) {
			return c;
		}
		return c;
	}
}

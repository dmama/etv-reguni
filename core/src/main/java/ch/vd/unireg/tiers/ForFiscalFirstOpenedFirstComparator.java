package ch.vd.unireg.tiers;

import java.util.Comparator;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * @author Raphaël Marmier, 2015-12-21
 */
public class ForFiscalFirstOpenedFirstComparator implements Comparator<ForFiscal> {

	/**
	 * Trie deux for fiscaux de manière à obtenir en premier les fors répondant aux critères suivants:
	 * 1. Le for commence plus tot
	 * 2. Le for est un for principal
	 * 3. Le for fini plus tot
	 * 4. Le no OFS de la commune du for est plus petit.
	 * @param o1 Le premier for à comparer
	 * @param o2 Le second for à comparer
	 * @return le résultat de la comparaison
	 */
	public int compare(ForFiscal o1, ForFiscal o2) {
		if(o1 == null && o2 == null) {
			return 0;
		} else if(o1 == null) {
			return -1;
		} else if(o2 == null) {
			return 1;
		} else {
			// On utilise les dates de fin en priorité dans ce comparateur
			RegDate d1 = o1.getDateDebut();
			RegDate d2 = o2.getDateDebut();
			RegDate f1 = o1.getDateFin();
			RegDate f2 = o2.getDateFin();

			// Une date de début nulle signifie un for encore ouvert, qui doit être considéré plus tardif que n'importe quelle date contemporaine.
			if (RegDateHelper.isBefore(d1, d2, NullDateBehavior.LATEST)) {
				return -1;
			} else if (RegDateHelper.isAfter(d1, d2, NullDateBehavior.LATEST)) {
				return 1;
			} else {
				// On présente en priorité les fors principaux, à dates de commencement égales. On va ici contre l'ordre ascendant
				if (o1 instanceof ForFiscalPrincipal && ! (o2 instanceof ForFiscalPrincipal)) {
					return -1;
				}
				if(!(o1 instanceof ForFiscalPrincipal) && o2 instanceof ForFiscalPrincipal) {
					return 1;
				}

				if (RegDateHelper.isBefore(f1, f2, NullDateBehavior.LATEST)) {
					return -1;
				} else if (RegDateHelper.isAfter(f1, f2, NullDateBehavior.LATEST)) {
					return 1;
				}
				else { // En dernier recourt, on compare les numéro OFS pour obtenir un ordre consistent.
					return o1.getNumeroOfsAutoriteFiscale().compareTo(o2.getNumeroOfsAutoriteFiscale());
				}
			}
		}
	}
}

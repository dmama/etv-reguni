package ch.vd.uniregctb.tiers;

import java.util.Comparator;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2015-12-21
 */
public class ForFiscalFirstOpenedFirstComparator implements Comparator<ForFiscal> {
	private static final long serialVersionUID = 1L;

	// Date arbitraire dans le futur pour "comparer" les fors ouverts.
	private static final int END_EPOCH = 3000;

	public int compare(ForFiscal o1, ForFiscal o2) {
		return compareRanges(o1, o2);
	}

	private static int compareRanges(ForFiscal o1, ForFiscal o2) {
		if(o1 == null && o2 == null) {
			return 0;
		} else if(o1 == null) {
			return -1;
		} else if(o2 == null) {
			return 1;
		} else {
			RegDate d1 = o1.getDateDebut();
			RegDate d2 = o2.getDateDebut();
			// Une date de fin nulle signifie un for encore ouvert, qui doit être considéré plus tardif que n'importe quelle date contemporaine.
			if (d1 == null) {
				d1 = RegDate.get(END_EPOCH);
			}
			if (d2 == null) {
				d2 = RegDate.get(END_EPOCH);
			}
			if((d1 != null || d2 != null) && (d1 == null || !d1.equals(d2))) {
				if(d1 == null) {
					return -1;
				}

				if(d2 == null) {
					return 1;
				}
			} else {
				// On présente en priorité les fors principaux, à dates de commencement égales. On va ici contre l'ordre ascendant
				if (o1 instanceof ForFiscalPrincipal) {
					return -1;
				}
				if(o2 instanceof ForFiscalPrincipal) {
					return 1;
				}
				d1 = o1.getDateFin();
				d2 = o2.getDateFin();
				// On peut comparer directement, car les RegDate ont une et une seule instance par valeur
				if(d1 == d2) { // En dernier recourt, on compare les numéro OFS pour obtenir un ordre consistent.
					return o1.getNumeroOfsAutoriteFiscale().compareTo(o2.getNumeroOfsAutoriteFiscale()); // En priorité les numéros de commune élevés. On va ici contre l'ordre ascendant
				}
				if(d1 == null) {
					return 1;
				}
				if(d2 == null) {
					return -1;
				}
			}

			return d1.compareTo(d2);
		}
	}
}

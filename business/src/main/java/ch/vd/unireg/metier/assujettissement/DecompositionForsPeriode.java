package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Décomposition des fors d'un contribuable par type sur une période donnée.
 */
public class DecompositionForsPeriode extends DecompositionFors {

	public DecompositionForsPeriode(Contribuable contribuable, RegDate debut, RegDate fin) {
		super(contribuable, debut, fin);
	}

	public DecompositionForsPeriode(Contribuable contribuable, DateRange periode) {
		super(contribuable, periode.getDateDebut(), periode.getDateFin());
	}
}
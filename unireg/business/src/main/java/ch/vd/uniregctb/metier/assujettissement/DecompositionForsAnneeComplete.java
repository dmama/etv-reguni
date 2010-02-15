package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Décomposition des fors d'un contribuable par type sur une année complète.
 */
public class DecompositionForsAnneeComplete extends DecompositionFors {

	/** Année considérée */
	public final int annee;

	public DecompositionForsAnneeComplete(Contribuable contribuable, int annee) {
		super(contribuable, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
		this.annee = annee;
	}
}

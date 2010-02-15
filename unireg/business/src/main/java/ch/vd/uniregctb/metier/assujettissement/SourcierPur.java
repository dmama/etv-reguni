package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Assujettissement de type sourcier pur; c'est-à-dire pour un contribuable imposé à la source, et qui ne remplit donc pas de déclaration
 * d'impôt (par opposition au sourcier mixte).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SourcierPur extends Sourcier {

	public SourcierPur(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin,
			DecompositionFors fors) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, fors);
	}

	public SourcierPur(SourcierPur courant, SourcierPur suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Imposition à la source";
	}

	public DateRange collate(DateRange next) {
		return new SourcierPur(this, (SourcierPur) next);
	}
}

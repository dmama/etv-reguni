package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Assujettissement de type sourcier mixte; c'est-à-dire pour un contribuable imposé à la source mais qui remplit une déclaration d'impôt
 * car :
 * <ul>
 * <li>il possède un revenu suffisamment élevé, ou</li>
 * <li>il est en ménage avec un contribuable Suisse ou permis C</li>
 * </ul>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SourcierMixte extends Sourcier {

	public SourcierMixte(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin,
			DecompositionFors fors) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, fors);
	}

	public SourcierMixte(SourcierMixte courant, SourcierMixte suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Imposition mixte";
	}

	public DateRange collate(DateRange next) {
		return new SourcierMixte(this, (SourcierMixte) next);
	}
}

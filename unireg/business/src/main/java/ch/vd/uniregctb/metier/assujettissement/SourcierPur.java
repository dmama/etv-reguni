package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type sourcier pur; c'est-à-dire pour un contribuable imposé à la source, et qui ne remplit donc pas de déclaration
 * d'impôt (par opposition au sourcier mixte).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SourcierPur extends Sourcier {

	public SourcierPur(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin, TypeAutoriteFiscale typeAutoriteFiscale) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, typeAutoriteFiscale);
	}

	public SourcierPur(SourcierPur courant, SourcierPur suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Imposition à la source";
	}

	@Override
	public DateRange collate(DateRange next) {
		return new SourcierPur(this, (SourcierPur) next);
	}

	@Override
	public String toString() {
		return "SourcierPur(" + getDateDebut() + " - " + getDateFin() + ")";
	}
}

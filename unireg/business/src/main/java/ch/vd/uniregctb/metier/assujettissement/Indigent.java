package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Assujettissement de type indigent; c'est-à-dire pour un contribuable domicilié dans une commune vaudoise, mais n'ayant pas de revenu ni
 * fortune personnelle et au bénéfice d'une décision de l'ACI l'exemptant de déclaration d'impôt.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Indigent extends Assujettissement {

	public Indigent(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin);
	}

	private Indigent(Indigent courant, Indigent suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Indigent";
	}

	@Override
	public DateRange collate(DateRange next) {
		return new Indigent(this, (Indigent) next);
	}

	@Override
	public String toString() {
		return "Indigent(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}

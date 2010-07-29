package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Assujettissement à la dépense; c'est-à-dire pour un contribuable étranger, domicilé dans le canton de Vaud mais sans activité lucrative
 * sur territoire Suisse, et qui a négocié un forfait d'imposition avec l'ACI.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class VaudoisDepense extends Assujettissement {

	public VaudoisDepense(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin);
	}

	private VaudoisDepense(VaudoisDepense courant, VaudoisDepense suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Imposition d'après la dépense";
	}

	public DateRange collate(DateRange next) {
		return new VaudoisDepense(this, (VaudoisDepense) next);
	}

	@Override
	public String toString() {
		return "VaudoisDepense(" + getDateDebut() + " - " + getDateFin() + ")";
	}
}

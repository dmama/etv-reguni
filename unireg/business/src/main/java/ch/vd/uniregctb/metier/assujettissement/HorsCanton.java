package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Assujettissement de type hors Canton; c'est-à-dire pour un contribuable domicilié dans un canton autre que le canton de vaud, et
 * possédant un ou plusieurs fors secondaires actifs dans une commune vaudoise.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class HorsCanton extends Assujettissement {

	public HorsCanton(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin);
	}

	private HorsCanton(HorsCanton courant, HorsCanton suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Imposition ordinaire HC";
	}

	@Override
	public DateRange collate(DateRange next) {
		return new HorsCanton(this, (HorsCanton) next);
	}

	@Override
	public String toString() {
		return "HorsCanton(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}

package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Période d'assujettissement de type diplomate Suisse; c'est-à-dire pour un diplomate de nationalité Suisse, basé à l'étranger mais
 * rattaché à une commune vaudoise (pour imposition à l'IFD).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DiplomateSuisse extends Assujettissement {

	public DiplomateSuisse(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin);
	}

	private DiplomateSuisse(DiplomateSuisse courant, DiplomateSuisse suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Diplomate Suisse";
	}

	@Override
	public DateRange collate(DateRange next) {
		return new DiplomateSuisse(this, (DiplomateSuisse) next);
	}

	@Override
	public String toString() {
		return "DiplomateSuisse(" + getDateDebut() + " - " + getDateFin() + ")";
	}
}

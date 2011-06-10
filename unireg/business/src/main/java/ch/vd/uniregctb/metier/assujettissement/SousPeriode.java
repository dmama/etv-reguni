package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.MotifFor;

public class SousPeriode implements DateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MotifFor motifFractDebut;
	private final MotifFor motifFractFin;

	public SousPeriode(RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.motifFractDebut = motifFractDebut;
		this.motifFractFin = motifFractFin;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public MotifFor getMotifFractDebut() {
		return motifFractDebut;
	}

	public MotifFor getMotifFractFin() {
		return motifFractFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateFin, date, NullDateBehavior.LATEST);
	}

}

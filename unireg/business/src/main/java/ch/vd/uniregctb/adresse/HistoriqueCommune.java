package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Commune;

/**
 * Décrit la présence dans une commune pendant une période
 */
public class HistoriqueCommune implements CollatableDateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Commune commune;

	public HistoriqueCommune(RegDate dateDebut, RegDate dateFin, Commune commune) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.commune = commune;
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next) && next instanceof HistoriqueCommune && sameCommune(commune, ((HistoriqueCommune) next).commune);
	}

	@SuppressWarnings({"SimplifiableIfStatement"})
	private static boolean sameCommune(Commune commune1, Commune commune2) {
		if (commune1 == commune2) {
			return true;
		}
		else if (commune1 == null || commune2 == null) {
			return false;
		}
		else {
			return commune1.getNoOFSEtendu() == commune2.getNoOFSEtendu();
		}
	}

	public DateRange collate(DateRange next) {
		return new HistoriqueCommune(dateDebut, next.getDateFin(), commune);
	}

	public Commune getCommune() {
		return commune;
	}
}

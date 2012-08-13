package ch.vd.uniregctb.tiers.timeline;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Représente la première cellule d'une ligne
 */
@SuppressWarnings("UnusedDeclaration")
public class TimelineRange implements DateRange {

	private RegDate dateDebut;
	@Nullable
	private RegDate dateFin;

	private boolean multiYears;
	private int yearSpan;

	public TimelineRange(DateRange range) {
		this.dateDebut = range.getDateDebut();
		this.dateFin = range.getDateFin();
		this.yearSpan = 1;
		this.multiYears = (dateFin == null || dateDebut.year() != dateFin.year());
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * @return vrai si la date de début et de fin ne sont pas dans le même année.
	 */
	public boolean isMultiYears() {
		return multiYears;
	}

	/**
	 * @return le nombre de lignes suivantes qui possèdent la même année que la ligne courante + 1 (utilisé pour spanner l'année sur plusieurs lignes)
	 */
	public int getYearSpan() {
		return yearSpan;
	}

	public void setYearSpan(int yearSpan) {
		this.yearSpan = yearSpan;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Nullable
	public RegDate getDateFin() {
		return dateFin;
	}

	public void incYearspan() {
		yearSpan++;
	}

	public String getAnneeLabel() {
		return String.valueOf(dateDebut.year());
	}
}

package ch.vd.uniregctb.json;

import ch.vd.registre.base.date.RegDate;

// TODO (msi) : supprimer cette classe et la remplacer par une RegDate (qui est dorénavant gérée correctement en JSON grâce à la classe RegDateJsonSerializer)
@SuppressWarnings({"UnusedDeclaration"})
public class DateView {
	private int year;
	private int month;
	private int day;

	public static DateView get(RegDate date) {
		if (date == null) {
			return null;
		}
		return new DateView(date);
	}

	public DateView(RegDate date) {
		this.year = date.year();
		this.month = date.month();
		this.day = date.day();
	}

	public DateView(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}
}

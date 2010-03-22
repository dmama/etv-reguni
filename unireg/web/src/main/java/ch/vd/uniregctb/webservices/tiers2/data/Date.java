package ch.vd.uniregctb.webservices.tiers2.data;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Date spécifique à Unireg définissant le jour, le mois et l'année. La time-zone implicite à celle du canton de Vaud, et le calendrier est
 * grégorien.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Date", propOrder = {
		"year", "month", "day"
})
public class Date implements Comparable<Date> {

	/**
	 * Year on four digits (e.g. 1337, 2001, 2400, ...).
	 */
	@XmlElement(required = true)
	public int year;

	/**
	 * Month (1..12). In case of partial date, the value may be 0.
	 */
	@XmlElement(required = true)
	public int month;

	/**
	 * Day of the month (1..31). In case of partial date, the value is 0.
	 */
	@XmlElement(required = true)
	public int day;

	public Date() {
		this.year = 0;
		this.month = 0;
		this.day = 0;
	}

	public Date(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}

	public Date(java.util.Date date) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(date);

		this.year = cal.get(Calendar.YEAR);
		this.month = cal.get(Calendar.MONTH) + 1;
		this.day = cal.get(Calendar.DAY_OF_MONTH);
	}

	public Date(ch.vd.registre.base.date.RegDate date) {
		this.year = date.year();
		this.month = date.month();
		this.day = date.day();
	}

	public java.util.Date asJavaDate() {
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(Calendar.YEAR, this.year);
		cal.set(Calendar.MONTH, this.month - 1);
		cal.set(Calendar.DAY_OF_MONTH, this.day);
		return cal.getTime();
	}

	public static ch.vd.registre.base.date.RegDate asRegDate(Date date) {
		if (date == null) {
			return null;
		}
		return ch.vd.registre.base.date.RegDate.get(date.year, date.month, date.day);
	}

	public static java.util.Date asJavaDate(Date date) {
		if (date == null) {
			return null;
		}
		return date.asJavaDate();
	}

	/**
	 * @return la date au format "yyyyMMdd".
	 */
	public String asIndexString() {
		if (day == 0) {
			if (month == 0) {
				return String.format("%4d", year);
			}
			else {
				return String.format("%4d%02d", year, month);
			}
		}
		else
		{
			return String.format("%4d%02d%02d", year, month, day);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + day;
		result = prime * result + month;
		result = prime * result + year;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Date other = (Date) obj;
		return day == other.day && month == other.month && year == other.year;
	}

	public int compareTo(Date o) {
		if (this.year == o.year) {
			if (this.month == o.month) {
				return this.day - o.day;
			}
			else {
				return this.month - o.month;
			}
		}
		else {
			return this.year - o.year;
		}
	}

	@Override
	public String toString() {
		return "Date{" +
				"year=" + year +
				", month=" + month +
				", day=" + day +
				'}';
	}
}

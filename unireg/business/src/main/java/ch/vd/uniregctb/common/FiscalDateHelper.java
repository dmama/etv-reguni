package ch.vd.uniregctb.common;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Individu;

/**
 * Classe de gestion des particularité fiscales dans le traitement des dates.
 */
public abstract class FiscalDateHelper {

	public static final int AGE_MAJORITE = 18;// en années

	/**
	 * Class immutable qui représente un jour particulier (jour + mois).
	 * <p>
	 * Le mois s'exprime selon la même définition que la GregorianCalendar,
	 * c'est-à-dire sur la plage 0..11.
	 */
	private static class DayMonth {
		public DayMonth(int day, int month) {
			this.day = day;
			this.month = month;
		}

		public final int day; // 1..31
		public final int month; // 0..11

		public boolean isBefore(DayMonth right) {
			return this.month < right.month || (this.month == right.month && this.day < right.day);
		}

		public boolean isAfter(DayMonth right) {
			return this.month > right.month || (this.month == right.month && this.day > right.day);
		}
	}

	/**
	 * Jour auquel l'année fiscal se termine. Toutes les dates après ce jour et
	 * la fin de l'année civile sont considérées comme appartenant à l'année
	 * fiscale suivante.
	 * <p>
	 * Il s'agit donc du 20 décembre.
	 */
	private static final DayMonth FISCAL_YEAR_LAST_DAY = new DayMonth(20, Calendar.DECEMBER);

	/**
	 * Calcul et retourne l'année fiscale courante (= année civile courante,
	 * sauf pour la période du 21 au 31 décembre où l'année suivante est
	 * considérée).
	 *
	 * @return l'année sur 4 positions (p.e. 2001)
	 */
	public static int getAnneeCourante() {

		final Calendar cal = DateHelper.getCurrentCalendar();
		return getAnneeCourante(cal);
	}

	/**
	 * Calcul et retourne la date d'événement fiscal correcte pour une date donnée.
	 * <p>
	 * Si la date donnée tombe entre la fin de l'année fiscale et la fin de
	 * l'année civile, la date est décalée au début de l'année civile suivante.
	 * Dans les autres cas, la date reste inchangée.
	 *
	 * @param date
	 *            n'importe quelle date
	 * @return la date correcte à utiliser pour une année fiscale
	 */
	public static Date getDateEvenementFiscal(Date date) {
		Calendar dateEvenement = new GregorianCalendar();
		dateEvenement.setTime(date);

		final int mois = dateEvenement.get(Calendar.MONTH); // 0..11
		final int jour = dateEvenement.get(Calendar.DAY_OF_MONTH); // 1..31
		final DayMonth dayMonth = new DayMonth(jour, mois);

		if (dayMonth.isAfter(FISCAL_YEAR_LAST_DAY)) {
			// la date est après la fin de l'année fiscal -> pousse la date au
			// premier janvier de l'année suivante
			final int annee = dateEvenement.get(Calendar.YEAR);
			dateEvenement.set(Calendar.YEAR, annee + 1);
			dateEvenement.set(Calendar.MONTH, Calendar.JANUARY);
			dateEvenement.set(Calendar.DAY_OF_MONTH, 1);
			return dateEvenement.getTime();
		}
		else
			return date;
	}

	/**
	 * Calcul et retourne la date d'ouverture correcte d'un for fiscal.
	 * <p/>
	 * Si la date donnée tombe entre la fin de l'année fiscale et la fin de l'année civile, la date est décalée au début de l'année civile suivante. Dans les autres cas, la date reste inchangée.
	 *
	 * @param date une date d'ouverture de for fiscal
	 * @return la date correcte à utiliser pour ouvrir un for fiscal
	 */
	public static RegDate getDateOuvertureForFiscal(RegDate date) {

		final int mois = date.month() - 1; // 0..11
		final int jour = date.day(); // 1..31
		final DayMonth dayMonth = new DayMonth(jour, mois);

		if (dayMonth.isAfter(FISCAL_YEAR_LAST_DAY)) {
			// la date est après la fin de l'année fiscal -> pousse la date au
			// premier janvier de l'année suivante
			return RegDate.get(date.year() + 1, 1, 1);
		}
		else
		{
			return date;
		}
	}

	/**
	 * Calcul et retourne la date de fermeture correcte d'un for fiscal.
	 * <p/>
	 * Si la date donnée tombe entre la fin de l'année fiscale et la fin de l'année civile, la date est décalée à la fin de l'année civile courante. Dans les autres cas, la date reste inchangée.
	 *
	 * @param date une date de fermeture de for fiscal
	 * @return la date correcte à utiliser pour fermer un for fiscal
	 */
	public static RegDate getDateFermetureForFiscal(RegDate date) {
		// pour une date de fermeture n, la date d'ouverture du for suivant est n+1 : on évite de coder deux fois l'algorithme et on passe par 'getDateOuvertureForFiscal' 
		return getDateOuvertureForFiscal(date.getOneDayAfter()).getOneDayBefore();
	}

	/**
	 * Détermine si un individu est majeur à la RegDate spécifiée.
	 *
	 * @param individu
	 *            l'individu en question
	 * @param date
	 *            la RegDate à laquelle la vérification est faite
	 * @return vrai si l'individu est majeur (18 ans révolu), faux autrement.
	 */
	public static boolean isMajeurAt(Individu individu, RegDate date) {

		final RegDate naissance = individu.getDateNaissance();
		Assert.notNull(naissance, "L'individu " + individu.getNoTechnique() + " n'a pas de date de naissance" );
		return isMajeur(date, naissance);
	}

	protected static int getAnneeCourante(final Calendar cal) {
		final int annee = cal.get(Calendar.YEAR);
		final int mois = cal.get(Calendar.MONTH); // 0..11
		final int jour = cal.get(Calendar.DAY_OF_MONTH); // 1..31

		final DayMonth aujourdui = new DayMonth(jour, mois);
		if (aujourdui.isAfter(FISCAL_YEAR_LAST_DAY))
			return annee + 1;
		else
			return annee;
	}

	/**
	 * Vérifie que l'individu né à la date de naissance donnée est majeur à la date référence donnée.
	 *
	 * @param dateReference
	 *            date de référence
	 * @param dateNaissance
	 *            date de naissance de l'individu
	 * @return true si l'individu est majeur
	 */
	public static boolean isMajeur(Date dateReference, Date dateNaissance) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(dateReference);
		cal1.add(Calendar.YEAR, -AGE_MAJORITE);

		return !dateNaissance.after(cal1.getTime());
	}

	/**
	 * Vérifie que l'individu né à la date de naissance donnée est majeur à la date référence donnée.
	 * <p>
	 * Note: cette méthode <b>supporte</b> les dates partielles.
	 *
	 * @param dateReference
	 *            date de référence
	 * @param dateNaissance
	 *            date de naissance de l'individu
	 * @return true si l'individu est majeur
	 */
	public static boolean isMajeur(RegDate dateReference, RegDate dateNaissance) {
		final RegDate dateMajorite = dateNaissance.addYears(AGE_MAJORITE);
		return dateReference.isAfterOrEqual(dateMajorite);
	}

	/**
	 * Retourne la date de majorité d'un individu à partir de sa date de naissance.
	 * <p>
	 * Si la date de naissance est partielle au niveau du jour, le 1er jour du mois de majorité est retourné. Si la date de naissance est
	 * partielle au niveau du jour et du mois, le 1er janvier de l'année de majorité est retourné.
	 *
	 * @return la date de majorité (date non-partielle)
	 */
	public static RegDate getDateMajorite(RegDate dateNaissance) {
		if (dateNaissance.day() == RegDate.UNDEFINED) {
			if (dateNaissance.month() == RegDate.UNDEFINED) {
				dateNaissance = RegDate.get(dateNaissance.year(), 1, 1);
			}
			else {
				dateNaissance = RegDate.get(dateNaissance.year(), dateNaissance.month(), 1);
			}
		}
		return dateNaissance.addYears(AGE_MAJORITE);
	}

	/**
	 * Détermine si la date appartient à une période fiscale échue.
	 *
	 * @param date
	 *            la date à vérifier.
	 * @return <code>true</code> si la période contenant la date est échue; <code>false</code> sinon.
	 */
	//UNIREG-] la période échue correspond à n-2 pour les tâches de contrôle de dossier notamment
	public static boolean isEnPeriodeEchue(RegDate date) {
		return date.year() <= RegDate.get().year() - 2;
	}
}

package ch.vd.unireg.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.type.DayMonth;

/**
 * Classe de gestion des particularité fiscales dans le traitement des dates.
 */
public abstract class FiscalDateHelper {

	public static final int AGE_MAJORITE = 18;// en années

	/**
	 * Jour auquel l'année fiscal se termine. Toutes les dates après ce jour et
	 * la fin de l'année civile sont considérées comme appartenant à l'année
	 * fiscale suivante.
	 * <p>
	 * Il s'agit donc du 20 décembre.
	 */
	private static final DayMonth FISCAL_YEAR_LAST_DAY = DayMonth.get(12, 20);

	/**
	 * Calcul et retourne l'année fiscale courante (= année civile courante,
	 * sauf pour la période du 21 au 31 décembre où l'année suivante est
	 * considérée).
	 *
	 * @return l'année sur 4 positions (p.e. 2001)
	 */
	public static int getAnneeCourante() {
		final RegDate now = RegDate.get();
		return getAnneeFiscale(now);
	}

	protected static int getAnneeFiscale(RegDate date) {
		final DayMonth aujourdhui = DayMonth.get(date);
		return aujourdhui.compareTo(FISCAL_YEAR_LAST_DAY) > 0 ? date.year() + 1 : date.year();
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
		final DayMonth dayMonth = DayMonth.get(date);
		// si la date est après la fin de l'année fiscale, on pousse la date au premier janvier de l'année suivante
		return dayMonth.compareTo(FISCAL_YEAR_LAST_DAY) > 0 ? RegDate.get(date.year() + 1, 1, 1) : date;
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
		if (naissance == null) {
			throw new IllegalArgumentException("L'individu " + individu.getNoTechnique() + " n'a pas de date de naissance");
		}
		return isMajeur(date, naissance);
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
		return getDateComplete(dateNaissance).addYears(AGE_MAJORITE);
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

	/**
	 * @param date date potentiellement partielle
	 * @return la date complète correspondante (si la date est déjà complète, c'est vite vu) en remplaçant les données manquantes par des 1
	 */
	public static RegDate getDateComplete(RegDate date) {
		if (date.day() == RegDate.UNDEFINED) {
			if (date.month() == RegDate.UNDEFINED) {
				return RegDate.get(date.year(), 1, 1);
			}
			return RegDate.get(date.year(), date.month(), 1);
		}
		return date;
	}

	/**
	 * Permet de calculer la date de fin minimal pour qu'une décision Aci ait encore une influence sur la fiscalité d'un contribuable
	 * elle correspond au 31.12 de l'année n - 2, si n est l'année courante
	 *
	 * @return la date calculée
	 */
	public static RegDate getDateMinimalePourEffetDecisionAci() {
		final int annee = getAnneeCourante() - 2;
		return RegDate.get(annee, 12, 31);
	}

	/**
	 * @param range le range (fermé à gauche et à droite) concerné
	 * @return le nombre de jours fiscaux dans le range (360 jours / an, 30 jours par mois...)
	 * @throws NullPointerException si le range est <code>null</code> ou ouvert à gauche ou à droite
	 * @throws IllegalArgumentException si le range est invalide (= à l'envers...)
	 */
	public static int getLongueurEnJours(@NotNull DateRange range) {
		final RegDate dateDebut = range.getDateDebut();
		final RegDate dateFin = range.getDateFin();
		return getLongueurEnJours(dateDebut, dateFin);
	}

	/**
	 * @param dateDebut la date du premier jour à considérer
	 * @param dateFin la date (incluse) du dernier jour à considérer
	 * @return le nombre de jours fiscaux dans le range (360 jours / an, 30 jours par mois...)
	 * @throws NullPointerException si l'une des deux dates est <code>null</code>
	 * @throws IllegalArgumentException si le range est invalide (= dates à l'envers...)
	 */
	public static int getLongueurEnJours(@NotNull RegDate dateDebut, @NotNull RegDate dateFin) {
		DateRangeHelper.assertValidRange(dateDebut, dateFin);

		final boolean finFinDeMois = dateFin == dateFin.getLastDayOfTheMonth();
		final boolean debutDebutDeMois = dateDebut.day() == 1;

		// cas simple -> de mois en mois
		if (finFinDeMois && debutDebutDeMois) {
			return 360 * (dateFin.year() - dateDebut.year()) + 30 * (dateFin.month() - dateDebut.month() + 1);
		}

		// cas moins simples...

		// durée à l'intérieur d'un mois
		if (dateFin.year() == dateDebut.year() && dateFin.month() == dateDebut.month()) {
			return dateFin.day() - dateDebut.day() + 1;             // n'oublions pas que les deux jours sont compris dans la période
		}

		// pas dans le même mois ... on se ramène au cas simple
		final boolean debutFinDeMois = dateDebut.getLastDayOfTheMonth() == dateDebut;
		final int debutVersFinMois;
		if (debutFinDeMois) {
			debutVersFinMois = 1;
		}
		else if (debutDebutDeMois) {
			debutVersFinMois = 30;
		}
		else {
			debutVersFinMois = 30 - dateDebut.day() + 1;
		}
		final int finDepuisDebutMois = finFinDeMois ? 30 : dateFin.day();
		final RegDate nouveauDebut = dateDebut.getLastDayOfTheMonth().getOneDayAfter();
		final RegDate nouvelleFin = RegDate.get(dateFin.year(), dateFin.month(), 1).getOneDayBefore();
		if (nouveauDebut.isBeforeOrEqual(nouvelleFin)) {
			// appel récursif en mode "de mois en mois"
			return debutVersFinMois + finDepuisDebutMois + getLongueurEnJours(nouveauDebut, nouvelleFin);
		}
		else {
			return debutVersFinMois + finDepuisDebutMois;
		}
	}

	/**
	 * @param range le range (potentiellement ouvert ou mal fagotté/à l'envers) concerné
	 * @return le nombre de jours fiscaux dans le range (360 jours / an, 30 jours par mois...), ou <code>null</code> si le calcul n'est pas possible (infini, dates à l'envers)
	 */
	@Nullable
	public static Integer getLongueurEnJoursOuNullSiPasPossible(@NotNull DateRange range) {
		if (range.getDateDebut() == null || range.getDateFin() == null || range.getDateDebut().isAfter(range.getDateFin())) {
			return null;
		}
		return getLongueurEnJours(range);
	}
}

package ch.vd.unireg.declaration.source;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;

/**
 * Stratégies qui savent dire s'il est temps d'envoyer une LR
 */
public enum SendingTimeStrategy {

	/**
	 * On peut envoyer la LR dès que sa période est commencée
	 */
	PERIOD_BEGIN {
		@Override
		public boolean isRightMoment(RegDate referenceDate, DateRange lrPeriod, PeriodiciteDecompte periodicite, PeriodeDecompte periodeDecompte) {
			return referenceDate.isAfterOrEqual(lrPeriod.getDateDebut());
		}
	},

	/**
	 * On peut envoyer la LR dès que la moitié de la période (en mois) est passée, par exemple :
	 * <ul>
	 *     <li>périodicié mensuelle, dès la fin du mois&nbsp;;</li>
	 *     <li>périodicité trimestrielle, dès la fin du deuxième mois&nbsp;;</li>
	 *     <li>périodicité semestrielle, dès la fin du troisième mois&nbsp;;</li>
	 *     <li>périodicité annuelle, dès la fin du sixième mois.</li>
	 * </ul>
	 */
	PERIOD_MIDDLE {
		@Override
		public boolean isRightMoment(RegDate referenceDate, DateRange lrPeriod, PeriodiciteDecompte periodicite, PeriodeDecompte periodeDecompte) {
			if (periodicite == PeriodiciteDecompte.UNIQUE) {
				final DateRange range = periodeDecompte.getPeriodeCourante(lrPeriod.getDateDebut());
				if (lrPeriod.getDateFin() != range.getDateFin()) {
					throw new IllegalArgumentException();
				}
				if (lrPeriod.getDateDebut() != range.getDateDebut()) {
					throw new IllegalArgumentException();
				}
			}
			else {
				if (lrPeriod.getDateFin() != periodicite.getFinPeriode(lrPeriod.getDateDebut())) {
					throw new IllegalArgumentException();
				}
				if (lrPeriod.getDateDebut() != periodicite.getDebutPeriode(lrPeriod.getDateFin())) {
					throw new IllegalArgumentException();
				}
			}

			final int length = lrPeriod.getDateFin().month() - lrPeriod.getDateDebut().month() + 1;     // en mois
			final int middle = length / 2 + length % 2;      // 1 -> 1; 2 -> 1; 3 -> 2; ... ; 6 -> 3; ...
			final RegDate threshold = lrPeriod.getDateDebut().addMonths(middle).getOneDayBefore();
			return referenceDate.isAfterOrEqual(threshold);
		}
	},

	/**
	 * On peut envoyée la LR dès qu'elle est complètement passée
	 */
	PERIOD_END {
		@Override
		public boolean isRightMoment(RegDate referenceDate, DateRange lrPeriod, PeriodiciteDecompte periodicite, PeriodeDecompte periodeDecompte) {
			return referenceDate.isAfterOrEqual(lrPeriod.getDateFin());
		}
	};

	/**
	 * @param referenceDate date de référence de l'exécution du batch d'envoi des LR (= fin du mois courant)
	 * @param lrPeriod période de la LR potentiellement envoyée maintenant
	 * @param periodicite la périodicité utilisée pour générer la LR
	 * @return <code>true</code> si la LR peut être imprimée
	 */
	public abstract boolean isRightMoment(RegDate referenceDate, DateRange lrPeriod, PeriodiciteDecompte periodicite, PeriodeDecompte periodeDecompte);

}

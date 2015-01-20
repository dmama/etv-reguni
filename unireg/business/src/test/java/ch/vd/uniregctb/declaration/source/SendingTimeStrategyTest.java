package ch.vd.uniregctb.declaration.source;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class SendingTimeStrategyTest extends WithoutSpringTest {

	@Test
	public void testDebutUnique() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_BEGIN;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.UNIQUE;
		final RegDate debut = date(2012, 1, 1);
		for (PeriodeDecompte periode : PeriodeDecompte.values()) {
			final DateRange range = periode.getPeriodeCourante(debut);
			final RegDate threshold = range.getDateDebut();
			for (RegDate date = date(2011, 12, 1); date.isBefore(date(2013, 2, 1)); date = date.getOneDayAfter()) {
				final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, periode);
				Assert.assertEquals(periode.name() + "/" + RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
			}
		}
	}

	@Test
	public void testDebutMensuel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_BEGIN;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.MENSUEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateDebut();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testDebutTrimestriel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_BEGIN;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.TRIMESTRIEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateDebut();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testDebutSemestriel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_BEGIN;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.SEMESTRIEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateDebut();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testDebutAnnuel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_BEGIN;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.ANNUEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateDebut();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testFinUnique() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_END;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.UNIQUE;
		final RegDate debut = date(2012, 1, 1);
		for (PeriodeDecompte periode : PeriodeDecompte.values()) {
			final DateRange range = periode.getPeriodeCourante(debut);
			final RegDate threshold = range.getDateFin();
			for (RegDate date = date(2011, 12, 1); date.isBefore(date(2013, 2, 1)); date = date.getOneDayAfter()) {
				final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, periode);
				Assert.assertEquals(periode.name() + "/" + RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
			}
		}
	}

	@Test
	public void testFinMensuel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_END;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.MENSUEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateFin();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testFinTrimestriel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_END;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.TRIMESTRIEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateFin();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testFinSemestriel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_END;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.SEMESTRIEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateFin();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testFinAnnuel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_END;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.ANNUEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateFin();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testMilieuUnique() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_MIDDLE;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.UNIQUE;
		final RegDate debut = date(2012, 1, 1);
		for (PeriodeDecompte periode : PeriodeDecompte.values()) {
			final DateRange range = periode.getPeriodeCourante(debut);
			final int month = (range.getDateDebut().month() + range.getDateFin().month()) / 2;
			final RegDate threshold = RegDate.get(range.getDateDebut().year(), month, 1).getLastDayOfTheMonth();
			for (RegDate date = date(2011, 12, 1); date.isBefore(date(2013, 2, 1)); date = date.getOneDayAfter()) {
				final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, periode);
				Assert.assertEquals(periode.name() + "/" + RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
			}
		}
	}

	@Test
	public void testMilieuMensuel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_MIDDLE;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.MENSUEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = range.getDateFin();
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testMilieuTrimestriel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_MIDDLE;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.TRIMESTRIEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = date(2012, 2, 29);
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testMilieuSemestriel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_MIDDLE;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.SEMESTRIEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = date(2012, 3, 31);
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}

	@Test
	public void testMilieuAnnuel() throws Exception {
		final SendingTimeStrategy strategy = SendingTimeStrategy.PERIOD_MIDDLE;
		final PeriodiciteDecompte periodicite = PeriodiciteDecompte.ANNUEL;
		final RegDate debut = date(2012, 1, 1);
		final DateRange range = new DateRangeHelper.Range(periodicite.getDebutPeriode(debut), periodicite.getFinPeriode(debut));
		final RegDate threshold = date(2012, 6, 30);
		for (RegDate date = date(2011, 12, 1) ; date.isBefore(date(2013, 2, 1)) ; date = date.getOneDayAfter()) {
			final boolean isRightMoment = strategy.isRightMoment(date, range, periodicite, null);
			Assert.assertEquals(RegDateHelper.dateToDisplayString(date), date.isAfterOrEqual(threshold), isRightMoment);
		}
	}
}

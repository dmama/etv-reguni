package ch.vd.uniregctb.declaration.ordinaire.pm;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class EnvoiDIsPMJobTest extends WithoutSpringTest {

	@Test
	public void testMonthAtEndOfTrimester() throws Exception {
		Assert.assertEquals(3, EnvoiDIsPMJob.getMonthAtEndOfTrimester(1));
		Assert.assertEquals(3, EnvoiDIsPMJob.getMonthAtEndOfTrimester(2));
		Assert.assertEquals(3, EnvoiDIsPMJob.getMonthAtEndOfTrimester(3));
		Assert.assertEquals(6, EnvoiDIsPMJob.getMonthAtEndOfTrimester(4));
		Assert.assertEquals(6, EnvoiDIsPMJob.getMonthAtEndOfTrimester(5));
		Assert.assertEquals(6, EnvoiDIsPMJob.getMonthAtEndOfTrimester(6));
		Assert.assertEquals(9, EnvoiDIsPMJob.getMonthAtEndOfTrimester(7));
		Assert.assertEquals(9, EnvoiDIsPMJob.getMonthAtEndOfTrimester(8));
		Assert.assertEquals(9, EnvoiDIsPMJob.getMonthAtEndOfTrimester(9));
		Assert.assertEquals(12, EnvoiDIsPMJob.getMonthAtEndOfTrimester(10));
		Assert.assertEquals(12, EnvoiDIsPMJob.getMonthAtEndOfTrimester(11));
		Assert.assertEquals(12, EnvoiDIsPMJob.getMonthAtEndOfTrimester(12));
	}

	@Test
	public void testEndOfTrimester() throws Exception {
		for (RegDate date = date(2010, 1, 1) ; date.isBefore(date(2015, 12, 31)) ; date = date.getOneDayAfter()) {
			final int expectedYear = date.year();
			final int expectedMonth;
			switch (date.month()) {
			case 1:
			case 2:
			case 3:
				expectedMonth = 3;
				break;
			case 4:
			case 5:
			case 6:
				expectedMonth = 6;
				break;
			case 7:
			case 8:
			case 9:
				expectedMonth = 9;
				break;
			case 10:
			case 11:
			case 12:
				expectedMonth = 12;
				break;
			default:
				throw new IllegalArgumentException("Date avec un mois bizarre : " + date);
			}

			final RegDate expectedDate = date(expectedYear, expectedMonth, 1).getLastDayOfTheMonth();
			Assert.assertEquals("Référence : " + date, expectedDate, EnvoiDIsPMJob.getEndOfTrimester(date));
		}
	}
}

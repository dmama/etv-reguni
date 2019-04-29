package ch.vd.unireg.common;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2016-01-06, <raphael.marmier@vd.ch>
 */
public class GentilDateRangeExtendedAdapterCallbackTest extends WithoutSpringTest {

	@Test
	public void testDuplicate() throws Exception {
		GentilDateRangeExtendedAdapterCallback<TestDateRange> cb = new GentilDateRangeExtendedAdapterCallback<>();
		TestDateRange result = cb.duplicate(new TestDateRange(date(2015, 6, 25), date(2015, 12, 31)));
		Assert.assertEquals(date(2015, 6, 25), result.getDateDebut());
		Assert.assertEquals(date(2015, 12, 31), result.getDateFin());
	}

	@Test
	public void testAdaptAvecSource() throws Exception {
		GentilDateRangeExtendedAdapterCallback<TestDateRange> cb = new GentilDateRangeExtendedAdapterCallback<>();
		TestDateRange sourceDebut = new TestDateRange(date(2015, 7, 5), date(2015, 8, 7));
		TestDateRange sourceFin = new TestDateRange(date(2015, 8, 8), date(2015, 9, 23));

		{
			TestDateRange result = cb.adapt(new TestDateRange(date(2015, 6, 25), date(2015, 12, 31)), date(2015, 7, 5), sourceDebut, date(2015, 9, 23), sourceFin);
			Assert.assertEquals(date(2015, 7, 5), result.getDateDebut());
			Assert.assertEquals(date(2015, 9, 23), result.getDateFin());
		}
		{
			TestDateRange result = cb.adapt(new TestDateRange(date(2015, 6, 25), date(2015, 12, 31)), date(2015, 7, 5), sourceDebut, null, null);
			Assert.assertEquals(date(2015, 7, 5), result.getDateDebut());
			Assert.assertEquals(date(2015, 12, 31), result.getDateFin());
		}
		{
			TestDateRange result = cb.adapt(new TestDateRange(date(2015, 6, 25), date(2015, 12, 31)), null, null, null, null);
			Assert.assertEquals(date(2015, 6, 25), result.getDateDebut());
			Assert.assertEquals(date(2015, 12, 31), result.getDateFin());
		}

	}

	@Test
	public void testAdapt() throws Exception {
		GentilDateRangeExtendedAdapterCallback<TestDateRange> cb = new GentilDateRangeExtendedAdapterCallback<>();
		TestDateRange result = cb.adapt(new TestDateRange(date(2015, 6, 25), date(2015, 12, 31)), date(2015, 7, 5), date(2015, 9, 23));
		Assert.assertEquals(date(2015, 7, 5), result.getDateDebut());
		Assert.assertEquals(date(2015, 9, 23), result.getDateFin());
	}

	/*
	 * Classe dédiée au test
	 */
	static class TestDateRange implements DateRange, Duplicable<TestDateRange>,Rerangeable<TestDateRange> {
		private RegDate dateDebut;
		private RegDate dateFin;

		TestDateRange(RegDate dateDebut, RegDate dateFin) {
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}

		@Override
		public TestDateRange rerange(DateRange range) {
			return new TestDateRange(range.getDateDebut(), range.getDateFin());
		}

		@Override
		public TestDateRange duplicate() {
			return new TestDateRange(this.dateDebut, this.dateFin);
		}
	}
}
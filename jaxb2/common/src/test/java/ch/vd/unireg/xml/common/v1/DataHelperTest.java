package ch.vd.unireg.xml.common.v1;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataHelperTest {

	private static class Range implements DateRange {
		private Date dateFrom;
		private Date dateTo;

		private Range(Date dateFrom, Date dateTo) {
			this.dateFrom = dateFrom;
			this.dateTo = dateTo;
		}

		@Override
		public Date getDateFrom() {
			return dateFrom;
		}

		public void setDateFrom(Date dateFrom) {
			this.dateFrom = dateFrom;
		}

		@Override
		public Date getDateTo() {
			return dateTo;
		}

		public void setDateTo(Date dateTo) {
			this.dateTo = dateTo;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final Range range = (Range) o;

			if (dateFrom != null ? !dateFrom.equals(range.dateFrom) : range.dateFrom != null) return false;
			//noinspection RedundantIfStatement
			if (dateTo != null ? !dateTo.equals(range.dateTo) : range.dateTo != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = dateFrom != null ? dateFrom.hashCode() : 0;
			result = 31 * result + (dateTo != null ? dateTo.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("Range");
			sb.append("{dateFrom=").append(dateFrom);
			sb.append(", dateTo=").append(dateTo);
			sb.append('}');
			return sb.toString();
		}
	}

	private static class CancelableRange extends Range implements Cancelable {

		private Date cancellationDate;

		private CancelableRange(Date dateFrom, Date dateTo, Date cancellationDate) {
			super(dateFrom, dateTo);
			this.cancellationDate = cancellationDate;
		}

		@Override
		public Date getCancellationDate() {
			return cancellationDate;
		}

		public void setCancellationDate(Date cancellationDate) {
			this.cancellationDate = cancellationDate;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;

			final CancelableRange that = (CancelableRange) o;

			//noinspection RedundantIfStatement
			if (cancellationDate != null ? !cancellationDate.equals(that.cancellationDate) : that.cancellationDate != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (cancellationDate != null ? cancellationDate.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("CancelableRange{");
			sb.append("dateFrom=").append(getDateFrom());
			sb.append(", dateTo=").append(getDateTo());
			sb.append(", cancellationDate=").append(cancellationDate);
			sb.append('}');
			return sb.toString();
		}
	}

	@Test
	public void testGetAt() throws Exception {
		
		assertNull(DateHelper.getAt(Collections.<DateRange>emptyList(), null));
		assertNull(DateHelper.getAt(Collections.<DateRange>emptyList(), new Date(2000, 1, 1)));

		final Range rnull_null = new Range(null, null);
		final Range rnull_19991231 = new Range(null, new Date(1999, 12, 31));
		final Range r20000101_null = new Range(new Date(2000, 1, 1), null);

		final List<Range> list0 = Arrays.asList(rnull_null);
		assertEquals(rnull_null, DateHelper.getAt(list0, null));
		assertEquals(rnull_null, DateHelper.getAt(list0, new Date(2000, 1, 1)));

		final List<Range> list1 = Arrays.asList(rnull_19991231);
		assertEquals(rnull_19991231, DateHelper.getAt(list1, new Date(1999, 1, 1)));
		assertEquals(rnull_19991231, DateHelper.getAt(list1, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAt(list1, new Date(2000, 1, 1)));

		final List<Range> list2 = Arrays.asList(rnull_19991231, r20000101_null);
		assertEquals(rnull_19991231, DateHelper.getAt(list2, new Date(1999, 1, 1)));
		assertEquals(rnull_19991231, DateHelper.getAt(list2, new Date(1999, 12, 31)));
		assertEquals(r20000101_null, DateHelper.getAt(list2, new Date(2000, 1, 1)));
		assertEquals(r20000101_null, DateHelper.getAt(list2, null));
	}

	@Test
	public void testGetAtWithCancelable() throws Exception {
		
		assertNull(DateHelper.getAt(Collections.<DateRange>emptyList(), null));
		assertNull(DateHelper.getAt(Collections.<DateRange>emptyList(), new Date(2000, 1, 1)));

		final CancelableRange rnull_null = new CancelableRange(null, null, null);
		final CancelableRange cnull_null = new CancelableRange(null, null, new Date(2000, 1, 1));
		final CancelableRange rnull_19991231 = new CancelableRange(null, new Date(1999, 12, 31), null);
		final CancelableRange cnull_19991231 = new CancelableRange(null, new Date(1999, 12, 31), new Date(2000, 1, 1));
		final CancelableRange r20000101_null = new CancelableRange(new Date(2000, 1, 1), null, null);
		final CancelableRange c20000101_null = new CancelableRange(new Date(2000, 1, 1), null, new Date(2000, 1, 1));

		final List<CancelableRange> list0 = Arrays.asList(rnull_null);
		assertEquals(rnull_null, DateHelper.getAt(list0, null));
		assertEquals(rnull_null, DateHelper.getAt(list0, new Date(2000, 1, 1)));

		final List<CancelableRange> list1 = Arrays.asList(cnull_null);
		assertNull(DateHelper.getAt(list1, null));
		assertNull(DateHelper.getAt(list1, new Date(2000, 1, 1)));

		final List<CancelableRange> list2 = Arrays.asList(rnull_19991231);
		assertEquals(rnull_19991231, DateHelper.getAt(list2, new Date(1999, 1, 1)));
		assertEquals(rnull_19991231, DateHelper.getAt(list2, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAt(list2, new Date(2000, 1, 1)));

		final List<CancelableRange> list3 = Arrays.asList(cnull_19991231);
		assertNull(DateHelper.getAt(list3, new Date(1999, 1, 1)));
		assertNull(DateHelper.getAt(list3, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAt(list3, new Date(2000, 1, 1)));

		final List<CancelableRange> list4 = Arrays.asList(rnull_19991231, r20000101_null);
		assertEquals(rnull_19991231, DateHelper.getAt(list4, new Date(1999, 1, 1)));
		assertEquals(rnull_19991231, DateHelper.getAt(list4, new Date(1999, 12, 31)));
		assertEquals(r20000101_null, DateHelper.getAt(list4, new Date(2000, 1, 1)));
		assertEquals(r20000101_null, DateHelper.getAt(list4, null));

		final List<CancelableRange> list5 = Arrays.asList(cnull_19991231, c20000101_null);
		assertNull(DateHelper.getAt(list5, new Date(1999, 1, 1)));
		assertNull(DateHelper.getAt(list5, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAt(list5, new Date(2000, 1, 1)));
		assertNull(DateHelper.getAt(list5, null));

		// mélange annulé/pas-annulé

		final List<CancelableRange> list6 = Arrays.asList(rnull_19991231, c20000101_null);
		assertEquals(rnull_19991231, DateHelper.getAt(list6, new Date(1999, 1, 1)));
		assertEquals(rnull_19991231, DateHelper.getAt(list6, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAt(list6, new Date(2000, 1, 1)));
		assertNull(DateHelper.getAt(list6, null));
	}

	@Test
	public void testGetAllAt() throws Exception {

		assertNull(DateHelper.getAllAt(Collections.<DateRange>emptyList(), null));
		assertNull(DateHelper.getAllAt(Collections.<DateRange>emptyList(), new Date(2000, 1, 1)));

		final Range rnull_null = new Range(null, null);
		final Range rnull_19991231 = new Range(null, new Date(1999, 12, 31));
		final Range r19980101_20001231 = new Range(new Date(1998, 1, 1), new Date(2000, 12, 31));
		final Range r20000101_null = new Range(new Date(2000, 1, 1), null);

		final List<Range> list0 = Arrays.asList(rnull_null);
		assertEquals(list0, DateHelper.getAllAt(list0, null));
		assertEquals(list0, DateHelper.getAllAt(list0, new Date(2000, 1, 1)));

		final List<Range> list1 = Arrays.asList(rnull_19991231);
		assertEquals(list1, DateHelper.getAllAt(list1, new Date(1999, 1, 1)));
		assertEquals(list1, DateHelper.getAllAt(list1, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAllAt(list1, new Date(2000, 1, 1)));

		final List<Range> list2 = Arrays.asList(rnull_19991231, r20000101_null);
		final List<Range> list3 = Arrays.asList(r20000101_null);
		assertEquals(list1, DateHelper.getAllAt(list2, new Date(1999, 1, 1)));
		assertEquals(list1, DateHelper.getAllAt(list2, new Date(1999, 12, 31)));
		assertEquals(list3, DateHelper.getAllAt(list2, new Date(2000, 1, 1)));
		assertEquals(list3, DateHelper.getAllAt(list2, null));

		final List<Range> list4 = Arrays.asList(rnull_null, rnull_19991231, r19980101_20001231, r20000101_null);
		assertEquals(Arrays.asList(rnull_null, rnull_19991231), DateHelper.getAllAt(list4, new Date(1980, 1, 1)));
		assertEquals(Arrays.asList(rnull_null, rnull_19991231, r19980101_20001231), DateHelper.getAllAt(list4, new Date(1998, 1, 1)));
		assertEquals(Arrays.asList(rnull_null, r19980101_20001231, r20000101_null), DateHelper.getAllAt(list4, new Date(2000, 1, 1)));
		assertEquals(Arrays.asList(rnull_null, r20000101_null), DateHelper.getAllAt(list4, new Date(2001, 1, 1)));
	}

	@Test
	public void testGetAllAtWithCancelable() throws Exception {

		assertNull(DateHelper.getAllAt(Collections.<DateRange>emptyList(), null));
		assertNull(DateHelper.getAllAt(Collections.<DateRange>emptyList(), new Date(2000, 1, 1)));

		final CancelableRange rnull_null = new CancelableRange(null, null, null);
		final CancelableRange cnull_null = new CancelableRange(null, null, new Date(2000,1,1));
		final CancelableRange rnull_19991231 = new CancelableRange(null, new Date(1999, 12, 31), null);
		final CancelableRange cnull_19991231 = new CancelableRange(null, new Date(1999, 12, 31), new Date(2000,1,1));
		final CancelableRange r19980101_20001231 = new CancelableRange(new Date(1998, 1, 1), new Date(2000, 12, 31), null);
		final CancelableRange c19980101_20001231 = new CancelableRange(new Date(1998, 1, 1), new Date(2000, 12, 31), new Date(2000,1,1));
		final CancelableRange r20000101_null = new CancelableRange(new Date(2000, 1, 1), null, null);
		final CancelableRange c20000101_null = new CancelableRange(new Date(2000, 1, 1), null, new Date(2000,1,1));

		final List<CancelableRange> list0 = Arrays.asList(rnull_null);
		assertEquals(list0, DateHelper.getAllAt(list0, null));
		assertEquals(list0, DateHelper.getAllAt(list0, new Date(2000, 1, 1)));

		final List<CancelableRange> list1 = Arrays.asList(cnull_null);
		assertNull(DateHelper.getAllAt(list1, null));
		assertNull(DateHelper.getAllAt(list1, new Date(2000, 1, 1)));

		final List<CancelableRange> list2 = Arrays.asList(rnull_19991231);
		assertEquals(list2, DateHelper.getAllAt(list2, new Date(1999, 1, 1)));
		assertEquals(list2, DateHelper.getAllAt(list2, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAllAt(list2, new Date(2000, 1, 1)));

		final List<CancelableRange> list3 = Arrays.asList(cnull_19991231);
		assertNull(DateHelper.getAllAt(list3, new Date(1999, 1, 1)));
		assertNull(DateHelper.getAllAt(list3, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAllAt(list3, new Date(2000, 1, 1)));

		final List<CancelableRange> list4 = Arrays.asList(rnull_19991231, r20000101_null);
		final List<CancelableRange> list5 = Arrays.asList(r20000101_null);
		assertEquals(list2, DateHelper.getAllAt(list4, new Date(1999, 1, 1)));
		assertEquals(list2, DateHelper.getAllAt(list4, new Date(1999, 12, 31)));
		assertEquals(list5, DateHelper.getAllAt(list4, new Date(2000, 1, 1)));
		assertEquals(list5, DateHelper.getAllAt(list4, null));

		final List<CancelableRange> list6 = Arrays.asList(cnull_19991231, c20000101_null);
		assertNull(DateHelper.getAllAt(list6, new Date(1999, 1, 1)));
		assertNull(DateHelper.getAllAt(list6, new Date(1999, 12, 31)));
		assertNull(DateHelper.getAllAt(list6, new Date(2000, 1, 1)));
		assertNull(DateHelper.getAllAt(list6, null));

		final List<CancelableRange> list7 = Arrays.asList(rnull_null, rnull_19991231, r19980101_20001231, r20000101_null);
		assertEquals(Arrays.asList(rnull_null, rnull_19991231), DateHelper.getAllAt(list7, new Date(1980, 1, 1)));
		assertEquals(Arrays.asList(rnull_null, rnull_19991231, r19980101_20001231), DateHelper.getAllAt(list7, new Date(1998, 1, 1)));
		assertEquals(Arrays.asList(rnull_null, r19980101_20001231, r20000101_null), DateHelper.getAllAt(list7, new Date(2000, 1, 1)));
		assertEquals(Arrays.asList(rnull_null, r20000101_null), DateHelper.getAllAt(list7, new Date(2001, 1, 1)));

		final List<CancelableRange> list8 = Arrays.asList(cnull_null, cnull_19991231, c19980101_20001231, c20000101_null);
		assertNull(DateHelper.getAllAt(list8, new Date(1980, 1, 1)));
		assertNull(DateHelper.getAllAt(list8, new Date(1998, 1, 1)));
		assertNull(DateHelper.getAllAt(list8, new Date(2000, 1, 1)));
		assertNull(DateHelper.getAllAt(list8, new Date(2001, 1, 1)));

		// mélange annulé/pas-annulé

		final List<CancelableRange> list9 = Arrays.asList(cnull_null, rnull_19991231, r19980101_20001231, c20000101_null);
		assertEquals(Arrays.asList(rnull_19991231), DateHelper.getAllAt(list9, new Date(1980, 1, 1)));
		assertEquals(Arrays.asList(rnull_19991231, r19980101_20001231), DateHelper.getAllAt(list9, new Date(1998, 1, 1)));
		assertEquals(Arrays.asList(r19980101_20001231), DateHelper.getAllAt(list9, new Date(2000, 1, 1)));
		assertNull(DateHelper.getAllAt(list9, new Date(2001, 1, 1)));
	}
}

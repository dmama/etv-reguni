package ch.vd.uniregctb.migration.pm.rcent.model;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class RCEntRangeListTest {

	private static class TestHistoryElement extends RCEntHistoryElement {
		private final String dummyField;

		public TestHistoryElement(RegDate beginDate, RegDate endDateDate, String dummyField) {
			super(beginDate, endDateDate);
			this.dummyField = dummyField;
		}

		public String getDummyField() {
			return dummyField;
		}
	}

	@Test
	public void testGetValuesFor() throws Exception {
		RCEntRangeList<TestHistoryElement> rangeList = new RCEntRangeList<>(Arrays.asList(
				new TestHistoryElement(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), "12"),
				new TestHistoryElement(RegDateHelper.get(2015, 5, 3), RegDateHelper.get(2015, 5, 4), "34"),
				new TestHistoryElement(RegDateHelper.get(2016, 8, 10), RegDateHelper.get(2016, 8, 20), "56"),
				new TestHistoryElement(RegDateHelper.get(2025, 5, 25), RegDateHelper.get(2025, 5, 28), "78")
		));

		List<TestHistoryElement> result = rangeList.getValuesFor(RegDateHelper.get(2016, 8, 14));
		assertThat(result.size(), equalTo(1));
		assertThat(result.get(0).getDummyField(), equalTo("56"));

	}
}
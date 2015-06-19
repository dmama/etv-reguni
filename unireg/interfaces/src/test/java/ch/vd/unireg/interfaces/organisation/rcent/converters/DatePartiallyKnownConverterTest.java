package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v1.DatePartiallyKnown;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class DatePartiallyKnownConverterTest {

	private DatePartiallyKnownConverter converter = new DatePartiallyKnownConverter();

	@Test
	public void testConvert() throws Exception {
		RegDate theDate = RegDate.get(2015, 6, 19);
		DatePartiallyKnown yearMonthDay = new DatePartiallyKnown(theDate, null, null);

		assertThat(converter.apply(yearMonthDay), equalTo(theDate));
		assertThat(converter.apply(yearMonthDay) == theDate, equalTo(true));
	}

	@Test
	public void testConvertPartialMonth() throws Exception {
		RegDate theDate = RegDate.get(2015, 6);
		DatePartiallyKnown yearMonthDay = new DatePartiallyKnown(null, XmlUtils.regdate2xmlcal(theDate), null);

		assertThat(converter.apply(yearMonthDay), equalTo(theDate));
	}

	@Test
	public void testConvertPartialDay() throws Exception {
		RegDate theDate = RegDate.get(2015);
		DatePartiallyKnown yearMonthDay = new DatePartiallyKnown(null, null, XmlUtils.regdate2xmlcal(theDate));

		assertThat(converter.apply(yearMonthDay), equalTo(theDate));
	}
}
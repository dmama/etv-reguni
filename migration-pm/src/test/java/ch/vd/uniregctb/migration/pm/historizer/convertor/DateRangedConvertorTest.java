package ch.vd.uniregctb.migration.pm.historizer.convertor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class DateRangedConvertorTest {

	@Test
	public void testConvert() throws Exception {
		List<DateRanged<String>> rl = new ArrayList<>();
		rl.add(new DateRanged<>(RegDate.get(2015, 4, 1), RegDate.get(2015, 4, 2), "DATA1"));
		rl.add(new DateRanged<>(RegDate.get(2015, 4, 3), null, "DATA2"));

		List<DateRanged<String>> convertedRl = DateRangedConvertor.convert(rl, e -> e + "_CONVERTED");
		{
			assertThat(convertedRl.get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 1)));
			assertThat(convertedRl.get(0).getDateFin(), equalTo(RegDate.get(2015, 4, 2)));
			assertThat(convertedRl.get(0).getPayload(), equalTo("DATA1_CONVERTED"));
		}
		{
			assertThat(convertedRl.get(1).getDateDebut(), equalTo(RegDate.get(2015, 4, 3)));
			assertThat(convertedRl.get(1).getDateFin(), nullValue());
			assertThat(convertedRl.get(1).getPayload(), equalTo("DATA2_CONVERTED"));
		}
	}

	@Test
	public void testDirectConvert() throws Exception {
		DateRanged<String> dr1 = new DateRanged<>(RegDate.get(2015, 4, 1), RegDate.get(2015, 4, 2), "DATA1");
		DateRanged<String> dr2 = new DateRanged<>(RegDate.get(2015, 4, 3), null, "DATA2");

		DateRanged<String> convertedDr1 = DateRangedConvertor.convert(dr1, e -> e + "_CONVERTED");
		DateRanged<String> convertedDr2 = DateRangedConvertor.convert(dr2, e -> e + "_CONVERTED");
		{
			assertThat(convertedDr1.getDateDebut(), equalTo(RegDate.get(2015, 4, 1)));
			assertThat(convertedDr1.getDateFin(), equalTo(RegDate.get(2015, 4, 2)));
			assertThat(convertedDr1.getPayload(), equalTo("DATA1_CONVERTED"));
		}
		{
			assertThat(convertedDr2.getDateDebut(), equalTo(RegDate.get(2015, 4, 3)));
			assertThat(convertedDr2.getDateFin(), nullValue());
			assertThat(convertedDr2.getPayload(), equalTo("DATA2_CONVERTED"));
		}
	}
}
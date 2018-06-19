package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.convertor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class DateRangedConvertorTest {

	@Test
	public void testConvert() throws Exception {
		List<DateRangeHelper.Ranged<String>> rl = new ArrayList<>();
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 1), RegDate.get(2015, 4, 2), "DATA1"));
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 3), null, "DATA2"));

		List<DateRangeHelper.Ranged<String>> convertedRl = DateRangedConvertor.convert(rl, e -> e + "_CONVERTED");
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
}
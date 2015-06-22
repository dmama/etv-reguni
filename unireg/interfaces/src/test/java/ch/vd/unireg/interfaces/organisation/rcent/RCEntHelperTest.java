package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.rcent.converters.Converter;
import ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RCEntHelperTest {

	 static final Converter<String, String> converter = new Converter<String, String>() {
		@Override
		public String apply(String s) {
			return s + "_CONVERTED";
		}
	};

	@Test
	public void testConvert() throws Exception {
		DateRanged<String> range = new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA");

		ch.vd.unireg.interfaces.organisation.data.DateRanged<String> rangeResult = RCEntHelper.convert(range);
		{
			assertThat(rangeResult.getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangeResult.getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangeResult.getPayload(), equalTo("DATA"));
		}
	}


	@Test
	public void testConvertList() throws Exception {
		List<DateRanged<String>> ranges = Arrays.asList(
				new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA1"),
				new DateRanged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA2"),
				new DateRanged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA3")
		);

		List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>> rangesResult = RCEntHelper.convert(ranges);
		{
			assertThat(rangesResult.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult.get(0).getPayload(), equalTo("DATA1"));
		}
		{
			assertThat(rangesResult.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult.get(1).getPayload(), equalTo("DATA2"));
		}
		{
			assertThat(rangesResult.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult.get(2).getPayload(), equalTo("DATA3"));
		}
	}

	@Test
	public void testConvertMap() throws Exception {
		Map<String, List<DateRanged<String>>> rangeMap = new HashMap<>();
		rangeMap.put("KEY1", Arrays.asList(
				new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA11"),
				new DateRanged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA12"),
				new DateRanged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA13")
		));
		rangeMap.put("KEY2", Arrays.asList(
				new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA21"),
				new DateRanged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA22"),
				new DateRanged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA23")
		));
		rangeMap.put("KEY3", Arrays.asList(
				new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA31"),
				new DateRanged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA32"),
				new DateRanged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA33")
		));

		Map<String, List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>>> rangeMapResult = RCEntHelper.convert(rangeMap);

		List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>> rangesResult1 = rangeMapResult.get("KEY1");
		{
			assertThat(rangesResult1.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult1.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult1.get(0).getPayload(), equalTo("DATA11"));
		}
		{
			assertThat(rangesResult1.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult1.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult1.get(1).getPayload(), equalTo("DATA12"));
		}
		{
			assertThat(rangesResult1.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult1.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult1.get(2).getPayload(), equalTo("DATA13"));
		}

		List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>> rangesResult2 = rangeMapResult.get("KEY2");
		{
			assertThat(rangesResult2.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult2.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult2.get(0).getPayload(), equalTo("DATA21"));
		}
		{
			assertThat(rangesResult2.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult2.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult2.get(1).getPayload(), equalTo("DATA22"));
		}
		{
			assertThat(rangesResult2.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult2.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult2.get(2).getPayload(), equalTo("DATA23"));
		}

		List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>> rangesResult3 = rangeMapResult.get("KEY3");
		{
			assertThat(rangesResult3.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult3.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult3.get(0).getPayload(), equalTo("DATA31"));
		}
		{
			assertThat(rangesResult3.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult3.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult3.get(1).getPayload(), equalTo("DATA32"));
		}
		{
			assertThat(rangesResult3.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult3.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult3.get(2).getPayload(), equalTo("DATA33"));
		}
	}

	@Test
	public void testConvertAndMap() throws Exception {
		DateRanged<String> range = new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA");

		ch.vd.unireg.interfaces.organisation.data.DateRanged<String> rangeResult = RCEntHelper.convertAndMap(range, converter);
		{
			assertThat(rangeResult.getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangeResult.getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangeResult.getPayload(), equalTo("DATA_CONVERTED"));
		}
	}

	@Test
	public void testConvertAndMapList() throws Exception {
		List<DateRanged<String>> ranges = Arrays.asList(
				new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA1"),
				new DateRanged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA2"),
				new DateRanged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA3"),
				new DateRanged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA4")
		);

		List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>> rangesResult = RCEntHelper.convertAndMap(ranges, converter);
		{
			assertThat(rangesResult.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult.get(0).getPayload(), equalTo("DATA1_CONVERTED"));
		}
		{
			assertThat(rangesResult.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult.get(1).getPayload(), equalTo("DATA2_CONVERTED"));
		}
		{
			assertThat(rangesResult.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult.get(2).getPayload(), equalTo("DATA3_CONVERTED"));
		}
	}

	@Test
	public void testConvertAndMapMap() throws Exception {
		Map<String, List<DateRanged<String>>> rangeMap = new HashMap<>();
		rangeMap.put("KEY1", Arrays.asList(
				new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA11"),
				new DateRanged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA12"),
				new DateRanged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA13"),
				new DateRanged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA14")
		));
		rangeMap.put("KEY2", Arrays.asList(
				new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA21"),
				new DateRanged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA22"),
				new DateRanged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA23"),
				new DateRanged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA24")
		));
		rangeMap.put("KEY3", Arrays.asList(
				new DateRanged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA31"),
				new DateRanged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA32"),
				new DateRanged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA33"),
				new DateRanged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA34")
		));


		Map<String, List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>>> rangeMapResult = RCEntHelper.convertAndMap(rangeMap, converter);

		List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>> rangesResult1 = rangeMapResult.get("KEY1");
		{
			assertThat(rangesResult1.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult1.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult1.get(0).getPayload(), equalTo("DATA11_CONVERTED"));
		}
		{
			assertThat(rangesResult1.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult1.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult1.get(1).getPayload(), equalTo("DATA12_CONVERTED"));
		}
		{
			assertThat(rangesResult1.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult1.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult1.get(2).getPayload(), equalTo("DATA13_CONVERTED"));
		}

		List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>> rangesResult2 = rangeMapResult.get("KEY2");
		{
			assertThat(rangesResult2.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult2.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult2.get(0).getPayload(), equalTo("DATA21_CONVERTED"));
		}
		{
			assertThat(rangesResult2.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult2.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult2.get(1).getPayload(), equalTo("DATA22_CONVERTED"));
		}
		{
			assertThat(rangesResult2.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult2.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult2.get(2).getPayload(), equalTo("DATA23_CONVERTED"));
		}

		List<ch.vd.unireg.interfaces.organisation.data.DateRanged<String>> rangesResult3 = rangeMapResult.get("KEY3");
		{
			assertThat(rangesResult3.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult3.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult3.get(0).getPayload(), equalTo("DATA31_CONVERTED"));
		}
		{
			assertThat(rangesResult3.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult3.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult3.get(1).getPayload(), equalTo("DATA32_CONVERTED"));
		}
		{
			assertThat(rangesResult3.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult3.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult3.get(2).getPayload(), equalTo("DATA33_CONVERTED"));
		}
	}

}
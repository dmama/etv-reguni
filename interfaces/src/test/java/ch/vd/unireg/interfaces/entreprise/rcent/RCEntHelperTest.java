package ch.vd.unireg.interfaces.entreprise.rcent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class RCEntHelperTest extends WithoutSpringTest {

	private static final Function<String, String> converter = s -> s + "_CONVERTED";

	@Test
	public void testConvert() throws Exception {
		DateRangeHelper.Ranged<String> range = new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA");

		ch.vd.unireg.interfaces.entreprise.data.DateRanged<String> rangeResult = RCEntHelper.convert(range);
		{
			assertThat(rangeResult.getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangeResult.getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangeResult.getPayload(), equalTo("DATA"));
		}
	}


	@Test
	public void testConvertList() throws Exception {
		List<DateRangeHelper.Ranged<String>> ranges = Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA1"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA2"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA3")
		);

		List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>> rangesResult = RCEntHelper.convert(ranges);
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
		Map<String, List<DateRangeHelper.Ranged<String>>> rangeMap = new HashMap<>();
		rangeMap.put("KEY1", Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA11"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA12"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA13")
		));
		rangeMap.put("KEY2", Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA21"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA22"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA23")
		));
		rangeMap.put("KEY3", Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA31"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA32"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA33")
		));

		Map<String, List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>>> rangeMapResult = RCEntHelper.convert(rangeMap);

		List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>> rangesResult1 = rangeMapResult.get("KEY1");
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

		List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>> rangesResult2 = rangeMapResult.get("KEY2");
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

		List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>> rangesResult3 = rangeMapResult.get("KEY3");
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
		DateRangeHelper.Ranged<String> range = new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA");

		ch.vd.unireg.interfaces.entreprise.data.DateRanged<String> rangeResult = RCEntHelper.convertAndMap(range, converter);
		{
			assertThat(rangeResult.getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangeResult.getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangeResult.getPayload(), equalTo("DATA_CONVERTED"));
		}
	}

	@Test
	public void testConvertAndMapList() throws Exception {
		List<DateRangeHelper.Ranged<String>> ranges = Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA1"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA2"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA3"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA4")
		);

		List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>> rangesResult = RCEntHelper.convertAndMap(ranges, converter);
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
		Map<String, List<DateRangeHelper.Ranged<String>>> rangeMap = new HashMap<>();
		rangeMap.put("KEY1", Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA11"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA12"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA13"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA14")
		));
		rangeMap.put("KEY2", Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA21"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA22"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA23"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA24")
		));
		rangeMap.put("KEY3", Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA31"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA32"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA33"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA34")
		));


		Map<String, List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>>> rangeMapResult = RCEntHelper.convertAndMap(rangeMap, converter);

		List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>> rangesResult1 = rangeMapResult.get("KEY1");
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

		List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>> rangesResult2 = rangeMapResult.get("KEY2");
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

		List<ch.vd.unireg.interfaces.entreprise.data.DateRanged<String>> rangesResult3 = rangeMapResult.get("KEY3");
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

	@Test
	public void testConvertAndDerangeList() throws Exception {

		List<DateRangeHelper.Ranged<String>> ranges = Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA1"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA2"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA3"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA4")
		);

		List<FlatMapResultTestData> rangesResult = RCEntHelper.convertAndDerange(ranges, stringRanged -> new FlatMapResultTestData(stringRanged.getDateDebut(), stringRanged.getDateFin(), converter.apply(stringRanged.getPayload())));

		{
			assertThat(rangesResult.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult.get(0).getData(), equalTo("DATA1_CONVERTED"));
		}
		{
			assertThat(rangesResult.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult.get(1).getData(), equalTo("DATA2_CONVERTED"));
		}
		{
			assertThat(rangesResult.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 29)));
			assertThat(rangesResult.get(2).getDateFin(), equalTo(RegDate.get(2015, 5, 30)));
			assertThat(rangesResult.get(2).getData(), equalTo("DATA3_CONVERTED"));
		}
		{
			assertThat(rangesResult.get(3).getDateDebut(), equalTo(RegDate.get(2015, 5, 31)));
			assertThat(rangesResult.get(3).getDateFin(), equalTo(RegDate.get(2015, 6, 1)));
			assertThat(rangesResult.get(3).getData(), equalTo("DATA4_CONVERTED"));
		}
	}

	@Test
	public void testConvertAndMapDerangeList() throws Exception {

		List<DateRangeHelper.Ranged<String>> ranges1 = Arrays.asList(
				new DateRangeHelper.Ranged<>(date(2015, 5, 25), date(2015, 5, 26), "DATA1"),
				new DateRangeHelper.Ranged<>(date(2015, 5, 25), date(2015, 5, 26), "DATA2")
		);
		List<DateRangeHelper.Ranged<String>> ranges2 = Arrays.asList(
				new DateRangeHelper.Ranged<>(date(2015, 5, 27), date(2015, 5, 30), "DATA3"),
				new DateRangeHelper.Ranged<>(date(2015, 5, 27), date(2015, 5, 30), "DATA4")
		);
		Map<RegDate, List<DateRangeHelper.Ranged<String>>> rangeMap = new HashMap<>();
		rangeMap.put(date(2015, 5, 25), ranges1);
		rangeMap.put(date(2015, 5, 27), ranges2);

		Map<RegDate, List<FlatMapResultTestData>> mapResult = RCEntHelper.convertAndMapDerange(rangeMap, stringRanged -> new FlatMapResultTestData(stringRanged.getDateDebut(), stringRanged.getDateFin(), converter.apply(stringRanged.getPayload())));

		{
			List<FlatMapResultTestData> rangeResult = mapResult.get(date(2015, 5, 25));
			{
				assertThat(rangeResult.get(0).getDateDebut(), equalTo(date(2015, 5, 25)));
				assertThat(rangeResult.get(0).getDateFin(), equalTo(date(2015, 5, 26)));
				assertThat(rangeResult.get(0).getData(), equalTo("DATA1_CONVERTED"));
			}
			{
				assertThat(rangeResult.get(1).getDateDebut(), equalTo(date(2015, 5, 25)));
				assertThat(rangeResult.get(1).getDateFin(), equalTo(date(2015, 5, 26)));
				assertThat(rangeResult.get(1).getData(), equalTo("DATA2_CONVERTED"));
			}
		}
		{
			List<FlatMapResultTestData> rangeResult = mapResult.get(date(2015, 5, 27));
			{
				assertThat(rangeResult.get(0).getDateDebut(), equalTo(date(2015, 5, 27)));
				assertThat(rangeResult.get(0).getDateFin(), equalTo(date(2015, 5, 30)));
				assertThat(rangeResult.get(0).getData(), equalTo("DATA3_CONVERTED"));
			}
			{
				assertThat(rangeResult.get(1).getDateDebut(), equalTo(date(2015, 5, 27)));
				assertThat(rangeResult.get(1).getDateFin(), equalTo(date(2015, 5, 30)));
				assertThat(rangeResult.get(1).getData(), equalTo("DATA4_CONVERTED"));
			}
		}
	}

	@Test
	public void testConvertAndDerangeListAvecPredicat() throws Exception {

		List<DateRangeHelper.Ranged<String>> ranges = Arrays.asList(
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 25), RegDate.get(2015, 5, 26), "DATA1"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 27), RegDate.get(2015, 5, 28), "DATA2"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 29), RegDate.get(2015, 5, 30), "DATA3"),
				new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 31), RegDate.get(2015, 6, 1), "DATA4")
		);

		List<FlatMapResultTestData> rangesResult = RCEntHelper.convertAndDerange(ranges,
		                                                                         stringRanged -> new FlatMapResultTestData(stringRanged.getDateDebut(), stringRanged.getDateFin(),
		                                                                                                          converter.apply(stringRanged.getPayload())),
		                                                                         s -> ! "DATA3".equals(s)
		);

		{
			assertThat(rangesResult.get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(rangesResult.get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 26)));
			assertThat(rangesResult.get(0).getData(), equalTo("DATA1_CONVERTED"));
		}
		{
			assertThat(rangesResult.get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 27)));
			assertThat(rangesResult.get(1).getDateFin(), equalTo(RegDate.get(2015, 5, 28)));
			assertThat(rangesResult.get(1).getData(), equalTo("DATA2_CONVERTED"));
		}
		{
			assertThat(rangesResult.get(2).getDateDebut(), equalTo(RegDate.get(2015, 5, 31)));
			assertThat(rangesResult.get(2).getDateFin(), equalTo(RegDate.get(2015, 6, 1)));
			assertThat(rangesResult.get(2).getData(), equalTo("DATA4_CONVERTED"));
		}
	}

	/**
	 * Represente une donnée "flatmappée".
	 */
	static class FlatMapResultTestData implements DateRange {

		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final String data;

		FlatMapResultTestData(RegDate dateDebut, RegDate dateFin, String data) {
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.data = data;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public RegDate getDateFin() {
			return dateFin;
		}

		public String getData() {
			return data;
		}
	}

	@Test
	public void testConvertNullReturnNull() {
		DateRangeHelper.Ranged<String> nullDr = null;
		List<DateRangeHelper.Ranged<String>> nullDrList = null;
		Map<String, List<DateRangeHelper.Ranged<String>>> nullDrMap = null;

		assertThat(RCEntHelper.convert(nullDr), nullValue());
		assertThat(RCEntHelper.convert(nullDrList), nullValue());
		assertThat(RCEntHelper.convert(nullDrMap), nullValue());
		assertThat(RCEntHelper.convertAndMap(nullDr, converter), nullValue());
		assertThat(RCEntHelper.convertAndMap(nullDrList, converter), nullValue());
		assertThat(RCEntHelper.convertAndMap(nullDrMap, converter), nullValue());
	}
}
package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.collector.IndexedDataCollector;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.collector.ListDataCollector;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.collector.MultiValueDataCollector;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.collector.MultiValueIndexedDataCollector;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.collector.SingleValueDataCollector;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.collector.SingleValueIndexedDataCollector;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.equalator.Equalator;

public class HistorizerTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testSingleValueCollector() throws Exception {

		final class SnapshotData {
			final int i;
			final String str;

			public SnapshotData(int i, String str) {
				this.i = i;
				this.str = str;
			}
		}

		final ListDataCollector<SnapshotData, Integer> iCollector = new SingleValueDataCollector<>(d -> d.i, Equalator.DEFAULT);
		final ListDataCollector<SnapshotData, String> strCollector = new SingleValueDataCollector<>(d -> d.str, String::equalsIgnoreCase);
		final ListDataCollector<SnapshotData, Object> nullCollector = new SingleValueDataCollector<>(d -> null, (d1, d2) -> false);

		final Map<RegDate, SnapshotData> input = new HashMap<>();
		input.put(RegDate.get(2000, 3, 27), new SnapshotData(1, "1020 VD"));
		input.put(RegDate.get(2000, 4, 6), new SnapshotData(2, "1020 VD"));
		input.put(RegDate.get(2002, 7, 9), new SnapshotData(3, "1020 VD"));
		input.put(RegDate.get(2005, 1, 19), new SnapshotData(3, null));
		input.put(RegDate.get(2005, 1, 31), new SnapshotData(3, "1040 VD"));
		input.put(RegDate.get(2005, 6, 30), new SnapshotData(4, "1040 vd"));

		Historizer.historize(input, Arrays.asList(iCollector, strCollector, nullCollector));

		final List<DateRangeHelper.Ranged<Integer>> is = iCollector.getCollectedData();
		Assert.assertNotNull(is);
		Assert.assertEquals(4, is.size());
		{
			final DateRangeHelper.Ranged<Integer> i = is.get(0);
			Assert.assertEquals(RegDate.get(2000, 3, 27), i.getDateDebut());
			Assert.assertEquals(RegDate.get(2000, 4, 5), i.getDateFin());
			Assert.assertEquals((Integer) 1, i.getPayload());
		}
		{
			final DateRangeHelper.Ranged<Integer> i = is.get(1);
			Assert.assertEquals(RegDate.get(2000, 4, 6), i.getDateDebut());
			Assert.assertEquals(RegDate.get(2002, 7, 8), i.getDateFin());
			Assert.assertEquals((Integer) 2, i.getPayload());
		}
		{
			final DateRangeHelper.Ranged<Integer> i = is.get(2);
			Assert.assertEquals(RegDate.get(2002, 7, 9), i.getDateDebut());
			Assert.assertEquals(RegDate.get(2005, 6, 29), i.getDateFin());
			Assert.assertEquals((Integer) 3, i.getPayload());
		}
		{
			final DateRangeHelper.Ranged<Integer> i = is.get(3);
			Assert.assertEquals(RegDate.get(2005, 6, 30), i.getDateDebut());
			Assert.assertNull(i.getDateFin());
			Assert.assertEquals((Integer) 4, i.getPayload());
		}

		final List<DateRangeHelper.Ranged<String>> strs = strCollector.getCollectedData();
		Assert.assertNotNull(strs);
		Assert.assertEquals(2, strs.size());
		{
			final DateRangeHelper.Ranged<String> str = strs.get(0);
			Assert.assertEquals(RegDate.get(2000, 3, 27), str.getDateDebut());
			Assert.assertEquals(RegDate.get(2005, 1, 18), str.getDateFin());
			Assert.assertEquals("1020 VD", str.getPayload());

		}
		{
			final DateRangeHelper.Ranged<String> str = strs.get(1);
			Assert.assertEquals(RegDate.get(2005, 1, 31), str.getDateDebut());
			Assert.assertNull(str.getDateFin());
			Assert.assertEquals("1040 VD", str.getPayload());
		}

		final List<DateRangeHelper.Ranged<Object>> os = nullCollector.getCollectedData();
		Assert.assertNotNull(os);
		Assert.assertEquals(0, os.size());
	}

	@Test
	public void testMultiValueCollector() throws Exception {

		final class Data {
			final int id;
			final String value;

			public Data(int id, String value) {
				this.id = id;
				this.value = value;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;

				final Data data = (Data) o;
				return id == data.id && !(value != null ? !value.equals(data.value) : data.value != null);
			}

			@Override
			public int hashCode() {
				int result = id;
				result = 31 * result + (value != null ? value.hashCode() : 0);
				return result;
			}
		}

		final class SnapshotData {
			final Collection<Data> data;

			public SnapshotData(Collection<Data> data) {
				this.data = data;
			}
		}

		final ListDataCollector<SnapshotData, Data> dataCollector = new MultiValueDataCollector<>(s -> s.data.stream(), Equalator.DEFAULT, d -> d.id);

		final Map<RegDate, SnapshotData> input = new HashMap<>();
		input.put(RegDate.get(2000, 1, 4), new SnapshotData(Arrays.asList(new Data(1, "One"), new Data(2, "Two"))));
		input.put(RegDate.get(2000, 7, 12), new SnapshotData(Arrays.asList(new Data(1, "One"), new Data(3, "Three"))));
		input.put(RegDate.get(2001, 8, 1), new SnapshotData(Arrays.asList(new Data(1, "One prime"), new Data(2, "Two"), new Data(3, "Three"))));
		input.put(RegDate.get(2002, 5, 12), new SnapshotData(Collections.singletonList(new Data(3, "Three"))));

		Historizer.historize(input, Collections.singletonList(dataCollector));

		final List<DateRangeHelper.Ranged<Data>> collected = dataCollector.getCollectedData();
		Assert.assertNotNull(collected);
		Assert.assertEquals(5, collected.size());

		final List<DateRangeHelper.Ranged<Data>> sorted = new ArrayList<>(collected);
		Collections.sort(sorted, Comparator.comparing(d -> d.getPayload().id));     // le tri est stable : à id égal, on aura les périodes dans l'ordre chronologique
		{
			final DateRangeHelper.Ranged<Data> data = sorted.get(0);
			Assert.assertNotNull(data);
			Assert.assertEquals(RegDate.get(2000, 1, 4), data.getDateDebut());
			Assert.assertEquals(RegDate.get(2001, 7, 31), data.getDateFin());       // "One" becomes "One prime" after this date
			Assert.assertEquals(1, data.getPayload().id);
			Assert.assertEquals("One", data.getPayload().value);
		}
		{
			final DateRangeHelper.Ranged<Data> data = sorted.get(1);
			Assert.assertNotNull(data);
			Assert.assertEquals(RegDate.get(2001, 8, 1), data.getDateDebut());
			Assert.assertEquals(RegDate.get(2002, 5, 11), data.getDateFin());
			Assert.assertEquals(1, data.getPayload().id);
			Assert.assertEquals("One prime", data.getPayload().value);
		}
		{
			final DateRangeHelper.Ranged<Data> data = sorted.get(2);
			Assert.assertNotNull(data);
			Assert.assertEquals(RegDate.get(2000, 1, 4), data.getDateDebut());
			Assert.assertEquals(RegDate.get(2000, 7, 11), data.getDateFin());
			Assert.assertEquals(2, data.getPayload().id);
			Assert.assertEquals("Two", data.getPayload().value);
		}
		{
			final DateRangeHelper.Ranged<Data> data = sorted.get(3);
			Assert.assertNotNull(data);
			Assert.assertEquals(RegDate.get(2001, 8, 1), data.getDateDebut());
			Assert.assertEquals(RegDate.get(2002, 5, 11), data.getDateFin());
			Assert.assertEquals(2, data.getPayload().id);
			Assert.assertEquals("Two", data.getPayload().value);
		}
		{
			final DateRangeHelper.Ranged<Data> data = sorted.get(4);
			Assert.assertNotNull(data);
			Assert.assertEquals(RegDate.get(2000, 7, 12), data.getDateDebut());
			Assert.assertNull(data.getDateFin());
			Assert.assertEquals(3, data.getPayload().id);
			Assert.assertEquals("Three", data.getPayload().value);
		}
	}

	/**
	 * Pour montrer qu'il est possible de récupérer, par exemple, les adresses "par établissement"...
	 */
	@Test
	public void testMultiValueIndexed() throws Exception {

		final class Adresse {
			final String type;
			final String adresse;

			public Adresse(String type, String adresse) {
				this.type = type;
				this.adresse = adresse;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;

				final Adresse adresse1 = (Adresse) o;
				return !(type != null ? !type.equals(adresse1.type) : adresse1.type != null) && !(adresse != null ? !adresse.equals(adresse1.adresse) : adresse1.adresse != null);
			}

			@Override
			public int hashCode() {
				int result = type != null ? type.hashCode() : 0;
				result = 31 * result + (adresse != null ? adresse.hashCode() : 0);
				return result;
			}
		}

		final class SubsnapshotData {
			final Long idSub;
			final List<Adresse> adresses;

			public SubsnapshotData(Long idSub, List<Adresse> adresses) {
				this.idSub = idSub;
				this.adresses = adresses;
			}
		}

		final class SnapshotData {
			final List<SubsnapshotData> subs;

			public SnapshotData(List<SubsnapshotData> subs) {
				this.subs = subs;
			}
		}

		final Function<SnapshotData, Stream<Keyed<Long, Adresse>>> dataExtractor = s ->
				s.subs.stream()
						.map(sub -> sub.adresses.stream().map(a -> new Keyed<>(sub.idSub, a)))
						.flatMap(Function.identity());
		final Function<? super Adresse, String> keyExtractor = a -> a.type;
		final IndexedDataCollector<SnapshotData, Adresse, Long> collector = new MultiValueIndexedDataCollector<>(dataExtractor, Equalator.DEFAULT, MultiValueIndexedDataCollector.enkey(keyExtractor));

		final Map<RegDate, SnapshotData> input = new HashMap<>();
		input.put(RegDate.get(2000, 1, 1), new SnapshotData(Collections.singletonList(new SubsnapshotData(1L, Arrays.asList(new Adresse("C", "Rue du Lac"), new Adresse("D", "Rue du Lac"))))));
		input.put(RegDate.get(2001, 6, 3), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, Arrays.asList(new Adresse("C", "Rue du Lac"), new Adresse("D", "Rue du Lac"))),
		                                                                  new SubsnapshotData(2L, Arrays.asList(new Adresse("C", "Dans les bois"), new Adresse("D", "Dans les bois"))),
		                                                                  new SubsnapshotData(3L, Arrays.asList(new Adresse("C", "Bahnhofstrasse 12"), new Adresse("D", "Torstrasse 1"))))));
		input.put(RegDate.get(2002, 12, 1), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, Arrays.asList(new Adresse("C", "Rue du Lac, Case postale"), new Adresse("D", "Rue du Lac"))),
		                                                                   new SubsnapshotData(2L, Arrays.asList(new Adresse("C", "Dans les bois, Case postale"), new Adresse("D", "Dans les bois"))))));

		Historizer.historize(input, Collections.singletonList(collector));

		final Map<Long, List<DateRangeHelper.Ranged<Adresse>>> collected = collector.getCollectedData();
		Assert.assertEquals(3, collected.size());
		{
			final List<DateRangeHelper.Ranged<Adresse>> adresses = collected.get(1L);
			Assert.assertNotNull(adresses);
			Assert.assertEquals(3, adresses.size());

			final List<DateRangeHelper.Ranged<Adresse>> sorted = new ArrayList<>(adresses);
			Collections.sort(sorted, Comparator.comparing(a -> a.getPayload().type));
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2000, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Rue du Lac", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2002, 12, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Rue du Lac, Case postale", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(2);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2000, 1, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("D", a.getPayload().type);
				Assert.assertEquals("Rue du Lac", a.getPayload().adresse);
			}
		}
		{
			final List<DateRangeHelper.Ranged<Adresse>> adresses = collected.get(2L);
			Assert.assertNotNull(adresses);
			Assert.assertEquals(3, adresses.size());

			final List<DateRangeHelper.Ranged<Adresse>> sorted = new ArrayList<>(adresses);
			Collections.sort(sorted, Comparator.comparing(a -> a.getPayload().type));
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2001, 6, 3), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Dans les bois", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2002, 12, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Dans les bois, Case postale", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(2);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2001, 6, 3), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("D", a.getPayload().type);
				Assert.assertEquals("Dans les bois", a.getPayload().adresse);
			}
		}
		{
			final List<DateRangeHelper.Ranged<Adresse>> adresses = collected.get(3L);
			Assert.assertNotNull(adresses);
			Assert.assertEquals(2, adresses.size());

			final List<DateRangeHelper.Ranged<Adresse>> sorted = new ArrayList<>(adresses);
			Collections.sort(sorted, Comparator.comparing(a -> a.getPayload().type));
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2001, 6, 3), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Bahnhofstrasse 12", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2001, 6, 3), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("D", a.getPayload().type);
				Assert.assertEquals("Torstrasse 1", a.getPayload().adresse);
			}
		}
	}

	/**
	 * Que se passe-t-il quand on utilise un collector indexé single value avec des données multiples?
	 */
	@Test
	public void testMultiValueIndexedWithWrongCollector() throws Exception {

		final class Adresse {
			final String type;
			final String adresse;

			public Adresse(String type, String adresse) {
				this.type = type;
				this.adresse = adresse;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;

				final Adresse adresse1 = (Adresse) o;
				return !(type != null ? !type.equals(adresse1.type) : adresse1.type != null) && !(adresse != null ? !adresse.equals(adresse1.adresse) : adresse1.adresse != null);
			}

			@Override
			public int hashCode() {
				int result = type != null ? type.hashCode() : 0;
				result = 31 * result + (adresse != null ? adresse.hashCode() : 0);
				return result;
			}
		}

		final class SubsnapshotData {
			final Long idSub;
			final List<Adresse> adresses;

			public SubsnapshotData(Long idSub, List<Adresse> adresses) {
				this.idSub = idSub;
				this.adresses = adresses;
			}
		}

		final class SnapshotData {
			final List<SubsnapshotData> subs;

			public SnapshotData(List<SubsnapshotData> subs) {
				this.subs = subs;
			}
		}

		final Function<SnapshotData, Stream<Keyed<Long, Adresse>>> dataExtractor = s ->
				s.subs.stream()
						.map(sub -> sub.adresses.stream().map(a -> new Keyed<>(sub.idSub, a)))
						.flatMap(Function.identity());

		final IndexedDataCollector<SnapshotData, Adresse, Long> collector = new SingleValueIndexedDataCollector<>(dataExtractor, Equalator.DEFAULT);

		final Map<RegDate, SnapshotData> input = new HashMap<>();
		input.put(RegDate.get(2000, 1, 1), new SnapshotData(Collections.singletonList(new SubsnapshotData(1L, Arrays.asList(new Adresse("C", "Rue du Lac"), new Adresse("D", "Rue du Lac"))))));
		input.put(RegDate.get(2001, 6, 3), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, Arrays.asList(new Adresse("C", "Rue du Lac"), new Adresse("D", "Rue du Lac"))),
		                                                                  new SubsnapshotData(2L, Arrays.asList(new Adresse("C", "Dans les bois"), new Adresse("D", "Dans les bois"))),
		                                                                  new SubsnapshotData(3L, Arrays.asList(new Adresse("C", "Bahnhofstrasse 12"), new Adresse("D", "Torstrasse 1"))))));
		input.put(RegDate.get(2002, 12, 1), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, Arrays.asList(new Adresse("C", "Rue du Lac, Case postale"), new Adresse("D", "Rue du Lac"))),
		                                                                   new SubsnapshotData(2L, Arrays.asList(new Adresse("C", "Dans les bois, Case postale"), new Adresse("D", "Dans les bois"))))));

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("A date identical to or greater than the starting date of the previous period has been encountered.");

		Historizer.historize(input, Collections.singletonList(collector));

	}

	/**
	 * Pour montrer qu'il est possible de récupérer, par exemple, les adresses "par établissement"...
	 */
	@Test
	public void testSingleValueIndexedWithHoles() throws Exception {

		final class Adresse {
			final String type;
			final String adresse;

			public Adresse(String type, String adresse) {
				this.type = type;
				this.adresse = adresse;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;

				final Adresse adresse1 = (Adresse) o;
				return !(type != null ? !type.equals(adresse1.type) : adresse1.type != null) && !(adresse != null ? !adresse.equals(adresse1.adresse) : adresse1.adresse != null);
			}

			@Override
			public int hashCode() {
				int result = type != null ? type.hashCode() : 0;
				result = 31 * result + (adresse != null ? adresse.hashCode() : 0);
				return result;
			}
		}

		final class SubsnapshotData {
			final Long idSub;
			final List<Adresse> adresses;

			public SubsnapshotData(Long idSub, List<Adresse> adresses) {
				this.idSub = idSub;
				this.adresses = adresses;
			}
		}

		final class SnapshotData {
			final List<SubsnapshotData> subs;

			public SnapshotData(List<SubsnapshotData> subs) {
				this.subs = subs;
			}
		}

		final Function<SnapshotData, Stream<Keyed<Long, Adresse>>> dataExtractor = s ->
				s.subs.stream()
						.map(sub -> sub.adresses.stream().map(a -> new Keyed<>(sub.idSub, a)))
						.flatMap(Function.identity());

		final IndexedDataCollector<SnapshotData, Adresse, Long> collector = new SingleValueIndexedDataCollector<>(dataExtractor, Equalator.DEFAULT);

		final Map<RegDate, SnapshotData> input = new HashMap<>();
		input.put(RegDate.get(2000, 1, 1), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, Collections.singletonList(new Adresse("C", "Rue du Lac"))),
		                                                                  new SubsnapshotData(4L, Collections.singletonList(new Adresse("C", "Sur le toit"))))));
		input.put(RegDate.get(2001, 6, 3), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, Collections.singletonList(new Adresse("C", "Rue du Lac"))),
		                                                                  new SubsnapshotData(2L, Collections.singletonList(new Adresse("C", "Dans les bois"))),
		                                                                  new SubsnapshotData(3L, Collections.singletonList(new Adresse("C", "Bahnhofstrasse 12"))),
		                                                                  new SubsnapshotData(4L, Collections.emptyList()))));
		input.put(RegDate.get(2002, 12, 1), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, Collections.singletonList(new Adresse("C", "Rue du Lac, Case postale"))),
		                                                                   new SubsnapshotData(2L, Collections.singletonList(new Adresse("C", "Dans les bois, Case postale"))),
		                                                                   new SubsnapshotData(4L, Collections.singletonList(new Adresse("D", "Dans le fossé"))))));
		input.put(RegDate.get(2003, 2, 1), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, Collections.singletonList(new Adresse("C", "Rue du Lac, Case postale"))),
		                                                                  new SubsnapshotData(2L, Collections.singletonList(new Adresse("C", "Dans les bois, Case postale"))),
		                                                                  new SubsnapshotData(3L, Collections.singletonList(new Adresse("D", "Sur l'alpage"))),
		                                                                  new SubsnapshotData(4L, Collections.singletonList(new Adresse("D", "Dans le fossé"))))));

		Historizer.historize(input, Collections.singletonList(collector));

		final Map<Long, List<DateRangeHelper.Ranged<Adresse>>> collected = collector.getCollectedData();
		Assert.assertEquals(4, collected.size());
		{
			final List<DateRangeHelper.Ranged<Adresse>> adresses = collected.get(1L);
			Assert.assertNotNull(adresses);
			Assert.assertEquals(2, adresses.size());

			final List<DateRangeHelper.Ranged<Adresse>> sorted = new ArrayList<>(adresses);
			Collections.sort(sorted, Comparator.comparing(a -> a.getPayload().type));
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2000, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Rue du Lac", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2002, 12, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Rue du Lac, Case postale", a.getPayload().adresse);
			}
		}
		{
			final List<DateRangeHelper.Ranged<Adresse>> adresses = collected.get(2L);
			Assert.assertNotNull(adresses);
			Assert.assertEquals(2, adresses.size());

			final List<DateRangeHelper.Ranged<Adresse>> sorted = new ArrayList<>(adresses);
			Collections.sort(sorted, Comparator.comparing(a -> a.getPayload().type));
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2001, 6, 3), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Dans les bois", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2002, 12, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Dans les bois, Case postale", a.getPayload().adresse);
			}
		}
		{
			final List<DateRangeHelper.Ranged<Adresse>> adresses = collected.get(3L);
			Assert.assertNotNull(adresses);
			Assert.assertEquals(2, adresses.size());

			final List<DateRangeHelper.Ranged<Adresse>> sorted = new ArrayList<>(adresses);
			Collections.sort(sorted, Comparator.comparing(a -> a.getPayload().type));
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2001, 6, 3), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Bahnhofstrasse 12", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2003, 2, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("D", a.getPayload().type);
				Assert.assertEquals("Sur l'alpage", a.getPayload().adresse);
			}
		}
		{
			final List<DateRangeHelper.Ranged<Adresse>> adresses = collected.get(4L);
			Assert.assertNotNull(adresses);
			Assert.assertEquals(2, adresses.size());

			final List<DateRangeHelper.Ranged<Adresse>> sorted = new ArrayList<>(adresses);
			Collections.sort(sorted, Comparator.comparing(a -> a.getPayload().type));
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2000, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2001, 6, 2), a.getDateFin());
				Assert.assertEquals("C", a.getPayload().type);
				Assert.assertEquals("Sur le toit", a.getPayload().adresse);
			}
			{
				final DateRangeHelper.Ranged<Adresse> a = adresses.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2002, 12, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("D", a.getPayload().type);
				Assert.assertEquals("Dans le fossé", a.getPayload().adresse);
			}
		}
	}

	/**
	 * Utilisation d'un extracteur qui renvoie des Keyed avec un chargement nul. Vérifie que le collecteur les ignore correctement.
	 */
	@Test
	public void testSingleValueIndexedWithHolesAndFaultyExtracor() throws Exception {

		final class SubsnapshotData {
			final Long idSub;
			final String dataValue;

			public SubsnapshotData(Long idSub, String dataValue) {
				this.idSub = idSub;
				this.dataValue = dataValue;
			}
		}

		final class SnapshotData {
			final List<SubsnapshotData> subs;

			public SnapshotData(List<SubsnapshotData> subs) {
				this.subs = subs;
			}
		}

		final Function<SnapshotData, Stream<Keyed<Long, String>>> dataExtractor = s ->
				s.subs.stream()
						.map(sub -> new Keyed<>(sub.idSub, sub.dataValue));


		final IndexedDataCollector<SnapshotData, String, Long> collector = new SingleValueIndexedDataCollector<>(dataExtractor, Equalator.DEFAULT);

		final Map<RegDate, SnapshotData> input = new HashMap<>();
		input.put(RegDate.get(2000, 1, 1), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, "Blah1"),
		                                                                  new SubsnapshotData(2L, "Blah1"),
		                                                                  new SubsnapshotData(3L, "Blah1")
		)));
		input.put(RegDate.get(2001, 6, 3), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, "Blah1"),
		                                                                  new SubsnapshotData(2L, "Blah1"),
		                                                                  new SubsnapshotData(3L, "Blah1")
		)));
		input.put(RegDate.get(2002, 12, 1), new SnapshotData(Arrays.asList(                                  // <-- Cas nominal d'absence
																		   new SubsnapshotData(2L, null),    // <-- Cas toléré d'absence (un Keyed(id, null) est créé
		                                                                   new SubsnapshotData(3L, "Blah1")
		)));
		input.put(RegDate.get(2003, 2, 1), new SnapshotData(Arrays.asList(new SubsnapshotData(1L, "Blah1"),
		                                                                  new SubsnapshotData(2L, "Blah1"),
		                                                                  new SubsnapshotData(3L, "Blah1")
		)));

		Historizer.historize(input, Collections.singletonList(collector));

		final Map<Long, List<DateRangeHelper.Ranged<String>>> collected = collector.getCollectedData();
		Assert.assertEquals(3, collected.size());
		{
			final List<DateRangeHelper.Ranged<String>> dataValues = collected.get(1L);
			Assert.assertNotNull(dataValues);
			Assert.assertEquals(2, dataValues.size());

			final List<DateRangeHelper.Ranged<String>> sorted = new ArrayList<>(dataValues);
			Collections.sort(sorted, Comparator.comparing(DateRangeHelper.Ranged::getDateDebut));
			{
				final DateRangeHelper.Ranged<String> a = dataValues.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2000, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("Blah1", a.getPayload());
			}
			{
				final DateRangeHelper.Ranged<String> a = dataValues.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2003, 2, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("Blah1", a.getPayload());
			}
		}
		{
			final List<DateRangeHelper.Ranged<String>> dataValues = collected.get(2L);
			Assert.assertNotNull(dataValues);
			Assert.assertEquals(2, dataValues.size());

			final List<DateRangeHelper.Ranged<String>> sorted = new ArrayList<>(dataValues);
			Collections.sort(sorted, Comparator.comparing(DateRangeHelper.Ranged::getDateDebut));
			{
				final DateRangeHelper.Ranged<String> a = dataValues.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2000, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 11, 30), a.getDateFin());
				Assert.assertEquals("Blah1", a.getPayload());
			}
			{
				final DateRangeHelper.Ranged<String> a = dataValues.get(1);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2003, 2, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("Blah1", a.getPayload());
			}
		}
		{
			final List<DateRangeHelper.Ranged<String>> dataValues = collected.get(3L);
			Assert.assertNotNull(dataValues);
			Assert.assertEquals(1, dataValues.size());

			final List<DateRangeHelper.Ranged<String>> sorted = new ArrayList<>(dataValues);
			Collections.sort(sorted, Comparator.comparing(DateRangeHelper.Ranged::getDateDebut));
			{
				final DateRangeHelper.Ranged<String> a = dataValues.get(0);
				Assert.assertNotNull(a);
				Assert.assertEquals(RegDate.get(2000, 1, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals("Blah1", a.getPayload());
			}
		}
	}
}
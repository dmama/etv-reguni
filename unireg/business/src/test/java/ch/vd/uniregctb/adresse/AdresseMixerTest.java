package ch.vd.uniregctb.adresse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AdresseMixerTest {

	@Test
	public void testSplitAt() throws Exception {

		// cas limites
		assertNull(AdresseMixer.splitAt(null, null));
		assertNull(AdresseMixer.splitAt(null, Collections.<RegDate>emptySet()));
		assertEmpty(AdresseMixer.splitAt(Collections.<AdresseGenerique>emptyList(), null));
		assertEmpty(AdresseMixer.splitAt(Collections.<AdresseGenerique>emptyList(), Collections.<RegDate>emptySet()));

		// une seule date qui coupe
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(null, null)), newSet(date(2000, 1, 1))),
				newAdresse(null, date(1999, 12, 31)), newAdresse(date(2000, 1, 1), null));
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(null, date(2010, 3, 12))), newSet(date(2000, 1, 1))),
				newAdresse(null, date(1999, 12, 31)), newAdresse(date(2000, 1, 1), date(2010, 3, 12)));
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(date(1955, 11, 22), date(2010, 3, 12))), newSet(date(2000, 1, 1))),
				newAdresse(date(1955, 11, 22), date(1999, 12, 31)), newAdresse(date(2000, 1, 1), date(2010, 3, 12)));

		// une seule date qui ne coupe pas
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(null, null)), Collections.<RegDate>emptySet()),
				newAdresse(null, null));
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(null, date(2010, 3, 12))), newSet(date(2020, 1, 1))),
				newAdresse(null, date(2010, 3, 12)));
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(date(1955, 11, 22), date(2010, 3, 12))), newSet(date(1925, 1, 1))),
				newAdresse(date(1955, 11, 22), date(2010, 3, 12)));

		// deux dates qui coupent la même adresse
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(null, null)), newSet(date(2000, 1, 1), date(2007, 7, 1))),
				newAdresse(null, date(1999, 12, 31)), newAdresse(date(2000, 1, 1), date(2007, 6, 30)), newAdresse(date(2007, 7, 1), null));
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(null, date(2010, 3, 12))), newSet(date(2000, 1, 1), date(2007, 7, 1))),
				newAdresse(null, date(1999, 12, 31)), newAdresse(date(2000, 1, 1), date(2007, 6, 30)), newAdresse(date(2007, 7, 1), date(2010, 3, 12)));
		assertRanges(AdresseMixer.splitAt(Arrays.asList(newAdresse(date(1955, 11, 22), date(2010, 3, 12))), newSet(date(2000, 1, 1), date(2007, 7, 1))),
				newAdresse(date(1955, 11, 22), date(1999, 12, 31)), newAdresse(date(2000, 1, 1), date(2007, 6, 30)), newAdresse(date(2007, 7, 1), date(2010, 3, 12)));
	}

	private static <T> Set<T> newSet(T... val) {
		Set<T> s = new HashSet<T>();
		Collections.addAll(s, val);
		return s;
	}

	private static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}

	private static void assertRanges(List<? extends DateRange> actual, DateRange... expected) {
		assertNotNull(actual);
		assertNotNull(expected);
		assertEquals(expected.length, actual.size());
		for (int i = 0, expectedLength = expected.length; i < expectedLength; i++) {
			final DateRange e = expected[i];
			final DateRange a = actual.get(i);
			assertEquals(e.getDateDebut(), a.getDateDebut());
			assertEquals(e.getDateFin(), a.getDateFin());
		}
	}

	private static AdresseGenerique newAdresse(@Nullable RegDate from, @Nullable RegDate to) {
		return new MockAdresseGeneric(from, to, null);
	}

	private static void assertEmpty(Collection<?> collection) {
		assertTrue(collection == null || collection.isEmpty());
	}
}

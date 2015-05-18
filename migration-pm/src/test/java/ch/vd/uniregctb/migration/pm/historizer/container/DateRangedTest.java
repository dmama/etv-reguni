package ch.vd.uniregctb.migration.pm.historizer.container;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateRangedTest {
	final String payload = "My payload.";

	final int year = 2015;
	final int month = 5;

	final int beforeDay = 1;
	final RegDate dateBefore = RegDateHelper.get(year, month, beforeDay);

	final int beginDay = 10;
	final RegDate dateBegin = RegDateHelper.get(year, month, beginDay);

	final int insideDay = 15;
	final RegDate dateInside = RegDateHelper.get(year, month, insideDay);

	final int endDay = 20;
	final RegDate dateEnd = RegDateHelper.get(year, month, endDay);

	final int afterDay = 30;
	final RegDate dateAfter = RegDateHelper.get(year, month, afterDay);

	// For separate testing of derived range.
	final int newEndDay = 21;
	final RegDate newDateEnd = RegDateHelper.get(year, month, newEndDay);

	final DateRanged<String> dateranged = new DateRanged<>(RegDateHelper.get(year, month, beginDay), RegDateHelper.get(year, month, endDay), payload);
	// Derived range to test as strictly as newly created range.
	final DateRanged<String> newDateranged = dateranged.withDateFin(newDateEnd);


	@Test
	public void testIsValidAt() {
		assertFalse(dateranged.isValidAt(dateBefore));
		assertTrue(dateranged.isValidAt(dateBegin));
		assertTrue(dateranged.isValidAt(dateInside));
		assertTrue(dateranged.isValidAt(dateEnd));
		assertFalse(dateranged.isValidAt(dateAfter));

		/*
			Testing derived range
		 */
		assertFalse(newDateranged.isValidAt(dateBefore));
		assertTrue(newDateranged.isValidAt(dateBegin));
		assertTrue(newDateranged.isValidAt(dateInside));
		assertTrue(newDateranged.isValidAt(newDateEnd));
		assertFalse(newDateranged.isValidAt(dateAfter));
	}

	@Test
	public void didNotLosePayload() {
		assertEquals(payload, dateranged.getPayload());

		/*
			Testing derived range
		 */
		assertEquals(payload, newDateranged.getPayload());
	}
}

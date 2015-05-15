package ch.vd.uniregctb.migration.pm.historizer.container;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateRangedTest {
	String payload = "My payload.";

	int year = 2015;
	int month = 5;
	int beforeDay = 1;
	int beginDay = 10;
	int insideDay = 15;
	int endDay = 20;
	int newEndDay = 21;
	int afterDay = 30;

	DateRanged<String> dateranged = new DateRanged<>(RegDateHelper.get(year, month, beginDay), RegDateHelper.get(year, month, endDay), payload);
	DateRanged<String> newDateranged;

	RegDate dateBefore = RegDateHelper.get(year, month, beforeDay);
	RegDate dateBegin = RegDateHelper.get(year, month, beginDay);
	RegDate dateInside = RegDateHelper.get(year, month, insideDay);
	RegDate dateEnd = RegDateHelper.get(year, month, endDay);
	RegDate newDateEnd = RegDateHelper.get(year, month, newEndDay);
	RegDate dateAfter = RegDateHelper.get(year, month, afterDay);

	@Before
	public void setUp() {
		newDateranged = dateranged.withDateFin(newDateEnd);
	}

	@Test
	public void testIsValidAt() {
		assertFalse(dateranged.isValidAt(dateBefore));
		assertTrue(dateranged.isValidAt(dateBegin));
		assertTrue(dateranged.isValidAt(dateInside));
		assertTrue(dateranged.isValidAt(dateEnd));
		assertFalse(dateranged.isValidAt(dateAfter));

		assertFalse(newDateranged.isValidAt(dateBefore));
		assertTrue(newDateranged.isValidAt(dateBegin));
		assertTrue(newDateranged.isValidAt(dateInside));
		assertTrue(newDateranged.isValidAt(newDateEnd));
		assertFalse(newDateranged.isValidAt(dateAfter));
	}

	@Test
	public void didNotLosePayload() {
		assertEquals(payload, dateranged.getPayload());
		assertEquals(payload, newDateranged.getPayload());
	}
}

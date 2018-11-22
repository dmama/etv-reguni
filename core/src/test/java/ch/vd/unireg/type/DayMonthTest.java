package ch.vd.unireg.type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;

public class DayMonthTest extends WithoutSpringTest {

	@Test
	public void testToString() {
		Assert.assertEquals("0101", DayMonth.get(1, 1).toString());
		Assert.assertEquals("1231", DayMonth.get(12, 31).toString());
	}

	@Test
	public void testFromString() {
		Assert.assertEquals(DayMonth.get(1, 1), DayMonth.fromString("0101"));
		Assert.assertEquals(DayMonth.get(12, 31), DayMonth.fromString("1231"));
	}

	@Test
	public void testFromRegDate() {
		final RegDate today = RegDate.get();
		Assert.assertEquals(today.day(), DayMonth.get(today).day());
		Assert.assertEquals(today.month(), DayMonth.get(today).month());
		Assert.assertSame(DayMonth.get(), DayMonth.get(today));

		try {
			DayMonth.get(RegDate.get(2015));
			Assert.fail("Aurait dû échouer pour cause de date partielle !");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Date partielle non acceptée.", e.getMessage());
		}

		try {
			DayMonth.get(RegDate.get(2015, 5));
			Assert.fail("Aurait dû échouer pour cause de date partielle !");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Date partielle non acceptée.", e.getMessage());
		}
	}

	@Test
	public void testIndex() {
		Assert.assertEquals(102, DayMonth.get(1, 2).index());
		Assert.assertEquals(1231, DayMonth.get(12, 31).index());
		Assert.assertEquals(DayMonth.get(5, 13), DayMonth.fromIndex(513));
		Assert.assertEquals(DayMonth.get(5, 1), DayMonth.fromIndex(501));
		Assert.assertEquals(DayMonth.get(12, 31), DayMonth.fromIndex(1231));

		try {
			DayMonth.fromIndex(12345);
			Assert.fail("Aurait dû échouer pour cause d'index invalide !");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Month should be between 1 and 12 (found 123).", e.getMessage());
		}

		try {
			DayMonth.fromIndex(1234);
			Assert.fail("Aurait dû échouer pour cause d'index invalide !");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Day should be between 1 and 31 in month 12 (found 34).", e.getMessage());
		}
	}

	@Test
	public void testNextAfterDateComplete() {
		// pas fin de mois
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 3, 1)));
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 3, 15)));
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 11, 25)));

		// fin de mois
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfter(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfter(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfter(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfter(RegDate.get(2015, 3, 30)));
		Assert.assertEquals(RegDate.get(2016, 3, 31), DayMonth.get(3, 31).nextAfter(RegDate.get(2015, 3, 31)));
		Assert.assertEquals(RegDate.get(2016, 3, 31), DayMonth.get(3, 31).nextAfter(RegDate.get(2015, 11, 25)));

		// fin de mois de février
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).nextAfter(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).nextAfter(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).nextAfter(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).nextAfter(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 28).nextAfter(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 29).nextAfter(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 28).nextAfter(RegDate.get(2015, 11, 25)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 29).nextAfter(RegDate.get(2015, 11, 25)));
	}

	@Test
	public void testPreviousBeforeDateComplete() {
		// pas fin de mois
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 3, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 3, 15)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 11, 25)));

		// fin de mois
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBefore(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBefore(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBefore(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBefore(RegDate.get(2015, 3, 30)));
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBefore(RegDate.get(2015, 3, 31)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).previousBefore(RegDate.get(2015, 11, 25)));

		// fin de mois de février
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).previousBefore(RegDate.get(2016, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).previousBefore(RegDate.get(2016, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).previousBefore(RegDate.get(2016, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).previousBefore(RegDate.get(2016, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).previousBefore(RegDate.get(2016, 2, 29)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).previousBefore(RegDate.get(2016, 2, 29)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 28).previousBefore(RegDate.get(2016, 11, 25)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 29).previousBefore(RegDate.get(2016, 11, 25)));
	}

	@Test
	public void testNextAfterDatePartielle() {
		// année seule connue
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015)));
		Assert.assertEquals(RegDate.get(2016, 1, 1), DayMonth.get(1, 1).nextAfter(RegDate.get(2015)));
		Assert.assertEquals(RegDate.get(2016, 12, 31), DayMonth.get(12, 31).nextAfter(RegDate.get(2015)));

		// année et mois connus
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfter(RegDate.get(2015, 4)));

		Assert.assertEquals(RegDate.get(2016, 1, 1), DayMonth.get(1, 1).nextAfter(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2016, 1, 1), DayMonth.get(1, 1).nextAfter(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2016, 1, 1), DayMonth.get(1, 1).nextAfter(RegDate.get(2015, 4)));

		Assert.assertEquals(RegDate.get(2015, 12, 31), DayMonth.get(12, 31).nextAfter(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2015, 12, 31), DayMonth.get(12, 31).nextAfter(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2015, 12, 31), DayMonth.get(12, 31).nextAfter(RegDate.get(2015, 4)));
	}

	@Test
	public void testPreviousBeforeDatePartielle() {
		// année seule connue
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015)));
		Assert.assertEquals(RegDate.get(2014, 1, 1), DayMonth.get(1, 1).previousBefore(RegDate.get(2015)));
		Assert.assertEquals(RegDate.get(2014, 12, 31), DayMonth.get(12, 31).previousBefore(RegDate.get(2015)));

		// année et mois connus
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBefore(RegDate.get(2015, 4)));

		Assert.assertEquals(RegDate.get(2015, 1, 1), DayMonth.get(1, 1).previousBefore(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2015, 1, 1), DayMonth.get(1, 1).previousBefore(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2015, 1, 1), DayMonth.get(1, 1).previousBefore(RegDate.get(2015, 4)));

		Assert.assertEquals(RegDate.get(2014, 12, 31), DayMonth.get(12, 31).previousBefore(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2014, 12, 31), DayMonth.get(12, 31).previousBefore(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2014, 12, 31), DayMonth.get(12, 31).previousBefore(RegDate.get(2015, 4)));
	}

	@Test
	public void testNextAfterOrEqualDateComplete() {
		// pas fin de mois
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 3, 1)));
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 3, 15)));
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 11, 25)));

		// fin de mois
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfterOrEqual(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfterOrEqual(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfterOrEqual(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfterOrEqual(RegDate.get(2015, 3, 30)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).nextAfterOrEqual(RegDate.get(2015, 3, 31)));
		Assert.assertEquals(RegDate.get(2016, 3, 31), DayMonth.get(3, 31).nextAfterOrEqual(RegDate.get(2015, 11, 25)));

		// fin de mois de février
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).nextAfterOrEqual(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).nextAfterOrEqual(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).nextAfterOrEqual(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).nextAfterOrEqual(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).nextAfterOrEqual(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).nextAfterOrEqual(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 28).nextAfterOrEqual(RegDate.get(2015, 11, 25)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 29).nextAfterOrEqual(RegDate.get(2015, 11, 25)));
	}

	@Test
	public void testPreviousBeforeOrEqualDateComplete() {
		// pas fin de mois
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 3, 1)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 3, 15)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 11, 25)));

		// fin de mois
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBeforeOrEqual(RegDate.get(2015, 1, 1)));
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBeforeOrEqual(RegDate.get(2015, 2, 1)));
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBeforeOrEqual(RegDate.get(2015, 2, 28)));
		Assert.assertEquals(RegDate.get(2014, 3, 31), DayMonth.get(3, 31).previousBeforeOrEqual(RegDate.get(2015, 3, 30)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).previousBeforeOrEqual(RegDate.get(2015, 3, 31)));
		Assert.assertEquals(RegDate.get(2015, 3, 31), DayMonth.get(3, 31).previousBeforeOrEqual(RegDate.get(2015, 11, 25)));

		// fin de mois de février
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).previousBeforeOrEqual(RegDate.get(2016, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).previousBeforeOrEqual(RegDate.get(2016, 1, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 28).previousBeforeOrEqual(RegDate.get(2016, 2, 1)));
		Assert.assertEquals(RegDate.get(2015, 2, 28), DayMonth.get(2, 29).previousBeforeOrEqual(RegDate.get(2016, 2, 1)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 28).previousBeforeOrEqual(RegDate.get(2016, 2, 29)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 29).previousBeforeOrEqual(RegDate.get(2016, 2, 29)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 28).previousBeforeOrEqual(RegDate.get(2016, 11, 25)));
		Assert.assertEquals(RegDate.get(2016, 2, 29), DayMonth.get(2, 29).previousBeforeOrEqual(RegDate.get(2016, 11, 25)));
	}

	@Test
	public void testNextAfterOrEqualDatePartielle() {
		// année seule connue
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015)));
		Assert.assertEquals(RegDate.get(2015, 1, 1), DayMonth.get(1, 1).nextAfterOrEqual(RegDate.get(2015)));
		Assert.assertEquals(RegDate.get(2015, 12, 31), DayMonth.get(12, 31).nextAfterOrEqual(RegDate.get(2015)));

		// année et mois connus
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2016, 3, 1), DayMonth.get(3, 1).nextAfterOrEqual(RegDate.get(2015, 4)));

		Assert.assertEquals(RegDate.get(2016, 1, 1), DayMonth.get(1, 1).nextAfterOrEqual(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2016, 1, 1), DayMonth.get(1, 1).nextAfterOrEqual(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2016, 1, 1), DayMonth.get(1, 1).nextAfterOrEqual(RegDate.get(2015, 4)));

		Assert.assertEquals(RegDate.get(2015, 12, 31), DayMonth.get(12, 31).nextAfterOrEqual(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2015, 12, 31), DayMonth.get(12, 31).nextAfterOrEqual(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2015, 12, 31), DayMonth.get(12, 31).nextAfterOrEqual(RegDate.get(2015, 4)));
	}

	@Test
	public void testPreviousBeforeOrEqualDatePartielle() {
		// année seule connue
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015)));
		Assert.assertEquals(RegDate.get(2015, 1, 1), DayMonth.get(1, 1).previousBeforeOrEqual(RegDate.get(2015)));
		Assert.assertEquals(RegDate.get(2015, 12, 31), DayMonth.get(12, 31).previousBeforeOrEqual(RegDate.get(2015)));

		// année et mois connus
		Assert.assertEquals(RegDate.get(2014, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2015, 3, 1), DayMonth.get(3, 1).previousBeforeOrEqual(RegDate.get(2015, 4)));

		Assert.assertEquals(RegDate.get(2015, 1, 1), DayMonth.get(1, 1).previousBeforeOrEqual(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2015, 1, 1), DayMonth.get(1, 1).previousBeforeOrEqual(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2015, 1, 1), DayMonth.get(1, 1).previousBeforeOrEqual(RegDate.get(2015, 4)));

		Assert.assertEquals(RegDate.get(2014, 12, 31), DayMonth.get(12, 31).previousBeforeOrEqual(RegDate.get(2015, 2)));
		Assert.assertEquals(RegDate.get(2014, 12, 31), DayMonth.get(12, 31).previousBeforeOrEqual(RegDate.get(2015, 3)));
		Assert.assertEquals(RegDate.get(2014, 12, 31), DayMonth.get(12, 31).previousBeforeOrEqual(RegDate.get(2015, 4)));
	}

	@Test
	public void testSerialisation() throws Exception {
		final DayMonth source = DayMonth.get(10, 31);
		final DayMonth clone = DayMonth.get(10, 31);
		Assert.assertSame(source, clone);

		// sérialisation
		final byte[] serializedData;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(out)) {
			oos.writeObject(source);
			oos.flush();
			serializedData = out.toByteArray();
		}

		// désérialisation
		final DayMonth extracted;
		try (ByteArrayInputStream in = new ByteArrayInputStream(serializedData); ObjectInputStream ois = new ObjectInputStream(in)) {
			final Object object = ois.readObject();
			Assert.assertNotNull(object);
			Assert.assertEquals(DayMonth.class, object.getClass());
			extracted = (DayMonth) object;
		}
		Assert.assertSame(source, extracted);
	}

	private static boolean isDayMonthSameDayOfTheMonth(RegDate d1, RegDate d2) {
		return d1.day() == d2.day()
				|| (d1 == d1.getLastDayOfTheMonth() && d2 == d2.getLastDayOfTheMonth())
				|| (d1 == d1.getLastDayOfTheMonth() && d2.month() == 2 && d2.day() >= 28)
				|| (d2 == d2.getLastDayOfTheMonth() && d1.month() == 2 && d1.day() >= 28);
	}

	@Test
	public void testIsSameDayOfMonthNonLeapYear() {
		final int annee = 2015;
		for (RegDate cursor1 = date(annee, 1, 1) ; cursor1.year() == annee ; cursor1 = cursor1.getOneDayAfter()) {
			for (RegDate cursor2 = date(annee, 1, 1) ; cursor2.year() == annee ; cursor2 = cursor2.getOneDayAfter()) {
				final boolean expected = isDayMonthSameDayOfTheMonth(cursor1, cursor2);
				Assert.assertEquals(String.format("%s/%s", cursor1, cursor2), expected, DayMonth.isSameDayOfMonth(DayMonth.get(cursor1), DayMonth.get(cursor2)));
			}
		}
	}

	@Test
	public void testIsSameDayOfMonthLeapYear() {
		final int annee = 2016;
		for (RegDate cursor1 = date(annee, 1, 1) ; cursor1.year() == annee ; cursor1 = cursor1.getOneDayAfter()) {
			for (RegDate cursor2 = date(annee, 1, 1) ; cursor2.year() == annee ; cursor2 = cursor2.getOneDayAfter()) {
				final boolean expected = isDayMonthSameDayOfTheMonth(cursor1, cursor2);
				Assert.assertEquals(String.format("%s/%s", cursor1, cursor2), expected, DayMonth.isSameDayOfMonth(DayMonth.get(cursor1), DayMonth.get(cursor2)));
			}
		}
	}
}

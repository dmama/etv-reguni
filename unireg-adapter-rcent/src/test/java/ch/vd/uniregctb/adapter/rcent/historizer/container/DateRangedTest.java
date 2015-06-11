package ch.vd.uniregctb.adapter.rcent.historizer.container;

import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class DateRangedTest {
	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final String payload = "My payload.";

	private final int year = 2015;
	private final int month = 5;

	private final int beforeDay = 1;
	private final RegDate dateBefore = RegDateHelper.get(year, month, beforeDay);

	private final int beginDay = 10;
	private final RegDate dateBegin = RegDateHelper.get(year, month, beginDay);

	private final int insideDay = 15;
	private final RegDate dateInside = RegDateHelper.get(year, month, insideDay);

	private final int endDay = 20;
	private final RegDate dateEnd = RegDateHelper.get(year, month, endDay);

	private final int afterDay = 30;
	private final RegDate dateAfter = RegDateHelper.get(year, month, afterDay);

	// For separate testing of derived range.
	private final int newEndDay = 21;
	private final RegDate newDateEnd = RegDateHelper.get(year, month, newEndDay);

	private final DateRanged<String> dateranged = new DateRanged<>(RegDateHelper.get(year, month, beginDay), RegDateHelper.get(year, month, endDay), payload);
	// Derived range to test as strictly as newly created range.
	private final DateRanged<String> newDateranged = dateranged.withDateFin(newDateEnd);


	@Test
	public void isValidAt() {
		assertThat(dateranged.isValidAt(dateBefore), is(false));
		assertThat(dateranged.isValidAt(dateBegin), is(true));
		assertThat(dateranged.isValidAt(dateInside), is(true));
		assertThat(dateranged.isValidAt(dateEnd), is(true));
		assertThat(dateranged.isValidAt(dateAfter), is(false));

		/*
			Testing derived range
		 */
		assertThat(newDateranged.isValidAt(dateBefore), is(false));
		assertThat(newDateranged.isValidAt(dateBegin), is(true));
		assertThat(newDateranged.isValidAt(dateInside), is(true));
		assertThat(newDateranged.isValidAt(newDateEnd), is(true));
		assertThat(newDateranged.isValidAt(dateAfter), is(false));
	}

	@Test
	public void didNotLosePayload() {
		assertThat(payload, equalTo(dateranged.getPayload()));
		/*
			Testing derived range
		 */
		assertThat(payload, equalTo(newDateranged.getPayload()));
	}

	@Test
	public void cannotCreateInvalidRange() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Tentative de créer une période dont le début [");
		DateRanged<String> range = new DateRanged<>(RegDateHelper.get(2015, 5, 20),
		                                            RegDateHelper.get(2015, 5, 10),
		                                            payload);
		assertThat(range, notNullValue());
	}

	@Test
	public void canCreateOpenRange() {
		DateRanged<String> range = new DateRanged<>(RegDateHelper.get(2015, 5, 20), null, payload);
		assertThat(range, notNullValue());
		assertThat(range.isValidAt(RegDateHelper.get(2044, 6, 6)), is(true));
	}

	@Test
	public void canCreateSingleDayRange() {
		DateRanged<String> range = new DateRanged<>(RegDateHelper.get(2015, 5, 20), RegDateHelper.get(2015, 5, 20), payload);
		assertThat(range, notNullValue());
		assertThat(range.isValidAt(RegDateHelper.get(2015, 5, 19)), is(false));
		assertThat(range.isValidAt(RegDateHelper.get(2015, 5, 20)), is(true));
		assertThat(range.isValidAt(RegDateHelper.get(2015, 5, 21)), is(false));
	}

	@Test
	public void testMap() {
		final RegDate base = RegDate.get(2000, 1, 1);
		final Random rnd = new Random();
		final RegDate debut = base.addDays(rnd.nextInt(1000));
		final RegDate fin = debut.addDays(rnd.nextInt(500));

		{
			// mapping chaîne -> longueur de la chaîne
			final DateRanged<String> range = new DateRanged<>(debut, fin, "MaDonnéeQuiVaBien");
			final DateRanged<Integer> longueur = range.map(String::length);
			assertThat(longueur.getDateDebut(), equalTo(debut));
			assertThat(longueur.getDateFin(), equalTo(fin));
			assertThat(longueur.getPayload(), equalTo(17));

			// mapping chaîne -> nombre de majuscules dans la chaîne
			final DateRanged<Long> nombreMajuscules = range.map(s -> s.chars().map(i -> (char) i).filter(Character::isUpperCase).count());
			assertThat(nombreMajuscules.getDateDebut(), equalTo(debut));
			assertThat(nombreMajuscules.getDateFin(), equalTo(fin));
			assertThat(nombreMajuscules.getPayload(), equalTo(5L));
		}
		{
			// mapping chaîne -> longueur de la chaîne
			final DateRanged<String> range = new DateRanged<>(debut, fin, "TaDonnéeQuiVaToutAussiBien");
			final DateRanged<Integer> mappe = range.map(String::length);
			assertThat(mappe.getDateDebut(), equalTo(debut));
			assertThat(mappe.getDateFin(), equalTo(fin));
			assertThat(mappe.getPayload(), equalTo(26));

			// mapping chaîne -> nombre de majuscules dans la chaîne
			final DateRanged<Long> nombreMajuscules = range.map(s -> s.chars().map(i -> (char) i).filter(Character::isUpperCase).count());
			assertThat(nombreMajuscules.getDateDebut(), equalTo(debut));
			assertThat(nombreMajuscules.getDateFin(), equalTo(fin));
			assertThat(nombreMajuscules.getPayload(), equalTo(7L));
		}
	}
}

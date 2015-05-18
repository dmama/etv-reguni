package ch.vd.uniregctb.migration.pm.historizer.container;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
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
		assertThat(dateranged.isValidAt(dateBefore), notNullValue());
		assertThat(dateranged.isValidAt(dateBegin), notNullValue());
		assertThat(dateranged.isValidAt(dateInside), notNullValue());
		assertThat(dateranged.isValidAt(dateEnd), notNullValue());
		assertThat(dateranged.isValidAt(dateAfter), notNullValue());

		/*
			Testing derived range
		 */
		assertThat(newDateranged.isValidAt(dateBefore), notNullValue());
		assertThat(newDateranged.isValidAt(dateBegin), notNullValue());
		assertThat(newDateranged.isValidAt(dateInside), notNullValue());
		assertThat(newDateranged.isValidAt(newDateEnd), notNullValue());
		assertThat(newDateranged.isValidAt(dateAfter), notNullValue());
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
}

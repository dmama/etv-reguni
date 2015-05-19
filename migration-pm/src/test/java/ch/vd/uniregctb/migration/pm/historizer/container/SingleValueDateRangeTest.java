package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.vd.registre.base.date.RegDateHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class SingleValueDateRangeTest {

	private final String payload = "abracadabra";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testCreateNormal() {
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 3), RegDateHelper.get(2015, 5, 4), payload),
				new DateRanged<>(RegDateHelper.get(2016, 8, 10), RegDateHelper.get(2016, 8, 20), payload),
				new DateRanged<>(RegDateHelper.get(2025, 5, 25), RegDateHelper.get(2025, 5, 28), payload)
		));
		SingleValueDateRanges<String> svdr = new SingleValueDateRanges<>(values);
		assertThat(svdr, notNullValue());
	}

	@Test
	public void testCreateWithOpenEndedLastRange() {
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 3), RegDateHelper.get(2015, 5, 4), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 10), null, payload)
		));
		SingleValueDateRanges<String> svdr = new SingleValueDateRanges<>(values);
		assertThat(svdr, notNullValue());
	}

	@Test
	public void testCreateWithOverlappingValues() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Essai d'ajouter une période chevauchant la précédente");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 4), payload)
		));
		SingleValueDateRanges<String> svdr = new SingleValueDateRanges<>(values);
		assertThat(svdr, notNullValue());
	}

	@Test
	public void testCreateWithMisplacedOpenEndedRange() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Essai d'ajouter une période chevauchant la précédente");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 3), null, payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 10), RegDateHelper.get(2015, 5, 20), payload)
		));
		SingleValueDateRanges<String> svdr = new SingleValueDateRanges<>(values);
		assertThat(svdr, notNullValue());
	}
}

package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.vd.registre.base.date.RegDateHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class SequentialDateRangesCheckerTest {

	private final String payload = "abracadabra";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testValidList() {
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 3), RegDateHelper.get(2015, 5, 4), payload),
				new DateRanged<>(RegDateHelper.get(2016, 8, 10), RegDateHelper.get(2016, 8, 20), payload),
				new DateRanged<>(RegDateHelper.get(2025, 5, 25), RegDateHelper.get(2025, 5, 28), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values);
		SequentialDateRangesChecker.ensureSequential(values.stream());
	}

	@Test
	public void testValidListWithOpenEndedLastRange() {
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 3), RegDateHelper.get(2015, 5, 4), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 10), null, payload)
		));
		SequentialDateRangesChecker.ensureSequential(values);
		SequentialDateRangesChecker.ensureSequential(values.stream());
	}

	@Test
	public void testOverlappingRanges() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Séquence invalide: deux périodes se chevauchent");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 4), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values);
	}

	@Test
	public void testOverlappingRangesStream() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Séquence invalide: deux périodes se chevauchent");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 4), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values.stream());
	}

	@Test
	public void testOverlappingRangesSameDay() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Séquence invalide: deux périodes se chevauchent");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 6, 3), payload),
				new DateRanged<>(RegDateHelper.get(2015, 6, 3), RegDateHelper.get(2015, 7, 4), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values);
	}
	@Test
	public void testOverlappingRangesSameDayStream() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Séquence invalide: deux périodes se chevauchent");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 6, 3), payload),
				new DateRanged<>(RegDateHelper.get(2015, 6, 3), RegDateHelper.get(2015, 7, 4), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values.stream());
	}

	@Test
	public void testOverlappingRangesEnclosingRanges() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Séquence invalide: deux périodes se chevauchent");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 7, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 6, 1), RegDateHelper.get(2015, 6, 4), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values);
	}
	@Test
	public void testOverlappingRangesEnclosingRangesStream() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Séquence invalide: deux périodes se chevauchent");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 7, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 6, 1), RegDateHelper.get(2015, 6, 4), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values.stream());
	}

	@Test
	public void testMisplacedOpenEndedRange() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Séquence invalide: deux périodes se chevauchent");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 3), null, payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 10), RegDateHelper.get(2015, 5, 20), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values);
	}
	@Test
	public void testMisplacedOpenEndedRangeStream() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Séquence invalide: deux périodes se chevauchent");
		List<DateRanged<String>> values = new ArrayList<>(Arrays.asList(
				new DateRanged<>(RegDateHelper.get(2015, 5, 1), RegDateHelper.get(2015, 5, 2), payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 3), null, payload),
				new DateRanged<>(RegDateHelper.get(2015, 5, 10), RegDateHelper.get(2015, 5, 20), payload)
		));
		SequentialDateRangesChecker.ensureSequential(values.stream());
	}
}

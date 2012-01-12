package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class GentilComparatorTest extends WithoutSpringTest {

	@Test
	public void testCasTousConnus() throws Exception {

		// on notera que les noms ne sont pas dans l'ordre alphabétique, pour être sûr...
		final GentilComparator<String> comparator = new GentilComparator<String>(Arrays.asList("Un", "Deux", "Trois"));
		final List<String> aTrier = new ArrayList<String>(3);
		aTrier.add("Deux");
		aTrier.add("Trois");
		aTrier.add("Un");

		Assert.assertEquals("Deux", aTrier.get(0));
		Assert.assertEquals("Trois", aTrier.get(1));
		Assert.assertEquals("Un", aTrier.get(2));

		Collections.sort(aTrier, comparator);

		Assert.assertEquals("Un", aTrier.get(0));
		Assert.assertEquals("Deux", aTrier.get(1));
		Assert.assertEquals("Trois", aTrier.get(2));
	}

	@Test
	public void testCasInconnus() throws Exception {

		// on notera que les noms ne sont pas dans l'ordre alphabétique, pour être sûr...
		final GentilComparator<String> comparator = new GentilComparator<String>(Arrays.asList("Un", "Deux", "Trois"));
		final List<String> aTrier = new ArrayList<String>(3);

		aTrier.add("Quatre");
		aTrier.add("Cinq");
		aTrier.add("Six");

		Assert.assertEquals("Quatre", aTrier.get(0));
		Assert.assertEquals("Cinq", aTrier.get(1));
		Assert.assertEquals("Six", aTrier.get(2));

		Collections.sort(aTrier, comparator);

		Assert.assertEquals("Quatre", aTrier.get(0));
		Assert.assertEquals("Cinq", aTrier.get(1));
		Assert.assertEquals("Six", aTrier.get(2));
	}

	@Test
	public void testCasMixte() throws Exception {

		// on notera que les noms ne sont pas dans l'ordre alphabétique, pour être sûr...
		final GentilComparator<String> comparator = new GentilComparator<String>(Arrays.asList("Un", "Deux", "Trois"));
		final List<String> aTrier = new ArrayList<String>(6);

		aTrier.add("Quatre");
		aTrier.add("Deux");
		aTrier.add("Cinq");
		aTrier.add("Trois");
		aTrier.add("Six");
		aTrier.add("Un");

		Assert.assertEquals("Quatre", aTrier.get(0));
		Assert.assertEquals("Deux", aTrier.get(1));
		Assert.assertEquals("Cinq", aTrier.get(2));
		Assert.assertEquals("Trois", aTrier.get(3));
		Assert.assertEquals("Six", aTrier.get(4));
		Assert.assertEquals("Un", aTrier.get(5));

		Collections.sort(aTrier, comparator);

		Assert.assertEquals("Un", aTrier.get(0));
		Assert.assertEquals("Deux", aTrier.get(1));
		Assert.assertEquals("Trois", aTrier.get(2));
		Assert.assertEquals("Quatre", aTrier.get(3));
		Assert.assertEquals("Cinq", aTrier.get(4));
		Assert.assertEquals("Six", aTrier.get(5));
	}
}

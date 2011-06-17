package ch.vd.uniregctb.webservices.tiers3.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class DataHelperTest extends WithoutSpringTest {

	/**
	 * Vérifie que toutes les parts Web sont reconnues par le DataHelper et qu'il ne lève pas d'exception
	 */
	@Test
	public void testPartsConversion() {
		final Set<TiersPart> set = new HashSet<TiersPart>();
		set.addAll(Arrays.asList(TiersPart.values()));
		DataHelper.webToCore(set);
	}
}

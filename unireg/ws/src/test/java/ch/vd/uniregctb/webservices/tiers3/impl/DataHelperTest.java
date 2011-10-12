package ch.vd.uniregctb.webservices.tiers3.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class DataHelperTest extends WithoutSpringTest {

	/**
	 * Vérifie que toutes les parts Web sont reconnues par le DataHelper et qu'il ne lève pas d'exception
	 */
	@Test
	public void testPartsConversion() {
		final Set<PartyPart> set = new HashSet<PartyPart>();
		set.addAll(Arrays.asList(PartyPart.values()));
		DataHelper.webToCore(set);
	}
}

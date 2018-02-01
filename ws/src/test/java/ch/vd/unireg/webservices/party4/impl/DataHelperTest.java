package ch.vd.unireg.webservices.party4.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.webservices.party4.PartyPart;
import ch.vd.unireg.common.WithoutSpringTest;

public class DataHelperTest extends WithoutSpringTest {

	/**
	 * Vérifie que toutes les parts Web sont reconnues par le DataHelper et qu'il ne lève pas d'exception
	 */
	@Test
	public void testPartsConversion() {
		final Set<PartyPart> set = new HashSet<>();
		set.addAll(Arrays.asList(PartyPart.values()));
		DataHelper.webToCore(set);
	}
}

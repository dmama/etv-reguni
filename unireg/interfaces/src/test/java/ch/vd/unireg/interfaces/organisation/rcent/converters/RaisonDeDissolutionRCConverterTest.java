package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.DissolutionReason;

public class RaisonDeDissolutionRCConverterTest {

	private final RaisonDeDissolutionRCConverter converter = new RaisonDeDissolutionRCConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(DissolutionReason.class, converter);
	}
}
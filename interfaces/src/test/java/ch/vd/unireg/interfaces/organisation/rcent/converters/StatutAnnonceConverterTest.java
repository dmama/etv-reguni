package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;

public class StatutAnnonceConverterTest {

	private final StatutAnnonceConverter converter = new StatutAnnonceConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(StatutAnnonce.class, converter);
	}

}
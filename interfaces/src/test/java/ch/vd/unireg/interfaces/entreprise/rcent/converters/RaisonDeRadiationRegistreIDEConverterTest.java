package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.unireg.interfaces.entreprise.data.RaisonDeRadiationRegistreIDE;

public class RaisonDeRadiationRegistreIDEConverterTest {

	private final RaisonDeRadiationRegistreIDEConverter converter = new RaisonDeRadiationRegistreIDEConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(RaisonDeRadiationRegistreIDE.class, converter);
	}
}
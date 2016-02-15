package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.Authorisation;

public class AutorisationConverterTest {

	private final AutorisationConverter converter = new AutorisationConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(Authorisation.class, converter);
	}
}
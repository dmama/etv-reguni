package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.UidDeregistrationReason;

public class UidRegisterRaisonDeRadiationRegistreIDEConverterTest {

	private final UidRegisterRaisonDeRadiationRegistreIDEConverter converter = new UidRegisterRaisonDeRadiationRegistreIDEConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(UidDeregistrationReason.class, converter);
	}
}
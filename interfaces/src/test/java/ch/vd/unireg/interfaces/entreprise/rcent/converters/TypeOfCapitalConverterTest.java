package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfCapital;

public class TypeOfCapitalConverterTest {

	private final TypeOfCapitalConverter converter = new TypeOfCapitalConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfCapital.class, converter);
	}
}
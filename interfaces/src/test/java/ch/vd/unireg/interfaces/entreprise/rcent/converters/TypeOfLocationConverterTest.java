package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfLocation;

public class TypeOfLocationConverterTest {

	private final TypeOfLocationConverter converter = new TypeOfLocationConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfLocation.class, converter);
	}
}
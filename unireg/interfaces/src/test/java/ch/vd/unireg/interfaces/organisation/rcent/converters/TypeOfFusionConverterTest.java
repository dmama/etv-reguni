package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfFusion;

public class TypeOfFusionConverterTest {

	private final TypeOfFusionConverter converter = new TypeOfFusionConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfFusion.class, converter);
	}
}
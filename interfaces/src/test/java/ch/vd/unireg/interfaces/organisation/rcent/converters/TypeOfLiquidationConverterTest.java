package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfLiquidation;

public class TypeOfLiquidationConverterTest {

	private final TypeOfLiquidationConverter converter = new TypeOfLiquidationConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfLiquidation.class, converter);
	}
}
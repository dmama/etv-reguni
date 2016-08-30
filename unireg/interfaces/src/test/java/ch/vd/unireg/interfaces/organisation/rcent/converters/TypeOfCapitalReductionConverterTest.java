package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfCapitalReduction;

public class TypeOfCapitalReductionConverterTest {

	private final TypeOfCapitalReductionConverter converter = new TypeOfCapitalReductionConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfCapitalReduction.class, converter);
	}
}
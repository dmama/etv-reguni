package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class TypeDeSiteConverterTest {

	private final TypeDeSiteConverter converter = new TypeDeSiteConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeDeSite.class, converter);
	}
}
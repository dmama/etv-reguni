package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfBusinessPublication;

public class TypeOfBusinessPublicationConverterTest {

	private final TypeOfBusinessPublicationConverter converter = new TypeOfBusinessPublicationConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfBusinessPublication.class, converter);
	}
}
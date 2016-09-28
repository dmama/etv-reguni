package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.unireg.interfaces.organisation.data.FormeLegale;

public class FormeLegaleConverterTest {

	private final FormeLegaleConverter converter = new FormeLegaleConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(FormeLegale.class, converter);
	}

}
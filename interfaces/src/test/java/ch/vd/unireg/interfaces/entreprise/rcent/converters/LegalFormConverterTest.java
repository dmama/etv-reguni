package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.LegalForm;

public class LegalFormConverterTest {

	private final LegalFormConverter converter = new LegalFormConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(LegalForm.class, converter);
	}

}
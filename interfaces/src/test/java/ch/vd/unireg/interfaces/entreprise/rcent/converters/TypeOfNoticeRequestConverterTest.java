package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfNoticeRequest;

public class TypeOfNoticeRequestConverterTest {

	private final TypeOfNoticeRequestConverter converter = new TypeOfNoticeRequestConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfNoticeRequest.class, converter);
	}

}
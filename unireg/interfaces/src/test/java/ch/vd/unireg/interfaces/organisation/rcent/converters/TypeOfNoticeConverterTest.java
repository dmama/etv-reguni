package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfNotice;

public class TypeOfNoticeConverterTest {

	private final TypeOfNoticeConverter converter = new TypeOfNoticeConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfNotice.class, converter);
	}
}
package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.NoticeRequestStatusCode;

public class StatutAnnonceConverterTest {

	private final NoticeRequestStatusCodeConverter converter = new NoticeRequestStatusCodeConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(NoticeRequestStatusCode.class, converter);
	}

}
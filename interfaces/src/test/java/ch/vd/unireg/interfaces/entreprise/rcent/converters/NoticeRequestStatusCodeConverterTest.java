package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.NoticeRequestStatusCode;

public class NoticeRequestStatusCodeConverterTest {

	private final NoticeRequestStatusCodeConverter converter = new NoticeRequestStatusCodeConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(NoticeRequestStatusCode.class, converter);
	}

}
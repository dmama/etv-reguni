package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.UidRegisterStatus;

public class UidRegisterStatusConverterTest {

	private final UidRegisterStatusConverter converter = new UidRegisterStatusConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(UidRegisterStatus.class, converter);
	}
}
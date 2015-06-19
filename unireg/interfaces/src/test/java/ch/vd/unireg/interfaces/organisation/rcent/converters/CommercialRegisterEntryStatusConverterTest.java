package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;

public class CommercialRegisterEntryStatusConverterTest {

	private final CommercialRegisterEntryStatusConverter converter = new CommercialRegisterEntryStatusConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(CommercialRegisterEntryStatus.class, converter);
	}
}
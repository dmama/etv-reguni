package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v1.CommercialRegisterStatus;

public class CommercialRegisterStatusConverterTest {
	private final CommercialRegisterStatusConverter converter = new CommercialRegisterStatusConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(CommercialRegisterStatus.class, converter);
	}

}
package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;

public class UidRegisterTypeOfOrganisationConverterTest {

	private final UidRegisterTypeOfOrganisationConverter converter = new UidRegisterTypeOfOrganisationConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(UidRegisterTypeOfOrganisation.class, converter);
	}
}
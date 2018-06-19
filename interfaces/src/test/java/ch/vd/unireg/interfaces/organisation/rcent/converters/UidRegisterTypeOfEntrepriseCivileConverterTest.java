package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.KindOfUidEntity;

public class UidRegisterTypeOfEntrepriseCivileConverterTest {

	private final UidRegisterTypeOfOrganisationConverter converter = new UidRegisterTypeOfOrganisationConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(KindOfUidEntity.class, converter);
	}
}
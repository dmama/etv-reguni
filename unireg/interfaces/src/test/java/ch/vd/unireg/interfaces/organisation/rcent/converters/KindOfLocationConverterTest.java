package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v1.KindOfLocation;

public class KindOfLocationConverterTest {

	private final KindOfLocationConverter converter = new KindOfLocationConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(KindOfLocation.class, converter);
	}
}
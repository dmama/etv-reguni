package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v1.Sex;

public class SexConverterTest {

	private final SexConverter converter = new SexConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(Sex.class, converter);
	}
}
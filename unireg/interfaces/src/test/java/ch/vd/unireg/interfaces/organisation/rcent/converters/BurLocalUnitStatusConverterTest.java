package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.BurLocalUnitStatus;

public class BurLocalUnitStatusConverterTest {
	private final BurLocalUnitStatusConverter converter = new BurLocalUnitStatusConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(BurLocalUnitStatus.class, converter);
	}

}
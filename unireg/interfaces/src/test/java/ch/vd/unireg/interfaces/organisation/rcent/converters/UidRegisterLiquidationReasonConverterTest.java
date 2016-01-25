package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v1.UidRegisterLiquidationReason;

public class UidRegisterLiquidationReasonConverterTest {

	private final UidRegisterLiquidationReasonConverter converter = new UidRegisterLiquidationReasonConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(UidRegisterLiquidationReason.class, converter);
	}
}
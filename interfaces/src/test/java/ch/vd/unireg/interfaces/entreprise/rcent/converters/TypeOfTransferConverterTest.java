package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfTransfer;

public class TypeOfTransferConverterTest {

	private final TypeOfTransferConverter converter = new TypeOfTransferConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeOfTransfer.class, converter);
	}
}
package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;

public class TypeAnnonceConverterTest {

	private final TypeAnnonceConverter converter = new TypeAnnonceConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeAnnonce.class, converter);
	}

}
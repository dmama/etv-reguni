package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.junit.Test;

import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;

public class TypeEtablissementCivilConverterTest {

	private final TypeEtablissementCivilConverter converter = new TypeEtablissementCivilConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(TypeEtablissementCivil.class, converter);
	}
}
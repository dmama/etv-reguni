package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BaseConverterTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * Vérifie qu'on détecte correctement le cas ou le convertisseur renvoie null alors
	 * qu'il devrait lancer une exception, lorsqu'une valeur non prise en charge est rencontrée.
	 */
	@Test
	public void testDetectFaultyConverter() {
		Converter<TestEnum, TestEnumConvertedBad> converter = new BadTestEnumConverter();

		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("La conversion de la valeur [VALUE3] a renvoyé une valeur nulle. Contrôler l'implémentation de convert()");

		EnumTestHelper.testAllValues(TestEnum.class, converter);
	}

	enum TestEnum {
		VALUE1, VALUE2, VALUE3
	}

	enum TestEnumConvertedBad {
		VALUE1, VALUE2
	}

	/*
	  Converter qui retourne un null plutôt que de lancer l'exception de rigueur lorsqu'il
	  ne trouve pas une valeur à convertir.
	  */
	public static class BadTestEnumConverter extends BaseEnumConverter<TestEnum, TestEnumConvertedBad> {

		@Override
		public TestEnumConvertedBad convert(@NotNull TestEnum value) {
			switch (value) {
			case VALUE1:
				return TestEnumConvertedBad.VALUE1;
			case VALUE2:
				return TestEnumConvertedBad.VALUE2;
			}
			return null;
		}
	}
}

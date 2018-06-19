package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EnumTestHelperTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testSuccessfullyTestAllConversions() {
		Function<TestEnum, TestEnumConverted> converter = new TestEnumConverter();
		EnumTestHelper.testAllValues(TestEnum.class, converter);
	}

	@Test
	public void testProperlyFailsWhenValueMissing() {
		Function<TestEnum, TestEnumConverted> converter = new TestEnumConverterBad();

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("La valeur [VALUE3] de l'énumération [TestEnum] n'est pas supportée.");

		EnumTestHelper.testAllValues(TestEnum.class, converter);
	}

	/**
	 * Vérifie qu'on détecte correctement le cas ou le convertisseur renvoie null alors
	 * qu'il devrait lancer une exception, lorsqu'une valeur non prise en charge est rencontrée.
	 */
	@Test
	public void testDetectFaultyConverter() {
		Function<TestEnum, TestEnumConvertedBad> converter = new BadTestEnumConverter();

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("La conversion de la valeur [VALUE3] a renvoyé une valeur nulle. Contrôler l'implémentation de convert()");

		EnumTestHelper.testAllValues(TestEnum.class, converter);
	}

	enum TestEnum {
		VALUE1, VALUE2, VALUE3
	}

	enum TestEnumConverted {
		VALUE1, VALUE2, VALUE3
	}

	enum TestEnumConvertedBad {
		VALUE1, VALUE2
	}

	public static class TestEnumConverter extends BaseEnumConverter<TestEnum, TestEnumConverted> {
		@Override
		@NotNull
		public TestEnumConverted convert(@NotNull TestEnum value) {
			switch (value) {
			case VALUE1:
				return TestEnumConverted.VALUE1;
			case VALUE2:
				return TestEnumConverted.VALUE2;
			case VALUE3:
				return TestEnumConverted.VALUE3;
			default:
				throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
			}
		}
	}

	public static class TestEnumConverterBad extends BaseEnumConverter<TestEnum, TestEnumConverted> {
		@Override
		@NotNull
		public TestEnumConverted convert(@NotNull TestEnum value) {
			switch (value) {
			case VALUE1:
				return TestEnumConverted.VALUE1;
			case VALUE2:
				return TestEnumConverted.VALUE2;
			default:
				throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
			}
		}
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

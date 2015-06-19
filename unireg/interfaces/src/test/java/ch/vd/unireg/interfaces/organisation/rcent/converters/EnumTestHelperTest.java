package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EnumTestHelperTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testSuccessfullyTestAllConversions() {
		Converter<TestEnum, TestEnumConverted> converter = new TestEnumConverter();
		EnumTestHelper.testAllValues(TestEnum.class, converter);
	}

	@Test
	public void testProperlyFailsWhenValueMissing() {
		Converter<TestEnum, TestEnumConverted> converter = new TestEnumConverterBad();

		thrown.expect(AssertionError.class);
		thrown.expectMessage("La valeur [VALUE3] de l'énumération [TestEnum] n'est pas supportée.");

		EnumTestHelper.testAllValues(TestEnum.class, converter);
	}

	enum TestEnum {
		VALUE1, VALUE2, VALUE3
	}

	enum TestEnumConverted {
		VALUE1, VALUE2, VALUE3
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
}

package ch.vd.uniregctb.webservices.tiers2;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.failNotEquals;

public abstract class EnumTest extends WithoutSpringTest {

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumLengthEquals(Class<E> expectedEnum, Class<A> actualEnum) {
		final E[] expectedValues = expectedEnum.getEnumConstants();
		final A[] actualValues = actualEnum.getEnumConstants();
		assertEnumLengthEquals(expectedValues, actualValues);
	}

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumLengthEquals(E[] expectedValues, A[] actualValues) {
		assertEquals(expectedValues.length, actualValues.length);
	}

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumConstantsEqual(Class<E> expectedEnum, Class<A> actualEnum) {
		final E[] expectedValues = expectedEnum.getEnumConstants();
		final A[] actualValues = actualEnum.getEnumConstants();
		assertEnumConstantsEqual(expectedValues, actualValues);
	}

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumConstantsEqual(E[] expectedValues, A[] actualValues) {
		for (int i = 0; i < expectedValues.length; ++i) {
			final E expected = expectedValues[i];
			final A actual = actualValues[i];
			final String message = "La constante " + expected.name() + " à la position " + i
					+ " et la constante " + actual.name() + " devraient avoir le même nom";
			assertEquals(message, expected.name(), actual.name());
		}
	}

	public static void assertContains(String containee, String container) {
		if (container == null || containee == null || !container.contains(containee)) {
			failNotEquals("", containee, container);
		}
	}

}

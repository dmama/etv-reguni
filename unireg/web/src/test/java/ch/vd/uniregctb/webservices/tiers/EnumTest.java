package ch.vd.uniregctb.webservices.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.failNotEquals;
import ch.vd.uniregctb.common.WithoutSpringTest;

public abstract class EnumTest extends WithoutSpringTest {

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumLengthEquals(Class<E> expectedEnum, Class<A> actualEnum) {
		final E[] expectedValues = expectedEnum.getEnumConstants();
		final A[] actualValues = actualEnum.getEnumConstants();

		final String message = "L'enum " + expectedEnum.getName() + " et l'enum " + actualEnum.getName()
				+ " devraient possèder le même nombre d'éléments";
		assertEquals(message, expectedValues.length, actualValues.length);
	}

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumConstantsEqual(Class<E> expectedEnum, Class<A> actualEnum) {
		final E[] expectedValues = expectedEnum.getEnumConstants();
		final A[] actualValues = actualEnum.getEnumConstants();

		for (int i = 0; i < expectedValues.length; ++i) {
			E expected = expectedValues[i];
			A actual = actualValues[i];
			final String message = "La constante " + expected.name() + " à la position " + i + " de l'enum " + expectedEnum.getName()
					+ " et la constante " + actual.name() + " à la même position de l'enum " + actualEnum.getName()
					+ " dans devraient être la même";
			assertEquals(message, expected.name(), actual.name());
		}
	}

	public static void assertContains(String containee, String container) {

		if (container == null || containee == null || !container.contains(containee)) {
			failNotEquals("", containee, container);
		}
	}

}

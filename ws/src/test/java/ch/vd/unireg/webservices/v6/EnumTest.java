package ch.vd.uniregctb.webservices.v6;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class EnumTest extends WithoutSpringTest {

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumLengthEquals(Class<E> expectedEnum, Class<A> actualEnum) {
		final E[] expectedValues = expectedEnum.getEnumConstants();
		final A[] actualValues = actualEnum.getEnumConstants();

		final String message = "L'enum " + expectedEnum.getName() + " et l'enum " + actualEnum.getName()
				+ " devraient possèder le même nombre d'éléments";
		assertEquals(message, expectedValues.length, actualValues.length);
	}

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumLengthEquals(E[] expectedValues, A[] actualValues) {
		assertEquals(expectedValues.length, actualValues.length);
	}

	public static <E extends Enum<E>, A extends Enum<A>> void assertEnumConstantsEqual(Class<E> expectedEnum, Class<A> actualEnum) {
		assertContainsEnum(expectedEnum, actualEnum);
		assertContainsEnum(actualEnum, expectedEnum);
	}

	private static <E extends Enum<E>, A extends Enum<A>> void assertContainsEnum(Class<E> container, Class<A> containee) {
		final E[] containerValues = container.getEnumConstants();
		final A[] containeeValues = containee.getEnumConstants();

		for (A a : containeeValues) {
			E found = null;
			for (E e : containerValues) {
				if (e.name().equals(a.name())) {
					found = e;
					break;
				}
			}
			final String message = "La constant [" + a.name() + "] n'existe pas dans l'enum [" + container.getSimpleName() + ']';
			assertNotNull(message, found);
		}
	}
}

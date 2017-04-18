package ch.vd.unireg.interfaces.organisation.rcent.converters;

import java.util.function.Function;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import ch.vd.registre.base.utils.Assert;

public class EnumTestHelper {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	public static <T extends Enum<T>, R extends Enum<R>> void testAllValues(Class<T> sourceEnum, Function<T, R> converter) {
		for (T value : sourceEnum.getEnumConstants()) {
			R converted = converter.apply(value);
			if (converted == null) {
				Assert.fail("La conversion de la valeur [" + value.toString() + "] " +
						            "a renvoyé une valeur nulle. Contrôler l'implémentation de convert().");
			}
		}
	}
}

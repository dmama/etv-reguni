package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class EnumTestHelper {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	public static <T extends Enum, R> void testAllValues(Class<T> enumClass, Converter<T, R> conv) {
		for (T value : enumClass.getEnumConstants()) {
			try {
				conv.apply(value);
			} catch (IllegalArgumentException ex) {
				Assert.fail(ex.getMessage());
			}
		}
	}
}

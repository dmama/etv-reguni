package ch.vd.uniregctb.common;

import java.lang.reflect.Field;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.ObjectUtils;

/**
 * Listener qui s'assure qu'il n'y a qu'un seul Context Spring chargé en même temps en mémoire. Pour cela, il doit être enregistré comme le premier de tous les listeners.
 */
public class SingleContextTestExecutionListener implements TestExecutionListener {

	private static TestContext previousContext;
	private static String previousKey;

	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {

		final String key = getKey(testContext);
		if (previousKey != null && !key.equals(previousKey)) {
			// si le context précédent est différent du (futur) context, on le ferme avant de continuer.
			previousContext.markApplicationContextDirty();
		}

		previousKey = key;
		previousContext = testContext;
	}

	private static String getKey(TestContext testContext) {
		final Object[] locations;
		try {
			final Field f = TestContext.class.getDeclaredField("locations");
			f.setAccessible(true);
			locations = (Object[]) f.get(testContext);
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return ObjectUtils.nullSafeToString(locations);
	}

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
	}

	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
	}
}

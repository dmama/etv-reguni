package ch.vd.uniregctb.common;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.util.Log4jConfigurer;

/**
 * Cette classe permet une compatibilité des onSetup/setUp entre les TestCase avec un ApplicationContext et sans
 *
 * @author jec
 *
 */
@RunWith(JUnit4ClassRunner.class)
public abstract class WithoutSpringTest {

	public WithoutSpringTest() {
		try {
			Log4jConfigurer.initLogging("classpath:ut/log4j.xml");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public WithoutSpringTest(String log4j) {
		try {
			Log4jConfigurer.initLogging(log4j);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A surcharger par les sous-classes
	 */
	@Before
	public void onSetUp() throws Exception {
	}

	/**
	 * A surcharger par les sous-classes
	 */
	@After
	public void onTearDown() throws Exception {
	}

	public static void assertEmpty(Collection<?> coll) {
		assertTrue(coll == null || coll.isEmpty());
	}

	public static void assertEmpty(String message, Collection<?> coll) {
		assertTrue(message, coll == null || coll.isEmpty());
	}

	public static void assertContains(String containee, String container, String msg) {
		if (container == null || containee == null || !container.contains(containee)) {
			Assert.fail(msg);
		}
	}

	public static void assertContains(String containee, String container) {
		assertContains(containee, container, "'" + container + "' doesn't contain '" + containee + "'");
	}

	public static void assertNotContains(String containee, String container) {
		assertNotContains(containee, container, "'" + container + "' contains '" + containee + "'");
	}

	public static void assertNotContains(String containee, String container, String msg) {
		if (container == null || containee == null || container.contains(containee)) {
			Assert.fail(msg);
		}
	}

	public static void assertContainsNoCase(String containee, String container) {
		container = (container == null ? null : container.toLowerCase());
		containee = (containee == null ? null : containee.toLowerCase());
		assertContains(containee, container);
	}
}

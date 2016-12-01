package ch.vd.uniregctb.common;

import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertTrue;

/**
 * Cette classe permet une compatibilité des onSetup/setUp entre les TestCase avec un ApplicationContext et sans
 *
 * @author jec
 *
 */
@RunWith(UniregJUnit4Runner.class)
public abstract class WithoutSpringTest {

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
		assertContains(containee, container, '\'' + container + "' doesn't contain '" + containee + '\'');
	}

	public static void assertNotContains(String containee, String container) {
		assertNotContains(containee, container, '\'' + container + "' contains '" + containee + '\'');
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

    public static RegDate date(int year, int month, int day) {
        return RegDate.get(year, month, day);
    }

    public static RegDate date(int year, int month) {
        return RegDate.get(year, month);
    }

    public static RegDate date(int year) {
        return RegDate.get(year);
    }
}

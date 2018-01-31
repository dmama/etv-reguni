package ch.vd.uniregctb.common;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TokenSetFactoryBeanTest extends WithoutSpringTest {

	private static <T> void check(T[] expected, Set<T> found) throws Exception {
		assertNotNull(found);
		assertEquals(expected.length, found.size());
		for (T exp : expected) {
			assertTrue("Expected: " + exp, found.contains(exp));
		}
	}

	private static Set<String> getStringSet(String toParse) throws Exception {
		final TokenSetFactoryBean<String> parser = new TokenSetFactoryBean.StringSet();
		parser.setElements(toParse);
		parser.afterPropertiesSet();
		return parser.getObject();
	}

	private static <T extends Enum<T>> Set<T> getEnumSet(Class<T> clazz, String toParse) throws Exception {
		final TokenSetFactoryBean<T> parser = new TokenSetFactoryBean.EnumSet<>(clazz);
		parser.setElements(toParse);
		parser.afterPropertiesSet();
		return parser.getObject();
	}

	@Test
	public void testParseString() throws Exception {

		{
			final Set<String> set = getStringSet(null);
			assertNotNull(set);
			assertEquals(0, set.size());
		}
		{
			final Set<String> set = getStringSet(StringUtils.EMPTY);
			assertNotNull(set);
			assertEquals(0, set.size());
		}
		{
			final Set<String> set = getStringSet("  , ;;,");
			assertNotNull(set);
			assertEquals(0, set.size());
		}

		check(new String[] {"TOTO"}, getStringSet("TOTO"));
		check(new String[] {"TOTO"}, getStringSet(" TOTO,,"));
		check(new String[] {"TOTO"}, getStringSet(" ,TOTO"));
		check(new String[] {"TOTO"}, getStringSet(" ;TOTO, , "));

		check(new String[] {"TOTO", "TATA"}, getStringSet("TOTO,TATA"));
		check(new String[] {"TOTO", "TATA"}, getStringSet("TOTO;TATA"));
		check(new String[] {"TOTO", "TATA"}, getStringSet(" ;TOTO, , TATA, "));
		check(new String[] {"TOTO", "TATA"}, getStringSet(" ;TOTO , , TATA, "));
	}

	public enum PourTest {
		ONE,
		TWO,
		THREE
	}

	@Test
	public void testParseEnum() throws Exception {

		{
			final Set<PourTest> set = getEnumSet(PourTest.class, null);
			assertNotNull(set);
			assertEquals(0, set.size());
		}
		{
			final Set<PourTest> set = getEnumSet(PourTest.class, StringUtils.EMPTY);
			assertNotNull(set);
			assertEquals(0, set.size());
		}
		{
			final Set<PourTest> set = getEnumSet(PourTest.class, "  , ;;,");
			assertNotNull(set);
			assertEquals(0, set.size());
		}

		check(new PourTest[] {PourTest.ONE}, getEnumSet(PourTest.class, "ONE"));
		check(new PourTest[] {PourTest.ONE}, getEnumSet(PourTest.class, " ONE,,"));
		check(new PourTest[] {PourTest.ONE}, getEnumSet(PourTest.class, " ,ONE"));
		check(new PourTest[] {PourTest.ONE}, getEnumSet(PourTest.class, " ;ONE, , "));

		check(new PourTest[] {PourTest.ONE, PourTest.THREE}, getEnumSet(PourTest.class, "THREE, ONE"));
		check(new PourTest[] {PourTest.ONE, PourTest.THREE}, getEnumSet(PourTest.class, "ONE,THREE"));
		check(new PourTest[] {PourTest.ONE, PourTest.THREE}, getEnumSet(PourTest.class, "THREE;ONE"));
		check(new PourTest[] {PourTest.ONE, PourTest.THREE}, getEnumSet(PourTest.class, ",, ; THREE ; ONE;"));

		try {
			getEnumSet(PourTest.class, "FOUR");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("No enum constant " + PourTest.class.getName().replaceAll("\\$", ".") + ".FOUR", e.getMessage());
		}
	}
}

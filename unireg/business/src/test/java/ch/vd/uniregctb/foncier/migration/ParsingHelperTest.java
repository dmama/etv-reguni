package ch.vd.uniregctb.foncier.migration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParsingHelperTest {

	@Test
	public void testParsedixmillePercent() throws Exception {
		assertEquals(10000, ParsingHelper.parsePourdixmille("100"));
		assertEquals(9850, ParsingHelper.parsePourdixmille("98.5"));
		assertEquals(150, ParsingHelper.parsePourdixmille("1.5"));
		assertEquals(1220, ParsingHelper.parsePourdixmille("12.2"));
		assertEquals(0, ParsingHelper.parsePourdixmille("0"));
	}
}

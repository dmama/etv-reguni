package ch.vd.uniregctb.common;

import org.junit.Test;

import ch.vd.uniregctb.type.TexteCasePostale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CasePostaleTest {

	/**
	 * Exemples de case postales vus en production :
	 * <pre>
	 * - "Case postale"
	 * - "Case postale 121"
	 * - "Cases postales 12/345"
	 * - "Postfach"
	 * - "Postfach 31"
	 * - "Case Postale 1245"
	 * - "CP 711"
	 * - "Casella postale 32"
	 * </pre>
	 */
	@Test
	public void testParseCasePostale() {
		assertNull(CasePostale.parse(null));
		assertNull(CasePostale.parse(""));
		assertNull(CasePostale.parse("Hase postale"));
		assertNull(CasePostale.parse("Case Vostale"));

		assertEquals(new CasePostale(TexteCasePostale.CASE_POSTALE, null), CasePostale.parse("Case postale"));
		assertEquals(new CasePostale(TexteCasePostale.CASE_POSTALE, null), CasePostale.parse("Case postale "));
		assertEquals(new CasePostale(TexteCasePostale.CASE_POSTALE, 121), CasePostale.parse("Case postale 121"));
		assertEquals(new CasePostale(TexteCasePostale.CASE_POSTALE, 12), CasePostale.parse("Cases postales 12/345"));
		assertEquals(new CasePostale(TexteCasePostale.CASE_POSTALE, 1245), CasePostale.parse("Case Postale 1245"));
		assertEquals(new CasePostale(TexteCasePostale.CASE_POSTALE, 711), CasePostale.parse("CP 711"));
	}

	@Test
	public void testParseBoitePostale() {
		assertNull(CasePostale.parse(null));
		assertNull(CasePostale.parse(""));
		assertNull(CasePostale.parse("Hoite postale"));
		assertNull(CasePostale.parse("Boite Vostale"));

		assertEquals(new CasePostale(TexteCasePostale.BOITE_POSTALE, null), CasePostale.parse("Boite postale"));
		assertEquals(new CasePostale(TexteCasePostale.BOITE_POSTALE, null), CasePostale.parse("Boîte postale "));
		assertEquals(new CasePostale(TexteCasePostale.BOITE_POSTALE, 121), CasePostale.parse("Boite postale 121"));
		assertEquals(new CasePostale(TexteCasePostale.BOITE_POSTALE, 12), CasePostale.parse("Boites postales 12/345"));
		assertEquals(new CasePostale(TexteCasePostale.BOITE_POSTALE, 1245), CasePostale.parse("Boîte Postale 1245"));
	}

	@Test
	public void testParsePostfach() {
		assertNull(CasePostale.parse(null));
		assertNull(CasePostale.parse(""));
		assertNull(CasePostale.parse("Hornbach"));
		assertNull(CasePostale.parse("Fostpach"));
		assertNull(CasePostale.parse("Prokofiev"));

		assertEquals(new CasePostale(TexteCasePostale.POSTFACH, null), CasePostale.parse("Postfach"));
		assertEquals(new CasePostale(TexteCasePostale.POSTFACH, null), CasePostale.parse("Postfach "));
		assertEquals(new CasePostale(TexteCasePostale.POSTFACH, 31), CasePostale.parse("Postfach 31"));

	}

	@Test
	public void testParseCasellaPostale() {
		assertNull(CasePostale.parse(null));
		assertNull(CasePostale.parse(""));
		assertNull(CasePostale.parse("Casetta Postale"));
		assertNull(CasePostale.parse("Una biretta"));
		assertNull(CasePostale.parse("Casella"));

		assertEquals(new CasePostale(TexteCasePostale.CASELLA_POSTALE, null), CasePostale.parse("Casella Postale"));
		assertEquals(new CasePostale(TexteCasePostale.CASELLA_POSTALE, null), CasePostale.parse("Casella Postale "));
		assertEquals(new CasePostale(TexteCasePostale.CASELLA_POSTALE, 121), CasePostale.parse("Casella Postale 121"));
		assertEquals(new CasePostale(TexteCasePostale.CASELLA_POSTALE, 12), CasePostale.parse("Casella Postale 12/345"));
		assertEquals(new CasePostale(TexteCasePostale.CASELLA_POSTALE, 1245), CasePostale.parse("casella postale 1245"));

	}

	@Test
	public void testParsePOBox() {
		assertNull(CasePostale.parse(null));
		assertNull(CasePostale.parse(""));
		assertNull(CasePostale.parse("PO Hox"));
		assertNull(CasePostale.parse("PO"));
		assertNull(CasePostale.parse("Bobox"));

		assertEquals(new CasePostale(TexteCasePostale.PO_BOX, null), CasePostale.parse("PO BOX"));
		assertEquals(new CasePostale(TexteCasePostale.PO_BOX, null), CasePostale.parse("PO Box "));
		assertEquals(new CasePostale(TexteCasePostale.PO_BOX, 121), CasePostale.parse("PO Box 121"));
		assertEquals(new CasePostale(TexteCasePostale.PO_BOX, 12), CasePostale.parse("PO Box 12/345"));
		assertEquals(new CasePostale(TexteCasePostale.PO_BOX, 1245), CasePostale.parse("po Box 1245"));
	}
}

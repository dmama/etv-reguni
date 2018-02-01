package ch.vd.unireg.type;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

public class TexteCasePostaleTest extends WithoutSpringTest {

	@Test
	public void testParse() throws Exception {
		Assert.assertEquals(TexteCasePostale.BOITE_POSTALE, TexteCasePostale.parse("bOiTe_poStAle"));
		Assert.assertEquals(TexteCasePostale.CASE_POSTALE, TexteCasePostale.parse("cASe_poStAle"));
		Assert.assertEquals(TexteCasePostale.POSTFACH, TexteCasePostale.parse("pOstFAcH"));
		Assert.assertEquals(TexteCasePostale.PO_BOX, TexteCasePostale.parse("po_bOx"));
		Assert.assertEquals(TexteCasePostale.CASELLA_POSTALE, TexteCasePostale.parse("cASeLla_poStAle"));

		Assert.assertEquals(TexteCasePostale.CASE_POSTALE, TexteCasePostale.parse(null));
		Assert.assertEquals(TexteCasePostale.CASE_POSTALE, TexteCasePostale.parse("   "));
		Assert.assertEquals(TexteCasePostale.BOITE_POSTALE, TexteCasePostale.parse("boîte pOstale"));
		Assert.assertEquals(TexteCasePostale.PO_BOX, TexteCasePostale.parse("pO BoX"));
	}
}

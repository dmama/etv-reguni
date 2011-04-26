package ch.vd.uniregctb.tiers;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;

/**
 * Test unitaire pour le FormatNumeroHelper
 */
public class FormatNumeroHelperTest extends WithoutSpringTest {

	// private static final Logger LOGGER = Logger.getLogger(FormatNumeroHelperTest.class);

	@Test
	public void testFormatNumAVS() throws Exception {

		String numAVS = "1245789012345";

		assertEquals("124.5789.0123.45", FormatNumeroHelper.formatNumAVS(numAVS));
	}

	@Test
	public void testFormatBadNumAVS() throws Exception {

		String numAVS = "abc";
		assertEquals("", FormatNumeroHelper.formatNumAVS(numAVS));
	}

	@Test
	public void testFormatAncienNumAVS() throws Exception {

		// Numero long sans point
		{
			String numAVS = "15489652357";
			assertEquals("154.89.652.357", FormatNumeroHelper.formatAncienNumAVS(numAVS));
		}
		// Numero long avec point
		{
			String numAVS = "154.89.652.357";
			assertEquals("154.89.652.357", FormatNumeroHelper.formatAncienNumAVS(numAVS));
		}
		// Numero court sans point
		{
			String numAVS = "15489652";
			assertEquals("154.89.652", FormatNumeroHelper.formatAncienNumAVS(numAVS));
		}
		// Numero court avec point
		{
			String numAVS = "154.89.652";
			assertEquals("154.89.652", FormatNumeroHelper.formatAncienNumAVS(numAVS));
		}
		// Numero fausse longueur
		{
			String numAVS = "154896527";
			assertEquals("", FormatNumeroHelper.formatAncienNumAVS(numAVS));
		}
		// Poubelle
		{
			String numAVS = "abc";
			assertEquals("", FormatNumeroHelper.formatAncienNumAVS(numAVS));
		}
	}

	@Test
	public void testFormatDate() throws Exception {

		String date = "19700123";

		assertEquals("23.01.1970", FormatNumeroHelper.formatDate(date));
	}

	@Test
	public void testFormatNumeroCTB() throws Exception {

		assertEquals("123.456.78", FormatNumeroHelper.numeroCTBToDisplay(new Long(12345678)));
	}

	@Test
	public void testFormatNumeroCTBBis() throws Exception {

		assertEquals("", FormatNumeroHelper.numeroCTBToDisplay(null));
	}

	@Test
	public void testFormatNumeroCTBThird() throws Exception {

		assertEquals("12.34", FormatNumeroHelper.numeroCTBToDisplay(new Long(1234)));
	}

	@Test
	public void testNumeroIndividuToDisplay() throws Exception {

		assertEquals("125141", FormatNumeroHelper.numeroIndividuToDisplay(new Long(125141)));
	}

	@Test
	public void testNumeroIndividuToDisplayBis() throws Exception {

		assertEquals("123", FormatNumeroHelper.numeroIndividuToDisplay(new Long(123)));
	}

	@Test
	public void testNumeroIndividuToDisplayNull() throws Exception {

		assertEquals("", FormatNumeroHelper.numeroIndividuToDisplay(null));
	}

	@Test
	public void testExtractNoReference() throws Exception {
		String noReferenceAttendu = "21 00000 00003 13947 14300 09017";
		String ligneCodage = "0100003949753>210000000003139471430009017+ 01010391391>";
		String noReference = FormatNumeroHelper.extractNoReference(ligneCodage);
		assertEquals(noReferenceAttendu, noReference);

		noReferenceAttendu = "21000 00003 13947 14300 09017";
		ligneCodage = "0100003949753>2100000003139471430009017+ 01010391391>";
		noReference = FormatNumeroHelper.extractNoReference(ligneCodage);
		assertEquals(noReferenceAttendu, noReference);

		noReferenceAttendu = "9017";
		ligneCodage = "0100003949753>9017+ 01010391391>";
		noReference = FormatNumeroHelper.extractNoReference(ligneCodage);
		assertEquals(noReferenceAttendu, noReference);

		noReferenceAttendu = null;
		ligneCodage = "0100003949753210000000003139471430009017+ 01010391391>";
		noReference = FormatNumeroHelper.extractNoReference(ligneCodage);
		assertEquals(noReferenceAttendu, noReference);

		noReferenceAttendu = null;
		ligneCodage = "0100003949753>210000000003139471430009017 01010391391>";
		noReference = FormatNumeroHelper.extractNoReference(ligneCodage);
		assertEquals(noReferenceAttendu, noReference);
	}

}

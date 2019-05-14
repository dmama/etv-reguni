package ch.vd.unireg.tiers;

import org.junit.Test;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;

/**
 * Test unitaire pour le FormatNumeroHelper
 */
public class FormatNumeroHelperTest extends WithoutSpringTest {

	// private static final Logger LOGGER = LoggerFactory.getLogger(FormatNumeroHelperTest.class);

	@Test
	public void testFormatNumAVS() throws Exception {
		assertEquals("1245789012345", FormatNumeroHelper.formatNumAVS("12.-45789. 012345"));        // pas de formattage car cela ne commence pas par 756
		assertEquals("", FormatNumeroHelper.formatNumAVS("1245789012345AA"));                       // à cause des lettres
		assertEquals("756.5789.0123.45", FormatNumeroHelper.formatNumAVS("75.--657  89012345"));    // formattage ok
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
		assertEquals("123.456.78", FormatNumeroHelper.numeroCTBToDisplay(12345678L));
	}

	@Test
	public void testFormatNumeroCTBBis() throws Exception {
		assertEquals("", FormatNumeroHelper.numeroCTBToDisplay(null));
	}

	@Test
	public void testFormatNumeroCTBThird() throws Exception {
		assertEquals("12.34", FormatNumeroHelper.numeroCTBToDisplay(1234L));
	}

	@Test
	public void testNumeroIndividuToDisplay() throws Exception {
		assertEquals("125141", FormatNumeroHelper.numeroIndividuToDisplay(125141L));
	}

	@Test
	public void testNumeroIndividuToDisplayBis() throws Exception {
		assertEquals("123", FormatNumeroHelper.numeroIndividuToDisplay(123L));
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

	@Test
	public void testFormatNumIDE() throws Exception {
		assertEquals("", FormatNumeroHelper.formatNumIDE(null));
		assertEquals("CHE-123.456.789", FormatNumeroHelper.formatNumIDE("CHE123456789"));
		assertEquals("CHE-123.456.789", FormatNumeroHelper.formatNumIDE("CHE-123..45.6789"));
		assertEquals("CHE-789.456.123", FormatNumeroHelper.formatNumIDE("CHE/789-456-123"));
	}

	@Test
	public void testFormatCantonalId() throws Exception {
		assertEquals("", FormatNumeroHelper.formatCantonalId(null));
		assertEquals("blabla", FormatNumeroHelper.formatCantonalId("blabla"));
		assertEquals("100-565-926", FormatNumeroHelper.formatCantonalId("100565926"));
		assertEquals("100-565-926", FormatNumeroHelper.formatCantonalId("100-565-926"));
	}
}

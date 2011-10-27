package ch.vd.uniregctb.utils;

import junit.framework.TestCase;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.Sexe;
public class AVSValidatorTest extends TestCase {


	public AVSValidatorTest(String name) {
		super(name);
	}


	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}


	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test NumAVS et Ancien numero AVS
	 *
	 * @throws EAN13CheckDigitException
	 */

	@Test
	public void testValideNouveauAVS() throws EAN13CheckDigitException {

		AVSValidator validator = new AVSValidator();
		String validNouveauAVS = "7563047500962";
		String validNouveauAVS1 = "756.6132.0550.88";
		String validNouveauAVS2 = "756.9613.1278.61";

		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		validator.setLength(13);
		assertEquals("Nouveau numero AVS valide", true, validator.isValidNouveauNumAVS(validNouveauAVS));
		String code = validator.getCheckDigit().calculate("756304750096");
		assertEquals("2", code);

		assertEquals("Nouveau numero AVS valide", true, validator.isValidNouveauNumAVS(validNouveauAVS1));
		String code1 = validator.getCheckDigit().calculate("756613205508");
		assertEquals("8", code1);

		assertEquals("Nouveau numero AVS valide", true, validator.isValidNouveauNumAVS(validNouveauAVS2));
		String code2 = validator.getCheckDigit().calculate("756961312786");
		assertEquals("1", code2);

	}

	@Test
	public void testValideNouveauAVSWithDash() throws EAN13CheckDigitException {

		AVSValidator validator = new AVSValidator();
		String validNouveauAVS = "756.6132.0550.88";
		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		validator.setLength(13);
		assertEquals("Nouveau numero AVS valide", true, validator.isValidNouveauNumAVS(validNouveauAVS));
		String codeControle = validator.getCheckDigit().calculate("756613205508");
		assertEquals("8", codeControle);

	}

	@Test
	public void testValideNouveauAVSWithSpace() throws EAN13CheckDigitException {

		AVSValidator validator = new AVSValidator();
		String validNouveauAVS = "756 6132 0550 88";
		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		validator.setLength(13);
		assertEquals("Nouveau numero AVS valide", true, validator.isValidNouveauNumAVS(validNouveauAVS));
		String codeControle = validator.getCheckDigit().calculate("756613205508");
		assertEquals("8", codeControle);

	}

	@Test
	public void testInValideNouveauAVS() throws EAN13CheckDigitException {

		AVSValidator validator = new AVSValidator();
		String invalidNouveauAVS = "7563047500966";
		String invalidNouveauAVS1 = "3523047500966";

		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		validator.setLength(13);
		assertEquals("Nouveau numero AVS invalide", false, validator.isValidNouveauNumAVS(invalidNouveauAVS));
		assertEquals("Nouveau numero AVS invalide", false, validator.isValidNouveauNumAVS(invalidNouveauAVS1));

	}

	@Test
	public void testInValideNouveauAVSWithCaractereSpeciaux() throws EAN13CheckDigitException {

		AVSValidator validator = new AVSValidator();
		String invalidNouveauAVS = "7563*04*7500966";

		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		validator.setLength(13);
		assertEquals("Nouveau numero AVS invalide", false, validator.isValidNouveauNumAVS(invalidNouveauAVS));

	}

	@Test
	public void testValideAncienNumAVS() throws Exception {

		AVSValidator validator = new AVSValidator();
		String validAncienNumAVS = "123.45.678.113";
		String validAncienNumAVS1 = "342.10.507.118"; // 07.01.1910

		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		assertEquals("ancien num AVS Valide", true, validator.isValidAncienNumAVS(validAncienNumAVS, date("04.1945"),Sexe.FEMININ));
		assertEquals("ancien num AVS valide", true, validator.isValidAncienNumAVS(validAncienNumAVS1, date("07.01.1910"),Sexe.FEMININ));

	}

	@Test
	public void testInValideAncienNumAVSWith8Digits() throws Exception {

		AVSValidator validator = new AVSValidator();
		String validAncienNumAVS = "123.45.678";
		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		assertFalse("ancien num AVS Valide", validator.isValidAncienNumAVS(validAncienNumAVS, date("04.1945"),Sexe.FEMININ));

	}

	@Test
	public void testValideAncienNumAVSWith11Digits() throws Exception {

		AVSValidator validator = new AVSValidator();
		String validAncienNumAVS = "123.45.678.000";
		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		assertTrue("ancien num AVS Valide", validator.isValidAncienNumAVS(validAncienNumAVS, date("04.1945"),Sexe.FEMININ));

	}

	@Test
	public void testInValideAncienNumAVS() throws Exception {

		AVSValidator validator = new AVSValidator();
		String invalidAncienNumAVS = "123.45.678.118";

		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		assertEquals("ancien num AVS Invalide", false, validator.isValidAncienNumAVS(invalidAncienNumAVS, date("04.1945"),Sexe.FEMININ));
	}

	@Test
	public void testInValideAncienNumAVSWithBadDateNaissance() throws Exception {

		AVSValidator validator = new AVSValidator();
		String invalidAncienNumAVS = "123.45.678.118";

		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		assertEquals("ancien num AVS Invalide", false, validator.isValidAncienNumAVS(invalidAncienNumAVS, date("04.1944"),Sexe.FEMININ));
	}
	@Test
	public void testInValideAncienNumAVSWithBadSexe() throws Exception {

		AVSValidator validator = new AVSValidator();
		String ancienNumAVS = "498.46.537.154";

		validator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
		assertEquals("ancien num AVS Invalide", false, validator.isValidAncienNumAVS(ancienNumAVS, date("02.1946"),Sexe.MASCULIN));
		assertEquals("ancien num AVS valide", true, validator.isValidAncienNumAVS(ancienNumAVS, date("02.1946"),Sexe.FEMININ));
	}

	private static RegDate date(String dateAsString) throws Exception {
		return RegDateHelper.displayStringToRegDate(dateAsString, true);
	}
}

package ch.vd.uniregctb.utils;

import org.junit.Test;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AVSValidatorTest {

	@Test
	public void testValideNouveauAVS() {

		String validNouveauAVS = "7563047500962";
		String validNouveauAVS1 = "756.6132.0550.88";
		String validNouveauAVS2 = "756.9613.1278.61";

		assertEquals("Nouveau numero AVS valide", true, AvsHelper.isValidNouveauNumAVS(validNouveauAVS));
		assertEquals("Nouveau numero AVS valide", true, AvsHelper.isValidNouveauNumAVS(validNouveauAVS1));
		assertEquals("Nouveau numero AVS valide", true, AvsHelper.isValidNouveauNumAVS(validNouveauAVS2));
	}

	@Test
	public void testValideNouveauAVSWithDash() {

		String validNouveauAVS = "756.6132.0550.88";
		assertEquals("Nouveau numero AVS valide", true, AvsHelper.isValidNouveauNumAVS(validNouveauAVS));
	}

	@Test
	public void testValideNouveauAVSWithSpace() {

		String validNouveauAVS = "756 6132 0550 88";
		assertEquals("Nouveau numero AVS valide", true, AvsHelper.isValidNouveauNumAVS(validNouveauAVS));
	}

	@Test
	public void testInValideNouveauAVS() {

		String invalidNouveauAVS = "7563047500966";
		String invalidNouveauAVS1 = "3523047500966";

		assertEquals("Nouveau numero AVS invalide", false, AvsHelper.isValidNouveauNumAVS(invalidNouveauAVS));
		assertEquals("Nouveau numero AVS invalide", false, AvsHelper.isValidNouveauNumAVS(invalidNouveauAVS1));
	}

	@Test
	public void testInValideNouveauAVSWithCaractereSpeciaux() {

		String invalidNouveauAVS = "7563*04*7500966";
		assertEquals("Nouveau numero AVS invalide", false, AvsHelper.isValidNouveauNumAVS(invalidNouveauAVS));
	}

	@Test
	public void testValideAncienNumAVS() throws Exception {

		String validAncienNumAVS = "123.45.678.113";
		String validAncienNumAVS1 = "342.10.507.118"; // 07.01.1910

		assertEquals("ancien num AVS Valide", true, AvsHelper.isValidAncienNumAVS(validAncienNumAVS, date("04.1945"),false));
		assertEquals("ancien num AVS valide", true, AvsHelper.isValidAncienNumAVS(validAncienNumAVS1, date("07.01.1910"),false));
	}

	@Test
	public void testInValideAncienNumAVSWith8Digits() throws Exception {

		String validAncienNumAVS = "123.45.678";
		assertFalse("ancien num AVS Valide", AvsHelper.isValidAncienNumAVS(validAncienNumAVS, date("04.1945"),false));

	}

	@Test
	public void testValideAncienNumAVSWith11Digits() throws Exception {

		String validAncienNumAVS = "123.45.678.000";
		assertTrue("ancien num AVS Valide", AvsHelper.isValidAncienNumAVS(validAncienNumAVS, date("04.1945"),false));

	}

	@Test
	public void testInValideAncienNumAVS() throws Exception {

		String invalidAncienNumAVS = "123.45.678.118";

		assertEquals("ancien num AVS Invalide", false, AvsHelper.isValidAncienNumAVS(invalidAncienNumAVS, date("04.1945"),false));
	}

	@Test
	public void testInValideAncienNumAVSWithBadDateNaissance() throws Exception {

		String invalidAncienNumAVS = "123.45.678.118";

		assertEquals("ancien num AVS Invalide", false, AvsHelper.isValidAncienNumAVS(invalidAncienNumAVS, date("04.1944"),false));
	}

	@Test
	public void testInValideAncienNumAVSWithBadSexe() throws Exception {

		String ancienNumAVS = "498.46.537.154";

		assertEquals("ancien num AVS Invalide", false, AvsHelper.isValidAncienNumAVS(ancienNumAVS, date("02.1946"),true));
		assertEquals("ancien num AVS valide", true, AvsHelper.isValidAncienNumAVS(ancienNumAVS, date("02.1946"),false));
	}

	private static RegDate date(String dateAsString) throws Exception {
		return RegDateHelper.displayStringToRegDate(dateAsString, true);
	}
}

package ch.vd.uniregctb.declaration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DeclarationImpotOrdinaireTest {

	// [SIFISC-1368] Vérifie que les codes de contrôles générés correspondent bien à la spécification suivante :
	// Spécification du format du NIP (vu le 05.07 avec D. Radelfinger) : 6 caractères
	// - une lettre (sans le "O") au hasard
	// - 5 chiffres au hasard.
	@Test
	public void testGenerateCodeControle() throws Exception {
		for (int i = 0; i < 20; ++i) {
			final String codeControle = DeclarationImpotOrdinaire.generateCodeControle();
			try {
				assertCodeControleIsValid(codeControle);
			}
			catch (Exception e) {
				System.err.println("Code de contrôle généré : [" + codeControle + "]");
				throw e;
			}
		}
	}

	public static void assertCodeControleIsValid(String codeControle) {
		assertNotNull(codeControle);
		assertEquals(6, codeControle.length());

		final char letter = codeControle.charAt(0);
		assertFalse(letter == 'O');
		assertTrue('A' <= letter && letter <= 'Z');

		final int number = Integer.parseInt(codeControle.substring(1));
		assertTrue(0 <= number && number < 100000);
	}
}

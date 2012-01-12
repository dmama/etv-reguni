package ch.vd.uniregctb.common;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

public class ValidatorUtilsTest extends WithoutSpringTest {

	//private static final Logger LOGGER = Logger.getLogger(ValidatorUtilsTest.class);

	/**
	 * Crée la connexion à la base de données
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	/**
	 * Test numéro AVS
	 */
	@Test
	public void testAvs() {
		String ancienNumero = "234.12.323.123";
		String nouveauNumero = "234.1256.3232.12";
		String badNumero = "231.123.123.432";

		assertTrue(ValidatorUtils.validateAvs(ancienNumero));
		assertTrue(ValidatorUtils.validateAvs(nouveauNumero));

		assertFalse(ValidatorUtils.validateAvs(badNumero));
	}

	/**
	 * Test l'email
	 */
	@Test
	public void testEmail() {
		String validEmail = "claudio.parnenzini@vd.ch";
		String invalidEmail = "claudio,parnenzini@vd.ch";

		assertTrue(ValidatorUtils.validateEmail(validEmail));
		assertFalse(ValidatorUtils.validateEmail(invalidEmail));
	}
}

package ch.vd.uniregctb.iban;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import ch.vd.uniregctb.common.BusinessTest;

public class IbanValidatorTest extends BusinessTest {

	private IbanValidator ibanValidator = null;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		ibanValidator = getBean(IbanValidator.class, "ibanValidator");
	}

	@Test
	public void testValidate() throws Exception {

		// ceux-ci sont valides
		ibanValidator.validate("CH9308440717427290198");
		ibanValidator.validate("CH690023000123456789A");
		ibanValidator.validate("DE43123456780000087512");
		ibanValidator.validate("FR4812345678901234567890123");

		// problème de longueur
		try {
			ibanValidator.validate("CH930844071742790198");
			Assert.fail("Pas la bonne longueur : aurait dû échouer");
		}
		catch (IbanBadLengthException e) {
			// ok...
		}

		// problème de longueur sur un autre pays
		try {
			ibanValidator.validate("DE4312087512");
			Assert.fail("Pas la bonne longueur : aurait dû échouer");
		}
		catch (IbanBadLengthException e) {
			// ok...
		}

		// pays inconnu
		try {
			ibanValidator.validate("ZZ930844071742790198");
			Assert.fail("Pays inconnu : aurait dû échouer");
		}
		catch (IbanUnknownCountryException e) {
			// ok...
		}

		// problème de clearing en Suisse
		try {
			ibanValidator.validate("CH7900000001234567890");
			Assert.fail("clearing invalide (0): aurait dû échouer");
		}
		catch (IbanBadClearingNumberException e) {
			// ok...
		}

		// mauvais caractères utilisés
		try {
			ibanValidator.validate("CH7900000001%&4567890");
			Assert.fail("Caractères invalides : aurait dû échouer");
		}
		catch (IbanBadFormatException e) {
			Assert.assertTrue(e.getMessage().contains("les caractères doivent être alpha-numériques"));
		}

		// IBAN suisse avec des lettres là où le numéro de clearing se trouve
		try {
			ibanValidator.validate("CH88003Z300123456789A");
			Assert.fail("Caractères invalides : aurait dû échouer");
		}
		catch (IbanBadFormatException e) {
			Assert.assertTrue(e.getMessage().contains("doivent être numériques"));
		}

		// Mauvais modulo 97
		try {
			ibanValidator.validate("CH690023000123456879A");
			Assert.fail("Mauvais modulo (inversion de 7 et 8 à la fin) : aurait dû échouer");
		}
		catch (IbanNonPlausibleException e) {
			// ok
		}
	}

	@Test
	public void testExtractionClearing() {

		final String clearingInvalide = ibanValidator.getClearing("?");
		Assert.assertNull(clearingInvalide);

		final String clearingFr = ibanValidator.getClearing("FR3467526783456");
		Assert.assertNull(clearingFr);

		final String clearingCh = ibanValidator.getClearing("CH9300762011623852957");
		Assert.assertEquals("00762", clearingCh);
	}

	@Test
	@Ignore(value = "On a décidé de transformer les minuscules en majuscules avant la validation")
	public void testValidateAvecMinuscule() throws Exception {

		try {
			ibanValidator.validate("CH630025525577788840w");
			Assert.fail("IBAN devrait être invalide");
		}
		catch (IbanUpperCaseException e) {
			// tout va bien
		}
	}
}

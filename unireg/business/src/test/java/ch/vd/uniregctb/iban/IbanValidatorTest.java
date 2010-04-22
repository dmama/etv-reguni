package ch.vd.uniregctb.iban;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.BusinessTest;

public class IbanValidatorTest extends BusinessTest{

	private static final Logger LOGGER = Logger.getLogger(IbanValidatorTest.class);

	private IbanValidator ibanValidator = null;

	@Test
	public void testValidate() {

		LOGGER.debug("Début de testValidate");
		ibanValidator = getBean(IbanValidator.class, "ibanValidator");
		try {
			ibanValidator.validate("CH9308440717427290198");
		}
		catch (IbanValidationException e) {
			LOGGER.error("Impossible de valider l'iban CH9300762011623852957", e);
			Assert.fail();
		}
	}

	@Test
	public void testExtractionClearing() {
		ibanValidator = getBean(IbanValidator.class, "ibanValidator");
		final String clearingInvalide = ibanValidator.getClearing("?");
		Assert.assertNull(clearingInvalide);

		final String clearingFr = ibanValidator.getClearing("FR3467526783456");
		Assert.assertNull(clearingFr);

		final String clearingCh = ibanValidator.getClearing("CH9300762011623852957");
		Assert.assertEquals("00762", clearingCh);
	}

}

package ch.vd.uniregctb.iban;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.BusinessTest;

public class IbanHelperTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(IbanHelperTest.class);


	@Test
	public void testRemoveSpaceAndDoUpperCase() {

		LOGGER.debug("Début de test removeSpaceAndDoUpperCase");


		final String ibanATraiter = " ch 93084 407174 2729 019 8";
		final String ibanCorrect = "CH9308440717427290198";
		String newIban = IbanHelper.removeSpaceAndDoUpperCase(ibanATraiter);
		Assert.assertEquals(newIban, ibanCorrect);

		String ibanNull=null;
		Assert.assertNull(IbanHelper.removeSpaceAndDoUpperCase(ibanNull));
	}

	@Test
	public void testPerfRemoveSpaceAndDoUpperCase() {

		LOGGER.debug("Début de test removeSpaceAndDoUpperCase");


		final String ibanATraiter = " ch 93084 407174 2729 019 8";
		final String ibanCorrect = "CH9308440717427290198";
		long startTime = System.currentTimeMillis();
		String newIban = IbanHelper.removeSpaceAndDoUpperCase(ibanATraiter);
		long time = System.currentTimeMillis() - startTime;
		LOGGER.info("Temps d'execution removeSpaceUpperCase: " + time+" ms");
		Assert.assertEquals(newIban, ibanCorrect);

		String ibanNull=null;
		Assert.assertNull(IbanHelper.removeSpaceAndDoUpperCase(ibanNull));
	}


}

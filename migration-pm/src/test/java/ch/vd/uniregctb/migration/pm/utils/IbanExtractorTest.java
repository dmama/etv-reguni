package ch.vd.uniregctb.migration.pm.utils;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.migration.pm.regpm.RegpmCoordonneesFinancieres;
import ch.vd.uniregctb.migration.pm.regpm.RegpmInstitutionFinanciere;

public class IbanExtractorTest {

	private static RegpmInstitutionFinanciere buildInstitutionFinanciere(int clearing) {
		final RegpmInstitutionFinanciere inst = new RegpmInstitutionFinanciere();
		inst.setNoClearing(Integer.toString(clearing));
		return inst;
	}

	private static RegpmCoordonneesFinancieres buildCoordonneesFinancieres(String bicSwift, String iban, RegpmInstitutionFinanciere institutionFinanciere, String ccp, String noCompteBancaire) {
		final RegpmCoordonneesFinancieres cf = new RegpmCoordonneesFinancieres();
		cf.setBicSwift(bicSwift);
		cf.setIban(iban);
		cf.setInstitutionFinanciere(institutionFinanciere);
		cf.setNoCCP(ccp);
		cf.setNoCompteBancaire(noCompteBancaire);
		return cf;
	}

	@Test
	public void testDepuisIban() throws Exception {
		Assert.assertEquals("CH003247823", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, "CH003247823", null, "1-000001-2", "36324214423")));
		Assert.assertEquals("CH003247822", IbanExtractor.extractIban(buildCoordonneesFinancieres("CHPOFIBEXX", "CH003247822", null, "1-000001-2", "36324214423")));
		Assert.assertEquals("CH003247824", IbanExtractor.extractIban(buildCoordonneesFinancieres("DUMMY", "CH003247824", null, "1-000001-2", "36324214423")));
	}

	@Test
	public void testDepuisCCP() throws Exception {
		Assert.assertEquals("CH7009000000170003317", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, null, null, "17-331-7", "36324214423")));
		Assert.assertEquals("CH4709000000250000019", IbanExtractor.extractIban(buildCoordonneesFinancieres("CHPOFIBEXX", null, null, "25-1-9", "36324214423")));
		Assert.assertEquals("CH1309000000120005014", IbanExtractor.extractIban(buildCoordonneesFinancieres("DUMMY", null, null, "12-00501-4", "36324214423")));
	}

	@Test
	public void testDepuisCompteBancaire() throws Exception {
		// Exemple tiré de http://www.six-interbank-clearing.com/dam/downloads/fr/standardization/iban/calculator/dl_tkicch_prufziffer.pdf
		Assert.assertEquals("CH10002300A1023502601", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, null, buildInstitutionFinanciere(230), null, "A-10.2350.26.01")));

		// Exemple de http://www.ubs.com/ch/en/swissbank/private/pay-and-save/payments/iban.html
		Assert.assertEquals("CH350023023050422318T", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, null, buildInstitutionFinanciere(230), null, "230-504223.18T")));
	}
}

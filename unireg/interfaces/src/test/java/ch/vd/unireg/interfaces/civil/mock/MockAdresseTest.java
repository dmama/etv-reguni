package ch.vd.unireg.interfaces.civil.mock;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MockAdresseTest extends WithoutSpringTest {

	@Test
	public void testIsValidAt() {
		MockAdresse adresse = new MockAdresse();

		// validité = [1.1.2000..30.6.2000]
		adresse.setDateDebutValidite(RegDate.get(2000, 1, 1));
		adresse.setDateFinValidite(RegDate.get(2000, 6, 30));

		assertFalse(adresse.isValidAt(RegDate.get(1999, 12, 31)));
		assertTrue(adresse.isValidAt(RegDate.get(2000, 1, 1)));
		assertTrue(adresse.isValidAt(RegDate.get(2000, 4, 1)));
		assertTrue(adresse.isValidAt(RegDate.get(2000, 6, 30)));
		assertFalse(adresse.isValidAt(RegDate.get(2000, 7, 1)));

		// validité = [1.7.2000..fin-des-temps]
		adresse.setDateDebutValidite(RegDate.get(2000, 7, 1));
		adresse.setDateFinValidite(null);

		assertFalse(adresse.isValidAt(RegDate.get(2000, 6, 30)));
		assertTrue(adresse.isValidAt(RegDate.get(2000, 7, 1)));
		assertTrue(adresse.isValidAt(RegDate.get(2000, 12, 31)));
		assertTrue(adresse.isValidAt(RegDate.get(2045, 6, 30)));
	}
}

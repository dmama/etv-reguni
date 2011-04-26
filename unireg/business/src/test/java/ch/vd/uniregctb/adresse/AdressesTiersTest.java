package ch.vd.uniregctb.adresse;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdressesTiersTest {

	@Test
	public void testOfType() {

		final AdresseTiers adressesCourrier = new AdresseCivile();
		final AdresseTiers adressesRepresentation = new AdresseCivile();
		final AdresseTiers adressesPoursuite = new AdresseCivile();
		final AdresseTiers adressesDomicile = new AdresseCivile();

		AdressesTiers adresses = new AdressesTiers();
		adresses.courrier = adressesCourrier;
		adresses.representation = adressesRepresentation;
		adresses.poursuite = adressesPoursuite;
		adresses.domicile = adressesDomicile;

		assertEquals(adressesCourrier, adresses.ofType(TypeAdresseTiers.COURRIER));
		assertEquals(adressesRepresentation, adresses.ofType(TypeAdresseTiers.REPRESENTATION));
		assertEquals(adressesPoursuite, adresses.ofType(TypeAdresseTiers.POURSUITE));
		assertEquals(adressesDomicile, adresses.ofType(TypeAdresseTiers.DOMICILE));
	}

	@Test
	public void testIsValid() {

		final AdresseTiers adresse = new AdresseCivile();
		adresse.setDateDebut(RegDate.get(2000, 1, 1));
		adresse.setDateFin(RegDate.get(2009, 12, 31));

		adresse.setAnnule(false);
		assertTrue(adresse.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(adresse.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(adresse.isValidAt(RegDate.get(2060, 1, 1)));

		adresse.setAnnule(true);
		assertFalse(adresse.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(adresse.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(adresse.isValidAt(RegDate.get(2060, 1, 1)));
	}
}

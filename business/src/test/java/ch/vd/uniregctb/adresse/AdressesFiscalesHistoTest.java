package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;

public class AdressesFiscalesHistoTest {

	@Test
	public void testOfType() {

		final List<AdresseGenerique> adressesCourrier = new ArrayList<>();
		final List<AdresseGenerique> adressesRepresentation = new ArrayList<>();
		final List<AdresseGenerique> adressesDomicile = new ArrayList<>();
		final List<AdresseGenerique> adressesPoursuite = new ArrayList<>();
		final List<AdresseGenerique> adressesPoursuiteAutreTiers = new ArrayList<>();

		AdressesFiscalesHisto adresses = new AdressesFiscalesHisto();
		adresses.courrier = adressesCourrier;
		adresses.representation = adressesRepresentation;
		adresses.domicile = adressesDomicile;
		adresses.poursuite = adressesPoursuite;
		adresses.poursuiteAutreTiers = adressesPoursuiteAutreTiers;

		assertEquals(adressesCourrier, adresses.ofType(TypeAdresseTiers.COURRIER));
		assertEquals(adressesRepresentation, adresses.ofType(TypeAdresseTiers.REPRESENTATION));
		assertEquals(adressesDomicile, adresses.ofType(TypeAdresseTiers.DOMICILE));
		assertEquals(adressesPoursuite, adresses.ofType(TypeAdresseTiers.POURSUITE));

		assertEquals(adressesCourrier, adresses.ofType(TypeAdresseFiscale.COURRIER));
		assertEquals(adressesRepresentation, adresses.ofType(TypeAdresseFiscale.REPRESENTATION));
		assertEquals(adressesDomicile, adresses.ofType(TypeAdresseFiscale.DOMICILE));
		assertEquals(adressesPoursuite, adresses.ofType(TypeAdresseFiscale.POURSUITE));
		assertEquals(adressesPoursuiteAutreTiers, adresses.ofType(TypeAdresseFiscale.POURSUITE_AUTRE_TIERS));
	}
}

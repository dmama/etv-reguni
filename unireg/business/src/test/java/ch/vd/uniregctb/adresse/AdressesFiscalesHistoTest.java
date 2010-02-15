package ch.vd.uniregctb.adresse;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseTiers;

@RunWith(JUnit4ClassRunner.class)
public class AdressesFiscalesHistoTest {

	final ServiceInfrastructureService serviceInfra = new DefaultMockServiceInfrastructureService();

	@Test
	public void testOfType() {

		final List<AdresseGenerique> adressesCourrier = new ArrayList<AdresseGenerique>();
		final List<AdresseGenerique> adressesRepresentation = new ArrayList<AdresseGenerique>();
		final List<AdresseGenerique> adressesPoursuite = new ArrayList<AdresseGenerique>();
		final List<AdresseGenerique> adressesDomicile = new ArrayList<AdresseGenerique>();

		AdressesFiscalesHisto adresses = new AdressesFiscalesHisto();
		adresses.courrier = adressesCourrier;
		adresses.representation = adressesRepresentation;
		adresses.poursuite = adressesPoursuite;
		adresses.domicile = adressesDomicile;

		assertEquals(adressesCourrier, adresses.ofType(TypeAdresseTiers.COURRIER));
		assertEquals(adressesRepresentation, adresses.ofType(TypeAdresseTiers.REPRESENTATION));
		assertEquals(adressesPoursuite, adresses.ofType(TypeAdresseTiers.POURSUITE));
		assertEquals(adressesDomicile, adresses.ofType(TypeAdresseTiers.DOMICILE));
	}
}

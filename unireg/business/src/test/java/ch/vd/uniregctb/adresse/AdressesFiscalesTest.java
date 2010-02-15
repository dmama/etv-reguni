package ch.vd.uniregctb.adresse;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseTiers;


@RunWith(JUnit4ClassRunner.class)
public class AdressesFiscalesTest {

	final ServiceInfrastructureService serviceInfra = new DefaultMockServiceInfrastructureService();

	@Test
	public void testOfType() throws Exception {

		final AdresseCivileAdapter adresseCourrier = new AdresseCivileAdapter(new MockAdresse(), false,serviceInfra);
		final AdresseCivileAdapter adresseRepresentation = new AdresseCivileAdapter(new MockAdresse(), false,serviceInfra);
		final AdresseCivileAdapter adressePoursuite = new AdresseCivileAdapter(new MockAdresse(), false,serviceInfra);
		final AdresseCivileAdapter adresseDomicile = new AdresseCivileAdapter(new MockAdresse(), false,serviceInfra);

		AdressesFiscales adresses = new AdressesFiscales();
		adresses.courrier = adresseCourrier;
		adresses.representation = adresseRepresentation;
		adresses.poursuite = adressePoursuite;
		adresses.domicile = adresseDomicile;

		assertEquals(adresseCourrier, adresses.ofType(TypeAdresseTiers.COURRIER));
		assertEquals(adresseRepresentation, adresses.ofType(TypeAdresseTiers.REPRESENTATION));
		assertEquals(adressePoursuite, adresses.ofType(TypeAdresseTiers.POURSUITE));
		assertEquals(adresseDomicile, adresses.ofType(TypeAdresseTiers.DOMICILE));
	}
}

package ch.vd.uniregctb.adresse;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;


@RunWith(JUnit4ClassRunner.class)
public class AdressesFiscalesTest {

	final ServiceInfrastructureService serviceInfra = new DefaultMockServiceInfrastructureService();

	@Test
	public void testOfType() throws Exception {

		final AdresseCivileAdapter adresseCourrier = new AdresseCivileAdapter(new MockAdresse(), (Tiers) null, false, serviceInfra);
		final AdresseCivileAdapter adresseRepresentation = new AdresseCivileAdapter(new MockAdresse(), (Tiers) null, false, serviceInfra);
		final AdresseCivileAdapter adresseDomicile = new AdresseCivileAdapter(new MockAdresse(), (Tiers) null, false, serviceInfra);
		final AdresseCivileAdapter adressePoursuite = new AdresseCivileAdapter(new MockAdresse(), (Tiers) null, false, serviceInfra);
		final AdresseCivileAdapter adressePoursuiteAutreTiers = new AdresseCivileAdapter(new MockAdresse(), (Tiers) null, false, serviceInfra);

		AdressesFiscales adresses = new AdressesFiscales();
		adresses.courrier = adresseCourrier;
		adresses.representation = adresseRepresentation;
		adresses.domicile = adresseDomicile;
		adresses.poursuite = adressePoursuite;
		adresses.poursuiteAutreTiers = adressePoursuiteAutreTiers;

		assertEquals(adresseCourrier, adresses.ofType(TypeAdresseTiers.COURRIER));
		assertEquals(adresseRepresentation, adresses.ofType(TypeAdresseTiers.REPRESENTATION));
		assertEquals(adresseDomicile, adresses.ofType(TypeAdresseTiers.DOMICILE));
		assertEquals(adressePoursuite, adresses.ofType(TypeAdresseTiers.POURSUITE));

		assertEquals(adresseCourrier, adresses.ofType(TypeAdresseFiscale.COURRIER));
		assertEquals(adresseRepresentation, adresses.ofType(TypeAdresseFiscale.REPRESENTATION));
		assertEquals(adresseDomicile, adresses.ofType(TypeAdresseFiscale.DOMICILE));
		assertEquals(adressePoursuite, adresses.ofType(TypeAdresseFiscale.POURSUITE));
		assertEquals(adressePoursuiteAutreTiers, adresses.ofType(TypeAdresseFiscale.POURSUITE_AUTRE_TIERS));
	}
}

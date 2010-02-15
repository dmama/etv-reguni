package ch.vd.uniregctb.tiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaireAdapter;
import ch.vd.uniregctb.adresse.AdresseGenerique.Source;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class AdresseSupplementaireAdapterTest extends WithoutSpringTest {

	final ServiceInfrastructureService serviceInfra = new DefaultMockServiceInfrastructureService();

	@Test
	public void testAdresseSuisse() {
		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(RegDate.get(1930, 1, 1));
		adresse.setDateFin(null);
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setComplement("complement");
		adresse.setRue("rue");
		adresse.setNumeroMaison("13bis");
		adresse.setNumeroAppartement("numero appartement");
		adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
		adresse.setNumeroCasePostale(1234);
		adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
		adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());

		final AdresseSupplementaireAdapter adapter = new AdresseSupplementaireAdapter(adresse, false, serviceInfra);
		assertEquals(adresse, adapter.getAdresse());
		assertEquals("Case Postale 1234", adapter.getCasePostale());

		assertEquals(RegDate.get(1930, 1, 1), adapter.getDateDebut());
		assertNull(adapter.getDateFin());
		assertEquals("Lausanne", adapter.getLieu());
		assertEquals("Lausanne", adapter.getLocalite());
		assertEquals("1000", adapter.getNpa());
		assertEquals("13bis", adapter.getNumero());
		assertEquals("numero appartement", adapter.getNumeroAppartement());
		assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue(), adapter.getNumeroRue());
		assertEquals(MockLocalite.Lausanne.getNoOrdre().intValue(), adapter.getNumeroOrdrePostal());
		assertEquals(MockLocalite.Lausanne.getNPA().toString(), adapter.getNumeroPostal());
		assertNull(adapter.getNumeroPostalComplementaire());
		assertEquals(ServiceInfrastructureService.noOfsSuisse, adapter.getNoOfsPays().intValue());
		assertEquals("Av de Beaulieu", adapter.getRue());
		assertEquals(Source.FISCALE, adapter.getSource());
		assertEquals("complement", adapter.getComplement());
	}

	@Test
	public void testAdresseEtrangere() {
		/*
		 * Adresse de travail:
		 *
		 * Google Copenhagen
		 * Google Denmark ApS
		 * Larsbjornsstraede 3
		 * 1454 Copenhagen K.
		 * Denmark
		 * Phone: +(0045) 3337 7199
		 * Fax: +(0045) 3332 4370
		 */
		final AdresseEtrangere adresse = new AdresseEtrangere();
		adresse.setDateDebut(RegDate.get(1995, 1, 1));
		adresse.setDateFin(null);
		adresse.setUsage(TypeAdresseTiers.REPRESENTATION);
		adresse.setComplement("Google Denmark ApS");
		adresse.setRue("Larsbjornsstraede");
		adresse.setNumeroMaison("3");
		adresse.setNumeroAppartement(null);
		adresse.setTexteCasePostale(null);
		adresse.setNumeroCasePostale(null);
		adresse.setNumeroPostalLocalite("1454 Copenhagen");
		adresse.setComplementLocalite("K.");
		adresse.setNumeroOfsPays(MockPays.Danemark.getNoOFS());

		final AdresseSupplementaireAdapter adapter = new AdresseSupplementaireAdapter(adresse, false, serviceInfra);
		assertEquals(adresse, adapter.getAdresse());
		assertNull(adapter.getCasePostale());
		assertEquals(RegDate.get(1995, 1, 1), adapter.getDateDebut());
		assertNull(adapter.getDateFin());
		assertEquals("1454 Copenhagen", adapter.getLieu());
		assertEquals("1454 Copenhagen", adapter.getLocalite());
		assertEquals("", adapter.getNpa());
		assertEquals("3", adapter.getNumero());
		assertNull(adapter.getNumeroAppartement());
		assertNull(adapter.getNumeroRue());
		assertEquals(0, adapter.getNumeroOrdrePostal());
		assertEquals("", adapter.getNumeroPostal());
		assertEquals("", adapter.getNumeroPostalComplementaire());
		assertEquals(MockPays.Danemark.getNoOFS(), adapter.getNoOfsPays().intValue());
		assertEquals("Larsbjornsstraede", adapter.getRue());
		assertEquals(Source.FISCALE, adapter.getSource());
		assertEquals("Google Denmark ApS", adapter.getComplement());
	}
}

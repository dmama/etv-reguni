package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseGenerique.SourceType;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaireAdapter;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class AdresseSupplementaireAdapterTest extends WithoutSpringTest {

	final ServiceInfrastructureService serviceInfra = new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService());

	/**
	 * [SIFISC-143] test la surcharge du npa de la localité par le npa de la case postale pour les adresses suisses
	 */
	@Test
	public void testAdresseSuisseAvecNpaCasePostaleRenseignee() {
		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(RegDate.get(1930, 1, 1));
		adresse.setDateFin(null);
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setRue("rue");
		adresse.setNumeroMaison("13bis");
		adresse.setNumeroAppartement("numero appartement");
		adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
		adresse.setNumeroCasePostale(1234);
		adresse.setNpaCasePostale(9999); // Surcharge de du npa de la localité par le npa de la case postale
		adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
		adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());

		final AdresseSupplementaireAdapter adapter = new AdresseSupplementaireAdapter(adresse, null, false, serviceInfra);
		assertFalse(MockLocalite.Lausanne.getNPA().toString().equals(adapter.getNumeroPostal()));
		assertEquals("9999", adapter.getNumeroPostal());
	}

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

		final AdresseSupplementaireAdapter adapter = new AdresseSupplementaireAdapter(adresse, null, false, serviceInfra);
		assertEquals(adresse, adapter.getAdresse());
		assertEquals(new CasePostale(TexteCasePostale.CASE_POSTALE, 1234), adapter.getCasePostale());

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
		assertEquals(SourceType.FISCALE, adapter.getSource().getType());
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

		final AdresseSupplementaireAdapter adapter = new AdresseSupplementaireAdapter(adresse, null, false, serviceInfra);
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
		assertEquals(SourceType.FISCALE, adapter.getSource().getType());
		assertEquals("Google Denmark ApS", adapter.getComplement());
	}
}

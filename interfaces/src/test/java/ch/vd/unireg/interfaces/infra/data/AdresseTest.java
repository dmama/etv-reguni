package ch.vd.unireg.interfaces.infra.data;

import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.fidor.xml.colladm.v1.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AdresseTest {

	/**
	 * [FISCPROJ-1213] Vérifie que le nom de la rue est bien construit à partir de l'estrid si renseigné.
	 */
	@Test
	public void testAdresseCollAdminFidorAvecEstrid() {

		final ServiceInfrastructureRaw service = Mockito.mock(ServiceInfrastructureRaw.class);
		Mockito.when(service.getRueByNumero(1132347, null)).thenReturn(MockRue.Echallens.PlaceEmileGardaz);
		Mockito.when(service.getLocaliteByONRP(185, null)).thenReturn(MockLocalite.Echallens);

		final Adresse adresseFidor = new Adresse();
		adresseFidor.setCasePostale("Case postale");
		adresseFidor.setNumeroCasePostale("56");
		adresseFidor.setEstrid(1132347);    // Place Emile Gardaz
		adresseFidor.setNumeroMaison("5");
		adresseFidor.setNoOrdrePoste(185); // 1040 Echallens

		final AdresseImpl adresse = new AdresseImpl(adresseFidor, service);
		assertNull(adresse.getDateDebut());
		assertNull(adresse.getDateFin());
		assertEquals(new CasePostale(TexteCasePostale.CASE_POSTALE, 56), adresse.getCasePostale());
		assertEquals("Echallens", adresse.getLocalite());
		assertEquals("5", adresse.getNumero());
		assertNull(adresse.getNumeroAppartement());
		assertEquals(Integer.valueOf(1132347), adresse.getNumeroRue());
		assertEquals(Integer.valueOf(185), adresse.getNumeroOrdrePostal());
		assertEquals("1040", adresse.getNumeroPostal());
		assertNull(adresse.getNumeroPostalComplementaire());
		assertEquals(Integer.valueOf(8100), adresse.getNoOfsPays());
		assertEquals("Place Emile Gardaz 5", adresse.getRue());
		assertNull(adresse.getTitre());
		assertEquals(TypeAdresseCivil.COURRIER, adresse.getTypeAdresse());
		assertNull(adresse.getNoOfsCommuneAdresse());
		assertNull(adresse.getEgid());
		assertNull(adresse.getEwid());
	}

	/**
	 * [FISCPROJ-1213] Vérifie que le nom de la rue est bien construit à partir du nom de rue si l'estrid n'est pas renseigné.
	 */
	@Test
	public void testAdresseCollAdminFidorAvecNomRue() {

		final ServiceInfrastructureRaw service = Mockito.mock(ServiceInfrastructureRaw.class);
		Mockito.when(service.getLocaliteByONRP(162, null)).thenReturn(MockLocalite.Lausanne1014);

		final Adresse adresseFidor = new Adresse();
		adresseFidor.setNomRue("Route de Berne");
		adresseFidor.setNumeroMaison("46");
		adresseFidor.setNoOrdrePoste(162); // 1014 Lausanne Adm cant VD

		final AdresseImpl adresse = new AdresseImpl(adresseFidor, service);
		assertNull(adresse.getDateDebut());
		assertNull(adresse.getDateFin());
		assertNull(adresse.getCasePostale());
		assertEquals("Lausanne Adm cant VD", adresse.getLocalite());
		assertEquals("46", adresse.getNumero());
		assertNull(adresse.getNumeroAppartement());
		assertNull(adresse.getNumeroRue());
		assertEquals(Integer.valueOf(162), adresse.getNumeroOrdrePostal());
		assertEquals("1014", adresse.getNumeroPostal());
		assertNull(adresse.getNumeroPostalComplementaire());
		assertEquals(Integer.valueOf(8100), adresse.getNoOfsPays());
		assertEquals("Route de Berne 46", adresse.getRue());
		assertNull(adresse.getTitre());
		assertEquals(TypeAdresseCivil.COURRIER, adresse.getTypeAdresse());
		assertNull(adresse.getNoOfsCommuneAdresse());
		assertNull(adresse.getEgid());
		assertNull(adresse.getEwid());
	}

	/**
	 * [FISCPROJ-1213] Vérifie que le numéro de maison fait bien partie de la rue quand il est renseigné.
	 */
	@Test
	public void testAdresseCollAdminFidorRueAvecNumero() {

		final ServiceInfrastructureRaw service = Mockito.mock(ServiceInfrastructureRaw.class);
		Mockito.when(service.getLocaliteByONRP(162, null)).thenReturn(MockLocalite.Lausanne1014);

		final Adresse adresseFidor = new Adresse();
		adresseFidor.setNomRue("Route de Berne");
		adresseFidor.setNumeroMaison("46");
		adresseFidor.setNoOrdrePoste(162); // 1014 Lausanne Adm cant VD

		final AdresseImpl adresse = new AdresseImpl(adresseFidor, service);
		assertEquals("Route de Berne 46", adresse.getRue());
	}
}
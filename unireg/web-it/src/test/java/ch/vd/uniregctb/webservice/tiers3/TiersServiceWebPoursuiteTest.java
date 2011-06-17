package ch.vd.uniregctb.webservice.tiers3;

import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.FormattedAddress;
import ch.vd.unireg.webservices.tiers3.GetTiersRequest;
import ch.vd.unireg.webservices.tiers3.MailAddress;
import ch.vd.unireg.webservices.tiers3.MailAddressOtherTiers;
import ch.vd.unireg.webservices.tiers3.MenageCommun;
import ch.vd.unireg.webservices.tiers3.Tiers;
import ch.vd.unireg.webservices.tiers3.TiersPart;
import ch.vd.unireg.webservices.tiers3.TypeAdressePoursuiteAutreTiers;
import ch.vd.unireg.webservices.tiers3.TypeAffranchissement;
import ch.vd.unireg.webservices.tiers3.UserLogin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Voir la spécification "BesoinsContentieux.doc"
 */
public class TiersServiceWebPoursuiteTest extends AbstractTiersServiceWebTest {

	// Les données du fichier ont été extraites des tests unitaires AdresseServiceTest.testGetAdressesPoursuite*().
	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebPoursuiteTest.xml";

	private UserLogin login;

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}

		login = new UserLogin();
		login.setUserId("[UT] TiersServiceWebPoursuiteTest");
		login.setOid(22);
	}

	@Test
	public void testGetAdressesPoursuiteContribuableCelibataire() throws Exception {

		final long noTiers = 44018108;

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_FORMATTEES);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final MailAddress domicile = tiers.getAdresseDomicileFormattee();
		final FormattedAddress formattee = domicile.getFormattedAddress();
		assertFormattedAddress(formattee, "Monsieur", "Philippe Galley", "Chemin Sous le Bois 22", "1523 Granges-Marnand");
		assertEquals(TypeAffranchissement.SUISSE, domicile.getAddressInformation().getTypeAffranchissement());
		assertAdresseEquals(formattee, tiers.getAdresseCourrierFormattee().getFormattedAddress());
		assertAdresseEquals(formattee, tiers.getAdressePoursuiteFormattee().getFormattedAddress());
		assertNull(tiers.getAdressePoursuiteAutreTiersFormattee());
	}

	@Test
	public void testGetAdressesPoursuiteContribuableCouple() throws Exception {

		final long noTiersMenage = 10069459;
		final long noTiersPrincipal;
		final long noTiersConjoint;

		// détermination des composants du ménage
		{
			final GetTiersRequest params = new GetTiersRequest();
			params.setLogin(login);
			params.setTiersNumber(noTiersMenage);
			params.getParts().add(TiersPart.COMPOSANTS_MENAGE);

			final MenageCommun tiers = (MenageCommun) service.getTiers(params);
			assertNotNull(tiers);
			assertEquals(noTiersMenage, tiers.getNumero());

			noTiersPrincipal = tiers.getContribuablePrincipal().getNumero();
			noTiersConjoint = tiers.getContribuableSecondaire().getNumero();
		}

		// récupération des adresses de poursuite du principal
		{
			final GetTiersRequest params = new GetTiersRequest();
			params.setLogin(login);
			params.setTiersNumber(noTiersPrincipal);
			params.getParts().add(TiersPart.ADRESSES);
			params.getParts().add(TiersPart.ADRESSES_FORMATTEES);

			final Tiers tiers = service.getTiers(params);
			assertNotNull(tiers);
			assertEquals(noTiersPrincipal, tiers.getNumero());

			// Teste les adresses formattées
			final MailAddress domicile = tiers.getAdresseDomicileFormattee();
			final FormattedAddress formattee = domicile.getFormattedAddress();
			assertFormattedAddress(formattee, "Monsieur", "Thierry Ralet", "Ch. des Fleurettes 6", "1860 Aigle");
			assertEquals(TypeAffranchissement.SUISSE, domicile.getAddressInformation().getTypeAffranchissement());
			assertFormattedAddress(tiers.getAdresseCourrierFormattee().getFormattedAddress(), "Monsieur", "Thierry Ralet", "Chemin des Fleurettes 6", "1860 Aigle");
			assertAdresseEquals(formattee, tiers.getAdressePoursuiteFormattee().getFormattedAddress());
			assertNull(tiers.getAdressePoursuiteAutreTiersFormattee());
		}

		// récupération des adresses de poursuite du conjoint
		{
			final GetTiersRequest params = new GetTiersRequest();
			params.setLogin(login);
			params.setTiersNumber(noTiersConjoint);
			params.getParts().add(TiersPart.ADRESSES);
			params.getParts().add(TiersPart.ADRESSES_FORMATTEES);

			final Tiers tiers = service.getTiers(params);
			assertNotNull(tiers);
			assertEquals(noTiersConjoint, tiers.getNumero());

			// Teste les adresses formattées
			final MailAddress domicile = tiers.getAdresseDomicileFormattee();
			assertFormattedAddress(domicile.getFormattedAddress(), "Madame", "Fabienne Girardet Ralet", "Route de Chailly 276", "1814 La Tour-de-Peilz");
			assertEquals(TypeAffranchissement.SUISSE, domicile.getAddressInformation().getTypeAffranchissement());
			final MailAddress courrier = tiers.getAdresseCourrierFormattee();
			assertFormattedAddress(courrier.getFormattedAddress(), "Madame", "Fabienne Girardet Ralet", "Route de Chailly 276", "1814 La Tour-de-Peilz");
			assertAdresseEquals(domicile.getFormattedAddress(), tiers.getAdressePoursuiteFormattee().getFormattedAddress());
			assertNull(tiers.getAdressePoursuiteAutreTiersFormattee());

		}
	}

	@Test
	public void testGetAdressesPoursuiteContribuableSousCuratelle() throws Exception {

		final long noTiers = 89016804;

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_FORMATTEES);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final MailAddress domicile = tiers.getAdresseDomicileFormattee();
		assertFormattedAddress(domicile.getFormattedAddress(), "Monsieur", "Marc Stäheli", "Chemin des Peupliers 1", "1008 Prilly");
		assertEquals(TypeAffranchissement.SUISSE, domicile.getAddressInformation().getTypeAffranchissement());
		assertFormattedAddress(tiers.getAdresseCourrierFormattee().getFormattedAddress(), "Monsieur", "Marc Stäheli", "p.a. Alain Bally", "Place Saint-François", "1003 Lausanne");
		assertAdresseEquals(domicile.getFormattedAddress(), tiers.getAdressePoursuiteFormattee().getFormattedAddress());
		assertEquals(TypeAdressePoursuiteAutreTiers.CURATELLE, tiers.getAdressePoursuiteAutreTiersFormattee().getType());
		assertFormattedAddress(tiers.getAdressePoursuiteAutreTiersFormattee().getFormattedAddress(), "Monsieur", "Alain Bally", "Place Saint-François", "1003 Lausanne");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableSousTutelle() throws Exception {

		final long noTiers = 60510843;

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_FORMATTEES);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final MailAddress domicile = tiers.getAdresseDomicileFormattee();
		assertFormattedAddress(domicile.getFormattedAddress(), "Madame", "Anabela Lopes", "Avenue Kiener 69", "1400 Yverdon-les-Bains");
		assertEquals(TypeAffranchissement.SUISSE, domicile.getAddressInformation().getTypeAffranchissement());
		assertFormattedAddress(tiers.getAdresseCourrierFormattee().getFormattedAddress(), "Madame", "Anabela Lopes", "p.a. TUTEUR GENERAL VD", "Chemin de Mornex 32", "1014 Lausanne Adm cant");

		// devrait être ci-après mais l'info n'est pas à jour dans le host : assertFormattedAddress(tiers.getAdressePoursuiteFormattee(), "Justice de Paix des districts du Jura-Nord vaudois et du Gros-de-Vaud", "Case Postale 693", "Rue du Pré 2", "1400 Yverdon-les-Bains");
		assertFormattedAddress(tiers.getAdressePoursuiteFormattee().getFormattedAddress(), "Monsieur le Juge de Paix de Belmont/Conc", "ise/Champvent/Grandson/Ste-Croix/Yverdon", "Rue du Lac",
				"1400 Yverdon-les-Bains");

		assertEquals(TypeAdressePoursuiteAutreTiers.TUTELLE, tiers.getAdressePoursuiteAutreTiersFormattee().getType());
		assertFormattedAddress(tiers.getAdressePoursuiteAutreTiersFormattee().getFormattedAddress(), "Office du tuteur général", "du Canton de Vaud", "Chemin de Mornex 32", "1014 Lausanne Adm cant");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableHSAvecRepresentantConventionel() throws Exception {

		final long noTiers = 10536395;

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_FORMATTEES);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		assertFormattedAddress(tiers.getAdresseDomicileFormattee().getFormattedAddress(), "Monsieur", "Claude-Alain Proz", "Izmir", "Turquie");
		assertFormattedAddress(tiers.getAdresseCourrierFormattee().getFormattedAddress(), "Monsieur", "Claude-Alain Proz", "p.a. KPMG AG (KPMG SA) (KPMG Ltd)", "Badenerstr. 172 - Postfach",
				"8026 Zürich");
		assertFormattedAddress(tiers.getAdressePoursuiteFormattee().getFormattedAddress(), "KPMG AG", "(KPMG SA)", "(KPMG Ltd)", "Badenerstr. 172 - Postfach", "8026 Zürich");
		assertEquals(TypeAdressePoursuiteAutreTiers.MANDATAIRE, tiers.getAdressePoursuiteAutreTiersFormattee().getType());
		assertFormattedAddress(tiers.getAdressePoursuiteAutreTiersFormattee().getFormattedAddress(), "KPMG AG", "(KPMG SA)", "(KPMG Ltd)", "Badenerstr. 172 - Postfach", "8026 Zürich");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableVDAvecRepresentantConventionel() throws Exception {

		final long noTiers = 10033975;

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_FORMATTEES);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final MailAddress domicile = tiers.getAdresseDomicileFormattee();
		assertFormattedAddress(domicile.getFormattedAddress(), "Monsieur", "Marcello Pesci", "Ch. de Réchoz 17", "1027 Lonay");
		assertEquals(TypeAffranchissement.SUISSE, domicile.getAddressInformation().getTypeAffranchissement());
		assertFormattedAddress(tiers.getAdresseCourrierFormattee().getFormattedAddress(), "Monsieur", "Marcello Pesci", "p.a. Curia Treuhand AG", "Postfach 132", "7000 Chur");
		assertAdresseEquals(domicile.getFormattedAddress(), tiers.getAdressePoursuiteFormattee().getFormattedAddress());
		assertNull(tiers.getAdressePoursuiteAutreTiersFormattee());
	}

	@Test
	public void testGetAdressesPoursuiteContribuableAvecAdresseSpecifiquePoursuite() throws Exception {

		final long noTiers = 44018109;

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_FORMATTEES);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final MailAddress domicile = tiers.getAdresseDomicileFormattee();
		assertFormattedAddress(domicile.getFormattedAddress(), "Monsieur", "Philippe Galley", "Chemin Sous le Bois 22", "1523 Granges-Marnand");
		assertEquals(TypeAffranchissement.SUISSE, domicile.getAddressInformation().getTypeAffranchissement());
		assertAdresseEquals(domicile.getFormattedAddress(), tiers.getAdresseCourrierFormattee().getFormattedAddress());

		assertFormattedAddress(tiers.getAdressePoursuiteFormattee().getFormattedAddress(), "Monsieur", "Philippe Galley", "Chemin de Praz-Berthoud", "1010 Lausanne");
		final MailAddressOtherTiers poursuiteAutreTiers = tiers.getAdressePoursuiteAutreTiersFormattee();
		assertEquals(TypeAdressePoursuiteAutreTiers.SPECIFIQUE, poursuiteAutreTiers.getType());
		assertFormattedAddress(poursuiteAutreTiers.getFormattedAddress(), "Monsieur", "Philippe Galley", "Chemin de Praz-Berthoud", "1010 Lausanne");
	}

	private static void assertAdresseEquals(FormattedAddress expected, FormattedAddress actual) {
		assertTrue((expected == null && actual == null) || (expected != null && actual != null));
		if (expected != null) {
			assertEquals(expected.getLine1(), actual.getLine1());
			assertEquals(expected.getLine2(), actual.getLine2());
			assertEquals(expected.getLine3(), actual.getLine3());
			assertEquals(expected.getLine4(), actual.getLine4());
			assertEquals(expected.getLine5(), actual.getLine5());
			assertEquals(expected.getLine6(), actual.getLine6());
		}
	}

	private static void assertFormattedAddress(FormattedAddress adresse, String... lignes) {
		assertNotNull(adresse);
		assertTrue(lignes.length <= 6);

		if (lignes.length > 0) {
			assertEquals(lignes[0], trimValiPattern(adresse.getLine1()));
			if (lignes.length > 1) {
				assertEquals(lignes[1], trimValiPattern(adresse.getLine2()));
				if (lignes.length > 2) {
					assertEquals(lignes[2], trimValiPattern(adresse.getLine3()));
					if (lignes.length > 3) {
						assertEquals(lignes[3], trimValiPattern(adresse.getLine4()));
						if (lignes.length > 4) {
							assertEquals(lignes[4], trimValiPattern(adresse.getLine5()));
							if (lignes.length > 5) {
								assertEquals(lignes[5], trimValiPattern(adresse.getLine6()));
							}
							else {
								assertNull(adresse.getLine6());
							}
						}
						else {
							assertNull(adresse.getLine5());
						}
					}
					else {
						assertNull(adresse.getLine4());
					}
				}
				else {
					assertNull(adresse.getLine3());
				}
			}
			else {
				assertNull(adresse.getLine2());
			}
		}
		else {
			assertNull(adresse.getLine1());
		}
	}
}

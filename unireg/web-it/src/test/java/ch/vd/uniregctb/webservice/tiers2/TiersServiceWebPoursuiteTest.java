package ch.vd.uniregctb.webservice.tiers2;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers2.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers2.AdresseEnvoiAutreTiers;
import ch.vd.uniregctb.webservices.tiers2.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.MenageCommun;
import ch.vd.uniregctb.webservices.tiers2.Tiers;
import ch.vd.uniregctb.webservices.tiers2.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.TypeAdressePoursuiteAutreTiers;
import ch.vd.uniregctb.webservices.tiers2.UserLogin;

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
		login.setOid(0);
	}

	@Test
	public void testGetAdressesPoursuiteContribuableCelibataire() throws Exception {

		final long noTiers = 44018108;

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.setDate(null);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final AdresseEnvoi domicile = tiers.getAdresseDomicileFormattee();
		assertAdresseEnvoi(domicile, "Monsieur", "Philippe Galley", "Ch. Sous le Bois", "1523 Granges-Marnand");
		assertAdresseEquals(domicile, tiers.getAdresseEnvoi());
		assertAdresseEquals(domicile, tiers.getAdressePoursuiteFormattee());
		assertNull(tiers.getAdressePoursuiteAutreTiersFormattee());
	}

	@Test
	public void testGetAdressesPoursuiteContribuableCouple() throws Exception {

		final long noTiersMenage = 10069459;
		final long noTiersPrincipal;
		final long noTiersConjoint;

		// détermination des composants du ménage
		{
			final GetTiers params = new GetTiers();
			params.setLogin(login);
			params.setTiersNumber(noTiersMenage);
			params.setDate(null);
			params.getParts().add(TiersPart.COMPOSANTS_MENAGE);

			final MenageCommun tiers = (MenageCommun) service.getTiers(params);
			assertNotNull(tiers);
			assertEquals(noTiersMenage, tiers.getNumero());

			noTiersPrincipal = tiers.getContribuablePrincipal().getNumero();
			noTiersConjoint = tiers.getContribuableSecondaire().getNumero();
		}

		// récupération des adresses de poursuite du principal
		{
			final GetTiers params = new GetTiers();
			params.setLogin(login);
			params.setTiersNumber(noTiersPrincipal);
			params.setDate(null);
			params.getParts().add(TiersPart.ADRESSES);
			params.getParts().add(TiersPart.ADRESSES_ENVOI);

			final Tiers tiers = service.getTiers(params);
			assertNotNull(tiers);
			assertEquals(noTiersPrincipal, tiers.getNumero());

			// Teste les adresses formattées
			final AdresseEnvoi domicile = tiers.getAdresseDomicileFormattee();
			assertAdresseEnvoi(domicile, "Monsieur", "Thierry Ralet", "Chemin des Fleurettes 6", "1860 Aigle");
			assertAdresseEquals(domicile, tiers.getAdresseEnvoi());
			assertAdresseEquals(domicile, tiers.getAdressePoursuiteFormattee());
			assertNull(tiers.getAdressePoursuiteAutreTiersFormattee());
		}

		// récupération des adresses de poursuite du conjoint
		{
			final GetTiers params = new GetTiers();
			params.setLogin(login);
			params.setTiersNumber(noTiersConjoint);
			params.setDate(null);
			params.getParts().add(TiersPart.ADRESSES);
			params.getParts().add(TiersPart.ADRESSES_ENVOI);

			final Tiers tiers = service.getTiers(params);
			assertNotNull(tiers);
			assertEquals(noTiersConjoint, tiers.getNumero());

			// Teste les adresses formattées
			final AdresseEnvoi domicile = tiers.getAdresseDomicileFormattee();
			assertAdresseEnvoi(domicile, "Madame", "Fabienne Girardet Ralet", "Route de Chailly 276", "1814 La Tour-de-Peilz");
			assertAdresseEquals(domicile, tiers.getAdresseEnvoi());
			assertAdresseEquals(domicile, tiers.getAdressePoursuiteFormattee());
			assertNull(tiers.getAdressePoursuiteAutreTiersFormattee());

		}
	}

	@Test
	public void testGetAdressesPoursuiteContribuableSousCuratelle() throws Exception {

		final long noTiers = 89016804;

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.setDate(null);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final AdresseEnvoi domicile = tiers.getAdresseDomicileFormattee();
		assertAdresseEnvoi(domicile, "Monsieur", "Marc Stäheli", "Rue de l'Industrie 19", "1030 Bussigny-Lausanne");
		assertAdresseEnvoi(tiers.getAdresseEnvoi(), "Monsieur", "Marc Stäheli", "p.a. Alain Bally", "Place Saint-François", "1003 Lausanne");
		assertAdresseEquals(domicile, tiers.getAdressePoursuiteFormattee());
		assertAdresseEnvoiAutreTiers(tiers.getAdressePoursuiteAutreTiersFormattee(), TypeAdressePoursuiteAutreTiers.CURATELLE, "Monsieur", "Alain Bally", "Place Saint-François", "1003 Lausanne");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableSousTutelle() throws Exception {

		final long noTiers = 60510843;

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.setDate(null);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final AdresseEnvoi domicile = tiers.getAdresseDomicileFormattee();
		assertAdresseEnvoi(domicile, "Madame", "Anabela Lopes", "Avenue Kiener 69", "1400 Yverdon-les-Bains");
		assertAdresseEnvoi(tiers.getAdresseEnvoi(), "Madame", "Anabela Lopes", "p.a. TUTEUR GENERAL VD", "Chemin de Mornex 32", "1014 Lausanne Adm cant");

		// devrait être ci-après mais l'info n'est pas à jour dans le host : assertAdresseEnvoi(tiers.getAdressePoursuiteFormattee(), "Justice de Paix des districts du Jura-Nord vaudois et du Gros-de-Vaud", "Case Postale 693", "Rue du Pré 2", "1400 Yverdon-les-Bains");
		assertAdresseEnvoi(tiers.getAdressePoursuiteFormattee(), "Monsieur le Juge de Paix de Belmont/Conc", "ise/Champvent/Grandson/Ste-Croix/Yverdon", "Rue du Lac", "1400 Yverdon-les-Bains");

		assertAdresseEnvoiAutreTiers(tiers.getAdressePoursuiteAutreTiersFormattee(), TypeAdressePoursuiteAutreTiers.TUTELLE, "Office du tuteur général", "du Canton de Vaud", "Chemin de Mornex 32",
				"1014 Lausanne Adm cant");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableHSAvecRepresentantConventionel() throws Exception {

		final long noTiers = 10536395;

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.setDate(null);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		assertAdresseEnvoi(tiers.getAdresseDomicileFormattee(), "Monsieur", "Claude-Alain Proz", "Izmir", "Turquie");
		assertAdresseEnvoi(tiers.getAdresseEnvoi(), "Monsieur", "Claude-Alain Proz", "p.a. KPMG AG (KPMG SA) (KPMG Ltd)", "Badenerstr. 172 - Postfach", "8026 Zürich 26 Aussersihl");
		assertAdresseEnvoi(tiers.getAdressePoursuiteFormattee(), "KPMG AG", "(KPMG SA)", "(KPMG Ltd)", "Badenerstr. 172 - Postfach", "8026 Zürich 26 Aussersihl");
		assertAdresseEnvoiAutreTiers(tiers.getAdressePoursuiteAutreTiersFormattee(), TypeAdressePoursuiteAutreTiers.MANDATAIRE, "KPMG AG", "(KPMG SA)", "(KPMG Ltd)", "Badenerstr. 172 - Postfach",
				"8026 Zürich 26 Aussersihl");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableVDAvecRepresentantConventionel() throws Exception {

		final long noTiers = 10033975;

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.setDate(null);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final AdresseEnvoi domicile = tiers.getAdresseDomicileFormattee();
		assertAdresseEnvoi(domicile, "Monsieur", "Marcello Pesci", "Ch. de Réchoz 17", "1027 Lonay");
		assertAdresseEnvoi(tiers.getAdresseEnvoi(), "Monsieur", "Marcello Pesci", "p.a. Curia Treuhand AG", "Postfach 132", "7000 Chur");
		assertAdresseEquals(domicile, tiers.getAdressePoursuiteFormattee());
		assertNull(tiers.getAdressePoursuiteAutreTiersFormattee());
	}

	@Test
	public void testGetAdressesPoursuiteContribuableAvecAdresseSpecifiquePoursuite() throws Exception {

		final long noTiers = 44018109;

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(noTiers);
		params.setDate(null);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumero());

		// Teste les adresses formattées
		final AdresseEnvoi domicile = tiers.getAdresseDomicileFormattee();
		assertAdresseEnvoi(domicile, "Monsieur", "Philippe Galley", "Ch. Sous le Bois", "1523 Granges-Marnand");
		assertAdresseEquals(domicile, tiers.getAdresseEnvoi());

		assertAdresseEnvoi(tiers.getAdressePoursuiteFormattee(), "Monsieur", "Philippe Galley", "Chemin de Praz-Berthoud", "1010 Lausanne");
		assertAdresseEnvoiAutreTiers(tiers.getAdressePoursuiteAutreTiersFormattee(), TypeAdressePoursuiteAutreTiers.SPECIFIQUE, "Monsieur", "Philippe Galley", "Chemin de Praz-Berthoud",
				"1010 Lausanne");
	}

	private static void assertAdresseEquals(AdresseEnvoi expected, AdresseEnvoi actual) {
		assertTrue((expected == null && actual == null) || (expected != null && actual != null));
		if (expected != null) {
			assertEquals(expected.getLigne1(), actual.getLigne1());
			assertEquals(expected.getLigne2(), actual.getLigne2());
			assertEquals(expected.getLigne3(), actual.getLigne3());
			assertEquals(expected.getLigne4(), actual.getLigne4());
			assertEquals(expected.getLigne5(), actual.getLigne5());
			assertEquals(expected.getLigne6(), actual.getLigne6());
		}
	}

	private static void assertAdresseEnvoiAutreTiers(AdresseEnvoiAutreTiers adresse, TypeAdressePoursuiteAutreTiers type, String... lignes) {
		assertEquals(type, adresse.getType());
		assertAdresseEnvoi(adresse, lignes);
	}

	private static void assertAdresseEnvoi(AdresseEnvoi adresse, String... lignes) {
		assertNotNull(adresse);
		assertTrue(lignes.length <= 6);

		if (lignes.length > 0) {
			assertEquals(lignes[0], trimValiPattern(adresse.getLigne1()));
			if (lignes.length > 1) {
				assertEquals(lignes[1], trimValiPattern(adresse.getLigne2()));
				if (lignes.length > 2) {
					assertEquals(lignes[2], trimValiPattern(adresse.getLigne3()));
					if (lignes.length > 3) {
						assertEquals(lignes[3], trimValiPattern(adresse.getLigne4()));
						if (lignes.length > 4) {
							assertEquals(lignes[4], trimValiPattern(adresse.getLigne5()));
							if (lignes.length > 5) {
								assertEquals(lignes[5], trimValiPattern(adresse.getLigne6()));
							}
							else {
								assertNull(adresse.getLigne6());
							}
						}
						else {
							assertNull(adresse.getLigne5());
						}
					}
					else {
						assertNull(adresse.getLigne4());
					}
				}
				else {
					assertNull(adresse.getLigne3());
				}
			}
			else {
				assertNull(adresse.getLigne2());
			}
		}
		else {
			assertNull(adresse.getLigne1());
		}
	}
}

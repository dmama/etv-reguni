package ch.vd.uniregctb.webservice.tiers3;

import java.util.List;

import org.junit.Test;

import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.v1.Party;

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

		final int noTiers = 44018108;

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(noTiers);
		params.getParts().add(PartyPart.ADDRESSES);

		final Party tiers = service.getParty(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumber());

		// Teste les adresses formattées
		final List<Address> residenceAddresses = tiers.getResidenceAddresses();
		final Address domicile = residenceAddresses.get(residenceAddresses.size() - 1);
		final FormattedAddress formattee = domicile.getFormattedAddress();
		assertFormattedAddress(formattee, "Monsieur", "Philippe Galley", "Chemin Sous le Bois 22", "1523 Granges-Marnand");
		assertEquals(TariffZone.SWITZERLAND, domicile.getAddressInformation().getTariffZone());

		final List<Address> mailAddresses = tiers.getMailAddresses();
		assertAdresseEquals(formattee, mailAddresses.get(mailAddresses.size() - 1).getFormattedAddress());

		final List<Address> debtProsecutionAddresses = tiers.getDebtProsecutionAddresses();
		assertAdresseEquals(formattee, debtProsecutionAddresses.get(debtProsecutionAddresses.size() - 1).getFormattedAddress());

		assertEmpty(tiers.getDebtProsecutionAddressesOfOtherParty());
	}

	@Test
	public void testGetAdressesPoursuiteContribuableCouple() throws Exception {

		final int noTiersMenage = 10069459;
		final int noTiersPrincipal;
		final int noTiersConjoint;

		// détermination des composants du ménage
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(noTiersMenage);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final CommonHousehold tiers = (CommonHousehold) service.getParty(params);
			assertNotNull(tiers);
			assertEquals(noTiersMenage, tiers.getNumber());

			noTiersPrincipal = tiers.getMainTaxpayer().getNumber();
			noTiersConjoint = tiers.getSecondaryTaxpayer().getNumber();
		}

		// récupération des adresses de poursuite du principal
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(noTiersPrincipal);
			params.getParts().add(PartyPart.ADDRESSES);

			final Party tiers = service.getParty(params);
			assertNotNull(tiers);
			assertEquals(noTiersPrincipal, tiers.getNumber());

			// Teste les adresses formattées
			final List<Address> residenceAddresses = tiers.getResidenceAddresses();
			final Address domicile = residenceAddresses.get(residenceAddresses.size() - 1);
			final FormattedAddress formattee = domicile.getFormattedAddress();
			assertFormattedAddress(formattee, "Monsieur", "Thierry Ralet", "Ch. des Fleurettes 6", "1860 Aigle");
			assertEquals(TariffZone.SWITZERLAND, domicile.getAddressInformation().getTariffZone());

			final List<Address> mailAddresses = tiers.getMailAddresses();
			assertFormattedAddress(mailAddresses.get(mailAddresses.size() - 1).getFormattedAddress(), "Monsieur", "Thierry Ralet", "Chemin des Fleurettes 6", "1860 Aigle");

			final List<Address> debtProsecutionAddresses = tiers.getDebtProsecutionAddresses();
			assertAdresseEquals(formattee, debtProsecutionAddresses.get(debtProsecutionAddresses.size() - 1).getFormattedAddress());
			assertEmpty(tiers.getDebtProsecutionAddressesOfOtherParty());
		}

		// récupération des adresses de poursuite du conjoint
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(noTiersConjoint);
			params.getParts().add(PartyPart.ADDRESSES);

			final Party tiers = service.getParty(params);
			assertNotNull(tiers);
			assertEquals(noTiersConjoint, tiers.getNumber());

			// Teste les adresses formattées
			final List<Address> residenceAddresses = tiers.getResidenceAddresses();
			final Address domicile = residenceAddresses.get(residenceAddresses.size() - 1);
			assertFormattedAddress(domicile.getFormattedAddress(), "Madame", "Fabienne Girardet Ralet", "Ch. des Fleurettes 6", "1860 Aigle");
			assertEquals(TariffZone.SWITZERLAND, domicile.getAddressInformation().getTariffZone());

			final List<Address> mailAddresses = tiers.getMailAddresses();
			final Address courrier = mailAddresses.get(mailAddresses.size() - 1);
			assertFormattedAddress(courrier.getFormattedAddress(), "Madame", "Fabienne Girardet Ralet", "Ch. des Fleurettes 6", "1860 Aigle");

			final List<Address> debtProsecutionAddresses = tiers.getDebtProsecutionAddresses();
			assertAdresseEquals(domicile.getFormattedAddress(), debtProsecutionAddresses.get(debtProsecutionAddresses.size() - 1).getFormattedAddress());

			assertEmpty(tiers.getDebtProsecutionAddressesOfOtherParty());

		}
	}

	@Test
	public void testGetAdressesPoursuiteContribuableSousCuratelle() throws Exception {

		final int noTiers = 89016804;

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(noTiers);
		params.getParts().add(PartyPart.ADDRESSES);

		final Party tiers = service.getParty(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumber());

		// Teste les adresses formattées
		final List<Address> residenceAddresses = tiers.getResidenceAddresses();
		final Address domicile = residenceAddresses.get(residenceAddresses.size() - 1);
		assertFormattedAddress(domicile.getFormattedAddress(), "Monsieur", "Marc Stäheli", "Chemin des Peupliers 1", "1008 Prilly");
		assertEquals(TariffZone.SWITZERLAND, domicile.getAddressInformation().getTariffZone());

		final List<Address> mailAddresses = tiers.getMailAddresses();
		assertFormattedAddress(mailAddresses.get(mailAddresses.size() - 1).getFormattedAddress(), "Monsieur", "Marc Stäheli", "p.a. Alain Bally", "Place Saint-François", "1003 Lausanne");

		final List<Address> debtProsecutionAddresses = tiers.getDebtProsecutionAddresses();
		assertAdresseEquals(domicile.getFormattedAddress(), debtProsecutionAddresses.get(debtProsecutionAddresses.size() - 1).getFormattedAddress());

		final List<AddressOtherParty> debtProsecutionAddressesOfOtherParty = tiers.getDebtProsecutionAddressesOfOtherParty();
		final AddressOtherParty debtProsecutionAddressOfOtherParty = debtProsecutionAddressesOfOtherParty.get(debtProsecutionAddressesOfOtherParty.size() - 1);
		assertEquals(OtherPartyAddressType.WELFARE_ADVOCATE, debtProsecutionAddressOfOtherParty.getOtherPartyType());
		assertFormattedAddress(debtProsecutionAddressOfOtherParty.getBase().getFormattedAddress(), "Monsieur", "Alain Bally", "Place Saint-François", "1003 Lausanne");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableSousTutelle() throws Exception {

		final int noTiers = 60510843;

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(noTiers);
		params.getParts().add(PartyPart.ADDRESSES);

		final Party tiers = service.getParty(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumber());

		// Teste les adresses formattées
		final List<Address> residenceAddresses = tiers.getResidenceAddresses();
		final Address domicile = residenceAddresses.get(residenceAddresses.size() - 1);
		assertFormattedAddress(domicile.getFormattedAddress(), "Madame", "Anabela Lopes", "Avenue Kiener 69", "1400 Yverdon-les-Bains");
		assertEquals(TariffZone.SWITZERLAND, domicile.getAddressInformation().getTariffZone());

		final List<Address> mailAddresses = tiers.getMailAddresses();
		assertFormattedAddress(mailAddresses.get(mailAddresses.size() - 1).getFormattedAddress(), "Madame", "Anabela Lopes", "p.a. TUTEUR GENERAL VD", "Chemin de Mornex 32", "1014 Lausanne Adm cant");

		// devrait être ci-après mais l'info n'est pas à jour dans le host : assertFormattedAddress(tiers.getAdressePoursuiteFormattee(), "Justice de Paix des districts du Jura-Nord vaudois et du Gros-de-Vaud", "Case Postale 693", "Rue du Pré 2", "1400 Yverdon-les-Bains");
		final List<Address> debtProsecutionAddresses = tiers.getDebtProsecutionAddresses();
		assertFormattedAddress(debtProsecutionAddresses.get(debtProsecutionAddresses.size() - 1).getFormattedAddress(), "Monsieur le Juge de Paix de Belmont/Conc",
				"ise/Champvent/Grandson/Ste-Croix/Yverdon", "Rue du Lac", "1400 Yverdon-les-Bains");

		final List<AddressOtherParty> debtProsecutionAddressesOfOtherParty = tiers.getDebtProsecutionAddressesOfOtherParty();
		final AddressOtherParty debtProsecutionAddressOfOtherParty = debtProsecutionAddressesOfOtherParty.get(debtProsecutionAddressesOfOtherParty.size() - 1);
		assertEquals(OtherPartyAddressType.GUARDIAN, debtProsecutionAddressOfOtherParty.getOtherPartyType());
		assertFormattedAddress(debtProsecutionAddressOfOtherParty.getBase().getFormattedAddress(), "Office du tuteur général", "du Canton de Vaud", "Chemin de Mornex 32", "1014 Lausanne Adm cant");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableHSAvecRepresentantConventionel() throws Exception {

		final int noTiers = 10536395;

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(noTiers);
		params.getParts().add(PartyPart.ADDRESSES);

		final Party tiers = service.getParty(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumber());

		// Teste les adresses formattées
		final List<Address> residenceAddresses = tiers.getResidenceAddresses();
		assertFormattedAddress(residenceAddresses.get(residenceAddresses.size() - 1).getFormattedAddress(), "Monsieur", "Claude-Alain Proz", "Izmir", "Turquie");

		final List<Address> mailAddresses = tiers.getMailAddresses();
		assertFormattedAddress(mailAddresses.get(mailAddresses.size() - 1).getFormattedAddress(), "Monsieur", "Claude-Alain Proz", "p.a. KPMG AG (KPMG SA) (KPMG Ltd)", "Badenerstr. 172 - Postfach",
				"8026 Zürich");

		final List<Address> debtProsecutionAddresses = tiers.getDebtProsecutionAddresses();
		assertFormattedAddress(debtProsecutionAddresses.get(debtProsecutionAddresses.size() - 1).getFormattedAddress(), "KPMG AG", "(KPMG SA)", "(KPMG Ltd)", "Badenerstr. 172 - Postfach",
				"8026 Zürich");

		final List<AddressOtherParty> debtProsecutionAddressesOfOtherParty = tiers.getDebtProsecutionAddressesOfOtherParty();
		final AddressOtherParty debtProsecutionAddressOfOtherParty = debtProsecutionAddressesOfOtherParty.get(debtProsecutionAddressesOfOtherParty.size() - 1);
		assertEquals(OtherPartyAddressType.REPRESENTATIVE, debtProsecutionAddressOfOtherParty.getOtherPartyType());
		assertFormattedAddress(debtProsecutionAddressOfOtherParty.getBase().getFormattedAddress(), "KPMG AG", "(KPMG SA)", "(KPMG Ltd)", "Badenerstr. 172 - Postfach", "8026 Zürich");
	}

	@Test
	public void testGetAdressesPoursuiteContribuableVDAvecRepresentantConventionel() throws Exception {

		final int noTiers = 10033975;

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(noTiers);
		params.getParts().add(PartyPart.ADDRESSES);

		final Party tiers = service.getParty(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumber());

		// Teste les adresses formattées
		final List<Address> residenceAddresses = tiers.getResidenceAddresses();
		final Address domicile = residenceAddresses.get(residenceAddresses.size() - 1);
		assertFormattedAddress(domicile.getFormattedAddress(), "Monsieur", "Marcello Pesci", "Ch. de Réchoz 17", "1027 Lonay");
		assertEquals(TariffZone.SWITZERLAND, domicile.getAddressInformation().getTariffZone());

		final List<Address> mailAddresses = tiers.getMailAddresses();
		assertFormattedAddress(mailAddresses.get(mailAddresses.size() - 1).getFormattedAddress(), "Monsieur", "Marcello Pesci", "p.a. Curia Treuhand AG", "Postfach 132", "7000 Chur");
		final List<Address> debtProsecutionAddresses = tiers.getDebtProsecutionAddresses();
		assertAdresseEquals(domicile.getFormattedAddress(), debtProsecutionAddresses.get(debtProsecutionAddresses.size() - 1).getFormattedAddress());
		assertEmpty(tiers.getDebtProsecutionAddressesOfOtherParty());
	}

	@Test
	public void testGetAdressesPoursuiteContribuableAvecAdresseSpecifiquePoursuite() throws Exception {

		final int noTiers = 44018109;

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(noTiers);
		params.getParts().add(PartyPart.ADDRESSES);

		final Party tiers = service.getParty(params);
		assertNotNull(tiers);
		assertEquals(noTiers, tiers.getNumber());

		// Teste les adresses formattées
		final List<Address> residenceAddresses = tiers.getResidenceAddresses();
		final Address domicile = residenceAddresses.get(residenceAddresses.size() - 1);
		assertFormattedAddress(domicile.getFormattedAddress(), "Monsieur", "Philippe Galley", "Chemin Sous le Bois 22", "1523 Granges-Marnand");
		assertEquals(TariffZone.SWITZERLAND, domicile.getAddressInformation().getTariffZone());

		final List<Address> mailAddresses = tiers.getMailAddresses();
		assertAdresseEquals(domicile.getFormattedAddress(), mailAddresses.get(mailAddresses.size() - 1).getFormattedAddress());

		final List<Address> debtProsecutionAddresses = tiers.getDebtProsecutionAddresses();
		assertFormattedAddress(debtProsecutionAddresses.get(debtProsecutionAddresses.size() - 1).getFormattedAddress(), "Monsieur", "Philippe Galley", "Chemin de Praz-Berthoud", "1010 Lausanne");

		final List<AddressOtherParty> debtProsecutionAddressesOfOtherParty = tiers.getDebtProsecutionAddressesOfOtherParty();
		final AddressOtherParty poursuiteAutreTiers = debtProsecutionAddressesOfOtherParty.get(debtProsecutionAddressesOfOtherParty.size() - 1);
		assertEquals(OtherPartyAddressType.SPECIFIC, poursuiteAutreTiers.getOtherPartyType());
		assertFormattedAddress(poursuiteAutreTiers.getBase().getFormattedAddress(), "Monsieur", "Philippe Galley", "Chemin de Praz-Berthoud", "1010 Lausanne");
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

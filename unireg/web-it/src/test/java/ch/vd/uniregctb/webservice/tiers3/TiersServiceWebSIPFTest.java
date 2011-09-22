package ch.vd.uniregctb.webservice.tiers3;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.GetPartyRequest;
import ch.vd.unireg.webservices.tiers3.PartyPart;
import ch.vd.unireg.webservices.tiers3.SearchCorporationEventsRequest;
import ch.vd.unireg.webservices.tiers3.SearchCorporationEventsResponse;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.OrganisationMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.corporation.v1.Capital;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.corporation.v1.CorporationEvent;
import ch.vd.unireg.xml.party.corporation.v1.CorporationStatus;
import ch.vd.unireg.xml.party.corporation.v1.LegalForm;
import ch.vd.unireg.xml.party.corporation.v1.LegalSeat;
import ch.vd.unireg.xml.party.corporation.v1.LegalSeatType;
import ch.vd.unireg.xml.party.corporation.v1.TaxSystem;
import ch.vd.unireg.xml.party.taxresidence.v1.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v1.SimplifiedTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.v1.AccountNumberFormat;
import ch.vd.unireg.xml.party.v1.BankAccount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Classe de test et d'exemple d'utilisation du web-service PM à l'usage de SIPF.
 * <p/>
 * Basé sur le document de spécification d'Eric Wyss "Echanges Registe - SIPF particularités des personnes morales" v0.96 du 10 octobre 2009.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class TiersServiceWebSIPFTest extends AbstractTiersServiceWebTest {

	// private static final Logger LOGGER = Logger.getLogger(WebitTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebSIPFTest.xml";

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
		login.setUserId("[UT] TiersServiceWebSIPFTest");
		login.setOid(22);
	}

	@Test
	public void testFournirForsPM() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(222);
		params.getParts().add(PartyPart.TAX_RESIDENCES);
		params.getParts().add(PartyPart.TAX_SYSTEMS);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumber());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getContactPerson()));
		assertEquals("KALESA", trimValiPattern(pm.getShortName()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getName1()));
		assertEquals("", trimValiPattern(pm.getName2()));
		assertEquals("en liquidation", trimValiPattern(pm.getName3()));

		// Récupération des informations des fors fiscaux

		final List<TaxResidence> forPrincipaux = pm.getMainTaxResidences();
		assertNotNull(forPrincipaux);
		assertEquals(1, forPrincipaux.size());

		final TaxResidence ffp0 = forPrincipaux.get(0);
		assertNotNull(ffp0);
		// Note : les communes hors-canton et les pays hors-Suisse sont aussi retourné. C'est à l'appelant de faire le tri si nécessaire.
		assertSameDay(newDate(1979, 8, 7), ffp0.getDateFrom());
		assertNull(ffp0.getDateTo());
		assertEquals(5413, ffp0.getTaxationAuthorityFSOId());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ffp0.getTaxationAuthorityType());
		assertEquals(TaxType.PROFITS_CAPITAL, ffp0.getTaxType());
		assertEquals(TaxLiabilityReason.RESIDENCE, ffp0.getTaxLiabilityReason());

		final List<TaxResidence> forSecondaires = pm.getOtherTaxResidences();
		assertNotNull(forSecondaires);
		assertEquals(1, forSecondaires.size());

		final TaxResidence ffs0 = forSecondaires.get(0);
		assertNotNull(ffs0);
		assertSameDay(newDate(1988, 7, 22), ffs0.getDateFrom());
		assertSameDay(newDate(2001, 9, 6), ffs0.getDateTo());
		assertEquals(5413, ffs0.getTaxationAuthorityFSOId());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ffs0.getTaxationAuthorityType());
		assertEquals(TaxType.PROFITS_CAPITAL, ffs0.getTaxType());
		assertEquals(TaxLiabilityReason.STABLE_ESTABLISHMENT, ffs0.getTaxLiabilityReason());

		// etc...

		// Récupération des informations des régimes fiscaux

		final List<TaxSystem> regimesICC = pm.getTaxSystemsVD();
		assertNotNull(regimesICC);
		assertEquals(1, regimesICC.size());

		final TaxSystem icc0 = regimesICC.get(0);
		assertNotNull(icc0);
		assertEquals("01", icc0.getCode()); // selon table TY_REGIME_FISCAL
		assertSameDay(newDate(1993, 1, 1), icc0.getDateFrom());
		assertNull(icc0.getDateTo());
		// note : la catégorie de PM se déduit du code

		final List<TaxSystem> regimesIFD = pm.getTaxSystemsCH();
		assertNotNull(regimesIFD);
		assertEquals(1, regimesIFD.size());

		final TaxSystem ifd0 = regimesIFD.get(0);
		assertNotNull(ifd0);
		assertEquals("01", ifd0.getCode()); // selon table TY_REGIME_FISCAL
		assertSameDay(newDate(1993, 1, 1), ifd0.getDateFrom());
		assertNull(ifd0.getDateTo());
	}

	@Test
	public void testFournirCapital() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(222);
		params.getParts().add(PartyPart.CAPITALS);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumber());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getContactPerson()));
		assertEquals("KALESA", trimValiPattern(pm.getShortName()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getName1()));
		assertEquals("", trimValiPattern(pm.getName2()));
		assertEquals("en liquidation", trimValiPattern(pm.getName3()));

		// Récupération du capital

		final List<Capital> capitaux = pm.getCapitals();
		assertNotNull(capitaux);
		assertEquals(1, capitaux.size());

		final Capital capital = capitaux.get(0);
		assertNotNull(capital);
		assertEquals(150000, capital.getPaidInCapital());
		assertEquals(150000, capital.getShareCapital());

		// note : il est de la responsabilité de l'appelant de déterminer si l'abscence ou non du capital libéré est normale ou non. Pour
		// rappel, cette abscence justifiée ou non se déduit de la catégorie de PM (= normal pour une APM, d'après le document d'Eric Wyss).
		// Cette catégorie est elle-même déduite du code du régime fiscal.
	}

	@Test
	public void testFournirEvenementsPMParNumero() throws Exception {

		final SearchCorporationEventsRequest params = new SearchCorporationEventsRequest();
		params.setLogin(login);
		params.setCorporationNumber(222);

		final SearchCorporationEventsResponse array = service.searchCorporationEvents(params);
		assertNotNull(array);

		final List<CorporationEvent> events = array.getEvents();
		assertNotNull(events);
		assertEquals(16, events.size());

		final CorporationEvent ev0 = events.get(0);
		assertNotNull(ev0);
		assertSameDay(newDate(1979, 10, 30), ev0.getDate());
		assertEquals("007", ev0.getCode()); // selon table EVENEMENT du host
		assertEquals(Integer.valueOf(222), ev0.getPartyNumber());

		final CorporationEvent ev1 = events.get(1);
		assertNotNull(ev1);
		assertSameDay(newDate(1979, 10, 30), ev1.getDate());
		assertEquals("026", ev1.getCode());
		assertEquals(Integer.valueOf(222), ev1.getPartyNumber());

		final CorporationEvent ev2 = events.get(2);
		assertNotNull(ev2);
		assertSameDay(newDate(1992, 1, 1), ev2.getDate());
		assertEquals("001", ev2.getCode());
		assertEquals(Integer.valueOf(222), ev2.getPartyNumber());

		final CorporationEvent ev3 = events.get(3);
		assertNotNull(ev3);
		assertSameDay(newDate(1992, 1, 1), ev3.getDate());
		assertEquals("001", ev3.getCode());
		assertEquals(Integer.valueOf(222), ev3.getPartyNumber());

		final CorporationEvent ev4 = events.get(4);
		assertNotNull(ev4);
		assertSameDay(newDate(1992, 11, 6), ev4.getDate());
		assertEquals("021", ev4.getCode());
		assertEquals(Integer.valueOf(222), ev4.getPartyNumber());

		final CorporationEvent ev5 = events.get(5);
		assertNotNull(ev5);
		assertSameDay(newDate(1996, 10, 24), ev5.getDate());
		assertEquals(Integer.valueOf(222), ev5.getPartyNumber());
		assertEquals("020", ev5.getCode());

		final CorporationEvent ev6 = events.get(6);
		assertNotNull(ev6);
		assertSameDay(newDate(1997, 7, 10), ev6.getDate());
		assertEquals(Integer.valueOf(222), ev6.getPartyNumber());
		assertEquals("008", ev6.getCode());

		final CorporationEvent ev7 = events.get(7);
		assertNotNull(ev7);
		assertSameDay(newDate(1997, 12, 1), ev7.getDate());
		assertEquals(Integer.valueOf(222), ev7.getPartyNumber());
		assertEquals("020", ev7.getCode());

		final CorporationEvent ev8 = events.get(8);
		assertNotNull(ev8);
		assertSameDay(newDate(2000, 1, 1), ev8.getDate());
		assertEquals(Integer.valueOf(222), ev8.getPartyNumber());
		assertEquals("020", ev8.getCode());

		final CorporationEvent ev9 = events.get(9);
		assertNotNull(ev9);
		assertSameDay(newDate(2001, 9, 6), ev9.getDate());
		assertEquals(Integer.valueOf(222), ev9.getPartyNumber());
		assertEquals("037", ev9.getCode());

		final CorporationEvent ev10 = events.get(10);
		assertNotNull(ev10);
		assertSameDay(newDate(2003, 4, 3), ev10.getDate());
		assertEquals(Integer.valueOf(222), ev10.getPartyNumber());
		assertEquals("003", ev10.getCode());

		final CorporationEvent ev11 = events.get(11);
		assertNotNull(ev11);
		assertSameDay(newDate(2003, 4, 3), ev11.getDate());
		assertEquals(Integer.valueOf(222), ev11.getPartyNumber());
		assertEquals("003", ev11.getCode());

		final CorporationEvent ev12 = events.get(12);
		assertNotNull(ev12);
		assertSameDay(newDate(2003, 4, 3), ev12.getDate());
		assertEquals(Integer.valueOf(222), ev12.getPartyNumber());
		assertEquals("016", ev12.getCode());

		final CorporationEvent ev13 = events.get(13);
		assertNotNull(ev13);
		assertSameDay(newDate(2003, 4, 3), ev13.getDate());
		assertEquals(Integer.valueOf(222), ev13.getPartyNumber());
		assertEquals("023", ev13.getCode());

		final CorporationEvent ev14 = events.get(14);
		assertNotNull(ev14);
		assertSameDay(newDate(2003, 11, 6), ev14.getDate());
		assertEquals(Integer.valueOf(222), ev14.getPartyNumber());
		assertEquals("002", ev14.getCode());

		final CorporationEvent ev15 = events.get(15);
		assertNotNull(ev15);
		assertSameDay(newDate(2003, 11, 6), ev15.getDate());
		assertEquals(Integer.valueOf(222), ev15.getPartyNumber());
		assertEquals("002", ev15.getCode());
	}

	@Test
	public void testFournirEvenementsPMParCode() throws Exception {

		final SearchCorporationEventsRequest params = new SearchCorporationEventsRequest();
		params.setLogin(login);
		params.setEventCode("001");
		params.setCorporationNumber(222); // pas obligatoire, juste pour limiter le nombre de résultats

		final SearchCorporationEventsResponse array = service.searchCorporationEvents(params);
		assertNotNull(array);

		final List<CorporationEvent> events = array.getEvents();
		assertNotNull(events);
		assertEquals(2, events.size());

		final CorporationEvent ev0 = events.get(1);
		assertNotNull(ev0);
		assertSameDay(newDate(1992, 1, 1), ev0.getDate());
		assertEquals("001", ev0.getCode());
		assertEquals(Integer.valueOf(222), ev0.getPartyNumber());

		final CorporationEvent ev1 = events.get(1);
		assertNotNull(ev1);
		assertSameDay(newDate(1992, 1, 1), ev1.getDate());
		assertEquals("001", ev1.getCode());
		assertEquals(Integer.valueOf(222), ev1.getPartyNumber());
	}

	/**
	 * [UNIREG-2039] vérifie que les paramètres date mini et date maxi fonctionnent correctement.
	 */
	@Test
	public void testFournirEvenementsPMDateMiniMaxi() throws Exception {

		final SearchCorporationEventsRequest params = new SearchCorporationEventsRequest();
		params.setLogin(login);
		params.setStartDate(newDate(2000, 1, 1));
		params.setEndDate(newDate(2003, 7, 1));
		params.setCorporationNumber(222);

		final SearchCorporationEventsResponse array = service.searchCorporationEvents(params);
		assertNotNull(array);

		final List<CorporationEvent> events = array.getEvents();
		assertNotNull(events);
		assertEquals(6, events.size());

		final CorporationEvent ev0 = events.get(0);
		assertNotNull(ev0);
		assertSameDay(newDate(2000, 1, 1), ev0.getDate());
		assertEquals("020", ev0.getCode());
		assertEquals(Integer.valueOf(222), ev0.getPartyNumber());

		final CorporationEvent ev1 = events.get(1);
		assertNotNull(ev1);
		assertSameDay(newDate(2001, 9, 6), ev1.getDate());
		assertEquals("037", ev1.getCode());
		assertEquals(Integer.valueOf(222), ev1.getPartyNumber());

		final CorporationEvent ev2 = events.get(2);
		assertNotNull(ev2);
		assertSameDay(newDate(2003, 4, 3), ev2.getDate());
		assertEquals("003", ev2.getCode());
		assertEquals(Integer.valueOf(222), ev2.getPartyNumber());

		final CorporationEvent ev3 = events.get(3);
		assertNotNull(ev3);
		assertSameDay(newDate(2003, 4, 3), ev3.getDate());
		assertEquals("003", ev3.getCode());
		assertEquals(Integer.valueOf(222), ev3.getPartyNumber());

		final CorporationEvent ev4 = events.get(4);
		assertNotNull(ev4);
		assertSameDay(newDate(2003, 4, 3), ev4.getDate());
		assertEquals("016", ev4.getCode());
		assertEquals(Integer.valueOf(222), ev4.getPartyNumber());

		final CorporationEvent ev5 = events.get(5);
		assertNotNull(ev5);
		assertSameDay(newDate(2003, 4, 3), ev5.getDate());
		assertEquals("023", ev5.getCode());
		assertEquals(Integer.valueOf(222), ev5.getPartyNumber());
	}

	@Test
	public void testFournirCoordonneesFinancieres() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(222);
		params.getParts().add(PartyPart.BANK_ACCOUNTS);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumber());

		final List<BankAccount> comptes = pm.getBankAccounts();
		assertNotNull(comptes);
		assertEquals(1, comptes.size());

		// Récupération du compte bancaire de la PM

		final BankAccount comptePM = comptes.get(0);
		assertNotNull(comptePM);
		assertEquals("18-25277-7", comptePM.getAccountNumber());
		assertNull(comptePM.getClearing());
		assertNull(comptePM.getBicAddress());
		assertEquals(AccountNumberFormat.SWISS_SPECIFIC, comptePM.getFormat());
		assertNull(comptePM.getOwnerName());
		assertEquals("La Poste Suisse", comptePM.getBankName());
	}

	@Ignore // exemple fictif
	@Test
	public void testFournirCoordonneesFinancieresAvecMandataire() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(123456);
		params.getParts().add(PartyPart.BANK_ACCOUNTS);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(123456L, pm.getNumber());

		final List<BankAccount> comptes = pm.getBankAccounts();
		assertNotNull(comptes);
		assertEquals(2, comptes.size());

		final BankAccount compte0 = comptes.get(0);
		final BankAccount compte1 = comptes.get(0);

		// Récupération du compte bancaire de la PM

		final BankAccount comptePM = (compte0.getOwnerPartyNumber() == pm.getNumber() ? compte0 : compte1);
		assertNotNull(comptePM);
		assertEquals("<un numéro IBAN>", comptePM.getAccountNumber());
		assertEquals("<un numéro de clearing>", comptePM.getClearing());
		assertNull(comptePM.getBicAddress());
		assertEquals(AccountNumberFormat.IBAN, comptePM.getFormat());
		assertEquals("Ma petite entreprise", comptePM.getOwnerName());
		assertEquals("Banque de la place", comptePM.getBankName());

		// Récupération du compte bancaire du mandataire

		final BankAccount compteMandataire = (compte0.getOwnerPartyNumber() == pm.getNumber() ? compte1 : compte0);
		assertNotNull(compteMandataire);
		assertEquals("<un numéro IBAN>", compteMandataire.getAccountNumber());
		assertEquals("<un numéro de clearing>", compteMandataire.getClearing());
		assertNull(compteMandataire.getBicAddress());
		assertEquals(AccountNumberFormat.IBAN, compteMandataire.getFormat());
		assertEquals("Maître George-Edouard Dubeauchapeau", compteMandataire.getOwnerName());
		assertEquals("Banque privée Piquet S.A.", compteMandataire.getBankName());
	}

	/**
	 * [UNIREG-2106] teste que les coordonnées fiscales d'un mandataire de type 'T' sont bien exposées
	 */
	@Test
	public void testFournirCoordonneesFinancieresAvecMandatairePM() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(32592);
		params.getParts().add(PartyPart.BANK_ACCOUNTS);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(32592L, pm.getNumber());

		final List<BankAccount> comptes = pm.getBankAccounts();
		assertNotNull(comptes);
		assertEquals(1, comptes.size());

		final BankAccount compteMandataire = comptes.get(0);
		assertNotNull(compteMandataire);
		assertEquals(426, compteMandataire.getOwnerPartyNumber());
		assertEquals("230-575.013.03", compteMandataire.getAccountNumber());
		assertNull(compteMandataire.getClearing());
		assertNull(compteMandataire.getBicAddress());
		assertEquals(AccountNumberFormat.SWISS_SPECIFIC, compteMandataire.getFormat());
		assertEquals("Deloitte AG", trimValiPattern(compteMandataire.getOwnerName()));
		assertEquals("UBS AG", trimValiPattern(compteMandataire.getBankName()));
	}

	@Test
	public void testFournirAdresseCourrier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(222);
		params.getParts().add(PartyPart.ADDRESSES);


		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumber());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getContactPerson()));
		assertEquals("KALESA", trimValiPattern(pm.getShortName()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getName1()));
		assertEquals("", trimValiPattern(pm.getName2()));
		assertEquals("en liquidation", trimValiPattern(pm.getName3()));

		final List<Address> adressesCourrier = pm.getMailAddresses();
		assertNotNull(adressesCourrier);
		assertEquals(1, adressesCourrier.size());

		final Address addressCourrier = adressesCourrier.get(0);
		assertNotNull(addressCourrier);
		assertSameDay(newDate(2003, 4, 3), addressCourrier.getDateFrom());
		assertNull(addressCourrier.getDateTo());

		final AddressInformation info = addressCourrier.getAddressInformation();
		assertNotNull(info);
		assertEquals("p.a. Office des faillites", info.getComplementaryInformation());
		assertNull(info.getPostOfficeBoxNumber());
		assertNull(info.getPostOfficeBoxText());
		assertNull(info.getDwellingNumber());
		assertNull(info.getStreet());
		assertNull(info.getHouseNumber());
		assertEquals(Long.valueOf(1860), info.getSwissZipCode());
		assertEquals("Aigle", info.getTown());
		assertNull(info.getCountry());
		assertEquals(Integer.valueOf(1100), info.getSwissZipCodeId());
		assertNull(info.getStreetId());
		assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());

		// Récupération de l'adresse d'envoi de la PM

		final FormattedAddress adresseEnvoi = addressCourrier.getFormattedAddress();
		assertNotNull(adresseEnvoi);
		assertEquals("Kalesa S.A.", trimValiPattern(adresseEnvoi.getLine1()));
		assertEquals("", trimValiPattern(adresseEnvoi.getLine2()));
		assertEquals("en liquidation", trimValiPattern(adresseEnvoi.getLine3()));
		assertEquals("p.a. Office des faillites", trimValiPattern(adresseEnvoi.getLine4()));
		assertEquals("1860 Aigle", trimValiPattern(adresseEnvoi.getLine5()));
		assertNull(adresseEnvoi.getLine6());
	}

	/**
	 * [UNIREG-1974]
	 */
	@Test
	public void testFournirAdresseCourrierLocaliteAbregee() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1314);
		params.getParts().add(PartyPart.ADDRESSES);


		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(1314L, pm.getNumber());
		assertEquals("R. Borgo", trimValiPattern(pm.getContactPerson()));
		assertEquals("JAL HOLDING", trimValiPattern(pm.getShortName()));
		assertEquals("Jal holding S.A.", trimValiPattern(pm.getName1()));
		assertEquals("", trimValiPattern(pm.getName2()));
		assertEquals("en liquidation", trimValiPattern(pm.getName3()));

		final List<Address> adressesCourrier = pm.getMailAddresses();
		assertNotNull(adressesCourrier);
		assertEquals(1, adressesCourrier.size());

		final Address addressCourrier = adressesCourrier.get(0);
		assertNotNull(addressCourrier);
		assertSameDay(newDate(2007, 6, 11), addressCourrier.getDateFrom());
		assertNull(addressCourrier.getDateTo());

		final AddressInformation info = addressCourrier.getAddressInformation();
		assertNotNull(info);
		assertEquals("pa Fidu. Commerce & Industrie", info.getComplementaryInformation());
		assertNull(info.getPostOfficeBoxNumber());
		assertNull(info.getPostOfficeBoxText());
		assertNull(info.getDwellingNumber());
		assertEquals("Avenue de la Gare", info.getStreet());
		assertEquals("10", info.getHouseNumber());
		assertEquals(Long.valueOf(1003), info.getSwissZipCode());
		assertEquals("Lausanne", info.getTown());
		assertNull(info.getCountry());
		assertEquals(Integer.valueOf(150), info.getSwissZipCodeId());
		assertEquals(Integer.valueOf(30317), info.getStreetId());
		assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());

		// Récupération de l'adresse d'envoi de la PM

		final FormattedAddress adresseEnvoi = addressCourrier.getFormattedAddress();
		assertNotNull(adresseEnvoi);
		assertEquals("Jal holding S.A.", trimValiPattern(adresseEnvoi.getLine1()));
		assertEquals("", trimValiPattern(adresseEnvoi.getLine2()));
		assertEquals("en liquidation", trimValiPattern(adresseEnvoi.getLine3()));
		assertEquals("pa Fidu. Commerce & Industrie", trimValiPattern(adresseEnvoi.getLine4()));
		assertEquals("Avenue de la Gare 10", trimValiPattern(adresseEnvoi.getLine5()));
		assertEquals("1003 Lausanne", trimValiPattern(adresseEnvoi.getLine6()));
	}

	/**
	 * [UNIREG-1973] la personne de contact de la PM ne doit pas apparaître dans l'adresse d'envoi.
	 */
	@Test
	public void testFournirAdresseEnvoi() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(25000);
		params.getParts().add(PartyPart.ADDRESSES);


		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);

		// Récupération de l'adresse d'envoi de la PM

		final List<Address> courriers = pm.getMailAddresses();
		final Address courrier = courriers.get(courriers.size() - 1);
		final FormattedAddress adresseEnvoi = courrier.getFormattedAddress();
		assertNotNull(adresseEnvoi);
		assertEquals("Fonds prévoyance en fa", trimValiPattern(adresseEnvoi.getLine1()));
		assertEquals("personnel Sté électriq", trimValiPattern(adresseEnvoi.getLine2()));
		assertEquals("intercommunale de la C", trimValiPattern(adresseEnvoi.getLine3()));
		assertEquals("Rte des Avouillons 2 / CP 321", trimValiPattern(adresseEnvoi.getLine4()));
		assertEquals("1196 Gland", trimValiPattern(adresseEnvoi.getLine5()));
		assertNull(adresseEnvoi.getLine6());
		assertEquals(TariffZone.SWITZERLAND, courrier.getAddressInformation().getTariffZone());
	}

	@Test
	public void testFournirAdresseContentieuxPM() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(37); // passé de la PM 222 à la PM 37 parce quelqu'un s'est amusé à entrer des valeurs bidon en développement...
		params.getParts().add(PartyPart.ADDRESSES);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(37L, pm.getNumber());
		assertEquals("Fiduciaire Pierre Terrier", trimValiPattern(pm.getContactPerson()));
		assertEquals("FIBER SEAL ROMANDIE", trimValiPattern(pm.getShortName()));
		assertEquals("Fiber Seal (Romandie)", trimValiPattern(pm.getName1()));
		assertEquals("", trimValiPattern(pm.getName2()));
		assertEquals("en liquidation", trimValiPattern(pm.getName3()));

		// Récupération des adresses de domicile (pour le contentieux)

		final List<Address> adressesDomicile = pm.getResidenceAddresses();
		assertNotNull(adressesDomicile);
		assertEquals(1, adressesDomicile.size());

		final Address addressDomicile = adressesDomicile.get(0);
		assertNotNull(addressDomicile);
		assertSameDay(newDate(1996, 4, 11), addressDomicile.getDateFrom());
		assertNull(addressDomicile.getDateTo());

		final AddressInformation infoDomicile = addressDomicile.getAddressInformation();
		assertNotNull(infoDomicile);
		assertNull(infoDomicile.getComplementaryInformation());
		assertNull(infoDomicile.getPostOfficeBoxNumber());
		assertNull(infoDomicile.getPostOfficeBoxText());
		assertNull(infoDomicile.getDwellingNumber());
		assertEquals("Quai du Seujet", infoDomicile.getStreet());
		assertEquals("28A", infoDomicile.getHouseNumber());
		assertEquals(Long.valueOf(1201), infoDomicile.getSwissZipCode());
		assertEquals("Genève", infoDomicile.getTown());
		assertNull(infoDomicile.getCountry());
		assertEquals(Integer.valueOf(367), infoDomicile.getSwissZipCodeId());
		assertEquals(Integer.valueOf(46421), infoDomicile.getStreetId());
		assertEquals(TariffZone.SWITZERLAND, infoDomicile.getTariffZone());

		final FormattedAddress adresseDomicileFormattee = addressDomicile.getFormattedAddress();
		assertNotNull(adresseDomicileFormattee);
		assertEquals("Fiber Seal (Romandie)", trimValiPattern(adresseDomicileFormattee.getLine1()));
		assertEquals("", trimValiPattern(adresseDomicileFormattee.getLine2()));
		assertEquals("en liquidation", trimValiPattern(adresseDomicileFormattee.getLine3()));
		assertEquals("Quai du Seujet 28A", trimValiPattern(adresseDomicileFormattee.getLine4()));
		assertEquals("1201 Genève", trimValiPattern(adresseDomicileFormattee.getLine5()));
		assertNull(adresseDomicileFormattee.getLine6());

		// Récupération des adresses de poursuite

		final List<Address> adressesPoursuite = pm.getDebtProsecutionAddresses();
		assertNotNull(adressesPoursuite);
		assertEquals(1, adressesPoursuite.size());

		final Address addressPoursuite = adressesPoursuite.get(0);
		assertNotNull(addressPoursuite);
		assertSameDay(newDate(1996, 4, 11), addressPoursuite.getDateFrom());
		assertNull(addressPoursuite.getDateTo());
		
		final AddressInformation infoPoursuite = addressPoursuite.getAddressInformation();
		assertNotNull(infoPoursuite);
		assertNull(infoPoursuite.getComplementaryInformation());
		assertNull(infoPoursuite.getPostOfficeBoxText());
		assertNull(infoPoursuite.getPostOfficeBoxNumber());
		assertNull(infoPoursuite.getDwellingNumber());
		assertEquals("Quai du Seujet", infoPoursuite.getStreet());
		assertEquals("28A", infoPoursuite.getHouseNumber());
		assertEquals(Long.valueOf(1201), infoPoursuite.getSwissZipCode());
		assertEquals("Genève", infoPoursuite.getTown());
		assertNull(infoPoursuite.getCountry());
		assertEquals(Integer.valueOf(367), infoPoursuite.getSwissZipCodeId());
		assertEquals(Integer.valueOf(46421), infoPoursuite.getStreetId());
		assertEquals(TariffZone.SWITZERLAND, infoPoursuite.getTariffZone());

		final FormattedAddress adressePoursuiteFormattee = addressPoursuite.getFormattedAddress();
		assertNotNull(adressePoursuiteFormattee);
		assertEquals("Fiber Seal (Romandie)", trimValiPattern(adressePoursuiteFormattee.getLine1()));
		assertEquals("", trimValiPattern(adressePoursuiteFormattee.getLine2()));
		assertEquals("en liquidation", trimValiPattern(adressePoursuiteFormattee.getLine3()));
		assertEquals("Quai du Seujet 28A", trimValiPattern(adressePoursuiteFormattee.getLine4()));
		assertEquals("1201 Genève", trimValiPattern(adressePoursuiteFormattee.getLine5()));
		assertNull(adressePoursuiteFormattee.getLine6());

		// Unireg n'est pas en mesure de déterminer l'adresse de l'OP. Ce travail est de la responsabilité du service infastructure.

		// Unireg n'est pas en mesure de retourner le nom et de prénom de l'administrateur, cette information n'est pas disponible.
	}

	@Test
	public void testFournirInformationDeConsultation() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(222);
		params.getParts().add(PartyPart.LEGAL_SEATS);
		params.getParts().add(PartyPart.TAX_RESIDENCES);
		params.getParts().add(PartyPart.LEGAL_FORMS);
		params.getParts().add(PartyPart.TAX_SYSTEMS);
		params.getParts().add(PartyPart.SIMPLIFIED_TAX_LIABILITIES);
		params.getParts().add(PartyPart.CORPORATION_STATUSES);
		params.getParts().add(PartyPart.CAPITALS);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumber());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getContactPerson()));
		assertEquals("KALESA", trimValiPattern(pm.getShortName()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getName1()));
		assertEquals("", trimValiPattern(pm.getName2()));
		assertEquals("en liquidation", trimValiPattern(pm.getName3()));

		// Sièges
		final List<LegalSeat> sieges = pm.getLegalSeats();
		assertNotNull(sieges);
		assertEquals(1, sieges.size());

		final LegalSeat siege = sieges.get(0);
		assertNotNull(siege);
		assertSameDay(newDate(1979, 8, 7), siege.getDateFrom());
		assertNull(siege.getDateTo());
		assertEquals(LegalSeatType.SWISS_MUNICIPALITY, siege.getType());
		assertEquals(5413, siege.getFsoId());

		// For principal actif
		final List<TaxResidence> forsFiscauxPrincipaux = pm.getMainTaxResidences();
		assertNotNull(forsFiscauxPrincipaux);
		assertEquals(1, forsFiscauxPrincipaux.size());

		final TaxResidence ffp = forsFiscauxPrincipaux.get(0);
		assertNull(ffp.getDateTo());
		assertEquals(5413, ffp.getTaxationAuthorityFSOId());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ffp.getTaxationAuthorityType());
		assertEquals(TaxType.PROFITS_CAPITAL, ffp.getTaxType());
		assertEquals(TaxLiabilityReason.RESIDENCE, ffp.getTaxLiabilityReason());
		// note : le nom de la commune/pays doit être demandé au service infrastructure

		// Fors secondaires actifs
		final List<TaxResidence> forSecondaires = pm.getOtherTaxResidences();
		assertNotNull(forSecondaires);
		assertEquals(1, forSecondaires.size());

		final TaxResidence ffs0 = forSecondaires.get(0);
		assertNotNull(ffs0);
		assertSameDay(newDate(1988, 7, 22), ffs0.getDateFrom());
		assertSameDay(newDate(2001, 9, 6), ffs0.getDateTo());
		assertEquals(5413, ffs0.getTaxationAuthorityFSOId());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ffs0.getTaxationAuthorityType());
		assertEquals(TaxType.PROFITS_CAPITAL, ffs0.getTaxType());
		assertEquals(TaxLiabilityReason.STABLE_ESTABLISHMENT, ffs0.getTaxLiabilityReason());
		// note : le nom de la commune doit être demandé au service infrastructure

		// Forme juridique
		final List<LegalForm> formesJuridiques = pm.getLegalForms();
		assertNotNull(formesJuridiques);
		assertEquals(1, formesJuridiques.size());

		final LegalForm legalForm = formesJuridiques.get(0);
		assertNotNull(legalForm);
		assertSameDay(newDate(1979, 8, 7), legalForm.getDateFrom());
		assertNull(legalForm.getDateTo());
		assertEquals("S.A.", legalForm.getCode()); // code selon table FORME_JURIDIQ_ACI

		// Régime fiscal ICC
		final List<TaxSystem> regimesFiscauxICC = pm.getTaxSystemsVD();
		assertNotNull(regimesFiscauxICC);
		assertEquals(1, regimesFiscauxICC.size());

		final TaxSystem regimeICC = regimesFiscauxICC.get(0);
		assertNotNull(regimeICC);
		assertSameDay(newDate(1993, 1, 1), regimeICC.getDateFrom());
		assertNull(regimeICC.getDateTo());
		assertEquals("01", regimeICC.getCode()); // selon table TY_REGIME_FISCAL

		// Date de fin du dernier exercice commercial
		assertNull(pm.getEndDateOfLastBusinessYear());

		// Date de bouclement future
		assertSameDay(newDate(2003, 12, 31), pm.getEndDateOfNextBusinessYear());

		// Dates de début et de fin de l'assujettissement LIC
		final List<SimplifiedTaxLiability> periodesAssujettissementLIC = pm.getSimplifiedTaxLiabilityVD();
		assertNotNull(periodesAssujettissementLIC);
		assertEquals(1, periodesAssujettissementLIC.size());

		final SimplifiedTaxLiability assujettissementLIC = periodesAssujettissementLIC.get(0);
		assertNotNull(assujettissementLIC);
		assertEquals(newDate(1992, 12, 31), assujettissementLIC.getDateFrom());
		assertEquals(newDate(2003, 12, 31), assujettissementLIC.getDateTo());
		assertEquals(SimplifiedTaxLiabilityType.UNLIMITED, assujettissementLIC.getType());

		// Dates de début et de fin de l'assujettissement LIFD
		final List<SimplifiedTaxLiability> periodesAssujettissementLIFD = pm.getSimplifiedTaxLiabilityCH();
		assertNotNull(periodesAssujettissementLIFD);
		assertEquals(1, periodesAssujettissementLIFD.size());

		final SimplifiedTaxLiability assujettissementLIFD = periodesAssujettissementLIFD.get(0);
		assertNotNull(assujettissementLIFD);
		assertEquals(newDate(1992, 12, 31), assujettissementLIFD.getDateFrom());
		assertEquals(newDate(2003, 12, 31), assujettissementLIFD.getDateTo());
		assertEquals(SimplifiedTaxLiabilityType.UNLIMITED, assujettissementLIFD.getType());

		// Numéro IPMRO
		assertEquals("01880", pm.getIpmroNumber());

		// Code blocage remboursement automatique
		assertTrue(pm.isAutomaticReimbursementBlocked());

		// Date de validite et code de l'état de la PM
		final List<CorporationStatus> etats = pm.getStatuses();
		assertNotNull(etats);
		assertEquals(3, etats.size());

		final CorporationStatus etat0 = etats.get(0);
		assertNotNull(etat0);
		assertSameDay(newDate(1979, 8, 7), etat0.getDateFrom());
		assertSameDay(newDate(2003, 4, 2), etat0.getDateTo());
		assertEquals("01", etat0.getCode()); // selon table ETAT du host

		final CorporationStatus etat1 = etats.get(1);
		assertNotNull(etat1);
		assertSameDay(newDate(2003, 4, 3), etat1.getDateFrom());
		assertSameDay(newDate(2003, 11, 5), etat1.getDateTo());
		assertEquals("04", etat1.getCode()); // selon table ETAT du host

		final CorporationStatus etat2 = etats.get(2);
		assertNotNull(etat2);
		assertSameDay(newDate(2003, 11, 6), etat2.getDateFrom());
		assertNull(etat2.getDateTo());
		assertEquals("06", etat2.getCode()); // selon table ETAT du host
		// note : le libellé des états doit être demandé au service infrastructure

		// Capital libéré + absence normale ou non
		final List<Capital> capitaux = pm.getCapitals();
		assertNotNull(capitaux);
		assertEquals(1, capitaux.size());

		final Capital capital = capitaux.get(0);
		assertNotNull(capital);
		assertEquals(150000, capital.getShareCapital());
		assertEquals(150000, capital.getPaidInCapital());
		assertFalse(capital.isAbsentPaidInCapitalNormal());
	}

	@Test
	public void testFournirAssujettissements() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(222);
		params.getParts().add(PartyPart.SIMPLIFIED_TAX_LIABILITIES);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumber());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getContactPerson()));
		assertEquals("KALESA", trimValiPattern(pm.getShortName()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getName1()));
		assertEquals("", trimValiPattern(pm.getName2()));
		assertEquals("en liquidation", trimValiPattern(pm.getName3()));

		final List<SimplifiedTaxLiability> lic = pm.getSimplifiedTaxLiabilityVD();
		assertEquals(1, lic.size());

		final SimplifiedTaxLiability lic0 = lic.get(0);
		assertNotNull(lic0);
		assertSameDay(newDate(1992, 12, 31), lic0.getDateFrom());
		assertSameDay(newDate(2003, 12, 31), lic0.getDateTo());
		assertEquals(SimplifiedTaxLiabilityType.UNLIMITED, lic0.getType());

		final List<SimplifiedTaxLiability> lifd = pm.getSimplifiedTaxLiabilityCH();
		assertEquals(1, lifd.size());

		final SimplifiedTaxLiability lifd0 = lifd.get(0);
		assertNotNull(lifd0);
		assertSameDay(newDate(1992, 12, 31), lifd0.getDateFrom());
		assertSameDay(newDate(2003, 12, 31), lifd0.getDateTo());
		assertEquals(SimplifiedTaxLiabilityType.UNLIMITED, lifd0.getType());
	}

	/**
	 * [UNIREG-1974] Vérifie que l'adresse de la fiduciaire Jal Holding utilise bien les trois lignes de la raison sociale et non pas la raison sociale abbrégée.
	 */
	@Test
	public void testGetAdresseEnvoiPM() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1314); // Jal Holding
		params.getParts().add(PartyPart.ADDRESSES);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);
		assertEquals(1314L, pm.getNumber());

		{
			final List<Address> adresses = pm.getMailAddresses();
			final Address adresse = adresses.get(adresses.size() - 1);
			final FormattedAddress adresseFormatteee = adresse.getFormattedAddress();
			assertNotNull(adresseFormatteee);
			assertEquals("Jal holding S.A.", trimValiPattern(adresseFormatteee.getLine1())); // <-- raison sociale ligne 1
			assertEquals("", trimValiPattern(adresseFormatteee.getLine2())); // <-- raison sociale ligne 2
			assertEquals("en liquidation", trimValiPattern(adresseFormatteee.getLine3())); // <-- raison sociale ligne 3
			assertEquals("pa Fidu. Commerce & Industrie", adresseFormatteee.getLine4());
			assertEquals("Avenue de la Gare 10", adresseFormatteee.getLine5());
			assertEquals("1003 Lausanne", adresseFormatteee.getLine6());

			final OrganisationMailAddressInfo destinataire = adresse.getOrganisation();
			assertNotNull(destinataire);
			assertEquals("Madame, Monsieur", destinataire.getFormalGreeting());
			assertEquals("Jal holding S.A.", trimValiPattern(destinataire.getOrganisationName()));
			assertEquals("", trimValiPattern(destinataire.getOrganisationNameAddOn1()));
			assertEquals("en liquidation", trimValiPattern(destinataire.getOrganisationNameAddOn2()));

			final AddressInformation destination = adresse.getAddressInformation();
			assertEquals(TariffZone.SWITZERLAND, destination.getTariffZone());
			assertEquals("pa Fidu. Commerce & Industrie", destination.getComplementaryInformation());
			assertNull(destination.getCareOf());
			assertEquals("Avenue de la Gare", destination.getStreet());
			assertEquals("10", destination.getHouseNumber());
			assertNull(destination.getPostOfficeBoxNumber());
			assertEquals(Long.valueOf(1003), destination.getSwissZipCode());
			assertEquals("Lausanne", destination.getTown());
			assertNull(destination.getCountry());
		}

		{
			final List<Address> adresses = pm.getResidenceAddresses();
			final Address adresse = adresses.get(adresses.size() - 1);
			final FormattedAddress adresseFormattee = adresse.getFormattedAddress();
			assertNotNull(adresseFormattee);
			assertEquals("Jal holding S.A.", trimValiPattern(adresseFormattee.getLine1())); // <-- raison sociale ligne 1
			assertEquals("", trimValiPattern(adresseFormattee.getLine2())); // <-- raison sociale ligne 2
			assertEquals("en liquidation", trimValiPattern(adresseFormattee.getLine3())); // <-- raison sociale ligne 3
			assertEquals("Fid.Commerce & Industrie S.A.", adresseFormattee.getLine4());
			assertEquals("Chemin Messidor 5", adresseFormattee.getLine5());
			assertEquals("1006 Lausanne", adresseFormattee.getLine6());

			final OrganisationMailAddressInfo destinataire = adresse.getOrganisation();
			assertNotNull(destinataire);
			assertEquals("Madame, Monsieur", destinataire.getFormalGreeting());
			assertEquals("Jal holding S.A.", trimValiPattern(destinataire.getOrganisationName()));
			assertEquals("", trimValiPattern(destinataire.getOrganisationNameAddOn1()));
			assertEquals("en liquidation", trimValiPattern(destinataire.getOrganisationNameAddOn2()));

			final AddressInformation destination = adresse.getAddressInformation();
			assertEquals(TariffZone.SWITZERLAND, destination.getTariffZone());
			assertEquals("Fid.Commerce & Industrie S.A.", destination.getComplementaryInformation());
			assertNull(destination.getCareOf());
			assertEquals("Chemin Messidor", destination.getStreet());
			assertEquals("5", destination.getHouseNumber());
			assertNull(destination.getPostOfficeBoxNumber());
			assertEquals(Long.valueOf(1006), destination.getSwissZipCode());
			assertEquals("Lausanne", destination.getTown());
			assertNull(destination.getCountry());
		}
	}
}

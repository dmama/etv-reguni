package ch.vd.uniregctb.webservice.party3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.BatchPartyEntry;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyTypeRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.SearchMode;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.SearchPartyResponse;
import ch.vd.unireg.webservices.party3.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.debtor.v1.CommunicationMode;
import ch.vd.unireg.xml.party.debtor.v1.Debtor;
import ch.vd.unireg.xml.party.debtor.v1.DebtorCategory;
import ch.vd.unireg.xml.party.debtor.v1.DebtorPeriodicity;
import ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity;
import ch.vd.unireg.xml.party.immovableproperty.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.immovableproperty.v1.OwnershipType;
import ch.vd.unireg.xml.party.immovableproperty.v1.PropertyShare;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.taxdeclaration.v1.DocumentType;
import ch.vd.unireg.xml.party.taxdeclaration.v1.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v1.WithholdingTaxDeclaration;
import ch.vd.unireg.xml.party.taxpayer.v1.FamilyStatus;
import ch.vd.unireg.xml.party.taxpayer.v1.Taxpayer;
import ch.vd.unireg.xml.party.taxpayer.v1.WithholdingTaxTariff;
import ch.vd.unireg.xml.party.taxresidence.v1.MixedWithholding137Par1;
import ch.vd.unireg.xml.party.taxresidence.v1.PureWithholding;
import ch.vd.unireg.xml.party.taxresidence.v1.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v1.SimplifiedTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationPeriod;
import ch.vd.unireg.xml.party.v1.AccountNumberFormat;
import ch.vd.unireg.xml.party.v1.BankAccount;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyInfo;
import ch.vd.unireg.xml.party.v1.PartyType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test unitaire pour le web service de la recherche.
 */
@SuppressWarnings({"JavaDoc"})
public class TiersServiceWebTest extends AbstractTiersServiceWebTest {

	// private static final Logger LOGGER = Logger.getLogger(TiersServiceWebTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebTest.xml";
	private static final int PREMIERE_ANNEE_FISCALE = 2003;

	private UserLogin login;
	private UserLogin zairfa; // Roselyne Favre

	private static boolean alreadySetUp = false;

	public TiersServiceWebTest() throws Exception {
	}

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}

		login = new UserLogin();
		login.setUserId("[UT] TiersServiceWebTest");
		login.setOid(22);

		zairfa = new UserLogin();
		zairfa.setUserId("zairfa");
		zairfa.setOid(22);
	}

	@Test
	public void testPing() {
		service.ping();
	}

	@Test
	public void testGetType() throws Exception {

		final GetPartyTypeRequest params = new GetPartyTypeRequest();
		params.setLogin(login);

		params.setPartyNumber(12100003); // Habitant
		assertEquals(PartyType.NATURAL_PERSON, service.getPartyType(params));

		params.setPartyNumber(34777810); // Habitant
		assertEquals(PartyType.NATURAL_PERSON, service.getPartyType(params));

		params.setPartyNumber(12100001); // Habitant
		assertEquals(PartyType.NATURAL_PERSON, service.getPartyType(params));

		params.setPartyNumber(12100002); // Habitant
		assertEquals(PartyType.NATURAL_PERSON, service.getPartyType(params));

		params.setPartyNumber(86116202); // Menage Commun
		assertEquals(PartyType.HOUSEHOLD, service.getPartyType(params));

		params.setPartyNumber(12500001); // DebiteurPrestationImposable
		assertEquals(PartyType.DEBTOR, service.getPartyType(params));

		params.setPartyNumber(12700101); // Entreprise
		assertEquals(PartyType.CORPORATION, service.getPartyType(params));

		params.setPartyNumber(12600101); // NonHabitant
		assertEquals(PartyType.NATURAL_PERSON, service.getPartyType(params));

		params.setPartyNumber(12800101); // AutreCommunaute
		assertEquals(PartyType.CORPORATION, service.getPartyType(params));
	}

	@Test
	public void testGetDateDebutFinActivite() throws Exception {

		assertActivite(null, null, 12100003, service);
		assertActivite(null, null, 12500001, service);
		assertActivite(newDate(2006, 9, 1), null, 12600101, service);
		assertActivite(null, null, 12100001, service);
		assertActivite(newDate(2002, 9, 1), null, 34777810, service);
		assertActivite(newDate(1987, 2, 1), null, 86116202, service);
		assertActivite(newDate(1980, 1, 1), newDate(1987, 1, 31), 12100002, service);
	}

	private void assertActivite(@Nullable Date debut, @Nullable Date fin, int numero, PartyWebService service) throws Exception {
		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(numero);
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final Party tiers = service.getParty(params);
		assertNotNull(tiers);
		assertSameDay(debut, tiers.getActivityStartDate());
		assertSameDay(fin, tiers.getActivityEndDate());
	}

	@Test
	public void testGetDebiteur() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12500001);
		params.getParts().add(PartyPart.DEBTOR_PERIODICITIES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);
		assertEquals(Integer.valueOf(12100002), debiteur.getAssociatedTaxpayerNumber());
		assertEquals("Employeur personnel menage", debiteur.getComplementaryName());
		assertEquals(DebtorCategory.ADMINISTRATORS, debiteur.getCategory());
		assertEquals(CommunicationMode.PAPER, debiteur.getCommunicationMode());
		assertTrue(debiteur.isWithoutReminder());
		assertTrue(debiteur.isWithoutWithholdingTaxDeclaration());
		assertEmpty(debiteur.getMailAddresses());
		assertEmpty(debiteur.getRepresentationAddresses());
		assertEmpty(debiteur.getDebtProsecutionAddresses());
		assertEmpty(debiteur.getRelationsBetweenParties());
		assertEmpty(debiteur.getTaxDeclarations());

		final List<DebtorPeriodicity> periodicites = debiteur.getPeriodicities();
		assertEquals(1, periodicites.size());

		final DebtorPeriodicity periodicite0 = periodicites.get(0);
		assertNotNull(periodicite0);
		assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, periodicite0.getPeriodicity());
		assertNull(periodicite0.getSpecificPeriod());
	}

	@Test
	public void testGetDebiteurAvecAdresses() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12500001);
		params.getParts().add(PartyPart.ADDRESSES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		final List<Address> adressesCourrier = debiteur.getMailAddresses();
		assertNotNull(adressesCourrier);
		assertEquals(2, adressesCourrier.size());

		final Address courrier0 = adressesCourrier.get(0);
		assertNotNull(courrier0);
		assertNull(courrier0.getDateFrom());
		assertSameDay(newDate(2004, 1, 28), courrier0.getDateTo());

		final AddressInformation courrierInfo0 = courrier0.getAddressInformation();
		assertNotNull(courrierInfo0);
		assertEquals(new Integer(0), courrierInfo0.getStreetId());
		assertNull(courrierInfo0.getHouseNumber());
		assertEquals(Integer.valueOf(283), courrierInfo0.getSwissZipCodeId());
		assertEquals(Long.valueOf(1168), courrierInfo0.getSwissZipCode());
		assertEquals("Villars-sous-Yens", courrierInfo0.getTown());

		final Address courrier1 = adressesCourrier.get(1);
		assertNotNull(courrier1);
		assertSameDay(newDate(2004, 1, 29), courrier1.getDateFrom());
		assertNull(courrier1.getDateTo());

		final AddressInformation courrierInfo1 = courrier1.getAddressInformation();
		assertNotNull(courrierInfo1);
		assertEquals(new Integer(141554), courrierInfo1.getStreetId());
		assertEquals("12", courrierInfo1.getHouseNumber());
		assertEquals(Integer.valueOf(1000), courrierInfo1.getSwissZipCodeId());
		assertEquals(Long.valueOf(1753), courrierInfo1.getSwissZipCode());
		assertEquals("Matran", courrierInfo1.getTown());

		final List<Address> adressesRepresentation = debiteur.getRepresentationAddresses();
		assertNotNull(adressesRepresentation);
		assertEquals(2, adressesRepresentation.size());

		final Address repres0 = adressesRepresentation.get(0);
		assertNotNull(repres0);
		assertNull(repres0.getDateFrom());
		assertSameDay(newDate(2004, 1, 28), repres0.getDateTo());

		final AddressInformation represInfo0 = repres0.getAddressInformation();
		assertNotNull(represInfo0);
		assertEquals(new Integer(0), represInfo0.getStreetId());
		assertNull(represInfo0.getHouseNumber());
		assertEquals(Integer.valueOf(283), represInfo0.getSwissZipCodeId());
		assertEquals("La Tuilière", represInfo0.getStreet());
		assertEquals(Long.valueOf(1168), represInfo0.getSwissZipCode());
		assertEquals("Villars-sous-Yens", represInfo0.getTown());

		final Address repres1 = adressesRepresentation.get(1);
		assertNotNull(repres1);
		assertSameDay(newDate(2004, 1, 29), repres1.getDateFrom());
		assertNull(repres1.getDateTo());

		final AddressInformation represInfo1 = repres1.getAddressInformation();
		assertNotNull(represInfo1);
		assertEquals(new Integer(32296), represInfo1.getStreetId());
		assertEquals("1", represInfo1.getHouseNumber());
		assertEquals(Integer.valueOf(528), represInfo1.getSwissZipCodeId());
		assertEquals("Avenue du Funiculaire", represInfo1.getStreet());
		assertEquals(Long.valueOf(1304), represInfo1.getSwissZipCode());
		assertEquals("Cossonay-Ville", represInfo1.getTown());
	}

	@Test
	public void testGetDebiteurAvecDeclarations() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12500001);
		params.getParts().add(PartyPart.TAX_DECLARATIONS);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		assertEmpty(debiteur.getMailAddresses());
		assertEmpty(debiteur.getRepresentationAddresses());
		assertEmpty(debiteur.getDebtProsecutionAddresses());

		final List<TaxDeclaration> declarations = debiteur.getTaxDeclarations();
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final TaxDeclaration declaration = declarations.get(0);
		assertNotNull(declaration);
		assertTrue(declaration instanceof WithholdingTaxDeclaration);

		final WithholdingTaxDeclaration lr = (WithholdingTaxDeclaration) declaration;
		assertSameDay(newDate(2008, 1, 1), lr.getDateFrom());
		assertSameDay(newDate(2008, 1, 31), lr.getDateTo());
		assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, lr.getPeriodicity());
		assertEquals(CommunicationMode.PAPER, lr.getCommunicationMode());
		assertNull(lr.getCancellationDate());
	}

	@Test
	public void testGetDebiteurComptesBancaires() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12500001);
		params.getParts().add(PartyPart.BANK_ACCOUNTS);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		final List<BankAccount> comptes = debiteur.getBankAccounts();
		assertEquals(1, comptes.size());

		final BankAccount compte = comptes.get(0);
		assertCompte("PME Duchemolle", "CH1900767000U01234567", AccountNumberFormat.IBAN, compte);
	}

	@Test
	public void testGetPersonnePhysique() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12100003);

		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);
		assertEquals(12100003L, personne.getNumber());
	}

	@Test
	public void testGetPersonnePhysiqueAvecDeclarations() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12100003);
		params.getParts().add(PartyPart.TAX_DECLARATIONS);

		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);

		assertEmpty(personne.getMailAddresses());
		assertEmpty(personne.getRepresentationAddresses());
		assertEmpty(personne.getDebtProsecutionAddresses());

		final List<TaxDeclaration> declarations = personne.getTaxDeclarations();
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final TaxDeclaration declaration = declarations.get(0);
		assertNotNull(declaration);
		assertTrue(declaration instanceof OrdinaryTaxDeclaration);

		final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) declaration;
		assertSameDay(newDate(2008, 1, 1), di.getDateFrom());
		assertSameDay(newDate(2008, 3, 31), di.getDateTo());
		assertNull(di.getCancellationDate());
		assertEquals(6789L, di.getSequenceNumber());
		assertEquals(DocumentType.VAUDTAX_TAX_DECLARATION, di.getDocumentType());
		assertEquals(5646L, di.getManagingMunicipalityFSOId());
	}

	@Test
	public void testGetPersonnePhysiqueAvecAdresseEnvoi() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12100003);
		params.getParts().add(PartyPart.ADDRESSES);

		final NaturalPerson pp = (NaturalPerson) service.getParty(params);
		assertNotNull(pp);

		final List<Address> courriers = pp.getMailAddresses();
		final Address courrier = courriers.get(courriers.size() - 1);
		assertNotNull(courrier);

		final FormattedAddress adresse = courrier.getFormattedAddress();
		assertNotNull(adresse);
		assertEquals("Madame", adresse.getLine1());
		assertEquals("Lyah Emery", trimValiPattern(adresse.getLine2()));
		assertEquals("Chemin du Riau 2A", adresse.getLine3());
		assertEquals("1162 St-Prex", adresse.getLine4());
		assertNull(adresse.getLine5());
		assertNull(adresse.getLine6());

		final PersonMailAddressInfo person = courrier.getPerson();
		assertNotNull(person);
		assertEquals("1", person.getMrMrs());
		assertEquals("Madame", person.getSalutation());
		assertEquals("Madame", person.getFormalGreeting());
		assertEquals("Lyah", trimValiPattern(person.getFirstName()));
		assertEquals("Emery", trimValiPattern(person.getLastName()));

		final AddressInformation info = courrier.getAddressInformation();
		assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
		assertNull(info.getComplementaryInformation());
		assertNull(info.getCareOf());
		assertEquals("Chemin du Riau 2A", info.getStreet());
		assertNull(info.getPostOfficeBoxNumber());
		assertEquals(Long.valueOf(1162), info.getSwissZipCode());
		assertEquals("St-Prex", info.getTown());
		assertEquals("CH", info.getCountry());
		assertEquals("Suisse", info.getCountryName());
	}

	@Test
	public void testGetPersonnePhysiqueDecedeeAvecAdresseEnvoi() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(20602603); // Delano Boschung
		params.getParts().add(PartyPart.ADDRESSES);

		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);
		assertSameDay(newDate(2008, 5, 1), personne.getDateOfDeath());

		final List<Address> courriers = personne.getMailAddresses();
		final Address courrier = courriers.get(courriers.size() - 1);
		assertNotNull(courrier);

		final FormattedAddress adresse = courrier.getFormattedAddress();
		assertNotNull(adresse);
		assertEquals("Aux héritiers de", adresse.getLine1());
		assertEquals("Delano Boschung, défunt", trimValiPattern(adresse.getLine2()));
		assertEquals("Ch. du Devin 81", adresse.getLine3());
		assertEquals("1012 Lausanne", adresse.getLine4());
		assertNull(adresse.getLine5());
		assertNull(adresse.getLine6());

		final PersonMailAddressInfo person = courrier.getPerson();
		assertNotNull(person);
		assertNull(person.getMrMrs());
		assertEquals("Aux héritiers de", person.getSalutation());
		assertEquals("Madame, Monsieur", person.getFormalGreeting()); // [UNIREG-1398]
		assertEquals("Delano", trimValiPattern(person.getFirstName()));
		assertEquals("Boschung, défunt", trimValiPattern(person.getLastName()));

		final AddressInformation info = courrier.getAddressInformation();
		assertNotNull(info);
		assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
		assertNull(info.getComplementaryInformation());
		assertNull(info.getCareOf());
		assertEquals("Ch. du Devin", info.getStreet());
		assertEquals("81", info.getHouseNumber());
		assertNull(info.getPostOfficeBoxNumber());
		assertEquals(Long.valueOf(1012), info.getSwissZipCode());
		assertEquals("Lausanne", info.getTown());
		assertEquals("CH", info.getCountry());
		assertEquals("Suisse", info.getCountryName());
	}

	@Test
	public void testGetPersonnePhysiqueAvecForFiscaux() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600101); // Andrea Conchita
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);

		final List<TaxResidence> forsFiscauxPrincipaux = personne.getMainTaxResidences();
		assertNotNull(forsFiscauxPrincipaux);
		assertEquals(1, forsFiscauxPrincipaux.size());

		final TaxResidence forPrincipal = forsFiscauxPrincipaux.get(0);
		assertNotNull(forPrincipal);
		assertEquals(TaxType.INCOME_WEALTH, forPrincipal.getTaxType());
		assertEquals(TaxLiabilityReason.RESIDENCE, forPrincipal.getTaxLiabilityReason());
		assertEquals(TaxationMethod.WITHHOLDING, forPrincipal.getTaxationMethod());
		assertSameDay(newDate(2006, 9, 1), forPrincipal.getDateFrom());
		assertNull(forPrincipal.getDateTo());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, forPrincipal.getTaxationAuthorityType());
		assertEquals(5586L, forPrincipal.getTaxationAuthorityFSOId());

		assertEmpty(personne.getOtherTaxResidences());
	}

	@Test
	public void testGetPersonnePhysiqueSituationFamillePersonneSeule() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12100003); // EMERY Lyah
		params.getParts().add(PartyPart.FAMILY_STATUSES);

		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);

		final List<FamilyStatus> situationsFamille = personne.getFamilyStatuses();
		assertNotNull(situationsFamille);
		assertEquals(1, situationsFamille.size());

		final FamilyStatus situation = situationsFamille.get(0);
		assertNotNull(situation);
		assertSameDay(newDate(1990, 2, 12), situation.getDateFrom());
		assertNull(situation.getDateTo());
		assertEquals(new Integer(0), situation.getNumberOfChildren());
		assertNull(situation.getApplicableTariff()); // seulement renseigné sur un couple
		assertNull(situation.getMainTaxpayerNumber()); // seulement renseigné sur un couple
	}

	@Test
	public void testGetPersonnePhysiqueAvecComptesBancaires() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12100003); // EMERY Lyah
		params.getParts().add(PartyPart.BANK_ACCOUNTS);

		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);

		final List<BankAccount> comptes = personne.getBankAccounts();
		assertEquals(1, comptes.size());

		final BankAccount compte = comptes.get(0);
		assertCompte("Emery Lyah", "CH1900767000U01234567", AccountNumberFormat.IBAN, compte);
	}

	@Test
	public void testGetPersonnePhysiqueSansComptesBancaires() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(34777810); // DESCLOUX Pascaline
		params.getParts().add(PartyPart.BANK_ACCOUNTS);

		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);
		assertEmpty(personne.getBankAccounts());
	}

	@Test
	public void testGetPersonnePhysiqueSituationFamilleCouple() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(86116202); // Les Schmidt
		params.getParts().add(PartyPart.FAMILY_STATUSES);

		final CommonHousehold menage = (CommonHousehold) service.getParty(params);
		assertNotNull(menage);

		final List<FamilyStatus> situationsFamille = menage.getFamilyStatuses();
		assertNotNull(situationsFamille);
		assertEquals(1, situationsFamille.size());

		final FamilyStatus situation = situationsFamille.get(0);
		assertNotNull(situation);
		assertSameDay(newDate(1990, 2, 12), situation.getDateFrom());
		assertNull(situation.getDateTo());
		assertEquals(new Integer(0), situation.getNumberOfChildren());
		assertEquals(WithholdingTaxTariff.NORMAL, situation.getApplicableTariff()); // seulement renseigné sur un couple
		assertEquals(Integer.valueOf(12100002), situation.getMainTaxpayerNumber()); // seulement renseigné sur un couple
	}

	@Test
	public void testGetMenageCommun() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(86116202);
		params.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);

		final CommonHousehold menage = (CommonHousehold) service.getParty(params);
		assertNotNull(menage);
		assertEquals(86116202L, menage.getNumber());

		final List<RelationBetweenParties> rapports = menage.getRelationsBetweenParties();
		assertEquals(2, rapports.size()); // 2 rapports appartenance ménages

		/* Extrait les différents type de rapports */
		List<RelationBetweenParties> rapportsMenage = new ArrayList<RelationBetweenParties>();
		for (RelationBetweenParties rapport : rapports) {
			assertNotNull(rapport);
			if (RelationBetweenPartiesType.HOUSEHOLD_MEMBER == rapport.getType()) {
				assertTrue("Trouvé plus de 2 rapports de type appartenance ménage", rapportsMenage.size() < 2);
				rapportsMenage.add(rapport);
			}
			else {
				fail("Type de rapport-entre-tiers non attendu [" + rapport.getType().name() + "]");
			}
		}

		/* Trie la collection de rapports appartenance ménage par ordre croissant de numéro de l'autre tiers */
		Collections.sort(rapportsMenage, new Comparator<RelationBetweenParties>() {
			@Override
			public int compare(RelationBetweenParties r1, RelationBetweenParties r2) {
				return (r1.getOtherPartyNumber() - r2.getOtherPartyNumber());
			}
		});
		assertEquals(2, rapportsMenage.size());

		final RelationBetweenParties rapportMenage0 = rapportsMenage.get(0);
		assertEquals(12100001L, rapportMenage0.getOtherPartyNumber());
		assertSameDay(newDate(1987, 2, 1), rapportMenage0.getDateFrom());
		assertNull(rapportMenage0.getDateTo());

		final RelationBetweenParties rapportMenage1 = rapportsMenage.get(1);
		assertEquals(12100002L, rapportMenage1.getOtherPartyNumber());
		assertSameDay(newDate(1987, 2, 1), rapportMenage1.getDateFrom());
		assertNull(rapportMenage1.getDateTo());
	}

	@Test
	public void testSearchTiersParNumeroZeroResultat() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setNumber("1239876");

		final SearchPartyResponse response = service.searchParty(params);
		assertNotNull(response);
		assertEquals(0, response.getItems().size());
	}

	@Test
	public void testSearchTiersParNumeroUnResultat() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setNumber("12100003");

		final SearchPartyResponse list = service.searchParty(params);
		assertNotNull(list);
		assertEquals(1, list.getItems().size());

		final PartyInfo info = list.getItems().get(0);
		assertEquals(12100003L, info.getNumber());
		assertEquals("Lyah Emery", trimValiPattern(info.getName1()));
		assertEquals("", info.getName2());
		assertEquals(newDate(2005, 8, 29), info.getDateOfBirth());
		assertEquals("1162", info.getZipCode());
		assertEquals("St-Prex", info.getTown());
		assertEquals("Suisse", info.getCountry());
		assertEquals("Chemin du Riau 2A", info.getStreet());
		assertEquals(PartyType.NATURAL_PERSON, info.getType());
	}

	@Test
	public void testSearchTiersParNumeroPlusieursResultats() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setNumber("12100001 + 12100002"); // Les Schmidt

		final SearchPartyResponse list = service.searchParty(params);
		assertNotNull(list);
		assertEquals(2, list.getItems().size());

		// on retrouve les schmidt (couple + 2 tiers)
		int nbFound = 0;
		for (int i = 0; i < list.getItems().size(); i++) {
			PartyInfo info = list.getItems().get(i);
			if (12100001L == info.getNumber()) { // Madame
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				nbFound++;
			}
			if (12100002L == info.getNumber()) { // Monsieur
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				nbFound++;
			}
		}
		assertEquals(2, nbFound);
	}

	@Test
	public void testSearchTiersZeroResultat() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setContactName("GENGIS KHAN");
		params.setSearchMode(SearchMode.CONTAINS);

		final SearchPartyResponse list = service.searchParty(params);
		assertNotNull(list);
		assertEquals(0, list.getItems().size());
	}

	@Test
	public void testSearchTiersUnResultat() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setContactName("EMERY");
		params.setSearchMode(SearchMode.CONTAINS);

		final SearchPartyResponse list = service.searchParty(params);
		assertNotNull(list);
		assertEquals(1, list.getItems().size());

		final PartyInfo info = list.getItems().get(0);
		assertEquals(12100003L, info.getNumber());
		assertEquals("Lyah Emery", trimValiPattern(info.getName1()));
		assertEquals("", info.getName2());
		assertEquals(newDate(2005, 8, 29), info.getDateOfBirth());
		assertEquals("1162", info.getZipCode());
		assertEquals("St-Prex", info.getTown());
		assertEquals("Suisse", info.getCountry());
		assertEquals("Chemin du Riau 2A", info.getStreet());
		assertEquals(PartyType.NATURAL_PERSON, info.getType());
	}

	@Test
	public void testSearchTiersPlusieursResultats() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setTownOrCountry("Yens");
		params.setSearchMode(SearchMode.CONTAINS);

		final SearchPartyResponse list = service.searchParty(params);
		assertNotNull(list);
		assertEquals(5, list.getItems().size());

		// on retrouve les schmidt (couple + 2 tiers), pascaline descloux et un débiteur associé
		int nbFound = 0;
		for (int i = 0; i < list.getItems().size(); i++) {
			PartyInfo info = list.getItems().get(i);
			if (34777810L == info.getNumber()) {
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				nbFound++;
			}
			if (12100001L == info.getNumber()) {
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				nbFound++;
			}
			if (12100002L == info.getNumber()) {
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				nbFound++;
			}
			if (86116202L == info.getNumber()) {
				assertEquals(PartyType.HOUSEHOLD, info.getType());
				nbFound++;
			}
			if (12500001L == info.getNumber()) {
				assertEquals(PartyType.DEBTOR, info.getType());
				nbFound++;
			}
		}
		assertEquals(5, nbFound);
	}

	@Test
	public void testSearchTiersSurNoOfsFor() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setTaxResidenceFSOId(5652);

		final SearchPartyResponse list = service.searchParty(params);
		assertNotNull(list);
		assertEquals(2, list.getItems().size());

		// on retrouve les schmidt (couple + un composant du ménage)
		int nbFound = 0;
		for (int i = 0; i < list.getItems().size(); i++) {
			PartyInfo info = list.getItems().get(i);
			long numero = info.getNumber();
			if (86116202L == numero) {
				assertEquals(PartyType.HOUSEHOLD, info.getType());
				nbFound++;
			}
			if (12100002L == numero) {
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				nbFound++;
			}
		}
		assertEquals(2, nbFound);
	}

	@Test
	public void testSearchTiersSurNoOfsForActif() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setTaxResidenceFSOId(5652);
		params.setActiveMainTaxResidence(true);

		final SearchPartyResponse list = service.searchParty(params);
		assertNotNull(list);
		assertEquals(1, list.getItems().size());

		PartyInfo info = list.getItems().get(0);
		assertEquals(86116202L, info.getNumber());
		assertEquals(PartyType.HOUSEHOLD, info.getType());
	}

	@Test
	public void testSetBlocageRemboursementAutomatique() throws Exception {

		/*
		 * Etat avant changement du blocage
		 */

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(12100003); // EMERY Lyah

			final NaturalPerson personne = (NaturalPerson) service.getParty(params);
			assertNotNull(personne);
			assertFalse(personne.isAutomaticReimbursementBlocked());
		}

		/*
		 * Blocage du remboursement automatique
		 */

		{
			final SetAutomaticReimbursementBlockingRequest params = new SetAutomaticReimbursementBlockingRequest();
			params.setLogin(login);
			params.setPartyNumber(12100003); // EMERY Lyah
			params.setBlocked(true);

			service.setAutomaticReimbursementBlocking(params);
		}

		/*
		 * Etat après changement du blocage
		 */

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(12100003); // EMERY Lyah

			final NaturalPerson personne = (NaturalPerson) service.getParty(params);
			assertNotNull(personne);
			assertTrue(personne.isAutomaticReimbursementBlocked());
		}
	}

	@Test
	public void testAnnulationFlag() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(77714803); // RAMONI Jean
		params.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);
		params.getParts().add(PartyPart.TAX_DECLARATIONS);
		params.getParts().add(PartyPart.TAX_DECLARATIONS_STATUSES);
		params.getParts().add(PartyPart.FAMILY_STATUSES);
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		// Personne annulée
		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);
		assertSameDay(newDate(2009, 3, 4), personne.getCancellationDate());

		// Rapport entre tiers annulé
		final List<RelationBetweenParties> rapports = personne.getRelationsBetweenParties();
		assertNotNull(rapports);
		assertEquals(1, rapports.size());

		final RelationBetweenParties tutelle = rapports.get(0);
		assertNotNull(tutelle);
		assertEquals(RelationBetweenPartiesType.GUARDIAN, tutelle.getType());
		assertSameDay(newDate(2009, 3, 4), tutelle.getCancellationDate());

		// Déclaration annulée
		final List<TaxDeclaration> declarations = personne.getTaxDeclarations();
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final TaxDeclaration decl = declarations.get(0);
		assertNotNull(decl);
		assertSameDay(newDate(2009, 3, 4), decl.getCancellationDate());

		// Délai déclaration annulé
		// final List<DelaiDeclaration> delais = decl.getDelais();
		// assertNotNull(delais);
		// assertEquals(1, delais.size());
		//
		// final DelaiDeclaration delai = delais.get(0);
		// assertNotNull(delai);
		// assertSameDay(newDate(2009, 3, 4), delai.getDateAnnulation());

		// Etat déclaration annulé
		final List<TaxDeclarationStatus> etats = decl.getStatuses();
		assertNotNull(etats);
		assertEquals(1, etats.size());

		final TaxDeclarationStatus etat = etats.get(0);
		assertNotNull(etat);
		assertSameDay(newDate(2009, 3, 4), etat.getCancellationDate());

		// Situation de famille annulée
		final List<FamilyStatus> situations = personne.getFamilyStatuses();
		assertNotNull(situations);
		assertEquals(1, situations.size());

		final FamilyStatus situ = situations.get(0);
		assertNotNull(situ);
		assertSameDay(newDate(2009, 3, 4), situ.getCancellationDate());

		// For fiscal annulé
		final List<TaxResidence> fors = personne.getMainTaxResidences();
		assertNotNull(fors);
		assertEquals(1, fors.size());

		final TaxResidence f = fors.get(0);
		assertNotNull(f);
		assertSameDay(newDate(2009, 3, 4), f.getCancellationDate());
	}

	/**
	 * [UNIREG-865] le service doit retourner null lorsque le tiers n'est pas connu, et non pas lever une exception.
	 */
	@Test
	public void testGetTiersTypeInconnu() throws Exception {

		final GetPartyTypeRequest params = new GetPartyTypeRequest();
		params.setLogin(login);
		params.setPartyNumber(32323232); // inconnu

		assertNull(service.getPartyType(params));
	}

	/**
	 * [UNIREG-865] le service doit retourner null lorsque le tiers n'est pas connu, et non pas lever une exception.
	 */
	@Test
	public void testGetTiersInconnu() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(32323232); // inconnu

		assertNull(service.getParty(params));
	}

	/**
	 * [UNIREG-910] la période d'imposition courante doit être ouverte
	 */
	@Test
	public void testGetTiersPeriodesImposition() throws Exception {

		final int anneeCourante = RegDate.get().year();

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(34777810); // DESCLOUX Pascaline
		params.getParts().add(PartyPart.TAXATION_PERIODS);

		// récupération des périodes d'imposition
		final Taxpayer ctb = (Taxpayer) service.getParty(params);
		assertNotNull(ctb);
		final List<TaxationPeriod> periodes = ctb.getTaxationPeriods();
		assertNotNull(periodes);

		final int size = periodes.size();
		assertEquals(anneeCourante - PREMIERE_ANNEE_FISCALE + 1, size);

		// année 2002 à année courante - 1
		for (int i = 0; i < size - 1; ++i) {
			final TaxationPeriod p = periodes.get(i);
			assertNotNull(p);
			assertSameDay(newDate(i + PREMIERE_ANNEE_FISCALE, 1, 1), p.getDateFrom());
			assertSameDay(newDate(i + PREMIERE_ANNEE_FISCALE, 12, 31), p.getDateTo());
		}

		// année courante
		final TaxationPeriod derniere = periodes.get(size - 1);
		assertNotNull(derniere);
		assertSameDay(newDate(anneeCourante, 1, 1), derniere.getDateFrom());
		assertNull(derniere.getDateTo());
	}

	/**
	 * [UNIREG-1133] le nom du pays doit être correct
	 */
	@Test
	public void testGetTiersAdresseHorsSuisse() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(10035633); // Tummers-De Wit Wouter
		params.getParts().add(PartyPart.ADDRESSES);

		final Taxpayer ctb = (Taxpayer) service.getParty(params);
		assertNotNull(ctb);

		final List<Address> courriers = ctb.getMailAddresses();
		final Address courrier = courriers.get(courriers.size() - 1);
		assertNotNull(courrier);
		assertSameDay(newDate(2009, 6, 25), courrier.getDateFrom());
		assertNull(courrier.getDateTo());

		final FormattedAddress adresseEnvoi = courrier.getFormattedAddress();
		assertNotNull(adresseEnvoi);
		assertEquals("Madame, Monsieur", adresseEnvoi.getLine1());
		assertEquals("Tummers-De Wit Wouter", adresseEnvoi.getLine2());
		assertEquals("De Wit Tummers Elisabeth", adresseEnvoi.getLine3());
		assertEquals("Olympialaan 17", adresseEnvoi.getLine4());
		assertEquals("4624 Aa Bergem Op Zoom", adresseEnvoi.getLine5());
		assertEquals("Pays-Bas", adresseEnvoi.getLine6());

		final PersonMailAddressInfo person = courrier.getPerson();
		assertNotNull(person);
		assertNull(person.getMrMrs());
		assertEquals("Madame, Monsieur", person.getSalutation());
		assertEquals("Madame, Monsieur", person.getFormalGreeting());
		assertNull(trimValiPattern(person.getFirstName()));
		assertEquals("Tummers-De Wit Wouter", trimValiPattern(person.getLastName()));

		final AddressInformation info = courrier.getAddressInformation();
		assertNotNull(info);
		assertEquals(TariffZone.EUROPE, info.getTariffZone());
		assertEquals("De Wit Tummers Elisabeth", info.getComplementaryInformation());
		assertNull(info.getCareOf());
		assertEquals("Olympialaan 17", info.getStreet());
		assertNull(info.getPostOfficeBoxNumber());
		assertNull(info.getSwissZipCode());
		assertNull(info.getForeignZipCode());
		assertEquals("4624 Aa Bergem Op Zoom", info.getTown());
		assertEquals("NL", info.getCountry());
		assertEquals("Pays-Bas", info.getCountryName());
		assertNull(info.getSwissZipCodeId());
		assertNull(info.getStreetId());
		assertNull(info.getDwellingNumber());
	}

	/**
	 * Vérifie que la méthode ne retourne rien si on lui demande rien.
	 */
	@Test
	public void testGetBatchTiersEmptyList() throws Exception {

		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(login);

		final BatchParty batch = service.getBatchParty(params);
		assertNotNull(batch);
		assertEmpty(batch.getEntries());
	}

	/**
	 * Vérifie que la méthode ne retourne rien si on lui demande un tiers inconnu.
	 */
	@Test
	public void testGetBatchTiersSurTiersInconnu() throws Exception {

		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(login);
		params.getPartyNumbers().add(32323232); // inconnu

		final BatchParty batch = service.getBatchParty(params);
		assertNotNull(batch);
		assertEmpty(batch.getEntries());
	}

	/**
	 * Vérifie que la méthode retourne bien un tiers.
	 */
	@Test
	public void testGetBatchTiersUnTiers() throws Exception {

		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(login);
		params.getPartyNumbers().add(12100003);

		final BatchParty batch = service.getBatchParty(params);
		assertNotNull(batch);
		assertEquals(1, batch.getEntries().size());

		final BatchPartyEntry entry = batch.getEntries().get(0);
		assertNotNull(entry);
		assertNull(entry.getExceptionInfo());

		final Party tiers = entry.getParty();
		assertNotNull(tiers);
		assertEquals(12100003L, tiers.getNumber());
	}

	/**
	 * Vérifie que la méthode retourne bien plusieurs tiers.
	 */
	@Test
	public void testGetBatchTiersHistoPlusieursTiers() throws Exception {

		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(login);
		params.getPartyNumbers().add(77714803);
		params.getPartyNumbers().add(12100003);
		params.getPartyNumbers().add(34777810);
		params.getPartyNumbers().add(12100001);
		params.getPartyNumbers().add(12100002);
		params.getPartyNumbers().add(86116202);
		params.getPartyNumbers().add(12500001);
		params.getPartyNumbers().add(12600101);
		params.getPartyNumbers().add(10035633);
		final int size = params.getPartyNumbers().size();

		final BatchParty batch = service.getBatchParty(params);
		assertNotNull(batch);
		assertEquals(size, batch.getEntries().size());

		final Set<Integer> ids = new HashSet<Integer>();
		for (BatchPartyEntry entry : batch.getEntries()) {
			ids.add(entry.getNumber());
			assertNotNull("Le tiers n°" + entry.getNumber() + " est nul !", entry.getParty());
			assertNull(entry.getExceptionInfo());
		}
		assertEquals(size, ids.size());
		assertTrue(ids.contains(77714803));
		assertTrue(ids.contains(12100003));
		assertTrue(ids.contains(34777810));
		assertTrue(ids.contains(12100001));
		assertTrue(ids.contains(12100002));
		assertTrue(ids.contains(86116202));
		assertTrue(ids.contains(12500001));
		assertTrue(ids.contains(12600101));
		assertTrue(ids.contains(10035633));
	}

	/**
	 * Vérifie que la méthode n'expose pas un tiers non-autorisé et renseigne correctement la raison de l'exception.
	 */
	@Test
	public void testGetBatchTiersHistoSurTiersNonAutorise() throws Exception {

		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(zairfa); // Roselyne Favre
		params.getPartyNumbers().add(10149508); // Pascal Broulis

		final BatchParty batch = service.getBatchParty(params);
		assertNotNull(batch);
		assertEquals(1, batch.getEntries().size());

		final BatchPartyEntry entry = batch.getEntries().get(0);
		assertNotNull(entry);
		assertNull(entry.getParty()); // autorisation exclusive pour Francis Perroset

		final ServiceExceptionInfo exceptionInfo = entry.getExceptionInfo();
		assertTrue(exceptionInfo instanceof AccessDeniedExceptionInfo);
		assertEquals("L'utilisateur spécifié (zairfa/22) n'a pas les droits d'accès en lecture sur le tiers n° 10149508", exceptionInfo.getMessage());
	}

	/**
	 * [UNIREG-1395] vérifie que la catégorie est bien calculée
	 */
	@Test
	public void testGetTiersCategorie() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);

		{ // catégorie inconnue
			params.setPartyNumber(10035633); // Tummers-De Wit Wouter
			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			assertNull(pp.getCategory());
		}

		{ // permis B
			params.setPartyNumber(10174192); // Eudina Mara Alencar Casal
			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			assertEquals(NaturalPersonCategory.C_02_B_PERMIT, pp.getCategory());
		}
	}

	/**
	 * [UNIREG-1291] teste les fors virtuels
	 */
	@Test
	public void testGetTiersForVirtuels() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.getParts().add(PartyPart.TAX_RESIDENCES);
		params.setPartyNumber(12100002); // Laurent Schmidt

		//
		// sans les fors virtuels
		//

		{
			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);

			final List<TaxResidence> fors = pp.getMainTaxResidences();
			assertNotNull(fors);
			assertEquals(1, fors.size());

			final TaxResidence fp = fors.get(0);
			assertSameDay(newDate(1980, 1, 1), fp.getDateFrom());
			assertSameDay(newDate(1987, 1, 31), fp.getDateTo());
			assertFalse(fp.isVirtual());
		}

		//
		// avec les fors virtuels
		//

		params.getParts().add(PartyPart.VIRTUAL_TAX_RESIDENCES);

		{
			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);

			final List<TaxResidence> fors = pp.getMainTaxResidences();
			assertNotNull(fors);
			assertEquals(2, fors.size());

			final TaxResidence fp0 = fors.get(0);
			assertSameDay(newDate(1980, 1, 1), fp0.getDateFrom());
			assertSameDay(newDate(1987, 1, 31), fp0.getDateTo());
			assertFalse(fp0.isVirtual());

			final TaxResidence fp1 = fors.get(1);
			assertSameDay(newDate(1987, 2, 1), fp1.getDateFrom());
			assertNull(fp1.getDateTo());
			assertTrue(fp1.isVirtual());
		}

		//
		// une nouvelle fois sans les fors virtuels (permet de vérifier que le cache ne nous retourne pas les fors virtuels s'ils ne sont
		// pas demandés)
		//

		params.getParts().remove(PartyPart.VIRTUAL_TAX_RESIDENCES);

		{
			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);

			final List<TaxResidence> fors = pp.getMainTaxResidences();
			assertNotNull(fors);
			assertEquals(1, fors.size());

			final TaxResidence fp = fors.get(0);
			assertSameDay(newDate(1980, 1, 1), fp.getDateFrom());
			assertSameDay(newDate(1987, 1, 31), fp.getDateTo());
			assertFalse(fp.isVirtual());
		}
	}

	/**
	 * [UNIREG-1517] l'assujettissement courant d'un contribuable encore assujetti doit avoir une date de fin nulle.
	 */
	@Test
	public void getTiersHistoPeriodesAssujettissement() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(87654321); // Alfred Dupneu
		params.getParts().add(PartyPart.SIMPLIFIED_TAX_LIABILITIES);

		final NaturalPerson pp = (NaturalPerson) service.getParty(params);
		assertNotNull(pp);

		final List<SimplifiedTaxLiability> lic = pp.getSimplifiedTaxLiabilityVD();
		assertEquals(2, lic.size());

		{ // assujettissement passé

			final SimplifiedTaxLiability a = lic.get(0);
			assertNotNull(a);
			assertSameDay(newDate(1985, 9, 1), a.getDateFrom());
			assertSameDay(newDate(1990, 2, 15), a.getDateTo());
			assertEquals(SimplifiedTaxLiabilityType.LIMITED, a.getType()); // Hors-Suisse
		}

		{ // assujettissement courant

			final SimplifiedTaxLiability a = lic.get(1);
			assertNotNull(a);
			assertSameDay(newDate(2003, 7, 12), a.getDateFrom());
			assertNull(a.getDateTo());
			assertEquals(SimplifiedTaxLiabilityType.LIMITED, a.getType()); // Hors-Suisse
		}

		final List<SimplifiedTaxLiability> lifd = pp.getSimplifiedTaxLiabilityCH();
		assertEquals(2, lifd.size());

		{ // assujettissement passé

			final SimplifiedTaxLiability a = lifd.get(0);
			assertNotNull(a);
			assertSameDay(newDate(1985, 9, 1), a.getDateFrom());
			assertSameDay(newDate(1990, 2, 15), a.getDateTo());
			assertEquals(SimplifiedTaxLiabilityType.LIMITED, a.getType()); // Hors-Suisse
		}

		{ // assujettissement courant

			final SimplifiedTaxLiability a = lifd.get(1);
			assertNotNull(a);
			assertSameDay(newDate(2003, 7, 12), a.getDateFrom());
			assertNull(a.getDateTo());
			assertEquals(SimplifiedTaxLiabilityType.LIMITED, a.getType()); // Hors-Suisse
		}
	}

	/**
	 * [UNIREG-1969] Vérification que le champ "chez" apparaît bien dans l'adresse d'envoi
	 */
	@Test
	public void getTiersPMAdresseEnvoi() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(312); // PLACE CENTRALE, La Sarraz
		params.getParts().add(PartyPart.ADDRESSES);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);

		final List<Address> adresses = pm.getMailAddresses();
		final FormattedAddress adresse = adresses.get(adresses.size() - 1).getFormattedAddress();
		assertNotNull(adresse);
		assertEquals("Société immobilière de", trimValiPattern(adresse.getLine1()));
		assertEquals("Place centrale S.A. Pe", trimValiPattern(adresse.getLine2()));
		assertEquals("en liquidation", trimValiPattern(adresse.getLine3()));
		assertEquals("c/o Mme Hugette Grisel", trimValiPattern(adresse.getLine4()));
		assertEquals("Rue du Chêne 9", trimValiPattern(adresse.getLine5()));
		assertEquals("1315 La Sarraz", trimValiPattern(adresse.getLine6()));
	}

	/**
	 * [UNIREG-1974] Vérification du libellé de la rue dans l'adresse d'envoi
	 */
	@Test
	public void getTiersPMAdresseEnvoiNomRue() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1314); // JAL HOLDING
		params.getParts().add(PartyPart.ADDRESSES);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);

		final List<Address> adresses = pm.getMailAddresses();
		final FormattedAddress adresse = adresses.get(adresses.size() - 1).getFormattedAddress();
		assertNotNull(adresse);
		assertEquals("Jal holding S.A.", trimValiPattern(adresse.getLine1()));
		assertEquals("", trimValiPattern(adresse.getLine2()));
		assertEquals("en liquidation", trimValiPattern(adresse.getLine3()));
		assertEquals("pa Fidu. Commerce & Industrie", trimValiPattern(adresse.getLine4()));
		assertEquals("Avenue de la Gare 10", trimValiPattern(adresse.getLine5()));
		assertEquals("1003 Lausanne", trimValiPattern(adresse.getLine6()));
	}

	// [SIFISC-2057]
	@Test
	public void testGetPartyTaxLiaibilities() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12900001); // Michel Lederet
		params.getParts().add(PartyPart.TAX_LIABILITIES);

		final NaturalPerson np = (NaturalPerson) service.getParty(params);
		assertNotNull(np);

		final List<TaxLiability> list = np.getTaxLiabilities();
		assertNotNull(list);
		assertEquals(2, list.size());

		final TaxLiability t0 = list.get(0);
		assertNotNull(t0);
		assertTrue(t0 instanceof PureWithholding);

		final PureWithholding p0 = (PureWithholding) t0;
		assertEquals(newDate(2000, 1, 1), p0.getDateFrom());
		assertEquals(newDate(2001, 12, 31), p0.getDateTo());

		final TaxLiability t1 = list.get(1);
		assertNotNull(t1);
		assertTrue(t1 instanceof MixedWithholding137Par1);

		final MixedWithholding137Par1 m1 = (MixedWithholding137Par1) t1;
		assertEquals(newDate(2002, 1, 1), m1.getDateFrom());
		assertNull(m1.getDateTo());
	}

	// [SIFISC-2588]
	@Test
	public void testGetImmovableProperties() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12900001); // Michel Lederet
		params.getParts().add(PartyPart.IMMOVABLE_PROPERTIES);

		final NaturalPerson np = (NaturalPerson) service.getParty(params);
		assertNotNull(np);

		final List<ImmovableProperty> immovableProperties = np.getImmovableProperties();
		assertNotNull(immovableProperties);
		assertEquals(1, immovableProperties.size());

		final ImmovableProperty immo0 = immovableProperties.get(0);
		assertNotNull(immo0);
		assertEquals("132/2345", immo0.getNumber());
		assertEquals(newDate(1976, 4, 27), immo0.getDateFrom());
		assertNull(immo0.getDateTo());
		assertEquals(860000, immo0.getEstimatedTaxValue());
		assertEquals(newDate(2002, 3, 2), immo0.getEstimatedTaxValueDate());
		assertEquals(Integer.valueOf(572000), immo0.getFormerEstimatedTaxValue());
		assertEquals("Place-jardin", immo0.getNature());
		assertEquals(OwnershipType.COLLECTIVE_OWNERSHIP, immo0.getType());

		final PropertyShare share = immo0.getShare();
		assertNotNull(share);
		assertEquals(1, share.getNumerator());
		assertEquals(2, share.getDenominator());
	}
}

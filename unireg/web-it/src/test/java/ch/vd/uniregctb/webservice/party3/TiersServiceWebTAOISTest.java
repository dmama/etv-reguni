package ch.vd.uniregctb.webservice.party3;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

import ch.ech.ech0044.v2.DatePartiallyKnown;
import ch.ech.ech0044.v2.PersonIdentification;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.webservices.party3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.party3.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.PartyNumberList;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.SearchMode;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.SearchPartyResponse;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.OrganisationMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.debtor.v1.CommunicationMode;
import ch.vd.unireg.xml.party.debtor.v1.Debtor;
import ch.vd.unireg.xml.party.debtor.v1.DebtorCategory;
import ch.vd.unireg.xml.party.debtor.v1.DebtorInfo;
import ch.vd.unireg.xml.party.debtor.v1.DebtorPeriodicity;
import ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.relation.v1.ActivityType;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.WithholdingTaxDeclaration;
import ch.vd.unireg.xml.party.taxpayer.v1.FamilyStatus;
import ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus;
import ch.vd.unireg.xml.party.taxpayer.v1.Taxpayer;
import ch.vd.unireg.xml.party.taxpayer.v1.WithholdingTaxTariff;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationPeriod;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyInfo;
import ch.vd.unireg.xml.party.v1.PartyType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class TiersServiceWebTAOISTest extends AbstractTiersServiceWebTest {

	//private static final Logger LOGGER = Logger.getLogger(WebitTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebTAOISTest.xml";

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
		login.setUserId("[UT] TiersServiceWebTAOISTest");
		login.setOid(22);
	}

	@Test
	public void testGetListCtbModifies() throws Exception {

		final GetModifiedTaxpayersRequest params = new GetModifiedTaxpayersRequest();
		params.setLogin(login);
		RegDate dateDebut = RegDate.get(2008, 1, 1);
		RegDate dateFin = RegDate.get(2008, 1, 2);
		XMLGregorianCalendar calDebut = regdate2xmlcal(dateDebut);
		XMLGregorianCalendar calFin = regdate2xmlcal(dateFin);
		params.setSearchBeginDate(calDebut);
		params.setSearchEndDate(calFin);

		PartyNumberList list = service.getModifiedTaxpayers(params);

		assertNotNull(list);
		assertEquals(9, list.getItem().size());
	}


	@Test
	public void testGetDateArriveeSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600001); // Isidor Pirez
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);
		assertSameDay(newDate(2008, 1, 29), tiers.getActivityStartDate());
	}

	@Test
	public void testGetNumeroAVSSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600001); // Isidor Pirez

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		final PersonIdentification identification = tiers.getIdentification();
		assertNotNull(identification);
		assertEquals(Long.valueOf(7565789312435L), identification.getVn());
	}

	@Test
	public void testGetNomSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600001); // Isidor Pirez

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		final PersonIdentification identification = tiers.getIdentification();
		assertNotNull(identification);
		assertEquals("Pirez", identification.getOfficialName());
	}

	@Test
	public void testGetPrenomSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600001); // Isidor Pirez

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		final PersonIdentification identification = tiers.getIdentification();
		assertNotNull(identification);
		assertEquals("Isidor", identification.getFirstName());
	}

	@Test
	public void testGetDateNaissanceSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600001); // Isidor Pirez

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);
		assertSameDay(newDate(1971, 1, 23), tiers.getDateOfBirth());

		final PersonIdentification identification = tiers.getIdentification();
		assertNotNull(identification);
		final DatePartiallyKnown date = identification.getDateOfBirth();
		assertNotNull(date);
		assertNull(date.getYear());
		assertNull(date.getYearMonth());
		final XMLGregorianCalendar cal = date.getYearMonthDay();
		assertNotNull(cal);
		assertEquals(1971, cal.getYear());
		assertEquals(1, cal.getMonth());
		assertEquals(23, cal.getDay());
	}

	@Test
	public void testGetTarifApplicableAvecHistorique() throws Exception {

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600003); // Mario Gomez
		params.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);

		// Récupération du tiers
		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RelationBetweenParties> rapports = tiers.getRelationsBetweenParties();
		assertNotNull(rapports);

		RelationBetweenParties rapportMenage = null;
		for (RelationBetweenParties r : rapports) {
			if (RelationBetweenPartiesType.HOUSEHOLD_MEMBER == r.getType() && r.getDateTo() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		// Note: l'information 'tarif applicable' n'a de sens et n'est renseignée que sur un ménage commun
		GetPartyRequest paramsMenage = new GetPartyRequest();
		paramsMenage.setLogin(login);
		paramsMenage.setPartyNumber(rapportMenage.getOtherPartyNumber());
		paramsMenage.getParts().add(PartyPart.FAMILY_STATUSES);

		final CommonHousehold menage = (CommonHousehold) service.getParty(paramsMenage);
		assertNotNull(menage);

		// Recherche de l'historique de la situation de famille du tiers ménage (ici, il n'y en a qu'une)
		final List<FamilyStatus> situations = menage.getFamilyStatuses();
		assertEquals(1, situations.size());

		final FamilyStatus situationFamille = situations.get(0);
		assertNotNull(situationFamille);
		assertEquals(WithholdingTaxTariff.NORMAL, situationFamille.getApplicableTariff());
		assertEquals(Integer.valueOf(12600003), situationFamille.getMainTaxpayerNumber());
	}

	@Test
	public void testGetEtatCivilSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600001); // Isidor Pirez
		params.getParts().add(PartyPart.FAMILY_STATUSES);

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		final PersonIdentification identification = tiers.getIdentification();
		assertNotNull(identification);
		assertEquals("Pirez", identification.getOfficialName());
		assertEquals("Isidor", identification.getFirstName());

		// Recherche de l'historique de la situation de famille du sourcier
		final List<FamilyStatus> situations = tiers.getFamilyStatuses();
		assertNotNull(situations);
		assertEquals(1, situations.size());

		final FamilyStatus situationFamille = situations.get(0);
		assertNotNull(situationFamille);
		assertEquals(MaritalStatus.SINGLE, situationFamille.getMaritalStatus());
	}

	@Test
	public void testGetForPrincipalSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600001); // Isidor Pirez
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		// Récupération de l'historique des fors fiscaux principaux du sourcier
		final List<TaxResidence> forsPrincipaux = tiers.getMainTaxResidences();
		assertNotNull(forsPrincipaux);
		assertEquals(1, forsPrincipaux.size());

		final TaxResidence forPrincipal = forsPrincipaux.get(0);
		assertNotNull(forPrincipal);
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, forPrincipal.getTaxationAuthorityType());
		assertEquals(5477L, forPrincipal.getTaxationAuthorityFSOId()); // Cossonay
	}

	@Test
	public void testGetRapportPrestationsImposablesHistoSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600003); // Mario Gomez
		params.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		// Récupération de l'historique des rapport de travail entre le sourcier et son (ses) employeur(s)
		final List<RelationBetweenParties> rapports = new ArrayList<RelationBetweenParties>();
		for (RelationBetweenParties r : tiers.getRelationsBetweenParties()) {
			if (RelationBetweenPartiesType.TAXABLE_REVENUE == r.getType()) {
				rapports.add(r);
			}
		}
		assertEquals(1, rapports.size());

		final RelationBetweenParties rapport = rapports.get(0);
		assertNotNull(rapport);
		assertEquals(RelationBetweenPartiesType.TAXABLE_REVENUE, rapport.getType()); // autre tiers = débiteur
		assertEquals(1678432L, rapport.getOtherPartyNumber()); // 1678432 = N° débiteur
		assertSameDay(newDate(2008, 1, 29), rapport.getDateFrom());
		assertNull(rapport.getDateTo());
		assertEquals(ActivityType.MAIN, rapport.getActivityType());
		assertEquals(Integer.valueOf(100), rapport.getActivityRate()); // 100%
	}

	@Test
	public void testGetDebiteur() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.DEBTOR_PERIODICITIES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);
		assertEquals(DebtorCategory.REGULAR, debiteur.getCategory());
		assertEquals(CommunicationMode.PAPER, debiteur.getCommunicationMode());
		assertEquals("Café du Commerce", debiteur.getComplementaryName());

		final List<DebtorPeriodicity> periodicites = debiteur.getPeriodicities();
		assertNotNull(periodicites);
		assertEquals(1, periodicites.size());

		final DebtorPeriodicity periodicite0 = periodicites.get(0);
		assertEquals(WithholdingTaxDeclarationPeriodicity.QUARTERLY, periodicite0.getPeriodicity());
	}

	@Test
	public void testGetActiviteDebiteur() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		final List<TaxResidence> fors = debiteur.getMainTaxResidences();
		assertNotNull(fors);
		assertEquals(2, fors.size());

		// Ancienne activité sur Renens
		final TaxResidence for0 = fors.get(0);
		assertNotNull(for0);
		assertEquals(5591, for0.getTaxationAuthorityFSOId()); // Renens
		assertSameDay(newDate(2002, 7, 1), for0.getDateFrom()); // = date début d'activité
		assertSameDay(newDate(2002, 12, 31), for0.getDateTo()); // = date fin d'activité

		// Nouvelle activité sur Lausanne
		final TaxResidence for1 = fors.get(1);
		assertNotNull(for1);
		assertEquals(5586, for1.getTaxationAuthorityFSOId()); // Lausanne
		assertSameDay(newDate(2003, 3, 1), for1.getDateFrom());
		assertNull(for1.getDateTo());
	}

	@Test
	public void testGetPeriodiciteDebiteur() throws Exception {
		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.DEBTOR_PERIODICITIES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		final List<DebtorPeriodicity> periodicites = debiteur.getPeriodicities();
		assertNotNull(periodicites);
		assertEquals(1, periodicites.size());
		DebtorPeriodicity p1 = periodicites.get(0);
		assertSameDay(newDate(2002, 7, 1), p1.getDateFrom());
		assertEquals(WithholdingTaxDeclarationPeriodicity.QUARTERLY, p1.getPeriodicity());
	}

	private static int index(Date date) {
		return date.getYear() * 10000 + date.getMonth() * 100 + date.getDay();
	}

	private static boolean isBeforeOrEquals(Date left, Date right) {
		return index(left) <= index(right);
	}

	private static boolean isTiersActif(Party tiers, Date date) {

		boolean estActif = false;
		for (TaxResidence f : tiers.getMainTaxResidences()) {
			if (isBeforeOrEquals(f.getDateFrom(), date)
					&& (f.getDateTo() == null || isBeforeOrEquals(date, f.getDateTo()))) {
				estActif = true;
				break;
			}
		}

		return estActif;
	}

	@Test
	public void testGetActiviteAUneDateDonneeDebiteur() throws Exception {

		final Date _2000_01_01 = new Date();
		_2000_01_01.setYear(2000);
		_2000_01_01.setMonth(1);
		_2000_01_01.setDay(1);

		final Date _2002_07_01 = new Date();
		_2002_07_01.setYear(2002);
		_2002_07_01.setMonth(7);
		_2002_07_01.setDay(1);

		final Date _2003_04_01 = new Date();
		_2003_04_01.setYear(2003);
		_2003_04_01.setMonth(4);
		_2003_04_01.setDay(1);

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		assertFalse(isTiersActif(debiteur, _2000_01_01));
		assertTrue(isTiersActif(debiteur, _2002_07_01)); // à Renens
		assertTrue(isTiersActif(debiteur, _2003_04_01)); // à Lausanne
	}

	@Test
	public void testSearchPersonneParNomEtDateNaissance() throws Exception {

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setSearchMode(SearchMode.CONTAINS);
		params.setContactName("Pirez");
		params.setDateOfBirth(newDate(1971, 1, 23));

		final SearchPartyResponse resultat = service.searchParty(params);
		assertNotNull(resultat);
		assertEquals(1, resultat.getItems().size());

		final PartyInfo info = resultat.getItems().get(0);
		assertEquals(12600001L, info.getNumber());
		assertEquals(PartyType.NATURAL_PERSON, info.getType());
	}

	@Test
	public void testGetContribuablePrincipalAvecHistorique() throws Exception {

		// Récupération du ménage commun à partir du contribuable

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600003); // Mario Gomez
		params.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);

		// Récupération du tiers
		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RelationBetweenParties> rapports = tiers.getRelationsBetweenParties();
		assertNotNull(rapports);

		RelationBetweenParties rapportMenage = null;
		for (RelationBetweenParties r : rapports) {
			if (RelationBetweenPartiesType.HOUSEHOLD_MEMBER == r.getType() && r.getDateTo() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		// Récupération du contribuable principal (au sens 'source') du ménage commun

		// Note: l'information 'contribuable principal' n'a de sens et n'est renseignée que sur un ménage commun
		GetPartyRequest paramsMenage = new GetPartyRequest();
		paramsMenage.setLogin(login);
		paramsMenage.setPartyNumber(rapportMenage.getOtherPartyNumber());
		paramsMenage.getParts().add(PartyPart.FAMILY_STATUSES);

		final CommonHousehold menage = (CommonHousehold) service.getParty(paramsMenage);
		assertNotNull(menage);

		// Recherche du contribuable principal du ménage commun (avec historique)
		final List<FamilyStatus> situations = menage.getFamilyStatuses();
		assertNotNull(situations);
		assertEquals(1, situations.size());

		final FamilyStatus situation = situations.get(0);
		assertNotNull(situation);
		assertEquals(Integer.valueOf(12600003), situation.getMainTaxpayerNumber());
	}

	@Test
	public void testGetContribuableAssocieAuDebiteur() throws Exception {

		// Recherche débiteur
		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		// Recherche du contribuable associé
		final long contribuableId = debiteur.getAssociatedTaxpayerNumber();
		assertEquals(43308102L, contribuableId);

		params.setPartyNumber((int) contribuableId);

		final NaturalPerson personne = (NaturalPerson) service.getParty(params);
		assertNotNull(personne);

		final PersonIdentification identification = personne.getIdentification();
		assertNotNull(identification);

		assertEquals("Sabri Inanç", identification.getFirstName());
		assertEquals("Ertem", trimValiPattern(identification.getOfficialName()));
		assertEquals("1", identification.getSex());
	}

	@Test
	public void testGetNoTelephoneEtEmailPersonneDeContactDebiteur() throws Exception {

		// Recherche débiteur
		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		assertEquals("0213332211", debiteur.getBusinessPhoneNumber());
		assertEquals("0213332212", debiteur.getPrivatePhoneNumber());
		assertEquals("0213332213", debiteur.getFaxNumber());
		assertEquals("0790001234", debiteur.getMobilePhoneNumber());
		assertEquals("sabri@cafeducommerce.ch", debiteur.getEmailAddress());
	}

	@Test
	public void testGetPeriodiciteDeclarationDebiteurAvecHistorique() throws Exception {

		// Recherche débiteur
		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.TAX_DECLARATIONS);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		// Récupère l'historique des déclarations
		final List<TaxDeclaration> declarations = debiteur.getTaxDeclarations();
		assertNotNull(declarations);
		assertEquals(2, declarations.size());

		final WithholdingTaxDeclaration lr0 = (WithholdingTaxDeclaration) declarations.get(0);
		assertNotNull(lr0);
		assertEquals(1, lr0.getId());
		assertEquals(WithholdingTaxDeclarationPeriodicity.QUARTERLY, lr0.getPeriodicity());

		final WithholdingTaxDeclaration lr1 = (WithholdingTaxDeclaration) declarations.get(1);
		assertNotNull(lr1);
		assertEquals(5, lr1.getId());
		assertEquals(WithholdingTaxDeclarationPeriodicity.QUARTERLY, lr1.getPeriodicity());
	}

	@Test
	public void testGetModeCommunicationDebiteur() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		// Mode communication
		assertEquals(CommunicationMode.PAPER, debiteur.getCommunicationMode());
	}

	@Test
	public void testGetModeCommunicationHistoDebiteur() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.TAX_DECLARATIONS);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		final List<TaxDeclaration> declarations = debiteur.getTaxDeclarations();
		assertNotNull(declarations);
		assertEquals(2, declarations.size());

		final WithholdingTaxDeclaration dis_2008_1er_trimestre = (WithholdingTaxDeclaration) declarations.get(0);
		final WithholdingTaxDeclaration dis_2008_2eme_trimestre = (WithholdingTaxDeclaration) declarations.get(1);

		// Mode communication
		assertEquals(CommunicationMode.PAPER, dis_2008_1er_trimestre.getCommunicationMode()); // 1er trimestre 2008
		assertEquals(CommunicationMode.PAPER, dis_2008_2eme_trimestre.getCommunicationMode());// 2ème trimestre 2008
		assertEquals(CommunicationMode.PAPER, debiteur.getCommunicationMode());// état courant
	}

	@Test
	public void testGetAdresseEnvoiDebiteur() throws Exception {

		// Recherche débiteur
		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.ADDRESSES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		/**
		 * <pre>
		 * Monsieur
		 * Sabri Inanç Ertem
		 * Café du Commerce
		 * Avenue de Beaulieu 12
		 * 1004 Lausanne Secteur de dist.
		 * </pre>
		 */
		final List<Address> courriers = debiteur.getMailAddresses();
		final Address courrier = courriers.get(courriers.size()-1);
		final FormattedAddress formattee = courrier.getFormattedAddress();
		assertNotNull(formattee);
		assertEquals("Sabri Inanç Ertem", formattee.getLine1());
		assertEquals("Café du Commerce", formattee.getLine2());
		assertEquals("Avenue de Beaulieu 12", formattee.getLine3());
		assertEquals("1004 Lausanne", formattee.getLine4());
		assertNull(formattee.getLine5());
		assertNull(formattee.getLine6());
		assertEquals(TariffZone.SWITZERLAND, courrier.getAddressInformation().getTariffZone());
	}

	@Test
	public void testGetAdressesDebiteur() throws Exception {

		// Recherche débiteur
		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.ADDRESSES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		/**
		 * <pre>
		 * Avenue de Beaulieu 12
		 * 1004 Lausanne Secteur de dist.
		 * </pre>
		 */
		final List<Address> adressesCourrier = debiteur.getMailAddresses();
		assertNotNull(adressesCourrier);
		assertEquals(2, adressesCourrier.size());

		final Address addressCourrier0 = adressesCourrier.get(0);
		assertNotNull(addressCourrier0);
		assertNull(addressCourrier0.getDateFrom());
		assertEquals(newDate(2002, 6, 30), addressCourrier0.getDateTo());

		final AddressInformation info0 = addressCourrier0.getAddressInformation();
		assertNotNull(info0);
		assertEquals("Rue de Lausanne 59", info0.getStreet());
		assertNull(info0.getHouseNumber());
		assertEquals(Long.valueOf(1028), info0.getSwissZipCode());
		assertEquals("Préverenges", info0.getTown());
		assertNull(info0.getPostOfficeBoxNumber());
		assertNull(info0.getPostOfficeBoxText());
		assertNull(info0.getDwellingNumber());
		assertNull(info0.getStreetId());
		assertEquals(Integer.valueOf(175), info0.getSwissZipCodeId());

		final Address addressCourrier1 = adressesCourrier.get(1);
		assertNotNull(addressCourrier1);
		assertEquals(newDate(2002, 7, 1), addressCourrier1.getDateFrom());
		assertNull(addressCourrier1.getDateTo());

		final AddressInformation info1 = addressCourrier1.getAddressInformation();
		assertNotNull(info1);
		assertEquals("Avenue de Beaulieu", info1.getStreet());
		assertEquals("12", info1.getHouseNumber());
		assertEquals(Long.valueOf(1004), info1.getSwissZipCode());
		assertEquals("Lausanne", info1.getTown());
		assertNull(info1.getPostOfficeBoxNumber());
		assertNull(info1.getPostOfficeBoxText());
		assertNull(info1.getDwellingNumber());
		assertEquals(Integer.valueOf(30387), info1.getStreetId());
		assertEquals(Integer.valueOf(151), info1.getSwissZipCodeId());

		// ... aussi disponibles :
		final List<Address> addressDomicile = debiteur.getResidenceAddresses();
		assertNotNull(addressDomicile);
		assertFalse(addressDomicile.isEmpty());

		final List<Address> addressPoursuite = debiteur.getDebtProsecutionAddresses();
		assertNotNull(addressPoursuite);
		assertFalse(addressPoursuite.isEmpty());

		final List<Address> addressRepresentation = debiteur.getRepresentationAddresses();
		assertNotNull(addressRepresentation);
		assertFalse(addressRepresentation.isEmpty());
	}

	@Test
	public void testGetNumeroOfsCommuneDebiteurAvecHistorique() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		final List<TaxResidence> fors = debiteur.getMainTaxResidences();
		assertNotNull(fors);
	}

	@Test
	public void testGetForDebiteur() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(1678432); // Café du Commerce
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		final List<TaxResidence> fors = debiteur.getMainTaxResidences();
		assertNotNull(fors);
		assertEquals(2, fors.size());

		final TaxResidence for0 = fors.get(0);
		assertNotNull(for0);
		assertSameDay(newDate(2002, 7, 1), for0.getDateFrom());
		assertSameDay(newDate(2002, 12, 31), for0.getDateTo());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, for0.getTaxationAuthorityType());
		assertEquals(5591, for0.getTaxationAuthorityFSOId()); // Renens VD

		final TaxResidence for1 = fors.get(1);
		assertNotNull(for1);
		assertSameDay(newDate(2003, 3, 1), for1.getDateFrom());
		assertNull(for1.getDateTo());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, for1.getTaxationAuthorityType());
		assertEquals(5586, for1.getTaxationAuthorityFSOId()); // Lausanne
	}

	@Test
	public void testGetModeImpositionSourcier() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600001); // Isidor Pirez
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		// Récupération de l'historique des fors fiscaux principaux du sourcier
		final List<TaxResidence> forsPrincipaux = tiers.getMainTaxResidences();
		assertNotNull(forsPrincipaux);
		assertEquals(1, forsPrincipaux.size());

		final TaxResidence forPrincipal = forsPrincipaux.get(0);
		assertNotNull(forPrincipal);
		assertEquals(TaxationMethod.MIXED_137_1, forPrincipal.getTaxationMethod());
	}

	@Test
	public void testGetNombreEnfantsAvecHistoriqueSurContribuableSeul() throws Exception {

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12300003); // Emery Lyah
		params.getParts().add(PartyPart.FAMILY_STATUSES);

		// Récupération du tiers
		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		// Recherche de la situation de famille courante du tiers
		final List<FamilyStatus> situationsFamille = tiers.getFamilyStatuses();
		assertNotNull(situationsFamille);
		assertEquals(1, situationsFamille.size());

		final FamilyStatus situationFamille = situationsFamille.get(0);
		assertNotNull(situationFamille);
		assertEquals(new Integer(1), situationFamille.getNumberOfChildren());
	}

	@Test
	public void testGetNombreEnfantsAvecHistoriqueSurContribuableMarie() throws Exception {

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(12600003); // Mario Gomez
		params.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);

		// Récupération du tiers
		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RelationBetweenParties> rapports = tiers.getRelationsBetweenParties();
		assertNotNull(rapports);

		RelationBetweenParties rapportMenage = null;
		for (RelationBetweenParties r : rapports) {
			if (RelationBetweenPartiesType.HOUSEHOLD_MEMBER == r.getType() && r.getDateTo() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		GetPartyRequest paramsHisto = new GetPartyRequest();
		paramsHisto.setLogin(login);
		paramsHisto.setPartyNumber(rapportMenage.getOtherPartyNumber());
		paramsHisto.getParts().add(PartyPart.FAMILY_STATUSES);

		final CommonHousehold menage = (CommonHousehold) service.getParty(paramsHisto);
		assertNotNull(menage);

		// Recherche de l'historique de la situation de famille du tiers ménage (ici, il n'y en a qu'une)
		final List<FamilyStatus> situations = menage.getFamilyStatuses();
		assertEquals(1, situations.size());

		final FamilyStatus situationFamille = situations.get(0);
		assertNotNull(situationFamille);
		assertEquals(new Integer(2), situationFamille.getNumberOfChildren());
	}

	// Récupération de la déclaration d'impôt source suite à la réception de l'événement fiscal OUVERTURE_PERIODE_DECOMPTE_LR
	@Test
	public void testGetDeclarationImpotSourceDepuisEvenementFiscal() throws Exception {

		// Données contenue dans l'événement fiscal reçu
		final Long numeroCtb = 1678432L;
		final Date dateEvenement = newDate(2008, 4, 1);

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(numeroCtb.intValue()); // Café du Commerce
		params.getParts().add(PartyPart.TAX_DECLARATIONS);

		final Debtor debiteur = (Debtor) service.getParty(params);
		assertNotNull(debiteur);

		final List<TaxDeclaration> declarations = debiteur.getTaxDeclarations();
		assertNotNull(declarations);

		WithholdingTaxDeclaration declaration = null;
		for (TaxDeclaration d : declarations) {
			WithholdingTaxDeclaration dis = (WithholdingTaxDeclaration) d;
			if (within(dateEvenement, dis.getDateFrom(), dis.getDateTo())) {
				declaration = dis;
				break;
			}
		}
		assertNotNull(declaration);
		assertSameDay(newDate(2008, 4, 1), declaration.getDateFrom());
		assertSameDay(newDate(2008, 6, 30), declaration.getDateTo());
	}

	/**
	 * [UNIREG-1253] Test qu'un contribuable vaudois qui part HS en milieu d'année et garde un immeuble sur sol vaudois possède bien une période d'imposition couvrant toute l'année
	 */
	@Test
	public void testGetHistoPeriodeImpositionContribuablePartiHSAvecImmeubleVD() throws Exception {

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(10184458); // Michel Dupont
		params.getParts().add(PartyPart.TAXATION_PERIODS);

		// Récupération du tiers
		final NaturalPerson tiers = (NaturalPerson) service.getParty(params);
		assertNotNull(tiers);

		// Vérifie qu'il possède une seul période fiscale pour 2008, malgré son départ HS (car il garde un immeuble)
		final List<TaxationPeriod> periodes = tiers.getTaxationPeriods();
		assertEquals(3, periodes.size()); // 2006, 2007 et 2008

		final TaxationPeriod p2006 = periodes.get(0);
		assertNotNull(p2006);
		assertSameDay(newDate(2006, 1, 1), p2006.getDateFrom());
		assertSameDay(newDate(2006, 12, 31), p2006.getDateTo());

		final TaxationPeriod p2007 = periodes.get(1);
		assertNotNull(p2007);
		assertSameDay(newDate(2007, 1, 1), p2007.getDateFrom());
		assertSameDay(newDate(2007, 12, 31), p2007.getDateTo());

		final TaxationPeriod p2008 = periodes.get(2);
		assertNotNull(p2008);
		assertSameDay(newDate(2008, 1, 1), p2008.getDateFrom());
		assertSameDay(newDate(2008, 12, 31), p2008.getDateTo());
		assertEquals(Long.valueOf(6), p2008.getTaxDeclarationId());
	}

	/**
	 * [UNIREG-2110]
	 */
	@Test
	public void testGetDebiteurInfoRequest() throws Exception {

		GetDebtorInfoRequest params = new GetDebtorInfoRequest();
		params.setLogin(login);
		params.setDebtorNumber(1678432); // débiteur trimestriel

		// 2008
		params.setTaxPeriod(2008);
		final DebtorInfo info2008 = service.getDebtorInfo(params);
		assertNotNull(info2008);
		assertEquals(1678432L, info2008.getNumber());
		assertEquals(2008, info2008.getTaxPeriod());
		assertEquals(4, info2008.getTheoreticalNumberOfWithholdingTaxDeclarations());
		assertEquals(2, info2008.getNumberOfWithholdingTaxDeclarationsIssued()); // deux LRs émises : 2008010->20080331 et 20080401->20080630

		// 2009
		params.setTaxPeriod(2009);
		final DebtorInfo info2009 = service.getDebtorInfo(params);
		assertNotNull(info2009);
		assertEquals(1678432L, info2009.getNumber());
		assertEquals(2009, info2009.getTaxPeriod());
		assertEquals(4, info2009.getTheoreticalNumberOfWithholdingTaxDeclarations());
		assertEquals(0, info2009.getNumberOfWithholdingTaxDeclarationsIssued()); // aucune LR émise
	}

	@Test
	public void testGetDebiteurInfoRequestDebiteurInconnu() throws Exception {

		GetDebtorInfoRequest params = new GetDebtorInfoRequest();
		params.setLogin(login);
		params.setDebtorNumber(1877222); // débiteur inconnu
		params.setTaxPeriod(2008);

		// 2008
		try {
			service.getDebtorInfo(params);
			fail("Le débiteur est inconnu, la méthode aurait dû lever une exception");
		}
		catch (WebServiceException e) {
			assertEquals("Le tiers n°1877222 n'existe pas.", e.getMessage());
		}
	}

	/**
	 * [UNIREG-2302]
	 */
	@Test
	public void testGetAdresseEnvoiPersonneMorale() throws Exception {

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(20222); // la BCV
		params.getParts().add(PartyPart.ADDRESSES);

		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);

		final List<Address> courriers = pm.getMailAddresses();
		final Address courrier = courriers.get(courriers.size() - 1);
		final FormattedAddress adresseEnvoi = courrier.getFormattedAddress();
		assertNotNull(adresseEnvoi);
		assertEquals("Banque Cantonale Vaudo", trimValiPattern(adresseEnvoi.getLine1()));
		assertEquals("", trimValiPattern(adresseEnvoi.getLine2()));
		assertEquals("", trimValiPattern(adresseEnvoi.getLine3()));
		assertEquals("pa Comptabilité financière", trimValiPattern(adresseEnvoi.getLine4()));
		assertEquals("M. Daniel Küffer / CP 300", trimValiPattern(adresseEnvoi.getLine5()));
		assertEquals("1001 Lausanne", adresseEnvoi.getLine6());

		final OrganisationMailAddressInfo organisation = courrier.getOrganisation();
		assertNotNull(organisation);
		assertEquals("Madame, Monsieur", organisation.getFormalGreeting());
		assertEquals("Banque Cantonale Vaudo", trimValiPattern(organisation.getOrganisationName()));
		assertEquals("", trimValiPattern(organisation.getOrganisationNameAddOn1()));
		assertEquals("", trimValiPattern(organisation.getOrganisationNameAddOn2()));

		final AddressInformation info = courrier.getAddressInformation();
		assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
		assertEquals("pa Comptabilité financière", info.getComplementaryInformation());
		assertNull(info.getCareOf());
		assertEquals("M. Daniel Küffer / CP 300", info.getStreet());
		assertNull(info.getPostOfficeBoxNumber());
		assertEquals(Long.valueOf(1001), info.getSwissZipCode());
		assertEquals("Lausanne", info.getTown());
		assertEquals("CH", info.getCountry());
	}

	// [SIFISC-2392] Vérifie que les ids des déclarations sont bien retournés.
	@Test
	public void testGetDeclarationContribuable() throws Exception {

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(86006202); // la BCV
		params.getParts().add(PartyPart.TAX_DECLARATIONS);

		final Taxpayer tp = (Taxpayer) service.getParty(params);
		assertNotNull(tp);

		final List<TaxDeclaration> declarations = tp.getTaxDeclarations();
		assertNotNull(declarations);
		assertEquals(3, declarations.size());

		final TaxDeclaration decl0 = declarations.get(0);
		assertNotNull(decl0);
		assertEquals(2, decl0.getId());
		assertEquals(newDate(2005, 1, 1), decl0.getDateFrom());
		assertEquals(newDate(2005, 12, 31), decl0.getDateTo());

		final TaxDeclaration decl1 = declarations.get(1);
		assertNotNull(decl1);
		assertEquals(3, decl1.getId());
		assertEquals(newDate(2006, 1, 1), decl1.getDateFrom());
		assertEquals(newDate(2006, 12, 31), decl1.getDateTo());

		final TaxDeclaration decl2 = declarations.get(2);
		assertNotNull(decl2);
		assertEquals(4, decl2.getId());
		assertEquals(newDate(2007, 1, 1), decl2.getDateFrom());
		assertEquals(newDate(2007, 12, 31), decl2.getDateTo());

	}

	public static XMLGregorianCalendar regdate2xmlcal(RegDate date) {
		if (date == null) {
			return null;
		}
		return getDataTypeFactory().newXMLGregorianCalendar(date.year(), date.month(), date.day(), 0, 0, 0, 0, DatatypeConstants.FIELD_UNDEFINED);
	}

	private static DatatypeFactory getDataTypeFactory() {
		DatatypeFactory datatypeFactory;
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		}
		catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
		return datatypeFactory;
	}
}

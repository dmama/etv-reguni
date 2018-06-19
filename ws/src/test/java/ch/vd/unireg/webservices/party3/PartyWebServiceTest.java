package ch.vd.unireg.webservices.party3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import ch.ech.ech0044.v2.NamedPersonId;
import ch.ech.ech0044.v2.PersonIdentification;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.common.WebserviceTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.CategorieIdentifiant;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType;
import ch.vd.unireg.xml.party.address.v1.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.adminauth.v1.AdministrativeAuthority;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.debtor.v1.CommunicationMode;
import ch.vd.unireg.xml.party.debtor.v1.DebtorCategory;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.person.v1.CommonHouseholdStatus;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategoryPeriod;
import ch.vd.unireg.xml.party.taxdeclaration.v1.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationKey;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v1.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod;
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
public class PartyWebServiceTest extends WebserviceTest {

	private PartyWebService service;
	private UserLogin login;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(PartyWebService.class, "partyService3Impl");
		login = new UserLogin("iamtestuser", 22);
		serviceCivil.setUp(new DefaultMockServiceCivil());
	}

	/**
	 * [UNIREG-1985] Vérifie que les fors fiscaux virtuels sont bien retournés <b>même si</b> on demande l'adresse d'envoi en même temps.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchPartyWithVirtualTaxResidencesAndFormattedAddresses() throws Exception {

		class Ids {
			long paul;
			long janine;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple normal, assujetti vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateMariage = date(1990, 1, 1);
				final RegDate veilleMariage = dateMariage.getOneDayBefore();

				final ch.vd.unireg.tiers.PersonnePhysique paul = addNonHabitant("Paul", "Duchemin", RegDate.get(1954, 3, 31), ch.vd.unireg.type.Sexe.MASCULIN);
				ids.paul = paul.getNumero();
				addForPrincipal(paul, date(1974, 3, 31), ch.vd.unireg.type.MotifFor.MAJORITE, veilleMariage, ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Lausanne);
				addAdresseSuisse(paul, TypeAdresseTiers.COURRIER, date(1954, 3, 31), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final ch.vd.unireg.tiers.PersonnePhysique janine = addNonHabitant("Janine", "Duchemin", RegDate.get(1954, 3, 31), ch.vd.unireg.type.Sexe.MASCULIN);
				ids.janine = janine.getNumero();
				addForPrincipal(janine, date(1974, 3, 31), ch.vd.unireg.type.MotifFor.MAJORITE, veilleMariage, ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Lausanne);
				addAdresseSuisse(janine, TypeAdresseTiers.COURRIER, date(1954, 3, 31), null, MockRue.Lausanne.AvenueJolimont);

				final ch.vd.unireg.tiers.EnsembleTiersCouple ensemble = addEnsembleTiersCouple(paul, janine, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				return null;
			}
		});

		// Demande de retourner les deux tiers en un seul batch
		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(login);
		params.getPartyNumbers().add((int) ids.paul);
		params.getPartyNumbers().add((int) ids.janine);
		params.getParts().addAll(Arrays.asList(PartyPart.TAX_RESIDENCES, PartyPart.VIRTUAL_TAX_RESIDENCES, PartyPart.ADDRESSES));

		final BatchParty batch = service.getBatchParty(params);
		assertNotNull(batch);
		assertEquals(2, batch.getEntries().size());

		Collections.sort(batch.getEntries(), new Comparator<BatchPartyEntry>() {
			@Override
			public int compare(BatchPartyEntry o1, BatchPartyEntry o2) {
				return Integer.valueOf(o1.getNumber()).compareTo(o2.getNumber());
			}
		});

		// On vérifie les fors fiscaux de Paul, il doit y en avoir 2 dont un virtuel
		final BatchPartyEntry entry0 = batch.getEntries().get(0);
		assertEquals(ids.paul, entry0.getNumber());

		final NaturalPerson paulHisto = (NaturalPerson) entry0.getParty();
		assertNotNull(paulHisto);
		assertEquals(2, paulHisto.getMainTaxResidences().size());

		final TaxResidence for0 = paulHisto.getMainTaxResidences().get(0);
		assertNotNull(for0);
		assertEquals(newDate(1974, 3, 31), for0.getDateFrom());
		assertEquals(newDate(1989, 12, 31), for0.getDateTo());
		assertFalse(for0.isVirtual());

		final TaxResidence for1 = paulHisto.getMainTaxResidences().get(1);
		assertNotNull(for1);
		assertEquals(newDate(1990, 1, 1), for1.getDateFrom());
		assertNull(for1.getDateTo());
		assertTrue(for1.isVirtual()); // il s'agit donc du for du ménage reporté sur la personne physique
	}

	/**
	 * [UNIREG-2227] Cas du contribuable n°237.056.03
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyAddressesWithGuardianOnNaturalPerson() throws Exception {

		class Ids {
			Long jeanpierre;
			Long marie;
			Long menage;
			Long tuteur;
		}
		final Ids ids = new Ids();

		// Crée un couple dont le mari est sous tutelle
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateMariage = date(1976, 1, 1);

				final ch.vd.unireg.tiers.PersonnePhysique jeanpierre = addNonHabitant("Jean-Pierre", "Bürki", RegDate.get(1947, 1, 11), ch.vd.unireg.type.Sexe.MASCULIN);
				ids.jeanpierre = jeanpierre.getNumero();
				addAdresseSuisse(jeanpierre, TypeAdresseTiers.COURRIER, date(1947, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final ch.vd.unireg.tiers.PersonnePhysique marie = addNonHabitant("Marie", "Bürki", RegDate.get(1954, 1, 1), ch.vd.unireg.type.Sexe.FEMININ);
				ids.marie = marie.getNumero();
				addAdresseSuisse(marie, TypeAdresseTiers.COURRIER, date(1954, 1, 11), null, MockRue.Lausanne.AvenueJolimont);

				final ch.vd.unireg.tiers.EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jeanpierre, marie, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final ch.vd.unireg.tiers.PersonnePhysique tuteur = addNonHabitant("Jacky", "Rod", RegDate.get(1947, 1, 1), ch.vd.unireg.type.Sexe.MASCULIN);
				ids.tuteur = tuteur.getNumero();
				addAdresseSuisse(tuteur, TypeAdresseTiers.COURRIER, date(1947, 1, 11), null, MockRue.Lausanne.BoulevardGrancy);

				addTutelle(jeanpierre, tuteur, null, date(2007, 9, 11), null);

				return null;
			}
		});

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(ids.menage.intValue());
			params.getParts().addAll(Arrays.asList(PartyPart.ADDRESSES, PartyPart.ADDRESSES));

			final CommonHousehold menage = (CommonHousehold) service.getParty(params);
			assertNotNull(menage);

			assertNotNull(menage.getMailAddresses());
			assertEquals(2, menage.getMailAddresses().size());
			assertAddress(new Date(1947, 1, 1), new Date(2007, 9, 10), "Avenue de Beaulieu", "Lausanne", menage.getMailAddresses().get(0)); // adresse de monsieur
			assertAddress(new Date(2007, 9, 11), null, "Avenue Jolimont", "Lausanne", menage.getMailAddresses().get(1)); // adresse de madame (puisque monsieur est sous tutelle)

			assertNotNull(menage.getDebtProsecutionAddresses());
			assertEquals(1, menage.getDebtProsecutionAddresses().size());
			assertAddress(new Date(1947, 1, 1), null, "Avenue de Beaulieu", "Lausanne",
					menage.getDebtProsecutionAddresses().get(0)); // adresse de monsieur (non-impacté par la tutelle, car pas d'autorité tutelaire renseignée)

			assertEmpty(menage.getDebtProsecutionAddressesOfOtherParty()); // [UNIREG-2227] pas d'adresse autre tiers car madame remplace monsieur dans la gestion du ménage
		}
	}

	/**
	 * [UNIREG-3203] Cas du contribuable n°497.050.02
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyAddressesWithLegalCounselorOnParty() throws Exception {

		class Ids {
			Long jeandaniel;
			Long myriam;
			Long menage;
			Long conseiller;
		}
		final Ids ids = new Ids();

		// Crée un couple dont le mari est sous tutelle
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateMariage = date(1976, 1, 1);

				final ch.vd.unireg.tiers.PersonnePhysique jeandaniel = addNonHabitant("Jean-Daniel", "Guex-Martin", RegDate.get(1947, 1, 11), ch.vd.unireg.type.Sexe.MASCULIN);
				ids.jeandaniel = jeandaniel.getNumero();
				addAdresseSuisse(jeandaniel, TypeAdresseTiers.COURRIER, date(1947, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final ch.vd.unireg.tiers.PersonnePhysique myriam = addNonHabitant("Myriam", "Guex-Martin", RegDate.get(1954, 1, 1), ch.vd.unireg.type.Sexe.FEMININ);
				ids.myriam = myriam.getNumero();
				addAdresseSuisse(myriam, TypeAdresseTiers.COURRIER, date(1954, 1, 11), null, MockRue.Lausanne.AvenueJolimont);

				final ch.vd.unireg.tiers.EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jeandaniel, myriam, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final ch.vd.unireg.tiers.PersonnePhysique conseiller = addNonHabitant("Philippe", "Rossy", RegDate.get(1947, 1, 1), ch.vd.unireg.type.Sexe.MASCULIN);
				ids.conseiller = conseiller.getNumero();
				addAdresseSuisse(conseiller, TypeAdresseTiers.COURRIER, date(1947, 1, 11), null, MockRue.Lausanne.BoulevardGrancy);

				addConseilLegal(jeandaniel, conseiller, date(2007, 9, 11), null);

				return null;
			}
		});

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(ids.menage.intValue());
			params.getParts().addAll(Collections.singletonList(PartyPart.ADDRESSES));

			final CommonHousehold menage = (CommonHousehold) service.getParty(params);
			assertNotNull(menage);

			assertNotNull(menage.getMailAddresses());
			assertEquals(2, menage.getMailAddresses().size());
			assertAddress(new Date(1947, 1, 1), new Date(2007, 9, 10), "Avenue de Beaulieu", "Lausanne", menage.getMailAddresses().get(0)); // adresse de monsieur
			assertAddress(new Date(2007, 9, 11), null, "Avenue Jolimont", "Lausanne", menage.getMailAddresses().get(1)); // adresse de madame (puisque monsieur est sous conseil légal)

			assertNotNull(menage.getDebtProsecutionAddresses());
			assertEquals(1, menage.getDebtProsecutionAddresses().size());
			assertAddress(new Date(1947, 1, 1), null, "Avenue de Beaulieu", "Lausanne", menage.getDebtProsecutionAddresses().get(0)); // adresse de monsieur (non-impacté par le conseil légal)

			assertEmpty(menage.getDebtProsecutionAddressesOfOtherParty()); // [UNIREG-2227] pas d'adresse autre tiers car madame remplace monsieur dans la gestion du ménage
		}
	}

	/**
	 * [UNIREG-2227] Cas du contribuable n°100.864.90 : on s'assure que la source de l'adresse 'poursuite autre tiers' est bien CURATELLE dans le cas d'une curatelle dont les adresses de début et de fin
	 * sont nulles.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyAddressesWithWelfareAdvocateOnParty() throws Exception {

		final long noIndividuTiia = 339619;
		final long noIndividuSylvie = 339618;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu tiia = addIndividu(noIndividuTiia, date(1989, 12, 21), "Tauxe", "Tiia", false);
				addAdresse(tiia, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(tiia, TypeAdresseCivil.COURRIER, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(tiia, TypeAdresseCivil.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(tiia, TypeAdresseCivil.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(tiia, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
				addAdresse(tiia, TypeAdresseCivil.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);

				MockIndividu sylvie = addIndividu(noIndividuSylvie, date(1955, 9, 19), "Tauxe", "Sylvie", false);
				addAdresse(sylvie, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(sylvie, TypeAdresseCivil.COURRIER, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(sylvie, TypeAdresseCivil.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(sylvie, TypeAdresseCivil.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(sylvie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
				addAdresse(sylvie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
			}
		});

		class Ids {
			long tiia;
			long sylvie;
		}
		final Ids ids = new Ids();

		// pas de validation : pour permettre l'ajout d'une curatelle avec date de début nulle
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				ch.vd.unireg.tiers.PersonnePhysique tiia = addHabitant(noIndividuTiia);
				addAdresseSuisse(tiia, TypeAdresseTiers.COURRIER, date(2009, 7, 8), null, MockRue.Lausanne.PlaceSaintFrancois);
				ids.tiia = tiia.getId();
				ch.vd.unireg.tiers.PersonnePhysique sylvie = addHabitant(noIndividuSylvie);
				ids.sylvie = sylvie.getId();
				addCuratelle(tiia, sylvie, null, null);
				return null;
			}
		});

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.tiia);
			params.getParts().addAll(Collections.singletonList(PartyPart.ADDRESSES));

			final NaturalPerson tiia = (NaturalPerson) service.getParty(params);
			assertNotNull(tiia);

			assertNotNull(tiia.getMailAddresses());
			assertEquals(4, tiia.getMailAddresses().size());
			assertAddress(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.getMailAddresses().get(0));
			assertAddress(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.getMailAddresses().get(1));
			assertAddress(new Date(2009, 2, 1), new Date(2009, 7, 7), "Place Saint-François", "Lausanne", tiia.getMailAddresses().get(2));
			assertAddress(new Date(2009, 7, 8), null, "Place Saint-François", "Lausanne",
					tiia.getMailAddresses().get(3)); // [UNIREG-3025] les adresses surchargées priment sur toutes les autres adresses

			assertNotNull(tiia.getRepresentationAddresses());
			assertEquals(3, tiia.getRepresentationAddresses().size());
			assertAddress(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.getRepresentationAddresses().get(0));
			assertAddress(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.getRepresentationAddresses().get(1));
			assertAddress(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.getRepresentationAddresses().get(2));

			assertNotNull(tiia.getResidenceAddresses());
			assertEquals(3, tiia.getResidenceAddresses().size());
			assertAddress(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.getResidenceAddresses().get(0));
			assertAddress(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.getResidenceAddresses().get(1));
			assertAddress(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.getResidenceAddresses().get(2));

			assertNotNull(tiia.getDebtProsecutionAddresses());
			assertEquals(3, tiia.getDebtProsecutionAddresses().size());
			assertAddress(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.getDebtProsecutionAddresses().get(0));
			assertAddress(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.getDebtProsecutionAddresses().get(1));
			assertAddress(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.getDebtProsecutionAddresses().get(2));

			final List<AddressOtherParty> debtProsecutionAddressesOfOtherParty = tiia.getDebtProsecutionAddressesOfOtherParty();
			assertNotNull(debtProsecutionAddressesOfOtherParty);
			assertEquals(3, debtProsecutionAddressesOfOtherParty.size());
			assertAddress(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", debtProsecutionAddressesOfOtherParty.get(0).getBase());
			assertEquals(OtherPartyAddressType.WELFARE_ADVOCATE, debtProsecutionAddressesOfOtherParty.get(0).getOtherPartyType());
			assertAddress(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", debtProsecutionAddressesOfOtherParty.get(1).getBase());
			assertEquals(OtherPartyAddressType.WELFARE_ADVOCATE, debtProsecutionAddressesOfOtherParty.get(1).getOtherPartyType());
			assertAddress(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", debtProsecutionAddressesOfOtherParty.get(2).getBase());
			assertEquals(OtherPartyAddressType.WELFARE_ADVOCATE, debtProsecutionAddressesOfOtherParty.get(2).getOtherPartyType());
		}
	}

	/**
	 * [UNIREG-2782] vérification que le quittancement d'une DI pour laquelle l'état "EMIS" n'a pas été créé fonctionne quand-même
	 */
	@Test
	public void testTaxDeclarationReturnWithoutSentStatus() throws Exception {

		class Ids {
			long ppId;
			long diId;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus transactionStatus) throws Exception {

				final ch.vd.unireg.tiers.PersonnePhysique pp = addNonHabitant("Albus", "Dumbledore", date(1957, 10, 3), ch.vd.unireg.type.Sexe.MASCULIN);
				addForPrincipal(pp, date(2009, 1, 1), ch.vd.unireg.type.MotifFor.ARRIVEE_HS, MockCommune.Croy);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument modele = addModeleDocument(ch.vd.unireg.type.TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				assertNull(di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMIS));

				final EtatDeclaration retour = new EtatDeclarationRetournee(RegDate.get(), "TEST");
				di.addEtat(retour);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// quittancement de la DI
		final AcknowledgeTaxDeclarationRequest demande = new AcknowledgeTaxDeclarationRequest();
		demande.setAcknowledgeDate(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get()));
		demande.setKey(new OrdinaryTaxDeclarationKey());
		demande.getKey().setTaxpayerNumber((int) ids.ppId);
		demande.getKey().setSequenceNumber(1);
		demande.getKey().setTaxPeriod(2009);

		final AcknowledgeTaxDeclarationsRequest quittances = new AcknowledgeTaxDeclarationsRequest();
		quittances.setLogin(login);
		quittances.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse reponse = service.acknowledgeTaxDeclarations(quittances);
		assertNotNull(reponse);

		final List<AcknowledgeTaxDeclarationResponse> retours = reponse.getResponses();
		assertNotNull(retours);
		assertEquals(1, retours.size());

		final AcknowledgeTaxDeclarationResponse retour = retours.get(0);
		assertNotNull(retour);
		assertEquals(ids.ppId, retour.getKey().getTaxpayerNumber());
		assertEquals(1, retour.getKey().getSequenceNumber());
		assertEquals(2009, retour.getKey().getTaxPeriod());
		assertEquals(TaxDeclarationAcknowledgeCode.OK, retour.getCode());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertNotNull(di.getDernierEtatDeclaration());
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, di.getDernierEtatDeclaration().getEtat());
				return null;
			}
		});
	}

	// [UNIREG-3179] si un tiers ne valide pas au milieu du lot, tout explose
	@Test
	public void testTaxDeclarationReturnOnInvalidTaxpayer() throws Exception {

		class Ids {
			long ppId;
			long diId;
		}
		final Ids ids = new Ids();

		final int annee = 2010;

		// on désactive la validation pour pouvoir sauver un tiers au moins qui ne valide pas...
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final RegDate debutAnnee = date(annee, 1, 1);
				final RegDate finAnnee = date(annee, 12, 31);
				final RegDate venteImmeuble = finAnnee.addMonths(-3);

				final ch.vd.unireg.tiers.PersonnePhysique pp = addNonHabitant(null, "Anonyme", date(1980, 10, 25), ch.vd.unireg.type.Sexe.MASCULIN);
				pp.setNom(null);        // <-- c'est là le problème de validation

				addForPrincipal(pp, debutAnnee, ch.vd.unireg.type.MotifFor.ACHAT_IMMOBILIER, MockPays.Allemagne);
				addForSecondaire(pp, debutAnnee, ch.vd.unireg.type.MotifFor.ACHAT_IMMOBILIER, venteImmeuble, ch.vd.unireg.type.MotifFor.VENTE_IMMOBILIER,
						MockCommune.Aigle, ch.vd.unireg.type.MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(ch.vd.unireg.type.TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);

				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, debutAnnee, venteImmeuble, TypeContribuable.HORS_SUISSE, md);
				final RegDate dateEmission = date(annee + 1, 1, 11);
				di.addEtat(new EtatDeclarationEmise(dateEmission));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
				delai.setDateTraitement(dateEmission);
				delai.setDelaiAccordeAu(date(annee + 1, 6, 30));
				di.addDelai(delai);

				ids.ppId = pp.getNumero();
				ids.diId = di.getId();

				return null;
			}
		});

		// quittancement de la DI
		final AcknowledgeTaxDeclarationRequest demande = new AcknowledgeTaxDeclarationRequest();
		demande.setAcknowledgeDate(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get()));
		demande.setKey(new OrdinaryTaxDeclarationKey());
		demande.getKey().setTaxpayerNumber((int) ids.ppId);
		demande.getKey().setSequenceNumber(1);
		demande.getKey().setTaxPeriod(annee);

		final AcknowledgeTaxDeclarationsRequest quittances = new AcknowledgeTaxDeclarationsRequest();
		quittances.setLogin(login);
		quittances.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse reponse = service.acknowledgeTaxDeclarations(quittances);
		assertNotNull(reponse);

		final List<AcknowledgeTaxDeclarationResponse> retours = reponse.getResponses();
		assertNotNull(retours);
		assertEquals(1, retours.size());

		final AcknowledgeTaxDeclarationResponse retour = retours.get(0);
		assertNotNull(retour);
		assertEquals(ids.ppId, retour.getKey().getTaxpayerNumber());
		assertEquals(1, retour.getKey().getSequenceNumber());
		assertEquals(annee, retour.getKey().getTaxPeriod());
		assertEquals(TaxDeclarationAcknowledgeCode.EXCEPTION, retour.getCode());

		final ServiceExceptionInfo exceptionInfo = retour.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		final String expectedMessage = String.format("PersonnePhysique #%d - 1 erreur(s) - 0 avertissement(s):\n [E] Le nom est un attribut obligatoire pour un non-habitant\n", ids.ppId);
		assertEquals(expectedMessage, exceptionInfo.getMessage());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertNotNull(di.getDernierEtatDeclaration());
				assertEquals(TypeEtatDocumentFiscal.EMIS, di.getDernierEtatDeclaration().getEtat());
				return null;
			}
		});
	}

	@Test
	public void testMultipleTaxDeclarationReturnWithOneTaxpayerWithoutSentStatus() throws Exception {

		final int annee = 2010;
		final RegDate debutAnnee = date(annee, 1, 1);
		final RegDate finAnnee = date(annee, 12, 31);
		final RegDate venteImmeuble = finAnnee.addMonths(-3);

		class Ids {
			final long idCtb;
			final long idDi;

			Ids(long idCtb, long idDi) {
				this.idCtb = idCtb;
				this.idDi = idDi;
			}
		}
		final List<Ids> liste = new ArrayList<>();

		// on désactive la validation pour pouvoir sauver un tiers au moins qui ne valide pas...
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(ch.vd.unireg.type.TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);

				final ch.vd.unireg.tiers.PersonnePhysique validePP = addPersonnePhysiqueAvecFor("Alfred", "de Montauban", date(1980, 10, 25), ch.vd.unireg.type.Sexe.MASCULIN);
				final ch.vd.unireg.tiers.PersonnePhysique invalidePP = addPersonnePhysiqueAvecFor(null, null, date(1986, 12, 5), ch.vd.unireg.type.Sexe.FEMININ);

				final DeclarationImpotOrdinaire valideDi = addDi(validePP, debutAnnee, venteImmeuble, pf, md);
				final DeclarationImpotOrdinaire invalideDi = addDi(invalidePP, debutAnnee, venteImmeuble, pf, md);

				liste.add(new Ids(validePP.getNumero(), valideDi.getId()));
				liste.add(new Ids(invalidePP.getNumero(), invalideDi.getId()));
				return null;
			}

			private ch.vd.unireg.tiers.PersonnePhysique addPersonnePhysiqueAvecFor(@Nullable String prenom, @Nullable String nom, RegDate dateNaissance, ch.vd.unireg.type.Sexe sexe) {
				final ch.vd.unireg.tiers.PersonnePhysique pp = addNonHabitant(prenom, nom, dateNaissance, sexe);
				addForPrincipal(pp, debutAnnee, ch.vd.unireg.type.MotifFor.ACHAT_IMMOBILIER, MockPays.Allemagne);
				addForSecondaire(pp, debutAnnee, ch.vd.unireg.type.MotifFor.ACHAT_IMMOBILIER, venteImmeuble, ch.vd.unireg.type.MotifFor.VENTE_IMMOBILIER,
						MockCommune.Aigle, ch.vd.unireg.type.MotifRattachement.IMMEUBLE_PRIVE);
				return pp;
			}

			private DeclarationImpotOrdinairePP addDi(ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques ctb, RegDate dateDebut, RegDate dateFin, PeriodeFiscale pf, ModeleDocument md) {
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(ctb, pf, dateDebut, dateFin, TypeContribuable.HORS_SUISSE, md);
				final RegDate dateEmission = date(annee + 1, 1, 11);
				di.addEtat(new EtatDeclarationEmise(dateEmission));
				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
				delai.setDateTraitement(dateEmission);
				delai.setDelaiAccordeAu(date(annee + 1, 6, 30));
				di.addDelai(delai);
				return di;
			}
		});

		// quittancement des DI
		final AcknowledgeTaxDeclarationsRequest quittances = new AcknowledgeTaxDeclarationsRequest();
		quittances.setLogin(login);

		for (Ids ids : liste) {
			final AcknowledgeTaxDeclarationRequest demande = new AcknowledgeTaxDeclarationRequest();
			demande.setAcknowledgeDate(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get()));
			demande.setKey(new OrdinaryTaxDeclarationKey());
			demande.getKey().setTaxpayerNumber((int) ids.idCtb);
			demande.getKey().setSequenceNumber(1);
			demande.getKey().setTaxPeriod(annee);
			quittances.getRequests().add(demande);
		}
		
		final AcknowledgeTaxDeclarationsResponse reponse = service.acknowledgeTaxDeclarations(quittances);
		assertNotNull(reponse);

		final List<AcknowledgeTaxDeclarationResponse> retours = reponse.getResponses();
		assertNotNull(retours);
		assertEquals(2, retours.size());

		final AcknowledgeTaxDeclarationResponse retourValide = retours.get(0);
		assertNotNull(retourValide);
		assertEquals(liste.get(0).idCtb, retourValide.getKey().getTaxpayerNumber());
		assertEquals(1, retourValide.getKey().getSequenceNumber());
		assertEquals(annee, retourValide.getKey().getTaxPeriod());
		assertEquals(TaxDeclarationAcknowledgeCode.OK, retourValide.getCode());

		final AcknowledgeTaxDeclarationResponse retourInvalide = retours.get(1);
		assertNotNull(retourInvalide);
		assertEquals(liste.get(1).idCtb, retourInvalide.getKey().getTaxpayerNumber());
		assertEquals(1, retourInvalide.getKey().getSequenceNumber());
		assertEquals(annee, retourInvalide.getKey().getTaxPeriod());
		assertEquals(TaxDeclarationAcknowledgeCode.EXCEPTION, retourInvalide.getCode());

		final ServiceExceptionInfo exceptionInfo = retourInvalide.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		final String expectedMessage = String.format("PersonnePhysique #%d - 1 erreur(s) - 0 avertissement(s):\n [E] Le nom est un attribut obligatoire pour un non-habitant\n", liste.get(1).idCtb);
		assertEquals(expectedMessage, exceptionInfo.getMessage());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire diValide = hibernateTemplate.get(DeclarationImpotOrdinaire.class, liste.get(0).idDi);
				assertNotNull(diValide);
				assertNotNull(diValide.getDernierEtatDeclaration());
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, diValide.getDernierEtatDeclaration().getEtat());

				final DeclarationImpotOrdinaire diInvalide = hibernateTemplate.get(DeclarationImpotOrdinaire.class, liste.get(1).idDi);
				assertNotNull(diInvalide);
				assertNotNull(diInvalide.getDernierEtatDeclaration());
				assertEquals(TypeEtatDocumentFiscal.EMIS, diInvalide.getDernierEtatDeclaration().getEtat());
				return null;
			}
		});
	}

	/**
	 * [UNIREG-2950] Vérifie que les rapports-entre-tiers d'appartenance ménage annulés ne sont pas pris en compte lors du calcul des fors fiscaux virtuels
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyAndVirtualTaxResidencesOnNaturalPersonHavingCancelledHouseholdRelation() throws Exception {

		final RegDate veilleMariage = date(1995, 7, 30);
		final RegDate dateMariage = date(1995, 8, 1);

		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {


				final ch.vd.unireg.tiers.PersonnePhysique arnold = addNonHabitant("Arnold", "Lokker", date(1971, 3, 12), ch.vd.unireg.type.Sexe.MASCULIN);
				addForPrincipal(arnold, date(1991, 3, 12), ch.vd.unireg.type.MotifFor.MAJORITE, veilleMariage, ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Bussigny);

				final ch.vd.unireg.tiers.PersonnePhysique lucette = addNonHabitant("Lucette", "Tartare", date(1973, 5, 12), ch.vd.unireg.type.Sexe.FEMININ);
				addForPrincipal(lucette, date(1993, 5, 12), ch.vd.unireg.type.MotifFor.MAJORITE, veilleMariage, ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Cossonay);

				final ch.vd.unireg.tiers.EnsembleTiersCouple ensemble = addEnsembleTiersCouple(arnold, lucette, dateMariage, null);
				final ch.vd.unireg.tiers.MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, dateMariage, ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);

				// on clone les liens d'appartenance ménage et on les annule (de cette manière, l'ensemble reste valide mais avec des rapports annulés)
				for (ch.vd.unireg.tiers.RapportEntreTiers r : menage.getRapportsObjet()) {
					ch.vd.unireg.tiers.RapportEntreTiers clone = r.duplicate();
					clone.setAnnule(true);
					hibernateTemplate.merge(clone);
				}

				return arnold.getNumero();
			}
		});

		// on vérifie que les données en base sont bien comme on pense
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final ch.vd.unireg.tiers.PersonnePhysique arnold = hibernateTemplate.get(ch.vd.unireg.tiers.PersonnePhysique.class, id);
				assertNotNull(arnold);
				final Set<ch.vd.unireg.tiers.RapportEntreTiers> rapports = arnold.getRapportsSujet();
				assertNotNull(rapports);
				assertEquals(2, rapports.size()); // 2 rapports d'appartenance ménage identiques, mais un est annulé
				return null;
			}
		});

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(id.intValue());
			params.getParts().addAll(Collections.singletonList(PartyPart.VIRTUAL_TAX_RESIDENCES));

			// on s'assure que l'appartenance ménage annulé n'est pas pris en compte (s'il l'était, on recevrait une exception avec le message "Détecté 2 fors fiscaux principaux valides à la même date")
			final NaturalPerson arnold = (NaturalPerson) service.getParty(params);
			assertNotNull(arnold);
			final List<TaxResidence> forsFiscauxPrincipaux = arnold.getMainTaxResidences();
			assertNotNull(forsFiscauxPrincipaux);
			assertEquals(2, forsFiscauxPrincipaux.size());

			assertTaxResidence(date(1991, 3, 12), LiabilityChangeReason.MAJORITY, veilleMariage, LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, MockCommune.Bussigny, false,
					forsFiscauxPrincipaux.get(0));
			assertTaxResidence(date(1995, 8, 1), LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, null, null, MockCommune.Bussigny, true, forsFiscauxPrincipaux.get(1));
		}
	}

	private static void assertTaxResidence(RegDate debut, LiabilityChangeReason motifDebut, @Nullable RegDate fin, @Nullable LiabilityChangeReason motifFin, MockCommune commune, boolean virtuel,
	                                       TaxResidence forFiscal) {
		assertNotNull(forFiscal);
		assertEquals(ch.vd.unireg.xml.DataHelper.coreToXMLv1(debut), forFiscal.getDateFrom());
		assertEquals(motifDebut, forFiscal.getStartReason());
		assertEquals(ch.vd.unireg.xml.DataHelper.coreToXMLv1(fin), forFiscal.getDateTo());
		assertEquals(motifFin, forFiscal.getEndReason());
		assertEquals(TaxType.INCOME_WEALTH, forFiscal.getTaxType());
		assertEquals(TaxationMethod.ORDINARY, forFiscal.getTaxationMethod());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, forFiscal.getTaxationAuthorityType());
		assertEquals(commune.getNoOFS(), forFiscal.getTaxationAuthorityFSOId());
		assertEquals(virtuel, forFiscal.isVirtual());
	}

	private static void assertAddress(@Nullable Date dateFrom, @Nullable Date dateTo, @Nullable String street, String town, Address address) {
		assertAddress(dateFrom, dateTo, street, null, town, false, address);
	}

	private static void assertAddress(@Nullable Date dateFrom, @Nullable Date dateTo, @Nullable String street, @Nullable String houseNumber, @Nullable String town,
	                                  boolean isFakeAddress, Address address) {
		assertNotNull(address);
		assertEquals(dateFrom, address.getDateFrom());
		assertEquals(dateTo, address.getDateTo());
		assertEquals(isFakeAddress, address.isFake());

		final AddressInformation addressInformation = address.getAddressInformation();
		if (isFakeAddress) {
			assertNull(addressInformation);
		}
		else{
			assertNotNull(addressInformation);
			assertEquals(street, addressInformation.getStreet());
			assertEquals(houseNumber, addressInformation.getHouseNumber());
			assertEquals(town, addressInformation.getTown());
		}
	}

	private Date newDate(int year, int month, int day) {
		return new Date(year, month, day);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTaxDeclarationReturnWithMultipleTaxDeclarationSharingTheSameSequenceNumber() throws Exception {

		final class Ids {
			long ppId;
			long diAnnuleeId;
			long diNonAnnuleeId;
			int noSequence;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final ch.vd.unireg.tiers.PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), ch.vd.unireg.type.Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), ch.vd.unireg.type.MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), ch.vd.unireg.type.MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, ch.vd.unireg.type.MotifRattachement.IMMEUBLE_PRIVE);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(ch.vd.unireg.type.TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire diAnnulee = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				diAnnulee.setAnnule(true);

				final DeclarationImpotOrdinaire diNonAnnulee = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				diNonAnnulee.setNumero(diAnnulee.getNumero());

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diAnnuleeId = diAnnulee.getId();
				ids.diNonAnnuleeId = diNonAnnulee.getId();
				ids.noSequence = diAnnulee.getNumero();
				return ids;
			}
		});

		final AcknowledgeTaxDeclarationRequest demande = new AcknowledgeTaxDeclarationRequest();
		demande.setAcknowledgeDate(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get()));
		demande.setKey(new OrdinaryTaxDeclarationKey());
		demande.getKey().setTaxpayerNumber((int) ids.ppId);
		demande.getKey().setSequenceNumber(ids.noSequence);
		demande.getKey().setTaxPeriod(annee);

		final AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse reponse = service.acknowledgeTaxDeclarations(params);
		assertNotNull(reponse);

		final List<AcknowledgeTaxDeclarationResponse> retours = reponse.getResponses();
		assertNotNull(retours);
		assertEquals(1, retours.size());

		final AcknowledgeTaxDeclarationResponse retour = retours.get(0);
		assertNotNull(retour);
		assertEquals(TaxDeclarationAcknowledgeCode.OK, retour.getCode());
	}

	/**
	 * [SIFISC-1782] Vérifie que la source de quittancement (qui est obligatoire) est bien stockées dans l'état de la déclaration lors d'un quittancement depuis le web-service.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTaxDeclarationReturnWithRegardToSource() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// On quittance la DI en précisant la source
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final AcknowledgeTaxDeclarationRequest demande = new AcknowledgeTaxDeclarationRequest();
				demande.setAcknowledgeDate(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get()));
				demande.setSource("TEST_QUITTANCEMENT");
				demande.setKey(new OrdinaryTaxDeclarationKey());
				demande.getKey().setTaxpayerNumber((int) ids.ppId);
				demande.getKey().setSequenceNumber(1);
				demande.getKey().setTaxPeriod(annee);

				final AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
				params.setLogin(login);
				params.getRequests().addAll(Collections.singletonList(demande));

				final AcknowledgeTaxDeclarationsResponse reponse = service.acknowledgeTaxDeclarations(params);
				assertNotNull(reponse);

				final List<AcknowledgeTaxDeclarationResponse> retours = reponse.getResponses();
				assertNotNull(retours);
				assertEquals(1, retours.size());

				final AcknowledgeTaxDeclarationResponse retour = retours.get(0);
				assertNotNull(retour);
				assertEquals(TaxDeclarationAcknowledgeCode.OK, retour.getCode());
				return null;
			}
		});

		// On vérifie que la DI a été quittancée par le CEDI (= valeur par défaut)
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertEquals(RegDate.get(), di.getDateRetour());

				final EtatDeclaration etat = di.getDernierEtatDeclaration();
				assertNotNull(etat);
				assertInstanceOf(EtatDeclarationRetournee.class, etat);

				final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
				assertEquals(RegDate.get(), retour.getDateObtention());
				assertEquals("TEST_QUITTANCEMENT", retour.getSource());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-2528] Vérifie que le code de segmentation est bien retourné par le web-service
 	 */
	@Test
	public void testGetPartyTaxDeclarationSegmentationOn() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				di.setCodeSegment(2);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int)ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			assertEquals(Integer.valueOf(2), d0.getSegmentationCode());
		}
	}

	/**
	 * [SIFISC-2528] Vérifie que le code de segmentation par défaut est bien retourné pour les DI >= 2011
 	 */
	@Test
	public void testGetPartyDefaultTaxDeclarationSegmentationOn() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final Ids ids = doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny,
						MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE, 1, 1),
				                                                           date(DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE, 12, 31), TypeContribuable.HORS_CANTON, md);
				di.setCodeSegment(null);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int)ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			assertEquals(Integer.valueOf(DeclarationImpotService.VALEUR_DEFAUT_CODE_SEGMENT), d0.getSegmentationCode());
		}
	}

	@Test
	public void testGetTaxOffices() throws Exception {

		class Ids {
			long lausanne;
			long orbe;
			long yverdon;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final CollectiviteAdministrative lausanne = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative orbe = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_ORBE.getNoColAdm());
				final CollectiviteAdministrative yverdon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_YVERDON.getNoColAdm());
				ids.lausanne = lausanne.getNumero();
				ids.orbe = orbe.getNumero();
				ids.yverdon = yverdon.getNumero();
			}
		});

		// Lausanne
		final GetTaxOfficesRequest params = new GetTaxOfficesRequest();
		params.setLogin(login);
		params.setMunicipalityFSOId(MockCommune.Lausanne.getNoOFS());

		final GetTaxOfficesResponse lausanne = service.getTaxOffices(params);
		assertNotNull(lausanne);
		assertEquals(ids.lausanne, lausanne.getDistrictTaxOfficePartyNumber());
		assertEquals(ids.lausanne, lausanne.getRegionTaxOfficePartyNumber());

		// Orbe
		params.setMunicipalityFSOId(MockCommune.Orbe.getNoOFS());

		final GetTaxOfficesResponse orbe = service.getTaxOffices(params);
		assertNotNull(orbe);
		assertEquals(ids.orbe, orbe.getDistrictTaxOfficePartyNumber());
		assertEquals(ids.yverdon, orbe.getRegionTaxOfficePartyNumber());

		// Genève
		params.setMunicipalityFSOId(MockCommune.Geneve.getNoOFS());
		assertNull(service.getTaxOffices(params)); // pas un commune vaudoise
	}

	@Test
	public void testGetPartyAdministrativeAuthority() throws Exception {

		class Ids {
			long lausanne;
			long orbe;
			long yverdon;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final CollectiviteAdministrative lausanne = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative orbe = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_ORBE.getNoColAdm());
				final CollectiviteAdministrative yverdon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_YVERDON.getNoColAdm());
				ids.lausanne = lausanne.getNumero();
				ids.orbe = orbe.getNumero();
				ids.yverdon = yverdon.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);

		// Lausanne
		params.setPartyNumber((int) ids.lausanne);
		final AdministrativeAuthority lausanne = (AdministrativeAuthority) service.getParty(params);
		assertNotNull(lausanne);
		assertEquals("OID LAUSANNE", lausanne.getName());
		assertEquals(Integer.valueOf(4), lausanne.getDistrictTaxOfficeId());
		assertEquals(Integer.valueOf(1), lausanne.getRegionTaxOfficeId());

		// Orbe
		params.setPartyNumber((int) ids.orbe);
		final AdministrativeAuthority orbe = (AdministrativeAuthority) service.getParty(params);
		assertNotNull(orbe);
		assertEquals("OID ORBE", orbe.getName());
		assertEquals(Integer.valueOf(10), orbe.getDistrictTaxOfficeId());
		assertNull(orbe.getRegionTaxOfficeId());

		// Yverdon
		params.setPartyNumber((int) ids.yverdon);
		final AdministrativeAuthority yverdon = (AdministrativeAuthority) service.getParty(params);
		assertNotNull(yverdon);
		assertEquals("OID YVERDON", yverdon.getName());
		assertEquals(Integer.valueOf(15), yverdon.getDistrictTaxOfficeId());
		assertEquals(Integer.valueOf(4), yverdon.getRegionTaxOfficeId());
	}

	/**
	 * [SIFISC-3399] Vérifie que le critère 'debtorCategory' est bien géré.
	 * [SIFISC-6587] Vérifie que la catégorie et le mode de communication sont bien retournés
	 */
	@Test
	public void testSearchPartyByDebtorCategory() throws Exception {

		removeTiersIndexData();
		setWantIndexationTiers(true);

		final Long id = doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				addDebiteur(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS, PeriodiciteDecompte.UNIQUE, date(2000, 1, 1));
				final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.ADMINISTRATEURS, PeriodiciteDecompte.TRIMESTRIEL, date(2000, 1, 1));
				debiteur.setModeCommunication(ModeCommunication.PAPIER);
				return debiteur.getNumero();
			}
		});

		globalTiersIndexer.sync();

		final SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(login);
		params.setDebtorCategory(DebtorCategory.ADMINISTRATORS);

		final SearchPartyResponse list = service.searchParty(params);
		assertNotNull(list);
		assertEquals(1, list.getItems().size());

		final PartyInfo info = list.getItems().get(0);
		assertEquals(id.intValue(), info.getNumber());
		assertEquals(PartyType.DEBTOR, info.getType());
		assertEquals(DebtorCategory.ADMINISTRATORS, info.getDebtorCategory());
		assertEquals(CommunicationMode.PAPER, info.getDebtorCommunicationMode());
	}

	/**
	 * [SIFISC-4352] Vérifie que le numéro AVS11 d'un non-habitant est bien retourné sur la catégorie "CH.AHV" (et non pas "CH_AHV_AVS")
	 */
	@Test
	public void testGetPartyNoAVS11() throws Exception {

		final Long id = doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Félix", "Sympa", date(1977, 3, 21), Sexe.MASCULIN);
				addIdentificationPersonne(pp, CategorieIdentifiant.CH_AHV_AVS, "12345678113");
				addIdentificationPersonne(pp, CategorieIdentifiant.CH_ZAR_RCE, "0453.2123/4");
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, id.intValue(), Collections.<PartyPart>emptyList());
		final Party party = service.getParty(params);
		assertNotNull(party);

		final NaturalPerson np = (NaturalPerson) party;
		final PersonIdentification ident = np.getIdentification();
		assertNotNull(ident);

		final List<NamedPersonId> otherIds = ident.getOtherPersonId();
		assertNotNull(otherIds);
		assertEquals(2, otherIds.size());

		Collections.sort(otherIds, Comparator.comparing(NamedPersonId::getPersonIdCategory));

		final NamedPersonId otherId0 = otherIds.get(0);
		assertNotNull(otherId0);
		assertEquals("CH.AHV", otherId0.getPersonIdCategory());
		assertEquals("12345678113", otherId0.getPersonId());

		final NamedPersonId otherId1 = otherIds.get(1);
		assertNotNull(otherId1);
		assertEquals("CH.ZAR", otherId1.getPersonIdCategory());
		assertEquals("0453.2123/4", otherId1.getPersonId());
	}

	/**
	 * [SIFISC-4352] Vérifie que le numéro RCE d'un habitant est bien retourné sur la catégorie "CH.ZAR"
	 */
	@Test
	public void testGetPartyNoRCE() throws Exception {

		final long noInd = 123456L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(noInd, date(1977, 3, 21), "Félix", "Talog", true);
				ind.setNoAVS11("12345678113");
				ind.setNumeroRCE("0453.2123/4");
			}
		});

		final Long id = doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, id.intValue(), Collections.<PartyPart>emptyList());
		final Party party = service.getParty(params);
		assertNotNull(party);

		final NaturalPerson np = (NaturalPerson) party;
		final PersonIdentification ident = np.getIdentification();
		assertNotNull(ident);

		final List<NamedPersonId> otherIds = ident.getOtherPersonId();
		assertNotNull(otherIds);
		assertEquals(2, otherIds.size());

		Collections.sort(otherIds, Comparator.comparing(NamedPersonId::getPersonIdCategory));

		final NamedPersonId otherId0 = otherIds.get(0);
		assertNotNull(otherId0);
		assertEquals("CH.AHV", otherId0.getPersonIdCategory());
		assertEquals("12345678113", otherId0.getPersonId());

		final NamedPersonId otherId1 = otherIds.get(1);
		assertNotNull(otherId1);
		assertEquals("CH.ZAR", otherId1.getPersonIdCategory());
		assertEquals("0453.2123/4", otherId1.getPersonId());
	}

	/**
	 * [SIFISC-4351] Vérifie que le motif de rattachement (taxLiabilityReason) est bien renseigné sur les fors secondaires (otherTaxResidences).
	 */
	@Test
	public void testGetPartyTaxResidences() throws Exception {

		final Long id = doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Félix", "Sympa", date(1977, 3, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.MAJORITE, MockCommune.Morges);
				addForSecondaire(pp, date(2011, 4, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens, MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, id.intValue(), Collections.singletonList(PartyPart.TAX_RESIDENCES));
		final Party party = service.getParty(params);
		assertNotNull(party);

		final List<TaxResidence> mainTaxResidences = party.getMainTaxResidences();
		assertNotNull(mainTaxResidences);
		assertEquals(1, mainTaxResidences.size());

		final TaxResidence main0 = mainTaxResidences.get(0);
		assertNotNull(main0);
		assertEquals(newDate(2000, 1, 1), main0.getDateFrom());
		assertEquals(LiabilityChangeReason.MAJORITY, main0.getStartReason());
		assertNull(main0.getDateTo());
		assertNull(main0.getEndReason());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, main0.getTaxationAuthorityType());
		assertEquals(MockCommune.Morges.getNoOFS(), main0.getTaxationAuthorityFSOId());
		assertEquals(TaxLiabilityReason.RESIDENCE, main0.getTaxLiabilityReason());

		final List<TaxResidence> otherTaxResidences = party.getOtherTaxResidences();
		assertNotNull(otherTaxResidences);
		assertEquals(1, otherTaxResidences.size());

		final TaxResidence other0 = otherTaxResidences.get(0);
		assertNotNull(other0);
		assertEquals(newDate(2011, 4, 1), other0.getDateFrom());
		assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, other0.getStartReason());
		assertNull(other0.getDateTo());
		assertNull(other0.getEndReason());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, other0.getTaxationAuthorityType());
		assertEquals(MockCommune.Renens.getNoOFS(), other0.getTaxationAuthorityFSOId());
		assertEquals(TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY, other0.getTaxLiabilityReason());
	}

	/**
	 * [SIFISC-4475] Vérifie que les adresses annulées ne sont pas exposées dans le web-service Party v3.
	 */
	@Test
	public void testGetCanceledAddresses() throws Exception {

		final Long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Félix", "Annulé", date(1977, 3, 21), Sexe.MASCULIN);
				final AdresseSuisse canceled = addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2005, 1, 1), date(2007, 4, 3), MockRue.Epesses.RueDeLaMottaz);
				canceled.setAnnule(true);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Moudon.LeBourg);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, id.intValue(), Collections.singletonList(PartyPart.ADDRESSES));
		final Party party = service.getParty(params);
		assertNotNull(party);

		final List<Address> addresses = party.getMailAddresses();
		assertNotNull(addresses);
		assertEquals(1, addresses.size()); // l'adresse annulé sur Epesse ne doit pas être exposée

		final Address address = addresses.get(0);
		assertNotNull(address);
		assertEquals(newDate(2000, 1, 1), address.getDateFrom());
		assertNull(address.getDateTo());
		assertEquals(AddressType.MAIL, address.getType());

		final AddressInformation info = address.getAddressInformation();
		assertEquals("Rue du Bourg", info.getStreet());
		assertEquals("Moudon", info.getTown());
	}

	/**
	 * [SIFISC-4623] Vérifie que le numéro de maison n'est retourné que si la rue est renseignée.
	 */
	@Test
	public void testGetAddressesWithHouseNumberButWihoutStreet() throws Exception {

		final long noPM = 20151L;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createSimpleEntrepriseRC(noPM, noPM + 1011, "Fiduciaire Galper S.A.", date(1993, 7, 23), null,
				                                                                                FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Cossonay);
				addEntreprise(ent);
				addAdresse(ent, TypeAdresseCivil.PRINCIPALE, null, "3bis", null, MockLocalite.CossonayVille, date(1993, 7, 23), date(1999, 12, 31));
				addAdresse(ent, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, "3bis", null, MockLocalite.CossonayVille, date(2000, 1, 1), null);
			}
		});

		final long idPM = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise pm = addEntrepriseConnueAuCivil(noPM);
				return pm.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, (int) idPM, Collections.singletonList(PartyPart.ADDRESSES));
		final Corporation pm = (Corporation) service.getParty(params);
		assertNotNull(pm);

		final List<Address> mailAddresses = pm.getMailAddresses();
		assertNotNull(mailAddresses);
		assertEquals(2, mailAddresses.size());

		// pas de rue, pas de numéro
		assertAddress(newDate(1993, 7, 23), newDate(1999, 12, 31), null, null, "Cossonay-Ville", false, mailAddresses.get(0));
		assertAddress(newDate(2000, 1, 1), null, "Avenue du Funiculaire", "3bis", "Cossonay-Ville", false, mailAddresses.get(1));
	}

	/**
	 * [SIFISC-4967] Vérifie que le service retourne bien une adresse "artifielle" lorsqu'on demande les adresses d'un contribuable qui n'en possède pas.
	 */
	@Test
	public void testGetAddressePartyWithoutAnyAddress() throws Exception {

		final long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Julien", "Leproux", date(1956, 1, 1), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, (int) id, Collections.singletonList(PartyPart.ADDRESSES));
		final NaturalPerson person = (NaturalPerson) service.getParty(params);
		assertNotNull(person);

		final List<Address> mailAddresses = person.getMailAddresses();
		assertNotNull(mailAddresses);
		assertEquals(1, mailAddresses.size());

		// pas de rue, pas de numéro
		final Address address0 = mailAddresses.get(0);
		assertAddress(null, null, null, null, null, true, address0);
		assertTrue(address0.isFake()); // il s'agit bien d'une adresse artificielle

		final PersonMailAddressInfo personInfo0 = address0.getPerson();
		assertNotNull(personInfo0);
		assertEquals("Julien", personInfo0.getFirstName());
		assertEquals("Leproux", personInfo0.getLastName());
		assertEquals("Monsieur", personInfo0.getFormalGreeting());
		assertEquals("Monsieur", personInfo0.getSalutation());

		final FormattedAddress formattedAddress0 = address0.getFormattedAddress();
		assertNotNull(formattedAddress0);
		assertEquals("Monsieur", formattedAddress0.getLine1());
		assertEquals("Julien Leproux", formattedAddress0.getLine2());
		assertNull(formattedAddress0.getLine3());
	}

	/**
	 * [SIFISC-4967] Vérifie que le service retourne bien une adresse "normale" lorsqu'on demande les adresses d'un contribuable qui en possède une.
	 */
	@Test
	public void testGetAddressePartyWithOneAddress() throws Exception {

		final long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Julien", "Leproux", date(1956, 1, 1), Sexe.MASCULIN);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(1956,1,1), null, MockRue.Gressy.LesPechaux);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, (int) id, Collections.singletonList(PartyPart.ADDRESSES));
		final NaturalPerson person = (NaturalPerson) service.getParty(params);
		assertNotNull(person);

		final List<Address> mailAddresses = person.getMailAddresses();
		assertNotNull(mailAddresses);
		assertEquals(1, mailAddresses.size());

		// pas de rue, pas de numéro
		final Address address0 = mailAddresses.get(0);
		assertAddress(newDate(1956, 1, 1), null, "Les Péchaux", "Gressy", address0);
		assertFalse(address0.isFake()); // il s'agit d'une adresse normale

		final PersonMailAddressInfo personInfo0 = address0.getPerson();
		assertNotNull(personInfo0);
		assertEquals("Julien", personInfo0.getFirstName());
		assertEquals("Leproux", personInfo0.getLastName());
		assertEquals("Monsieur", personInfo0.getFormalGreeting());
		assertEquals("Monsieur", personInfo0.getSalutation());

		final FormattedAddress formattedAddress0 = address0.getFormattedAddress();
		assertNotNull(formattedAddress0);
		assertEquals("Monsieur", formattedAddress0.getLine1());
		assertEquals("Julien Leproux", formattedAddress0.getLine2());
		assertEquals("Les Péchaux", formattedAddress0.getLine3());
		assertEquals("1432 Gressy", formattedAddress0.getLine4());
		assertNull(formattedAddress0.getLine5());
	}

	//[SIFISC-8060] vérifier si une adresse incomplète est bien indiquée comme telle
	@Test
	public void testGetAddressePartyIncomplete() throws Exception {

		final long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Julien", "Leproux", date(1956, 1, 1), Sexe.MASCULIN);
				addAdresseEtrangere(pp, TypeAdresseTiers.COURRIER, date(1956, 1, 1), null, null, null, MockPays.EtatsUnis);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, (int) id, Collections.singletonList(PartyPart.ADDRESSES));
		final NaturalPerson person = (NaturalPerson) service.getParty(params);
		assertNotNull(person);

		final List<Address> mailAddresses = person.getMailAddresses();
		assertNotNull(mailAddresses);
		assertEquals(1, mailAddresses.size());

		final Address address0 = mailAddresses.get(0);
		assertTrue(address0.isIncomplete());
	}


	@Test
	public void testGetAddressePartyComplete() throws Exception {

		final long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Julien", "Leproux", date(1956, 1, 1), Sexe.MASCULIN);
				addAdresseEtrangere(pp, TypeAdresseTiers.COURRIER, date(1956,1,1),null, "8 Avenue foch","7007 Paris", MockPays.France);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest(login, (int) id, Collections.singletonList(PartyPart.ADDRESSES));
		final NaturalPerson person = (NaturalPerson) service.getParty(params);
		assertNotNull(person);

		final List<Address> mailAddresses = person.getMailAddresses();
		assertNotNull(mailAddresses);
		assertEquals(1, mailAddresses.size());

		final Address address0 = mailAddresses.get(0);
		assertFalse(address0.isIncomplete());
		assertEquals(TariffZone.EUROPE,address0.getAddressInformation().getTariffZone());
	}

	/**
	 * [SIFISC-5846] Vérifie qu'il est possible de recherche quelqu'un à partir de son ancien numéro de sourcier.
	 */
	@Test
	public void testSearchPartyByOldWithholdingNumber() throws Exception {


		class Ids {
			long marcel;
			long jules;
		}
		final Ids ids = new Ids();

		removeTiersIndexData();
		setWantIndexationTiers(true);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique marcel = addNonHabitant("Marcel", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				marcel.setAncienNumeroSourcier(333111L);
				ids.marcel = marcel.getId();

				final PersonnePhysique jules = addNonHabitant("Jules", "Espol", date(1936, 8, 22), Sexe.MASCULIN);
				ids.jules = jules.getId();
				return null;
			}
		});

		globalTiersIndexer.sync();

		// numéro connu
		{
			final SearchPartyRequest params = new SearchPartyRequest();
			params.setLogin(login);
			params.setOldWithholdingNumber(333111);

			final SearchPartyResponse list = service.searchParty(params);
			assertNotNull(list);
			assertEquals(1, list.getItems().size());

			final PartyInfo info = list.getItems().get(0);
			assertEquals(ids.marcel, info.getNumber());
		}

		// numéro inconnu
		{
			final SearchPartyRequest params = new SearchPartyRequest();
			params.setLogin(login);
			params.setOldWithholdingNumber(111333);

			final SearchPartyResponse list = service.searchParty(params);
			assertNotNull(list);
			assertEquals(0, list.getItems().size());
		}

		// pas de critère sur le numéro
		{
			final SearchPartyRequest params = new SearchPartyRequest();
			params.setLogin(login);
			params.setContactName("Jules");

			final SearchPartyResponse list = service.searchParty(params);
			assertNotNull(list);
			assertEquals(1, list.getItems().size());

			final PartyInfo info = list.getItems().get(0);
			assertEquals(ids.jules, info.getNumber());
		}
	}

	@Test
	public void testGetCommonHouseholdStatus() throws Exception {

		final RegDate dateFin = date(1997, 12, 3);

		class Ids {
			long menageVide;
			long menageActif;
			long menageSepare;
			long menageSepareDecesApres;
			long menageTermineCauseDecesPrincipal;
			long menageTermineCauseDecesConjoint;
			long menageTermineCauseDecesComourants;
			long menageSeparePuisReconcilie;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				ids.menageVide = addMenageCommun(null).getId();

				final PersonnePhysique marcel = addNonHabitant("Marcel", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				final PersonnePhysique julie = addNonHabitant("Julie", "Espol", date(1936, 8, 22), Sexe.FEMININ);
				final EnsembleTiersCouple menageActif = addEnsembleTiersCouple(marcel, julie, date(1970, 5, 1), null);
				ids.menageActif = menageActif.getMenage().getId();

				final PersonnePhysique john = addNonHabitant("John", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				final PersonnePhysique selene = addNonHabitant("Sélène", "Espol", date(1936, 8, 22), Sexe.FEMININ);
				final EnsembleTiersCouple menageSepare = addEnsembleTiersCouple(john, selene, date(1970, 5, 1), dateFin);
				ids.menageSepare = menageSepare.getMenage().getId();

				final PersonnePhysique olivier = addNonHabitant("Olivier", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				olivier.setDateDeces(date(2000, 3, 1));
				final PersonnePhysique sabrina = addNonHabitant("Sabrina", "Espol", date(1936, 8, 22), Sexe.FEMININ);
				final EnsembleTiersCouple menageSepareDecesApres = addEnsembleTiersCouple(olivier, sabrina, date(1970, 5, 1), dateFin);
				ids.menageSepareDecesApres = menageSepareDecesApres.getMenage().getId();

				final PersonnePhysique hughes = addNonHabitant("Hughes", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				hughes.setDateDeces(dateFin);
				final PersonnePhysique juliette = addNonHabitant("Juliette", "Espol", date(1936, 8, 22), Sexe.FEMININ);
				final EnsembleTiersCouple decesPrincipal = addEnsembleTiersCouple(hughes, juliette, date(1970, 5, 1), dateFin);
				ids.menageTermineCauseDecesPrincipal = decesPrincipal.getMenage().getId();

				final PersonnePhysique ramon = addNonHabitant("Ramon", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				final PersonnePhysique eva = addNonHabitant("Eva", "Espol", date(1936, 8, 22), Sexe.FEMININ);
				eva.setDateDeces(dateFin);
				final EnsembleTiersCouple decesConjoint = addEnsembleTiersCouple(ramon, eva, date(1970, 5, 1), dateFin);
				ids.menageTermineCauseDecesConjoint = decesConjoint.getMenage().getId();

				final PersonnePhysique samson = addNonHabitant("Samson", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				samson.setDateDeces(dateFin);
				final PersonnePhysique dalila = addNonHabitant("Dalila", "Espol", date(1936, 8, 22), Sexe.FEMININ);
				dalila.setDateDeces(dateFin);
				final EnsembleTiersCouple comourant = addEnsembleTiersCouple(samson, dalila, date(1970, 5, 1), dateFin);
				ids.menageTermineCauseDecesComourants = comourant.getMenage().getId();

				final PersonnePhysique patrick = addNonHabitant("Patrick", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				final PersonnePhysique monique = addNonHabitant("Monique", "Espol", date(1936, 8, 22), Sexe.FEMININ);
				final EnsembleTiersCouple menageSeparePuisReconcilie = addEnsembleTiersCouple(patrick, monique, date(1970, 5, 1), dateFin);
				addAppartenanceMenage(menageSeparePuisReconcilie.getMenage(), patrick, dateFin.addMonths(3), null, false);
				addAppartenanceMenage(menageSeparePuisReconcilie.getMenage(), monique, dateFin.addMonths(3), null, false);
				ids.menageSeparePuisReconcilie = menageSeparePuisReconcilie.getMenage().getId();
				return null;
			}
		});

		// ménage vide
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.menageVide);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(CommonHousehold.class, party);

			final CommonHousehold hh = (CommonHousehold) party;
			final CommonHouseholdStatus status = hh.getStatus();
			assertNull(status);
		}

		// ménage actif
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.menageActif);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(CommonHousehold.class, party);

			final CommonHousehold hh = (CommonHousehold) party;
			final CommonHouseholdStatus status = hh.getStatus();
			assertNotNull(status);
			assertEquals(CommonHouseholdStatus.ACTIVE, status);
		}

		// ménage séparé
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.menageSepare);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(CommonHousehold.class, party);

			final CommonHousehold hh = (CommonHousehold) party;
			final CommonHouseholdStatus status = hh.getStatus();
			assertNotNull(status);
			assertEquals(CommonHouseholdStatus.SEPARATED_DIVORCED, status);
		}

		// ménage séparé et décès plus tardif du principal
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.menageSepareDecesApres);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(CommonHousehold.class, party);

			final CommonHousehold hh = (CommonHousehold) party;
			final CommonHouseholdStatus status = hh.getStatus();
			assertNotNull(status);
			assertEquals(CommonHouseholdStatus.SEPARATED_DIVORCED, status);
		}

		// ménage terminé cause décès du principal
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.menageTermineCauseDecesPrincipal);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(CommonHousehold.class, party);

			final CommonHousehold hh = (CommonHousehold) party;
			final CommonHouseholdStatus status = hh.getStatus();
			assertNotNull(status);
			assertEquals(CommonHouseholdStatus.ENDED_BY_DEATH, status);
		}

		// ménage terminé cause décès du conjoint
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.menageTermineCauseDecesConjoint);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(CommonHousehold.class, party);

			final CommonHousehold hh = (CommonHousehold) party;
			final CommonHouseholdStatus status = hh.getStatus();
			assertNotNull(status);
			assertEquals(CommonHouseholdStatus.ENDED_BY_DEATH, status);
		}

		// ménage terminé cause décès simultané du principal et du conjoint (comourants)
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.menageTermineCauseDecesComourants);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(CommonHousehold.class, party);

			final CommonHousehold hh = (CommonHousehold) party;
			final CommonHouseholdStatus status = hh.getStatus();
			assertNotNull(status);
			assertEquals(CommonHouseholdStatus.ENDED_BY_DEATH, status);
		}

		// ménage séparé puis réconcilié
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.menageSeparePuisReconcilie);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(CommonHousehold.class, party);

			final CommonHousehold hh = (CommonHousehold) party;
			final CommonHouseholdStatus status = hh.getStatus();
			assertNotNull(status);
			assertEquals(CommonHouseholdStatus.ACTIVE, status);
		}
	}

	/**
	 * On vérifie que tout se passe bien lorsque le remboursement automatique est nul.
	 */
	@Test
	public void testGetCorporationWithoutAutomaticReimbursementInfo() throws Exception {

		final long idEntrepriseCivile = 12345L;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createSimpleEntrepriseRC(idEntrepriseCivile, idEntrepriseCivile + 1011, "Biscottes Duchmole", date(1990, 4, 5), null,
				                                                                                FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, MockCommune.Cossonay);
				addEntreprise(ent);
			}
		});

		final Long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise ent = addEntrepriseConnueAuCivil(idEntrepriseCivile);
				ent.setBlocageRemboursementAutomatique(null);
				return ent.getId();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(id.intValue());

		final Party party = service.getParty(params);
		assertNotNull(party);
		assertInstanceOf(Corporation.class, party);

		final Corporation hh = (Corporation) party;
		assertFalse(hh.isAutomaticReimbursementBlocked());
	}

	/**
	 * [SIFISC-6228] Vérifie que les délais des déclarations sont bien retournés par le web-service.
	 */
	@Test
	public void testGetPartyTaxDeclarationDeadlines() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				addDelaiDeclaration(di, date(annee + 1, 1, 15), date(annee + 1, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// sans la part qui va bien
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			assertEmpty(d0.getDeadlines());
		}

		// avec la part qui va bien
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS_DEADLINES));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			final List<TaxDeclarationDeadline> deadlines = d0.getDeadlines();
			assertNotNull(deadlines);
			assertEquals(1, deadlines.size());

			final TaxDeclarationDeadline deadline0 = deadlines.get(0);
			assertNotNull(deadline0);
			assertEquals(newDate(annee + 1, 1, 15), deadline0.getApplicationDate());
			assertEquals(newDate(annee + 1, 1, 15), deadline0.getProcessingDate());
			assertEquals(newDate(annee + 1, 6, 30), deadline0.getDeadline());
			assertFalse(deadline0.isWrittenConfirmation());
		}
	}

	/**
	 * [SIFISC-6500] Vérifie que les états des déclarations sont bien retournés par le web-service.
	 */
	@Test
	public void testGetPartyTaxDeclarationStatuses() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				addDelaiDeclaration(di, date(annee + 1, 1, 15), date(annee + 1, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 15));
				addEtatDeclarationRetournee(di, date(annee + 1, 4, 23), "TEST");

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// sans la part qui va bien
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			assertEmpty(d0.getStatuses());
		}

		// avec la part qui va bien
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS_STATUSES));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			final List<TaxDeclarationStatus> statuses = d0.getStatuses();
			assertNotNull(statuses);
			assertEquals(2, statuses.size());

			final TaxDeclarationStatus status0 = statuses.get(0);
			assertNotNull(status0);
			assertEquals(newDate(annee + 1, 1, 15), status0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, status0.getType());
			assertNull(status0.getSource());

			final TaxDeclarationStatus status1 = statuses.get(1);
			assertNotNull(status1);
			assertEquals(newDate(annee + 1, 4, 23), status1.getDateFrom());
			assertEquals(TaxDeclarationStatusType.RETURNED, status1.getType());
			assertEquals("TEST", status1.getSource());
		}
	}

	/**
	 * [SIFISC-6864] Vérifie que les délais des déclarations sont bien retournés par le web-service dans l'ordre chronologique.
	 */
	@Test
	public void testGetPartyTaxDeclarationDeadlinesSortingOrder() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				addDelaiDeclaration(di, date(annee + 1, 3, 15), date(annee + 1, 9, 30), EtatDelaiDocumentFiscal.ACCORDE);
				addDelaiDeclaration(di, date(annee + 1, 1, 15), date(annee + 1, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// sans la part qui va bien
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			assertEmpty(d0.getDeadlines());
		}

		// avec la part qui va bien
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS_DEADLINES));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			final List<TaxDeclarationDeadline> deadlines = d0.getDeadlines();
			assertNotNull(deadlines);
			assertEquals(2, deadlines.size());

			final TaxDeclarationDeadline deadline0 = deadlines.get(0);
			assertNotNull(deadline0);
			assertEquals(newDate(annee + 1, 1, 15), deadline0.getApplicationDate());
			assertEquals(newDate(annee + 1, 1, 15), deadline0.getProcessingDate());
			assertEquals(newDate(annee + 1, 6, 30), deadline0.getDeadline());
			assertFalse(deadline0.isWrittenConfirmation());

			final TaxDeclarationDeadline deadline1 = deadlines.get(1);
			assertNotNull(deadline1);
			assertEquals(newDate(annee + 1, 3, 15), deadline1.getApplicationDate());
			assertEquals(newDate(annee + 1, 3, 15), deadline1.getProcessingDate());
			assertEquals(newDate(annee + 1, 9, 30), deadline1.getDeadline());
			assertFalse(deadline1.isWrittenConfirmation());
		}
	}

	/**
	 * [SIFISC-6864] Vérifie que les états des déclarations sont bien retournés par le web-service dans l'ordre nominal.
	 */
	@Test
	public void testGetPartyTaxDeclarationStatusesSortingOrder() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				addDelaiDeclaration(di, date(annee + 1, 3, 15), date(annee + 1, 9, 30), EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 7));
				addEtatDeclarationRetournee(di, date(annee + 1, 7, 31));
				addEtatDeclarationSommee(di, date(annee + 1, 7, 20), date(annee + 1, 7, 18), null);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// sans la part qui va bien
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			assertEmpty(d0.getDeadlines());
		}

		// avec la part qui va bien
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.ppId);
			params.getParts().addAll(Collections.singletonList(PartyPart.TAX_DECLARATIONS_STATUSES));

			final NaturalPerson pp = (NaturalPerson) service.getParty(params);
			assertNotNull(pp);
			final List<TaxDeclaration> declarations = pp.getTaxDeclarations();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());

			final OrdinaryTaxDeclaration d0 = (OrdinaryTaxDeclaration) declarations.get(0);
			final List<TaxDeclarationStatus> statuses = d0.getStatuses();
			assertNotNull(statuses);
			assertEquals(3, statuses.size());

			final TaxDeclarationStatus status0 = statuses.get(0);
			assertNotNull(status0);
			assertEquals(newDate(annee + 1, 1, 7), status0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, status0.getType());

			final TaxDeclarationStatus status1 = statuses.get(1);
			assertNotNull(status1);
			assertEquals(newDate(annee + 1, 7, 18), status1.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SUMMONS_SENT, status1.getType());

			final TaxDeclarationStatus status2 = statuses.get(2);
			assertNotNull(status2);
			assertEquals(newDate(annee + 1, 7, 31), status2.getDateFrom());
			assertEquals(TaxDeclarationStatusType.RETURNED, status2.getType());
		}
	}

	/**
	 * [SIFISC-6228]
	 */
	@Test
	public void testExtendDeadline() throws Exception {

		final class Ids {
			long ppId;
			long di2008;
			long di2009;
			long di2010;
			long di2011;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(2009, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(2009, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				// 2008 : échue
				final DeclarationImpotOrdinaire di2008 = addDI(pp, 2008);
				addEtatDeclarationEchue(di2008, date(2009, 7, 1));

				// 2009 : retournée
				final DeclarationImpotOrdinaire di2009 = addDI(pp, 2009);
				addEtatDeclarationRetournee(di2009, date(2010, 4, 2));

				// 2010 : sommée
				final DeclarationImpotOrdinaire di2010 = addDI(pp, 2010);
				addEtatDeclarationSommee(di2010, date(2011, 9, 1), date(2011, 9, 3), null);

				// 2010 : émise
				final DeclarationImpotOrdinaire di2011 = addDI(pp, 2011);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.di2008 = di2008.getId();
				ids.di2009 = di2009.getId();
				ids.di2010 = di2010.getId();
				ids.di2011 = di2011.getId();
				return ids;
			}

			private DeclarationImpotOrdinaire addDI(PersonnePhysique pp, int annee) {
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				addDelaiDeclaration(di, date(annee + 1, 1, 15), date(annee + 1, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationEmise(di, date(annee, 1, 30));
				return di;
			}
		});

		final Date today = ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get());

		// il ne doit pas être possible d'ajouter un délai sur une déclaration dont l'état est différent d'émise.

		// 2008 : échue
		{
			final ExtendDeadlineRequest params = new ExtendDeadlineRequest();
			params.setLogin(login);
			params.setKey(new TaxDeclarationKey((int) ids.ppId, 2008, 1));
			params.setApplicationDate(today);
			params.setNewDeadline(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get().addMonths(1)));

			final ExtendDeadlineResponse results = service.extendDeadline(params);
			assertNotNull(results);
			assertEquals(ExtendDeadlineCode.ERROR_BAD_TAX_DECLARATION_STATUS, results.getCode());
			assertEquals("La déclaration n'est pas dans l'état 'émise' (état=[ECHU]).", results.getExceptionInfo().getMessage());
		}

		// 2009 : retournée
		{
			final ExtendDeadlineRequest params = new ExtendDeadlineRequest();
			params.setLogin(login);
			params.setKey(new TaxDeclarationKey((int) ids.ppId, 2009, 1));
			params.setApplicationDate(today);
			params.setNewDeadline(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get().addMonths(1)));

			final ExtendDeadlineResponse results = service.extendDeadline(params);
			assertNotNull(results);
			assertEquals(ExtendDeadlineCode.ERROR_BAD_TAX_DECLARATION_STATUS, results.getCode());
			assertEquals("La déclaration n'est pas dans l'état 'émise' (état=[RETOURNE]).", results.getExceptionInfo().getMessage());
		}

		// 2010 : sommée
		{
			final ExtendDeadlineRequest params = new ExtendDeadlineRequest();
			params.setLogin(login);
			params.setKey(new TaxDeclarationKey((int) ids.ppId, 2010, 1));
			params.setApplicationDate(today);
			params.setNewDeadline(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get().addMonths(1)));

			final ExtendDeadlineResponse results = service.extendDeadline(params);
			assertNotNull(results);
			assertEquals(ExtendDeadlineCode.ERROR_BAD_TAX_DECLARATION_STATUS, results.getCode());
			assertEquals("La déclaration n'est pas dans l'état 'émise' (état=[SOMME]).", results.getExceptionInfo().getMessage());
		}

		// 2011 : émise

		// demande dans le futur
		{
			final ExtendDeadlineRequest params = new ExtendDeadlineRequest();
			params.setLogin(login);
			params.setKey(new TaxDeclarationKey((int) ids.ppId, 2011, 1));
			params.setApplicationDate(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get().addMonths(1)));
			params.setNewDeadline(ch.vd.unireg.xml.DataHelper.coreToXMLv1(RegDate.get().addMonths(1)));

			final ExtendDeadlineResponse results = service.extendDeadline(params);
			assertNotNull(results);
			assertEquals(ExtendDeadlineCode.ERROR_INVALID_APPLICATION_DATE, results.getCode());
			assertEquals("La date de demande spécifiée [" + RegDateHelper.dateToDisplayString(RegDate.get().addMonths(1)) +
					"] est postérieure à la date du jour [" + RegDateHelper.dateToDisplayString(RegDate.get()) + "].", results.getExceptionInfo().getMessage());
		}

		// nouveau délai dans le passé
		{
			final ExtendDeadlineRequest params = new ExtendDeadlineRequest();
			params.setLogin(login);
			params.setKey(new TaxDeclarationKey((int) ids.ppId, 2011, 1));
			params.setApplicationDate(today);
			params.setNewDeadline(newDate(2012, 9, 14));

			final ExtendDeadlineResponse results = service.extendDeadline(params);
			assertNotNull(results);
			assertEquals(ExtendDeadlineCode.ERROR_INVALID_DEADLINE, results.getCode());
			assertEquals("Le délai spécifié [14.09.2012] est antérieur à la date du jour [" + RegDateHelper.dateToDisplayString(RegDate.get()) + "].", results.getExceptionInfo().getMessage());
		}

		// ok
		final RegDate nouveauDelai = RegDate.get().addMonths(1);
		{
			final ExtendDeadlineRequest params = new ExtendDeadlineRequest();
			params.setLogin(login);
			params.setKey(new TaxDeclarationKey((int) ids.ppId, 2011, 1));
			params.setApplicationDate(today);
			params.setNewDeadline(ch.vd.unireg.xml.DataHelper.coreToXMLv1(nouveauDelai));

			final ExtendDeadlineResponse results = service.extendDeadline(params);
			assertNotNull(results);
			assertEquals(ExtendDeadlineCode.OK, results.getCode());
		}

		// on vérifie que le nouveau délai est en base de données
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.di2011);
				assertNotNull(di);

				final List<DelaiDeclaration> delais = di.getDelaisDeclarationSorted();
				assertNotNull(delais);
				assertEquals(2, delais.size());

				assertEquals(date(2012, 6, 30), delais.get(0).getDelaiAccordeAu());
				assertEquals(nouveauDelai, delais.get(1).getDelaiAccordeAu());

				return null;
			}
		});

		// nouveau délai avant le précédent
		{
			final ExtendDeadlineRequest params = new ExtendDeadlineRequest();
			params.setLogin(login);
			params.setKey(new TaxDeclarationKey((int) ids.ppId, 2011, 1));
			params.setApplicationDate(today);
			final RegDate newDeadline = RegDate.get().addDays(10);
			params.setNewDeadline(ch.vd.unireg.xml.DataHelper.coreToXMLv1(newDeadline));

			final ExtendDeadlineResponse results = service.extendDeadline(params);
			assertNotNull(results);
			assertEquals(ExtendDeadlineCode.ERROR_INVALID_DEADLINE, results.getCode());
			assertEquals("Le délai spécifié [" + RegDateHelper.dateToDisplayString(newDeadline) + "] est antérieur ou égal au délai existant [" +
					RegDateHelper.dateToDisplayString(RegDate.get().addMonths(1)) + "].", results.getExceptionInfo().getMessage());
		}
	}

	/**
	 * [SIFISC-8072] Vérifie que l'historique des permis d'un habitant Suisse est bien retourné par le web-service
	 */
	@Test
	public void testGetNaturalPersonneCategoryHabitantSuisse() throws Exception {

		final long noInd = 13432;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1983, 3, 31), "Gourt", "Eva", Sexe.FEMININ);
				addNationalite(ind, MockPays.Suisse, date(1983, 3, 31), null);
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getId();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(id.intValue());

		final NaturalPerson pp = (NaturalPerson) service.getParty(params);
		assertNotNull(pp);

		// l'attribut 'categoryHisto' contient l'historique complet des permis
		final List<NaturalPersonCategoryPeriod> permis = pp.getCategories();
		assertNotNull(permis);
		assertEquals(1, permis.size());
		assertPermis(null, null, NaturalPersonCategory.SWISS, permis.get(0));

		// l'attribut 'category' contient le type du dernier permis
		assertEquals(NaturalPersonCategory.SWISS, pp.getCategory());
	}

	/**
	 * [SIFISC-8072] Vérifie que l'historique des permis d'un habitant permis B puis C est bien retourné par le web-service
	 */
	@Test
	public void testGetNaturalPersonneCategoryHabitantPermisC() throws Exception {

		final long noInd = 13432;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1983, 3, 31), "Gourt", "Eva", Sexe.FEMININ);
				addNationalite(ind, MockPays.France, date(1983, 3, 31), null);
				addPermis(ind, TypePermis.SEJOUR, date(2000, 2, 2), date(2005, 2, 1), false);
				addPermis(ind, TypePermis.ETABLISSEMENT, date(2005, 2, 2), null, false);
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getId();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(id.intValue());

		final NaturalPerson pp = (NaturalPerson) service.getParty(params);
		assertNotNull(pp);

		// l'attribut 'categoryHisto' contient l'historique complet des permis
		final List<NaturalPersonCategoryPeriod> permis = pp.getCategories();
		assertNotNull(permis);
		assertEquals(2, permis.size());
		assertPermis(newDate(2000, 2, 2), newDate(2005, 2, 1), NaturalPersonCategory.C_02_B_PERMIT, permis.get(0));
		assertPermis(newDate(2005, 2, 2), null, NaturalPersonCategory.C_03_C_PERMIT, permis.get(1));

		// l'attribut 'category' contient le type du dernier permis
		assertEquals(NaturalPersonCategory.C_03_C_PERMIT, pp.getCategory());
	}

	/**
	 * [SIFISC-8072] Vérifie que l'historique des permis d'un non-habitant permis B est bien retourné par le web-service
	 */
	@Test
	public void testGetNaturalPersonneCategoryNonHabitantPermisB() throws Exception {

		final RegDate dateDebutPermis = date(2009, 9, 22);
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Eva", "Gourt", date(1983, 3, 31), Sexe.FEMININ);
				pp.setCategorieEtranger(CategorieEtranger._02_PERMIS_SEJOUR_B);
				pp.setDateDebutValiditeAutorisation(dateDebutPermis);
				return pp.getId();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber(id.intValue());

		final NaturalPerson pp = (NaturalPerson) service.getParty(params);
		assertNotNull(pp);

		// l'attribut 'categoryHisto' contient l'historique complet des permis
		final List<NaturalPersonCategoryPeriod> permis = pp.getCategories();
		assertNotNull(permis);
		assertEquals(1, permis.size());
		assertPermis(DataHelper.coreToXMLv1(dateDebutPermis), null, NaturalPersonCategory.C_02_B_PERMIT, permis.get(0));

		// l'attribut 'category' contient le type du dernier permis
		assertEquals(NaturalPersonCategory.C_02_B_PERMIT, pp.getCategory());
	}

	@Test
	public void testNonNullSimplifiedTaxLiabilities() throws Exception {

		final int year = RegDate.get().year();

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Ava", "Gardner", date(1922, 12, 24), Sexe.FEMININ);
				pp.setCategorieEtranger(CategorieEtranger._02_PERMIS_SEJOUR_B);
				addForPrincipal(pp, date(year - 3, 1, 1), MotifFor.ARRIVEE_HS, date(year - 2, 5, 12), MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, date(year - 2, 5, 13), MotifFor.DEPART_HS, null, null, MockPays.EtatsUnis, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) ppId);
		params.getParts().add(PartyPart.SIMPLIFIED_TAX_LIABILITIES);

		final NaturalPerson pp = (NaturalPerson) service.getParty(params);
		assertNotNull(pp);

		// les collections de tax liabilities ne doivent pas contenir de valeurs nulles
		final List<SimplifiedTaxLiability> ch = pp.getSimplifiedTaxLiabilityCH();
		assertNotNull(ch);
		assertEquals(1, ch.size());
		for (int i = 0 ; i < ch.size() ; ++ i) {
			assertNotNull(Integer.toString(i), ch.get(i));
		}
		final List<SimplifiedTaxLiability> vd = pp.getSimplifiedTaxLiabilityVD();
		assertNotNull(vd);
		assertEquals(1, vd.size());
		for (int i = 0 ; i < vd.size() ; ++ i) {
			assertNotNull(Integer.toString(i), vd.get(i));
		}
	}

	private static void assertPermis(Date dateFrom, Date dateTo, NaturalPersonCategory category, NaturalPersonCategoryPeriod permis) {
		assertNotNull(permis);
		assertEquals(category, permis.getCategory());
		assertEquals(dateFrom, permis.getDateFrom());
		assertEquals(dateTo, permis.getDateTo());
	}

	/**
	 * Type de débiteur non-supporté par cette version du service
	 */
	@Test
	public void testGetDebiteurParticipationsHorsSuisse() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
				return dpi.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) dpiId);
		params.getParts().add(PartyPart.DEBTOR_PERIODICITIES);

		try {
			final Party party = service.getParty(params);
			fail("Ca aurait dû exploser");
		}
		catch (WebServiceException e) {
			assertEquals("Type de catégorie impôt source non supporté dans cette version du service", e.getFaultInfo().getMessage());
		}
	}

	/**
	 * Type de débiteur non-supporté par cette version du service
	 */
	@Test
	public void testGetDebiteurEffeuilleuses() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.EFFEUILLEUSES, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
				return dpi.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) dpiId);
		params.getParts().add(PartyPart.DEBTOR_PERIODICITIES);

		try {
			final Party party = service.getParty(params);
			fail("Ca aurait dû exploser");
		}
		catch (WebServiceException e) {
			assertEquals("Type de catégorie impôt source non supporté dans cette version du service", e.getFaultInfo().getMessage());
		}
	}

	/**
	 * Types de débiteur non-supportés par cette version du service
	 */
	@Test
	public void testSearchDebiteurAvecTypeNonSupporte() throws Exception {

		final class Ids {
			long phs;
			long eff;
			long reg;
		}

		// pour être certain de ne rien récupérer d'autre que ce que je vais créer maintenant
		globalTiersIndexer.overwriteIndex();

		final boolean otfi = globalTiersIndexer.onTheFlyIndexationSwitch().isEnabled();
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(true);
		try {
			// mise en place des débiteurs
			final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final DebiteurPrestationImposable reg = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
					reg.setNom1("Toto");        // pour la recherche

					final DebiteurPrestationImposable eff = addDebiteur(CategorieImpotSource.EFFEUILLEUSES, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
					eff.setNom1("Toto");        // pour la recherche

					final DebiteurPrestationImposable phs = addDebiteur(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
					phs.setNom1("Toto");        // pour la recherche

					final Ids ids = new Ids();
					ids.phs = phs.getNumero();
					ids.eff = eff.getNumero();
					ids.reg = reg.getNumero();
					return ids;
				}
			});

			// on attend que l'indexation des trois nouveaux débiteurs soit terminée
			globalTiersIndexer.sync();

			// recherche par nom
			final SearchPartyRequest params = new SearchPartyRequest();
			params.setLogin(login);
			params.setContactName("Toto");

			final SearchPartyResponse found = service.searchParty(params);
			assertNotNull(found);
			assertNotNull(found.getItems());
			assertEquals(1, found.getItems().size());

			final PartyInfo singleton = found.getItems().get(0);
			assertNotNull(singleton);
			assertEquals(ids.reg, singleton.getNumber());
		}
		finally {
			globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(otfi);
		}
	}

	@Test
	public void testMunicipalityOnResidenceAddress() throws Exception {

		final long noIndividu = 2387432376L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "de Montaigne", "Michel", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) ppId);
		params.getParts().add(PartyPart.ADDRESSES);

		final Party party = service.getParty(params);
		assertNotNull(party);
		assertInstanceOf(NaturalPerson.class, party);

		final List<Address> residences = party.getResidenceAddresses();
		assertNotNull(residences);
		assertEquals(1, residences.size());

		final Address residence = residences.get(0);
		assertNotNull(residence);
		assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), residence.getAddressInformation().getMunicipalityId());
	}

	@Test
	public void testParentesFiscalesNonRenvoyeesCommeRapportEntreTiers() throws Exception {

		final long noIndPapa = 7856487345L;
		final long noIndFiston = 438953683L;
		final RegDate dateNaissanceFiston = date(1975, 10, 22);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu papa = addIndividu(noIndPapa, date(1950, 8, 26), "Smith", "John Senior", Sexe.MASCULIN);
				final MockIndividu fiston = addIndividu(noIndFiston, dateNaissanceFiston, "Smith", "Johnny Baby", Sexe.MASCULIN);
				addLiensFiliation(fiston, papa, null, dateNaissanceFiston, null);
			}
		});

		// mise en place fiscale
		final class Ids {
			long idPapa;
			long idFiston;
		}
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique papa = addHabitant(noIndPapa);
				final PersonnePhysique fiston = addHabitant(noIndFiston);
				addParente(fiston, papa, dateNaissanceFiston, null);
				final Ids ids = new Ids();
				ids.idPapa = papa.getNumero();
				ids.idFiston = fiston.getNumero();
				return ids;
			}
		});

		// vue depuis le père, en ne demandant que les relations -> pas d'enfants
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.idPapa);
			params.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEmpty(party.getRelationsBetweenParties());
		}

		// vue depuis le père, en ne demandant que les enfants -> un enfant
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.idPapa);
			params.getParts().add(PartyPart.CHILDREN);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEquals(1, party.getRelationsBetweenParties().size());
		}

		// vue depuis l'enfant, en ne demandant que les relations -> pas de papa
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.idFiston);
			params.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEmpty(party.getRelationsBetweenParties());
		}

		// vue depuis le père, en ne demandant que les parents -> un enfant
		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.idFiston);
			params.getParts().add(PartyPart.PARENTS);

			final Party party = service.getParty(params);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEquals(1, party.getRelationsBetweenParties().size());
		}
	}
}

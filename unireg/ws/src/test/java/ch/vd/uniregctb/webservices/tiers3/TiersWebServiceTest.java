package ch.vd.uniregctb.webservices.tiers3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.webservices.tiers3.BatchParty;
import ch.vd.unireg.webservices.tiers3.BatchPartyEntry;
import ch.vd.unireg.webservices.tiers3.CommonHousehold;
import ch.vd.unireg.webservices.tiers3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.tiers3.GetPartyRequest;
import ch.vd.unireg.webservices.tiers3.LiabilityChangeReason;
import ch.vd.unireg.webservices.tiers3.NaturalPerson;
import ch.vd.unireg.webservices.tiers3.OrdinaryTaxDeclarationKey;
import ch.vd.unireg.webservices.tiers3.PartyPart;
import ch.vd.unireg.webservices.tiers3.PartyWebService;
import ch.vd.unireg.webservices.tiers3.ReturnTaxDeclarationsRequest;
import ch.vd.unireg.webservices.tiers3.ReturnTaxDeclarationsResponse;
import ch.vd.unireg.webservices.tiers3.TaxDeclarationReturnCode;
import ch.vd.unireg.webservices.tiers3.TaxDeclarationReturnRequest;
import ch.vd.unireg.webservices.tiers3.TaxDeclarationReturnResponse;
import ch.vd.unireg.webservices.tiers3.TaxResidence;
import ch.vd.unireg.webservices.tiers3.TaxType;
import ch.vd.unireg.webservices.tiers3.TaxationAuthorityType;
import ch.vd.unireg.webservices.tiers3.TaxationMethod;
import ch.vd.unireg.xml.address.Address;
import ch.vd.unireg.xml.address.OtherPartyAddressType;
import ch.vd.unireg.xml.common.Date;
import ch.vd.unireg.xml.common.UserLogin;
import ch.vd.unireg.xml.exception.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.ServiceExceptionInfo;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.validation.ValidationInterceptor;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class TiersWebServiceTest extends WebserviceTest {

	private PartyWebService service;
	private UserLogin login;
	private ValidationInterceptor validationInterceptor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(PartyWebService.class, "tiersService3Impl");
		validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");
		login = new UserLogin("iamtestuser", 22);
		serviceCivil.setUp(new DefaultMockServiceCivil());
	}

	/**
	 * [UNIREG-1985] Vérifie que les fors fiscaux virtuels sont bien retournés <b>même si</b> on demande l'adresse d'envoi en même temps.
	 */
	@Test
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

				final ch.vd.uniregctb.tiers.PersonnePhysique paul = addNonHabitant("Paul", "Duchemin", RegDate.get(1954, 3, 31), ch.vd.uniregctb.type.Sexe.MASCULIN);
				ids.paul = paul.getNumero();
				addForPrincipal(paul, date(1974, 3, 31), ch.vd.uniregctb.type.MotifFor.MAJORITE, veilleMariage, ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Lausanne);
				addAdresseSuisse(paul, TypeAdresseTiers.COURRIER, date(1954, 3, 31), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final ch.vd.uniregctb.tiers.PersonnePhysique janine = addNonHabitant("Janine", "Duchemin", RegDate.get(1954, 3, 31), ch.vd.uniregctb.type.Sexe.MASCULIN);
				ids.janine = janine.getNumero();
				addForPrincipal(janine, date(1974, 3, 31), ch.vd.uniregctb.type.MotifFor.MAJORITE, veilleMariage, ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Lausanne);
				addAdresseSuisse(janine, TypeAdresseTiers.COURRIER, date(1954, 3, 31), null, MockRue.Lausanne.AvenueDeMarcelin);

				final ch.vd.uniregctb.tiers.EnsembleTiersCouple ensemble = addEnsembleTiersCouple(paul, janine, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

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

				final ch.vd.uniregctb.tiers.PersonnePhysique jeanpierre = addNonHabitant("Jean-Pierre", "Bürki", RegDate.get(1947, 1, 11), ch.vd.uniregctb.type.Sexe.MASCULIN);
				ids.jeanpierre = jeanpierre.getNumero();
				addAdresseSuisse(jeanpierre, TypeAdresseTiers.COURRIER, date(1947, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final ch.vd.uniregctb.tiers.PersonnePhysique marie = addNonHabitant("Marie", "Bürki", RegDate.get(1954, 1, 1), ch.vd.uniregctb.type.Sexe.FEMININ);
				ids.marie = marie.getNumero();
				addAdresseSuisse(marie, TypeAdresseTiers.COURRIER, date(1954, 1, 11), null, MockRue.Lausanne.AvenueDeMarcelin);

				final ch.vd.uniregctb.tiers.EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jeanpierre, marie, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final ch.vd.uniregctb.tiers.PersonnePhysique tuteur = addNonHabitant("Jacky", "Rod", RegDate.get(1947, 1, 1), ch.vd.uniregctb.type.Sexe.MASCULIN);
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
			assertAddress(new Date(1947, 1, 1), new Date(2007, 9, 10), "Av de Beaulieu", "Lausanne", menage.getMailAddresses().get(0)); // adresse de monsieur
			assertAddress(new Date(2007, 9, 11), null, "Av de Marcelin", "Lausanne", menage.getMailAddresses().get(1)); // adresse de madame (puisque monsieur est sous tutelle)

			assertNotNull(menage.getDebtProsecutionAddresses());
			assertEquals(1, menage.getDebtProsecutionAddresses().size());
			assertAddress(new Date(1947, 1, 1), null, "Av de Beaulieu", "Lausanne",
					menage.getDebtProsecutionAddresses().get(0)); // adresse de monsieur (non-impacté par la tutelle, car pas d'autorité tutelaire renseignée)

			assertEmpty(menage.getDebtProsecutionAddressesOfOtherParty()); // [UNIREG-2227] pas d'adresse autre tiers car madame remplace monsieur dans la gestion du ménage
		}
	}

	/**
	 * [UNIREG-3203] Cas du contribuable n°497.050.02
	 */
	@Test
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

				final ch.vd.uniregctb.tiers.PersonnePhysique jeandaniel = addNonHabitant("Jean-Daniel", "Guex-Martin", RegDate.get(1947, 1, 11), ch.vd.uniregctb.type.Sexe.MASCULIN);
				ids.jeandaniel = jeandaniel.getNumero();
				addAdresseSuisse(jeandaniel, TypeAdresseTiers.COURRIER, date(1947, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final ch.vd.uniregctb.tiers.PersonnePhysique myriam = addNonHabitant("Myriam", "Guex-Martin", RegDate.get(1954, 1, 1), ch.vd.uniregctb.type.Sexe.FEMININ);
				ids.myriam = myriam.getNumero();
				addAdresseSuisse(myriam, TypeAdresseTiers.COURRIER, date(1954, 1, 11), null, MockRue.Lausanne.AvenueDeMarcelin);

				final ch.vd.uniregctb.tiers.EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jeandaniel, myriam, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final ch.vd.uniregctb.tiers.PersonnePhysique conseiller = addNonHabitant("Philippe", "Rossy", RegDate.get(1947, 1, 1), ch.vd.uniregctb.type.Sexe.MASCULIN);
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
			params.getParts().addAll(Arrays.asList(PartyPart.ADDRESSES));

			final CommonHousehold menage = (CommonHousehold) service.getParty(params);
			assertNotNull(menage);

			assertNotNull(menage.getMailAddresses());
			assertEquals(2, menage.getMailAddresses().size());
			assertAddress(new Date(1947, 1, 1), new Date(2007, 9, 10), "Av de Beaulieu", "Lausanne", menage.getMailAddresses().get(0)); // adresse de monsieur
			assertAddress(new Date(2007, 9, 11), null, "Av de Marcelin", "Lausanne", menage.getMailAddresses().get(1)); // adresse de madame (puisque monsieur est sous conseil légal)

			assertNotNull(menage.getDebtProsecutionAddresses());
			assertEquals(1, menage.getDebtProsecutionAddresses().size());
			assertAddress(new Date(1947, 1, 1), null, "Av de Beaulieu", "Lausanne", menage.getDebtProsecutionAddresses().get(0)); // adresse de monsieur (non-impacté par le conseil légal)

			assertEmpty(menage.getDebtProsecutionAddressesOfOtherParty()); // [UNIREG-2227] pas d'adresse autre tiers car madame remplace monsieur dans la gestion du ménage
		}
	}

	/**
	 * [UNIREG-2227] Cas du contribuable n°100.864.90 : on s'assure que la source de l'adresse 'poursuite autre tiers' est bien CURATELLE dans le cas d'une curatelle dont les adresses de début et de fin
	 * sont nulles.
	 */
	@Test
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

		validationInterceptor.setEnabled(false); // pour permettre l'ajout d'une curatelle avec date de début nulle
		try {
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					ch.vd.uniregctb.tiers.PersonnePhysique tiia = addHabitant(noIndividuTiia);
					addAdresseSuisse(tiia, TypeAdresseTiers.COURRIER, date(2009, 7, 8), null, MockRue.Lausanne.PlaceSaintFrancois);
					ids.tiia = tiia.getId();
					ch.vd.uniregctb.tiers.PersonnePhysique sylvie = addHabitant(noIndividuSylvie);
					ids.sylvie = sylvie.getId();
					addCuratelle(tiia, sylvie, null, null);
					return null;
				}
			});
		}
		finally {
			validationInterceptor.setEnabled(true);
		}

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber((int) ids.tiia);
			params.getParts().addAll(Arrays.asList(PartyPart.ADDRESSES));

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

			assertNotNull(tiia.getDebtProsecutionAddressesOfOtherParty());
			assertEquals(3, tiia.getDebtProsecutionAddressesOfOtherParty().size());
			assertAddress(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.getDebtProsecutionAddressesOfOtherParty().get(0));
			assertEquals(OtherPartyAddressType.WELFARE_ADVOCATE, tiia.getDebtProsecutionAddressesOfOtherParty().get(0).getOtherPartyType());
			assertAddress(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.getDebtProsecutionAddressesOfOtherParty().get(1));
			assertEquals(OtherPartyAddressType.WELFARE_ADVOCATE, tiia.getDebtProsecutionAddressesOfOtherParty().get(1).getOtherPartyType());
			assertAddress(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.getDebtProsecutionAddressesOfOtherParty().get(2));
			assertEquals(OtherPartyAddressType.WELFARE_ADVOCATE, tiia.getDebtProsecutionAddressesOfOtherParty().get(2).getOtherPartyType());
		}
	}

	/**
	 * [UNIREG-2782] vérification que le quittancement d'une DI pour laquelle l'état "EMISE" n'a pas été créé fonctionne quand-même
	 */
	@Test
	@NotTransactional
	public void testTaxDeclarationReturnWithoutSentStatus() throws Exception {

		class Ids {
			long ppId;
			long diId;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus transactionStatus) throws Exception {

				final ch.vd.uniregctb.tiers.PersonnePhysique pp = addNonHabitant("Albus", "Dumbledore", date(1957, 10, 3), ch.vd.uniregctb.type.Sexe.MASCULIN);
				addForPrincipal(pp, date(2009, 1, 1), ch.vd.uniregctb.type.MotifFor.ARRIVEE_HS, MockCommune.Croy);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument modele = addModeleDocument(ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				assertNull(di.getEtatDeclarationActif(ch.vd.uniregctb.type.TypeEtatDeclaration.EMISE));

				final EtatDeclaration retour = new EtatDeclarationRetournee(RegDate.get());
				di.addEtat(retour);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// quittancement de la DI
		final TaxDeclarationReturnRequest demande = new TaxDeclarationReturnRequest();
		demande.setReturnDate(DataHelper.coreToWeb(RegDate.get()));
		demande.setKey(new OrdinaryTaxDeclarationKey());
		demande.getKey().setTaxpayerNumber((int) ids.ppId);
		demande.getKey().setSequenceNumber(1);
		demande.getKey().setTaxPeriod(2009);

		final ReturnTaxDeclarationsRequest quittances = new ReturnTaxDeclarationsRequest();
		quittances.setLogin(login);
		quittances.getRequests().add(demande);

		final ReturnTaxDeclarationsResponse reponse = service.returnTaxDeclarations(quittances);
		assertNotNull(reponse);

		final List<TaxDeclarationReturnResponse> retours = reponse.getResponses();
		assertNotNull(retours);
		assertEquals(1, retours.size());

		final TaxDeclarationReturnResponse retour = retours.get(0);
		assertNotNull(retour);
		assertEquals(ids.ppId, retour.getKey().getTaxpayerNumber());
		assertEquals(1, retour.getKey().getSequenceNumber());
		assertEquals(2009, retour.getKey().getTaxPeriod());
		assertEquals(TaxDeclarationReturnCode.OK, retour.getCode());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertNotNull(di.getDernierEtat());
				assertEquals(ch.vd.uniregctb.type.TypeEtatDeclaration.RETOURNEE, di.getDernierEtat().getEtat());
				return null;
			}
		});
	}

	// [UNIREG-3179] si un tiers ne valide pas au milieu du lot, tout explose
	@Test
	@NotTransactional
	public void testTaxDeclarationReturnOnInvalidTaxpayer() throws Exception {

		class Ids {
			long ppId;
			long diId;
		}
		final Ids ids = new Ids();

		final int annee = 2010;

		// on désactive la validation pour pouvoir sauver un tiers au moins qui ne valide pas...
		validationInterceptor.setEnabled(false);
		try {
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {

					final RegDate debutAnnee = date(annee, 1, 1);
					final RegDate finAnnee = date(annee, 12, 31);

					final ch.vd.uniregctb.tiers.PersonnePhysique pp = addNonHabitant(null, "Anonyme", date(1980, 10, 25), ch.vd.uniregctb.type.Sexe.MASCULIN);
					pp.setNom(null);        // <-- c'est là le problème de validation

					addForPrincipal(pp, debutAnnee, ch.vd.uniregctb.type.MotifFor.ACHAT_IMMOBILIER, MockPays.Allemagne);
					addForSecondaire(pp, debutAnnee, ch.vd.uniregctb.type.MotifFor.ACHAT_IMMOBILIER, finAnnee.addMonths(-3), ch.vd.uniregctb.type.MotifFor.VENTE_IMMOBILIER,
							MockCommune.Aigle.getNoOFSEtendu(), ch.vd.uniregctb.type.MotifRattachement.IMMEUBLE_PRIVE);

					final PeriodeFiscale pf = addPeriodeFiscale(annee);
					final ModeleDocument md = addModeleDocument(ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
					addCollAdm(MockCollectiviteAdministrative.CEDI);

					final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, debutAnnee, finAnnee, TypeContribuable.HORS_SUISSE, md);
					final RegDate dateEmission = date(annee + 1, 1, 11);
					di.addEtat(new EtatDeclarationEmise(dateEmission));

					final DelaiDeclaration delai = new DelaiDeclaration();
					delai.setDateTraitement(dateEmission);
					delai.setDelaiAccordeAu(date(annee + 1, 6, 30));
					di.addDelai(delai);

					ids.ppId = pp.getNumero();
					ids.diId = di.getId();

					return null;
				}
			});
		}
		finally {
			// maintenant, on peut réactiver la validation
			validationInterceptor.setEnabled(true);
		}

		// quittancement de la DI
		final TaxDeclarationReturnRequest demande = new TaxDeclarationReturnRequest();
		demande.setReturnDate(DataHelper.coreToWeb(RegDate.get()));
		demande.setKey(new OrdinaryTaxDeclarationKey());
		demande.getKey().setTaxpayerNumber((int) ids.ppId);
		demande.getKey().setSequenceNumber(1);
		demande.getKey().setTaxPeriod(annee);

		final ReturnTaxDeclarationsRequest quittances = new ReturnTaxDeclarationsRequest();
		quittances.setLogin(login);
		quittances.getRequests().add(demande);

		final ReturnTaxDeclarationsResponse reponse = service.returnTaxDeclarations(quittances);
		assertNotNull(reponse);

		final List<TaxDeclarationReturnResponse> retours = reponse.getResponses();
		assertNotNull(retours);
		assertEquals(1, retours.size());

		final TaxDeclarationReturnResponse retour = retours.get(0);
		assertNotNull(retour);
		assertEquals(ids.ppId, retour.getKey().getTaxpayerNumber());
		assertEquals(1, retour.getKey().getSequenceNumber());
		assertEquals(annee, retour.getKey().getTaxPeriod());
		assertEquals(TaxDeclarationReturnCode.EXCEPTION, retour.getCode());

		final ServiceExceptionInfo exceptionInfo = retour.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		final String expectedMessage = String.format("PersonnePhysique #%d - 1 erreur(s) - 0 warning(s):\n [E] Le nom est un attribut obligatoire pour un non-habitant\n", ids.ppId);
		assertEquals(expectedMessage, exceptionInfo.getMessage());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertNotNull(di.getDernierEtat());
				assertEquals(ch.vd.uniregctb.type.TypeEtatDeclaration.EMISE, di.getDernierEtat().getEtat());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testMultipleTaxDeclarationReturnWithOneTaxpayerWithoutSentStatus() throws Exception {

		final int annee = 2010;
		final RegDate debutAnnee = date(annee, 1, 1);
		final RegDate finAnnee = date(annee, 12, 31);

		class Ids {
			final long idCtb;
			final long idDi;

			Ids(long idCtb, long idDi) {
				this.idCtb = idCtb;
				this.idDi = idDi;
			}
		}
		final List<Ids> liste = new ArrayList<Ids>();

		// on désactive la validation pour pouvoir sauver un tiers au moins qui ne valide pas...
		validationInterceptor.setEnabled(false);
		try {
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {

					final PeriodeFiscale pf = addPeriodeFiscale(annee);
					final ModeleDocument md = addModeleDocument(ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
					addCollAdm(MockCollectiviteAdministrative.CEDI);

					final ch.vd.uniregctb.tiers.PersonnePhysique validePP = addPersonnePhysiqueAvecFor("Alfred", "de Montauban", date(1980, 10, 25), ch.vd.uniregctb.type.Sexe.MASCULIN);
					final ch.vd.uniregctb.tiers.PersonnePhysique invalidePP = addPersonnePhysiqueAvecFor(null, null, date(1986, 12, 5), ch.vd.uniregctb.type.Sexe.FEMININ);

					final DeclarationImpotOrdinaire valideDi = addDi(validePP, debutAnnee, finAnnee, pf, md);
					final DeclarationImpotOrdinaire invalideDi = addDi(invalidePP, debutAnnee, finAnnee, pf, md);

					liste.add(new Ids(validePP.getNumero(), valideDi.getId()));
					liste.add(new Ids(invalidePP.getNumero(), invalideDi.getId()));
					return null;
				}

				private ch.vd.uniregctb.tiers.PersonnePhysique addPersonnePhysiqueAvecFor(@Nullable String prenom, @Nullable String nom, RegDate dateNaissance, ch.vd.uniregctb.type.Sexe sexe) {
					final ch.vd.uniregctb.tiers.PersonnePhysique pp = addNonHabitant(prenom, nom, dateNaissance, sexe);
					addForPrincipal(pp, debutAnnee, ch.vd.uniregctb.type.MotifFor.ACHAT_IMMOBILIER, MockPays.Allemagne);
					addForSecondaire(pp, debutAnnee, ch.vd.uniregctb.type.MotifFor.ACHAT_IMMOBILIER, finAnnee.addMonths(-3), ch.vd.uniregctb.type.MotifFor.VENTE_IMMOBILIER,
							MockCommune.Aigle.getNoOFSEtendu(), ch.vd.uniregctb.type.MotifRattachement.IMMEUBLE_PRIVE);
					return pp;
				}

				private DeclarationImpotOrdinaire addDi(ch.vd.uniregctb.tiers.Contribuable ctb, RegDate dateDebut, RegDate dateFin, PeriodeFiscale pf, ModeleDocument md) {
					final DeclarationImpotOrdinaire di = addDeclarationImpot(ctb, pf, dateDebut, dateFin, TypeContribuable.HORS_SUISSE, md);
					final RegDate dateEmission = date(annee + 1, 1, 11);
					di.addEtat(new EtatDeclarationEmise(dateEmission));
					final DelaiDeclaration delai = new DelaiDeclaration();
					delai.setDateTraitement(dateEmission);
					delai.setDelaiAccordeAu(date(annee + 1, 6, 30));
					di.addDelai(delai);
					return di;
				}
			});
		}
		finally {
			// maintenant, on peut réactiver la validation
			validationInterceptor.setEnabled(true);
		}

		// quittancement des DI
		final ReturnTaxDeclarationsRequest quittances = new ReturnTaxDeclarationsRequest();
		quittances.setLogin(login);

		for (Ids ids : liste) {
			final TaxDeclarationReturnRequest demande = new TaxDeclarationReturnRequest();
			demande.setReturnDate(DataHelper.coreToWeb(RegDate.get()));
			demande.setKey(new OrdinaryTaxDeclarationKey());
			demande.getKey().setTaxpayerNumber((int) ids.idCtb);
			demande.getKey().setSequenceNumber(1);
			demande.getKey().setTaxPeriod(annee);
			quittances.getRequests().add(demande);
		}
		
		final ReturnTaxDeclarationsResponse reponse = service.returnTaxDeclarations(quittances);
		assertNotNull(reponse);

		final List<TaxDeclarationReturnResponse> retours = reponse.getResponses();
		assertNotNull(retours);
		assertEquals(2, retours.size());

		final TaxDeclarationReturnResponse retourValide = retours.get(0);
		assertNotNull(retourValide);
		assertEquals(liste.get(0).idCtb, retourValide.getKey().getTaxpayerNumber());
		assertEquals(1, retourValide.getKey().getSequenceNumber());
		assertEquals(annee, retourValide.getKey().getTaxPeriod());
		assertEquals(TaxDeclarationReturnCode.OK, retourValide.getCode());

		final TaxDeclarationReturnResponse retourInvalide = retours.get(1);
		assertNotNull(retourInvalide);
		assertEquals(liste.get(1).idCtb, retourInvalide.getKey().getTaxpayerNumber());
		assertEquals(1, retourInvalide.getKey().getSequenceNumber());
		assertEquals(annee, retourInvalide.getKey().getTaxPeriod());
		assertEquals(TaxDeclarationReturnCode.EXCEPTION, retourInvalide.getCode());

		final ServiceExceptionInfo exceptionInfo = retourInvalide.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		final String expectedMessage = String.format("PersonnePhysique #%d - 1 erreur(s) - 0 warning(s):\n [E] Le nom est un attribut obligatoire pour un non-habitant\n", liste.get(1).idCtb);
		assertEquals(expectedMessage, exceptionInfo.getMessage());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire diValide = hibernateTemplate.get(DeclarationImpotOrdinaire.class, liste.get(0).idDi);
				assertNotNull(diValide);
				assertNotNull(diValide.getDernierEtat());
				assertEquals(ch.vd.uniregctb.type.TypeEtatDeclaration.RETOURNEE, diValide.getDernierEtat().getEtat());

				final DeclarationImpotOrdinaire diInvalide = hibernateTemplate.get(DeclarationImpotOrdinaire.class, liste.get(1).idDi);
				assertNotNull(diInvalide);
				assertNotNull(diInvalide.getDernierEtat());
				assertEquals(ch.vd.uniregctb.type.TypeEtatDeclaration.EMISE, diInvalide.getDernierEtat().getEtat());
				return null;
			}
		});
	}

	/**
	 * [UNIREG-2950] Vérifie que les rapports-entre-tiers d'appartenance ménage annulés ne sont pas pris en compte lors du calcul des fors fiscaux virtuels
	 */
	@Test
	public void testGetPartyAndVirtualTaxResidencesOnNaturalPersonHavingCancelledHouseholdRelation() throws Exception {

		final RegDate veilleMariage = date(1995, 7, 30);
		final RegDate dateMariage = date(1995, 8, 1);

		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {


				final ch.vd.uniregctb.tiers.PersonnePhysique arnold = addNonHabitant("Arnold", "Lokker", date(1971, 3, 12), ch.vd.uniregctb.type.Sexe.MASCULIN);
				addForPrincipal(arnold, date(1991, 3, 12), ch.vd.uniregctb.type.MotifFor.MAJORITE, veilleMariage, ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Bussigny);

				final ch.vd.uniregctb.tiers.PersonnePhysique lucette = addNonHabitant("Lucette", "Tartare", date(1973, 5, 12), ch.vd.uniregctb.type.Sexe.FEMININ);
				addForPrincipal(lucette, date(1993, 5, 12), ch.vd.uniregctb.type.MotifFor.MAJORITE, veilleMariage, ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Cossonay);

				final ch.vd.uniregctb.tiers.EnsembleTiersCouple ensemble = addEnsembleTiersCouple(arnold, lucette, dateMariage, null);
				final ch.vd.uniregctb.tiers.MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, dateMariage, ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);

				// on clone les liens d'appartenance ménage et on les annule (de cette manière, l'ensemble reste valide mais avec des rapports annulés)
				for (ch.vd.uniregctb.tiers.RapportEntreTiers r : menage.getRapportsObjet()) {
					ch.vd.uniregctb.tiers.RapportEntreTiers clone = r.duplicate();
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
				final ch.vd.uniregctb.tiers.PersonnePhysique arnold = hibernateTemplate.get(ch.vd.uniregctb.tiers.PersonnePhysique.class, id);
				assertNotNull(arnold);
				final Set<ch.vd.uniregctb.tiers.RapportEntreTiers> rapports = arnold.getRapportsSujet();
				assertNotNull(rapports);
				assertEquals(2, rapports.size()); // 2 rapports d'appartenance ménage identiques, mais un est annulé
				return null;
			}
		});

		{
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(id.intValue());
			params.getParts().addAll(Arrays.asList(PartyPart.VIRTUAL_TAX_RESIDENCES));

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
		assertEquals(DataHelper.coreToWeb(debut), forFiscal.getDateFrom());
		assertEquals(motifDebut, forFiscal.getStartReason());
		assertEquals(DataHelper.coreToWeb(fin), forFiscal.getDateTo());
		assertEquals(motifFin, forFiscal.getEndReason());
		assertEquals(TaxType.INCOME_WEALTH, forFiscal.getTaxType());
		assertEquals(TaxationMethod.ORDINARY, forFiscal.getTaxationMethod());
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, forFiscal.getTaxationAuthorityType());
		assertEquals(commune.getNoOFS(), forFiscal.getTaxationAuthorityFSOId());
		assertEquals(virtuel, forFiscal.isVirtual());
	}

	private static void assertAddress(@Nullable Date dateFrom, @Nullable Date dateTo, String street, String town, Address address) {
		assertNotNull(address);
		assertEquals(dateFrom, address.getDateFrom());
		assertEquals(dateTo, address.getDateTo());
		assertEquals(street, address.getAddressInformation().getStreet());
		assertEquals(town, address.getAddressInformation().getTown());
	}

	private Date newDate(int year, int month, int day) {
		return new Date(year, month, day);
	}

	@Test
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
				final ch.vd.uniregctb.tiers.PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), ch.vd.uniregctb.type.Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), ch.vd.uniregctb.type.MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), ch.vd.uniregctb.type.MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), ch.vd.uniregctb.type.MotifRattachement.IMMEUBLE_PRIVE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
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

		final TaxDeclarationReturnRequest demande = new TaxDeclarationReturnRequest();
		demande.setReturnDate(DataHelper.coreToWeb(RegDate.get()));
		demande.setKey(new OrdinaryTaxDeclarationKey());
		demande.getKey().setTaxpayerNumber((int) ids.ppId);
		demande.getKey().setSequenceNumber(ids.noSequence);
		demande.getKey().setTaxPeriod(annee);

		final ReturnTaxDeclarationsRequest params = new ReturnTaxDeclarationsRequest();
		params.setLogin(login);
		params.getRequests().add(demande);

		final ReturnTaxDeclarationsResponse reponse = service.returnTaxDeclarations(params);
		assertNotNull(reponse);

		final List<TaxDeclarationReturnResponse> retours = reponse.getResponses();
		assertNotNull(retours);
		assertEquals(1, retours.size());

		final TaxDeclarationReturnResponse retour = retours.get(0);
		assertNotNull(retour);
		assertEquals(TaxDeclarationReturnCode.OK, retour.getCode());
	}
}

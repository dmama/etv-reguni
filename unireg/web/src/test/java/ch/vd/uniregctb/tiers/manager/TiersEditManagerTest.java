package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class TiersEditManagerTest extends WebTest {

	private final static String DB_UNIT_FILE = "TiersEditManagerTest.xml";

	TiersEditManager tiersEditManager ;


	/**
	 * @see ch.vd.uniregctb.common.AbstractCoreDAOTest#onSetUp()
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				final MockIndividu individu2 = addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(1980, 1, 1), null);
			}
		});

		loadDatabase(DB_UNIT_FILE);
		tiersEditManager = getBean(TiersEditManager.class, "tiersEditManager");
	}

	/**
	 * Teste la methode getView
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetView() throws Exception {

		TiersEditView view = tiersEditManager.getView(new Long(6789));
		assertEquals("Bolomey", view.getIndividu().getNom());
	}

	/**
	 * Teste la methode creePersonne
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreePersonne() {

		TiersEditView view = tiersEditManager.creePersonne();
		Tiers tiers = view.getTiers();
		PersonnePhysique nonHab = (PersonnePhysique) tiers;
		assertNull(nonHab.getSexe());
	}

	/**
	 * Teste la methode creeOrganisation
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreeOrganisation() {

		TiersEditView view = tiersEditManager.creeOrganisation();
		Tiers tiers = view.getTiers();
		AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
		assertNotNull(autreCommunaute);
	}

	/**
	 * Teste la methode creeDebiteur
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreeDebiteur() throws Exception{

		TiersEditView view = tiersEditManager.creeDebiteur(new Long(6789));
		Tiers tiers = view.getTiers();
		DebiteurPrestationImposable debiteurPrestationImposable = (DebiteurPrestationImposable) tiers;
		assertNotNull(debiteurPrestationImposable);
	}

	/**
	 * Teste la methode save
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSave() {

		TiersEditView view = tiersEditManager.creePersonne();
		Tiers tiers = view.getTiers();
		PersonnePhysique nonHab = (PersonnePhysique) tiers;
		nonHab.setNom("claude");
		view.setTiers(nonHab);
		Tiers tiersSaved = tiersEditManager.save(view);
		PersonnePhysique nonHabitantSaved = (PersonnePhysique)tiersSaved;
		assertEquals("claude", nonHabitantSaved.getNom());

	}

	/**
	 * Cas jira UNIREG-3180
	 */
	@Test
	public void testChangeModeCommunicationDebiteur() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), null));
				return dpi.getNumero();
			}
		});

		DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		assertEquals(ModeCommunication.PAPIER, view.getModeCommunication());
		{
			view.setModeCommunication(ModeCommunication.ELECTRONIQUE);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(ModeCommunication.ELECTRONIQUE, view.getModeCommunication());
		}
		{
			view.setModeCommunication(ModeCommunication.SITE_WEB);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(ModeCommunication.SITE_WEB, view.getModeCommunication());
		}
	}

	/**
	 * Cas jira UNIREG-3180
	 */
	@Test
	public void testChangeCategorieImpotSource() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), null));
				return dpi.getNumero();
			}
		});

		DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		assertEquals(CategorieImpotSource.REGULIERS, view.getCategorieImpotSource());
		{
			view.setCategorieImpotSource(CategorieImpotSource.ADMINISTRATEURS);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(CategorieImpotSource.ADMINISTRATEURS, view.getCategorieImpotSource());
		}
		{
			view.setCategorieImpotSource(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR, view.getCategorieImpotSource());
		}
	}

	@Test
	public void testChangePeriodiciteDebiteurSansLR() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), null));
				addForDebiteur(dpi, date(2010, 11, 1), MotifFor.INDETERMINE, null, null, MockCommune.Aigle);
				return dpi.getNumero();
			}
		});

		DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		assertEquals(PeriodiciteDecompte.MENSUEL, view.getNouvellePeriodicite());
		assertEquals(PeriodiciteDecompte.MENSUEL, view.getPeriodiciteActive());
		assertEquals(date(2010, 1, 1), view.getDateDebutNouvellePeriodicite());
		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
			view.setDateDebutNouvellePeriodicite(date(2010, 10, 1));
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.TRIMESTRIEL, view.getNouvellePeriodicite());
			assertEquals(date(2010, 10, 1), view.getDateDebutNouvellePeriodicite());
		}
		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			view.setDateDebutNouvellePeriodicite(date(2010, 7, 1));
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getNouvellePeriodicite());
			assertEquals(date(2010, 7, 1), view.getDateDebutNouvellePeriodicite());
		}
		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.ANNUEL);
			view.setDateDebutNouvellePeriodicite(date(2010, 1, 1));
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.ANNUEL, view.getNouvellePeriodicite());
			assertEquals(date(2010, 1, 1), view.getDateDebutNouvellePeriodicite());
		}

		// petite vérification en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
				assertNotNull(dpi);

				// on trie les périodicité par leur ordre de création
				final List<Periodicite> periodicites = new ArrayList<>(dpi.getPeriodicites());
				Collections.sort(periodicites, new Comparator<Periodicite>() {
					@Override
					public int compare(Periodicite o1, Periodicite o2) {
						return Long.compare(o1.getId(), o2.getId());
					}
				});

				assertEquals(4, periodicites.size());
				{
					final Periodicite p = periodicites.get(0);
					assertEquals(PeriodiciteDecompte.MENSUEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 1, 1), p.getDateDebut());
					assertEquals(date(2010, 6, 30), p.getDateFin());        // assignée au 30.9.2010 par la périodicité trimestrielle puis au 30.6.2010 par la périodicité semestrielle
					assertTrue(p.isAnnule());                               // annulée par l'avénement de la périodicité annulelle
				}
				{
					final Periodicite p = periodicites.get(1);
					assertEquals(PeriodiciteDecompte.TRIMESTRIEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 10, 1), p.getDateDebut());
					assertNull(p.getDateFin());
					assertTrue(p.isAnnule());                               // annulée par l'avénement de la périodicité semestrielle
				}
				{
					final Periodicite p = periodicites.get(2);
					assertEquals(PeriodiciteDecompte.SEMESTRIEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 7, 1), p.getDateDebut());
					assertNull(p.getDateFin());
					assertTrue(p.isAnnule());                               // annulée par l'avénement de la périodicité annuelle
				}
				{
					final Periodicite p = periodicites.get(3);
					assertEquals(PeriodiciteDecompte.ANNUEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 1, 1), p.getDateDebut());
					assertNull(p.getDateFin());
					assertFalse(p.isAnnule());
				}
				return null;
			}
		});

		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getNouvellePeriodicite());
		}
		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.TRIMESTRIEL, view.getNouvellePeriodicite());
		}
		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.MENSUEL);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.MENSUEL, view.getNouvellePeriodicite());
		}

		// petite vérification en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
				assertNotNull(dpi);

				// on trie les périodicité par leur ordre de création
				final List<Periodicite> periodicites = new ArrayList<>(dpi.getPeriodicites());
				Collections.sort(periodicites, new Comparator<Periodicite>() {
					@Override
					public int compare(Periodicite o1, Periodicite o2) {
						return Long.compare(o1.getId(), o2.getId());
					}
				});

				assertEquals(7, periodicites.size());
				{
					final Periodicite p = periodicites.get(0);
					assertEquals(PeriodiciteDecompte.MENSUEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 1, 1), p.getDateDebut());
					assertEquals(date(2010, 6, 30), p.getDateFin());        // assignée au 30.9.2010 par la périodicité trimestrielle puis au 30.6.2010 par la périodicité semestrielle
					assertTrue(p.isAnnule());                               // annulée par l'avénement de la périodicité annulelle
				}
				{
					final Periodicite p = periodicites.get(1);
					assertEquals(PeriodiciteDecompte.TRIMESTRIEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 10, 1), p.getDateDebut());
					assertNull(p.getDateFin());
					assertTrue(p.isAnnule());                               // annulée par l'avénement de la périodicité semestrielle
				}
				{
					final Periodicite p = periodicites.get(2);
					assertEquals(PeriodiciteDecompte.SEMESTRIEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 7, 1), p.getDateDebut());
					assertNull(p.getDateFin());
					assertTrue(p.isAnnule());                               // annulée par l'avénement de la périodicité annuelle
				}
				{
					final Periodicite p = periodicites.get(3);
					assertEquals(PeriodiciteDecompte.ANNUEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 1, 1), p.getDateDebut());
					assertEquals(date(2010, 6, 30), p.getDateFin());        // date de fin "bizarre" (= non conforme à la périodicité annoncée) en raison de l'arrivée ultérieure de la périodicité S sans LR présente
					assertFalse(p.isAnnule());
				}
				{
					final Periodicite p = periodicites.get(4);
					assertEquals(PeriodiciteDecompte.SEMESTRIEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 7, 1), p.getDateDebut());
					assertEquals(date(2010, 9, 30), p.getDateFin());        // date de fin "bizarre" (= non conforme à la périodicité annoncée) en raison de l'arrivée ultérieure de la périodicité T sans LR présente
					assertFalse(p.isAnnule());
				}
				{
					final Periodicite p = periodicites.get(5);
					assertEquals(PeriodiciteDecompte.TRIMESTRIEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 10, 1), p.getDateDebut());
					assertEquals(date(2010, 10, 31), p.getDateFin());        // date de fin "bizarre" (= non conforme à la périodicité annoncée) en raison de l'arrivée ultérieure de la périodicité M sans LR présente
					assertFalse(p.isAnnule());
				}
				{
					final Periodicite p = periodicites.get(6);
					assertEquals(PeriodiciteDecompte.MENSUEL, p.getPeriodiciteDecompte());
					assertEquals(date(2010, 11, 1), p.getDateDebut());
					assertNull(p.getDateFin());
					assertFalse(p.isAnnule());
				}
				return null;
			}
		});
	}

	/**
	 * SIFISC-2934
	 */
	@Test
	public void testVidageAdresseEmail() throws Exception {
		final String mail = "toto@titi.com";
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				pp.setAdresseCourrierElectronique(mail);
				return pp.getNumero();
			}
		});

		final TiersEditView view = tiersEditManager.getView(ppId);
		assertNotNull(view);
		assertNotNull(view.getComplement());
		assertEquals(mail, view.getComplement().getAdresseCourrierElectronique());

		view.getComplement().setAdresseCourrierElectronique("");        // vidé par l'opérateur
		tiersEditManager.save(view);

		final TiersEditView nouvelleView = tiersEditManager.getView(ppId);
		assertNotNull(nouvelleView);
		assertNotNull(nouvelleView.getComplement());
		assertNull(nouvelleView.getComplement().getAdresseCourrierElectronique());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRefresh() throws AdresseException, ServiceInfrastructureException {
		final TiersEditView view = tiersEditManager.getView(6789L);
		view.getTiers().setPersonneContact("toto");
		tiersEditManager.refresh(view, 6789L);
	}

	@Test
	public void testGetDatesPossiblesPourDebutNouvellePeriodiciteSansPeriodiciteExistante() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				return dpi.getNumero();
			}
		});

		for (PeriodiciteDecompte nvelle : PeriodiciteDecompte.values()) {
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, nvelle, RegDate.get().addYears(1));
			assertNotNull(nvelle.name(), dates);
			assertEquals(nvelle.name(), 1, dates.size());
			assertEquals(nvelle.name(), date(RegDate.get().year(), 1, 1), dates.get(0));
		}
	}

	@Test
	public void testGetDatesPossiblesPourDebutNouvellePeriodiciteAvecPeriodiciteExistanteActive() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null));
				return dpi.getNumero();
			}
		});

		final RegDate oneYearFromNow = RegDate.get().addYears(1);

		// passage en MENSUEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(3);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en TRIMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(3);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en SEMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en ANNUEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(12);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.ANNUEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en UNIQUE
		{
			final List<RegDate> expectedDates = Arrays.asList(date(RegDate.get().year(), 1, 1), date(RegDate.get().year() + 1, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.UNIQUE, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}
	}

	@Test
	public void testGetDatesPossiblesPourDebutNouvellePeriodiciteAvecPremierePeriodiciteActiveEtAnnulable() throws Exception {

		final int year = RegDate.get().year();

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, date(year, 1, 1), null));
				return dpi.getNumero();
			}
		});

		final RegDate oneYearFromNow = RegDate.get().addYears(1);

		// passage en MENSUEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(3);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en TRIMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(3);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en SEMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en ANNUEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(12);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.ANNUEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en UNIQUE
		{
			final List<RegDate> expectedDates = Arrays.asList(date(RegDate.get().year(), 1, 1), date(RegDate.get().year() + 1, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.UNIQUE, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}
	}

	@Test
	public void testGetDatesPossiblesPourDebutNouvellePeriodiciteAvecPeriodiciteExistanteNonActive() throws Exception {

		final int year = RegDate.get().year();
		final RegDate nextFirstOfJanuary = date(year + 1, 1, 1);

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2010, 1, 1), nextFirstOfJanuary.getOneDayBefore()));
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, nextFirstOfJanuary, null));
				return dpi.getNumero();
			}
		});

		final RegDate oneYearFromNow = RegDate.get().addYears(1);

		// passage en MENSUEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBefore(nextFirstOfJanuary)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(3);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en TRIMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBefore(nextFirstOfJanuary)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(3);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en SEMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en ANNUEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(12);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.ANNUEL, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en UNIQUE
		{
			final List<RegDate> expectedDates = Arrays.asList(date(RegDate.get().year(), 1, 1), date(RegDate.get().year() + 1, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.UNIQUE, oneYearFromNow);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}
	}

	public TiersEditManager getTiersEditManager() {
		return tiersEditManager;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}
}

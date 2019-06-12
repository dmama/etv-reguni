package ch.vd.unireg.tiers.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.dao.RemarqueDAO;
import ch.vd.unireg.tiers.view.DebiteurEditView;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class TiersEditManagerTest extends WebTest {

	private static final String DB_UNIT_FILE = "TiersEditManagerTest.xml";

	private TiersEditManager tiersEditManager;
	private TiersMapHelper tiersMapHelper;
	private RemarqueDAO remarqueDAO;

	/**
	 * @see ch.vd.unireg.common.AbstractCoreDAOTest#onSetUp()
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockIndividuConnector() {
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
		tiersMapHelper = getBean(TiersMapHelper.class, "tiersMapHelper");
		remarqueDAO = getBean(RemarqueDAO.class, "remarqueDAO");
	}

	/**
	 * Teste la methode getView
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetView() throws Exception {

		TiersEditView view = tiersEditManager.getView(6789L);
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
	 * Teste la methode creeEntreprise
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreeEntreprise() {

		TiersEditView view = tiersEditManager.creeEntreprise();
		Tiers tiers = view.getTiers();
		AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
		assertNotNull(autreCommunaute);
	}

	/**
	 * Cas jira UNIREG-3180
	 */
	@Test
	public void testChangeModeCommunicationDebiteur() throws Exception {

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), null));
			return dpi.getNumero();
		});

		DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		assertEquals(ModeCommunication.PAPIER, view.getModeCommunication());
		{
			view.setModeCommunication(ModeCommunication.ELECTRONIQUE);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(ModeCommunication.ELECTRONIQUE, view.getModeCommunication());

			// [SIFISC-12197] vérifions l'insertion de la remarque
			doInNewTransactionAndSession(status -> {
				final List<Remarque> remarques = remarqueDAO.getRemarques(dpiId);
				assertNotNull(remarques);
				assertEquals(1, remarques.size());

				final Remarque remarque = remarques.get(0);
				assertNotNull(remarque);
				assertEquals("Changement de mode de communication :\n'Papier' --> 'Echange de fichier'", remarque.getTexte());
				assertTrue(remarque.getLogCreationUser(), remarque.getLogCreationUser().endsWith("-auto"));
				return null;
			});
		}
		{
			view.setModeCommunication(ModeCommunication.SITE_WEB);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(ModeCommunication.SITE_WEB, view.getModeCommunication());

			// [SIFISC-12197] vérifions l'insertion de la remarque
			doInNewTransactionAndSession(status -> {
				final List<Remarque> remarques = remarqueDAO.getRemarques(dpiId);
				assertNotNull(remarques);
				assertEquals(2, remarques.size());

				final List<Remarque> sorted = new ArrayList<>(remarques);
				Collections.sort(sorted, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

				{
					final Remarque remarque = sorted.get(0);
					assertNotNull(remarque);
					assertEquals("Changement de mode de communication :\n'Papier' --> 'Echange de fichier'", remarque.getTexte());
					assertTrue(remarque.getLogCreationUser(), remarque.getLogCreationUser().endsWith("-auto"));
				}
				{
					final Remarque remarque = sorted.get(1);
					assertNotNull(remarque);
					assertEquals("Changement de mode de communication :\n'Echange de fichier' --> 'Saisie en ligne'", remarque.getTexte());
					assertTrue(remarque.getLogCreationUser(), remarque.getLogCreationUser().endsWith("-auto"));
				}
				return null;
			});
		}
	}

	/**
	 * Cas jira UNIREG-3180
	 */
	@Test
	public void testChangeCategorieImpotSource() throws Exception {

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), null));
			return dpi.getNumero();
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

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), null));
			addForDebiteur(dpi, date(2010, 11, 1), MotifFor.INDETERMINE, null, null, MockCommune.Aigle);
			return dpi.getNumero();
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
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
			assertNotNull(dpi);

			// on trie les périodicité par leur ordre de création
			final List<Periodicite> periodicites = new ArrayList<>(dpi.getPeriodicites());
			Collections.sort(periodicites, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

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
		});

		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			view.setDateDebutNouvellePeriodicite(date(2010, 7, 1));
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getNouvellePeriodicite());
		}
		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
			view.setDateDebutNouvellePeriodicite(date(2010, 10, 1));
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.TRIMESTRIEL, view.getNouvellePeriodicite());
		}
		{
			view.setNouvellePeriodicite(PeriodiciteDecompte.MENSUEL);
			view.setDateDebutNouvellePeriodicite(date(2010, 11, 1));
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.MENSUEL, view.getNouvellePeriodicite());
		}

		// petite vérification en base
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
			assertNotNull(dpi);

			// on trie les périodicité par leur ordre de création
			final List<Periodicite> periodicites = new ArrayList<>(dpi.getPeriodicites());
			Collections.sort(periodicites, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

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
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRefresh() throws AdresseException, InfrastructureException {
		final TiersEditView view = tiersEditManager.getView(6789L);
		view.getTiers().setPersonneContact("toto");
		tiersEditManager.refresh(view, 6789L);
	}

	@Test
	public void testGetDatesPossiblesPourNouvellePeriodiciteAvecLREmise() throws Exception {

		final RegDate today = RegDate.get();
		final int year = today.year();
		final RegDate endOfYear = date(year, 12, 31);

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null));

			addForDebiteur(dpi, date(year, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

			final PeriodeFiscale pf = addPeriodeFiscale(year);
			final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
			addListeRecapitulative(dpi, pf, date(year, 1, 1), date(year, 3, 31), md);
			return dpi.getNumero();
		});

		// cas mensuel (= périodicité plus petite)
		{
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, endOfYear, false);
			assertNotNull(dates);
			assertEquals(3, dates.size());
			assertEquals(date(year, 4, 1), dates.get(0));
			assertEquals(date(year, 7, 1), dates.get(1));
			assertEquals(date(year, 10, 1), dates.get(2));
		}

		// cas trimestriel (= même périodicité)
		{
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, endOfYear, false);
			assertNotNull(dates);
			assertEquals(0, dates.size());
		}

		// cas semestriel (= périodicité plus grande)
		{
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, endOfYear, false);
			assertNotNull(dates);
			assertEquals(1, dates.size());
			assertEquals(date(year, 7, 1), dates.get(0));
		}
	}

	@Test
	public void testGetDatesPossiblesPourDebutNouvellePeriodiciteSansPeriodiciteExistante() throws Exception {

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			return dpi.getNumero();
		});

		for (PeriodiciteDecompte nvelle : PeriodiciteDecompte.values()) {
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, nvelle, RegDate.get().addYears(1), false);
			assertNotNull(nvelle.name(), dates);
			assertEquals(nvelle.name(), 1, dates.size());
			assertEquals(nvelle.name(), date(RegDate.get().year(), 1, 1), dates.get(0));
		}
	}

	@Test
	public void testGetDatesPossiblesPourDebutNouvellePeriodiciteAvecPeriodiciteExistanteActive() throws Exception {

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null));
			return dpi.getNumero();
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

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en TRIMESTRIEL
		{
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(0, dates.size());      // aucune date retournée parce qu'on est déjà en trimestriel
		}

		// passage en SEMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, oneYearFromNow, false);
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

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.ANNUEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en UNIQUE
		{
			final List<RegDate> expectedDates = Arrays.asList(date(RegDate.get().year(), 1, 1), date(RegDate.get().year() + 1, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.UNIQUE, oneYearFromNow, false);
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

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, date(year, 1, 1), null));
			return dpi.getNumero();
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

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en TRIMESTRIEL
		{
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(1, dates.size());      // juste la date de début de la périodicité actuelle
		}

		// passage en SEMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(RegDate.get().year(), 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, oneYearFromNow, false);
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

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.ANNUEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en UNIQUE
		{
			final List<RegDate> expectedDates = Arrays.asList(date(RegDate.get().year(), 1, 1), date(RegDate.get().year() + 1, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.UNIQUE, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}
	}

	@Test
	public void testGetDatesPossiblesPourDebutNouvellePeriodiciteAvecPeriodiciteExistanteNonActive() throws Exception {

		final RegDate today = RegDate.get();
		final int year = today.year();
		final RegDate nextFirstOfJanuary = date(year + 1, 1, 1);

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2010, 1, 1), nextFirstOfJanuary.getOneDayBefore()));
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, nextFirstOfJanuary, null));
			return dpi.getNumero();
		});

		final RegDate oneYearFromNow = today.addYears(1);

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

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en TRIMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBeforeOrEqual(nextFirstOfJanuary)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en SEMESTRIEL
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = nextFirstOfJanuary;           // pas la peine de commencer avant, puisqu'avant c'est déjà semestriel
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
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

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.ANNUEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en UNIQUE
		{
			final List<RegDate> expectedDates = Arrays.asList(date(today.year(), 1, 1), date(today.year() + 1, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.UNIQUE, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}
	}

	@Test
	public void testGetDatesPossiblesPeriodicitePlusGrande() throws Exception {

		final RegDate today = RegDate.get();
		final int year = today.year();
		final RegDate oneYearFromNow = today.addYears(1);
		final RegDate nextNewYear = date(year + 1, 1, 1);

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2010, 1, 1), nextNewYear.getOneDayBefore()));
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, nextNewYear, null));
			return dpi.getNumero();
		});

		// passage en MENSUEL / supergra
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBeforeOrEqual(nextNewYear)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}
			// on ne va pas plus loin car après on est déjà en mensuel

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en MENSUEL / non-supergra -> pas de différence car la périodicité est la même que la dernière existante
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBeforeOrEqual(nextNewYear)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}
			// on ne va pas plus loin car après on est déjà en mensuel

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, oneYearFromNow, true);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en TRIMESTRIEL / supergra
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBefore(nextNewYear)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(3);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en TRIMESTRIEL / non-supergra
		{
			final List<RegDate> expectedDates = Arrays.asList(RegDate.get(year, 1, 1), RegDate.get(year, 7, 1), nextNewYear);
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, oneYearFromNow, true);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en SEMESTRIEL / supergra
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = nextNewYear;      // on ne commence pas avant, c'est déjà semestriel avant
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(6);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en SEMESTRIEL / non-supergra
		{
			final List<RegDate> expectedDates = Collections.singletonList(nextNewYear);         // on ne commence pas avant, c'est déjà semestriel avant
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.SEMESTRIEL, oneYearFromNow, true);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en ANNUEL / supergra
		{
			final List<RegDate> expectedDates = new ArrayList<>();
			RegDate current = date(year, 1, 1);
			while (current.isBeforeOrEqual(oneYearFromNow)) {
				expectedDates.add(current);
				current = current.addMonths(12);
			}

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.ANNUEL, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en ANNUEL / non-supergra
		{
			final List<RegDate> expectedDates = Collections.singletonList(date(year, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.ANNUEL, oneYearFromNow, true);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en UNIQUE / supergra
		{
			final List<RegDate> expectedDates = Arrays.asList(date(today.year(), 1, 1), date(today.year() + 1, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.UNIQUE, oneYearFromNow, false);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}

		// passage en UNIQUE / non-supergra
		{
			final List<RegDate> expectedDates = Arrays.asList(date(today.year(), 1, 1), date(today.year() + 1, 1, 1));
			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.UNIQUE, oneYearFromNow, true);
			assertNotNull(dates);
			assertEquals(Arrays.toString(dates.toArray()) + " vs " + Arrays.toString(expectedDates.toArray()), expectedDates.size(), dates.size());
			for (int i = 0 ; i < expectedDates.size() ; ++ i) {
				assertEquals("index " + i, expectedDates.get(i), dates.get(i));
			}
		}
	}

	/**
	 * Cas où on a joué avec l'IHM, mis une date dans le futur dans la combo des dates possibles et puis qu'on est revenu sur la périodicité
	 * originelle -> la combo a disparu de l'affichage mais sa dernière valeur est tout de même postée dans le formulaire
	 */
	@Test
	public void testConservationMemePeriodiciteAvecDateDifferente() throws Exception {

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2010, 1, 1), null));
			return dpi.getNumero();
		});

		// on reste en SEMESTRIEL
		{
			DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getNouvellePeriodicite());
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getPeriodiciteActive());
			assertEquals(date(2010, 1, 1), view.getDateDebutNouvellePeriodicite());
			{
				view.setNouvellePeriodicite(PeriodiciteDecompte.SEMESTRIEL);
				view.setDateDebutNouvellePeriodicite(date(RegDate.get().year() + 1, 1, 1));
				tiersEditManager.save(view);
				view = tiersEditManager.getDebiteurEditView(dpiId);
				assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getNouvellePeriodicite());
				assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getPeriodiciteActive());
				assertEquals(date(2010, 1, 1), view.getDateDebutNouvellePeriodicite());
			}
		}

		// et en base, rien ne devrait avoir changé non plus
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final Set<Periodicite> allPeriodicites = dpi.getPeriodicites();
			assertNotNull(allPeriodicites);
			assertEquals(1, allPeriodicites.size());

			final Periodicite periodicite = allPeriodicites.iterator().next();
			assertNotNull(periodicite);
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, periodicite.getPeriodiciteDecompte());
			assertEquals(date(2010, 1, 1), periodicite.getDateDebut());
			assertNull(periodicite.getDateFin());
			assertFalse(periodicite.isAnnule());
			return null;
		});
	}

	/**
	 * Cas où la combo avec les dates n'a jamais été montrée à l'IHM -> la date est donc forcément nulle
	 */
	@Test
	public void testDateDebutPeriodiciteNulle() throws Exception {

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2010, 1, 1), null));
			return dpi.getNumero();
		});

		// on reste en SEMESTRIEL
		{
			DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getNouvellePeriodicite());
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getPeriodiciteActive());
			assertEquals(date(2010, 1, 1), view.getDateDebutNouvellePeriodicite());
			{
				view.setNouvellePeriodicite(PeriodiciteDecompte.SEMESTRIEL);
				view.setDateDebutNouvellePeriodicite(null);         // <--- NULL
				tiersEditManager.save(view);
				view = tiersEditManager.getDebiteurEditView(dpiId);
				assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getNouvellePeriodicite());
				assertEquals(PeriodiciteDecompte.SEMESTRIEL, view.getPeriodiciteActive());
				assertEquals(date(2010, 1, 1), view.getDateDebutNouvellePeriodicite());
			}
		}

		// et en base, rien ne devrait avoir changé non plus
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final Set<Periodicite> allPeriodicites = dpi.getPeriodicites();
			assertNotNull(allPeriodicites);
			assertEquals(1, allPeriodicites.size());

			final Periodicite periodicite = allPeriodicites.iterator().next();
			assertNotNull(periodicite);
			assertEquals(PeriodiciteDecompte.SEMESTRIEL, periodicite.getPeriodiciteDecompte());
			assertEquals(date(2010, 1, 1), periodicite.getDateDebut());
			assertNull(periodicite.getDateFin());
			assertFalse(periodicite.isAnnule());
			return null;
		});
	}

	@Test
	public void testRetourMemePeriodiciteApresPassageParAutre() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate debutTrimestriel = today.addMonths(1).getLastDayOfTheMonth().getOneDayAfter().addMonths(3 - (today.month() + 1) % 3);
		assertEquals(1, debutTrimestriel.month() % 3);                  // vérification que la date calculée est bien un début de trimestre
		final RegDate retourMensuel = debutTrimestriel.addMonths(3);    // un seul trimestre

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), debutTrimestriel.getOneDayBefore()));
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, debutTrimestriel, null));
			return dpi.getNumero();
		});

		// on retourne en mensuel
		{
			DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(PeriodiciteDecompte.TRIMESTRIEL, view.getNouvellePeriodicite());
			assertEquals(PeriodiciteDecompte.MENSUEL, view.getPeriodiciteActive());
			assertEquals(debutTrimestriel, view.getDateDebutNouvellePeriodicite());
			{
				view.setNouvellePeriodicite(PeriodiciteDecompte.MENSUEL);
				view.setDateDebutNouvellePeriodicite(retourMensuel);
				tiersEditManager.save(view);
				view = tiersEditManager.getDebiteurEditView(dpiId);
				assertEquals(PeriodiciteDecompte.MENSUEL, view.getNouvellePeriodicite());
				assertEquals(PeriodiciteDecompte.MENSUEL, view.getPeriodiciteActive());
				assertEquals(retourMensuel, view.getDateDebutNouvellePeriodicite());
			}
		}

		// et en base, on devrait voir les trois périodicités : mensuelle, trimestrielle puis mensuelle à nouveau
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final Set<Periodicite> allPeriodicites = dpi.getPeriodicites();
			assertNotNull(allPeriodicites);
			assertEquals(3, allPeriodicites.size());

			final List<Periodicite> sortedPeriodicites = new ArrayList<>(allPeriodicites);
			Collections.sort(sortedPeriodicites, new DateRangeComparator<Periodicite>());

			{
				final Periodicite periodicite = sortedPeriodicites.get(0);
				assertNotNull(periodicite);
				assertEquals(PeriodiciteDecompte.MENSUEL, periodicite.getPeriodiciteDecompte());
				assertEquals(date(2010, 1, 1), periodicite.getDateDebut());
				assertEquals(debutTrimestriel.getOneDayBefore(), periodicite.getDateFin());
				assertFalse(periodicite.isAnnule());
			}
			{
				final Periodicite periodicite = sortedPeriodicites.get(1);
				assertNotNull(periodicite);
				assertEquals(PeriodiciteDecompte.TRIMESTRIEL, periodicite.getPeriodiciteDecompte());
				assertEquals(debutTrimestriel, periodicite.getDateDebut());
				assertEquals(retourMensuel.getOneDayBefore(), periodicite.getDateFin());
				assertFalse(periodicite.isAnnule());
			}
			{
				final Periodicite periodicite = sortedPeriodicites.get(2);
				assertNotNull(periodicite);
				assertEquals(PeriodiciteDecompte.MENSUEL, periodicite.getPeriodiciteDecompte());
				assertEquals(retourMensuel, periodicite.getDateDebut());
				assertNull(periodicite.getDateFin());
				assertFalse(periodicite.isAnnule());
			}
			return null;
		});
	}

	/**
	 * [SIFISC-11532]
	 */
	@Test
	public void testRetourMemePeriodiciteAvecAnnulation() throws Exception {
		final RegDate dateDebut = date(2012, 1, 1);
		final RegDate dateChangement = date(2013, 1, 1);

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, dateDebut);
			addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Bussigny);

			// les périodicités
			dpi.setPeriodicites(new HashSet<>(Arrays.asList(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, dateDebut, dateChangement.getOneDayBefore()),
			                                                new Periodicite(PeriodiciteDecompte.ANNUEL, null, dateChangement, null))));

			final PeriodeFiscale pf = addPeriodeFiscale(2012);
			final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);

			// les LRs sur 2012
			for (int i = 0; i < 4; ++i) {
				final RegDate debutLr = date(2012, i * 3 + 1, 1);
				final RegDate finLr = debutLr.addMonths(3).getOneDayBefore();
				addListeRecapitulative(dpi, pf, debutLr, finLr, md);
			}
			;
			return dpi.getNumero();
		});

		final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.TRIMESTRIEL, date(2015, 1, 1), false);
		assertNotNull(dates);
		assertEquals(3, dates.size());
		assertEquals(date(2013, 1, 1), dates.get(0));
		assertEquals(date(2014, 1, 1), dates.get(1));
		assertEquals(date(2015, 1, 1), dates.get(2));
	}

	/**
	 * [SIFISC-11532] Trimestriel, puis annuel et passage en mensuel (qui écraserait l'annuel)
	 * --> une même date était proposée deux fois dans la liste déroulante
	 */
	@Test
	public void testMensuelApresAnnuelDepuisLongtemps() throws Exception {

		final RegDate dateDebut = date(2009, 1, 1);
		final int lastPfWithLrs = 2012;
		final RegDate dateChangementAnnuel = date(lastPfWithLrs + 1, 1, 1);

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, dateDebut);
			addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Bussigny);

			// les périodicités
			dpi.setPeriodicites(new HashSet<>(Arrays.asList(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, dateDebut, dateChangementAnnuel.getOneDayBefore()),
			                                                new Periodicite(PeriodiciteDecompte.ANNUEL, null, dateChangementAnnuel, null))));

			final PeriodeFiscale pf = addPeriodeFiscale(lastPfWithLrs);
			final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);

			// les LRs sur 2012
			for (int i = 0; i < 4; ++i) {
				final RegDate debutLr = date(lastPfWithLrs, i * 3 + 1, 1);
				final RegDate finLr = debutLr.addMonths(3).getOneDayBefore();
				addListeRecapitulative(dpi, pf, debutLr, finLr, md);
			}
			;
			return dpi.getNumero();
		});

		final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, PeriodiciteDecompte.MENSUEL, date(2015, 1, 1), false);
		assertNotNull(dates);
		assertEquals(3, dates.size());
		assertEquals(date(2013, 1, 1), dates.get(0));
		assertEquals(date(2014, 1, 1), dates.get(1));
		assertEquals(date(2015, 1, 1), dates.get(2));
	}

	@Test
	public void testPeriodiciteNonActiveDejaUtiliseeParListe() throws Exception {

		final int anneeCourante = RegDate.get().year();
		final RegDate dateDebut = date(anneeCourante, 1, 1);
		final RegDate dateChangementMensuel = date(anneeCourante + 1, 1, 1);

		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, dateDebut);
			addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Bussigny);

			final PeriodeFiscale pfCourante = addPeriodeFiscale(anneeCourante);
			final ModeleDocument mdCourante = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pfCourante);

			// les périodicités
			dpi.setPeriodicites(new HashSet<>(Arrays.asList(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, dateDebut, dateChangementMensuel.getOneDayBefore()),
			                                                new Periodicite(PeriodiciteDecompte.MENSUEL, null, dateChangementMensuel, null))));

			// les LR de l'année courante
			for (int i = 0; i < 4; ++i) {
				final RegDate debutLr = date(anneeCourante, i * 3 + 1, 1);
				final RegDate finLr = debutLr.addMonths(3).getOneDayBefore();
				addListeRecapitulative(dpi, pfCourante, debutLr, finLr, mdCourante);
			}
			;
			return dpi.getNumero();
		});

		// quelles sont les périodicités proposées ?
		{
			final DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
			assertNotNull(view);
			assertEquals(PeriodiciteDecompte.MENSUEL, view.getNouvellePeriodicite());
			assertEquals(date(anneeCourante + 1, 1, 1), view.getDateDebutNouvellePeriodicite());
			assertEquals(PeriodiciteDecompte.TRIMESTRIEL, view.getPeriodiciteActive());
			assertEquals(date(anneeCourante, 1, 1), view.getDateDebutPeriodiciteActive());

			final Map<PeriodiciteDecompte, String> map = tiersMapHelper.getMapLimiteePeriodiciteDecompte(view.getPeriodiciteActive());
			assertNotNull(map);
			assertEquals(EnumSet.of(PeriodiciteDecompte.UNIQUE, PeriodiciteDecompte.TRIMESTRIEL, PeriodiciteDecompte.MENSUEL), map.keySet());

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, view.getPeriodiciteActive(), date(anneeCourante + 1, 3, 31), true);
			assertNotNull(dates);
			assertEquals(1, dates.size());
			assertEquals(date(anneeCourante + 1, 1, 1), dates.get(0));      // pour annuler la périodicité mensuelle non-encore utilisée
		}

		// maintenant on ajoute une LR sur l'année suivante (mensuelle, donc)
		doInNewTransactionAndSession(status -> {
			final PeriodeFiscale pfSuivante = addPeriodeFiscale(anneeCourante + 1);
			final ModeleDocument mdSuivante = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pfSuivante);

			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			addListeRecapitulative(dpi, pfSuivante, date(anneeCourante + 1, 1, 1), date(anneeCourante + 1, 1, 31), mdSuivante);
			return null;
		});

		// quelles sont les périodicités proposées ?
		{
			final DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
			assertNotNull(view);
			assertEquals(PeriodiciteDecompte.MENSUEL, view.getNouvellePeriodicite());
			assertEquals(date(anneeCourante + 1, 1, 1), view.getDateDebutNouvellePeriodicite());
			assertEquals(PeriodiciteDecompte.MENSUEL, view.getPeriodiciteActive());
			assertEquals(date(anneeCourante + 1, 1, 1), view.getDateDebutPeriodiciteActive());

			final Map<PeriodiciteDecompte, String> map = tiersMapHelper.getMapLimiteePeriodiciteDecompte(view.getPeriodiciteActive());
			assertNotNull(map);
			assertEquals(EnumSet.of(PeriodiciteDecompte.UNIQUE, PeriodiciteDecompte.MENSUEL), map.keySet());

			final List<RegDate> dates = tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, view.getPeriodiciteActive(), date(anneeCourante + 1, 3, 31), true);
			assertNotNull(dates);
			assertEquals(0, dates.size());
		}
	}

	/**
	 * [SIFISC-14671] Cas d'un débiteur à périodicité unique dont la LR de l'année a déjà été émise, et dont on change
	 * la périodicité dès l'année suivante, en la conservant unique mais en changeant seulement la période de décompte
	 * (voir UNIREG-2683 pour l'ancienne implémentation, qui écrasait la périodicité courante)
	 */
	@Test
	public void testModificationPeriodeDecompteSurPeriodiciteUniqueAvecLrDejaEmise() throws Exception {

		final int lastYear = RegDate.get().year() - 1;

		// Création du DPI, de son for, de sa périodicité unique (juin) et de sa LR
		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(lastYear, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M06, date(lastYear, 1, 1), null));

			addForDebiteur(dpi, date(lastYear, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);

			final PeriodeFiscale pf = addPeriodeFiscale(lastYear);
			final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
			addListeRecapitulative(dpi, pf, date(lastYear, 6, 1), date(lastYear, 6, 30), md);
			return dpi.getNumero();
		});

		// modification dans l'écran de la périodicité dès le 1.1, en restant unique mais en changeant de période de décompte (s1)
		final DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		view.setDateDebutNouvellePeriodicite(date(lastYear + 1, 1, 1));
		view.setNouvellePeriodicite(PeriodiciteDecompte.UNIQUE);
		view.setPeriodeDecompte(PeriodeDecompte.S1);
		tiersEditManager.save(view);

		// on vérifie maintenant qu'une nouvelle périodicité a bien été créée dès la date demandée (et que la précédente a bien été conservée)
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final List<Periodicite> periodicites = dpi.getPeriodicitesSorted();
			assertEquals(2, periodicites.size());
			{
				final Periodicite p = periodicites.get(0);
				assertNotNull(p);
				assertEquals(date(lastYear, 1, 1), p.getDateDebut());
				assertEquals(date(lastYear, 12, 31), p.getDateFin());
				assertEquals(PeriodiciteDecompte.UNIQUE, p.getPeriodiciteDecompte());
				assertEquals(PeriodeDecompte.M06, p.getPeriodeDecompte());
				assertFalse(p.isAnnule());
			}
			{
				final Periodicite p = periodicites.get(1);
				assertNotNull(p);
				assertEquals(date(lastYear + 1, 1, 1), p.getDateDebut());
				assertNull(p.getDateFin());
				assertEquals(PeriodiciteDecompte.UNIQUE, p.getPeriodiciteDecompte());
				assertEquals(PeriodeDecompte.S1, p.getPeriodeDecompte());
				assertFalse(p.isAnnule());
			}
			return null;
		});
	}

	/**
	 * [SIFISC-14671] Cas d'un débiteur à périodicité unique dont la LR de l'année n'a pas encore été émise, et dont on change
	 * la périodicité dès l'année suivante, en la conservant unique mais en changeant seulement la période de décompte
	 * (voir UNIREG-2683 pour l'ancienne implémentation, qui écrasait la périodicité courante)
	 */
	@Test
	public void testModificationPeriodeDecompteSurPeriodiciteUniqueAvecLrNonEmise() throws Exception {

		final int lastYear = RegDate.get().year() - 1;

		// Création du DPI, de son for, de sa périodicité unique (juin) et de sa LR
		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(lastYear, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M06, date(lastYear, 1, 1), null));

			addForDebiteur(dpi, date(lastYear, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);
			return dpi.getNumero();
		});

		// modification dans l'écran de la périodicité dès le 1.1, en restant unique mais en changeant de période de décompte (s1)
		final DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		view.setDateDebutNouvellePeriodicite(date(lastYear + 1, 1, 1));
		view.setNouvellePeriodicite(PeriodiciteDecompte.UNIQUE);
		view.setPeriodeDecompte(PeriodeDecompte.S1);
		tiersEditManager.save(view);

		// on vérifie maintenant qu'une nouvelle périodicité a bien été créée dès la date demandée (et que la précédente a bien été conservée)
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final List<Periodicite> periodicites = dpi.getPeriodicitesSorted();
			assertEquals(2, periodicites.size());
			{
				final Periodicite p = periodicites.get(0);
				assertNotNull(p);
				assertEquals(date(lastYear, 1, 1), p.getDateDebut());
				assertEquals(date(lastYear, 12, 31), p.getDateFin());
				assertEquals(PeriodiciteDecompte.UNIQUE, p.getPeriodiciteDecompte());
				assertEquals(PeriodeDecompte.M06, p.getPeriodeDecompte());
				assertFalse(p.isAnnule());
			}
			{
				final Periodicite p = periodicites.get(1);
				assertNotNull(p);
				assertEquals(date(lastYear + 1, 1, 1), p.getDateDebut());
				assertNull(p.getDateFin());
				assertEquals(PeriodiciteDecompte.UNIQUE, p.getPeriodiciteDecompte());
				assertEquals(PeriodeDecompte.S1, p.getPeriodeDecompte());
				assertFalse(p.isAnnule());
			}
			return null;
		});
	}

	/**
	 * [SIFISC-14671] Cas d'un débiteur à périodicité unique dont la LR de l'année n'a pas encore été émise, et dont on change
	 * la périodicité dès l'année suivante, en la conservant unique mais en changeant seulement la période de décompte
	 * (voir UNIREG-2683 pour l'ancienne implémentation, qui écrasait la périodicité courante)
	 */
	@Test
	public void testConservationPeriodeDecompteSurPeriodiciteUniqueAvecLrNonEmise() throws Exception {

		final int lastYear = RegDate.get().year() - 1;

		// Création du DPI, de son for, de sa périodicité unique (juin) et de sa LR
		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(lastYear, 1, 1));
			dpi.setModeCommunication(ModeCommunication.PAPIER);
			dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M06, date(lastYear, 1, 1), null));

			addForDebiteur(dpi, date(lastYear, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);
			return dpi.getNumero();
		});

		// modification dans l'écran de la périodicité dès le 1.1, en restant unique et en conservant la période de décompte
		final DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		view.setDateDebutNouvellePeriodicite(date(lastYear + 1, 1, 1));
		view.setNouvellePeriodicite(PeriodiciteDecompte.UNIQUE);
		view.setPeriodeDecompte(PeriodeDecompte.M06);       // la même qu'avant
		tiersEditManager.save(view);

		// on vérifie maintenant qu'aucune nouvelle périodicité n'a été créée
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final List<Periodicite> periodicites = dpi.getPeriodicitesSorted();
			assertEquals(1, periodicites.size());
			{
				final Periodicite p = periodicites.get(0);
				assertNotNull(p);
				assertEquals(date(lastYear, 1, 1), p.getDateDebut());
				assertNull(p.getDateFin());
				assertEquals(PeriodiciteDecompte.UNIQUE, p.getPeriodiciteDecompte());
				assertEquals(PeriodeDecompte.M06, p.getPeriodeDecompte());
				assertFalse(p.isAnnule());
			}
			return null;
		});
	}
}

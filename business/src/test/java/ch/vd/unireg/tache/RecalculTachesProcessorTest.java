package ch.vd.unireg.tache;

import java.util.List;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.BusinessTestingConstants;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class RecalculTachesProcessorTest extends BusinessTest {

	private ParametreAppService paramAppService;
	private Integer premierePeriodeFiscaleDeclarationPM;

	private RecalculTachesProcessor processor;
	private TacheDAO tacheDAO;
	private PeriodeFiscaleDAO periodeFiscaleDAO;

	private static final int PREMIERE_PERIODE_DECLARATIONS_PM = 2014;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");

		final TacheService tacheService = getBean(TacheService.class, "tacheService");
		processor = new RecalculTachesProcessor(transactionManager, hibernateTemplate, tacheService, tacheSynchronizer);

		doInNewTransactionAndSession(status -> {
			for (int pf = 2003; pf <= RegDate.get().year(); ++pf) {
				addPeriodeFiscale(pf);
			}
			return null;
		});

		paramAppService = getBean(ParametreAppService.class, "parametreAppService");
		premierePeriodeFiscaleDeclarationPM = paramAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		paramAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(PREMIERE_PERIODE_DECLARATIONS_PM);
	}

	@Override
	public void onTearDown() throws Exception {
		if (premierePeriodeFiscaleDeclarationPM != null) {
			paramAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(premierePeriodeFiscaleDeclarationPM);
		}
		super.onTearDown();
	}

	@Test
	public void testCreationTachePP() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateArrivee = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
			return pp.getNumero();
		});

		// vérification que la tâche d'envoi de DI n'est pas là...
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(0, taches.size());
			return null;
		});

		// utilisation du processeur (mode cleanup)
		{
			final TacheSyncResults resCleanup = processor.run(true, 1, RecalculTachesProcessor.Scope.PP, null);
			assertNotNull(resCleanup);
			assertEquals(0, resCleanup.getActions().size());
			assertEquals(0, resCleanup.getExceptions().size());
		}

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults resFull = processor.run(false, 1, RecalculTachesProcessor.Scope.PP, null);
			assertNotNull(resFull);
			assertEquals(0, resFull.getExceptions().size());
			assertEquals(1, resFull.getActions().size());

			final TacheSyncResults.ActionInfo info = resFull.getActions().get(0);
			assertNotNull(info);
			assertEquals(ppId, info.ctbId);
			assertEquals(String.format("création d'une tâche d'émission de déclaration d'impôt PP ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateArrivee), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});
	}

	@Test
	public void testCreationTachePM() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi de DI n'est pas là...
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(0, taches.size());
			return null;
		});

		// utilisation du processeur (mode cleanup)
		{
			final TacheSyncResults resCleanup = processor.run(true, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(resCleanup);
			assertEquals(0, resCleanup.getActions().size());
			assertEquals(0, resCleanup.getExceptions().size());
		}

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults resFull = processor.run(false, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(resFull);
			assertEquals(0, resFull.getExceptions().size());
			assertEquals(1, resFull.getActions().size());

			final TacheSyncResults.ActionInfo info = resFull.getActions().get(0);
			assertNotNull(info);
			assertEquals(pmId, info.ctbId);
			assertEquals(String.format("création d'une tâche d'émission de déclaration d'impôt PM ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateDebutExploitation), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});
	}

	@Test
	public void testCreationTacheSNC() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SC);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Bex.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.REVENU_FORTUNE);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi de questionnaire n'est pas là...
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(0, taches.size());
			return null;
		});

		// utilisation du processeur (mode cleanup)
		{
			final TacheSyncResults resCleanup = processor.run(true, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(resCleanup);
			assertEquals(0, resCleanup.getActions().size());
			assertEquals(0, resCleanup.getExceptions().size());
		}

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults resFull = processor.run(false, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(resFull);
			assertEquals(0, resFull.getExceptions().size());
			assertEquals(1, resFull.getActions().size());

			final TacheSyncResults.ActionInfo info = resFull.getActions().get(0);
			assertNotNull(info);
			assertEquals(pmId, info.ctbId);
			assertEquals(String.format("création d'une tâche d'émission de questionnaire SNC couvrant la période du 01.01.%d au 31.12.%d", year - 1, year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de questionnaire SNC est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			assertEquals(date(year - 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year - 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			return null;
		});
	}

	@Test
	public void testCleanupPP() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateArrivee = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long ppId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
			return pp.getNumero();
		});

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de DI est bien toujours là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode cleanup)
		{
			final TacheSyncResults res = processor.run(true, 1, RecalculTachesProcessor.Scope.PP, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(ppId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi de la déclaration d'impôt PP ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateArrivee), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est maintenant annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			return null;
		});
	}

	@Test
	public void testCleanupPM() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final ForFiscalPrincipal ffp = e.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de DI est bien toujours là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode cleanup)
		{
			final TacheSyncResults res = processor.run(true, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(pmId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi de la déclaration d'impôt PM ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateDebutExploitation), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est maintenant annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			return null;
		});
	}

	@Test
	public void testCleanupSNC() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SC);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Bex.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.REVENU_FORTUNE);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi de questionnaire SNC est bien là
		// (parce que l'intercepteur était enclenché ici)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final ForFiscalPrincipal ffp = e.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de questionnaire est bien toujours là, non-annulée
		// (parce que l'intercepteur n'est pas enclenché !!)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode cleanup)
		{
			final TacheSyncResults res = processor.run(true, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(pmId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi du questionnaire SNC couvrant la période du 01.01.%d au 31.12.%d", year - 1, year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi est maintenant annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			assertEquals(date(year - 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year - 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			return null;
		});
	}

	@Test
	public void testCleanupModeFullPP() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateArrivee = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long ppId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
			return pp.getNumero();
		});

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de DI est bien toujours là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults res = processor.run(false, 1, RecalculTachesProcessor.Scope.PP, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(ppId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi de la déclaration d'impôt PP ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateArrivee), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est maintenant annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			return null;
		});
	}

	@Test
	public void testCleanupModeFullPM() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final ForFiscalPrincipal ffp = e.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de DI est bien toujours là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults res = processor.run(false, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(pmId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi de la déclaration d'impôt PM ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateDebutExploitation), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est maintenant annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			return null;
		});
	}

	@Test
	public void testCleanupModeFullSNC() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SC);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Bex.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.REVENU_FORTUNE);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi est bien là
		// (parce que l'intercepteur était enclenché)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			assertEquals(date(year - 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year - 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final ForFiscalPrincipal ffp = e.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi est bien toujours là, non-annulée
		// (parce que l'intercepteur n'était pas enclenché)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			assertEquals(date(year - 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year - 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			return null;
		});

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults res = processor.run(false, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(pmId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi du questionnaire SNC couvrant la période du 01.01.%d au 31.12.%d", year - 1, year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est maintenant annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			assertEquals(date(year - 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year - 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			return null;
		});
	}

	@Test
	public void testTacheDejaAnnuleePP() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateArrivee = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long ppId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
			return pp.getNumero();
		});

		// vérification que la tâche d'envoi de DI est bien là -> on l'annule
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			tache.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de DI est bien annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode cleanup) : rien ne doit être traité car toutes les tâches du contribuable sont annulées
		{
			final TacheSyncResults res = processor.run(true, 1, RecalculTachesProcessor.Scope.PP, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(0, res.getActions().size());
		}
	}

	@Test
	public void testTacheDejaAnnuleePM() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi de DI est bien là -> on l'annule
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			tache.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de DI est bien annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode cleanup) : rien ne doit être traité car toutes les tâches du contribuable sont annulées
		{
			final TacheSyncResults res = processor.run(true, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(0, res.getActions().size());
		}
	}

	@Test
	public void testTacheDejaAnnuleeSNC() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SC);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Bex.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.REVENU_FORTUNE);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi est bien là -> on l'annule
		// (elle a été créée car l'intecepteur était enclenché)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			assertEquals(date(year - 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year - 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			tache.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi est bien annulée
		// (l'intercepteur n'a rien modifié, vu qu'il n'était pas enclenché)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode cleanup) : rien ne doit être traité car toutes les tâches du contribuable sont annulées
		{
			final TacheSyncResults res = processor.run(true, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(0, res.getActions().size());
		}
	}

	@Test
	public void testScopePPSurEntreprise() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final ForFiscalPrincipal ffp = e.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de DI est bien toujours là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults res = processor.run(false, 1, RecalculTachesProcessor.Scope.PP, null);       // <-- le scope ne correspond pas à une entreprise
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(0, res.getActions().size());
		}

		// vérification que la tâche d'envoi de DI est toujours identique
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});
	}

	@Test
	public void testScopePMSurPersonnePhysique() throws Exception {

		final int year = RegDate.get().year();
		final RegDate dateArrivee = date(year - 1, 3, 12);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long ppId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
			return pp.getNumero();
		});

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi de DI est bien toujours là
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults res = processor.run(false, 1, RecalculTachesProcessor.Scope.PM, null);       // <-- mauvais scope sur une personne physique
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(0, res.getActions().size());
		}

		// vérification que la tâche d'envoi de DI n'a pas bougé
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(ppId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiDeclarationImpotPP, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			return null;
		});
	}

	/**
	 * [SIFISC-18732] Cas de questionnaires SNC existants (= migrés depuis RegPM) avant 2016 (donc 2014 dans ces tests)
	 * pour lesquels la mécanique de recalcul des tâches générait des tâches d'annulation (alors que les tâches ne doivent
	 * rien gérer - ni émission ni annulation - avant la période 2016...)
	 */
	@Test
	public void testNonAnnulationQuestionnaireSNCAvantPremierePeriodeDeclarationPM() throws Exception {

		final int year = PREMIERE_PERIODE_DECLARATIONS_PM - 1;
		final RegDate dateDebutExploitation = date(year - 1, 3, 12);
		final RegDate dateFinExploitation = date(PREMIERE_PERIODE_DECLARATIONS_PM, 5, 3);

		// mise en place civile -> personne !
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un monde vierge...
			}
		});

		// y compris au niveau des entreprises...
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// ... complètement vierge...
			}
		});

		// mise en place fiscale en générant la tâche qui va bien
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebutExploitation, null, "Megatrucs");
			addFormeJuridique(e, dateDebutExploitation, null, FormeJuridiqueEntreprise.SC);
			addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addBouclement(e, dateDebutExploitation, DayMonth.get(12, 31), 12);
			addForPrincipal(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, dateFinExploitation, MotifFor.FIN_EXPLOITATION, MockCommune.Bex.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE,
			                GenreImpot.REVENU_FORTUNE);

			final PeriodeFiscale pfAvant = periodeFiscaleDAO.getPeriodeFiscaleByYear(year);
			addQuestionnaireSNC(e, pfAvant);
			return e.getNumero();
		});

		// vérification que la tâche d'envoi est bien là
		// (parce que l'intercepteur était enclenché)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());         // tâche d'émission, pas de tâche d'annulation

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			assertEquals(date(year + 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year + 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			return null;
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final ForFiscalPrincipal ffp = e.getDernierForFiscalPrincipal();
			ffp.setAnnule(true);
			return null;
		});

		// vérification que la tâche d'envoi est bien toujours là, non-annulée
		// (parce que l'intercepteur n'était pas enclenché)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertFalse(tache.isAnnule());
			assertEquals(date(year + 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year + 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			return null;
		});

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults res = processor.run(false, 1, RecalculTachesProcessor.Scope.PM, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(pmId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi du questionnaire SNC couvrant la période du 01.01.%d au 31.12.%d", year + 1, year + 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est maintenant annulée
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
			assertTrue(tache.isAnnule());
			assertEquals(date(year + 1, 1, 1), ((TacheEnvoiQuestionnaireSNC) tache).getDateDebut());
			assertEquals(date(year + 1, 12, 31), ((TacheEnvoiQuestionnaireSNC) tache).getDateFin());
			return null;
		});


	}
}

package ch.vd.uniregctb.tache;

import java.util.List;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class RecalculTachesProcessorTest extends BusinessTest {

	private RecalculTachesProcessor processor;
	private TacheDAO tacheDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		tacheDAO = getBean(TacheDAO.class, "tacheDAO");

		final TacheService tacheService = getBean(TacheService.class, "tacheService");
		processor = new RecalculTachesProcessor(transactionManager, hibernateTemplate, tacheService, tacheSynchronizer);

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				for (int pf = 2003 ; pf <= RegDate.get().year() ; ++ pf) {
					addPeriodeFiscale(pf);
				}
				return null;
			}
		});
	}

	@Test
	public void testCreationTache() throws Exception {

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
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
				return pp.getNumero();
			}
		});

		// vérification que la tâche d'envoi de DI n'est pas là...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(0, taches.size());
				return null;
			}
		});

		// utilisation du processeur (mode cleanup)
		{
			final TacheSyncResults resCleanup = processor.run(true, 1, null);
			assertNotNull(resCleanup);
			assertEquals(0, resCleanup.getActions().size());
			assertEquals(0, resCleanup.getExceptions().size());
		}

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults resFull = processor.run(false, 1, null);
			assertNotNull(resFull);
			assertEquals(0, resFull.getExceptions().size());
			assertEquals(1, resFull.getActions().size());

			final TacheSyncResults.ActionInfo info = resFull.getActions().get(0);
			assertNotNull(info);
			assertEquals(ppId, info.ctbId);
			assertEquals(String.format("création d'une tâche d'émission de déclaration d'impôt ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateArrivee), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertFalse(tache.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testCleanup() throws Exception {

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
		final long ppId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
				return pp.getNumero();
			}
		});

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertFalse(tache.isAnnule());
				return null;
			}
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				ffp.setAnnule(true);
				return null;
			}
		});

		// vérification que la tâche d'envoi de DI est bien toujours là
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertFalse(tache.isAnnule());
				return null;
			}
		});

		// utilisation du processeur (mode cleanup)
		{
			final TacheSyncResults res = processor.run(true, 1, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(ppId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi de la déclaration d'impôt ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateArrivee), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est maintenant annulée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertTrue(tache.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testCleanupModeFull() throws Exception {

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
		final long ppId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
				return pp.getNumero();
			}
		});

		// vérification que la tâche d'envoi de DI est bien là
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertFalse(tache.isAnnule());
				return null;
			}
		});

		// annulation du for fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				ffp.setAnnule(true);
				return null;
			}
		});

		// vérification que la tâche d'envoi de DI est bien toujours là
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertFalse(tache.isAnnule());
				return null;
			}
		});

		// utilisation du processeur (mode full)
		{
			final TacheSyncResults res = processor.run(false, 1, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(1, res.getActions().size());

			final TacheSyncResults.ActionInfo info = res.getActions().get(0);
			assertNotNull(info);
			assertEquals(ppId, info.ctbId);
			assertEquals(String.format("annulation de la tâche d'envoi de la déclaration d'impôt ordinaire couvrant la période du %s au 31.12.%d", RegDateHelper.dateToDisplayString(dateArrivee), year - 1), info.actionMsg);
		}

		// vérification que la tâche d'envoi de DI est maintenant annulée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertTrue(tache.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testTacheDejaAnnulee() throws Exception {

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
		final long ppId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Guillaume", "Le Conquérant", date(1966, 4, 12), Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
				return pp.getNumero();
			}
		});

		// vérification que la tâche d'envoi de DI est bien là -> on l'annule
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertFalse(tache.isAnnule());
				tache.setAnnule(true);
				return null;
			}
		});

		// vérification que la tâche d'envoi de DI est bien annulée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertTrue(tache.isAnnule());
				return null;
			}
		});

		// utilisation du processeur (mode cleanup) : rien ne doit être traité car toutes les tâches du contribuable sont annulées
		{
			final TacheSyncResults res = processor.run(true, 1, null);
			assertNotNull(res);
			assertEquals(0, res.getExceptions().size());
			assertEquals(0, res.getActions().size());
		}
	}
}

package ch.vd.uniregctb.tache.sync;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscalePP;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeTache;

public class AddDITest extends BusinessTest {

	private TacheDAO tacheDAO;
	private DeclarationImpotService diService;
	private DeclarationImpotOrdinaireDAO diDAO;
	private PeriodeFiscaleDAO pfDAO;
	private PeriodeImpositionService periodeImpositionService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		diService = getBean(DeclarationImpotService.class, "diService");
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		pfDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
	}

	/**
	 * Classe utilisée ici pour les tests à la place de la vraie classe pour simuler la date du jour
	 */
	private static final class MyAddDI extends AddDI {

		private final RegDate today;

		/**
		 * @param periodeImposition la période d'imposition qui conduit à l'ajout de la tâche
		 * @param today la date du jour à considérer (si <code>null</code>, alors la vraie implémentation de la classe de base sera utilisée)
		 */
		public MyAddDI(@NotNull PeriodeImpositionPersonnesPhysiques periodeImposition, RegDate today) {
			super(periodeImposition);
			this.today = today;
		}

		@Override
		protected RegDate getToday() {
			return today == null ? super.getToday() : today;
		}
	}

	@Test
	public void testDateEcheanceDebutAnneeSuivanteTresPresDeLaLimite() throws Exception {

		// service civil vide...
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final int currentYear = RegDate.get().year();
		final RegDate dateEnvoiMasseDI = Tache.getDefaultEcheance(date(currentYear + 1, 1, 15)).addDays(-4);    // un mercredi...

		// création d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Otto", "Rhino", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(currentYear, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				final PeriodeFiscale pf = addPeriodeFiscale(currentYear);
				pf.addAllPeriodeFiscaleParametresPP(dateEnvoiMasseDI, date(currentYear + 1, 3, 15), date(currentYear + 1, 6, 30));
				return pp.getNumero();
			}
		});

		// on vérifie qu'aucune tâche d'envoi de DI n'a été créée jusqu'ici (c'est pour ça qu'on a mis l'arrivée HS au premier janvier de l'année courante)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
				return null;
			}
		});

		// on se place en tout début d'année suivante
		doInNewTransactionAndSession(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RegDate ref = dateEnvoiMasseDI.addDays(-1);       // donc la date d'échéance normale (= prochain dimanche) est postérieure à la date limite
				Assert.assertEquals(currentYear + 1, ref.year());       // pour être sûr qu'on n'a pas changé d'année..

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final CollectiviteAdministrative ca = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
				final CollectiviteAdministrative caSuccessions = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACISuccessions);
				final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(pp, currentYear);
				Assert.assertNotNull(periodesImposition);
				Assert.assertEquals(1, periodesImposition.size());
				final PeriodeImposition periodeImposition = periodesImposition.get(0);
				Assert.assertNotNull(periodeImposition);
				assertInstanceOf(PeriodeImpositionPersonnesPhysiques.class, periodeImposition);

				final Context ctx = new Context(pp, ca, tacheDAO, diService, caSuccessions, diDAO,  pfDAO);
				final AddDI add = new MyAddDI((PeriodeImpositionPersonnesPhysiques) periodeImposition, ref);
				add.execute(ctx);
				return null;
			}
		});

		// vérification de la tâche créée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);

				// la date attendue est le dimanche suivant la limite
				RegDate echeanceAttendue = dateEnvoiMasseDI;
				while (echeanceAttendue.getWeekDay() != RegDate.WeekDay.SUNDAY) {
					echeanceAttendue = echeanceAttendue.getOneDayAfter();
				}

				Assert.assertEquals(echeanceAttendue, tache.getDateEcheance());
				return null;
			}
		});
	}

	@Test
	public void testDateEcheanceDebutAnneeSuivanteAssezLoinDeLaLimite() throws Exception {

		// service civil vide...
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final int currentYear = RegDate.get().year();
		final RegDate dateEnvoiMasseDI = Tache.getDefaultEcheance(date(currentYear + 1, 1, 15)).addDays(-4);    // un mercredi...

		// création d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Otto", "Rhino", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(currentYear, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				final PeriodeFiscale pf = addPeriodeFiscale(currentYear);
				pf.addAllPeriodeFiscaleParametresPP(dateEnvoiMasseDI, date(currentYear + 1, 3, 15), date(currentYear + 1, 6, 30));
				return pp.getNumero();
			}
		});

		// on vérifie qu'aucune tâche d'envoi de DI n'a été créée jusqu'ici (c'est pour ça qu'on a mis l'arrivée HS au premier janvier de l'année courante)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
				return null;
			}
		});

		// on se place en tout début d'année suivante
		doInNewTransactionAndSession(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RegDate ref = dateEnvoiMasseDI.addDays(-4);       // un samedi (donc le lendemain serait la date d'échéance normale, sauf que c'est trop tôt)
				Assert.assertEquals(currentYear + 1, ref.year());       // pour être sûr qu'on n'a pas changé d'année..

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final CollectiviteAdministrative ca = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
				final CollectiviteAdministrative caSuccessions = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACISuccessions);
				final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(pp, currentYear);
				Assert.assertNotNull(periodesImposition);
				Assert.assertEquals(1, periodesImposition.size());
				final PeriodeImposition periodeImposition = periodesImposition.get(0);
				Assert.assertNotNull(periodeImposition);
				assertInstanceOf(PeriodeImpositionPersonnesPhysiques.class, periodeImposition);

				final Context ctx = new Context(pp, ca, tacheDAO, diService, caSuccessions, diDAO,  pfDAO);
				final AddDI add = new MyAddDI((PeriodeImpositionPersonnesPhysiques) periodeImposition, ref);
				add.execute(ctx);
				return null;
			}
		});

		// vérification de la tâche créée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertEquals(dateEnvoiMasseDI, tache.getDateEcheance());
				return null;
			}
		});
	}

	@Test
	public void testDateEcheanceMilieuAnneeSuivante() throws Exception {

		// service civil vide...
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final int currentYear = RegDate.get().year();
		final RegDate dateEnvoiMasseDI = date(currentYear + 1, 2, 1);
		final RegDate dateReference = dateEnvoiMasseDI.addDays(4);

		// création d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Otto", "Rhino", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(currentYear, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				final PeriodeFiscale pf = addPeriodeFiscale(currentYear);
				pf.addAllPeriodeFiscaleParametresPP(dateEnvoiMasseDI, date(currentYear + 1, 3, 15), date(currentYear + 1, 6, 30));
				return pp.getNumero();
			}
		});

		// on vérifie qu'aucune tâche d'envoi de DI n'a été créée jusqu'ici (c'est pour ça qu'on a mis l'arrivée HS au premier janvier de l'année courante)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
				return null;
			}
		});

		// on se place en tout début d'année suivante
		doInNewTransactionAndSession(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final CollectiviteAdministrative ca = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
				final CollectiviteAdministrative caSuccessions = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACISuccessions);
				final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(pp, currentYear);
				Assert.assertNotNull(periodesImposition);
				Assert.assertEquals(1, periodesImposition.size());
				final PeriodeImposition periodeImposition = periodesImposition.get(0);
				Assert.assertNotNull(periodeImposition);
				assertInstanceOf(PeriodeImpositionPersonnesPhysiques.class, periodeImposition);

				final Context ctx = new Context(pp, ca, tacheDAO, diService, caSuccessions, diDAO,  pfDAO);
				final AddDI add = new MyAddDI((PeriodeImpositionPersonnesPhysiques) periodeImposition, dateReference);
				add.execute(ctx);
				return null;
			}
		});

		// vérification de la tâche créée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);

				// la date attendue est dimanche prochain
				RegDate echeanceAttendue = dateReference;
				while (echeanceAttendue.getWeekDay() != RegDate.WeekDay.SUNDAY) {
					echeanceAttendue = echeanceAttendue.getOneDayAfter();
				}

				Assert.assertEquals(echeanceAttendue, tache.getDateEcheance());
				return null;
			}
		});
	}

	@Test
	public void testDateEcheanceResultanteParTypeContribuable() throws Exception {

		// service civil vide...
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final int currentYear = RegDate.get().year();
		final RegDate dateLimiteOrdinaire = date(currentYear + 1, 1, 31);
		final RegDate dateLimiteICCD = date(currentYear + 1, 2, 28);

		class Ids {
			final long ordId;
			final long iccdId;

			Ids(long ordId, long iccdId) {
				this.ordId = ordId;
				this.iccdId = iccdId;
			}
		}

		// création d'un contribuable
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PeriodeFiscale pf = addPeriodeFiscale(currentYear);
				final Set<ParametrePeriodeFiscale> params = new HashSet<>();
				params.add(new ParametrePeriodeFiscalePP(TypeContribuable.VAUDOIS_ORDINAIRE, dateLimiteOrdinaire, date(currentYear + 1, 3, 15), date(currentYear + 1, 6, 30), pf));
				params.add(new ParametrePeriodeFiscalePP(TypeContribuable.VAUDOIS_DEPENSE, dateLimiteICCD, date(currentYear + 1, 3, 15), date(currentYear + 1, 6, 30), pf));
				pf.setParametrePeriodeFiscale(params);

				// contribuable vaudois ordinaire
				final PersonnePhysique ord = addNonHabitant("Otto", "Rhino", null, Sexe.MASCULIN);
				addForPrincipal(ord, date(currentYear, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);

				// contribuable vaudois ICCD
				final PersonnePhysique iccd = addNonHabitant("Michel", "Schummi", null, Sexe.MASCULIN);
				addForPrincipal(iccd, date(currentYear, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny, ModeImposition.DEPENSE);

				return new Ids(ord.getNumero(), iccd.getNumero());
			}
		});

		// on vérifie qu'aucune tâche d'envoi de DI n'a été créée jusqu'ici (c'est pour ça qu'on a mis les arrivées HS au premier janvier de l'année courante)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
				return null;
			}
		});

		// on se place en tout début d'année suivante
		doInNewTransactionAndSession(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RegDate ref = date(currentYear + 1, 1, 2);
				final CollectiviteAdministrative ca = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
				final CollectiviteAdministrative caSuccessions = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACISuccessions);

				// contribuable ordinaire
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ordId);
					final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(pp, currentYear);
					Assert.assertNotNull(periodesImposition);
					Assert.assertEquals(1, periodesImposition.size());
					final PeriodeImposition periodeImposition = periodesImposition.get(0);
					Assert.assertNotNull(periodeImposition);
					assertInstanceOf(PeriodeImpositionPersonnesPhysiques.class, periodeImposition);
					Assert.assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, periodeImposition.getTypeContribuable());

					final Context ctx = new Context(pp, ca, tacheDAO, diService, caSuccessions, diDAO,  pfDAO);
					final AddDI add = new MyAddDI((PeriodeImpositionPersonnesPhysiques) periodeImposition, ref);
					add.execute(ctx);
				}

				// contribuable iccd
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.iccdId);
					final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(pp, currentYear);
					Assert.assertNotNull(periodesImposition);
					Assert.assertEquals(1, periodesImposition.size());
					final PeriodeImposition periodeImposition = periodesImposition.get(0);
					Assert.assertNotNull(periodeImposition);
					assertInstanceOf(PeriodeImpositionPersonnesPhysiques.class, periodeImposition);
					Assert.assertEquals(TypeContribuable.VAUDOIS_DEPENSE, periodeImposition.getTypeContribuable());

					final Context ctx = new Context(pp, ca, tacheDAO, diService, caSuccessions, diDAO,  pfDAO);
					final AddDI add = new MyAddDI((PeriodeImpositionPersonnesPhysiques) periodeImposition, ref);
					add.execute(ctx);
				}

				return null;
			}
		});

		// vérification des tâches créées
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// contribuable ordinaire
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ordId);
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);

					// la date attendue à la limite "ordinaire"
					Assert.assertEquals(dateLimiteOrdinaire, tache.getDateEcheance());
				}

				// contribuable ICCD
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.iccdId);
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);

					// la date attendue à la limite "dépense"
					Assert.assertEquals(dateLimiteICCD, tache.getDateEcheance());
				}

				return null;
			}
		});

	}

	@Test
	public void testLimiteDateEcheanceParTypeContribuable() throws Exception {

		// service civil vide...
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final int currentYear = RegDate.get().year();
		final RegDate dateLimiteOrdinaire = date(currentYear + 1, 1, 31);
		final RegDate dateLimiteICCD = date(currentYear + 1, 2, 28);
		final RegDate dateReference = date(currentYear + 1, 2, 15);     // entre les deux limites ordinaire et iccd

		class Ids {
			final long ordId;
			final long iccdId;

			Ids(long ordId, long iccdId) {
				this.ordId = ordId;
				this.iccdId = iccdId;
			}
		}

		// création d'un contribuable
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PeriodeFiscale pf = addPeriodeFiscale(currentYear);
				final Set<ParametrePeriodeFiscale> params = new HashSet<>();
				params.add(new ParametrePeriodeFiscalePP(TypeContribuable.VAUDOIS_ORDINAIRE, dateLimiteOrdinaire, date(currentYear + 1, 3, 15), date(currentYear + 1, 6, 30), pf));
				params.add(new ParametrePeriodeFiscalePP(TypeContribuable.VAUDOIS_DEPENSE, dateLimiteICCD, date(currentYear + 1, 3, 15), date(currentYear + 1, 6, 30), pf));
				pf.setParametrePeriodeFiscale(params);

				// contribuable vaudois ordinaire
				final PersonnePhysique ord = addNonHabitant("Otto", "Rhino", null, Sexe.MASCULIN);
				addForPrincipal(ord, date(currentYear, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);

				// contribuable vaudois ICCD
				final PersonnePhysique iccd = addNonHabitant("Michel", "Schummi", null, Sexe.MASCULIN);
				addForPrincipal(iccd, date(currentYear, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny, ModeImposition.DEPENSE);

				return new Ids(ord.getNumero(), iccd.getNumero());
			}
		});

		// on vérifie qu'aucune tâche d'envoi de DI n'a été créée jusqu'ici (c'est pour ça qu'on a mis les arrivées HS au premier janvier de l'année courante)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
				return null;
			}
		});

		// on se place en tout début d'année suivante
		doInNewTransactionAndSession(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CollectiviteAdministrative ca = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
				final CollectiviteAdministrative caSuccessions = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACISuccessions);

				// contribuable ordinaire
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ordId);
					final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(pp, currentYear);
					Assert.assertNotNull(periodesImposition);
					Assert.assertEquals(1, periodesImposition.size());
					final PeriodeImposition periodeImposition = periodesImposition.get(0);
					Assert.assertNotNull(periodeImposition);
					assertInstanceOf(PeriodeImpositionPersonnesPhysiques.class, periodeImposition);
					Assert.assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, periodeImposition.getTypeContribuable());

					final Context ctx = new Context(pp, ca, tacheDAO, diService, caSuccessions, diDAO,  pfDAO);
					final AddDI add = new MyAddDI((PeriodeImpositionPersonnesPhysiques) periodeImposition, dateReference);
					add.execute(ctx);
				}

				// contribuable iccd
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.iccdId);
					final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(pp, currentYear);
					Assert.assertNotNull(periodesImposition);
					Assert.assertEquals(1, periodesImposition.size());
					final PeriodeImposition periodeImposition = periodesImposition.get(0);
					Assert.assertNotNull(periodeImposition);
					assertInstanceOf(PeriodeImpositionPersonnesPhysiques.class, periodeImposition);
					Assert.assertEquals(TypeContribuable.VAUDOIS_DEPENSE, periodeImposition.getTypeContribuable());

					final Context ctx = new Context(pp, ca, tacheDAO, diService, caSuccessions, diDAO,  pfDAO);
					final AddDI add = new MyAddDI((PeriodeImpositionPersonnesPhysiques) periodeImposition, dateReference);
					add.execute(ctx);
				}

				return null;
			}
		});

		// vérification des tâches créées
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// contribuable ordinaire
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ordId);
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);

					// la date attendue au prochain dimanche, car la limite est dépassée pour les ordinaires
					Assert.assertEquals(TacheEnvoiDeclarationImpot.getDefaultEcheance(dateReference), tache.getDateEcheance());
				}

				// contribuable ICCD
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.iccdId);
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);

					// la date attendue à la limite "dépense"
					Assert.assertEquals(dateLimiteICCD, tache.getDateEcheance());
				}

				return null;
			}
		});
	}
}

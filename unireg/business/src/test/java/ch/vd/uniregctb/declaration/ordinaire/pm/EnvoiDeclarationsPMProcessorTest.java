package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDeclaration;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;

public class EnvoiDeclarationsPMProcessorTest extends BusinessTest {

	private static final int TAILLE_LOT = 100;

	private EnvoiDeclarationsPMProcessor processor;
	private TacheDAO tacheDAO;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final ModeleDocumentDAO modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		final ParametreAppService parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final TicketService ticketService = getBean(TicketService.class, "ticketService");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		final PeriodeImpositionService periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");

		processor = new EnvoiDeclarationsPMProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO, delaisService, diService, assujettissementService,
		                                             periodeImpositionService, TAILLE_LOT, transactionManager, parametreAppService, adresseService, ticketService);

		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	@Test
	public void testAucuneTache() throws Exception {
		final EnvoiDIsPMResults res = processor.run(2014, TypeDeclarationImpotPM.APM, date(2014, 11, 30), null, RegDate.get(), 3, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getEnvoyees().size());
		Assert.assertEquals(0, res.getIgnorees().size());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(date(2014, 11, 30), res.getDateLimiteBouclements());
		Assert.assertEquals(RegDate.get(), res.getDateTraitement());
		Assert.assertEquals(0, res.getNbContribuablesVus());
		Assert.assertNull(res.getNbMaxEnvois());
		Assert.assertEquals(3, res.getNbThreads());
		Assert.assertEquals(2014, res.getPeriodeFiscale());
		Assert.assertEquals(TypeDeclarationImpotPM.APM, res.getType());
	}

	@Test
	public void testTacheEnInstance() throws Exception {

		final int year = RegDate.get().year();
		final int pf = year - 1;
		final RegDate dateTraitement = date(pf, 10, 5);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				// l'entreprise
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, date(pf - 1, 5, 3), null, "Ma petite entreprise");
				addFormeJuridique(e, date(pf - 1, 5, 3), null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, date(pf - 1, 5, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.YverdonLesBains);
				addAdresseSuisse(e, TypeAdresseTiers.COURRIER, date(pf - 1, 5, 3), null, MockRue.YverdonLesBains.RueDeLaFaiencerie, null);
				addBouclement(e, date(pf - 1, 7, 1), DayMonth.get(6, 30), 12);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				addPeriodeFiscale(pf);

				// la tâche de DI "pf" (= celle que l'on va traiter)
				addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, RegDate.get(), date(pf - 1, 5, 3), date(pf, 6, 30),
				                  date(pf - 1, 5, 3), date(pf, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE,
				                  TypeDocument.DECLARATION_IMPOT_PM, e, CategorieEntreprise.PM, oipm);

				return e.getNumero();
			}
		});

		// lancement du job "pf - 1" -> rien à faire
		{
			final EnvoiDIsPMResults res = processor.run(pf - 1, TypeDeclarationImpotPM.PM, date(pf - 1, 12, 31), null, dateTraitement, 1, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.getEnvoyees().size());
			Assert.assertEquals(0, res.getIgnorees().size());
			Assert.assertEquals(0, res.getErreurs().size());
			Assert.assertEquals(date(pf - 1, 12, 31), res.getDateLimiteBouclements());
			Assert.assertEquals(dateTraitement, res.getDateTraitement());
			Assert.assertEquals(0, res.getNbContribuablesVus());
			Assert.assertNull(res.getNbMaxEnvois());
			Assert.assertEquals(1, res.getNbThreads());
			Assert.assertEquals(pf - 1, res.getPeriodeFiscale());
			Assert.assertEquals(TypeDeclarationImpotPM.PM, res.getType());

			// vérification des données en base
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Entreprise e = (Entreprise) tiersDAO.get(pmId);
					Assert.assertNotNull(e);
					Assert.assertNotNull(e.getDeclarations());
					Assert.assertEquals(0, e.getDeclarations().size());

					final List<Tache> taches = tacheDAO.find(e.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());

					final TacheEnvoiDeclarationImpotPM tidipm = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(pf - 1, 5, 3), tidipm.getDateDebut());
					Assert.assertEquals(date(pf, 6, 30), tidipm.getDateFin());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tidipm.getEtat());
					Assert.assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tidipm.getTypeContribuable());
					Assert.assertEquals(TypeDocument.DECLARATION_IMPOT_PM, tidipm.getTypeDocument());
				}
			});
		}

		// lancement du job "pf" (on testera la limite de bouclement dans un autre test)
		{
			final EnvoiDIsPMResults res = processor.run(pf, TypeDeclarationImpotPM.PM, date(pf, 12, 31), null, dateTraitement, 1, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.getEnvoyees().size());
			Assert.assertEquals(0, res.getIgnorees().size());
			Assert.assertEquals(0, res.getErreurs().size());
			Assert.assertEquals(date(pf, 12, 31), res.getDateLimiteBouclements());
			Assert.assertEquals(dateTraitement, res.getDateTraitement());
			Assert.assertEquals(1, res.getNbContribuablesVus());
			Assert.assertNull(res.getNbMaxEnvois());
			Assert.assertEquals(1, res.getNbThreads());
			Assert.assertEquals(pf, res.getPeriodeFiscale());
			Assert.assertEquals(TypeDeclarationImpotPM.PM, res.getType());

			final EnvoiDIsPMResults.DiEnvoyee envoyee = res.getEnvoyees().get(0);
			Assert.assertNotNull(envoyee);
			Assert.assertEquals(pmId, envoyee.getNoCtb());
			Assert.assertEquals(date(pf - 1, 5, 3), envoyee.getDateDebut());
			Assert.assertEquals(date(pf, 6, 30), envoyee.getDateFin());

			// vérification des données en base
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Entreprise e = (Entreprise) tiersDAO.get(pmId);
					Assert.assertNotNull(e);
					Assert.assertNotNull(e.getDeclarations());
					Assert.assertEquals(1, e.getDeclarations().size());

					final Declaration declaration = e.getDeclarations().iterator().next();
					Assert.assertNotNull(declaration);
					Assert.assertFalse(declaration.isAnnule());
					Assert.assertEquals(date(pf - 1, 5, 3), declaration.getDateDebut());
					Assert.assertEquals(date(pf, 6, 30), declaration.getDateFin());
					Assert.assertEquals(dateTraitement, declaration.getDateExpedition());
					Assert.assertEquals(null, declaration.getDateRetour());
					Assert.assertNotNull(declaration.getDernierEtat());
					Assert.assertEquals(TypeEtatDeclaration.EMISE, declaration.getDernierEtat().getEtat());
					Assert.assertNotNull(declaration.getDelais());
					Assert.assertEquals(1, declaration.getDelais().size());
					final DelaiDeclaration delai = declaration.getDelais().iterator().next();
					Assert.assertNotNull(delai);
					Assert.assertFalse(delai.isAnnule());
					Assert.assertEquals(dateTraitement, delai.getDateDemande());
					Assert.assertEquals(dateTraitement, delai.getDateTraitement());
					Assert.assertEquals(date(pf, 6, 30).addMonths(6).addDays(75), delai.getDelaiAccordeAu());       // 6 mois + 75 jours de tolérance

					Assert.assertEquals(DeclarationImpotOrdinairePM.class, declaration.getClass());
					final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) declaration;
					Assert.assertNotNull(di.getCodeControle());
					Assert.assertEquals(pf, di.getPeriode().getAnnee().intValue());
					Assert.assertEquals((Integer) 1, di.getNumero());

					final List<Tache> taches = tacheDAO.find(e.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());

					final TacheEnvoiDeclarationImpotPM tidipm = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(pf - 1, 5, 3), tidipm.getDateDebut());
					Assert.assertEquals(date(pf, 6, 30), tidipm.getDateFin());
					Assert.assertEquals(TypeEtatTache.TRAITE, tidipm.getEtat());
					Assert.assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tidipm.getTypeContribuable());
					Assert.assertEquals(TypeDocument.DECLARATION_IMPOT_PM, tidipm.getTypeDocument());

					// vérification de la présence d'un événement fiscal
					final List<EvenementFiscal> all = evenementFiscalDAO.getAll();
					Assert.assertNotNull(all);
					Assert.assertEquals(1, all.size());

					final EvenementFiscal evtFiscal = all.get(0);
					Assert.assertNotNull(evtFiscal);
					Assert.assertFalse(evtFiscal.isAnnule());
					Assert.assertSame(evtFiscal.getTiers(), e);
					Assert.assertEquals(EvenementFiscalDeclaration.class, evtFiscal.getClass());

					final EvenementFiscalDeclaration evtFiscalDeclaration = (EvenementFiscalDeclaration) evtFiscal;
					Assert.assertSame(declaration, evtFiscalDeclaration.getDeclaration());
					Assert.assertEquals(EvenementFiscalDeclaration.TypeAction.EMISSION, evtFiscalDeclaration.getTypeAction());
				}
			});
		}
	}

	@Test
	public void testLimiteBouclementFinExcerciceCommercialEncoreDansLeFutur() throws Exception {

		final int year = RegDate.get().year();
		final int pf = year - 1;
		final RegDate dateTraitement = date(pf, 10, 5);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				// l'entreprise
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, date(pf - 1, 5, 3), null, "Ma petite entreprise");
				addFormeJuridique(e, date(pf - 1, 5, 3), null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, date(pf - 1, 5, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.YverdonLesBains);
				addAdresseSuisse(e, TypeAdresseTiers.COURRIER, date(pf - 1, 5, 3), null, MockRue.YverdonLesBains.RueDeLaFaiencerie, null);
				addBouclement(e, date(pf - 1, 7, 1), DayMonth.get(6, 30), 12);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				addPeriodeFiscale(pf);

				// la tâche de DI "pf" (= celle que l'on va traiter)
				addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, RegDate.get(), date(pf - 1, 5, 3), date(pf, 6, 30),
				                  date(pf - 1, 5, 3), date(pf, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE,
				                  TypeDocument.DECLARATION_IMPOT_PM, e, CategorieEntreprise.PM, oipm);

				return e.getNumero();
			}
		});

		// lancement du job "pf" avec la limite de date de bouclement avant la date effective de bouclement
		{
			final EnvoiDIsPMResults res = processor.run(pf, TypeDeclarationImpotPM.PM, date(pf, 6, 29), null, dateTraitement, 1, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.getEnvoyees().size());
			Assert.assertEquals(1, res.getIgnorees().size());
			Assert.assertEquals(0, res.getErreurs().size());
			Assert.assertEquals(date(pf, 6, 29), res.getDateLimiteBouclements());
			Assert.assertEquals(dateTraitement, res.getDateTraitement());
			Assert.assertEquals(1, res.getNbContribuablesVus());
			Assert.assertNull(res.getNbMaxEnvois());
			Assert.assertEquals(1, res.getNbThreads());
			Assert.assertEquals(pf, res.getPeriodeFiscale());
			Assert.assertEquals(TypeDeclarationImpotPM.PM, res.getType());

			final EnvoiDIsPMResults.TacheIgnoree ignoree = res.getIgnorees().get(0);
			Assert.assertNotNull(ignoree);
			Assert.assertEquals(pmId, ignoree.getNoCtb());
			Assert.assertEquals(date(pf - 1, 5, 3), ignoree.getDateDebut());
			Assert.assertEquals(date(pf, 6, 30), ignoree.getDateFin());
			Assert.assertEquals(EnvoiDIsPMResults.IgnoreType.BOUCLEMENT_TROP_RECENT, ignoree.getType());

			// vérification des données en base
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Entreprise e = (Entreprise) tiersDAO.get(pmId);
					Assert.assertNotNull(e);
					Assert.assertNotNull(e.getDeclarations());
					Assert.assertEquals(0, e.getDeclarations().size());

					final List<Tache> taches = tacheDAO.find(e.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());

					final TacheEnvoiDeclarationImpotPM tidipm = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(pf - 1, 5, 3), tidipm.getDateDebut());
					Assert.assertEquals(date(pf, 6, 30), tidipm.getDateFin());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tidipm.getEtat());
					Assert.assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tidipm.getTypeContribuable());
					Assert.assertEquals(TypeDocument.DECLARATION_IMPOT_PM, tidipm.getTypeDocument());
				}
			});
		}
	}

	@Test
	public void testLimiteBouclementFinExcerciceCommercialJustePasse() throws Exception {

		final int year = RegDate.get().year();
		final int pf = year - 1;
		final RegDate dateTraitement = date(pf, 10, 5);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				// l'entreprise
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, date(pf - 1, 5, 3), null, "Ma petite entreprise");
				addFormeJuridique(e, date(pf - 1, 5, 3), null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, date(pf - 1, 5, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.YverdonLesBains);
				addAdresseSuisse(e, TypeAdresseTiers.COURRIER, date(pf - 1, 5, 3), null, MockRue.YverdonLesBains.RueDeLaFaiencerie, null);
				addBouclement(e, date(pf - 1, 7, 1), DayMonth.get(6, 30), 12);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				addPeriodeFiscale(pf);

				// la tâche de DI "pf" (= celle que l'on va traiter)
				addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, RegDate.get(), date(pf - 1, 5, 3), date(pf, 6, 30),
				                  date(pf - 1, 5, 3), date(pf, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE,
				                  TypeDocument.DECLARATION_IMPOT_PM, e, CategorieEntreprise.PM, oipm);

				return e.getNumero();
			}
		});

		// lancement du job "pf" avec la limite de date de bouclement à la date effective de bouclement
		{
			final EnvoiDIsPMResults res = processor.run(pf, TypeDeclarationImpotPM.PM, date(pf, 6, 30), null, dateTraitement, 1, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.getEnvoyees().size());
			Assert.assertEquals(0, res.getIgnorees().size());
			Assert.assertEquals(0, res.getErreurs().size());
			Assert.assertEquals(date(pf, 6, 30), res.getDateLimiteBouclements());
			Assert.assertEquals(dateTraitement, res.getDateTraitement());
			Assert.assertEquals(1, res.getNbContribuablesVus());
			Assert.assertNull(res.getNbMaxEnvois());
			Assert.assertEquals(1, res.getNbThreads());
			Assert.assertEquals(pf, res.getPeriodeFiscale());
			Assert.assertEquals(TypeDeclarationImpotPM.PM, res.getType());

			final EnvoiDIsPMResults.DiEnvoyee envoyee = res.getEnvoyees().get(0);
			Assert.assertNotNull(envoyee);
			Assert.assertEquals(pmId, envoyee.getNoCtb());
			Assert.assertEquals(date(pf - 1, 5, 3), envoyee.getDateDebut());
			Assert.assertEquals(date(pf, 6, 30), envoyee.getDateFin());

			// vérification des données en base
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Entreprise e = (Entreprise) tiersDAO.get(pmId);
					Assert.assertNotNull(e);
					Assert.assertNotNull(e.getDeclarations());
					Assert.assertEquals(1, e.getDeclarations().size());

					final Declaration declaration = e.getDeclarations().iterator().next();
					Assert.assertNotNull(declaration);
					Assert.assertFalse(declaration.isAnnule());
					Assert.assertEquals(date(pf - 1, 5, 3), declaration.getDateDebut());
					Assert.assertEquals(date(pf, 6, 30), declaration.getDateFin());
					Assert.assertEquals(dateTraitement, declaration.getDateExpedition());
					Assert.assertEquals(null, declaration.getDateRetour());
					Assert.assertNotNull(declaration.getDernierEtat());
					Assert.assertEquals(TypeEtatDeclaration.EMISE, declaration.getDernierEtat().getEtat());
					Assert.assertNotNull(declaration.getDelais());
					Assert.assertEquals(1, declaration.getDelais().size());
					final DelaiDeclaration delai = declaration.getDelais().iterator().next();
					Assert.assertNotNull(delai);
					Assert.assertFalse(delai.isAnnule());
					Assert.assertEquals(dateTraitement, delai.getDateDemande());
					Assert.assertEquals(dateTraitement, delai.getDateTraitement());
					Assert.assertEquals(date(pf, 6, 30).addMonths(6).addDays(75), delai.getDelaiAccordeAu());        // 6 mois + 75 jours de tolérance

					Assert.assertEquals(DeclarationImpotOrdinairePM.class, declaration.getClass());
					final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) declaration;
					Assert.assertNotNull(di.getCodeControle());
					Assert.assertEquals(pf, di.getPeriode().getAnnee().intValue());
					Assert.assertEquals((Integer) 1, di.getNumero());

					final List<Tache> taches = tacheDAO.find(e.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());

					final TacheEnvoiDeclarationImpotPM tidipm = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(pf - 1, 5, 3), tidipm.getDateDebut());
					Assert.assertEquals(date(pf, 6, 30), tidipm.getDateFin());
					Assert.assertEquals(TypeEtatTache.TRAITE, tidipm.getEtat());
					Assert.assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tidipm.getTypeContribuable());
					Assert.assertEquals(TypeDocument.DECLARATION_IMPOT_PM, tidipm.getTypeDocument());
				}
			});
		}
	}

	@Test
	public void testLimiteBouclementFinAssujettissementPasse() throws Exception {

		final int year = RegDate.get().year();
		final int pf = year - 1;
		final RegDate dateTraitement = date(pf, 10, 5);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				// l'entreprise
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, date(pf - 1, 5, 3), null, "Ma petite entreprise");
				addFormeJuridique(e, date(pf - 1, 5, 3), null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, date(pf - 1, 5, 3), MotifFor.DEBUT_EXPLOITATION, date(pf, 6, 15), MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MockCommune.YverdonLesBains);
				addAdresseSuisse(e, TypeAdresseTiers.COURRIER, date(pf - 1, 5, 3), null, MockRue.YverdonLesBains.RueDeLaFaiencerie, null);
				addBouclement(e, date(pf - 1, 7, 1), DayMonth.get(6, 30), 12);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				addPeriodeFiscale(pf);

				// la tâche de DI "pf" (= celle que l'on va traiter)
				addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, RegDate.get(), date(pf - 1, 5, 3), date(pf, 6, 15),
				                  date(pf - 1, 5, 3), date(pf, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE,
				                  TypeDocument.DECLARATION_IMPOT_PM, e, CategorieEntreprise.PM, oipm);

				return e.getNumero();
			}
		});

		// lancement du job "pf" avec la limite de date de bouclement avant la date effective de bouclement sur une fin d'assujettissement
		{
			final EnvoiDIsPMResults res = processor.run(pf, TypeDeclarationImpotPM.PM, date(pf, 3, 31), null, dateTraitement, 1, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.getEnvoyees().size());
			Assert.assertEquals(0, res.getIgnorees().size());
			Assert.assertEquals(0, res.getErreurs().size());
			Assert.assertEquals(date(pf, 3, 31), res.getDateLimiteBouclements());
			Assert.assertEquals(dateTraitement, res.getDateTraitement());
			Assert.assertEquals(1, res.getNbContribuablesVus());
			Assert.assertNull(res.getNbMaxEnvois());
			Assert.assertEquals(1, res.getNbThreads());
			Assert.assertEquals(pf, res.getPeriodeFiscale());
			Assert.assertEquals(TypeDeclarationImpotPM.PM, res.getType());

			final EnvoiDIsPMResults.DiEnvoyee envoyee = res.getEnvoyees().get(0);
			Assert.assertNotNull(envoyee);
			Assert.assertEquals(pmId, envoyee.getNoCtb());
			Assert.assertEquals(date(pf - 1, 5, 3), envoyee.getDateDebut());
			Assert.assertEquals(date(pf, 6, 15), envoyee.getDateFin());

			// vérification des données en base
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Entreprise e = (Entreprise) tiersDAO.get(pmId);
					Assert.assertNotNull(e);
					Assert.assertNotNull(e.getDeclarations());
					Assert.assertEquals(1, e.getDeclarations().size());

					final Declaration declaration = e.getDeclarations().iterator().next();
					Assert.assertNotNull(declaration);
					Assert.assertFalse(declaration.isAnnule());
					Assert.assertEquals(date(pf - 1, 5, 3), declaration.getDateDebut());
					Assert.assertEquals(date(pf, 6, 15), declaration.getDateFin());
					Assert.assertEquals(dateTraitement, declaration.getDateExpedition());
					Assert.assertEquals(null, declaration.getDateRetour());
					Assert.assertNotNull(declaration.getDernierEtat());
					Assert.assertEquals(TypeEtatDeclaration.EMISE, declaration.getDernierEtat().getEtat());
					Assert.assertNotNull(declaration.getDelais());
					Assert.assertEquals(1, declaration.getDelais().size());
					final DelaiDeclaration delai = declaration.getDelais().iterator().next();
					Assert.assertNotNull(delai);
					Assert.assertFalse(delai.isAnnule());
					Assert.assertEquals(dateTraitement, delai.getDateDemande());
					Assert.assertEquals(dateTraitement, delai.getDateTraitement());
					Assert.assertEquals(date(pf, 6, 15).addMonths(6).addDays(75), delai.getDelaiAccordeAu());        // 6 mois + 75 jours de tolérance

					Assert.assertEquals(DeclarationImpotOrdinairePM.class, declaration.getClass());
					final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) declaration;
					Assert.assertNotNull(di.getCodeControle());
					Assert.assertEquals(pf, di.getPeriode().getAnnee().intValue());
					Assert.assertEquals((Integer) 1, di.getNumero());

					final List<Tache> taches = tacheDAO.find(e.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());

					final TacheEnvoiDeclarationImpotPM tidipm = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(pf - 1, 5, 3), tidipm.getDateDebut());
					Assert.assertEquals(date(pf, 6, 15), tidipm.getDateFin());
					Assert.assertEquals(TypeEtatTache.TRAITE, tidipm.getEtat());
					Assert.assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tidipm.getTypeContribuable());
					Assert.assertEquals(TypeDocument.DECLARATION_IMPOT_PM, tidipm.getTypeDocument());
				}
			});
		}
	}

	@Test
	public void testLimiteBouclementFinAssujettissementFutur() throws Exception {

		final int year = RegDate.get().year();
		final int pf = year - 1;
		final RegDate dateTraitement = date(pf, 4, 5);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				// l'entreprise
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, date(pf - 1, 5, 3), null, "Ma petite entreprise");
				addFormeJuridique(e, date(pf - 1, 5, 3), null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, RegDate.get(pf - 1, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, date(pf - 1, 5, 3), MotifFor.DEBUT_EXPLOITATION, date(pf, 6, 15), MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MockCommune.YverdonLesBains);
				addAdresseSuisse(e, TypeAdresseTiers.COURRIER, date(pf - 1, 5, 3), null, MockRue.YverdonLesBains.RueDeLaFaiencerie, null);
				addBouclement(e, date(pf - 1, 7, 1), DayMonth.get(6, 30), 12);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				addPeriodeFiscale(pf);

				// la tâche de DI "pf" (= celle que l'on va traiter)
				addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, RegDate.get(), date(pf - 1, 5, 3), date(pf, 6, 15),
				                  date(pf - 1, 5, 3), date(pf, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE,
				                  TypeDocument.DECLARATION_IMPOT_PM, e, CategorieEntreprise.PM, oipm);

				return e.getNumero();
			}
		});

		// lancement du job "pf" avec la limite de date de bouclement avant la date effective de bouclement sur une fin d'assujettissement
		// avec une date de traitement antérieure à la date de bouclement
		{
			final EnvoiDIsPMResults res = processor.run(pf, TypeDeclarationImpotPM.PM, date(pf, 3, 31), null, dateTraitement, 1, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.getEnvoyees().size());
			Assert.assertEquals(1, res.getIgnorees().size());
			Assert.assertEquals(0, res.getErreurs().size());
			Assert.assertEquals(date(pf, 3, 31), res.getDateLimiteBouclements());
			Assert.assertEquals(dateTraitement, res.getDateTraitement());
			Assert.assertEquals(1, res.getNbContribuablesVus());
			Assert.assertNull(res.getNbMaxEnvois());
			Assert.assertEquals(1, res.getNbThreads());
			Assert.assertEquals(pf, res.getPeriodeFiscale());
			Assert.assertEquals(TypeDeclarationImpotPM.PM, res.getType());

			final EnvoiDIsPMResults.TacheIgnoree ignoree = res.getIgnorees().get(0);
			Assert.assertNotNull(ignoree);
			Assert.assertEquals(pmId, ignoree.getNoCtb());
			Assert.assertEquals(date(pf - 1, 5, 3), ignoree.getDateDebut());
			Assert.assertEquals(date(pf, 6, 15), ignoree.getDateFin());
			Assert.assertEquals(EnvoiDIsPMResults.IgnoreType.BOUCLEMENT_FUTUR, ignoree.getType());


			// vérification des données en base
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Entreprise e = (Entreprise) tiersDAO.get(pmId);
					Assert.assertNotNull(e);
					Assert.assertNotNull(e.getDeclarations());
					Assert.assertEquals(0, e.getDeclarations().size());

					final List<Tache> taches = tacheDAO.find(e.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());

					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());

					final TacheEnvoiDeclarationImpotPM tidipm = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(pf - 1, 5, 3), tidipm.getDateDebut());
					Assert.assertEquals(date(pf, 6, 15), tidipm.getDateFin());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tidipm.getEtat());
					Assert.assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tidipm.getTypeContribuable());
					Assert.assertEquals(TypeDocument.DECLARATION_IMPOT_PM, tidipm.getTypeDocument());
				}
			});
		}
	}
}

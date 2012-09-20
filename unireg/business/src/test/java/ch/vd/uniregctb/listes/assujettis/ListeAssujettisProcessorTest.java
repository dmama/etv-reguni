package ch.vd.uniregctb.listes.assujettis;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

public class ListeAssujettisProcessorTest extends BusinessTest {

	private ListeAssujettisProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		processor = new ListeAssujettisProcessor(hibernateTemplate, tiersService, serviceCivilCacheWarmer, transactionManager, tiersDAO, assujettissementService);
	}

	@Test
	public void testVaudoisOrdinaire() throws Exception {

		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.INDETERMINE, MockCommune.Aigle);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.getNbCtbAssujettis());
		Assert.assertEquals(1, results.getNbAssujettissements());
		Assert.assertEquals(0, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbAssujetti a = results.getAssujettis().get(0);
		Assert.assertNotNull(a);
		Assert.assertEquals(ppId, a.noCtb);
		Assert.assertEquals(date(2010, 1, 1), a.debutAssujettissement);
		Assert.assertEquals(date(2010, 12, 31), a.finAssujettissement);
		Assert.assertEquals("Imposition ordinaire VD", a.typeAssujettissement);
		Assert.assertNull(a.motifDebut);
		Assert.assertNull(a.motifFin);
	}

	@Test
	public void testVaudoisOrdinaireMariage() throws Exception {

		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.INDETERMINE, date(2010, 3, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(0, results.getNbCtbAssujettis());
		Assert.assertEquals(0, results.getNbAssujettissements());
		Assert.assertEquals(1, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbIgnore a = results.getIgnores().get(0);
		Assert.assertNotNull(a);
		Assert.assertEquals(ppId, a.noCtb);
		Assert.assertEquals(ListeAssujettisResults.CauseIgnorance.NON_ASSUJETTI, a.cause);
	}

	@Test
	public void testCoupleVaudoisOrdinaireMariage() throws Exception {

		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final class Ids {
			long ppId;
			long mcId;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, date(2010, 3, 11), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(2010, 3, 12), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2010, 3, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.mcId = mc.getNumero();
				return ids;
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.getNbCtbAssujettis());
		Assert.assertEquals(1, results.getNbAssujettissements());
		Assert.assertEquals(1, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbIgnore i = results.getIgnores().get(0);
		Assert.assertNotNull(i);
		Assert.assertEquals(ids.ppId, i.noCtb);
		Assert.assertEquals(ListeAssujettisResults.CauseIgnorance.NON_ASSUJETTI, i.cause);

		final ListeAssujettisResults.InfoCtbAssujetti a = results.getAssujettis().get(0);
		Assert.assertNotNull(a);
		Assert.assertEquals(ids.mcId, a.noCtb);
		Assert.assertEquals(date(2010, 1, 1), a.debutAssujettissement);
		Assert.assertEquals(date(2010, 12, 31), a.finAssujettissement);
		Assert.assertEquals("Imposition ordinaire VD", a.typeAssujettissement);
		Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, a.motifDebut);
		Assert.assertNull(a.motifFin);
	}

	@Test
	public void testHorsCanton() throws Exception {

		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.getNbCtbAssujettis());
		Assert.assertEquals(1, results.getNbAssujettissements());
		Assert.assertEquals(0, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbAssujetti a = results.getAssujettis().get(0);
		Assert.assertNotNull(a);
		Assert.assertEquals(ppId, a.noCtb);
		Assert.assertEquals(date(2010, 1, 1), a.debutAssujettissement);
		Assert.assertEquals(date(2010, 12, 31), a.finAssujettissement);
		Assert.assertEquals("Imposition ordinaire HC", a.typeAssujettissement);
		Assert.assertNull(a.motifDebut);
		Assert.assertNull(a.motifFin);
	}

	@Test
	public void testHorsSuisse() throws Exception {

		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.Colombie);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.getNbCtbAssujettis());
		Assert.assertEquals(1, results.getNbAssujettissements());
		Assert.assertEquals(0, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbAssujetti a = results.getAssujettis().get(0);
		Assert.assertNotNull(a);
		Assert.assertEquals(ppId, a.noCtb);
		Assert.assertEquals(date(2010, 1, 1), a.debutAssujettissement);
		Assert.assertEquals(date(2010, 12, 31), a.finAssujettissement);
		Assert.assertEquals("Imposition ordinaire HS", a.typeAssujettissement);
		Assert.assertNull(a.motifDebut);
		Assert.assertNull(a.motifFin);
	}

	@Test
	public void testHorsSuisseQuiDebarque() throws Exception {

		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2010, 3, 11), MotifFor.ARRIVEE_HS, MockPays.Colombie);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.getNbCtbAssujettis());
		Assert.assertEquals(2, results.getNbAssujettissements());
		Assert.assertEquals(0, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbAssujetti hs = results.getAssujettis().get(0);
		Assert.assertNotNull(hs);
		Assert.assertEquals(ppId, hs.noCtb);
		Assert.assertEquals(date(2010, 1, 1), hs.debutAssujettissement);
		Assert.assertEquals(date(2010, 3, 11), hs.finAssujettissement);
		Assert.assertEquals("Imposition ordinaire HS", hs.typeAssujettissement);
		Assert.assertNull(hs.motifDebut);
		Assert.assertEquals(MotifFor.ARRIVEE_HS, hs.motifFin);

		final ListeAssujettisResults.InfoCtbAssujetti vd = results.getAssujettis().get(1);
		Assert.assertNotNull(vd);
		Assert.assertEquals(ppId, vd.noCtb);
		Assert.assertEquals(date(2010, 3, 12), vd.debutAssujettissement);
		Assert.assertEquals(date(2010, 12, 31), vd.finAssujettissement);
		Assert.assertEquals("Imposition ordinaire VD", vd.typeAssujettissement);
		Assert.assertEquals(MotifFor.ARRIVEE_HS, vd.motifDebut);
		Assert.assertNull(vd.motifFin);
	}

	@Test
	public void testSourcierInclus() throws Exception {
		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2005, 1, 1), MotifFor.INDETERMINE, MockCommune.Aigle);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.getNbCtbAssujettis());
		Assert.assertEquals(1, results.getNbAssujettissements());
		Assert.assertEquals(0, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbAssujetti a = results.getAssujettis().get(0);
		Assert.assertNotNull(a);
		Assert.assertEquals(ppId, a.noCtb);
		Assert.assertEquals(date(2010, 1, 1), a.debutAssujettissement);
		Assert.assertEquals(date(2010, 12, 31), a.finAssujettissement);
		Assert.assertEquals("Imposition à la source", a.typeAssujettissement);
		Assert.assertNull(a.motifDebut);
		Assert.assertNull(a.motifFin);
	}

	@Test
	public void testSourcierExclu() throws Exception {
		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2005, 1, 1), MotifFor.INDETERMINE, MockCommune.Aigle);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, false, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(0, results.getNbCtbAssujettis());
		Assert.assertEquals(0, results.getNbAssujettissements());
		Assert.assertEquals(1, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbIgnore i = results.getIgnores().get(0);
		Assert.assertNotNull(i);
		Assert.assertEquals(ppId, i.noCtb);
		Assert.assertEquals(ListeAssujettisResults.CauseIgnorance.SOURCIER_PUR, i.cause);
	}

	@Test
	public void testDepartHorsSuissePasSeulementAssujettisFinAnnee() throws Exception {
		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.INDETERMINE, date(2010, 5, 12), MotifFor.DEPART_HS, MockCommune.Aigle);
				addForPrincipal(pp, date(2010, 5, 13), MotifFor.DEPART_HS, MockPays.Colombie);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.getNbCtbAssujettis());
		Assert.assertEquals(1, results.getNbAssujettissements());
		Assert.assertEquals(0, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbAssujetti a = results.getAssujettis().get(0);
		Assert.assertNotNull(a);
		Assert.assertEquals(ppId, a.noCtb);
		Assert.assertEquals(date(2010, 1, 1), a.debutAssujettissement);
		Assert.assertEquals(date(2010, 5, 12), a.finAssujettissement);
		Assert.assertEquals("Imposition ordinaire VD", a.typeAssujettissement);
		Assert.assertNull(a.motifDebut);
		Assert.assertEquals(MotifFor.DEPART_HS, a.motifFin);
	}

	@Test
	public void testDepartHorsSuisseSeulementAssujettisFinAnnee() throws Exception {
		final long noIndividu = 1235435L;

		// service civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1964, 5, 30), "Parker", "Camilla", false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.INDETERMINE, date(2010, 5, 12), MotifFor.DEPART_HS, MockCommune.Aigle);
				addForPrincipal(pp, date(2010, 5, 13), MotifFor.DEPART_HS, MockPays.Colombie);
				return pp.getNumero();
			}
		});

		final ListeAssujettisResults results = processor.run(RegDate.get(), 1, 2010, true, true, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(0, results.getNbCtbAssujettis());
		Assert.assertEquals(0, results.getNbAssujettissements());
		Assert.assertEquals(1, results.getNbCtbIgnores());
		Assert.assertEquals(0, results.getListeErreurs().size());

		final ListeAssujettisResults.InfoCtbIgnore i = results.getIgnores().get(0);
		Assert.assertNotNull(i);
		Assert.assertEquals(ppId, i.noCtb);
		Assert.assertEquals(ListeAssujettisResults.CauseIgnorance.NON_ASSUJETTI_FIN_PERIODE, i.cause);
	}
}

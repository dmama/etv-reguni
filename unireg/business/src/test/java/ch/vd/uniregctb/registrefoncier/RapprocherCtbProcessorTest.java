package ch.vd.uniregctb.registrefoncier;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

public class RapprocherCtbProcessorTest extends BusinessTest {

	private RapprocherCtbProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		processor = new RapprocherCtbProcessor(hibernateTemplate, transactionManager, tiersDAO, adresseService, tiersService, serviceCivil);
	}

	@Test
	public void testSansNumeroContribuable() throws Exception {
		final long noRf = 1234L;
		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto", date(1975, 3, 15), null)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto", p.getPrenom());
		Assert.assertEquals(date(1975, 3, 15), p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.CTB_NON_TROUVE, p.getResultat());
		Assert.assertNull(p.getNumeroContribuable());
		Assert.assertNull(p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertNull(p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertNull(p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertNull(p.getNomCourrier1());
		Assert.assertNull(p.getNomCourrier2());
		Assert.assertNull(p.getFormulePolitesse());
		Assert.assertNull(p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
	}

	@Test
	public void testAvecNumeroContribuableInconnu() throws Exception {
		final long noRf = 1234L;
		final Long noCtbDonne = 99999998L;
		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto", date(1975, 3, 15), noCtbDonne)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto", p.getPrenom());
		Assert.assertEquals(date(1975, 3, 15), p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.CTB_NON_TROUVE, p.getResultat());
		Assert.assertEquals(noCtbDonne, p.getNumeroContribuable());
		Assert.assertNull(p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertNull(p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertNull(p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertNull(p.getNomCourrier1());
		Assert.assertNull(p.getNomCourrier2());
		Assert.assertNull(p.getFormulePolitesse());
		Assert.assertNull(p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
	}

	@Test
	public void testAvecMatchParfait() throws Exception {
		final long noRf = 1234L;
		final long noIndividu = 425673452738L;
		final RegDate dateNaissance = date(1976, 8, 12);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Tartempion", "Toto", true);
			}
		});

		final Long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});
		Assert.assertNotNull(ppId);

		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto", dateNaissance, ppId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto", p.getPrenom());
		Assert.assertEquals(dateNaissance, p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT, p.getResultat());
		Assert.assertEquals(ppId, p.getNumeroContribuable());
		Assert.assertEquals(ppId, p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertEquals(dateNaissance, p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertEquals("Tartempion", p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertEquals("Toto", p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertNull(p.getNomCourrier2());
		Assert.assertEquals("Monsieur", p.getFormulePolitesse());
	}

	@Test
	public void testAvecMatchParfaitSaufDateNaissance() throws Exception {
		final long noRf = 1234L;
		final long noIndividu = 425673452738L;
		final RegDate dateNaissance = date(1976, 8, 12);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Tartempion", "Toto", true);
			}
		});

		final Long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});
		Assert.assertNotNull(ppId);

		final RegDate dateNaissanceRf = dateNaissance.addMonths(7);
		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto", dateNaissanceRf, ppId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto", p.getPrenom());
		Assert.assertEquals(dateNaissanceRf, p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT_SAUF_NAISSANCE, p.getResultat());
		Assert.assertEquals(ppId, p.getNumeroContribuable());
		Assert.assertEquals(ppId, p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertEquals(dateNaissance, p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertEquals("Tartempion", p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertEquals("Toto", p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertNull(p.getNomCourrier2());
		Assert.assertEquals("Monsieur", p.getFormulePolitesse());
	}

	@Test
	public void testAvecMatchParfaitSansDateNaissanceFournie() throws Exception {
		final long noRf = 1234L;
		final long noIndividu = 425673452738L;
		final RegDate dateNaissance = date(1976, 8, 12);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Tartempion", "Toto", true);
			}
		});

		final Long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});
		Assert.assertNotNull(ppId);

		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto", null, ppId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto", p.getPrenom());
		Assert.assertNull(p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT_SAUF_NAISSANCE, p.getResultat());
		Assert.assertEquals(ppId, p.getNumeroContribuable());
		Assert.assertEquals(ppId, p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertEquals(dateNaissance, p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertEquals("Tartempion", p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertEquals("Toto", p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertNull(p.getNomCourrier2());
		Assert.assertEquals("Monsieur", p.getFormulePolitesse());
	}

	@Test
	public void testAvecMatchParfaitSurInconnuCdH() throws Exception {
		final long noRf = 1234L;
		final RegDate dateNaissance = date(1976, 8, 12);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final Long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", dateNaissance, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});
		Assert.assertNotNull(ppId);

		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto", dateNaissance, ppId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto", p.getPrenom());
		Assert.assertEquals(dateNaissance, p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDU_NON_TROUVE, p.getResultat());
		Assert.assertEquals(ppId, p.getNumeroContribuable());
		Assert.assertNull(p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertNull(p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertNull(p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertNull(p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertNull(p.getNomCourrier2());
		Assert.assertEquals("Monsieur", p.getFormulePolitesse());
	}

	@Test
	public void testAvecMatchParfaitSurNonHabitantConnuAuCivil() throws Exception {
		final long noRf = 1234L;
		final long noIndividu = 425673452738L;
		final RegDate dateNaissance = date(1976, 8, 12);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Tartempion", "Toto", true);
			}
		});

		final Long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				tiersService.changeHabitantenNH(pp);
				return pp.getNumero();
			}
		});
		Assert.assertNotNull(ppId);

		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto", dateNaissance, ppId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto", p.getPrenom());
		Assert.assertEquals(dateNaissance, p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT, p.getResultat());
		Assert.assertEquals(ppId, p.getNumeroContribuable());
		Assert.assertEquals(ppId, p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertEquals(dateNaissance, p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertEquals("Tartempion", p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertEquals("Toto", p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertNull(p.getNomCourrier2());
		Assert.assertEquals("Monsieur", p.getFormulePolitesse());
	}

	@Test
	public void testAvecMatchInexact() throws Exception {
		final long noRf = 1234L;
		final long noIndividu = 425673452738L;
		final RegDate dateNaissance = date(1976, 8, 12);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Tartempion", "Toto", true);
			}
		});

		final Long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				tiersService.changeHabitantenNH(pp);
				return pp.getNumero();
			}
		});
		Assert.assertNotNull(ppId);

		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto Henri", dateNaissance, ppId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto Henri", p.getPrenom());
		Assert.assertEquals(dateNaissance, p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_NON_EXACT, p.getResultat());
		Assert.assertEquals(ppId, p.getNumeroContribuable());
		Assert.assertEquals(ppId, p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertEquals(dateNaissance, p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertEquals("Tartempion", p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertEquals("Toto", p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertNull(p.getNomCourrier2());
		Assert.assertEquals("Monsieur", p.getFormulePolitesse());
	}

	@Test
	public void testAvecMatchSurContribuablePrincipalDeMenageFourni() throws Exception {
		final long noRf = 1234L;
		final long noIndividuM = 425673452738L;
		final long noIndividuMMe = 4236784L;
		final RegDate dateNaissanceM = date(1976, 8, 12);
		final RegDate dateNaissanceMMe = date(1976, 2, 15);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividuM, dateNaissanceM, "Tartempion", "Toto", true);
				final MockIndividu mme = addIndividu(noIndividuMMe, dateNaissanceMMe, "Tartempion", "Tata", false);
				marieIndividus(m, mme, date(2000, 1, 1));
			}
		});

		final class Ids {
			final Long mId;
			final Long mmeId;
			final Long mcId;

			Ids(long mId, long mmeId, long mcId) {
				this.mId = mId;
				this.mmeId = mmeId;
				this.mcId = mcId;
			}
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividuM);
				final PersonnePhysique mme = addHabitant(noIndividuMMe);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, date(2000, 1, 1), null);
				final MenageCommun mc = couple.getMenage();
				return new Ids(m.getNumero(), mme.getNumero(), mc.getNumero());
			}
		});
		Assert.assertNotNull(ids);

		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Toto", dateNaissanceM, ids.mcId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Toto", p.getPrenom());
		Assert.assertEquals(dateNaissanceM, p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT, p.getResultat());
		Assert.assertEquals(ids.mcId, p.getNumeroContribuable());
		Assert.assertEquals(ids.mId, p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertEquals(dateNaissanceM, p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertEquals("Tartempion", p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertEquals("Toto", p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertEquals("Tata Tartempion", p.getNomCourrier2());
		Assert.assertEquals("Monsieur et Madame", p.getFormulePolitesse());
	}

	@Test
	public void testAvecMatchSurContribuableConjointDeMenageFourni() throws Exception {
		final long noRf = 1234L;
		final long noIndividuM = 425673452738L;
		final long noIndividuMme = 4236784L;
		final RegDate dateNaissanceM = date(1976, 8, 12);
		final RegDate dateNaissanceMme = date(1976, 2, 15);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividuM, dateNaissanceM, "Tartempion", "Toto", true);
				final MockIndividu mme = addIndividu(noIndividuMme, dateNaissanceMme, "Tartempion", "Tata", false);
				marieIndividus(m, mme, date(2000, 1, 1));
			}
		});

		final class Ids {
			final Long mId;
			final Long mmeId;
			final Long mcId;

			Ids(long mId, long mmeId, long mcId) {
				this.mId = mId;
				this.mmeId = mmeId;
				this.mcId = mcId;
			}
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividuM);
				final PersonnePhysique mme = addHabitant(noIndividuMme);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, date(2000, 1, 1), null);
				final MenageCommun mc = couple.getMenage();
				return new Ids(m.getNumero(), mme.getNumero(), mc.getNumero());
			}
		});
		Assert.assertNotNull(ids);

		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Tata", dateNaissanceMme, ids.mcId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Tata", p.getPrenom());
		Assert.assertEquals(dateNaissanceMme, p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT, p.getResultat());
		Assert.assertEquals(ids.mcId, p.getNumeroContribuable());
		Assert.assertEquals(ids.mmeId, p.getNumeroContribuable1());
		Assert.assertNull(p.getNumeroContribuable2());
		Assert.assertEquals(dateNaissanceMme, p.getDateNaissance1());
		Assert.assertNull(p.getDateNaissance2());
		Assert.assertEquals("Tartempion", p.getNom1());
		Assert.assertNull(p.getNom2());
		Assert.assertEquals("Tata", p.getPrenom1());
		Assert.assertNull(p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertEquals("Tata Tartempion", p.getNomCourrier2());
		Assert.assertEquals("Monsieur et Madame", p.getFormulePolitesse());
	}

	@Test
	public void testAvecMatchSurAucunePersonnePhysiqueDeMenageFourni() throws Exception {
		final long noRf = 1234L;
		final long noIndividuM = 425673452738L;
		final long noIndividuMme = 4236784L;
		final RegDate dateNaissanceM = date(1976, 8, 12);
		final RegDate dateNaissanceMme = date(1976, 2, 15);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividuM, dateNaissanceM, "Tartempion", "Toto", true);
				final MockIndividu mme = addIndividu(noIndividuMme, dateNaissanceMme, "Tartempion", "Tata", false);
				marieIndividus(m, mme, date(2000, 1, 1));
			}
		});

		final class Ids {
			final Long mId;
			final Long mmeId;
			final Long mcId;

			Ids(long mId, long mmeId, long mcId) {
				this.mId = mId;
				this.mmeId = mmeId;
				this.mcId = mcId;
			}
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividuM);
				final PersonnePhysique mme = addHabitant(noIndividuMme);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, date(2000, 1, 1), null);
				final MenageCommun mc = couple.getMenage();
				return new Ids(m.getNumero(), mme.getNumero(), mc.getNumero());
			}
		});
		Assert.assertNotNull(ids);

		final RapprocherCtbResults res = processor.run(Arrays.asList(new ProprietaireFoncier(noRf, "Tartempion", "Tutu", dateNaissanceMme, ids.mcId)), null, RegDate.get(), 1);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.nbCtbsTotal);
		Assert.assertNotNull(res.ctbsEnErreur);
		Assert.assertEquals(0, res.ctbsEnErreur.size());
		Assert.assertNotNull(res.listeRapproche);
		Assert.assertEquals(1, res.listeRapproche.size());

		final ProprietaireRapproche p = res.listeRapproche.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(noRf, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Tutu", p.getPrenom());
		Assert.assertEquals(dateNaissanceMme, p.getDateNaissance());
		Assert.assertEquals(ProprietaireRapproche.CodeRetour.INDIVIDUS_TROUVE_NON_EXACT, p.getResultat());
		Assert.assertEquals(ids.mcId, p.getNumeroContribuable());
		Assert.assertEquals(ids.mId, p.getNumeroContribuable1());
		Assert.assertEquals(ids.mmeId, p.getNumeroContribuable2());
		Assert.assertEquals(dateNaissanceM, p.getDateNaissance1());
		Assert.assertEquals(dateNaissanceMme, p.getDateNaissance2());
		Assert.assertEquals("Tartempion", p.getNom1());
		Assert.assertEquals("Tartempion", p.getNom2());
		Assert.assertEquals("Toto", p.getPrenom1());
		Assert.assertEquals("Tata", p.getPrenom2());
		Assert.assertEquals("Toto Tartempion", p.getNomCourrier1());
		Assert.assertEquals("Tata Tartempion", p.getNomCourrier2());
		Assert.assertEquals("Monsieur et Madame", p.getFormulePolitesse());
	}
}

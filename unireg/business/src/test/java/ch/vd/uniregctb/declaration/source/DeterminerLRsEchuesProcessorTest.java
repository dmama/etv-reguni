package ch.vd.uniregctb.declaration.source;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class DeterminerLRsEchuesProcessorTest extends BusinessTest {

	private DeterminerLRsEchuesProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final ListeRecapService lrService = getBean(ListeRecapService.class, "lrService");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final ListeRecapitulativeDAO lrDAO = getBean(ListeRecapitulativeDAO.class, "lrDAO");
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");

		processor = new DeterminerLRsEchuesProcessor(transactionManager, hibernateTemplate, lrService, delaisService, tiersDAO, lrDAO, evenementFiscalService, tiersService, adresseService);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurAvecLrNonSommees() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(0, results.lrEchues.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurRegulierAvecLrSommeeMaisUneLrNonEmise() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4), date(2009, 12, 4)));
				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(1, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(0, results.lrEchues.size());

		Assert.assertEquals(DeterminerLRsEchuesResults.Raison.ENCORE_LR_A_EMETTRE_SUR_PF, results.ignores.get(0).getRaison());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurNonRegulierAvecLrSommeeMaisUneLrNonEmise() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.ADMINISTRATEURS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1),PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4)));
				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(1, results.lrEchues.size());

		final DeterminerLRsEchuesResults.ResultLrEchue lr = results.lrEchues.get(0);
		Assert.assertEquals(date(2009, 1, 1), lr.debutPeriode);
		Assert.assertEquals(date(2009, 3, 31), lr.finPeriode);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurRegulierAvecLrSommeeToutesEmises() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4)));
				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(1, results.lrEchues.size());

		final DeterminerLRsEchuesResults.ResultLrEchue lr = results.lrEchues.get(0);
		Assert.assertEquals(date(2009, 1, 1), lr.debutPeriode);
		Assert.assertEquals(date(2009, 3, 31), lr.finPeriode);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurNonRegulierAvecLrSommeeToutesEmises() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.ADMINISTRATEURS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4)));
				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(1, results.lrEchues.size());

		final DeterminerLRsEchuesResults.ResultLrEchue lr = results.lrEchues.get(0);
		Assert.assertEquals(date(2009, 1, 1), lr.debutPeriode);
		Assert.assertEquals(date(2009, 3, 31), lr.finPeriode);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurAvecLrSommeeTresRecemment() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(RegDate.get().addDays(-10),RegDate.get().addDays(-10)));
				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(0, results.lrEchues.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurAvecLrSommeeEtDejaEchue() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4), date(2009, 12, 4)));
				lr.addEtat(new EtatDeclarationEchue(date(2010, 1, 20)));
				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(0, results.lrEchues.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurAvecUneLrSommeeEtUneDejaEchue() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);

				final DeclarationImpotSource lr1 = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr1.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4)));
				lr1.addEtat(new EtatDeclarationEchue(date(2010, 1, 20)));

				final DeclarationImpotSource lr2 = addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr2.addEtat(new EtatDeclarationSommee(date(2009, 12, 4), date(2009, 12, 4)));

				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(1, results.lrEchues.size());

		final DeterminerLRsEchuesResults.ResultLrEchue lr = results.lrEchues.get(0);
		Assert.assertEquals(date(2009, 4, 1), lr.debutPeriode);
		Assert.assertEquals(date(2009, 6, 30), lr.finPeriode);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurAvecUneLrRetourneeApresSommation() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);

				final DeclarationImpotSource lr1 = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr1.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4)));
				lr1.addEtat(new EtatDeclarationRetournee(date(2010, 1, 20), "TEST"));

				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(0, results.lrEchues.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurAvecPlusieursLrSommees() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);

				final DeclarationImpotSource lr1 = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr1.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4)));

				final DeclarationImpotSource lr2 = addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr2.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4)));

				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(2009, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(2, results.lrEchues.size());

		final DeterminerLRsEchuesResults.ResultLrEchue lr1 = results.lrEchues.get(0);
		Assert.assertEquals(date(2009, 1, 1), lr1.debutPeriode);
		Assert.assertEquals(date(2009, 3, 31), lr1.finPeriode);

		final DeterminerLRsEchuesResults.ResultLrEchue lr2 = results.lrEchues.get(1);
		Assert.assertEquals(date(2009, 4, 1), lr2.debutPeriode);
		Assert.assertEquals(date(2009, 6, 30), lr2.finPeriode);
	}

}

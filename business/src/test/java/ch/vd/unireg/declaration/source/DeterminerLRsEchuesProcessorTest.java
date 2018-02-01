package ch.vd.unireg.declaration.source;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.EtatDeclarationEchue;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.ListeRecapitulativeDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodiciteDecompte;

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
	public void testDebiteurRegulierAvecLrSommeeMaisUneLrNonEmise() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4), date(2009, 12, 4), null));
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
	public void testDebiteurNonRegulierAvecLrSommeeMaisUneLrNonEmise() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.ADMINISTRATEURS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1),PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));
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
	public void testDebiteurRegulierAvecLrSommeeToutesEmises() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));
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
	public void testDebiteurNonRegulierAvecLrSommeeToutesEmises() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.ADMINISTRATEURS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));
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
	public void testDebiteurAvecLrSommeeTresRecemment() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(RegDate.get().addDays(-10),RegDate.get().addDays(-10), null));
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
	public void testDebiteurAvecLrSommeeEtDejaEchue() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationSommee(date(2009, 12, 4), date(2009, 12, 4), null));
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
	public void testDebiteurAvecUneLrSommeeEtUneDejaEchue() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);

				final DeclarationImpotSource lr1 = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr1.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));
				lr1.addEtat(new EtatDeclarationEchue(date(2010, 1, 20)));

				final DeclarationImpotSource lr2 = addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr2.addEtat(new EtatDeclarationSommee(date(2009, 12, 4), date(2009, 12, 4), null));

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
	public void testDebiteurAvecUneLrRetourneeApresSommation() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);

				final DeclarationImpotSource lr1 = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr1.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));
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
	public void testDebiteurAvecPlusieursLrSommees() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);

				final DeclarationImpotSource lr1 = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr1.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));

				final DeclarationImpotSource lr2 = addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr2.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));

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

	@Test
	public void testPlusieursPfConcerneesMaisUneSeuleDemandee() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				// LR 2009
				final PeriodeFiscale pf2009 = addPeriodeFiscale(2009);
				{
					final DeclarationImpotSource lr1 = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2009);
					lr1.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));

					final DeclarationImpotSource lr2 = addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2009);
					lr2.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));

					addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2009);
					addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2009);
				}

				// LR 2010
				final PeriodeFiscale pf2010 = addPeriodeFiscale(2010);
				{
					final DeclarationImpotSource lr1 = addLR(dpi, date(2010, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2010);
					lr1.addEtat(new EtatDeclarationSommee(date(2010, 12, 4),date(2010, 12, 4), null));

					final DeclarationImpotSource lr2 = addLR(dpi, date(2010, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2010);
					lr2.addEtat(new EtatDeclarationSommee(date(2010, 12, 4),date(2010, 12, 4), null));

					addLR(dpi, date(2010, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2010);
					addLR(dpi, date(2010, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2010);
				}
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

	@Test
	public void testPlusieursPfConcerneesEtDemandees() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				// LR 2009
				final PeriodeFiscale pf2009 = addPeriodeFiscale(2009);
				{
					final DeclarationImpotSource lr1 = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2009);
					lr1.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));

					final DeclarationImpotSource lr2 = addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2009);
					lr2.addEtat(new EtatDeclarationSommee(date(2009, 12, 4),date(2009, 12, 4), null));

					addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2009);
					addLR(dpi, date(2009, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2009);
				}

				// LR 2010
				final PeriodeFiscale pf2010 = addPeriodeFiscale(2010);
				{
					final DeclarationImpotSource lr1 = addLR(dpi, date(2010, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2010);
					lr1.addEtat(new EtatDeclarationSommee(date(2010, 12, 4),date(2010, 12, 4), null));

					final DeclarationImpotSource lr2 = addLR(dpi, date(2010, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2010);
					lr2.addEtat(new EtatDeclarationSommee(date(2010, 12, 4),date(2010, 12, 4), null));

					addLR(dpi, date(2010, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2010);
					addLR(dpi, date(2010, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, pf2010);
				}
				return null;
			}
		});

		final DeterminerLRsEchuesResults results = processor.run(null, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(0, results.ignores.size());
		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size());
		Assert.assertNotNull(results.lrEchues);
		Assert.assertEquals(4, results.lrEchues.size());

		{
			final DeterminerLRsEchuesResults.ResultLrEchue lr = results.lrEchues.get(0);
			Assert.assertEquals(date(2009, 1, 1), lr.debutPeriode);
			Assert.assertEquals(date(2009, 3, 31), lr.finPeriode);
		}
		{
			final DeterminerLRsEchuesResults.ResultLrEchue lr = results.lrEchues.get(1);
			Assert.assertEquals(date(2009, 4, 1), lr.debutPeriode);
			Assert.assertEquals(date(2009, 6, 30), lr.finPeriode);
		}
		{
			final DeterminerLRsEchuesResults.ResultLrEchue lr = results.lrEchues.get(2);
			Assert.assertEquals(date(2010, 1, 1), lr.debutPeriode);
			Assert.assertEquals(date(2010, 3, 31), lr.finPeriode);
		}
		{
			final DeterminerLRsEchuesResults.ResultLrEchue lr = results.lrEchues.get(3);
			Assert.assertEquals(date(2010, 4, 1), lr.debutPeriode);
			Assert.assertEquals(date(2010, 6, 30), lr.finPeriode);
		}
	}
}

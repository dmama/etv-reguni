package ch.vd.unireg.declaration.source;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeDocument;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class EnvoiLRsEnMasseProcessorTest extends BusinessTest {

	private ListeRecapService lrService;
	private AdresseService adresseService;
	private TicketService ticketService;

	private static class ListRecapServiceWrapper implements ListeRecapService {
		private final ListeRecapService target;

		private ListRecapServiceWrapper(ListeRecapService target) {
			this.target = target;
		}

		@Override
		public EditiqueResultat getCopieConformeSommationLR(DeclarationImpotSource lr) throws EditiqueException {
			return target.getCopieConformeSommationLR(lr);
		}

		@Override
		public void imprimerSommationLR(DeclarationImpotSource lr, RegDate dateTraitement) throws Exception {
			target.imprimerSommationLR(lr, dateTraitement);
		}

		@Override
		public EnvoiLRsResults imprimerAllLR(RegDate dateFinPeriode, StatusManager status) throws Exception {
			return target.imprimerAllLR(dateFinPeriode, status);
		}

		@Override
		public EnvoiSommationLRsResults sommerAllLR(CategorieImpotSource categorie, RegDate dateFinPeriode, RegDate dateTraitement, StatusManager status) {
			return target.sommerAllLR(categorie, dateFinPeriode, dateTraitement, status);
		}

		@Override
		public void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception {
			target.imprimerLR(dpi, dateDebutPeriode, dateFinPeriode);
		}

		@Override
		public List<DateRange> findLRsManquantes(DebiteurPrestationImposable dpi, RegDate dateFinPeriode, List<DateRange> lrTrouveesOut) {
			return target.findLRsManquantes(dpi, dateFinPeriode, lrTrouveesOut);
		}

		@Override
		public DeterminerLRsEchuesResults determineLRsEchues(Integer periodeFiscale, RegDate dateTraitement, StatusManager status) throws Exception {
			return target.determineLRsEchues(periodeFiscale, dateTraitement, status);
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		adresseService = getBean(AdresseService.class, "adresseService");
		lrService = getBean(ListeRecapService.class, "lrService");
		ticketService = getBean(TicketService.class, "ticketService");
	}

	private EnvoiLRsEnMasseProcessor buildProcessor(ListeRecapService lrService) {
		// création du processeur à la main pour pouvoir accéder aux méthodes protégées
		return new EnvoiLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService, tiersService, adresseService, ticketService);
	}

	/**
	 * [SIFISC-12895] les LR de périodicités uniques doivent maintenant être envoyées
	 */
	@Test
	public void testEnvoiLRPeriodiciteUnique() throws Exception {

		final int anneeReference = 2010;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setSansListeRecapitulative(false);

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M10, date(anneeReference, 1, 1), null);
				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				addPeriodeFiscale(anneeReference);
				return dpi.getNumero();
			}
		});

		final List<Pair<Long, DateRange>> imprimees = new ArrayList<>();
		final ListeRecapService lrs = new ListRecapServiceWrapper(lrService) {
			@Override
			public void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception {
				imprimees.add(new Pair<>(dpi.getNumero(), new DateRangeHelper.Range(dateDebutPeriode, dateFinPeriode)));
			}
		};

		final EnvoiLRsEnMasseProcessor processor = buildProcessor(lrs);

		// début de période -> rien
		{
			imprimees.clear();
			final EnvoiLRsResults results = processor.run(date(2010, 9, 30), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(0, results.LRTraitees.size());
			Assert.assertEquals(0, imprimees.size());
		}

		// fin de période -> la LR doit être envoyée
		{
			imprimees.clear();
			final EnvoiLRsResults results = processor.run(date(2010, 10, 31), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(1, results.LRTraitees.size());
			Assert.assertEquals(1, imprimees.size());

			final Pair<Long, DateRange> data = imprimees.get(0);
			Assert.assertNotNull(data);
			Assert.assertEquals((Long) dpiId, data.getFirst());
			Assert.assertEquals(date(anneeReference, 10, 1), data.getSecond().getDateDebut());
			Assert.assertEquals(date(anneeReference, 10, 31), data.getSecond().getDateFin());
		}

		// après la période -> la LR doit être envoyée
		{
			imprimees.clear();
			final EnvoiLRsResults results = processor.run(date(2010, 12, 31), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(1, results.LRTraitees.size());
			Assert.assertEquals(1, imprimees.size());

			final Pair<Long, DateRange> data = imprimees.get(0);
			Assert.assertNotNull(data);
			Assert.assertEquals((Long) dpiId, data.getFirst());
			Assert.assertEquals(date(anneeReference, 10, 1), data.getSecond().getDateDebut());
			Assert.assertEquals(date(anneeReference, 10, 31), data.getSecond().getDateFin());
		}
	}

	@Test
	public void testEnvoiPeriodiciteUniqueRienAEnvoyer() throws Exception {
		final int anneeReference = 2010;
		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setSansListeRecapitulative(false);

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M10, date(anneeReference, 1, 1), null);
				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale pf = addPeriodeFiscale(anneeReference);
				final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				addListeRecapitulative(dpi, pf, date(anneeReference, 10, 1), date(anneeReference, 10, 31), md);

				return dpi.getNumero();
			}
		});

		final List<Pair<Long, DateRange>> imprimees = new ArrayList<>();
		final ListeRecapService lrs = new ListRecapServiceWrapper(lrService) {
			@Override
			public void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception {
				imprimees.add(new Pair<>(dpi.getNumero(), new DateRangeHelper.Range(dateDebutPeriode, dateFinPeriode)));
			}
		};

		final EnvoiLRsEnMasseProcessor processor = buildProcessor(lrs);

		// la LR a déjà été envoyée -> rien de plus!
		{
			imprimees.clear();
			final EnvoiLRsResults results = processor.run(date(2010, 12, 31), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(0, results.LRTraitees.size());
			Assert.assertEquals(0, imprimees.size());
		}
	}

	/**
	 * [SIFISC-14407] La période de la LR persistée en base était fausse (= fin au 31.12) alors qu'elle était correcte dans le rapport d'exécution
	 */
	@Test
	public void testEnvoiPeriodiciteUniqueEnvoiReel() throws Exception {
		final int anneeReference = RegDate.get().year();
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M04, date(anneeReference, 1, 1), null);
				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Aigle);

				addPeriodeFiscale(anneeReference);
				return dpi.getNumero();
			}
		});

		final EnvoiLRsEnMasseProcessor processor = buildProcessor(lrService);
		final EnvoiLRsResults results = processor.run(date(anneeReference, 6, 30), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.nbDPIsTotal);
		Assert.assertEquals(0, results.LREnErreur.size());
		Assert.assertEquals(1, results.LRTraitees.size());

		final EnvoiLRsResults.Traite traitee = results.LRTraitees.get(0);
		Assert.assertNotNull(traitee);
		Assert.assertEquals(date(anneeReference, 4, 1), traitee.dateDebut);
		Assert.assertEquals(date(anneeReference, 4, 30), traitee.dateFin);

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Assert.assertNotNull(dpi);

				final List<Declaration> lrs = dpi.getDeclarationsDansPeriode(Declaration.class, anneeReference, true);
				Assert.assertNotNull(lrs);
				Assert.assertEquals(1, lrs.size());

				final Declaration lr = lrs.get(0);
				Assert.assertNotNull(lr);
				Assert.assertEquals(DeclarationImpotSource.class, lr.getClass());
				Assert.assertEquals(date(anneeReference, 4, 1), lr.getDateDebut());
				Assert.assertEquals(date(anneeReference, 4, 30), lr.getDateFin());

				final DelaiDeclaration delai = lr.getDernierDelaiDeclarationAccorde();
				Assert.assertNotNull(delai);
				Assert.assertEquals(RegDate.get(), delai.getDateTraitement());
				Assert.assertEquals(RegDate.get(), delai.getDateDemande());
			}
		});
	}

	@Test
	public void testEnvoiEffeuilleuseEnMilieuDePeriode() throws Exception {

		final int annee = 2010;
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.EFFEUILLEUSES, PeriodiciteDecompte.SEMESTRIEL, date(annee, 1, 1));
				addForDebiteur(dpi, date(annee, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aubonne);
				addPeriodeFiscale(annee);
				return dpi.getNumero();
			}
		});

		final List<Pair<Long, DateRange>> imprimees = new ArrayList<>();
		final ListeRecapService lrs = new ListRecapServiceWrapper(lrService) {
			@Override
			public void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception {
				imprimees.add(new Pair<>(dpi.getNumero(), new DateRangeHelper.Range(dateDebutPeriode, dateFinPeriode)));
			}
		};

		final EnvoiLRsEnMasseProcessor processor = buildProcessor(lrs);

		// début de période -> rien
		{
			imprimees.clear();
			final EnvoiLRsResults results = processor.run(date(2010, 2, 28), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(0, results.LRTraitees.size());
			Assert.assertEquals(0, imprimees.size());
		}

		// milieu de période -> la LR doit être envoyée
		{
			imprimees.clear();
			final EnvoiLRsResults results = processor.run(date(2010, 3, 31), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(1, results.LRTraitees.size());
			Assert.assertEquals(1, imprimees.size());

			final Pair<Long, DateRange> data = imprimees.get(0);
			Assert.assertNotNull(data);
			Assert.assertEquals((Long) dpiId, data.getFirst());
			Assert.assertEquals(date(annee, 1, 1), data.getSecond().getDateDebut());
			Assert.assertEquals(date(annee, 6, 30), data.getSecond().getDateFin());
		}

		// fin de période -> la LR doit être envoyée
		{
			imprimees.clear();
			final EnvoiLRsResults results = processor.run(date(2010, 6, 30), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(1, results.LRTraitees.size());
			Assert.assertEquals(1, imprimees.size());

			final Pair<Long, DateRange> data = imprimees.get(0);
			Assert.assertNotNull(data);
			Assert.assertEquals((Long) dpiId, data.getFirst());
			Assert.assertEquals(date(annee, 1, 1), data.getSecond().getDateDebut());
			Assert.assertEquals(date(annee, 6, 30), data.getSecond().getDateFin());
		}
	}

	/**
	 * [SIFISC-14408]
	 */
	@Test
	public void testEnvoiEffeuilleuseEnMilieuDePeriodeUnique() throws Exception {

		final int annee = 2015;
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setCategorieImpotSource(CategorieImpotSource.EFFEUILLEUSES);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.T1, date(annee, 1, 1), null);
				addForDebiteur(dpi, date(annee, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aubonne);
				addPeriodeFiscale(annee);
				return dpi.getNumero();
			}
		});

		final EnvoiLRsEnMasseProcessor processor = buildProcessor(lrService);

		// début de période -> rien
		{
			final EnvoiLRsResults results = processor.run(date(annee, 1, 31), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(0, results.LRTraitees.size());
			Assert.assertEquals(0, results.LREnErreur.size());
		}

		// milieu de période -> la LR doit être envoyée
		{
			final EnvoiLRsResults results = processor.run(date(annee, 2, 28), null);

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(1, results.LRTraitees.size());
			Assert.assertEquals(0, results.LREnErreur.size());

			final EnvoiLRsResults.Traite traitee = results.LRTraitees.get(0);
			Assert.assertNotNull(traitee);
			Assert.assertEquals(dpiId, traitee.noCtb);
			Assert.assertEquals(date(annee, 1, 1), traitee.dateDebut);
			Assert.assertEquals(date(annee, 3, 31), traitee.dateFin);

			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
					Assert.assertNotNull(dpi);

					final List<Declaration> lrs = dpi.getDeclarationsDansPeriode(Declaration.class, annee, true);
					Assert.assertNotNull(lrs);
					Assert.assertEquals(1, lrs.size());

					final Declaration lr = lrs.get(0);
					Assert.assertNotNull(lr);
					Assert.assertEquals(DeclarationImpotSource.class, lr.getClass());
					Assert.assertEquals(date(annee, 1, 1), lr.getDateDebut());
					Assert.assertEquals(date(annee, 3, 31), lr.getDateFin());
				}
			});
		}
	}
}

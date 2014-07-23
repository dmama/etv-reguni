package ch.vd.uniregctb.declaration.source;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class EnvoiLRsEnMasseProcessorTest extends BusinessTest {

	private ListeRecapService lrService;
	private AdresseService adresseService;

	private static class ListRecapServiceWrapper implements ListeRecapService {
		private final ListeRecapService target;

		private ListRecapServiceWrapper(ListeRecapService target) {
			this.target = target;
		}

		@Override
		public InputStream getCopieConformeSommationLR(DeclarationImpotSource lr) throws EditiqueException {
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
	}

	private EnvoiLRsEnMasseProcessor buildProcessor(ListeRecapService lrService) {
		// création du processeur à la main pour pouvoir accéder aux méthodes protégées
		return new EnvoiLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService, tiersService, adresseService);
	}

	@Test
	public void testEnvoiLRPeriodiciteUnique() throws Exception {

		final int anneeReference = 2010;
		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setSansListeRecapitulative(false);

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M10, date(anneeReference, 9, 1), null);
				addForDebiteur(dpi, date(anneeReference, 9, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				addPeriodeFiscale(anneeReference);
				return dpi.getNumero();
			}
		});

		final EnvoiLRsEnMasseProcessor processor = buildProcessor(lrService);
		final EnvoiLRsResults envoiLRsResults = doInNewTransaction(new TxCallback<EnvoiLRsResults>() {
			@Override
			public EnvoiLRsResults execute(TransactionStatus status) throws Exception {
				return processor.run(date(2010, 12, 31), null);
			}
		});

		Assert.assertEquals(0, envoiLRsResults.LRTraitees.size());
		Assert.assertEquals(0, envoiLRsResults.nbDPIsTotal);
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
				imprimees.add(new Pair<Long, DateRange>(dpi.getNumero(), new DateRangeHelper.Range(dateDebutPeriode, dateFinPeriode)));
			}
		};

		final EnvoiLRsEnMasseProcessor processor = buildProcessor(lrs);

		// début de période -> rien
		{
			imprimees.clear();
			final EnvoiLRsResults results = doInNewTransactionAndSession(new TransactionCallback<EnvoiLRsResults>() {
				@Override
				public EnvoiLRsResults doInTransaction(TransactionStatus status) {
					return processor.run(date(2010, 2, 28), null);
				}
			});

			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.nbDPIsTotal);
			Assert.assertEquals(0, results.LRTraitees.size());
			Assert.assertEquals(0, imprimees.size());
		}

		// milieu de période -> la LR doit être envoyée
		{
			imprimees.clear();
			final EnvoiLRsResults results = doInNewTransactionAndSession(new TransactionCallback<EnvoiLRsResults>() {
				@Override
				public EnvoiLRsResults doInTransaction(TransactionStatus status) {
					return processor.run(date(2010, 3, 31), null);
				}
			});

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
			final EnvoiLRsResults results = doInNewTransactionAndSession(new TransactionCallback<EnvoiLRsResults>() {
				@Override
				public EnvoiLRsResults doInTransaction(TransactionStatus status) {
					return processor.run(date(2010, 6, 30), null);
				}
			});

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
}

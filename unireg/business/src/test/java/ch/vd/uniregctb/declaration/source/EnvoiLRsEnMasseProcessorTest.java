package ch.vd.uniregctb.declaration.source;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class EnvoiLRsEnMasseProcessorTest extends BusinessTest {

	private ListeRecapService lrService;

	private EnvoiLRsEnMasseProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		lrService = getBean(ListeRecapService.class, "lrService");

		// création du processeur à la main pour pouvoir accéder aux méthodes protégées
		processor = new EnvoiLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService);
	}

	/**
	 * [UNIREG-3115] teste que l'envoi de LR ne plante pas lorsque la LR généré a une date de début antérieur à la date de début de // la périodicité
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiLRAnterieurPeriodicite() throws Exception {


		final int anneeReference = 2010;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setSansListeRecapitulative(false);

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.ANNUEL, null, date(anneeReference, 9, 1), null);
				addForDebiteur(dpi, date(anneeReference, 9, 1), null, MockCommune.Bex);

				final PeriodeFiscale fiscale = addPeriodeFiscale(anneeReference);
				return dpi.getNumero();
			}
		});

		final DebiteurPrestationImposable dpi = hibernateTemplate.get(DebiteurPrestationImposable.class, dpiId);
		Assert.assertNotNull(dpi);
		final EnvoiLRsResults envoiLRsResults = doInNewTransaction(new TxCallback<EnvoiLRsResults>() {
			@Override
			public EnvoiLRsResults execute(TransactionStatus status) throws Exception {
				return processor.run(date(2010, 12, 31), null);
			}
		});

		assertEquals(1,envoiLRsResults.LRTraitees.size());
		assertEquals(dpiId,envoiLRsResults.LRTraitees.get(0).noCtb);
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiLRPeriodiciteUnique() throws Exception {


		final int anneeReference = 2010;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setSansListeRecapitulative(false);

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M10, date(anneeReference, 9, 1), null);
				addForDebiteur(dpi, date(anneeReference, 9, 1), null, MockCommune.Bex);

				final PeriodeFiscale fiscale = addPeriodeFiscale(anneeReference);
				return dpi.getNumero();
			}
		});

		final DebiteurPrestationImposable dpi = hibernateTemplate.get(DebiteurPrestationImposable.class, dpiId);
		Assert.assertNotNull(dpi);
		final EnvoiLRsResults envoiLRsResults = doInNewTransaction(new TxCallback<EnvoiLRsResults>() {
			@Override
			public EnvoiLRsResults execute(TransactionStatus status) throws Exception {
				return processor.run(date(2010, 12, 31), null);
			}
		});

		assertEquals(0,envoiLRsResults.LRTraitees.size());
		assertEquals(0,envoiLRsResults.nbDPIsTotal);
	}

}

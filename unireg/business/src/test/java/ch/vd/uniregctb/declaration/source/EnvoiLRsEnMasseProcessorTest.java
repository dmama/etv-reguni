package ch.vd.uniregctb.declaration.source;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class EnvoiLRsEnMasseProcessorTest extends BusinessTest {

	private EnvoiLRsEnMasseProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final ListeRecapService lrService = getBean(ListeRecapService.class, "lrService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");

		// création du processeur à la main pour pouvoir accéder aux méthodes protégées
		processor = new EnvoiLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService, tiersService, adresseService);
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

		final EnvoiLRsResults envoiLRsResults = doInNewTransaction(new TxCallback<EnvoiLRsResults>() {
			@Override
			public EnvoiLRsResults execute(TransactionStatus status) throws Exception {
				return processor.run(date(2010, 12, 31), null);
			}
		});

		assertEquals(0, envoiLRsResults.LRTraitees.size());
		assertEquals(0, envoiLRsResults.nbDPIsTotal);
	}

}

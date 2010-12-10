package ch.vd.uniregctb.declaration.source;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
	public void testEnvoiLRAnterieurPeriodicite() throws Exception {


		final int anneeReference = 2010;
		final long dpiId = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setSansListeRecapitulative(false);
				dpi.setPeriodiciteDecompte(PeriodiciteDecompte.TRIMESTRIEL);

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.ANNUEL, null, date(anneeReference, 9, 1), null);
				addForDebiteur(dpi, date(anneeReference, 9, 1), null, MockCommune.Bex);

				final PeriodeFiscale fiscale = addPeriodeFiscale(anneeReference);
				return dpi.getNumero();
			}
		});

			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) hibernateTemplate.get(DebiteurPrestationImposable.class, dpiId);
			Assert.assertNotNull(dpi);
		final EnvoiLRsResults envoiLRsResults = (EnvoiLRsResults) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return processor.run(date(2010, 12, 31), null);
			}
		});

		assertEquals(1,envoiLRsResults.LRTraitees.size());
		assertEquals(dpiId,envoiLRsResults.LRTraitees.get(0).noCtb);
	}


}

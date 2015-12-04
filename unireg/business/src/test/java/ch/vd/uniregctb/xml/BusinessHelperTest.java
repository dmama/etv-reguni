package ch.vd.uniregctb.xml;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;

public class BusinessHelperTest extends BusinessTest {

	/**
	 * [SIFISC-12997] Seule la première ligne de la raison sociale d'un débiteur était retournée dans le getParty
	 */
	@Test
	public void testGetDebtorName() throws Exception {

		// une PM
		servicePM.setUp(new MockServicePM() {
			@Override
			protected void init() {
				addPM(MockPersonneMorale.BanqueCoopBale);
			}
		});

		// mise en place d'un débiteur avec une raison sociale sur plusieurs lignes
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm =  addEntreprise(MockPersonneMorale.BanqueCoopBale.getNumeroEntreprise());
				final DebiteurPrestationImposable dpi = addDebiteur("TotoComplément", pm, date(2000, 1, 1));
				return dpi.getNumero();
			}
		});

		// récupération du nom
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final String name = BusinessHelper.getDebtorName(dpi, tiersService);
				Assert.assertEquals("Bank Coop AG (Banque Coop SA) (Banca Coop SA) (Bank Coop Ltd)", name);
			}
		});
	}
}

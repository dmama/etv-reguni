package ch.vd.unireg.xml;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;

public class BusinessHelperTest extends BusinessTest {

	/**
	 * [SIFISC-12997] Seule la première ligne de la raison sociale d'un débiteur était retournée dans le getParty
	 */
	@Test
	public void testGetDebtorName() throws Exception {

		// une PM
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.BANQUE_COOP);
			}
		});

		// mise en place d'un débiteur avec une raison sociale sur plusieurs lignes
		final long dpiId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BANQUE_COOP.getNumeroEntreprise());
			final DebiteurPrestationImposable dpi = addDebiteur("TotoComplément", pm, date(2000, 1, 1));
			return dpi.getNumero();
		});

		// récupération du nom
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			final String name = BusinessHelper.getDebtorName(dpi, tiersService);
			Assert.assertEquals("Bank Coop AG", name);
			return null;
		});
	}
}

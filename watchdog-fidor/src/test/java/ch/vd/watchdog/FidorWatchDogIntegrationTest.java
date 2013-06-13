package ch.vd.watchdog;

import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClientImpl;

/**
 * Teste que Fidor est accessible dans l'environnement d'intégration
 */
public class FidorWatchDogIntegrationTest extends FidorWatchDogTest {

	@Override
	protected FidorClient connectToFidor() {
		final FidorClientImpl client = new FidorClientImpl();
		client.setUsername("gvd0unireg");
		client.setPassword("Welc0me_");
		client.setServiceUrl("http://rp-ws-va.etat-de-vaud.ch/fiscalite/int-fidor/ws/v5");
		return client;
	}
}

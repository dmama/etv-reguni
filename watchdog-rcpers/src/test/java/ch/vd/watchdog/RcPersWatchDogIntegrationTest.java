package ch.vd.watchdog;

import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;

/**
 * Teste que RcPers est accessible dans l'environnement d'intégration
 */
public class RcPersWatchDogIntegrationTest extends RcPersWatchDogTest {

	@Override
	protected RcPersClientImpl buildClient() throws Exception {
		final RcPersClientImpl client = new RcPersClientImpl();
		client.setBaseUrl("http://rp-ws-va.etat-de-vaud.ch/registres/int-rcpers/west/ws/v3");
		client.setPeoplePath("persons/ct.vd.rcpers");
		client.setEventPath("event");
		client.setRelationsPath("relations/ct.vd.rcpers");
		client.setUsername("gvd0unireg");
		client.setPassword("welc0me_");
		client.afterPropertiesSet();
		return client;
	}
}

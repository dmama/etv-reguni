package ch.vd.watchdog;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorDate;
import ch.vd.fidor.ws.v2.Logiciel;
import ch.vd.fidor.ws.v2.Pays;
import ch.vd.uniregctb.webservice.fidor.FidorClient;
import ch.vd.uniregctb.webservice.fidor.FidorClientImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Teste que Fidor est accessible dans l'environnement d'intégration
 */
public class FidorWatchDogIntegrationTest extends FidorWatchDogTest {

	@Override
	protected FidorClient connectToFidor() {
		final FidorClientImpl client = new FidorClientImpl();
		client.setUsername("");
		client.setPassword("");
		client.setServiceUrl("http://fidor-in.etat-de-vaud.ch/fiscalite/int-fidor/ws/v2");
		return client;
	}
}

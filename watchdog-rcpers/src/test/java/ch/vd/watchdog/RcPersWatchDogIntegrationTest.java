package ch.vd.watchdog;

import java.util.Arrays;
import java.util.List;

import ch.ech.ech0044.v2.NamedPersonId;
import org.junit.Test;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.Relations;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0006.v1.Event;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Teste que RcPers est accessible dans l'environnement d'intégration
 */
public class RcPersWatchDogIntegrationTest extends RcPersWatchDogTest {

	@Override
	protected RcPersClientImpl buildClient() throws Exception {
		final RcPersClientImpl client = new RcPersClientImpl();
		client.setBaseUrl("http://rp-ws-va.etat-de-vaud.ch/registres/int-rcpers/west/ws/v3");
		client.setPeoplePath("persons/ct.vd.rcpers.unireg");
		client.setEventPath("event");
		client.setRelationsPath("relations/ct.vd.rcpers.unireg");
		client.setUsername("gvd0unireg");
		client.setPassword("welc0me_");
		client.afterPropertiesSet();
		return client;
	}
}

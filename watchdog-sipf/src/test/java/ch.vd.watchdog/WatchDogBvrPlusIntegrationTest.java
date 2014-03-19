package ch.vd.watchdog;

import junit.framework.TestCase;

import ch.vd.uniregctb.webservice.sipf.BVRPlusClientImpl;

/**
 * Teste que le service BVRPlus de SIPF est disponible et fonctionne.
 */
public class WatchDogBvrPlusIntegrationTest extends TestCase {

	public void testIntegration() throws Exception {

		final BVRPlusClientImpl service = new BVRPlusClientImpl();
		service.setServiceUrl("http://slv2352v.etat-de-vaud.ch:50100/fiscalite/int-sipf/services/bvrplus");
		service.ping();
	}
}
package ch.vd.watchdog;

import java.math.BigInteger;

import junit.framework.TestCase;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;
import ch.vd.uniregctb.webservice.sipf.BVRPlusClientImpl;

/**
 * Teste que le service BVRPlus de SIPF est disponible et fonctionne.
 */
public class WatchDogBvrPlusIntegrationTest extends TestCase {

	public void testIntegration() throws Exception {

		final BVRPlusClientImpl service = new BVRPlusClientImpl();
		service.setServiceUrl("http://ssv0213v.etat-de-vaud.ch:50110/fiscalite/int-sipf/bvrplus/SipfBVRPlusImpl");
		service.ping();
	}
}
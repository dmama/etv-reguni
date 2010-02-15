package ch.vd.uniregctb.webservice.sipf;

import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;

import org.junit.Test;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;

public class BVRPlusServiceTest {

	//private static final Logger LOGGER = Logger.getLogger(BVRPlusServiceTest.class);

	@Test
	public void testGetBVRDemande() throws Exception {
		BVRPlusClientImpl service = new BVRPlusClientImpl();
		service.setServiceUrl("http://ssv0214v:64804/fiscalite/form-sipf/bvrplus/SipfBVRPlusImpl");

		BvrDemande demande = new BvrDemande();
		demande.setNdc("289179");
		demande.setAnneeTaxation(BigInteger.valueOf(2008));
		demande.setTypeDebiteurIS("REGULIER");

		final BvrReponse resultat = service.getBVRDemande(demande);
		assertNotNull(resultat);
	}

}

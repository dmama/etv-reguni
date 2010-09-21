package ch.vd.uniregctb.webservice.sipf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;

public class BVRPlusServiceTest {

	//private static final Logger LOGGER = Logger.getLogger(BVRPlusServiceTest.class);

	@Ignore(value = "le service est down pour une durée indéterminée")
	@Test
	public void testGetBVRDemande() throws Exception {
		final BVRPlusClientImpl service = new BVRPlusClientImpl();
		service.setServiceUrl("http://ssv0213v.etat-de-vaud.ch:64904/fiscalite/int-sipf/bvrplus/SipfBVRPlusImpl");

		BvrDemande demande = new BvrDemande();
		demande.setNdc("0");
		demande.setAnneeTaxation(BigInteger.valueOf(2008));
		demande.setTypeDebiteurIS("REGULIER");

		// on essaie avec le débiteur 0 qui n'existe pas pour ne pas générer de nouveau numéro de BVR, la seule chose qui nous intéresse, c'est de recevoir une réponse
		final BvrReponse reponse = service.getBVRDemande(demande);
		assertNotNull(reponse);
		assertTrue(reponse.getMessage().contains("CONTRIB_ABSENT"));
	}

}

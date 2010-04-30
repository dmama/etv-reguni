package ch.vd.uniregctb.webservice.acicom;

import org.junit.Test;


import ch.vd.dfin.acicom.web.services.meldewesen.impl.ContenuMessage;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.RecupererContenuMessage;

import static org.junit.Assert.assertNotNull;

public class AcicomServiceTest {

	//private static final Logger LOGGER = Logger.getLogger(BVRPlusServiceTest.class);

	@Test
	public void testGetRecupererMessage() throws Exception {
		AciComClientImpl service = new AciComClientImpl();
		service.setServiceUrl("http://ssv0213v:60102/fiscalite/int-acicom/services/meldewesenConsultationService");

		RecupererContenuMessage demande = new RecupererContenuMessage();
		demande.setMessageId("3002-000101-2007-3-CH-30-3002-000101_PCAP3b_120");

		try{
			final ContenuMessage  reponse = service.recupererMessage(demande);
			assertNotNull(reponse);
		}
		catch (Exception e){
		
			
		}


	}

}
package ch.vd.vuta.processing;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import ch.vd.vuta.AbstractSmsgwTestCase;
import ch.vd.vuta.SmsException;
import ch.vd.vuta.model.SmsModel;

public class SmsProcessorTest extends AbstractSmsgwTestCase {
	
	private SmsProcessor processor;
	
	public void onSetUp() {

		super.onSetUp();

		processor = new SmsProcessor(applicationContext);
	}

	public void onTearDown() {
		
		super.onTearDown();
	}
	
	public void testTreatSms() throws Exception {
		
		String natel = "+41791234567";
		Integer ctb = 439382535;
		String texte = "IFD "+ctb; // Existe dans le mock
		String operateur = "sunrise";
		String langue = "fr";
		String requestUid = "sms98562";
		
		// Numero CTB OK
		{
			String smsAsXml = SmsProcessor.getSmsAsXml(natel, texte, operateur, langue, requestUid);
			ProcessorResponse resp = processor.treatSms(smsAsXml);
			String text = resp.getTexteForSender();
			assertTrue(text, text.startsWith("IFD "+ctb.toString()+": Merci"));
			assertEquals(true, resp.getSms().getStatusAsBool());
			assertEquals(processor.encodeNoCtbAsXmlResponse(ctb), resp.getTexteForSender());
		}
		
		// Wrong XML format
		{
			String smsAsXml = SmsProcessor.getSmsAsXml(natel, texte, operateur, langue, requestUid);
			smsAsXml += "bla"; // Wrong XML
			
			ProcessorResponse resp = processor.treatSms(smsAsXml);
			assertEquals(ProcessorResponse.ERROR_RESPONSE, resp.getTexteForSender());
			assertEquals(false, resp.getSms().getStatusAsBool());
			assertEquals(ProcessorResponse.STATUS_XML_INVALIDE, resp.getSms().getStatusString());
		}
		
		// Numero CTB doesn't exists
		{
			String smsAsXml = SmsProcessor.getSmsAsXml(natel, "IFD "+(ctb+10)/*N'existe pas dans le mock*/, operateur, langue, requestUid);
			ProcessorResponse resp = processor.treatSms(smsAsXml);
			assertEquals(ProcessorResponse.ERROR_RESPONSE, resp.getTexteForSender());
			assertEquals(false, resp.getSms().getStatusAsBool());
			assertEquals(SmsProcessor.generateNumeroCtbIntrouvableMessage(new Integer(ctb+10).toString()), resp.getSms().getStatusString());
		}
		
		// SMS1 => Numero CTB inexistant
		{
			InputStream in = this.getClass().getResourceAsStream("/ch/vd/dfin/smsgw/processing/sms1.xml");
			InputStreamReader buf = new InputStreamReader(in);
			BufferedReader reader = new BufferedReader(buf);
			String smsAsXml = "";
			String line;
			while ((line = reader.readLine()) != null) {
				smsAsXml += line;
			}

			ProcessorResponse resp = processor.treatSms(smsAsXml);
			assertEquals(ProcessorResponse.ERROR_RESPONSE, resp.getTexteForSender());
			assertEquals(SmsProcessor.generateNumeroCtbIntrouvableMessage("10000002"), resp.getSms().getStatusString());
		}
	}
	
	public void testEncodeResponseAsXml() {
	
		Integer noCtb = 3243452;
		String xml = SmsProcessor.encodeNoCtbAsXmlResponse(noCtb);
		assertTrue(xml.startsWith("IFD "+noCtb.toString()+": Merci"));
	}
	
	public void testValidateNumeroContribuableSmsModel() {
		
		SmsModel sms = new SmsModel();
		
		// Numero correct
		try {
			sms.setTexte("IFD 19");
			processor.validateNumeroContribuable(sms);
		}
		catch (SmsException e) {
			fail();
		}

		// Numero avec espace => doit marcher
		try {
			sms.setTexte("IFD  1234  ");
			processor.validateNumeroContribuable(sms);
		}
		catch (SmsException e) {
			fail();
		}

		// Numero inexistant
		try {
			sms.setTexte("IFD 199322");
			processor.validateNumeroContribuable(sms);
			fail();
		}
		catch (SmsException e) {
			assertEquals(SmsProcessor.generateNumeroCtbIntrouvableMessage("199322"), e.getMessage());
		}

		// Numero pourri
		try {
			sms.setTexte("IFD  56ss56 ds 12 ");
			processor.validateNumeroContribuable(sms);
			fail();
		}
		catch (SmsException e) {
			assertEquals(SmsProcessor.generateNumeroCtbInvalideMessage("56ss56 ds 12"), e.getMessage());
		}

	}

	public void testDecodeSms() throws Exception {
		
		String natel = "+41791234567";
		Integer ctb = 98765432;
		String operateur = "sunrise";
		String langue = "fr";
		String requestUid = "sms98562";
		
		Date beforeDate = new Date();
		
		String smsAsXml = SmsProcessor.getSmsAsXml(natel, "IFD "+ctb, operateur, langue, requestUid);
		SmsModel sms = processor.decodeSmsAsXml(smsAsXml);

		Date afterDate = new Date();
		
		//LOGGER.debug("Before:"+beforeDate+" Sms:"+sms.getDateReception()+" After:"+afterDate);
		
		assertEquals(natel, sms.getNumeroNatel());
		assertEquals("IFD "+ctb, sms.getTexte());
		assertEquals(operateur, sms.getOperateur());
		assertEquals(langue, sms.getLangue());
		assertEquals(requestUid, sms.getRequestUid());
		assertTrue(beforeDate.compareTo(sms.getDateReception()) <= 0);
		assertTrue(afterDate.compareTo(sms.getDateReception()) >= 0);
	}
	
}

package ch.vd.vuta.web;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.vd.vuta.AbstractSmsgwTestCase;
import ch.vd.vuta.processing.ProcessorResponse;
import ch.vd.vuta.processing.SmsProcessor;

public class Ifd2008PageTest extends AbstractSmsgwTestCase {

	public void testPageNumeroExists() throws Exception {
		
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String natel = "+41791234587";
		Integer ctb = 19;
		String operateur = "sunrise";
		String langue = "fr";
		String requestUid = "sms9856232";
		String texte = "IFD  "+ctb+"  ";
		String xml = SmsProcessor.getSmsAsXml(natel, texte, operateur, langue, requestUid);
		
		byte[] content = xml.getBytes();
		req.setContent(content);
		
		Ifd2008Page page = new Ifd2008Page(applicationContext);
		try {
			page.processPage(req, resp);
		}
		catch (Exception e) {
			fail();
		}

		String expected = SmsProcessor.encodeNoCtbAsXmlResponse(ctb);
		expected = SmsProcessor.getTextAsXmlResponse(expected);
		
		String actual = resp.getContentAsString();
		assertEquals(expected, actual);
	}

	public void testPageNumeroExistePas() throws Exception {
		
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String natel = "+41791234587";
		Integer ctb = 98765432;
		String operateur = "sunrise";
		String langue = "fr";
		String requestUid = "sms98562325";
		String texte = "IFD "+ctb;
		String xml = SmsProcessor.getSmsAsXml(natel, texte, operateur, langue, requestUid);
		
		byte[] content = xml.getBytes();
		req.setContent(content);
		
		Ifd2008Page page = new Ifd2008Page(applicationContext);
		try {
			page.processPage(req, resp);
		}
		catch (Exception e) {
			fail();
		}

		String expected = SmsProcessor.getTextAsXmlResponse(ProcessorResponse.ERROR_RESPONSE);
		
		assertEquals(expected, resp.getContentAsString());
		
	}

	public void testPageNumeroInsulte() throws Exception {
		
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String natel = "+41791234587";
		Integer ctb = 98765432;
		String operateur = "sunrise";
		String langue = "fr";
		String requestUid = "sms98562325";
		String texte = "IFD Espece de petit méchant ";
		String xml = SmsProcessor.getSmsAsXml(natel, texte, operateur, langue, requestUid);
		
		byte[] content = xml.getBytes();
		req.setContent(content);
		
		Ifd2008Page page = new Ifd2008Page(applicationContext);
		try {
			page.processPage(req, resp);
		}
		catch (Exception e) {
			fail();
		}

		String expected = SmsProcessor.getTextAsXmlResponse(ProcessorResponse.ERROR_RESPONSE);
		
		assertEquals(expected, resp.getContentAsString());
		
	}
	
}

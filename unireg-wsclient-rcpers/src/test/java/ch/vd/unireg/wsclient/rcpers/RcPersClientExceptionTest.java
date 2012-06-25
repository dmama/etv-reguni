package ch.vd.unireg.wsclient.rcpers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RcPersClientExceptionTest {

	@Test
	public void testExtractTitle() {

		assertNull(RcPersClientException.extractTitle("<html><head><bloblo>titre</bloblo></head></html>"));
		assertEquals("titre", RcPersClientException.extractTitle("<html><head><title>titre</title></head></html>"));
		assertEquals("titre", RcPersClientException.extractTitle("<html><head><title>titre</title></head><body><div><title>www</title></div></body></html>"));

		assertEquals("503 Service Temporarily Unavailable", RcPersClientException.extractTitle("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" +
				"<html><head>\n" +
				"<title>503 Service Temporarily Unavailable</title>\n" +
				"</head><body>\n" +
				"<h1>Service Temporarily Unavailable</h1>\n" +
				"<p>The server is temporarily unable to service your\n" +
				"request due to maintenance downtime or capacity\n" +
				"problems. Please try again later.</p>\n" +
				"</body></html>\n"));
	}
}

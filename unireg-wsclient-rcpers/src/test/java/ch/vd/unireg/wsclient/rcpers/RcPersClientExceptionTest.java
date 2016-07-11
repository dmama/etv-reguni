package ch.vd.unireg.wsclient.rcpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

	@Test
	public void testTransientCause() throws Exception {
		final RcPersClientException e;
		try {
			throw new IllegalAccessException("Boum!");
		}
		catch (IllegalAccessException iae) {
			e = new RcPersClientException("Badaboum!", iae);
		}

		assertNotNull(e.getCause());
		assertEquals("Boum!", e.getCause().getMessage());
		assertEquals("Badaboum!", e.getMessage());

		// sérialisation
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(baos);
		try {
			out.writeObject(e);
		}
		finally {
			out.close();
		}

		// dé-sérialisation
		final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bais);
		final Object deserialized;
		try {
			deserialized = in.readObject();
		}
		finally {
			in.close();
		}

		assertNotNull(deserialized);
		assertEquals(RcPersClientException.class, deserialized.getClass());

		final RcPersClientException exception = (RcPersClientException) deserialized;
		assertNull(exception.getCause());
		assertEquals("Badaboum!", exception.getMessage());
	}
}

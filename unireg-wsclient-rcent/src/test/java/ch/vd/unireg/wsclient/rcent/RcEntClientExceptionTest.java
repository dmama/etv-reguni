package ch.vd.unireg.wsclient.rcent;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Raphaël Marmier, 2015-08-10
 */
public class RcEntClientExceptionTest {

	@Test
	public void testExtractSimple() throws Exception {
		final String erreurSimple = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<eVD-0004:errors xmlns:eVD-0004=\"http://evd.vd.ch/xmlns/eVD-0004/3\"><eVD-0004:error><eVD-0004:code>100</eVD-0004:code><eVD-0004:message>Exception : catégorie de l'identifiant non valide : ct.vd.party</eVD-0004:message></eVD-0004:error></eVD-0004:errors>\n";

		final String extracted = RcEntClientException.extractMessage(erreurSimple);

		Assert.assertEquals("Exception : catégorie de l'identifiant non valide : ct.vd.party", extracted);
	}

	@Test
	public void testExtractMultipleMultiline() throws Exception {
		final String erreurMultipleMultiline = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<eVD-0004:errors xmlns:eVD-0004=\"http://evd.vd.ch/xmlns/eVD-0004/3\">\n  <eVD-0004:error>\n    <eVD-0004:code>100</eVD-0004:code>\n    <eVD-0004:message>Exception : catégorie de l'identifiant non valide : ct.vd.party</eVD-0004:message>\n  </eVD-0004:error>\n\n" +
				"  <eVD-0004:error>\n" +
				"    <eVD-0004:code>100</eVD-0004:code>\n" +
				"    <eVD-0004:message>Deuxième message d'erreur!</eVD-0004:message>\n" +
				"  </eVD-0004:error>\n</eVD-0004:errors>\n";

		final String extracted = RcEntClientException.extractMessage(erreurMultipleMultiline);

		Assert.assertEquals("Exception : catégorie de l'identifiant non valide : ct.vd.party | Deuxième message d'erreur!", extracted);
	}
}
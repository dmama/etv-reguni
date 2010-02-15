package ch.vd.uniregctb.fiscal.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.logging.Level;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;

import ch.vd.registre.fiscal.model.EnumTypeImposition;
import ch.vd.registre.fiscal.model.impl.ContribuableRetourInfoDiImpl;
import ch.vd.registre.fiscal.service.impl.ServiceFiscalMDB;

/**
 *
 * Test de non regression du Message driven Bean
 *
 *  @author xsibnm
 * @version $Revision: 1.0 $
 */
public class ServiceFiscalMDBTestCase extends TestCase {

	private static final String CONTRIBUABLE_RETOUR_INFO_DI = "sdiDocTest01.xml";
	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getAnonymousLogger();

	public void testCreateContribuableRetourInfoDiFromDocument() {
		try {
			ServiceFiscalMDB mdb = new ServiceFiscalMDB();
			mdb.setXpathFactory(XPathFactory.newInstance());

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(getClass().getClassLoader().getResourceAsStream(CONTRIBUABLE_RETOUR_INFO_DI));

			ContribuableRetourInfoDiImpl info = mdb.createContribuableRetourInfoDiFromDocument(doc);

			assertEquals(info.getNoContribuable(), 10024519);
			assertEquals(info.getAnneeFiscale(), 2007);
			assertEquals(info.getEmail(), "georges.dupont@abraxas.ch");
			assertEquals(info.getIban(), "NO IBAN");
			assertEquals(info.getNoImpotAnnee(), 1);
			assertEquals(info.getNoMobile(), "0791334555");
			assertEquals(info.getNoTelephone(), "0217654321");
			assertEquals(info.getTitulaireCompte(), "Cas 3 Dupont Georges");
			assertEquals(info.getTypeImposition(), EnumTypeImposition.ELECTRONIQUE);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "testCreateContribuableRetourInfoDiFromDocument", e);
			fail(e.getMessage());
		}
	}

	public void a_testSendMessage() throws Exception {

		Connection qc = null;
		Session s = null;

		try {
			InputStream stream = getClass().getResourceAsStream("/"+CONTRIBUABLE_RETOUR_INFO_DI);
			InputStreamReader reader = new InputStreamReader(stream);

			StringBuffer sb = new StringBuffer(1024);
			char[] chars = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(chars)) > -1) {
				sb.append(String.valueOf(chars, 0, numRead));
			}
			reader.close();

			Hashtable map = new Hashtable();
			map.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
			map.put(Context.PROVIDER_URL, "t3://localhost:7001");
			InitialContext ctx = new InitialContext(map);
			ConnectionFactory qcf = (ConnectionFactory) ctx.lookup("ch.vd.fiscalite.integration.jms.CF");
			qc = qcf.createConnection();
			s = qc.createSession(false, QueueSession.CLIENT_ACKNOWLEDGE);
			Destination d = (Destination) ctx.lookup("ch.vd.fiscalite.integration.jms.DIProviderTopic");
			MessageProducer mp = s.createProducer(d);
			TextMessage tm = s.createTextMessage(sb.toString());
			mp.send(tm);
		}
		catch (Exception e) {
			LOGGER.log(Level.SEVERE, "a_testSendMessage", e);
		}
		finally {
			try {
				s.close();
			}
			catch (Exception ignored) {
			}

			try {
				qc.close();
			}
			catch (Exception ignored) {
			}

		}

		LOGGER.info("Terminé");
	}

	public static void main(String[] args) {
		ServiceFiscalMDBTestCase launcher = new ServiceFiscalMDBTestCase();
		try {
			launcher.a_testSendMessage();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "launcher", e);
		}
	}
}

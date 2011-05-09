package ch.vd.uniregctb.evenement;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.springframework.util.Log4jConfigurer;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.utils.UniregProperties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class EvenementTest {

	protected EsbJmsTemplate esbTemplate;

	protected EsbMessageFactory esbMessageFactory;
	protected UniregProperties uniregProperties;

	protected EvenementTest() {
		initLog4j();
		initProps();
	}

	private void initLog4j() {
		try {
			Log4jConfigurer.initLogging("classpath:ut/log4j.xml");
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void initProps() {
		try {
			uniregProperties = new UniregProperties();
			uniregProperties.setFilename("file:../base/unireg-ut.properties");
			uniregProperties.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void clearQueue(String queueName) throws Exception {
		while (esbTemplate.receive(queueName) != null) {}
	}

	protected void assertTextMessage(String queueName, final String texte) throws Exception {

		esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
		final EsbMessage msg = esbTemplate.receive(queueName);
		assertNotNull(msg);
		String actual = msg.getBodyAsString();
		actual = actual.replaceAll(" standalone=\"(no|yes)\"", ""); // on ignore l'attribut standalone s'il existe
		assertEquals(texte, actual);

		final EsbMessage noMsg = esbTemplate.receive(queueName);
		assertNull(noMsg);
	}

	protected void sendTextMessage(String queueName, String texte) throws Exception {
		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);

		esbTemplate.send(m);
	}

	/**
	 * @param year  l'année
	 * @param month le mois (1-12)
	 * @param day   le jour (1-31)
	 * @return une date initialisée au jour, mois et année spécifiés.
	 */
	protected static Date newUtilDate(int year, int month, int day) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.clear();
		cal.set(year, month - 1, day);
		return cal.getTime();
	}
}


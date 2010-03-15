package ch.vd.uniregctb.evenement;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.jms.core.BrowserCallback;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class EvenementTest {

	protected EsbJmsTemplate esbTemplate;

	protected EsbMessageFactory esbMessageFactory;

	protected void clearQueue(String queueName) throws Exception {
		while (esbTemplate.receive(queueName) != null) {}
	}

	protected void assertTextMessage(String queueName, final String texte) throws Exception {

		esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
		final EsbMessage msg = esbTemplate.receive(queueName);
		assertNotNull(msg);
		assertEquals(texte, msg.getBodyAsString());

		final EsbMessage noMsg = esbTemplate.receive(queueName);
		assertNull(noMsg);
	}

	protected void sendTexteMessage(String queueName, String texte) throws Exception {

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setDomain("fiscalite");
		m.setContext("test");
		m.setApplication("unireg");
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


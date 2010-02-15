package ch.vd.uniregctb.evenement;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.spring.EsbTemplate;
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

	protected EsbTemplate esbTemplate;

	protected void clearQueue(String queueName) {
		while (esbTemplate.receive(queueName) != null) {
		}
	}

	protected void assertTextMessage(String queueName, final String texte) throws Exception {

		esbTemplate.browse(queueName, new BrowserCallback() {
			public Object doInJms(Session session, QueueBrowser browser) throws JMSException {
				final Enumeration<?> enumeration = browser.getEnumeration();

				// On attend le message jusqu'à 3 secondes
				for (int i = 0; !enumeration.hasMoreElements() && i < 30; i++) {
					try {
						Thread.sleep(100); // nécessaire pour laisser à l'ESB le temps de publier le message
					}
					catch (InterruptedException e) {
						// ignored
					}
				}
				assertTrue(enumeration.hasMoreElements());

				final Message msg = (Message) enumeration.nextElement();
				final ActiveMQTextMessage txtMsg = (ActiveMQTextMessage) msg;
				assertNotNull(txtMsg);
				assertEquals(texte, txtMsg.getText());
				assertFalse(enumeration.hasMoreElements());
				return null;
			}
		});
	}

	protected void sendTexteMessage(String queueName, String texte) throws Exception {

		EsbMessage m = new EsbMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setDomain("fiscalite");
		m.setContext("test");
		m.setApplication("unireg");
		m.setServiceDestination(queueName);
		m.setBody(texte);

		esbTemplate.sendEsbMessage(queueName, m);
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


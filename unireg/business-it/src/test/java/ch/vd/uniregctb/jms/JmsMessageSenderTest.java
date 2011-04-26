package ch.vd.uniregctb.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactoryBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;

/**
 * Ce test n'est pas un vrai test: il permet juste de simuler l'envoi d'un message pour du debug manuel.
 */
@RunWith(JUnit4ClassRunner.class)
public class JmsMessageSenderTest {

	private static final Logger LOGGER = Logger.getLogger(JmsMessageSenderTest.class);

	@Test
	public void testSendMessage() throws Exception {

		//sendMessage();
		//sendCorrectionOrigineMessages();
	}


	public void sendMessage() throws Exception {

		//BufferedReader reader = new BufferedReader(new FileReader("sdiDocTest01.xml"));

		InputStream stream = getClass().getResourceAsStream("/jms/EvtRegCivil-jms-message.xml");
		InputStreamReader reader = new InputStreamReader(stream);

		final StringBuffer sb = new StringBuffer(1024);
		char[] chars = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(chars)) > -1) {
			sb.append(String.valueOf(chars, 0, numRead));
		}
		reader.close();

		ActiveMQConnectionFactory jmsConnectionManager = new ActiveMQConnectionFactory();
		jmsConnectionManager.setBrokerURL("tcp://ssv0309v:50900");
		jmsConnectionManager.setUserName("smx");
		jmsConnectionManager.setPassword("smx");

		PooledConnectionFactoryBean jmsConnectionFactory = new PooledConnectionFactoryBean();
		jmsConnectionFactory.setConnectionFactory(jmsConnectionManager);
		jmsConnectionFactory.setMaxConnections(8);
		//jmsConnectionFactory.setTransactionManager(transactionManager);

		JmsTemplate jmsTemplate = new JmsTemplate(jmsConnectionFactory.getConnectionFactory());

		jmsTemplate.send("ch.vd.registre.evtCivil", new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				TextMessage tm = session.createTextMessage();
				tm.setText(sb.toString());
				return tm;
			}
		});

		LOGGER.info("Terminé");
	}

	public void sendCorrectionOrigineMessages() throws Exception {

		final ActiveMQConnectionFactory jmsConnectionManager = new ActiveMQConnectionFactory();
		jmsConnectionManager.setBrokerURL("tcp://ssv0309v:50900");
		jmsConnectionManager.setUserName("smx");
		jmsConnectionManager.setPassword("smx");

		final PooledConnectionFactoryBean jmsConnectionFactory = new PooledConnectionFactoryBean();
		jmsConnectionFactory.setConnectionFactory(jmsConnectionManager);
		jmsConnectionFactory.setMaxConnections(8);
		//jmsConnectionFactory.setTransactionManager(transactionManager);

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		final EsbJmsTemplate esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionManager);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");

		EsbMessageFactory esbMessageFactory = new EsbMessageFactory();

		final List<Long> ids = getIndividuIds();
		final int size = ids.size();
		
		for (int i = 0; i < size; i++) {

			final int eventId = i + 816138;
			final String body = "<EvtRegCivil xmlns=\"http://www.vd.ch/schema/registreCivil/20070914/EvtRegCivil\">\n" +
					"  <NoTechnique>" + eventId + "</NoTechnique>\n" +
					"  <Code>41070</Code>\n" +
					"  <NoIndividu>" + ids.get(i) + "</NoIndividu>\n" +
					"  <DateEvenement>2010-05-10</DateEvenement>\n" +
					"  <DateTraitement>2010-05-10</DateTraitement>\n" +
					"  <NumeroOFS>5264</NumeroOFS>\n" +
					"</EvtRegCivil>";

			final EsbMessage m = esbMessageFactory.createMessage();
			m.setBusinessId(String.valueOf(eventId));
			m.setBusinessUser("sendCorrectionDateNaissanceMessages");
			m.setServiceDestination("ch.vd.registre.evtCivil.zsimsn");
			m.setContext("evenementCivil");
			m.setBody(body);
			
			esbTemplate.send(m);
		}

		LOGGER.info("Terminé");
	}

	private List<Long> getIndividuIds() throws FileNotFoundException {
		
		final String filename = "/home/msi/bidon/nos_individus_10000.txt";
		File file = new File(filename);
		if (!file.exists()) {
			throw new FileNotFoundException("Le fichier '" + filename + "' n'existe pas.");
		}
		if (!file.canRead()) {
			throw new FileNotFoundException("Le fichier '" + filename + "' n'est pas lisible.");
		}

		final List<Long> noInd = new ArrayList<Long>();

		Scanner s = new Scanner(file);
		while (s.hasNextLine()) {
			final String line = s.nextLine();
			if (StringUtils.isBlank(line)) {
				continue;
			}

			final Long no = Long.valueOf(line.trim());
			noInd.add(no);
		}
		s.close();
		return noInd;
	}

}

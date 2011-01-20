package ch.vd.uniregctb.evenement.pm;

import java.io.File;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;

import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.data.MockDataEventService;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.indexer.tiers.MockTiersIndexer;
import ch.vd.uniregctb.pm.EntrepriseEventListener;

import static org.junit.Assert.assertEquals;

/**
 * Classe de test du listener d'événements PM. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@RunWith(JUnit4ClassRunner.class)
public class EntrepriseEventListenerTest extends EvenementTest {

	private static final String INPUT_QUEUE = "unireg.test.pm.input";
	private DefaultMessageListenerContainer container;
	private MockDataEventService dataEventService;
	private MockTiersIndexer indexer;

	@Before
	public void setUp() throws Exception {

		Log4jConfigurer.initLogging("classpath:ut/log4j.xml");

		final ActiveMQConnectionFactory jmsConnectionManager = new ActiveMQConnectionFactory();
		jmsConnectionManager.setBrokerURL("tcp://ssv0309v:50900");
		jmsConnectionManager.setUserName("smx");
		jmsConnectionManager.setPassword("smx");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionManager);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");

		clearQueue(INPUT_QUEUE);

		dataEventService = new MockDataEventService();
		indexer = new MockTiersIndexer();

		final EntrepriseEventListener listener = new EntrepriseEventListener();
		listener.setEsbTemplate(esbTemplate);
		listener.setDataEventService(dataEventService);
		listener.setIndexer(indexer);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[] {new ClassPathResource("xsd/pm/EvenementEntreprise.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(jmsConnectionManager);
		container.setMessageListener(listener);
		container.setDestinationName(INPUT_QUEUE);
		container.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		container.destroy();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveEvenementPM() throws Exception {

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/pm/pm_event.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (dataEventService.changedTiers.isEmpty()) {
			Thread.sleep(100);
		}

		// On vérifique que la PM est bien réindexée et que le service 'data event' est bien notifié du changement
		assertEquals(1, dataEventService.changedTiers.size());
		assertEquals(Long.valueOf(53220), dataEventService.changedTiers.get(0));
		assertEquals(1, indexer.scheduled.size());
		assertEquals(Long.valueOf(53220), indexer.scheduled.get(0));
	}
}
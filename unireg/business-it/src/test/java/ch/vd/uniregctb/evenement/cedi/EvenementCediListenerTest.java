package ch.vd.uniregctb.evenement.cedi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.ResourceUtils;

import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Classe de test du listener d'événements CEDI. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementCediListenerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementCediListenerImpl listener;
	private DefaultMessageListenerContainer container;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtCedi");

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

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplate() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		listener = new EvenementCediListenerImpl();
		listener.setEsbTemplate(esbTemplate);
		listener.setHibernateTemplate(hibernateTemplate);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[] {new ClassPathResource("xsd/cedi/Sdi.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(jmsConnectionManager);
		container.setMessageListener(listener);
		container.setDestinationName(INPUT_QUEUE);
		container.afterPropertiesSet();
		container.start();
	}

	@After
	public void tearDown() {
		container.destroy();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveRetourDI() throws Exception {

		final List<EvenementCedi> events = new ArrayList<EvenementCedi>();

		listener.setHandler(new EvenementCediHandler() {
			@Override
			public void onEvent(EvenementCedi event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/retour_di.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(12500001L, q.getNoContribuable());
		assertEquals(2009, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.ORDINAIRE, q.getTypeDocument());
		assertNull(q.getEmail());
		assertEquals("CH3708401016ZZ0535380", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}

	/**
	 * [UNIREG-2603] Vérifie qu'on ne crashe pas quand on reçoit un retour de DI presque vide.
	 */
	@SuppressWarnings({"JavaDoc"})
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveRetourDIPresqueVide() throws Exception {

		final List<EvenementCedi> events = new ArrayList<EvenementCedi>();

		listener.setHandler(new EvenementCediHandler() {
			@Override
			public void onEvent(EvenementCedi event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/retour_di_presque_vide.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(12500001L, q.getNoContribuable());
		assertEquals(2009, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.ORDINAIRE, q.getTypeDocument());
		assertNull(q.getEmail());
		assertNull(q.getIban());
		assertNull(q.getNoTelephone());
		assertNull(q.getNoMobile());
		assertNull(q.getTitulaireCompte());
	}
}

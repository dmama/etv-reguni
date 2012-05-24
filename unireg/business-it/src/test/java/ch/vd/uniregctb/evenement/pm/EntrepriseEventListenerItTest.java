package ch.vd.uniregctb.evenement.pm;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.orm.hibernate3.HibernateTemplate;
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
public class EntrepriseEventListenerItTest extends EvenementTest {

	private String INPUT_QUEUE;
	private MockDataEventService dataEventService;
	private MockTiersIndexer indexer;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.pm.event.input");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");

		clearQueue(INPUT_QUEUE);

		dataEventService = new MockDataEventService();
		indexer = new MockTiersIndexer();

		// on crée un mock de l'hibernate template qui ne fait rien
		final HibernateTemplate hibernateTemplate = new HibernateTemplate() {
			@Override
			public <T> T get(Class<T> entityClass, Serializable id) throws DataAccessException {
				return null;
			}

			@Override
			public Serializable save(Object entity) throws DataAccessException {
				return null;
			}

			@Override
			public void flush() throws DataAccessException {
			}
		};

		final EntrepriseEventListener listener = new EntrepriseEventListener();
		listener.setEsbTemplate(esbTemplate);
		listener.setDataEventService(dataEventService);
		listener.setIndexer(indexer);
		listener.setHibernateTemplate(hibernateTemplate);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[] {new ClassPathResource("xsd/pm/EvenementEntreprise.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveEvenementPM() throws Exception {

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/pm/pm_event.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (dataEventService.changedPMs.isEmpty()) {
			Thread.sleep(100);
		}

		// On vérifique que la PM est bien réindexée et que le service 'data event' est bien notifié du changement
		assertEquals(1, dataEventService.changedPMs.size());
		assertEquals(Long.valueOf(53220), dataEventService.changedPMs.get(0));
		assertEquals(1, indexer.scheduled.size());
		assertEquals(Long.valueOf(53220), indexer.scheduled.get(0));
	}
}

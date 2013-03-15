package ch.vd.uniregctb.evenement.cedi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.util.ResourceUtils;

import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Classe de test du listener d'événements CEDI. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementCediEsbMessageHandlerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementCediEsbMessageHandler handler;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtCedi");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);

		clearQueue(INPUT_QUEUE);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplateImpl() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		handler = new EvenementCediEsbMessageHandler();
		handler.setHibernateTemplate(hibernateTemplate);

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setHandler(handler);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[]{new ClassPathResource("xsd/cedi/DossierElectronique-1-0.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveRetourDI() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();

		handler.setHandler(new EvenementCediHandler() {
			@Override
			public void onEvent(EvenementCedi event, Map<String, String> incomingHeaders) {
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

		final List<EvenementCedi> events = new ArrayList<>();

		handler.setHandler(new EvenementCediHandler() {
			@Override
			public void onEvent(EvenementCedi event, Map<String, String> incomingHeaders) {
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

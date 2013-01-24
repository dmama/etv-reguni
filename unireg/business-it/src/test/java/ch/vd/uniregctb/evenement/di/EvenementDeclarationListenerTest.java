package ch.vd.uniregctb.evenement.di;

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
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements Declaration. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
public class EvenementDeclarationListenerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementDeclarationListenerImpl listener;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtDeclaration");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
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

		listener = new EvenementDeclarationListenerImpl();
		listener.setEsbTemplate(esbTemplate);
		listener.setHibernateTemplate(hibernateTemplate);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());
		esbValidator.setSources(new Resource[]{new ClassPathResource("unireg-common-1.xsd"),
				new ClassPathResource("/event/di/evenementDeclarationImpot-input-1.xsd"),
				new ClassPathResource("/event/di/evenementDeclarationImpot-common-1.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveQuittancementDI() throws Exception {

		final List<EvenementDeclaration> events = new ArrayList<EvenementDeclaration>();

		listener.setHandler(new EvenementDeclarationHandler() {
			@Override
			public void onEvent(EvenementDeclaration event, Map<String, String> incomingHeaders) {
				events.add(event);
			}

			@Override
			public ClassPathResource getRequestXSD() {
				return new ClassPathResource("/event/di/evenementDeclarationImpot-input-1.xsd");
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/di/quittancementStandard.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final QuittancementDI q = (QuittancementDI) events.get(0);
		assertNotNull(q);
		assertEquals(12344556L, q.getNumeroContribuable());
		assertEquals(2010, q.getPeriodeFiscale());
		assertEquals("ADDI", q.getSource());
		assertEquals(RegDate.get(2011, 5, 26), q.getDate());

	}

}

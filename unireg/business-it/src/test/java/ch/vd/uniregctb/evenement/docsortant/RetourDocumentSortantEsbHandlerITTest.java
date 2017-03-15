package ch.vd.uniregctb.evenement.docsortant;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.util.ResourceUtils;

import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.xml.event.docsortant.retour.v3.Quittance;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RetourDocumentSortantEsbHandlerITTest extends EvenementTest {

	private String INPUT_QUEUE;
	private RetourDocumentSortantEsbHandler handler;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.notification.document.sortant.reponse");

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

		handler = new RetourDocumentSortantEsbHandler();
		handler.setHibernateTemplate(hibernateTemplate);
		handler.afterPropertiesSet();

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setHandler(handler);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("/event/dperm/typeSimpleDPerm-1.xsd"),
				new ClassPathResource("/event/docsortant/quittanceRepElec-3.xsd")
		});

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReception() throws Exception {

		final List<Pair<Quittance, Map<String, String>>> collected = new LinkedList<>();
		handler.setHandler((quittance, headers) -> {
			synchronized (collected) {
				collected.add(Pair.of(quittance, headers));
				collected.notifyAll();
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/docsortant/retourDocumentSortant.xml");
		final String texte = FileUtils.readFileToString(file);

		// quelques attributs "custom"
		final Map<String, String> attributes = new HashMap<>();
		attributes.put("Devise", "Tralalatsointsoin");
		attributes.put("Finalité", "Aucune, vraiment");

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte, attributes);

		// On attend le message
		synchronized (collected) {
			while (collected.isEmpty()) {
				collected.wait(1000);
			}
		}
		Assert.assertEquals(1, collected.size());

		final Pair<Quittance, Map<String, String>> q = collected.get(0);
		assertNotNull(q);
		assertNotNull(q.getLeft());
		assertNotNull(q.getRight());
		assertEquals(2, q.getRight().size());
		assertEquals("Tralalatsointsoin", q.getRight().get("Devise"));
		assertEquals("Aucune, vraiment", q.getRight().get("Finalité"));
		assertNotNull(q.getLeft().getDocumentsQuittances().getDocumentQuittance());
		assertEquals(1, q.getLeft().getDocumentsQuittances().getDocumentQuittance().size());
		assertEquals("businessIdDossier201703081052149968963933859", q.getLeft().getDocumentsQuittances().getDocumentQuittance().get(0).getIdentifiantRepelecDossier());
	}
}

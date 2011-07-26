package ch.vd.uniregctb.evenement.addi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.evenement.cedi.EvenementCedi;
import ch.vd.uniregctb.evenement.cedi.EvenementCediHandler;
import ch.vd.uniregctb.evenement.cedi.EvenementCediListenerImpl;
import ch.vd.uniregctb.evenement.cedi.RetourDI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Classe de test du listener d'événements ADDI. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
public class EvenementAddiListenerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementAddiListenerImpl listener;
	private DefaultMessageListenerContainer container;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtAddi");

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

		listener = new EvenementAddiListenerImpl();
		listener.setEsbTemplate(esbTemplate);
		listener.setHibernateTemplate(hibernateTemplate);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[]{new ClassPathResource("unireg-common-1.xsd"),
				new ClassPathResource("/event/di/evenementDeclarationImpot-input-1.xsd"),
				new ClassPathResource("/event/di/evenementDeclarationImpot-common-1.xsd")});

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
	public void testReceiveQuittancementDI() throws Exception {

		final List<EvenementAddi> events = new ArrayList<EvenementAddi>();

		listener.setHandler(new EvenementAddiHandler() {
			@Override
			public void onEvent(EvenementAddi event) {
				events.add(event);
			}

			@Override
			public ClassPathResource getRequestXSD() {
				return new ClassPathResource("/event/di/evenementDeclarationImpot-input-1.xsd");
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/addi/quittancementStandard.xml");
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

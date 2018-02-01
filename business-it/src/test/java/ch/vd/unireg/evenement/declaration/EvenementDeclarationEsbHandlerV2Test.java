package ch.vd.unireg.evenement.declaration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.xml.event.declaration.ack.v2.DeclarationAck;
import ch.vd.unireg.xml.event.declaration.v2.DeclarationEvent;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.hibernate.HibernateTemplateImpl;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements Declaration. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
public class EvenementDeclarationEsbHandlerV2Test extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementDeclarationEsbHandlerV2 handler;

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
		esbTemplate.setSessionTransacted(true);

		clearQueue(INPUT_QUEUE);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplateImpl() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		handler = new EvenementDeclarationEsbHandlerV2();
		handler.setHibernateTemplate(hibernateTemplate);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("/event/declaration/declaration-event-2.xsd"),
				new ClassPathResource("/event/declaration/declaration-ack-2.xsd")
		});

		initListenerContainer(INPUT_QUEUE, handler);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveQuittancementDI() throws Exception {

		final List<DeclarationEvent> events = new ArrayList<>();

		// quittancement handler
		final EvenementDeclarationHandler<DeclarationAck> ackHandler = new EvenementDeclarationHandler<DeclarationAck>() {
			@Override
			public void handle(DeclarationAck event, Map<String, String> headers) throws EsbBusinessException {
				events.add(event);
			}

			@Override
			public ClassPathResource getXSD() {
				return new ClassPathResource("/event/declaration/declaration-ack-2.xsd");
			}
		};
		//noinspection unchecked
		final Map<Class<? extends DeclarationEvent>, EvenementDeclarationHandler<?>> handlers = Collections.singletonMap(DeclarationAck.class, ackHandler);
		handler.setHandlers(handlers);
		handler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/evenement/declaration/quittancement.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		assertEquals(1, events.size());

		final DeclarationAck q = (DeclarationAck) events.get(0);
		assertNotNull(q);
		assertEquals(12344556L, q.getDeclaration().getPartyNumber());
		assertEquals(2010, q.getDeclaration().getTaxPeriod());
		assertEquals("ADDI", q.getSource());
		assertEquals(RegDate.get(2011, 5, 26), DataHelper.xmlToCore(q.getDate()));
	}
}

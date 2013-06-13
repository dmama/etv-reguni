package ch.vd.uniregctb.evenement.externe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.validation.EsbXmlValidation;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements externes. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementExterneEsbHandlerItTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementExterneEsbHandler handler;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtExterne");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(INPUT_QUEUE);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplateImpl() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		handler = new EvenementExterneEsbHandler();
		handler.setHibernateTemplate(hibernateTemplate);

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setEsbTemplate(esbTemplate);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setHandler(handler);

		esbValidator = new EsbXmlValidation();
		esbValidator.setSources(new Resource[]{new ClassPathResource("event/lr/evtQuittanceListe-v1.xsd"), new ClassPathResource("event/lr/evtListe-1.xsd")});

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveOldQuittanceLR() throws Exception {

		final List<EvenementExterne> events = new ArrayList<>();

		handler.setHandler(new EvenementExterneHandler() {
			@Override
			public void onEvent(EvenementExterne event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/externe/old_quittance_lr.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final QuittanceLR q = (QuittanceLR) events.get(0);
		assertNotNull(q);
		Assert.assertEquals(1500001L, q.getTiersId().longValue());
		assertEquals(RegDate.get(2009, 12, 7), RegDateHelper.get(q.getDateEvenement()));
		assertEquals(RegDate.get(2008, 1, 1), q.getDateDebut());
		assertEquals(RegDate.get(2008, 1, 31), q.getDateFin());
		Assert.assertEquals(TypeQuittance.QUITTANCEMENT, q.getType());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveNewQuittanceLR() throws Exception {

		final List<EvenementExterne> events = new ArrayList<>();

		handler.setHandler(new EvenementExterneHandler() {
			@Override
			public void onEvent(EvenementExterne event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/externe/new_quittance_lr.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final QuittanceLR q = (QuittanceLR) events.get(0);
		assertNotNull(q);
		Assert.assertEquals(1500001L, q.getTiersId().longValue());
		assertEquals(RegDate.get(2009, 12, 7), RegDateHelper.get(q.getDateEvenement()));
		assertEquals(RegDate.get(2008, 1, 1), q.getDateDebut());
		assertEquals(RegDate.get(2008, 1, 31), q.getDateFin());
		Assert.assertEquals(TypeQuittance.QUITTANCEMENT, q.getType());
	}
}

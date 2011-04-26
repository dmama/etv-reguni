package ch.vd.uniregctb.evenement.externe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements externes. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementExterneListenerItTest extends EvenementTest {

	private static final String INPUT_QUEUE = "ch.vd.unireg.test.input";
	private static final String OUTPUT_QUEUE = "ch.vd.unireg.test.output";
	private EvenementExterneListenerImpl listener;
	private DefaultMessageListenerContainer container;

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
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(OUTPUT_QUEUE);
		clearQueue(INPUT_QUEUE);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplate() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		listener = new EvenementExterneListenerImpl();
		listener.setEsbTemplate(esbTemplate);
		listener.setHibernateTemplate(hibernateTemplate);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[] {new ClassPathResource("xsd/fiscal/evtQuittanceListe-v1.xsd")});

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
	public void testReceiveQuittanceLR() throws Exception {

		final List<EvenementExterne> events = new ArrayList<EvenementExterne>();

		listener.setHandler(new EvenementExterneHandler() {
			public void onEvent(EvenementExterne event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/externe/quittance_lr.xml");
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
		assertEquals(RegDate.get(2009,12,7), RegDate.get(q.getDateEvenement()));
		assertEquals(RegDate.get(2008,1,1), q.getDateDebut());
		assertEquals(RegDate.get(2008,1,31), q.getDateFin());
		Assert.assertEquals(TypeQuittance.QUITTANCEMENT, q.getType());
	}
}

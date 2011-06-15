package ch.vd.uniregctb.evenement.iam;

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

import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.type.ModeCommunication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements IAM. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Baba NGOM  <baba-issa.ngom@vd.ch>
 */
public class EvenementIAMListenerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementIAMListenerImpl listener;
	private DefaultMessageListenerContainer container;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtIAM");

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

		listener = new EvenementIAMListenerImpl();
		listener.setEsbTemplate(esbTemplate);
		listener.setHibernateTemplate(hibernateTemplate);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[]{new ClassPathResource("xsd/iam/messageIAM_EMPIS.xsd")});

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
		// si le setup saute avant la fin... container peut encore être null
		if (container != null) {
			container.destroy();
		}
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveEnregistrementEmployeur() throws Exception {

		final List<EvenementIAM> events = new ArrayList<EvenementIAM>();

		listener.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_employeur.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur);
		final InfoEmployeur q = enregistrementEmployeur.getEmployeursAMettreAJour().get(0);
		assertEquals(1038580L, q.getNoEmployeur());
		assertEquals(20, q.getLogicielId());
		assertEquals(ModeCommunication.SITE_WEB, q.getModeCommunication());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceive2EnregistrementEmployeur() throws Exception {

		final List<EvenementIAM> events = new ArrayList<EvenementIAM>();

		listener.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_2employeur.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur1 = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur1);
		final InfoEmployeur q1 = enregistrementEmployeur1.getEmployeursAMettreAJour().get(0);
		assertEquals(1038580L, q1.getNoEmployeur());
		assertEquals(20, q1.getLogicielId());
		assertEquals(ModeCommunication.SITE_WEB, q1.getModeCommunication());

		final InfoEmployeur q2 = enregistrementEmployeur1.getEmployeursAMettreAJour().get(1);
		assertEquals(1038640L, q2.getNoEmployeur());
		assertEquals(15, q2.getLogicielId());
		assertEquals(ModeCommunication.ELECTRONIQUE, q2.getModeCommunication());
	}


}

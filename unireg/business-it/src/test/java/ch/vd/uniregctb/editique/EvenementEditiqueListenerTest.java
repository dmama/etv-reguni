package ch.vd.uniregctb.editique;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.editique.impl.EvenementEditiqueListenerImpl;
import ch.vd.uniregctb.evenement.EvenementTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements des retours d'impression de l'éditique.
 * Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
@RunWith(JUnit4ClassRunner.class)
public class EvenementEditiqueListenerTest extends EvenementTest {

	private static final String INPUT_QUEUE = "ch.vd.unireg.test.input";
	private static final String OUTPUT_QUEUE = "ch.vd.unireg.test.output";
	private EvenementEditiqueListenerImpl listener;
	private DefaultMessageListenerContainer container;

	private static final String DI_ID = "2004 01 062901707 20100817145346417";

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

		clearQueue(OUTPUT_QUEUE);
		clearQueue(INPUT_QUEUE);

		listener = new EvenementEditiqueListenerImpl();
		listener.setEsbStore(esbStore);
		listener.setEsbTemplate(esbTemplate);
		if (listener instanceof InitializingBean) {
			((InitializingBean) listener).afterPropertiesSet();
		}

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(null);

		container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(jmsConnectionManager);
		container.setMessageListener(listener);
		container.setDestinationName(INPUT_QUEUE);
		container.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		container.destroy();
	}

	@Test
	public void testRetourImpression() throws Exception {

		final List<EditiqueResultat> events = new ArrayList<EditiqueResultat>();

		listener.setStorageService(new EditiqueRetourImpressionStorageService() {
			public void onArriveeRetourImpression(EditiqueResultat resultat) {
				events.add(resultat);
			}

			public EditiqueResultat getDocument(String nomDocument, long timeout) {
				throw new NotImplementedException();
			}

			public int getDocumentsRecus() {
				throw new NotImplementedException();
			}

			public int getDocumentsEnAttenteDeDispatch() {
				throw new NotImplementedException();
			}

			public int getCleanupPeriod() {
				throw new NotImplementedException();
			}

			public void setCleanupPeriod(int period) {
				throw new NotImplementedException();
			}

			public int getDocumentsPurges() {
				throw new NotImplementedException();
			}

			public Date getDateDernierePurgeEffective() {
				throw new NotImplementedException();
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/editique/retour_impression.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendDiMessage(INPUT_QUEUE, texte, DI_ID);

		// On attend le message jusqu'à 10 secondes
		for (int i = 0; events.isEmpty() && i < 100; i++) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EditiqueResultat q = events.get(0);
		assertNotNull(q);
		assertEquals(DI_ID, q.getIdDocument());
	}

	private void sendDiMessage(String queueName, String texte, String idDocument) throws Exception {
		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		m.addHeader(EditiqueHelper.DI_ID, idDocument);
		esbTemplate.send(m);
	}
}
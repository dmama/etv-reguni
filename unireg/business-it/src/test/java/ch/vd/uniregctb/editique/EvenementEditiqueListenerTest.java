package ch.vd.uniregctb.editique;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.utils.Pair;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.editique.impl.EvenementEditiqueListenerImpl;
import ch.vd.uniregctb.evenement.EvenementTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements des retours d'impression de l'éditique.
 * Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
public class EvenementEditiqueListenerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementEditiqueListenerImpl listener;

	private static final String DI_ID = "2004 01 062901707 20100817145346417";

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.editique.input");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");

		clearQueue(INPUT_QUEUE);

		listener = new EvenementEditiqueListenerImpl();
		listener.setEsbTemplate(esbTemplate);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		if (listener instanceof InitializingBean) {
			((InitializingBean) listener).afterPropertiesSet();
		}

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(null);

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRetourImpression() throws Exception {

		final List<EditiqueResultat> events = new ArrayList<EditiqueResultat>();

		listener.setStorageService(new EditiqueRetourImpressionStorageService() {
			@Override
			public void onArriveeRetourImpression(EditiqueResultatRecu resultat) {
				events.add(resultat);
			}

			@Override
			public void registerTrigger(String nomDocument, RetourImpressionTrigger trigger) {
				throw new NotImplementedException();
			}

			@Override
			public EditiqueResultat getDocument(String nomDocument, long timeout) {
				throw new NotImplementedException();
			}

			@Override
			public int getDocumentsRecus() {
				throw new NotImplementedException();
			}

			@Override
			public int getDocumentsEnAttenteDeDispatch() {
				throw new NotImplementedException();
			}

			@Override
			public int getCleanupPeriod() {
				throw new NotImplementedException();
			}

			@Override
			public void setCleanupPeriod(int period) {
				throw new NotImplementedException();
			}

			@Override
			public int getDocumentsPurges() {
				throw new NotImplementedException();
			}

			@Override
			public Date getDateDernierePurgeEffective() {
				throw new NotImplementedException();
			}

			@Override
			public Collection<Pair<Long,RetourImpressionTrigger>> getTriggersEnregistres() {
				throw new NotImplementedException();
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/editique/retour_impression.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendDiMessage(INPUT_QUEUE, texte, DI_ID);

		// On attend le message
		while (events.isEmpty()) {
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

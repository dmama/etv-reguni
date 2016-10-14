package ch.vd.uniregctb.evenement.registrefoncier;

import javax.activation.DataHandler;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.connection.JmsTransactionManager;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EvenementRFImmeubleEsbHandlerItTest extends EvenementTest {

	private EvenementRFImmeubleDAO evenementRFImmeubleDAO;

	private String INPUT_QUEUE;
	private EvenementRFImmeubleEsbHandler handler;

	private final MutableInt receivedCount = new MutableInt(0);

	@Before
	public void setup() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtRfImmeuble");

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

		evenementRFImmeubleDAO = new MockEvenementRFImmeubleDAO();

		handler = new EvenementRFImmeubleEsbHandler() {
			@Override
			public void onEsbMessage(EsbMessage message) throws Exception {
				try {
					super.onEsbMessage(message);
				}
				finally {
					// on notifie ceux qui écoutent que le traitement est terminé
					synchronized (receivedCount) {
						receivedCount.increment();
						receivedCount.notifyAll();
					}
				}
			}
		};
		handler.setEvenementRFImmeubleDAO(evenementRFImmeubleDAO);

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);
		listener.setHandler(handler);

		initEndpointManager(INPUT_QUEUE, listener);
	}

	private void sendRfMessage(String queueName, final String raftUrl) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.addAttachment("data", new EsbDataHandler() {
			@Override
			public DataHandler getDataHandler() {
				throw new NotImplementedException();
			}

			@Override
			public String getRef() {
				return raftUrl;
			}
		});
		esbTemplate.send(m);
	}

	/**
	 * Ce test vérifie que la réception d'un événement JMS fonctionne bien et que les données correspondantes sont insérées dans la DB (cas passant).
	 */
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenement() throws Exception {

		// précondition : il n'y a pas d'événement en base
		assertEquals(0, evenementRFImmeubleDAO.getCount(EvenementRFImmeuble.class));

		// on envoie l'événement
		sendRfMessage(INPUT_QUEUE, "http://example.com/turlututu");

		// on attend que le traitement du message soit terminé
		synchronized (receivedCount) {
			while (receivedCount.intValue() == 0) {
				receivedCount.wait();
			}
		}

		// postcondition : l'événemnt correspondant doit exister dans la base
		final List<EvenementRFImmeuble> list = evenementRFImmeubleDAO.getAll();
		assertEquals(1, list.size());

		final EvenementRFImmeuble event = list.get(0);
		assertNotNull(event);
		assertEquals(EtatEvenementRF.A_TRAITER, event.getEtat());
		assertEquals("http://example.com/turlututu", event.getFileUrl());
	}
}
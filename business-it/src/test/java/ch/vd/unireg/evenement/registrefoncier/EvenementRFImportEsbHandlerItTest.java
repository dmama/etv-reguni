package ch.vd.unireg.evenement.registrefoncier;

import javax.activation.DataHandler;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.registrefoncier.MockRegistreFoncierImportService;
import ch.vd.unireg.transaction.MockTxSyncManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EvenementRFImportEsbHandlerItTest extends EvenementTest {

	private EvenementRFImportDAO evenementRFImportDAO;

	private String INPUT_QUEUE;
	private EvenementRFImportEsbHandler handler;

	private final MutableInt receivedCount = new MutableInt(0);
	private MockRegistreFoncierImportService serviceImportRF;

	@Before
	public void setup() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtRfImport");

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

		evenementRFImportDAO = new MockEvenementRFImportDAO();
		serviceImportRF = new MockRegistreFoncierImportService();

		handler = new EvenementRFImportEsbHandler() {
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
		handler.setEvenementRFImportDAO(evenementRFImportDAO);
		handler.setTxSyncManager(new MockTxSyncManager());
		handler.setServiceImportRF(serviceImportRF);

		initListenerContainer(INPUT_QUEUE, handler);
	}

	private void sendRfMessage(String queueName, String raftUrl, String dateValeur, @Nullable String typeImport) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.addAttachment("data", new EsbDataHandler() {
			@Override
			public DataHandler getDataHandler() {
				throw new NotImplementedException("");
			}

			@Override
			public String getRef() {
				return raftUrl;
			}
		});
		m.addHeader("dateValeur", dateValeur);
		m.addHeader("typeImport", typeImport);
		esbTemplate.send(m);
	}

	/**
	 * Ce test vérifie que la réception d'un événement JMS fonctionne bien, que le type est bien détecté que les données correspondantes sont insérées dans la DB (cas passant) et que le batch de traitement est bien déclenché.
	 */
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenementImportPrincipal() throws Exception {

		// précondition : il n'y a pas d'événement en base
		assertEquals(0, evenementRFImportDAO.getCount(EvenementRFImport.class));

		// on envoie l'événement
		sendRfMessage(INPUT_QUEUE, "http://example.com/turlututu", "20161001", "PRINCIPAL");

		// on attend que le traitement du message soit terminé
		synchronized (receivedCount) {
			while (receivedCount.intValue() == 0) {
				receivedCount.wait();
			}
		}

		// postcondition : l'événemnt correspondant doit exister dans la base
		final List<EvenementRFImport> list = evenementRFImportDAO.getAll();
		assertEquals(1, list.size());

		final EvenementRFImport event = list.get(0);
		assertNotNull(event);
		assertEquals(TypeImportRF.PRINCIPAL, event.getType());
		assertEquals(EtatEvenementRF.A_TRAITER, event.getEtat());
		assertEquals(RegDate.get(2016, 10, 1), event.getDateEvenement());
		assertEquals("http://example.com/turlututu", event.getFileUrl());

		// le batch doit être démarré
		final List<Long> started = serviceImportRF.getStartedImports();
		assertEquals(1, started.size());
		assertEquals(event.getId(), started.get(0));
	}

	/**
	 * Ce test vérifie que la réception d'un événement JMS fonctionne bien, que le type est bien détecté, que les données correspondantes sont insérées dans la DB (cas passant) et que le batch de traitement est bien déclenché.
	 */
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenementImportServitudes() throws Exception {

		// précondition : il n'y a pas d'événement en base
		assertEquals(0, evenementRFImportDAO.getCount(EvenementRFImport.class));

		// on envoie l'événement
		sendRfMessage(INPUT_QUEUE, "http://example.com/turlututu", "20161001", "SERVITUDES");

		// on attend que le traitement du message soit terminé
		synchronized (receivedCount) {
			while (receivedCount.intValue() == 0) {
				receivedCount.wait();
			}
		}

		// postcondition : l'événemnt correspondant doit exister dans la base
		final List<EvenementRFImport> list = evenementRFImportDAO.getAll();
		assertEquals(1, list.size());

		final EvenementRFImport event = list.get(0);
		assertNotNull(event);
		assertEquals(TypeImportRF.SERVITUDES, event.getType());
		assertEquals(EtatEvenementRF.A_TRAITER, event.getEtat());
		assertEquals(RegDate.get(2016, 10, 1), event.getDateEvenement());
		assertEquals("http://example.com/turlututu", event.getFileUrl());

		// le batch doit être démarré
		final List<Long> started = serviceImportRF.getStartedImports();
		assertEquals(1, started.size());
		assertEquals(event.getId(), started.get(0));
	}

	/**
	 * Ce test vérifie que la réception d'un événement JMS fonctionne bien et que le type d'import est bien détecté (défaut=PRINCIPAL) s'il n'est pas explicitement spécifié.
	 */
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenementTypeImportParDefaut() throws Exception {

		// précondition : il n'y a pas d'événement en base
		assertEquals(0, evenementRFImportDAO.getCount(EvenementRFImport.class));

		// on envoie l'événement
		sendRfMessage(INPUT_QUEUE, "http://example.com/turlututu", "20161001", null);

		// on attend que le traitement du message soit terminé
		synchronized (receivedCount) {
			while (receivedCount.intValue() == 0) {
				receivedCount.wait();
			}
		}

		// postcondition : l'événemnt correspondant doit exister dans la base
		final List<EvenementRFImport> list = evenementRFImportDAO.getAll();
		assertEquals(1, list.size());

		final EvenementRFImport event = list.get(0);
		assertNotNull(event);
		assertEquals(TypeImportRF.PRINCIPAL, event.getType());
		assertEquals(EtatEvenementRF.A_TRAITER, event.getEtat());
		assertEquals(RegDate.get(2016, 10, 1), event.getDateEvenement());
		assertEquals("http://example.com/turlututu", event.getFileUrl());

		// le batch doit être démarré
		final List<Long> started = serviceImportRF.getStartedImports();
		assertEquals(1, started.size());
		assertEquals(event.getId(), started.get(0));
	}
}
package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.connection.JmsTransactionManager;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;
import ch.vd.uniregctb.registrefoncier.RapprochementManuelTiersRFService;

public class EvenementIdentificationRapprochementProprietaireEsbHandlerItTest extends EvenementTest {

	private String INPUT_QUEUE;
	private final List<DonneeRecue> donneesRecues = new LinkedList<>();

	private static class DonneeRecue {
		final long idContribuable;
		final long idTiersRF;
		final String principal;

		public DonneeRecue(long idContribuable, long idTiersRF, String principal) {
			this.idContribuable = idContribuable;
			this.idTiersRF = idTiersRF;
			this.principal = principal;
		}
	}

	@Before
	public void setup() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtRapprochementTiersRF");

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

		donneesRecues.clear();
		final HibernateTemplate hibernateTemplate = new HibernateTemplateImpl() {
			@Override
			public void flush() {
				// c'est la seule méthode appelée ici... on ne fait rien (l'accès à la DB n'est pas configuré dans ce test
			}
		};
		final RapprochementProprietaireHandler handler = (idContribuable, idTiersRF) -> {
			synchronized (donneesRecues) {
				donneesRecues.add(new DonneeRecue(idContribuable, idTiersRF, AuthenticationHelper.getCurrentPrincipal()));
				donneesRecues.notifyAll();
			}
		};
		final EvenementIdentificationRapprochementProprietaireEsbHandler esbHandler = new EvenementIdentificationRapprochementProprietaireEsbHandler();
		esbHandler.setHibernateTemplate(hibernateTemplate);
		esbHandler.setHandler(handler);

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);
		listener.setHandler(esbHandler);

		initEndpointManager(INPUT_QUEUE, listener);
	}

	private void sendIdentificationReponse(String queueName, String businessUser, long noContribuable, @Nullable Map<String, String> headers) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser(businessUser);
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		if (headers != null) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				m.addHeader(entry.getKey(), entry.getValue());
			}
		}
		m.setBody(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				                        "<iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.7\">\n" +
				                        "  <iden:reponse>\n" +
				                        "    <iden:date>2016-11-11T09:22:04.845+01:00</iden:date>\n" +
				                        "    <iden:contribuable>\n" +
				                        "      <iden:numeroContribuableIndividuel>%d</iden:numeroContribuableIndividuel>\n" +
				                        "    </iden:contribuable>\n" +
				                        "  </iden:reponse>\n" +
				                        "</iden:identificationCTB>", noContribuable));
		esbTemplate.send(m);
	}

	private void waitForDonnees() throws InterruptedException {
		synchronized (donneesRecues) {
			while (donneesRecues.isEmpty()) {
				donneesRecues.wait();
			}
		}
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testEnvoiFonctionnel() throws Exception {

		Assert.assertEquals(0, donneesRecues.size());
		sendIdentificationReponse(INPUT_QUEUE, "toubidou", 425424L, Collections.singletonMap(RapprochementManuelTiersRFService.ID_TIERS_RF, "2357867"));
		waitForDonnees();

		Assert.assertEquals(1, donneesRecues.size());
		{
			final DonneeRecue donneeRecue = donneesRecues.get(0);
			Assert.assertNotNull(donneeRecue);
			Assert.assertEquals(425424L, donneeRecue.idContribuable);
			Assert.assertEquals(2357867L, donneeRecue.idTiersRF);
			Assert.assertEquals("toubidou", donneeRecue.principal);
		}
	}
}

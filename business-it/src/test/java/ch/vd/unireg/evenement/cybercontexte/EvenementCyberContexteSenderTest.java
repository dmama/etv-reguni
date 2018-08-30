package ch.vd.unireg.evenement.cybercontexte;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EvenementTest;

public class EvenementCyberContexteSenderTest extends EvenementTest {

	private String OUTPUT_QUEUE;

	private EvenementCyberContexteSenderImpl sender;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtCyberContexte.output");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);

		clearQueue(OUTPUT_QUEUE);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("event/di/evtPublicationContextePrestationCyber-1.xsd")
		});

		sender = new EvenementCyberContexteSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		sender.setServiceDestination(OUTPUT_QUEUE);
		sender.setEnabled(true);
		sender.afterPropertiesSet();

		AuthenticationHelper.pushPrincipal("EvenementTest");
	}

	@Override
	public void tearDown() {
		super.tearDown();
		AuthenticationHelper.popPrincipal();
	}

	@Test
	public void testSendEmissionDeclarationEvent() throws Exception {
		sender.sendEmissionDeclarationEvent(4215L, 2016, 1, "2X3ff%", RegDate.get(2017, 1, 15));

		final String message =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<ev-di-cyber-cp-1:evtPublicationContextePrestationCyber xmlns:ev-di-cyber-cp-1=\"http://www.vd.ch/fiscalite/cyber/contexte-prestation/1\">" +
						"<ev-di-cyber-cp-1:prestationCode>e-delai</ev-di-cyber-cp-1:prestationCode>" +
						"<ev-di-cyber-cp-1:authentificationHashKey>497bcb2d92ed9efa4904791bc9f9f894b5a8df21dd1034350e5d6b6a2973407c87fe2c871ef48e9644418d48a2a20647f4c5785c82d98219c7d257439fe56f3c85</ev-di-cyber-cp-1:authentificationHashKey>" +
						"<ev-di-cyber-cp-1:identificationHashKey>4a71b446b587ee4c9a97a39905a51ad36daa063b7354dc3413a2303a7ddb322f6d6a7681ee5156e5bba3fc3e3dbd3f9098c2d38581d5468d722ec09402efd35d15</ev-di-cyber-cp-1:identificationHashKey>" +
						"<ev-di-cyber-cp-1:emissionDate>2017-01-15T00:00:00.000</ev-di-cyber-cp-1:emissionDate>" +
						"<ev-di-cyber-cp-1:source>UNIREG</ev-di-cyber-cp-1:source>" +
						"<ev-di-cyber-cp-1:statut>ACTIF</ev-di-cyber-cp-1:statut>" +
						"<ev-di-cyber-cp-1:documentId>2016-4215-1</ev-di-cyber-cp-1:documentId>" +
						"<ev-di-cyber-cp-1:documentType>DELAI-DI</ev-di-cyber-cp-1:documentType>" +
						"<ev-di-cyber-cp-1:periodeFiscale>2016</ev-di-cyber-cp-1:periodeFiscale>" +
						"<ev-di-cyber-cp-1:numeroContribuable>4215</ev-di-cyber-cp-1:numeroContribuable>" +
						"</ev-di-cyber-cp-1:evtPublicationContextePrestationCyber>";

		assertTextMessage(OUTPUT_QUEUE, message);
	}

	@Test
	public void testSendAnnulationDeclarationEvent() throws Exception {
		sender.sendAnnulationDeclarationEvent(4215L, 2016, 1, "2X3ff%", RegDate.get(2017, 1, 15));

		final String message =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<ev-di-cyber-cp-1:evtPublicationContextePrestationCyber xmlns:ev-di-cyber-cp-1=\"http://www.vd.ch/fiscalite/cyber/contexte-prestation/1\">" +
						"<ev-di-cyber-cp-1:prestationCode>e-delai</ev-di-cyber-cp-1:prestationCode>" +
						"<ev-di-cyber-cp-1:authentificationHashKey>497bcb2d92ed9efa4904791bc9f9f894b5a8df21dd1034350e5d6b6a2973407c87fe2c871ef48e9644418d48a2a20647f4c5785c82d98219c7d257439fe56f3c85</ev-di-cyber-cp-1:authentificationHashKey>" +
						"<ev-di-cyber-cp-1:identificationHashKey>4a71b446b587ee4c9a97a39905a51ad36daa063b7354dc3413a2303a7ddb322f6d6a7681ee5156e5bba3fc3e3dbd3f9098c2d38581d5468d722ec09402efd35d15</ev-di-cyber-cp-1:identificationHashKey>" +
						"<ev-di-cyber-cp-1:emissionDate>2017-01-15T00:00:00.000</ev-di-cyber-cp-1:emissionDate>" +
						"<ev-di-cyber-cp-1:source>UNIREG</ev-di-cyber-cp-1:source>" +
						"<ev-di-cyber-cp-1:statut>INACTIF</ev-di-cyber-cp-1:statut>" +
						"<ev-di-cyber-cp-1:documentId>2016-4215-1</ev-di-cyber-cp-1:documentId>" +
						"<ev-di-cyber-cp-1:documentType>DELAI-DI</ev-di-cyber-cp-1:documentType>" +
						"<ev-di-cyber-cp-1:periodeFiscale>2016</ev-di-cyber-cp-1:periodeFiscale>" +
						"<ev-di-cyber-cp-1:numeroContribuable>4215</ev-di-cyber-cp-1:numeroContribuable>" +
						"</ev-di-cyber-cp-1:evtPublicationContextePrestationCyber>";

		assertTextMessage(OUTPUT_QUEUE, message);
	}

}
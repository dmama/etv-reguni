package ch.vd.unireg.evenement.di;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;

public class EvenementLiberationDeclarationImpotSenderTest extends EvenementTest {

	private String OUTPUT_QUEUE;

	private EvenementLiberationDeclarationImpotSenderImpl sender;

	@Before
	public void setUp() throws Exception {

		OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtDeclaration");

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
				new ClassPathResource("unireg-common-2.xsd"),
				new ClassPathResource("event/di/evtLiberationDeclarationImpot-1.xsd")
		});

		sender = new EvenementLiberationDeclarationImpotSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		sender.setServiceDestination(OUTPUT_QUEUE);
		sender.afterPropertiesSet();

		AuthenticationHelper.pushPrincipal("EvenementTest");
	}

	@Override
	public void tearDown() {
		super.tearDown();
		AuthenticationHelper.popPrincipal();
	}

	@Test
	public void testSendValidPMMessage() throws Exception {
		sender.demandeLiberationDeclarationImpot(7154L, 2015, 42, EvenementLiberationDeclarationImpotSender.TypeDeclarationLiberee.DI_PM);
		assertTextMessage(OUTPUT_QUEUE,
		                  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				                  "<ev-di-lib-1:demandeLiberation " +
				                  "xmlns:ev-di-lib-1=\"http://www.vd.ch/fiscalite/unireg/event/di/liberation/1\">" +
				                  "<ev-di-lib-1:numeroContribuable>7154</ev-di-lib-1:numeroContribuable>" +
				                  "<ev-di-lib-1:periodeFiscale>2015</ev-di-lib-1:periodeFiscale>" +
				                  "<ev-di-lib-1:numeroSequence>42</ev-di-lib-1:numeroSequence>" +
				                  "<ev-di-lib-1:typeDeclarationImpot>PM</ev-di-lib-1:typeDeclarationImpot>" +
				                  "</ev-di-lib-1:demandeLiberation>");
	}

	@Test
	public void testSendValidPPMessage() throws Exception {
		sender.demandeLiberationDeclarationImpot(45117465L, 2014, 26, EvenementLiberationDeclarationImpotSender.TypeDeclarationLiberee.DI_PP);
		assertTextMessage(OUTPUT_QUEUE,
		                  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				                  "<ev-di-lib-1:demandeLiberation " +
				                  "xmlns:ev-di-lib-1=\"http://www.vd.ch/fiscalite/unireg/event/di/liberation/1\">" +
				                  "<ev-di-lib-1:numeroContribuable>45117465</ev-di-lib-1:numeroContribuable>" +
				                  "<ev-di-lib-1:periodeFiscale>2014</ev-di-lib-1:periodeFiscale>" +
				                  "<ev-di-lib-1:numeroSequence>26</ev-di-lib-1:numeroSequence>" +
				                  "<ev-di-lib-1:typeDeclarationImpot>PP</ev-di-lib-1:typeDeclarationImpot>" +
				                  "</ev-di-lib-1:demandeLiberation>");
	}

	@Test
	public void testSendInvalidMessage() throws Exception {
		try {
			sender.demandeLiberationDeclarationImpot(45117465L, 0, 42, EvenementLiberationDeclarationImpotSender.TypeDeclarationLiberee.DI_PM);
			Assert.fail();
		}
		catch (EvenementDeclarationException e) {
			Assert.assertEquals("ch.vd.technical.esb.util.exception.ESBValidationException: org.xml.sax.SAXParseException; cvc-minInclusive-valid: Value '0' is not facet-valid with respect to minInclusive '1800' for type 'yearType'.", e.getMessage());
		}
	}
}

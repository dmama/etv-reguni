package ch.vd.uniregctb.evenement.di;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.evenement.EvenementTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Vérifie le fonctionnement de l'émetteur d'événement de déclarations à destination d'ADDI.
 */
public class EvenementDeclarationSenderTest extends EvenementTest {

	private String OUTPUT_QUEUE;

	private EvenementDeclarationSenderImpl sender;

	@Before
	public void setUp() throws Exception {

		OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.pm.event.input");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionManager);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");

		clearQueue(OUTPUT_QUEUE);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[]{new ClassPathResource("di/evenementDeclarationImpot_1.0.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		sender = new EvenementDeclarationSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbMessageFactory(esbMessageFactory);
		sender.setBusinessUser("EvenementTest");
		sender.setServiceDestination(OUTPUT_QUEUE);
	}

	@Test
	public void testSendEvenementEmissionDeclaration() throws Exception {
		sender.sendEmissionEvent(12344556L, 2000, RegDate.get(2000, 1, 1), "2X3ff%", "A14");

		assertTextMessage(OUTPUT_QUEUE,
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<evenement xmlns=\"http://www.vd.ch/fiscalite/registre/evenementDeclarationImpot/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"evenementEmissionDeclarationImpotType\">" +
						"<periodeFiscale>2000</periodeFiscale>" +
						"<numeroContribuable>12344556</numeroContribuable>" +
						"<date><year>2000</year><month>1</month><day>1</day></date>" +
						"<codeControle>2X3ff%</codeControle>" +
						"<codeRoutage>A14</codeRoutage>" +
						"</evenement>");
	}

	@Test
	public void testSendEvenementAnnulationDeclaration() throws Exception {
		sender.sendAnnulationEvent(12344556L, 2000, RegDate.get(2000, 1, 1));

		assertTextMessage(OUTPUT_QUEUE,
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><evenement xmlns=\"http://www.vd.ch/fiscalite/registre/evenementDeclarationImpot/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"evenementAnnulationDeclarationImpotType\">" +
						"<periodeFiscale>2000</periodeFiscale>" +
						"<numeroContribuable>12344556</numeroContribuable>" +
						"<date><year>2000</year><month>1</month><day>1</day></date>" +
						"</evenement>");
	}

	@Test
	public void testSendEvenementEmissionDeclarationInvalide() throws Exception {
		try {
			sender.sendEmissionEvent(1000000000L, 2000, RegDate.get(2000, 1, 1), "2X3ff%", "R13");
			fail();
		}
		catch (EvenementDeclarationException e) {
			assertEquals("ch.vd.technical.esb.util.exception.ESBValidationException: org.xml.sax.SAXParseException: " +
					"cvc-maxInclusive-valid: Value '1000000000' is not facet-valid with respect to maxInclusive '99999999' for type 'numeroContribuableType'.", e.getMessage());
		}
	}

	@Test
	public void testSendEvenementAnnulationDeclarationInvalide() throws Exception {
		try {
			sender.sendAnnulationEvent(1000000000L, 2000, RegDate.get(2000, 1, 1));
			fail();
		}
		catch (EvenementDeclarationException e) {
			assertEquals("ch.vd.technical.esb.util.exception.ESBValidationException: org.xml.sax.SAXParseException: " +
					"cvc-maxInclusive-valid: Value '1000000000' is not facet-valid with respect to maxInclusive '99999999' for type 'numeroContribuableType'.", e.getMessage());
		}
	}
}

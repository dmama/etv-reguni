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
		esbValidator.setSources(new Resource[]{new ClassPathResource("di/evenementDeclarationImpot_common_1.0.xsd"), new ClassPathResource("di/evenementDeclarationImpot_unireg2addi_1.0.xsd")});

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
						"<ns3:evenement " +
						"xmlns:ns3=\"http://www.vd.ch/fiscalite/registre/evenementDeclarationImpot/unireg2addi/1\" " +
						"xmlns=\"http://www.vd.ch/fiscalite/registre/evenementDeclarationImpot/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
						"xsi:type=\"ns3:evenementEmissionDeclarationImpotType\">" +
						"<periodeFiscale>2000</periodeFiscale>" +
						"<numeroContribuable>12344556</numeroContribuable>" +
						"<date><ns2:year>2000</ns2:year><ns2:month>1</ns2:month><ns2:day>1</ns2:day></date>" +
						"<ns3:codeControle>2X3ff%</ns3:codeControle>" +
						"<ns3:codeRoutage>A14</ns3:codeRoutage>" +
						"</ns3:evenement>");
	}

	@Test
	public void testSendEvenementAnnulationDeclaration() throws Exception {
		sender.sendAnnulationEvent(12344556L, 2000, RegDate.get(2000, 1, 1));

		assertTextMessage(OUTPUT_QUEUE,
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<ns3:evenement " +
						"xmlns:ns3=\"http://www.vd.ch/fiscalite/registre/evenementDeclarationImpot/unireg2addi/1\" " +
						"xmlns=\"http://www.vd.ch/fiscalite/registre/evenementDeclarationImpot/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
						"xsi:type=\"ns3:evenementAnnulationDeclarationImpotType\">" +
						"<periodeFiscale>2000</periodeFiscale>" +
						"<numeroContribuable>12344556</numeroContribuable>" +
						"<date><ns2:year>2000</ns2:year><ns2:month>1</ns2:month><ns2:day>1</ns2:day></date>" +
						"</ns3:evenement>");
	}

	@Test
	public void testSendEvenementEmissionDeclarationInvalide() throws Exception {
		try {
			sender.sendEmissionEvent(1000000000L, 2000, RegDate.get(2000, 1, 1), "2X3ff%", "R13");
			fail();
		}
		catch (EvenementDeclarationException e) {
			assertEquals("ch.vd.technical.esb.util.exception.ESBValidationException: org.xml.sax.SAXParseException: " +
					"cvc-maxInclusive-valid: Value '1000000000' is not facet-valid with respect to maxInclusive '99999999' for type 'partyNumberType'.", e.getMessage());
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
					"cvc-maxInclusive-valid: Value '1000000000' is not facet-valid with respect to maxInclusive '99999999' for type 'partyNumberType'.", e.getMessage());
		}
	}
}

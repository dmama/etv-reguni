package ch.vd.unireg.evenement.di;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationPMSenderImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Vérifie le fonctionnement de l'émetteur d'événement de déclarations PM à destination du SI fiscal
 */
public class EvenementDeclarationPMSenderTest extends EvenementTest {

	private String OUTPUT_QUEUE_DI;
	private String OUTPUT_QUEUE_DD;

	private EvenementDeclarationPMSenderImpl sender;

	@Before
	public void setUp() throws Exception {

		OUTPUT_QUEUE_DI = uniregProperties.getProperty("testprop.jms.queue.evtDeclaration.di");
		OUTPUT_QUEUE_DD = uniregProperties.getProperty("testprop.jms.queue.evtDeclaration.dd");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);

		clearQueue(OUTPUT_QUEUE_DI);
		clearQueue(OUTPUT_QUEUE_DD);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("event/di/evtPublicationCodeControleCyber-2.xsd")
		});

		sender = new EvenementDeclarationPMSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		sender.setServiceDestinationDI(OUTPUT_QUEUE_DI);
		sender.setServiceDestinationDD(OUTPUT_QUEUE_DD);
		sender.afterPropertiesSet();

		AuthenticationHelper.pushPrincipal("EvenementTest");
	}

	@Override
	public void tearDown() {
		super.tearDown();
		AuthenticationHelper.popPrincipal();
	}

	@Test
	public void testSendEvenementEmissionDeclaration() throws Exception {
		sender.sendEmissionDIEvent(4215L, 2016, 1, "2X3ff%", "8");

		// La partie "HORODATAGE" va être remplacée par une regexp...
		final String expectedAvantHorodatage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<ev-di-cyber-cc-2:evtPublicationCodeControleCyber xmlns:ev-di-cyber-cc-2=\"http://www.vd.ch/fiscalite/cyber/codeControle/2\">" +
				"<ev-di-cyber-cc-2:horodatagePublication>";
		final String expectedApresHorodatage = "</ev-di-cyber-cc-2:horodatagePublication>" +
				"<ev-di-cyber-cc-2:applicationEmettrice>UNIREG</ev-di-cyber-cc-2:applicationEmettrice>" +
				"<ev-di-cyber-cc-2:statut>ACTIF</ev-di-cyber-cc-2:statut>" +
				"<ev-di-cyber-cc-2:typeDocument>DI-PM</ev-di-cyber-cc-2:typeDocument>" +
				"<ev-di-cyber-cc-2:periodeFiscale>2016</ev-di-cyber-cc-2:periodeFiscale>" +
				"<ev-di-cyber-cc-2:numeroContribuable>4215</ev-di-cyber-cc-2:numeroContribuable>" +
				"<ev-di-cyber-cc-2:codeControle>2X3ff%</ev-di-cyber-cc-2:codeControle>" +
				"<ev-di-cyber-cc-2:numeroSequence>1</ev-di-cyber-cc-2:numeroSequence>" +
				"<ev-di-cyber-cc-2:informationsComplementaires><ev-di-cyber-cc-2:informationComplementaire><ev-di-cyber-cc-2:attribut>CODE_ROUTAGE</ev-di-cyber-cc-2:attribut><ev-di-cyber-cc-2:valeur>8</ev-di-cyber-cc-2:valeur></ev-di-cyber-cc-2:informationComplementaire></ev-di-cyber-cc-2:informationsComplementaires>" +
				"</ev-di-cyber-cc-2:evtPublicationCodeControleCyber>";

		final Pattern pattern = Pattern.compile(Pattern.quote(expectedAvantHorodatage) + "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{1,3}[+-][0-9]{2}:[0-9]{2}" + Pattern.quote(expectedApresHorodatage));
		assertTextMessage(OUTPUT_QUEUE_DI, pattern);
	}

	@Test
	public void testSendEvenementAnnulationDeclaration() throws Exception {
		sender.sendAnnulationDIEvent(12344556L, 2000, 1, "5635sS", "5");

		final String expectedAvantHorodatage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<ev-di-cyber-cc-2:evtPublicationCodeControleCyber xmlns:ev-di-cyber-cc-2=\"http://www.vd.ch/fiscalite/cyber/codeControle/2\">" +
				"<ev-di-cyber-cc-2:horodatagePublication>";
		final String expectedApresHorodatage = "</ev-di-cyber-cc-2:horodatagePublication>" +
				"<ev-di-cyber-cc-2:applicationEmettrice>UNIREG</ev-di-cyber-cc-2:applicationEmettrice>" +
				"<ev-di-cyber-cc-2:statut>INACTIF</ev-di-cyber-cc-2:statut>" +
				"<ev-di-cyber-cc-2:typeDocument>DI-PM</ev-di-cyber-cc-2:typeDocument>" +
				"<ev-di-cyber-cc-2:periodeFiscale>2000</ev-di-cyber-cc-2:periodeFiscale>" +
				"<ev-di-cyber-cc-2:numeroContribuable>12344556</ev-di-cyber-cc-2:numeroContribuable>" +
				"<ev-di-cyber-cc-2:codeControle>5635sS</ev-di-cyber-cc-2:codeControle>" +
				"<ev-di-cyber-cc-2:numeroSequence>1</ev-di-cyber-cc-2:numeroSequence>" +
				"<ev-di-cyber-cc-2:informationsComplementaires><ev-di-cyber-cc-2:informationComplementaire><ev-di-cyber-cc-2:attribut>CODE_ROUTAGE</ev-di-cyber-cc-2:attribut><ev-di-cyber-cc-2:valeur>5</ev-di-cyber-cc-2:valeur></ev-di-cyber-cc-2:informationComplementaire></ev-di-cyber-cc-2:informationsComplementaires>" +
				"</ev-di-cyber-cc-2:evtPublicationCodeControleCyber>";

		final Pattern pattern = Pattern.compile(Pattern.quote(expectedAvantHorodatage) + "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{1,3}[+-][0-9]{2}:[0-9]{2}" + Pattern.quote(expectedApresHorodatage));
		assertTextMessage(OUTPUT_QUEUE_DI, pattern);
	}

	@Test
	public void testSendEvenementEmissionDeclarationInvalide() throws Exception {
		try {
			sender.sendEmissionDIEvent(1000000000L, 2000, 1, "2X3ff%", "R13");
			fail();
		}
		catch (EvenementDeclarationException e) {
			assertEquals("ch.vd.technical.esb.util.exception.ESBValidationException: org.xml.sax.SAXParseException; " +
					"cvc-maxInclusive-valid: Value '1000000000' is not facet-valid with respect to maxInclusive '99999999' for type 'numeroContribuableType'.", e.getMessage());
		}
	}

	@Test
	public void testSendEvenementAnnulationDeclarationInvalide() throws Exception {
		try {
			sender.sendAnnulationDIEvent(1000000000L, 2000, 1, "2143d2", "5");
			fail();
		}
		catch (EvenementDeclarationException e) {
			assertEquals("ch.vd.technical.esb.util.exception.ESBValidationException: org.xml.sax.SAXParseException; " +
					"cvc-maxInclusive-valid: Value '1000000000' is not facet-valid with respect to maxInclusive '99999999' for type 'numeroContribuableType'.", e.getMessage());
		}
	}
}

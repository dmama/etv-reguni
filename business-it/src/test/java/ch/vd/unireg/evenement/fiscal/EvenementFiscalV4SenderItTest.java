package ch.vd.unireg.evenement.fiscal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.PersonnePhysique;

public class EvenementFiscalV4SenderItTest extends EvenementTest {

	private final static Long NUMERO_CONTRIBUABLE = 12300652L;

	private String OUTPUT_QUEUE;
	private EvenementFiscalV4SenderImpl sender;

	@Before
	public void setUp() throws Exception {

		OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtFiscal");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(OUTPUT_QUEUE);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("eCH-0007-4-0.xsd"),
				new ClassPathResource("eCH-0010-5-0.xsd"),
				new ClassPathResource("eCH-0044-3-0.xsd"),
				new ClassPathResource("unireg-common-2.xsd"),
				new ClassPathResource("unireg-exception-1.xsd"),
				new ClassPathResource("party/unireg-party-address-3.xsd"),
				new ClassPathResource("party/unireg-party-agent-1.xsd"),
				new ClassPathResource("party/unireg-party-relation-4.xsd"),
				new ClassPathResource("party/unireg-party-withholding-1.xsd"),
				new ClassPathResource("party/unireg-party-taxdeclaration-5.xsd"),
				new ClassPathResource("party/unireg-party-taxresidence-4.xsd"),
				new ClassPathResource("party/unireg-party-immovableproperty-2.xsd"),
				new ClassPathResource("party/unireg-party-ebilling-1.xsd"),
				new ClassPathResource("party/unireg-party-landregistry-1.xsd"),
				new ClassPathResource("party/unireg-party-landtaxlightening-1.xsd"),
				new ClassPathResource("party/unireg-party-5.xsd"),
				new ClassPathResource("party/unireg-party-taxpayer-5.xsd"),
				new ClassPathResource("party/unireg-party-corporation-5.xsd"),
				new ClassPathResource("event/fiscal/evt-fiscal-4.xsd")
		});

		sender = new EvenementFiscalV4SenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		if (sender instanceof InitializingBean) {
			((InitializingBean) sender).afterPropertiesSet();
		}
	}

	@Test
	public void publierEvenementArgumentNull() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			sender.sendEvent(null);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Argument evenement ne peut être null.", e.getMessage());
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
	
	@Test(timeout = 10000L)
	public void testSendEvenementImpressionFourreNeutre() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message
			final ContribuableImpositionPersonnesPhysiques pp = new PersonnePhysique(false);
			pp.setNumero(NUMERO_CONTRIBUABLE);
			final EvenementFiscalImpressionFourreNeutre event = new EvenementFiscalImpressionFourreNeutre(pp,2015,RegDate.get(2017, 01, 20));
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ev-fiscal-4:evenementFiscal xmlns:ev-fiscal-4=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/4\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ev-fiscal-4:ImpressionFourreNeutreType\"><ev-fiscal-4:categorieTiers>PP</ev-fiscal-4:categorieTiers><ev-fiscal-4:numeroTiers>12300652</ev-fiscal-4:numeroTiers><ev-fiscal-4:date><common-2:year>2017</common-2:year><common-2:month>1</common-2:month><common-2:day>20</common-2:day></ev-fiscal-4:date><ev-fiscal-4:periodeFiscale>2015</ev-fiscal-4:periodeFiscale></ev-fiscal-4:evenementFiscal>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}


}

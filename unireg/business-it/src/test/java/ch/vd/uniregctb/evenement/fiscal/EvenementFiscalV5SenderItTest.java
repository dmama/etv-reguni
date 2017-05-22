package ch.vd.uniregctb.evenement.fiscal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("Duplicates")
public class EvenementFiscalV5SenderItTest extends EvenementTest {

	private final static Long NUMERO_CONTRIBUABLE = 12300652L;

	private String OUTPUT_QUEUE;
	private EvenementFiscalV5SenderImpl sender;

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

		buildEsbMessageValidator(new Resource[]{new ClassPathResource("event/fiscal/evt-fiscal-5.xsd")});

		sender = new EvenementFiscalV5SenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		sender.afterPropertiesSet();
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
	public void testSendEvenementSituationFamille() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message
			ContribuableImpositionPersonnesPhysiques pp = new PersonnePhysique(false);
			pp.setNumero(NUMERO_CONTRIBUABLE);
			EvenementFiscalSituationFamille event = new EvenementFiscalSituationFamille(RegDate.get(2009, 12, 9), pp);
			event.setId(1234L);

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:familyStatusEventType\">" +
					"<fisc-evt-5:partyKind>NATURAL_PERSON</fisc-evt-5:partyKind>" +
					"<fisc-evt-5:partyNumber>12300652</fisc-evt-5:partyNumber>" +
					"<fisc-evt-5:date><common-2:year>2009</common-2:year><common-2:month>12</common-2:month><common-2:day>9</common-2:day></fisc-evt-5:date>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test(timeout = 10000L)
	public void testSendEvenementFor() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message
			final ContribuableImpositionPersonnesPhysiques pp = new PersonnePhysique(false);
			pp.setNumero(NUMERO_CONTRIBUABLE);
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2009, 12, 9), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			pp.addForFiscal(ffp);
			EvenementFiscalFor event = new EvenementFiscalFor(RegDate.get(2009, 12, 9), ffp, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
			event.setId(1234L);

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:taxResidenceStartEventType\">" +
					"<fisc-evt-5:partyKind>NATURAL_PERSON</fisc-evt-5:partyKind>" +
					"<fisc-evt-5:partyNumber>12300652</fisc-evt-5:partyNumber>" +
					"<fisc-evt-5:date><common-2:year>2009</common-2:year><common-2:month>12</common-2:month><common-2:day>9</common-2:day></fisc-evt-5:date>" +
					"<fisc-evt-5:mainTaxResidence>true</fisc-evt-5:mainTaxResidence>" +
					"<fisc-evt-5:typeAuthority>VAUD_MUNICIPALITY</fisc-evt-5:typeAuthority>" +
					"<fisc-evt-5:startReason>MOVE_IN_FROM_FOREIGN_COUNTRY</fisc-evt-5:startReason>" +
					"<fisc-evt-5:padding>0</fisc-evt-5:padding>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
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
			ContribuableImpositionPersonnesPhysiques pp = new PersonnePhysique(false);
			pp.setNumero(NUMERO_CONTRIBUABLE);
			EvenementFiscalImpressionFourreNeutre event = new EvenementFiscalImpressionFourreNeutre(pp,2015,RegDate.get(2017, 1, 20));
			event.setId(1234L);

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:neutralJacketPrintEventType\">" +
					"<fisc-evt-5:partyKind>NATURAL_PERSON</fisc-evt-5:partyKind>" +
					"<fisc-evt-5:partyNumber>12300652</fisc-evt-5:partyNumber>" +
					"<fisc-evt-5:date><common-2:year>2017</common-2:year><common-2:month>1</common-2:month><common-2:day>20</common-2:day></fisc-evt-5:date>" +
					"<fisc-evt-5:period>2015</fisc-evt-5:period>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
}

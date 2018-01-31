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

public class EvenementFiscalV3SenderItTest extends EvenementTest {

	private final static Long NUMERO_CONTRIBUABLE = 12300002L;

	private String OUTPUT_QUEUE;
	private EvenementFiscalV3SenderImpl sender;

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

		buildEsbMessageValidator(new Resource[]{new ClassPathResource("event/fiscal/evt-fiscal-3.xsd")});

		sender = new EvenementFiscalV3SenderImpl();
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
	public void testSendEvenementSituationFamille() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message
			final ContribuableImpositionPersonnesPhysiques pp = new PersonnePhysique(false);
			pp.setNumero(NUMERO_CONTRIBUABLE);
			final EvenementFiscalSituationFamille event = new EvenementFiscalSituationFamille(RegDate.get(2009, 12, 9), pp);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ev-fiscal-3:evenementFiscal xmlns:ev-fiscal-3=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/3\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ev-fiscal-3:changementSituationFamilleType\"><ev-fiscal-3:categorieTiers>PP</ev-fiscal-3:categorieTiers><ev-fiscal-3:numeroTiers>12300002</ev-fiscal-3:numeroTiers><ev-fiscal-3:date><common-2:year>2009</common-2:year><common-2:month>12</common-2:month><common-2:day>9</common-2:day></ev-fiscal-3:date></ev-fiscal-3:evenementFiscal>";
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
			final EvenementFiscalFor event = new EvenementFiscalFor(RegDate.get(2009, 12, 9), ffp, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ev-fiscal-3:evenementFiscal xmlns:ev-fiscal-3=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/3\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ev-fiscal-3:ouvertureForType\"><ev-fiscal-3:categorieTiers>PP</ev-fiscal-3:categorieTiers><ev-fiscal-3:numeroTiers>12300002</ev-fiscal-3:numeroTiers><ev-fiscal-3:date><common-2:year>2009</common-2:year><common-2:month>12</common-2:month><common-2:day>9</common-2:day></ev-fiscal-3:date><ev-fiscal-3:forPrincipal>true</ev-fiscal-3:forPrincipal><ev-fiscal-3:localisationFor>VAUD_MUNICIPALITY</ev-fiscal-3:localisationFor><ev-fiscal-3:motifOuverture>MOVE_IN_FROM_FOREIGN_COUNTRY</ev-fiscal-3:motifOuverture><ev-fiscal-3:padding>0</ev-fiscal-3:padding></ev-fiscal-3:evenementFiscal>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
}

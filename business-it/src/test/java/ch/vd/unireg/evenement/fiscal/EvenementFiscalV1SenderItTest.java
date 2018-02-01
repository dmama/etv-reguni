package ch.vd.unireg.evenement.fiscal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.fiscalite.registre.evenementFiscalV1.MotifForEnumType;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.parametrage.MockParameterAppService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class EvenementFiscalV1SenderItTest extends EvenementTest {

	private final static Long NUMERO_CONTRIBUABLE = 12300002L;

	private String OUTPUT_QUEUE;
	private EvenementFiscalV1SenderImpl sender;

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

		buildEsbMessageValidator(new Resource[]{new ClassPathResource("xsd/fiscal/evenementFiscalMaster-v1.xsd")});

		sender = new EvenementFiscalV1SenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		sender.setParametres(new MockParameterAppService());
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
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><even:evenementFiscalSituationFamille xmlns:even=\"http://www.vd.ch/fiscalite/registre/evenementFiscal-v1\"><even:dateEvenement>2009-12-09+01:00</even:dateEvenement><even:numeroTiers>12300002</even:numeroTiers><even:numeroTechnique>1234</even:numeroTechnique><even:codeEvenement>CHANGEMENT_SITUATION_FAMILLE</even:codeEvenement></even:evenementFiscalSituationFamille>";
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
			final ForFiscalPrincipal ffp =
					new ForFiscalPrincipalPP(RegDate.get(2009, 12, 9), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			pp.addForFiscal(ffp);
			final EvenementFiscalFor event = new EvenementFiscalFor(RegDate.get(2009, 12, 9), ffp, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><even:evenementFiscalFor xmlns:even=\"http://www.vd.ch/fiscalite/registre/evenementFiscal-v1\"><even:dateEvenement>2009-12-09+01:00</even:dateEvenement><even:numeroTiers>12300002</even:numeroTiers><even:numeroTechnique>1234</even:numeroTechnique><even:codeEvenement>OUVERTURE_FOR</even:codeEvenement><even:motifFor>ARRIVEE_HS</even:motifFor></even:evenementFiscalFor>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test
	public void testMotifsFors() throws Exception {
		// on doit vérifier que tous les motifs de fors connus dans unireg sont acceptés par l'XSD des événements fiscaux
		for (MotifFor motif : MotifFor.values()) {
			final MotifForEnumType.Enum m = EvenementFiscalV1SenderImpl.mapMotif(motif);
			Assert.assertNotNull("Motif " + motif + " inconnu dans la XSD des événements fiscaux v1", m);
		}
	}

	@Test
	public void testModesImposition() throws Exception {
		// on doit vérifier que tous les modes d'imposition connus dans unireg sont acceptés par l'XSD des événements fiscaux
		for (ModeImposition mode : ModeImposition.values()) {
			Assert.assertNotNull("Mode d'imposition " + mode + " inconnu dans la XSD des événements fiscaux v1", EvenementFiscalV1SenderImpl.mapModeImposition(mode));
		}
	}
}

package ch.vd.uniregctb.evenement.fiscal;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.fiscalite.registre.evenementFiscalV1.ModeImpositionEnumType;
import ch.vd.fiscalite.registre.evenementFiscalV1.MotifForEnumType;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.EvenementFiscalSituationFamille;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

public class EvenementFiscalSenderTest extends EvenementTest {

	private final static Long NUMERO_CONTRIBUABLE = 12300002L;

	private  String OUTPUT_QUEUE;
	private EvenementFiscalSenderImpl sender;

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
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(OUTPUT_QUEUE);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[] {new ClassPathResource("xsd/fiscal/evenementFiscalMaster-v1.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		sender = new EvenementFiscalSenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbMessageFactory(esbMessageFactory);

		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
	}

	@Override
	@After
	public void tearDown() {
		AuthenticationHelper.popPrincipal();
	}

	@Test
	public void publierEvenementArgumentNull() throws Exception {
		try {
			sender.sendEvent(null);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Argument evenement ne peut être null.", e.getMessage());
		}
	}
	
	@Test
	public void testSendEvenementSituationFamille() throws Exception {

		// Création du message
		Tiers tiers = new PersonnePhysique(false);
		tiers.setNumero(NUMERO_CONTRIBUABLE);
		EvenementFiscalSituationFamille event = new EvenementFiscalSituationFamille(tiers, RegDate.get(2009, 12, 9), (long) 1);
		event.setId(1234L);

		// Envoi du message
		sender.sendEvent(event);

		// On vérifie que l'on a bien envoyé le message
		final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><even:evenementFiscalSituationFamille xmlns:even=\"http://www.vd.ch/fiscalite/registre/evenementFiscal-v1\"><even:dateEvenement>2009-12-09+01:00</even:dateEvenement><even:numeroTiers>12300002</even:numeroTiers><even:numeroTechnique>1234</even:numeroTechnique><even:codeEvenement>CHANGEMENT_SITUATION_FAMILLE</even:codeEvenement></even:evenementFiscalSituationFamille>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}

	@Test
	public void testSendEvenementFor() throws Exception {

		// Création du message
		Tiers tiers = new PersonnePhysique(false);
		tiers.setNumero(NUMERO_CONTRIBUABLE);
		EvenementFiscalFor event = new EvenementFiscalFor(tiers, RegDate.get(2009, 12, 9), TypeEvenementFiscal.OUVERTURE_FOR, MotifFor.ARRIVEE_HS, ModeImposition.ORDINAIRE, (long) 1);
		event.setId(1234L);

		// Envoi du message
		sender.sendEvent(event);

		// On vérifie que l'on a bien envoyé le message
		final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><even:evenementFiscalFor xmlns:even=\"http://www.vd.ch/fiscalite/registre/evenementFiscal-v1\"><even:dateEvenement>2009-12-09+01:00</even:dateEvenement><even:numeroTiers>12300002</even:numeroTiers><even:numeroTechnique>1234</even:numeroTechnique><even:codeEvenement>OUVERTURE_FOR</even:codeEvenement><even:motifFor>ARRIVEE_HS</even:motifFor></even:evenementFiscalFor>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}

	@Test
	public void testMotifsFors() throws Exception {
		// on doit vérifier que tous les motifs de fors connus dans unireg sont acceptés par l'XSD des événements fiscaux
		for (MotifFor motif : MotifFor.values()) {
			final MotifForEnumType.Enum m = EvenementFiscalSenderImpl.core2xml(motif);
			Assert.assertNotNull("Motif " + motif + " inconnu dans l'XSD des événements fiscaux", m);
		}
	}

	@Test
	public void testModesImposition() throws Exception {
		// on doit vérifier que tous les modes d'imposition connus dans unireg sont acceptés par l'XSD des événements fiscaux
		for (ModeImposition mode : ModeImposition.values()) {
			Assert.assertNotNull("Mode d'imposition " + mode + " inconnu dans l'XSD des événements fiscaux", ModeImpositionEnumType.Enum.forString(mode.toString()));
		}
	}
}

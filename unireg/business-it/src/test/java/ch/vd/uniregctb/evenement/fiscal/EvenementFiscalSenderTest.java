package ch.vd.uniregctb.evenement.fiscal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.spring.EsbTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.EvenementFiscalSituationFamille;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.util.Log4jConfigurer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(JUnit4ClassRunner.class)
public class EvenementFiscalSenderTest extends EvenementTest {

	private final static Long NUMERO_CONTRIBUABLE = 12300002L;

	private static final String INPUT_QUEUE = "ch.vd.unireg.test.input";
	private static final String OUTPUT_QUEUE = "ch.vd.unireg.test.output";
	private EvenementFiscalSenderImpl sender;


	@Before
	public void setUp() throws Exception {

		Log4jConfigurer.initLogging("classpath:ut/log4j.xml");

		final ActiveMQConnectionFactory jmsConnectionManager = new ActiveMQConnectionFactory();
		jmsConnectionManager.setBrokerURL("tcp://grominet:4500");
		jmsConnectionManager.setUserName("smx");
		jmsConnectionManager.setPassword("smx");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionManager);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.afterPropertiesSet();

		clearQueue(OUTPUT_QUEUE);
		clearQueue(INPUT_QUEUE);

		sender = new EvenementFiscalSenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);

		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
	}

	@After
	public void tearDown() {
		AuthenticationHelper.popPrincipal();
	}

	@Test
	public void publierEvenementArgumentNull() throws Exception {
		try {
			sender.sendEvent(null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Argument evenement ne peut être null.", e.getMessage());
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
}

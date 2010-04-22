package ch.vd.uniregctb.evenement.identification.contribuable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.type.Sexe;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Classe de test du handler de messages d'identification de contribuables. Cette classe nécessite une connexion à l'ESB de développement
 * pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@RunWith(JUnit4ClassRunner.class)
public class IdentificationContribuableMessageHandlerTest extends EvenementTest {

	private static final String INPUT_QUEUE = "ch.vd.unireg.test.input";
	private static final String OUTPUT_QUEUE = "ch.vd.unireg.test.output";
	private IdentificationContribuableMessageHandlerImpl handler;
	private DefaultMessageListenerContainer container;

	@Before
	public void setUp() throws Exception {

		Log4jConfigurer.initLogging("classpath:ut/log4j.xml");

		final ActiveMQConnectionFactory jmsConnectionManager = new ActiveMQConnectionFactory();
		jmsConnectionManager.setBrokerURL("tcp://grominet:4500");
		jmsConnectionManager.setUserName("smx");
		jmsConnectionManager.setPassword("smx");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionManager);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
//		esbTemplate.afterPropertiesSet();       // la méthode n'existe plus en 2.1

		clearQueue(OUTPUT_QUEUE);
		clearQueue(INPUT_QUEUE);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[] {new ClassPathResource("xsd/identification/serviceIdentificationCTBAsynchrone_1-6.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplate() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		handler = new IdentificationContribuableMessageHandlerImpl();
		handler.setOutputQueue(OUTPUT_QUEUE);
		handler.setEsbTemplate(esbTemplate);
		handler.setEsbMessageFactory(esbMessageFactory);
		handler.setHibernateTemplate(hibernateTemplate);

		container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(jmsConnectionManager);
		container.setMessageListener(handler);
		container.setDestinationName(INPUT_QUEUE);
		container.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		container.destroy();
	}

	@Test
	public void testSendReponseContribuableTrouve() throws Exception {

		// Création du message
		final IdentificationContribuable message = new IdentificationContribuable();
		final EsbHeader header = new EsbHeader();
		header.setBusinessUser("IdentificationContribuableTest");
		header.setBusinessId(String.valueOf(message.hashCode()));
		header.setReplyTo("ReplyToTest");
		message.setHeader(header);
		final Reponse reponse = new Reponse();
		reponse.setDate(newUtilDate(2008, 3, 23));
		reponse.setNoContribuable(123456789L);
		message.setReponse(reponse);

		// Envoi du message
		handler.sendReponse(message);

		// On vérifie que l'on a bien envoyé le message
		final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.6\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:contribuable><iden:numeroContribuableIndividuel>123456789</iden:numeroContribuableIndividuel></iden:contribuable></iden:reponse></iden:identificationCTB>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}

	@Test
	public void testSendReponseContribuableNonTrouve() throws Exception {

		// Création du message
		final IdentificationContribuable message = new IdentificationContribuable();
		final EsbHeader header = new EsbHeader();
		header.setBusinessUser("IdentificationContribuableTest");
		header.setBusinessId(String.valueOf(message.hashCode()));
		header.setReplyTo("ReplyToTest");
		message.setHeader(header);
        // demande
        final CriteresPersonne personne = new CriteresPersonne();
        personne.setNAVS13("1234567890");
        final Demande demande = new Demande();
        demande.setPersonne(personne);
        message.setDemande(demande);
        // réponse
		final Reponse reponse = new Reponse();
		reponse.setErreur(new Erreur(TypeErreur.METIER, "01", "Aucun contribuable ne correspond au message"));
		reponse.setDate(newUtilDate(2008, 3, 23));
		reponse.setNoContribuable(null);
		message.setReponse(reponse);

		// Envoi du message
		handler.sendReponse(message);

		// On vérifie que l'on a bien envoyé le message et qu'il ne contient pas la demande
		final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.6\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:erreur><iden:type>M</iden:type><iden:code>01</iden:code><iden:message>Aucun contribuable ne correspond au message</iden:message></iden:erreur></iden:reponse></iden:identificationCTB>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}

	@Test
	public void testSendReponseErreurTechnique() throws Exception {

		// Création du message
		final IdentificationContribuable message = new IdentificationContribuable();
		final EsbHeader header = new EsbHeader();
		header.setBusinessUser("IdentificationContribuableTest");
		header.setBusinessId(String.valueOf(message.hashCode()));
		header.setReplyTo("ReplyToTest");
		message.setHeader(header);
		final Reponse reponse = new Reponse();
		reponse.setDate(newUtilDate(2008, 3, 23));
		final Erreur erreur = new Erreur();
		erreur.setType(TypeErreur.TECHNIQUE);
		erreur.setCode("TestErreurTechnique");
		erreur.setMessage("Erreur technique de test");
		reponse.setErreur(erreur);
		message.setReponse(reponse);

		// Envoi du message
		handler.sendReponse(message);

		// On vérifie que l'on a bien envoyé le message
		final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.6\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:erreur><iden:type>T</iden:type><iden:code>TestErreurTechnique</iden:code><iden:message>Erreur technique de test</iden:message></iden:erreur></iden:reponse></iden:identificationCTB>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}

	@Test
	public void testSendReponseErreurMetier() throws Exception {

		// Création du message
		final IdentificationContribuable message = new IdentificationContribuable();
		final EsbHeader header = new EsbHeader();
		header.setBusinessUser("IdentificationContribuableTest");
		header.setBusinessId(String.valueOf(message.hashCode()));
		header.setReplyTo("ReplyToTest");
		message.setHeader(header);
		final Reponse reponse = new Reponse();
		reponse.setDate(newUtilDate(2008, 3, 23));
		final Erreur erreur = new Erreur();
		erreur.setType(TypeErreur.METIER);
		erreur.setCode("TestErreurMetier");
		erreur.setMessage("Erreur métier de test");
		reponse.setErreur(erreur);
		message.setReponse(reponse);

		// Envoi du message
		handler.sendReponse(message);

		// On vérifie que l'on a bien envoyé le message
		final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.6\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:erreur><iden:type>M</iden:type><iden:code>TestErreurMetier</iden:code><iden:message>Erreur métier de test</iden:message></iden:erreur></iden:reponse></iden:identificationCTB>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}

	@Test
	public void testReceiveDemandeIdentificationCtb() throws Exception {

		final List<IdentificationContribuable> messages = new ArrayList<IdentificationContribuable>();

		handler.setDemandeHandler(new DemandeHandler() {
			public void handleDemande(IdentificationContribuable message) {
				messages.add(message);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils
				.getFile("classpath:ch/vd/uniregctb/evenement/identification/contribuable/demande_identification_alfred_hitchcock.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTexteMessage(INPUT_QUEUE, texte);

		// On attend le message jusqu'à 3 secondes
		for (int i = 0; messages.isEmpty() && i < 30; i++) {
			Thread.sleep(100);
		}
		assertEquals(1, messages.size());

		final IdentificationContribuable m = messages.get(0);
		assertNotNull(m);

		final EsbHeader header = m.getHeader();
		assertNotNull(header);
		assertEquals("EvenementTest", header.getBusinessUser());

		final Demande demande = m.getDemande();
		assertNotNull(demande);
		assertEquals("IdentificationContribuableTest", demande.getEmetteurId());
		assertEquals(2008, demande.getPeriodeFiscale());
		assertEquals(PrioriteEmetteur.NON_PRIORITAIRE, demande.getPrioriteEmetteur());
		assertEquals(2, demande.getPrioriteUtilisateur());
		assertEquals("ssk-3001-000101", demande.getTypeMessage());

		final CriteresPersonne personne = demande.getPersonne();
		assertNotNull(personne);
		assertEquals(RegDate.get(1903, 8, 13), personne.getDateNaissance());
		assertNull(personne.getNAVS11());
		assertNull(personne.getNAVS13());
		assertEquals("Hitchcock", personne.getNom());
		assertEquals("Alfred Joseph", personne.getPrenoms());
		assertEquals(Sexe.MASCULIN, personne.getSexe());

		final CriteresAdresse adresse = personne.getAdresse();
		assertNotNull(adresse);
		assertNull(adresse.getChiffreComplementaire());
		assertEquals("UK", adresse.getCodePays());
		assertEquals("London", adresse.getLieu());
		assertNull(adresse.getLigneAdresse1());
		assertNull(adresse.getLigneAdresse2());
		assertNull(adresse.getLocalite());
		assertNull(adresse.getNoAppartement());
		assertNull(adresse.getNoOrdrePosteSuisse());
		assertNull(adresse.getNoPolice());
		assertNull(adresse.getNpaEtranger());
		assertNull(adresse.getNpaSuisse());
		assertNull(adresse.getNumeroCasePostale());
		assertNull(adresse.getRue());
		assertNull(adresse.getTexteCasePostale());
		assertNull(adresse.getTypeAdresse());
	}
}

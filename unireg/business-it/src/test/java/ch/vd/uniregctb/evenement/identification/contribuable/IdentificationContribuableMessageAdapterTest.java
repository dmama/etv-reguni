package ch.vd.uniregctb.evenement.identification.contribuable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Classe de test du handler de messages d'identification de contribuables. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IdentificationContribuableMessageAdapterTest extends EvenementTest {

	private String INPUT_QUEUE;
	private String OUTPUT_QUEUE;
	private IdentificationContribuableMessageListenerImpl handler;
	private EsbTemplateWithErrorCollector esbTemplateWithErrorCollector;

	private static class EsbTemplateWithErrorCollector extends EsbJmsTemplate {

		private static final class ErrorDescription {
			public final EsbMessage msg;
			public final String errorMessage;
			public final Exception exception;
			public final ErrorType errorType;
			public final String errorCode;

			private ErrorDescription(EsbMessage msg, String errorMessage, Exception exception, ErrorType errorType, String errorCode) {
				this.msg = msg;
				this.errorMessage = errorMessage;
				this.exception = exception;
				this.errorType = errorType;
				this.errorCode = errorCode;
			}
		}

		public final List<ErrorDescription> collectedErrors = new ArrayList<ErrorDescription>();

		@Override
		public void sendError(EsbMessage esbMessage, String errorMessage, Exception exception, ErrorType errorType, String errorCode) throws Exception {
			collectedErrors.add(new ErrorDescription(esbMessage, errorMessage, exception, errorType, errorCode));
		}
	}

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.ident.ctb.input");
		OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.ident.ctb.output");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplateWithErrorCollector = new EsbTemplateWithErrorCollector();
		esbTemplate = esbTemplateWithErrorCollector;
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(OUTPUT_QUEUE);
		clearQueue(INPUT_QUEUE);

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[]{new ClassPathResource("xsd/identification/serviceIdentificationCTBAsynchrone_1-7.2.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplate() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		handler = new IdentificationContribuableMessageListenerImpl();
		handler.setOutputQueue(OUTPUT_QUEUE);
		handler.setEsbTemplate(esbTemplate);
		handler.setEsbMessageFactory(esbMessageFactory);
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));

		initEndpointManager(INPUT_QUEUE, handler);
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
		final String texte =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.7\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:contribuable><iden:numeroContribuableIndividuel>123456789</iden:numeroContribuableIndividuel></iden:contribuable></iden:reponse></iden:identificationCTB>";
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
		final String texte =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.7\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:erreur><iden:type>M</iden:type><iden:code>01</iden:code><iden:message>Aucun contribuable ne correspond au message</iden:message></iden:erreur></iden:reponse></iden:identificationCTB>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}

	@Test
	public void testSendReponseContribuableSansManuel() throws Exception {

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
		demande.setModeIdentification(Demande.ModeIdentificationType.SANS_MANUEL);
		demande.setPersonne(personne);
		message.setDemande(demande);
		// réponse
		final Reponse reponse = new Reponse();
		String contenuMessage = "Aucun contribuable n’a été trouvé avec l’identification automatique et l’identification manuelle n’a pas été demandée";
		Erreur erreur = new Erreur(TypeErreur.METIER, "01", contenuMessage);
		reponse.setErreur(erreur);
		reponse.setDate(newUtilDate(2008, 3, 23));
		reponse.setNoContribuable(null);
		message.setReponse(reponse);

		// Envoi du message
		handler.sendReponse(message);

		// On vérifie que l'on a bien envoyé le message et qu'il ne contient pas la demande
		final String texte =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.7\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:erreur><iden:type>M</iden:type><iden:code>01</iden:code><iden:message>Aucun contribuable n’a été trouvé avec l’identification automatique et l’identification manuelle n’a pas été demandée</iden:message></iden:erreur></iden:reponse></iden:identificationCTB>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}


	@Test
	public void testSendReponseContribuableManuelAveckAck() throws Exception {

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
		demande.setModeIdentification(Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		demande.setPersonne(personne);
		message.setDemande(demande);
		// réponse
		final Reponse reponse = new Reponse();
		reponse.setEnAttenteIdentifManuel(true);
		reponse.setDate(newUtilDate(2008, 3, 23));
		reponse.setNoContribuable(null);
		message.setReponse(reponse);

		// Envoi du message
		handler.sendReponse(message);

		// On vérifie que l'on a bien envoyé le message et qu'il ne contient pas la demande
		final String texte =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.7\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:enAttenteIdentifManuel>true</iden:enAttenteIdentifManuel></iden:reponse></iden:identificationCTB>";
		assertTextMessage(OUTPUT_QUEUE, texte);
	}

	private static final String EXPECTED_XML_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><iden:identificationCTB xmlns:iden=\"http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.7\"><iden:reponse><iden:date>2008-03-23T00:00:00.000+01:00</iden:date><iden:erreur><iden:type>%s</iden:type><iden:code>%s</iden:code><iden:message>%s</iden:message></iden:erreur></iden:reponse></iden:identificationCTB>";

	@Test
	public void testSendReponseErreurTechnique() throws Exception {
		testSendReponseErreur(TypeErreur.TECHNIQUE,	"TestErreurTechnique",	"Erreur technique de test");
	}

	@Test
	public void testSendReponseErreurContribuableInconnu() throws Exception {
		testSendReponseErreur(IdentificationContribuable.ErreurMessage.AUCUNE_CORRESPONDANCE);
	}

	@Test
	public void testSendReponseErreurVersACI() throws Exception {
		testSendReponseErreur(IdentificationContribuable.ErreurMessage.ACI_AUTRE_CANTON);
	}

	@Test
	public void testSendReponseErreurVersIs() throws Exception {
		testSendReponseErreur(IdentificationContribuable.ErreurMessage.SECTION_IMPOT_SOURCE);
	}

	@Test
	public void testSendReponseErreurVersOMPI() throws Exception {
		testSendReponseErreur(IdentificationContribuable.ErreurMessage.OIPM);
	}

	@Test
	public void testSendReponseErreurFrontalier() throws Exception {
		testSendReponseErreur(IdentificationContribuable.ErreurMessage.FRONTALIER);
	}

	private void testSendReponseErreur (IdentificationContribuable.ErreurMessage erreurMessage) throws Exception {
		testSendReponseErreur(TypeErreur.METIER, erreurMessage.getCode(), erreurMessage.getLibelle());
	}

	private void testSendReponseErreur (TypeErreur type, String code, String msgErreur) throws Exception {
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
		erreur.setType(type);
		erreur.setCode(code);
		erreur.setMessage(msgErreur);
		reponse.setErreur(erreur);
		message.setReponse(reponse);

		// Envoi du message
		handler.sendReponse(message);

		// On vérifie que l'on a bien envoyé le message
		assertTextMessage(OUTPUT_QUEUE, String.format(EXPECTED_XML_TEMPLATE, type.name().substring(0,1), code, msgErreur));
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveDemandeIdentificationCtb() throws Exception {

		final List<IdentificationContribuable> messages = new ArrayList<IdentificationContribuable>();

		handler.setDemandeHandler(new DemandeHandler() {
			@Override
			public void handleDemande(IdentificationContribuable message) {
				messages.add(message);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/identification/contribuable/demande_identification_alfred_hitchcock.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (messages.isEmpty()) {
			Thread.sleep(100);
		}
		assertEquals(1, messages.size());

		final IdentificationContribuable m = messages.get(0);
		assertEquals(TypeDemande.MELDEWESEN, m.getDemande().getTypeDemande());
		assertNotNull(m);

		final EsbHeader header = m.getHeader();
		assertNotNull(header);
		assertEquals("EvenementTest", header.getBusinessUser());
		assertNull(header.getDocumentUrl());

		final Demande demande = m.getDemande();
		assertNotNull(demande);
		assertEquals("IdentificationContribuableTest", demande.getEmetteurId());
		assertEquals(2008, demande.getPeriodeFiscale());
		assertEquals(PrioriteEmetteur.NON_PRIORITAIRE, demande.getPrioriteEmetteur());
		assertEquals(2, demande.getPrioriteUtilisateur());
		assertEquals("ssk-3001-000101", demande.getTypeMessage());
		assertEquals(Demande.ModeIdentificationType.MANUEL_SANS_ACK, demande.getModeIdentification());

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

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAvecUrlDocument() throws Exception {
		final List<IdentificationContribuable> messages = new ArrayList<IdentificationContribuable>();

		handler.setDemandeHandler(new DemandeHandler() {
			@Override
			public void handleDemande(IdentificationContribuable message) {
				messages.add(message);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/identification/contribuable/demande_identification_alfred_hitchcock.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<String, String>();
		final String url = "http://mamachine:3421/mondocument.pdf";
		customAttributes.put(IdentificationContribuableMessageListenerImpl.DOCUMENT_URL_ATTRIBUTE_NAME, url);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (messages.isEmpty()) {
			Thread.sleep(100);
		}
		assertEquals(1, messages.size());

		final IdentificationContribuable m = messages.get(0);
		assertNotNull(m);

		final EsbHeader header = m.getHeader();
		assertNotNull(header);
		assertEquals("EvenementTest", header.getBusinessUser());
		assertEquals(url, header.getDocumentUrl());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAvecDateFarfelue() throws Exception {

		final List<IdentificationContribuable> messages = new ArrayList<IdentificationContribuable>();

		handler.setDemandeHandler(new DemandeHandler() {
			@Override
			public void handleDemande(IdentificationContribuable message) {
				messages.add(message);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/identification/contribuable/demande_identification_date_farfelue.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (messages.isEmpty()) {
			Thread.sleep(100);
		}
		assertEquals(1, messages.size());
		assertEquals(0, esbTemplateWithErrorCollector.collectedErrors.size());

		final IdentificationContribuable m = messages.get(0);
		assertNotNull(m);
		assertNull(m.getDemande().getPersonne().getDateNaissance());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationNCS() throws Exception {
		final List<IdentificationContribuable> messages = new ArrayList<IdentificationContribuable>();

		handler.setDemandeHandler(new DemandeHandler() {
			@Override
			public void handleDemande(IdentificationContribuable message) {
				messages.add(message);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/identification/contribuable/demande_identification_NCS_alfred_hitchcock.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<String, String>();
		final String url = "";
		customAttributes.put(IdentificationContribuableMessageListenerImpl.DOCUMENT_URL_ATTRIBUTE_NAME, url);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (messages.isEmpty()) {
			Thread.sleep(100);
		}
		assertEquals(1, messages.size());

		final IdentificationContribuable m = messages.get(0);
		assertNotNull(m);
		assertEquals(TypeDemande.NCS, m.getDemande().getTypeDemande());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationImpotSource_LISTE_IS() throws Exception {
		final List<IdentificationContribuable> messages = new ArrayList<IdentificationContribuable>();

		handler.setDemandeHandler(new DemandeHandler() {
			@Override
			public void handleDemande(IdentificationContribuable message) {
				messages.add(message);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/identification/contribuable/demande_identification_IMPOT_SOURCE_LISTE_IS_alfred_hitchcock.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<String, String>();
		final String url = "";
		customAttributes.put(IdentificationContribuableMessageListenerImpl.DOCUMENT_URL_ATTRIBUTE_NAME, url);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (messages.isEmpty()) {
			Thread.sleep(100);
		}
		assertEquals(1, messages.size());

		final IdentificationContribuable m = messages.get(0);
		assertNotNull(m);
		assertEquals(TypeDemande.IMPOT_SOURCE, m.getDemande().getTypeDemande());
	}


	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationE_Facture() throws Exception {
		final List<IdentificationContribuable> messages = new ArrayList<IdentificationContribuable>();

		handler.setDemandeHandler(new DemandeHandler() {
			@Override
			public void handleDemande(IdentificationContribuable message) {
				messages.add(message);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/identification/contribuable/demande_identification_E_FACTURE_alfred_hitchcock.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<String, String>();
		final String url = "";
		customAttributes.put(IdentificationContribuableMessageListenerImpl.DOCUMENT_URL_ATTRIBUTE_NAME, url);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (messages.isEmpty()) {
			Thread.sleep(100);
		}
		assertEquals(1, messages.size());

		final IdentificationContribuable m = messages.get(0);
		assertNotNull(m);
		assertEquals(TypeDemande.E_FACTURE, m.getDemande().getTypeDemande());
	}

}

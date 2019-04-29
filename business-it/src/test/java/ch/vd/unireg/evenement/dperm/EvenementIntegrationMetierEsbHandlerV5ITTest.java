package ch.vd.unireg.evenement.dperm;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.util.ResourceUtils;

import ch.vd.dperm.xml.common.v1.TypImmeuble;
import ch.vd.dperm.xml.common.v1.TypeImposition;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.evenement.degrevement.EvenementIntegrationMetierDegrevementHandler;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.hibernate.HibernateTemplateImpl;
import ch.vd.unireg.jms.EsbMessageHelper;
import ch.vd.unireg.xml.degrevement.quittance.v1.Commune;
import ch.vd.unireg.xml.degrevement.quittance.v1.QuittanceIntegrationMetierImmDetails;
import ch.vd.unireg.xml.event.degrevement.v1.Caracteristiques;
import ch.vd.unireg.xml.event.degrevement.v1.CodeSupport;
import ch.vd.unireg.xml.event.degrevement.v1.DonneesMetier;
import ch.vd.unireg.xml.event.degrevement.v1.Message;
import ch.vd.unireg.xml.event.degrevement.v1.SousTypeDocument;
import ch.vd.unireg.xml.event.degrevement.v1.Supervision;
import ch.vd.unireg.xml.event.degrevement.v1.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EvenementIntegrationMetierEsbHandlerV5ITTest extends EvenementTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementIntegrationMetierEsbHandlerV5ITTest.class);

	private String INPUT_QUEUE;
	private EvenementIntegrationMetierDegrevementHandler degrevHandler;
	private List<EsbMessage> sentMessages;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtIntegrationMetierDPerm");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		sentMessages = new LinkedList<>();
		final EsbJmsTemplate sendingTemplate = new EsbJmsTemplate() {
			@Override
			public void send(EsbMessage esbMessage) throws Exception {
				synchronized (sentMessages) {
					sentMessages.add(esbMessage);
					sentMessages.notifyAll();
				}
			}
		};

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);

		clearQueue(INPUT_QUEUE);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplateImpl() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		degrevHandler = new EvenementIntegrationMetierDegrevementHandler();
		degrevHandler.setHibernateTemplate(hibernateTemplate);
		degrevHandler.afterPropertiesSet();

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("/event/dperm/typeSimpleDPerm-1.xsd"),
				new ClassPathResource("/event/dperm/elementsIntegrationMetier-5.xsd"),
				new ClassPathResource("/event/degrevement/quittanceIntegrationMetierImmDetails-1.xsd")
		});

		final EvenementIntegrationMetierEsbHandlerV5 handler = new EvenementIntegrationMetierEsbHandlerV5();
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setEsbTemplate(sendingTemplate);
		handler.setEsbValidator(esbValidator);
		handler.setHandlers(Collections.singletonMap(ch.vd.dperm.xml.common.v1.TypeDocument.DEM_DEGREV, degrevHandler));
		handler.afterPropertiesSet();

		initListenerContainer(INPUT_QUEUE, handler);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReception() throws Exception {

		final List<Pair<Message, Map<String, String>>> collected = new LinkedList<>();
		degrevHandler.setHandler((quittance, headers) -> {
			synchronized (collected) {
				collected.add(Pair.of(quittance, headers));
				collected.notifyAll();
				return new QuittanceIntegrationMetierImmDetails(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()),
				                                                BigInteger.valueOf(quittance.getDonneesMetier().getPeriodeFiscale()),
				                                                quittance.getDonneesMetier().getNumeroContribuable(),
				                                                TypeImposition.IMPOT_COMPLEMENTAIRE_IMMEUBLE,
				                                                new Commune(BigInteger.valueOf(42), "Tralalaouti"),
				                                                "142-9",
				                                                TypImmeuble.B_F,
				                                                "Jardin",
				                                                BigDecimal.valueOf(1200),
				                                                true,
				                                                null,
				                                                null,
				                                                null);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/evenement/dperm/evenementIntegrationMetierDegrevement.xml");
		final String texte = FileUtils.readFileToString(file);

		// quelques attributs "custom"
		final Map<String, String> attributes = new HashMap<>();
		attributes.put("Devise", "Tralalatsointsoin");
		attributes.put("Finalité", "Aucune, vraiment");

		// Envoie le message
		final String businessID = UUID.randomUUID().toString();
		sendTextMessage(INPUT_QUEUE, texte, businessID, attributes);

		// On attend le message
		synchronized (collected) {
			while (collected.isEmpty()) {
				collected.wait(1000);
			}
		}
		Assert.assertEquals(1, collected.size());

		final Pair<Message, Map<String, String>> q = collected.get(0);
		assertNotNull(q);
		assertNotNull(q.getLeft());
		assertNotNull(q.getRight());
		assertEquals(2, q.getRight().size());
		assertEquals("Tralalatsointsoin", q.getRight().get("Devise"));
		assertEquals("Aucune, vraiment", q.getRight().get("Finalité"));

		final Caracteristiques caracteristiques = q.getLeft().getCaracteristiques();
		assertNotNull(caracteristiques);
		assertEquals(TypeDocument.DEM_DEGREV, caracteristiques.getTypeDocument());
		assertEquals(SousTypeDocument.DEM_DEGREV, caracteristiques.getSousTypeDocument());
		assertEquals(CodeSupport.ELECTRONIQUE, caracteristiques.getSupport());
		assertEquals("Super-application-magnifique", caracteristiques.getEmetteur());

		final Supervision supervision = q.getLeft().getSupervision();
		assertNotNull(supervision);
		assertEquals(RegDate.get(2017, 3, 8), XmlUtils.xmlcal2regdate(supervision.getHorodatageReception()));
		assertEquals(RegDate.get(2017, 3, 8), XmlUtils.xmlcal2regdate(supervision.getHorodatagePublication()));
		assertEquals("Tralala", supervision.getIdBusinessId());

		final DonneesMetier donneesMetier = q.getLeft().getDonneesMetier();
		assertNotNull(donneesMetier);
		assertEquals(2016, donneesMetier.getPeriodeFiscale());
		assertEquals(12454, donneesMetier.getNumeroContribuable());
		assertEquals(BigInteger.valueOf(7415), donneesMetier.getNumeroSequenceDemande());
		assertEquals(BigInteger.valueOf(123456), donneesMetier.getRevenuLocatifEncaisse().getValue());
		assertTrue(donneesMetier.getRevenuLocatifEncaisse().isValide());
		assertEquals(BigInteger.valueOf(1231), donneesMetier.getVolumeLocatif().getValue());
		assertTrue(donneesMetier.getVolumeLocatif().isValide());
		assertEquals(BigInteger.valueOf(140), donneesMetier.getSurfaceLocatif().getValue());
		assertTrue(donneesMetier.getSurfaceLocatif().isValide());
		assertEquals(0, BigDecimal.valueOf(6000, 2).compareTo(donneesMetier.getPourcentageLocatif().getValue()));
		assertTrue(donneesMetier.getPourcentageLocatif().isValide());
		assertEquals(BigInteger.valueOf(54848), donneesMetier.getRevenuLocatifEstime().getValue());
		assertTrue(donneesMetier.getRevenuLocatifEstime().isValide());
		assertEquals(BigInteger.valueOf(4541), donneesMetier.getVolumePropreUsage().getValue());
		assertTrue(donneesMetier.getVolumePropreUsage().isValide());
		assertEquals(BigInteger.valueOf(85), donneesMetier.getSurfacePropreUsage().getValue());
		assertTrue(donneesMetier.getSurfacePropreUsage().isValide());
		assertEquals(0, BigDecimal.valueOf(4000, 2).compareTo(donneesMetier.getPourcentagePropreUsage().getValue()));
		assertTrue(donneesMetier.getPourcentagePropreUsage().isValide());

		// attente du message de réponse
		synchronized (sentMessages) {
			while (sentMessages.isEmpty()) {
				sentMessages.wait(1000);
			}
		}

		assertEquals(1, sentMessages.size());
		final EsbMessage reponse = sentMessages.get(0);
		assertEquals(businessID, reponse.getBusinessCorrelationId());
		assertEquals(businessID + "-answer", reponse.getBusinessId());
		assertEquals("http://www.vd.ch/fiscalite/dperm/bpms/unireg/quittanceIntegrationMetierImm/1", EsbMessageHelper.extractNamespaceURI(reponse, LOGGER));
	}
}

package ch.vd.uniregctb.evenement.degrevement;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.xml.XmlUtils;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.xml.event.degrevement.v1.Caracteristiques;
import ch.vd.unireg.xml.event.degrevement.v1.CodeSupport;
import ch.vd.unireg.xml.event.degrevement.v1.DonneesMetier;
import ch.vd.unireg.xml.event.degrevement.v1.Message;
import ch.vd.unireg.xml.event.degrevement.v1.SousTypeDocument;
import ch.vd.unireg.xml.event.degrevement.v1.Supervision;
import ch.vd.unireg.xml.event.degrevement.v1.TypeDocument;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EvenementDegrevementEsbHandlerV1ITTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementDegrevementEsbHandlerV1 handler;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtDegrevement");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

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

		handler = new EvenementDegrevementEsbHandlerV1();
		handler.setHibernateTemplate(hibernateTemplate);
		handler.afterPropertiesSet();

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setHandler(handler);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("/event/degrevement/documentDematDegrevement-1.xsd")
		});

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReception() throws Exception {

		final List<Pair<Message, Map<String, String>>> collected = new LinkedList<>();
		handler.setHandler((quittance, headers) -> {
			synchronized (collected) {
				collected.add(Pair.of(quittance, headers));
				collected.notifyAll();
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/degrevement/evenementDegrevementV1.xml");
		final String texte = FileUtils.readFileToString(file);

		// quelques attributs "custom"
		final Map<String, String> attributes = new HashMap<>();
		attributes.put("Devise", "Tralalatsointsoin");
		attributes.put("Finalité", "Aucune, vraiment");

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte, attributes);

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
		assertEquals(RegDate.get(2017, 3, 8), XmlUtils.cal2regdate(supervision.getHorodatageReception()));
		assertEquals(RegDate.get(2017, 3, 8), XmlUtils.cal2regdate(supervision.getHorodatagePublication()));
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
	}

}

package ch.vd.uniregctb.evenement.cedi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.util.ResourceUtils;

import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Classe de test du listener d'événements CEDI. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementCediEsbMessageHandlerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementCediEsbMessageHandler esbHandler;

	@Before
	public void setUp() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtCedi");

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

		esbHandler = new EvenementCediEsbMessageHandler();
		esbHandler.setHibernateTemplate(hibernateTemplate);

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setHandler(esbHandler);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("event/taxation/DossierElectronique-1-0.xsd"),
				new ClassPathResource("event/taxation/DossierElectronique-2-0.xsd"),
				new ClassPathResource("event/taxation/DossierElectronique-3-2.xsd"),
				new ClassPathResource("event/taxation/DossierElectronique-2015-2.xsd"),
				new ClassPathResource("event/taxation/DossierElectronique-2016-1.xsd"),
				new ClassPathResource("event/taxation/DossierElectronique-2016-2.xsd"),
				new ClassPathResource("event/taxation/DossierElectronique-2017-1.xsd"),
		});

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveRetourDI() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();

		esbHandler.setHandlers(Arrays.<DossierElectroniqueHandler<?>>asList(new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) {
				events.add(evt);
			}
		}));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/retour_di.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(12500001L, q.getNoContribuable());
		assertEquals(2009, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.MANUSCRITE, q.getTypeDocument());
		assertNull(q.getEmail());
		assertEquals("CH3708401016ZZ0535380", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}

	/**
	 * [UNIREG-2603] Vérifie qu'on ne crashe pas quand on reçoit un retour de DI presque vide.
	 */
	@SuppressWarnings({"JavaDoc"})
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveRetourDIPresqueVide() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();

		esbHandler.setHandlers(Arrays.<DossierElectroniqueHandler<?>>asList(new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) {
				events.add(evt);
			}
		}));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/retour_di_presque_vide.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(12500001L, q.getNoContribuable());
		assertEquals(2009, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.MANUSCRITE, q.getTypeDocument());
		assertNull(q.getEmail());
		assertNull(q.getIban());
		assertNull(q.getNoTelephone());
		assertNull(q.getNoMobile());
		assertNull(q.getTitulaireCompte());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testFormatV2() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final DossierElectroniqueHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v2 ne devrait pas arriver dans le handler v1");
			}
		};
		final DossierElectroniqueHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};
		final DossierElectroniqueHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v2 ne devrait pas arriver dans le handler v3");
			}
		};
		final DossierElectroniqueHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v2 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final DossierElectroniqueHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v2 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/DossierElectronique-2.0-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(10500171, q.getNoContribuable());
		assertEquals(2013, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.VAUDTAX, q.getTypeDocument());
		assertEquals("toto@earth.net", q.getEmail());
		assertEquals("CH2800767000U09565735", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testFormatV3() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final DossierElectroniqueHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler v1");
			}
		};
		final DossierElectroniqueHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler v2");
			}
		};
		final DossierElectroniqueHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final DossierElectroniqueHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final DossierElectroniqueHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/DossierElectronique-3.0-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(10500171, q.getNoContribuable());
		assertEquals(2014, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.VAUDTAX, q.getTypeDocument());
		assertEquals("toto@earth.net", q.getEmail());
		assertEquals("CH2800767000U09565735", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testFormatV3_2() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final DossierElectroniqueHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler v1");
			}
		};
		final DossierElectroniqueHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler v2");
			}
		};
		final DossierElectroniqueHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final DossierElectroniqueHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final DossierElectroniqueHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/DossierElectronique-3.2-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(10500171, q.getNoContribuable());
		assertEquals(2014, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.VAUDTAX, q.getTypeDocument());
		assertEquals("toto@earth.net", q.getEmail());
		assertEquals("CH2800767000U09565735", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}



	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testFormatPf2015_V2() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final DossierElectroniqueHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2015 v2 ne devrait pas arriver dans le handler v1");
			}
		};
		final DossierElectroniqueHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2015 v2 ne devrait pas arriver dans le handler v2");
			}
		};
		final DossierElectroniqueHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2015 v2 ne devrait pas arriver dans le handler v3");
			}
		};
		final DossierElectroniqueHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message periode 2015 v2 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final DossierElectroniqueHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/DossierElectronique-2015.2-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(10500171, q.getNoContribuable());
		assertEquals(2015, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.VAUDTAX, q.getTypeDocument());
		assertEquals("toto@earth.net", q.getEmail());
		assertEquals("CH2800767000U09565735", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testFormatPf2016_V1() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final DossierElectroniqueHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v1 ne devrait pas arriver dans le handler v1");
			}
		};
		final DossierElectroniqueHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v1 ne devrait pas arriver dans le handler v2");
			}
		};
		final DossierElectroniqueHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v1 ne devrait pas arriver dans le handler v3");
			}
		};
		final DossierElectroniqueHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};
		final DossierElectroniqueHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message periode 2016 v1 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/DossierElectronique-2016.1-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(10500171, q.getNoContribuable());
		assertEquals(2016, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.VAUDTAX, q.getTypeDocument());
		assertEquals("toto@earth.net", q.getEmail());
		assertEquals("CH2800767000U09565735", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testFormatPf2016_V2() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final DossierElectroniqueHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler v1");
			}
		};
		final DossierElectroniqueHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler v2");
			}
		};
		final DossierElectroniqueHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler v3");
			}
		};
		final DossierElectroniqueHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final DossierElectroniqueHandler<?> pf2017v1Handler = new Pf2017V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler Periode 2017 v1");
			}
		};
		final DossierElectroniqueHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};

		final DossierElectroniqueHandler<?> pf2016v2Handler = new Pf2016V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};


		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler,pf2016v2Handler,pf2017v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/DossierElectronique-2016.2-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(10500171, q.getNoContribuable());
		assertEquals(2016, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.VAUDTAX, q.getTypeDocument());
		assertEquals("toto@earth.net", q.getEmail());
		assertEquals("CH2800767000U09565735", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testFormatPf2017_V1() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final DossierElectroniqueHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler v1");
			}
		};
		final DossierElectroniqueHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler v2");
			}
		};
		final DossierElectroniqueHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler v3");
			}
		};
		final DossierElectroniqueHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final DossierElectroniqueHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final DossierElectroniqueHandler<?> pf2017v1Handler = new Pf2017V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};


		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler,pf2017v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/cedi/DossierElectronique-2017.1-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final RetourDI q = (RetourDI) events.get(0);
		assertNotNull(q);
		assertEquals(10500171, q.getNoContribuable());
		assertEquals(2017, q.getPeriodeFiscale());
		assertEquals(1, q.getNoSequenceDI());
		assertEquals(RetourDI.TypeDocument.VAUDTAX, q.getTypeDocument());
		assertEquals("toto@earth.net", q.getEmail());
		assertEquals("CH2800767000U09565735", q.getIban());
		assertEquals("0211234567", q.getNoTelephone());
		assertEquals("0797654321", q.getNoMobile());
		assertEquals("Toto le rigolo", q.getTitulaireCompte());
	}
}
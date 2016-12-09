package ch.vd.uniregctb.evenement.retourdi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.evenement.retourdi.pm.AdresseRaisonSociale;
import ch.vd.uniregctb.evenement.retourdi.pm.Localisation;
import ch.vd.uniregctb.evenement.retourdi.pp.EvenementCedi;
import ch.vd.uniregctb.evenement.retourdi.pp.EvenementCediException;
import ch.vd.uniregctb.evenement.retourdi.pp.Pf2015V2Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.Pf2016V1Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.Pf2016V2Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.Pf2017V1Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.RetourDI;
import ch.vd.uniregctb.evenement.retourdi.pp.V1Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.V2Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.V3Handler;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Classe de test du listener d'événements de retour de scan de DI (en provenance du CEDI, de ADDI ou, de e-DIPM). Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementRetourDiEsbMessageHandlerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementRetourDiEsbMessageHandler esbHandler;

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

		esbHandler = new EvenementRetourDiEsbMessageHandler();
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

				new ClassPathResource("event/taxation/DeclarationIBC-1.xsd"),
				new ClassPathResource("event/taxation/DeclarationIBC-2.xsd"),
		});

		initEndpointManager(INPUT_QUEUE, listener);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveRetourDI() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();

		esbHandler.setHandlers(Collections.<RetourDiHandler<?>>singletonList(new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) {
				events.add(evt);
			}
		}));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/retour_di.xml");
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

		esbHandler.setHandlers(Collections.<RetourDiHandler<?>>singletonList(new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) {
				events.add(evt);
			}
		}));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/retour_di_presque_vide.xml");
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
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v2 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v2 ne devrait pas arriver dans le handler v3");
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v2 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v2 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-2.0-exemple.xml");
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
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler v2");
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-3.0-exemple.xml");
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
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler v2");
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message v3 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-3.2-exemple.xml");
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
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2015 v2 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2015 v2 ne devrait pas arriver dans le handler v2");
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2015 v2 ne devrait pas arriver dans le handler v3");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message periode 2015 v2 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-2015.2-exemple.xml");
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
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v1 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v1 ne devrait pas arriver dans le handler v2");
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v1 ne devrait pas arriver dans le handler v3");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message periode 2016 v1 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-2016.1-exemple.xml");
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
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler v2");
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler v3");
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final RetourDiHandler<?> pf2017v1Handler = new Pf2017V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler Periode 2017 v1");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2016 v2 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};

		final RetourDiHandler<?> pf2016v2Handler = new Pf2016V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};


		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler,pf2016v2Handler,pf2017v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-2016.2-exemple.xml");
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
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler v2");
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler v3");
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message Periode 2017 v1 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final RetourDiHandler<?> pf2017v1Handler = new Pf2017V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				events.add(evt);
			}
		};


		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler,pf2015v2Handler,pf2016v1Handler,pf2017v1Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-2017.1-exemple.xml");
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

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testIBC_V1() throws Exception {

		final List<ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> events = new ArrayList<>();
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v1 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v1 ne devrait pas arriver dans le handler v2");
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v1 ne devrait pas arriver dans le handler v3");
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v1 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v1 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final RetourDiHandler<?> pf2017v1Handler = new Pf2017V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v1 ne devrait pas arriver dans le handler Periode 2017 v1");
			}
		};
		final RetourDiHandler<?> ibcv1Handler = new ch.vd.uniregctb.evenement.retourdi.pm.V1Handler() {
			@Override
			protected void traiterRetour(ch.vd.uniregctb.evenement.retourdi.pm.RetourDI retour, Map<String, String> headers) throws EsbBusinessException {
				events.add(retour);
			}
		};
		final RetourDiHandler<?> ibcv2Handler = new ch.vd.uniregctb.evenement.retourdi.pm.V2Handler() {
			@Override
			protected void traiterRetour(ch.vd.uniregctb.evenement.retourdi.pm.RetourDI retour, Map<String, String> headers) throws EsbBusinessException {
				Assert.fail("Un message IBC v1 ne devrait pas arriver dans le handler IBC v2");
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler, pf2015v2Handler, pf2016v1Handler, pf2017v1Handler, ibcv1Handler, ibcv2Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pm/IBC-1-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final ch.vd.uniregctb.evenement.retourdi.pm.RetourDI q = events.get(0);
		assertNotNull(q);
		assertEquals(518L, q.getNoCtb());
		assertEquals(2016, q.getPf());
		assertEquals(1, q.getNoSequence());

		assertNotNull(q.getEntreprise());
		assertEquals("CH78005540A1024502601", q.getEntreprise().getIban());
		assertEquals("Jörg Åström", q.getEntreprise().getTitulaireCompteBancaire());
		assertEquals(RegDate.get(2015, 12, 31), q.getEntreprise().getDateFinExerciceCommercial());
		assertNotNull(q.getEntreprise().getSiege());
		assertEquals(Localisation.CommuneSuisse.class, q.getEntreprise().getSiege().getClass());
		assertEquals(5586, ((Localisation.CommuneSuisse) q.getEntreprise().getSiege()).getNoOfsCommune());
		assertNotNull(q.getEntreprise().getAdresseCourrier());
		assertEquals(AdresseRaisonSociale.StructureeSuisse.class, q.getEntreprise().getAdresseCourrier().getClass());
		assertEquals("1000", ((AdresseRaisonSociale.StructureeSuisse) q.getEntreprise().getAdresseCourrier()).getNumeroPostal());
		assertEquals("Lausanne", ((AdresseRaisonSociale.StructureeSuisse) q.getEntreprise().getAdresseCourrier()).getLocalite());
		assertEquals("11", ((AdresseRaisonSociale.StructureeSuisse) q.getEntreprise().getAdresseCourrier()).getNumero());
		assertEquals("Chemin de Bellevue", ((AdresseRaisonSociale.StructureeSuisse) q.getEntreprise().getAdresseCourrier()).getRue());

		assertNotNull(q.getMandataire());
		assertNull(q.getMandataire().getIdeMandataire());       // le numéro IDE du mandataire, même valide, est maintenant ignoré...
		assertEquals(Boolean.TRUE, q.getMandataire().getSansCopieMandataire());
		assertEquals("0218887766", q.getMandataire().getNoTelContact());
		assertNotNull(q.getMandataire().getAdresse());
		assertEquals(AdresseRaisonSociale.Brutte.class, q.getMandataire().getAdresse().getClass());
		assertEquals("Mon bon soldat", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne1());
		assertNull(((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne2());
		assertEquals("Ou pas", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne3());
		assertNull(((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne4());
		assertNull(((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne5());
		assertEquals("1004", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getNpa());
		assertEquals("Lausanne", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLocalite());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testIBC_V2() throws Exception {

		final List<ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> events = new ArrayList<>();
		final RetourDiHandler<?> v1Handler = new V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v2 ne devrait pas arriver dans le handler v1");
			}
		};
		final RetourDiHandler<?> v2Handler = new V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v2 ne devrait pas arriver dans le handler v2");
			}
		};
		final RetourDiHandler<?> v3Handler = new V3Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v2 ne devrait pas arriver dans le handler v3");
			}
		};
		final RetourDiHandler<?> pf2015v2Handler = new Pf2015V2Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v2 ne devrait pas arriver dans le handler Periode 2015 v2");
			}
		};
		final RetourDiHandler<?> pf2016v1Handler = new Pf2016V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v2 ne devrait pas arriver dans le handler Periode 2016 v1");
			}
		};
		final RetourDiHandler<?> pf2017v1Handler = new Pf2017V1Handler() {
			@Override
			protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
				Assert.fail("Un message IBC v2 ne devrait pas arriver dans le handler Periode 2017 v1");
			}
		};
		final RetourDiHandler<?> ibcv1Handler = new ch.vd.uniregctb.evenement.retourdi.pm.V1Handler() {
			@Override
			protected void traiterRetour(ch.vd.uniregctb.evenement.retourdi.pm.RetourDI retour, Map<String, String> headers) throws EsbBusinessException {
				Assert.fail("Un message IBC v2 ne devrait pas arriver dans le handler IBC v1");
			}
		};
		final RetourDiHandler<?> ibcv2Handler = new ch.vd.uniregctb.evenement.retourdi.pm.V2Handler() {
			@Override
			protected void traiterRetour(ch.vd.uniregctb.evenement.retourdi.pm.RetourDI retour, Map<String, String> headers) throws EsbBusinessException {
				events.add(retour);
			}
		};

		esbHandler.setHandlers(Arrays.asList(v1Handler, v2Handler, v3Handler, pf2015v2Handler, pf2016v1Handler, pf2017v1Handler, ibcv1Handler, ibcv2Handler));
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pm/IBC-2-exemple.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final ch.vd.uniregctb.evenement.retourdi.pm.RetourDI q = events.get(0);
		assertNotNull(q);
		assertEquals(518L, q.getNoCtb());
		assertEquals(2016, q.getPf());
		assertEquals(1, q.getNoSequence());

		assertNotNull(q.getEntreprise());
		assertEquals("CH78005540A1024502601", q.getEntreprise().getIban());
		assertEquals("Jörg Åström", q.getEntreprise().getTitulaireCompteBancaire());
		assertEquals(RegDate.get(2015, 12, 31), q.getEntreprise().getDateFinExerciceCommercial());
		assertNotNull(q.getEntreprise().getSiege());
		assertEquals(Localisation.CommuneSuisse.class, q.getEntreprise().getSiege().getClass());
		assertEquals(5586, ((Localisation.CommuneSuisse) q.getEntreprise().getSiege()).getNoOfsCommune());
		assertNotNull(q.getEntreprise().getAdresseCourrier());
		assertEquals(AdresseRaisonSociale.StructureeSuisse.class, q.getEntreprise().getAdresseCourrier().getClass());
		assertEquals("1000", ((AdresseRaisonSociale.StructureeSuisse) q.getEntreprise().getAdresseCourrier()).getNumeroPostal());
		assertEquals("Lausanne", ((AdresseRaisonSociale.StructureeSuisse) q.getEntreprise().getAdresseCourrier()).getLocalite());
		assertEquals("11", ((AdresseRaisonSociale.StructureeSuisse) q.getEntreprise().getAdresseCourrier()).getNumero());
		assertEquals("Chemin de Bellevue", ((AdresseRaisonSociale.StructureeSuisse) q.getEntreprise().getAdresseCourrier()).getRue());

		assertNotNull(q.getMandataire());
		assertNull(q.getMandataire().getIdeMandataire());       // le numéro IDE du mandataire, même valide, est maintenant ignoré...
		assertEquals(Boolean.TRUE, q.getMandataire().getSansCopieMandataire());
		assertEquals("0218887766", q.getMandataire().getNoTelContact());
		assertNotNull(q.getMandataire().getAdresse());
		assertEquals(AdresseRaisonSociale.Brutte.class, q.getMandataire().getAdresse().getClass());
		assertEquals("Mon bon soldat", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne1());
		assertNull(((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne2());
		assertEquals("Ou pas", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne3());
		assertNull(((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne4());
		assertNull(((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLigne5());
		assertEquals("1004", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getNpa());
		assertEquals("Lausanne", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLocalite());
	}

}
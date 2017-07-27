package ch.vd.uniregctb.evenement.retourdi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
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
import ch.vd.uniregctb.evenement.retourdi.pp.Pf2017V2Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.Pf2018V1Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.RetourDI;
import ch.vd.uniregctb.evenement.retourdi.pp.V1Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.V2Handler;
import ch.vd.uniregctb.evenement.retourdi.pp.V3Handler;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.jms.EsbBusinessException;

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

		final Consumer<Object> noop = data -> {};
		final List<Resource> resources = new ArrayList<>();
		for (XmlVersionPP pp : XmlVersionPP.values()) {
			final RetourDiHandler<?> handler = pp.buildHandler(noop);
			resources.add(handler.getRequestXSD());
		}
		for (XmlVersionPM pm : XmlVersionPM.values()) {
			final RetourDiHandler<?> handler = pm.buildHandler(noop);
			resources.add(handler.getRequestXSD());
		}
		buildEsbMessageValidator(resources.toArray(new Resource[resources.size()]));

		initListenerContainer(INPUT_QUEUE, esbHandler);
	}

	/**
	 * Différentes versions supportées de la XSD PP
	 */
	private enum XmlVersionPP {
		V1 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new V1Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		V2 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new V2Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		V3 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new V3Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		PF2015_2 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new Pf2015V2Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		PF2016_1 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new Pf2016V1Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		PF2016_2 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new Pf2016V2Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		PF2017_1 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new Pf2017V1Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		PF2017_2 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new Pf2017V2Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		PF2018_1 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action) {
				return new Pf2018V1Handler() {
					@Override
					protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
						action.accept(evt);
					}
				};
			}
		},
		;

		/**
		 * constructeur de handler pour une version de la XSD, qui appelle le {@link Consumer} fourni
		 * @param action action lancée par le handler construit
		 */
		public abstract RetourDiHandler<?> buildHandler(Consumer<? super EvenementCedi> action);

		@Override
		public String toString() {
			return "XmlVersionPP." + name();
		}
	}

	/**
	 * Différentes versions supportées de la XSD PM
	 */
	private enum XmlVersionPM {
		V1 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> action) {
				return new ch.vd.uniregctb.evenement.retourdi.pm.V1Handler() {
					@Override
					protected void traiterRetour(ch.vd.uniregctb.evenement.retourdi.pm.RetourDI retour, Map<String, String> headers) throws EsbBusinessException {
						action.accept(retour);
					}
				};
			}
		},
		V2 {
			@Override
			public RetourDiHandler<?> buildHandler(Consumer<? super ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> action) {
				return new ch.vd.uniregctb.evenement.retourdi.pm.V2Handler() {
					@Override
					protected void traiterRetour(ch.vd.uniregctb.evenement.retourdi.pm.RetourDI retour, Map<String, String> headers) throws EsbBusinessException {
						action.accept(retour);
					}
				};
			}
		},
		;

		/**
		 * constructeur de handler pour une version de la XSD, qui appelle le {@link Consumer} fourni
		 * @param action action lancée par le handler construit
		 */
		public abstract RetourDiHandler<?> buildHandler(Consumer<? super ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> action);

		@Override
		public String toString() {
			return "XmlVersionPM." + name();
		}
	}

	/**
	 * Construction de la liste complète des handlers pour toutes les versions de XSD supportées, dans laquelle seul le handler
	 * pour la version dite 'acceptée' remplira la collection fournie (les autres explosent car ils ne sont pas sensés être utilisés)
	 * @param accepted version de la XSD PP acceptée
	 * @param collector collection qui récupérera tous les événements correspondant à la version acceptée
	 * @return une liste de tous les handlers
	 */
	private static List<RetourDiHandler<?>> buildHandlers(XmlVersionPP accepted, List<EvenementCedi> collector) {
		final List<RetourDiHandler<?>> list = new ArrayList<>();
		for (XmlVersionPP pp : XmlVersionPP.values()) {
			final Consumer<EvenementCedi> consumer;
			if (pp == accepted) {
				consumer = collector::add;
			}
			else {
				consumer = evt -> Assert.fail("Un message " + accepted + " ne devrait pas arriver dans le canal " + pp);
			}
			list.add(pp.buildHandler(consumer));
		}
		for (XmlVersionPM pm : XmlVersionPM.values()) {
			final Consumer<ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> consumer = evt -> Assert.fail("un message " + accepted + " ne devrait pas arriver dans le canal " + pm);
			list.add(pm.buildHandler(consumer));
		}
		return list;
	}

	/**
	 * Construction de la liste complète des handlers pour toutes les versions de XSD supportées, dans laquelle seul le handler
	 * pour la version dite 'acceptée' remplira la collection fournie (les autres explosent car ils ne sont pas sensés être utilisés)
	 * @param accepted version de la XSD PM acceptée
	 * @param collector collection qui récupérera tous les événements correspondant à la version acceptée
	 * @return une liste de tous les handlers
	 */
	private static List<RetourDiHandler<?>> buildHandlers(XmlVersionPM accepted, List<ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> collector) {
		final List<RetourDiHandler<?>> list = new ArrayList<>();
		for (XmlVersionPM pm : XmlVersionPM.values()) {
			final Consumer<ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> consumer;
			if (pm == accepted) {
				consumer = collector::add;
			}
			else {
				consumer = evt -> Assert.fail("Un message " + accepted + " ne devrait pas arriver dans le canal " + pm);
			}
			list.add(pm.buildHandler(consumer));
		}
		for (XmlVersionPP pp : XmlVersionPP.values()) {
			final Consumer<EvenementCedi> consumer = evt -> Assert.fail("un message " + accepted + " ne devrait pas arriver dans le canal " + pp);
			list.add(pp.buildHandler(consumer));
		}
		return list;
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveRetourDI() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.V1, events);
		esbHandler.setHandlers(handlers);
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.V1, events);
		esbHandler.setHandlers(handlers);
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.V2, events);
		esbHandler.setHandlers(handlers);
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.V3, events);
		esbHandler.setHandlers(handlers);
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.V3, events);
		esbHandler.setHandlers(handlers);
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.PF2015_2, events);
		esbHandler.setHandlers(handlers);
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.PF2016_1, events);
		esbHandler.setHandlers(handlers);
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.PF2016_2, events);
		esbHandler.setHandlers(handlers);
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.PF2017_1, events);
		esbHandler.setHandlers(handlers);
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
	public void testFormatPf2017_V2() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.PF2017_2, events);
		esbHandler.setHandlers(handlers);
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-2017.2-exemple.xml");
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
	public void testFormatPf2018_V1() throws Exception {

		final List<EvenementCedi> events = new ArrayList<>();
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPP.PF2018_1, events);
		esbHandler.setHandlers(handlers);
		esbHandler.afterPropertiesSet();

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/retourdi/pp/DossierElectronique-2018.1-exemple.xml");
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
		assertEquals(2018, q.getPeriodeFiscale());
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
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPM.V1, events);
		esbHandler.setHandlers(handlers);
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
		assertNull(((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getPersonneContact());
		assertEquals("1004", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getNpa());
		assertEquals("Lausanne", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLocalite());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testIBC_V2() throws Exception {

		final List<ch.vd.uniregctb.evenement.retourdi.pm.RetourDI> events = new ArrayList<>();
		final List<RetourDiHandler<?>> handlers = buildHandlers(XmlVersionPM.V2, events);
		esbHandler.setHandlers(handlers);
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
		assertNull(((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getPersonneContact());
		assertEquals("1004", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getNpa());
		assertEquals("Lausanne", ((AdresseRaisonSociale.Brutte) q.getMandataire().getAdresse()).getLocalite());
	}

}
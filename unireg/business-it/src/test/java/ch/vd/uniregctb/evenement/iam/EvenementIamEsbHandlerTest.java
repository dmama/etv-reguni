package ch.vd.uniregctb.evenement.iam;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.util.ResourceUtils;

import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.type.ModeCommunication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Classe de test du listener d'événements IAM. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Baba NGOM  <baba-issa.ngom@vd.ch>
 */
public class EvenementIamEsbHandlerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementIamEsbHandler handler;

	public void setUp() throws Exception {
		super.setUp();

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtIAM");

		clearQueue(INPUT_QUEUE);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplateImpl() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		handler = new EvenementIamEsbHandler();
		handler.setHibernateTemplate(hibernateTemplate);

		initListenerContainer(INPUT_QUEUE, handler);

		buildEsbMessageValidator(new Resource[]{new ClassPathResource("xsd/iam/messageIAM_EMPIS.xsd")});
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveEnregistrementEmployeur() throws Exception {

		final List<EvenementIAM> events = new ArrayList<>();

		handler.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_employeur.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<>();
		customAttributes.put(EvenementIamEsbHandler.ACTION, EvenementIamEsbHandler.CREATE);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur);
		final InfoEmployeur q = enregistrementEmployeur.getEmployeursAMettreAJour().get(0);
		assertEquals(1038580L, q.getNoEmployeur());
		assertEquals(20L, q.getLogicielId().longValue());
		assertEquals(ModeCommunication.SITE_WEB, q.getModeCommunication());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceive2EnregistrementEmployeur() throws Exception {

		final List<EvenementIAM> events = new ArrayList<>();

		handler.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_2employeur.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<>();
		customAttributes.put(EvenementIamEsbHandler.ACTION, EvenementIamEsbHandler.CREATE);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur1 = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur1);
		final InfoEmployeur q1 = enregistrementEmployeur1.getEmployeursAMettreAJour().get(0);
		assertEquals(1038580L, q1.getNoEmployeur());
		assertEquals(20L, q1.getLogicielId().longValue());
		assertEquals(ModeCommunication.SITE_WEB, q1.getModeCommunication());

		final InfoEmployeur q2 = enregistrementEmployeur1.getEmployeursAMettreAJour().get(1);
		assertEquals(1038640L, q2.getNoEmployeur());
		assertEquals(15L, q2.getLogicielId().longValue());
		assertEquals(ModeCommunication.ELECTRONIQUE, q2.getModeCommunication());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveEnregistrementEmployeurSansIdLogicielSansTypAcces() throws Exception {

		final List<EvenementIAM> events = new ArrayList<>();

		handler.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_employeurSansIdLogSansAcces.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<>();
		customAttributes.put(EvenementIamEsbHandler.ACTION, EvenementIamEsbHandler.CREATE);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur1 = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur1);
		final InfoEmployeur q1 = enregistrementEmployeur1.getEmployeursAMettreAJour().get(0);
		assertEquals(1038580L, q1.getNoEmployeur());
		assertEquals(null, q1.getLogicielId());
		assertEquals(null, q1.getModeCommunication());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveEnregistrementMinimal() throws Exception {

		final List<EvenementIAM> events = new ArrayList<>();

		handler.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_employeurMinimal.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<>();
		customAttributes.put(EvenementIamEsbHandler.ACTION, EvenementIamEsbHandler.UPDATE);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur1 = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur1);
		assertNull(enregistrementEmployeur1.getEmployeursAMettreAJour());
	}


	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveEnregistrementMinimalAvecInfoMetier() throws Exception {

		final List<EvenementIAM> events = new ArrayList<>();

		handler.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_employeur_infoMetier_minimal.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<>();
		customAttributes.put(EvenementIamEsbHandler.ACTION, EvenementIamEsbHandler.CREATE);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur1 = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur1);
		assertNotNull(enregistrementEmployeur1.getEmployeursAMettreAJour());
		final InfoEmployeur q1 = enregistrementEmployeur1.getEmployeursAMettreAJour().get(0);
		assertEquals(1038580L, q1.getNoEmployeur());
		assertEquals(null, q1.getLogicielId());
		assertEquals(null, q1.getModeCommunication());
	}


	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveActionCreate() throws Exception {

		final List<EvenementIAM> events = new ArrayList<>();

		handler.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_employeur.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<>();
		customAttributes.put(EvenementIamEsbHandler.ACTION, EvenementIamEsbHandler.CREATE);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur);
		final InfoEmployeur q = enregistrementEmployeur.getEmployeursAMettreAJour().get(0);
		assertEquals(1038580L, q.getNoEmployeur());
		assertEquals(20L, q.getLogicielId().longValue());
		assertEquals(ModeCommunication.SITE_WEB, q.getModeCommunication());
	}


	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveActionDelete() throws Exception {

		final List<EvenementIAM> events = new ArrayList<>();

		handler.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_employeur.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<>();
		customAttributes.put(EvenementIamEsbHandler.ACTION, EvenementIamEsbHandler.DELETE);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend .un hypothétique message
		Thread.sleep(1000);
		Assert.assertEquals(0, events.size());

	}


	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveActionUpdate() throws Exception {

		final List<EvenementIAM> events = new ArrayList<>();

		handler.setHandler(new EvenementIAMHandler() {
			@Override
			public void onEvent(EvenementIAM event) {
				events.add(event);
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/evenement/iam/enregistrement_employeur.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		final Map<String, String> customAttributes = new HashMap<>();
		customAttributes.put(EvenementIamEsbHandler.ACTION, EvenementIamEsbHandler.UPDATE);
		sendTextMessage(INPUT_QUEUE, texte, customAttributes);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EnregistrementEmployeur enregistrementEmployeur = (EnregistrementEmployeur) events.get(0);
		assertNotNull(enregistrementEmployeur);
		final InfoEmployeur q = enregistrementEmployeur.getEmployeursAMettreAJour().get(0);
		assertEquals(1038580L, q.getNoEmployeur());
		assertEquals(20L, q.getLogicielId().longValue());
		assertEquals(ModeCommunication.SITE_WEB, q.getModeCommunication());
	}


}

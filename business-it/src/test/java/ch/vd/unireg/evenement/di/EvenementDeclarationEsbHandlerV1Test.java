package ch.vd.unireg.evenement.di;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.hibernate.HibernateTemplateImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements Declaration. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
public class EvenementDeclarationEsbHandlerV1Test extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementDeclarationEsbHandlerV1 handler;

	public void setUp() throws Exception {
		super.setUp();

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtDeclaration");

		clearQueue(INPUT_QUEUE);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplateImpl() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		handler = new EvenementDeclarationEsbHandlerV1();
		handler.setHibernateTemplate(hibernateTemplate);
		handler.afterPropertiesSet();

		initListenerContainer(INPUT_QUEUE, handler);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("unireg-common-1.xsd"),
				new ClassPathResource("/event/di/evenementDeclarationImpot-input-1.xsd"),
				new ClassPathResource("/event/di/evenementDeclarationImpot-common-1.xsd")
		});
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceiveQuittancementDI() throws Exception {

		final List<EvenementDeclaration> events = new ArrayList<>();

		handler.setHandler(new EvenementDeclarationHandler() {
			@Override
			public void onEvent(EvenementDeclaration event, Map<String, String> incomingHeaders) {
				events.add(event);
			}

			@Override
			public ClassPathResource getRequestXSD() {
				return new ClassPathResource("/event/di/evenementDeclarationImpot-input-1.xsd");
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/evenement/di/quittancementStandard.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendTextMessage(INPUT_QUEUE, texte);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final QuittancementDI q = (QuittancementDI) events.get(0);
		assertNotNull(q);
		assertEquals(12344556L, q.getNumeroContribuable());
		assertEquals(2010, q.getPeriodeFiscale());
		assertEquals("ADDI", q.getSource());
		assertEquals(RegDate.get(2011, 5, 26), q.getDate());

	}

}

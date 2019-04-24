package ch.vd.unireg.efacture;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementHelper;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.jms.EsbBusinessErrorHandler;
import ch.vd.unireg.jms.GentilEsbMessageListenerContainer;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;

public class EFactureMessageHandlerItTest extends BusinessItTest {

	private String INPUT_QUEUE;
	private EsbJmsTemplate esbTemplate;
	private EFactureMessageHandler handler;
	private GentilEsbMessageListenerContainer listener;
	private List<Demande> evenementsRecus;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtEfacture.input");
		EvenementHelper.clearQueue(esbTemplate, INPUT_QUEUE, transactionManager);

		evenementsRecus = new ArrayList<>();

		handler = new EFactureMessageHandler();
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setHandler(new EFactureEventHandlerImpl() {
			@Override
			public void handle(Demande event) {
				evenementsRecus.add(event);
			}
		});
		handler.afterPropertiesSet();

		listener = new GentilEsbMessageListenerContainer();
		listener.setEsbTemplate(esbTemplate);
		listener.setEsbErrorHandler(getBean(EsbBusinessErrorHandler.class, "esbErrorHandler"));
		listener.setTransactionManager(transactionManager);
		listener.setConnectionFactory(getBean(ConnectionFactory.class, "jmsConnectionFactory"));
		listener.setHandler(handler);
		listener.setDestinationName(INPUT_QUEUE);
		listener.afterPropertiesSet();
		listener.start();
	}

	@Override
	public void onTearDown() throws Exception {
		if (listener != null) {
			listener.stop();
			listener.destroy();
		}
		super.onTearDown();
	}

	/**
	 * Teste qu'une demande d'inscription est bien traitée dans le cas passant.
	 */
	@Test(timeout = 20000)
	public void testInscription() throws Exception {

		final long idPP1 = 10000010L;

		// mise en place, 1 contribuable
		doInNewTransaction(status -> {
			addNonHabitant(idPP1, "Un", "Contribuable", date(1970, 1, 1), Sexe.MASCULIN);
			return null;
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/evenement/efacture/inscription_efacture.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		EvenementHelper.sendTextMessage(esbTemplate, INPUT_QUEUE, texte, "businessId", null, transactionManager);

		// On attend que l'événement soit reçu
		Demande d;
		do {
			d = evenementsRecus.isEmpty() ? null : evenementsRecus.get(0);
			Thread.sleep(100);
		}
		while (d == null);

		// On vérifie que la demande est bien décodée
		assertEquals("1234", d.getIdDemande());
		assertEquals(10000010L, d.getCtbId());
		assertEquals(RegDate.get(2019, 4, 18), d.getDateDemande());
		assertEquals("un.contribuable@example.com", d.getEmail());
		assertEquals(BigInteger.valueOf(41100000000000000L), d.getNoAdherent());
		assertEquals("7560000000000", d.getNoAvs());
	}

}
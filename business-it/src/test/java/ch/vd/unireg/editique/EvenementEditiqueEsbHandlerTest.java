package ch.vd.unireg.editique;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.editique.impl.EvenementEditiqueEsbHandler;
import ch.vd.unireg.evenement.EvenementTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener d'événements des retours d'impression de l'éditique.
 * Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
public class EvenementEditiqueEsbHandlerTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementEditiqueEsbHandler handler;

	private static final String DI_ID = "2004 01 062901707 20100817145346417";

	public void setUp() throws Exception {
		super.setUp();

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.editique.input");

		clearQueue(INPUT_QUEUE);

		handler = new EvenementEditiqueEsbHandler();
		initListenerContainer(INPUT_QUEUE, handler);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRetourImpression() throws Exception {

		final List<EditiqueResultat> events = new ArrayList<>();

		handler.setStorageService(new EditiqueRetourImpressionStorageService() {
			@Override
			public void onArriveeRetourImpression(EditiqueResultatRecu resultat) {
				events.add(resultat);
			}

			@Override
			public void registerTrigger(String nomDocument, RetourImpressionTrigger trigger) {
				throw new NotImplementedException("");
			}

			@Override
			public EditiqueResultat getDocument(String nomDocument, Duration timeout) {
				throw new NotImplementedException("");
			}

			@Override
			public int getDocumentsRecus() {
				throw new NotImplementedException("");
			}

			@Override
			public int getDocumentsEnAttenteDeDispatch() {
				throw new NotImplementedException("");
			}

			@Override
			public int getCleanupPeriod() {
				throw new NotImplementedException("");
			}

			@Override
			public int getDocumentsPurges() {
				throw new NotImplementedException("");
			}

			@Override
			public Date getDateDernierePurgeEffective() {
				throw new NotImplementedException("");
			}

			@Override
			public Collection<Pair<Long, RetourImpressionTrigger>> getTriggersEnregistres() {
				throw new NotImplementedException("");
			}
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/editique/retour_impression.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		sendDiMessage(INPUT_QUEUE, texte, DI_ID);

		// On attend le message
		while (events.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, events.size());

		final EditiqueResultat q = events.get(0);
		assertNotNull(q);
		assertEquals(DI_ID, q.getIdDocument());
	}

	private void sendDiMessage(String queueName, String texte, String idDocument) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		m.addHeader(ConstantesEditique.UNIREG_DOCUMENT_ID, idDocument);
		m.addHeader(ConstantesEditique.UNIREG_FORMAT_DOCUMENT, FormatDocumentEditique.PDF.name());
		m.addHeader(ConstantesEditique.UNIREG_TYPE_DOCUMENT, TypeDocumentEditique.CONFIRMATION_DELAI.name());

		// pas de validation car pas de namespace dans la XSD
		// esbValidator.validate(m);

		esbTemplate.send(m);
	}
}

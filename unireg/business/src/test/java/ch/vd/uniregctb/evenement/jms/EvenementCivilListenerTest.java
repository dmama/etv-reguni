package ch.vd.uniregctb.evenement.jms;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.AuditLine;
import ch.vd.uniregctb.audit.AuditLineDAO;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.engine.EvenementCivilAsyncProcessor;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author xsilbn
 *
 */
public class EvenementCivilListenerTest extends BusinessTest {

	private EvenementCivilListener evenementCivilListener;
	private EvenementCivilDAO evenementCivilDAO;
	private AuditLineDAO auditLineDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {

			@Override
			protected void init() {
				MockIndividu bastien = addIndividu(123, RegDate.get(1970, 5, 5), "Malorme", "Bastien", true);
				MockIndividu julienne = addIndividu(124, RegDate.get(1973, 11, 13), "Malorme", "Julienne", false);
				MockIndividu bebe = addIndividu(125, RegDate.get(2008, 2, 2), "Malorme", "Alain", true);

				addAdresse(bastien, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(julienne, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(bebe, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);

				marieIndividus(bastien, julienne, RegDate.get(2007, 2, 3));
			}
		});

		evenementCivilDAO = getBean(EvenementCivilDAO.class, "evenementCivilDAO");
		auditLineDAO = getBean(AuditLineDAO.class, "auditLineDAO");

		final EvenementCivilAsyncProcessor evtCivilProcessor = getBean(EvenementCivilAsyncProcessor.class, "evenementCivilAsyncProcessor");

		// On crée le listener à la main pour pouvoir appeler les méthodes protégées
		evenementCivilListener = new EvenementCivilListener();
		evenementCivilListener.setEvenementCivilAsyncProcessor(evtCivilProcessor);
		evenementCivilListener.setTransactionManager(transactionManager);
		evenementCivilListener.setEvenementCivilDAO(evenementCivilDAO);
	}

	@Override
	public void onTearDown() throws Exception {
		evenementCivilListener.sync(); // pour s'assurer que tous les événemens ont été traités (même en cas de timeout) avant de resetter le service civil.
		super.onTearDown();
	}

	public static String createMessage(int id, int type, long noInd, RegDate dateEvt, int ofs) throws Exception {

		final String dateStr = RegDateHelper.dateToDashString(dateEvt);

		final StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xmlContent.append("<EvtRegCivil xmlns=\"http://www.vd.ch/schema/registreCivil/20070914/EvtRegCivil\">");
		xmlContent.append("<NoTechnique>").append(id).append("</NoTechnique>");
		xmlContent.append("<Code>").append(type).append("</Code>");
		xmlContent.append("<NoIndividu>").append(noInd).append("</NoIndividu>");
		xmlContent.append("<DateEvenement>").append(dateStr).append("</DateEvenement>");
		xmlContent.append("<DateTraitement>").append(dateStr).append("</DateTraitement>");
		xmlContent.append("<NumeroOFS>").append(ofs).append("</NumeroOFS>");
		xmlContent.append("</EvtRegCivil>");

		return xmlContent.toString();
	}

	@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter"})
	private void sendMessageSync(String xmlContent) throws EvenementCivilException, InterruptedException {
		sendMessageSync(evenementCivilListener, xmlContent);
	}

	@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter"})
	public static void sendMessageSync(EvenementCivilListener listener, String xmlContent) throws EvenementCivilException, InterruptedException {
		listener.onEvenementCivil(xmlContent);
		listener.sync();
	}

	@Test(timeout = 10000)
	public void testOnMessageGood() throws Exception {

		final int id = 54;
		final String xmlContent = createMessage(id, 1000, 125, RegDate.get(2008, 2, 12), 5586);

		// Envoi du contenu à EvenementCivilDatabaseInserter
		sendMessageSync(xmlContent);

		// test que l'evenement est bien la
		final List<EvenementCivilData> ecr = evenementCivilDAO.getAll();
		assertEquals(1, ecr.size());

		final EvenementCivilData evenement = ecr.get(0);
		assertEquals(EtatEvenementCivil.TRAITE, evenement.getEtat());
		assertTrue("La date de l'événement n'a pas été récupérée correctement", RegDate.get(2008, 2, 12).equals(evenement.getDateEvenement()));
		assertEquals("Le numéro technique n'a pas été récupéré correctement", new Long(id), evenement.getId());
		assertEquals("Le type d'événement n'a pas été récupéré correctement", TypeEvenementCivil.NAISSANCE, evenement.getType());
		assertEquals("Le numéro d'individu n'a pas été récupéré correctement", new Long(125L), evenement.getNumeroIndividuPrincipal());
		assertEquals("Le numéro OFS n'a pas été récupéré correctement", new Integer(5586), evenement.getNumeroOfsCommuneAnnonce());
	}

	/**
	 * @throws Exception
	 */
	@Test(timeout = 10000)
	public void testOnMessageBad() throws Exception {

		final int id = 54;
		final String xmlContent = createMessage(id, 1000, 9876543, RegDate.get(2007, 9, 18), 111); // no individu inconnu

		sendMessageSync(xmlContent);

		// vérifie que l'evenement est bien là, mais que le numéro d'individu inconnu a bien provoqué une erreur
		List<EvenementCivilData> evenements = evenementCivilDAO.getAll();
		assertEquals(1, evenements.size());
		EvenementCivilData evenement = evenements.get(0);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evenement.getEtat());
		assertTrue("La date de l'événement n'a pas été récupérée correctement", RegDate.get(2007, 9, 18).equals(evenement.getDateEvenement()));
		assertEquals("Le numéro technique n'a pas été récupéré correctement", new Long(id), evenement.getId());
		assertEquals("Le type d'événement n'a pas été récupéré correctement", TypeEvenementCivil.NAISSANCE, evenement.getType());
		assertEquals("Le numéro d'individu n'a pas été récupéré correctement", new Long(9876543L), evenement.getNumeroIndividuPrincipal());
		assertEquals("Le numéro OFS n'a pas été récupéré correctement", new Integer(111), evenement.getNumeroOfsCommuneAnnonce());
	}

	@Test(timeout = 10000)
	public void testMessageDeTypeInconnu() throws Exception {
		final String xmlContent = createMessage(42, 1542313, 9876543, RegDate.get(2007, 9, 18), 111);
		sendMessageSync(xmlContent);
		assertEmpty(evenementCivilDAO.getAll());
	}

	@Test(timeout = 10000)
	public void testMessageDeTypeConnuMaisIgnore() throws Exception {
		final int typeConnuMaisIgnore = 0;
		final TypeEvenementCivil typeIgnore = TypeEvenementCivil.valueOf(typeConnuMaisIgnore);
		assertNotNull(typeIgnore);
		assertTrue(typeIgnore.isIgnore());

		final String xmlContent = createMessage(42, typeConnuMaisIgnore, 9876543, RegDate.get(2007, 9, 18), 111);
		sendMessageSync(xmlContent);
		assertEmpty(evenementCivilDAO.getAll());
	}

	@Test(timeout = 10000)
	public void testOnMessageDuplicateId() throws Exception {

		final int id = 54;
		final String xmlContent = createMessage(id, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE.getId(), 123, RegDate.get(1980, 1, 1), MockCommune.Lausanne.getNoOFSEtendu());

		// Le premier doit passer sans probleme
		sendMessageSync(xmlContent);
		{
			final List<EvenementCivilData> evenements = evenementCivilDAO.getAll();
			assertEquals(1, evenements.size());
			assertEquals(EtatEvenementCivil.TRAITE, evenements.get(0).getEtat());
		}

		// Le deuxieme doit aussi passer mais sans etre inséré
		sendMessageSync(xmlContent);
		{
			final List<EvenementCivilData> evenements = evenementCivilDAO.getAll();
			assertEquals(1, evenements.size());
			assertEquals(EtatEvenementCivil.TRAITE, evenements.get(0).getEtat());

			final List<AuditLine> audits = auditLineDAO.find(id);
			assertHasLine("L'événement civil n°" + id + " existe DEJA en DB", audits);
		}
	}

	private static void assertHasLine(String s, List<AuditLine> audits) {
		boolean found = false;
		for (AuditLine line :audits) {
			if (line.getMessage().equals(s)) {
				found =  true;
				break;
			}
		}
		Assert.isTrue(found);
	}
}

package ch.vd.uniregctb.evenement.jms;

import java.util.List;

import org.junit.Test;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.AuditLine;
import ch.vd.uniregctb.audit.AuditLineDAO;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupeDAO;
import ch.vd.uniregctb.evenement.engine.EvenementCivilProcessor;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author xsilbn
 *
 */
public class EvenementCivilUnitaireListenerTest extends BusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(EvenementCivilUnitaireListenerTest.class);

	private EvenementCivilUnitaireListener evenementCivilUnitaireMDP;
	private EvenementCivilRegroupeDAO evenementCivilRegroupeDAO;
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

				addAdresse(bastien, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(julienne, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(bebe, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);

				marieIndividus(bastien, julienne, RegDate.get(2007, 2, 3));
			}
		});

		evenementCivilRegroupeDAO = getBean(EvenementCivilRegroupeDAO.class, "evenementCivilRegroupeDAO");
		auditLineDAO = getBean(AuditLineDAO.class, "auditLineDAO");

		// On crée le listener à la main pour pouvoir appeler les méthodes protégées
		evenementCivilUnitaireMDP = new EvenementCivilUnitaireListener();
		evenementCivilUnitaireMDP.setEvenementCivilProcessor(getBean(EvenementCivilProcessor.class, "evenementCivilProcessor"));
		evenementCivilUnitaireMDP.setTransactionManager(transactionManager);
		evenementCivilUnitaireMDP.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		evenementCivilUnitaireMDP.setEvenementCivilRegroupeDAO(evenementCivilRegroupeDAO);
		evenementCivilUnitaireMDP.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
	}

	public static String createMessage(int id, int type, long noInd, RegDate dateEvt, int ofs) throws Exception {

		String dateStr = RegDateHelper.dateToDashString(dateEvt);

		StringBuffer xmlContent = new StringBuffer();
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

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnMessageGood() throws Exception {

		final int id = 54;
		final String xmlContent = createMessage(id, 1000, 125, RegDate.get(2008, 2, 12), 5586);

		// Envoi du contenu à EvenementCivilDatabaseInserter
		evenementCivilUnitaireMDP.onEvenementCivil(xmlContent);

		// test que l'evenement est bien la
		final List<EvenementCivilRegroupe> ecr = evenementCivilRegroupeDAO.getAll();
		assertEquals(1, ecr.size());

		final EvenementCivilRegroupe evenement = ecr.get(0);
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
	@Test
	public void testOnMessageBad() throws Exception {

		final int id = 54;
		final String xmlContent = createMessage(id, 1000, 9876543, RegDate.get(2007, 9, 18), 111); // no individu inconnu
		evenementCivilUnitaireMDP.onEvenementCivil(xmlContent);

		// vérifie que l'evenement est bien là, mais que le numéro d'individu inconnu a bien provoqué une erreur
		List<EvenementCivilRegroupe> evenements = evenementCivilRegroupeDAO.getAll();
		assertEquals(1, evenements.size());
		EvenementCivilRegroupe evenement = evenements.get(0);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evenement.getEtat());
		assertTrue("La date de l'événement n'a pas été récupérée correctement", RegDate.get(2007, 9, 18).equals(evenement.getDateEvenement()));
		assertEquals("Le numéro technique n'a pas été récupéré correctement", new Long(id), evenement.getId());
		assertEquals("Le type d'événement n'a pas été récupéré correctement", TypeEvenementCivil.NAISSANCE, evenement.getType());
		assertEquals("Le numéro d'individu n'a pas été récupéré correctement", new Long(9876543L), evenement.getNumeroIndividuPrincipal());
		assertEquals("Le numéro OFS n'a pas été récupéré correctement", new Integer(111), evenement.getNumeroOfsCommuneAnnonce());
	}

	@Test
	public void testMessageDeTypeInconnu() throws Exception {
		final String xmlContent = createMessage(42, 1542313, 9876543, RegDate.get(2007, 9, 18), 111);
		evenementCivilUnitaireMDP.onEvenementCivil(xmlContent);
		assertEmpty(evenementCivilRegroupeDAO.getAll());
	}

	@Test
	public void testMessageDeTypeConnuMaisIgnore() throws Exception {
		final int typeConnuMaisIgnore = 0;
		final TypeEvenementCivil typeIgnore = TypeEvenementCivil.valueOf(typeConnuMaisIgnore);
		assertNotNull(typeIgnore);
		assertTrue(typeIgnore.isIgnore());

		final String xmlContent = createMessage(42, typeConnuMaisIgnore, 9876543, RegDate.get(2007, 9, 18), 111);
		evenementCivilUnitaireMDP.onEvenementCivil(xmlContent);
		assertEmpty(evenementCivilRegroupeDAO.getAll());
	}

	@Test
	public void testOnMessageDuplicateId() throws Exception {

		final int id = 54;
		String xmlContent = createMessage(id, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE.getId(), 123, RegDate.get(1980, 1, 1), MockCommune.Lausanne.getNoOFSEtendu());

		// Le premier doit passer sans probleme
		evenementCivilUnitaireMDP.onEvenementCivil(xmlContent);
		{
			List<EvenementCivilRegroupe> evenements = evenementCivilRegroupeDAO.getAll();
			assertEquals(1, evenements.size());
			assertEquals(EtatEvenementCivil.TRAITE, evenements.get(0).getEtat());
		}

		// Le deuxieme doit aussi passer mais sans etre inséré
		evenementCivilUnitaireMDP.onEvenementCivil(xmlContent);
		{
			List<EvenementCivilRegroupe> evenements = evenementCivilRegroupeDAO.getAll();
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

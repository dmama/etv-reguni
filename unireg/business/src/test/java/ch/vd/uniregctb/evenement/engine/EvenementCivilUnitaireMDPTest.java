package ch.vd.uniregctb.evenement.engine;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupeDAO;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaireDAO;
import ch.vd.uniregctb.evenement.jms.EvenementCivilUnitaireMDP;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * @author xsilbn
 *
 */
public class EvenementCivilUnitaireMDPTest extends BusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(EvenementCivilUnitaireMDPTest.class);

	private EvenementCivilUnitaireMDP evenementCivilUnitaireMDP;

	private EvenementCivilUnitaireDAO evenementCivilUnitaireDAO;

	private EvenementCivilRegroupeDAO evenementCivilRegroupeDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {

			@Override
			protected void init() {
				MockIndividu bastien = addIndividu(123, RegDate.get(1970, 5, 5), "Malorme", "Bastien", true);
				MockIndividu julienne = addIndividu(124, RegDate.get(1973, 11, 13), "Malorme", "Julienne", false);
				MockIndividu bebe = addIndividu(125, RegDate.get(2008, 2, 2), "Malorme", "Alain", true);

				addAdresse(bastien, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(1980, 1, 1), null);
				addAdresse(julienne, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(1980, 1, 1), null);
				addAdresse(bebe, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(1980, 1, 1), null);

				marieIndividus(bastien, julienne, RegDate.get(2007, 2, 3));
			}
		});

		evenementCivilUnitaireMDP = getBean(EvenementCivilUnitaireMDP.class, "evenementCivilUnitaireMDP");
		evenementCivilUnitaireDAO = getBean(EvenementCivilUnitaireDAO.class, "evenementCivilUnitaireDAO");
		evenementCivilRegroupeDAO = getBean(EvenementCivilRegroupeDAO.class, "evenementCivilRegroupeDAO");
	}

	private String createMessage(int id, int type, int noInd, RegDate dateEvt, int ofs) throws Exception {

		String dateStr = RegDateHelper.dateToDashString(dateEvt);

		StringBuffer xmlContent = new StringBuffer();
		xmlContent.append(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
							"<EvtRegCivil xmlns=\"http://www.vd.ch/schema/registreCivil/20070914/EvtRegCivil\">"+
								"<NoTechnique>"+id+"</NoTechnique>"+
								"<Code>"+type+"</Code>"+
								"<NoIndividu>"+noInd+"</NoIndividu>"+
								"<DateEvenement>"+dateStr+"</DateEvenement>"+
								"<DateTraitement>"+dateStr+"</DateTraitement>"+
								"<NumeroOFS>"+ofs+"</NumeroOFS>"+
							"</EvtRegCivil>");

		return xmlContent.toString();
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnMessageGood() throws Exception {

		int id = 54;
		String xmlContent = createMessage(id, 1000, 125, RegDate.get(2008, 2, 12), 5586);

		// Envoi du contenu à EvenementCivilDatabaseInserter
		StringBuffer errorMsg = new StringBuffer();
		evenementCivilUnitaireMDP.insertRegroupeAndTraite(xmlContent, errorMsg);

		// test que l'evenement est bien la
		{
			List<EvenementCivilUnitaire> evenements = evenementCivilUnitaireDAO.getAll();
			assertEquals(1, evenements.size());
			EvenementCivilUnitaire evenement = evenements.get(0);
			assertEquals(EtatEvenementCivil.TRAITE, evenement.getEtat());
			assertTrue("La date de l'événement n'a pas été récupérée correctement", RegDate.get(2008, 2, 12).equals(evenement.getDateEvenement()));
			assertEquals("Le numéro technique n'a pas été récupéré correctement", new Long(id), evenement.getId());
			assertEquals("Le type d'événement n'a pas été récupéré correctement", TypeEvenementCivil.NAISSANCE, evenement.getType());
			assertEquals("Le numéro d'individu n'a pas été récupéré correctement", new Long(125L), evenement.getNumeroIndividu());
			assertEquals("Le numéro OFS n'a pas été récupéré correctement", new Integer(5586), evenement.getNumeroOfsCommuneAnnonce());
		}

		{
			List<EvenementCivilRegroupe> ecr = evenementCivilRegroupeDAO.getAll();
			assertEquals(1, ecr.size());
			EvenementCivilRegroupe evenement = ecr.get(0);
			assertEquals(EtatEvenementCivil.TRAITE, evenement.getEtat());
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnMessageBad() throws Exception {

		int id = 54;
		String xmlContent = createMessage(id, 1000, 9876543, RegDate.get(2007, 9, 18), 111);

		// Envoi du contenu à EvenementCivilDatabaseInserter
		StringBuffer errorMsg = new StringBuffer();
		evenementCivilUnitaireMDP.insertRegroupeAndTraite(xmlContent, errorMsg);

		// test que l'evenement est bien la
		List<EvenementCivilUnitaire> evenements = evenementCivilUnitaireDAO.getAll();
		assertEquals(1, evenements.size());
		EvenementCivilUnitaire evenement = evenements.get(0);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evenement.getEtat());
		assertTrue("La date de l'événement n'a pas été récupérée correctement", RegDate.get(2007, 9, 18).equals(evenement.getDateEvenement()));
		assertEquals("Le numéro technique n'a pas été récupéré correctement", new Long(id), evenement.getId());
		assertEquals("Le type d'événement n'a pas été récupéré correctement", TypeEvenementCivil.NAISSANCE, evenement.getType());
		assertEquals("Le numéro d'individu n'a pas été récupéré correctement", new Long(9876543L), evenement.getNumeroIndividu());
		assertEquals("Le numéro OFS n'a pas été récupéré correctement", new Integer(111), evenement.getNumeroOfsCommuneAnnonce());
	}

	@Test
	public void testOnMessageDuplicateId() throws Exception {

		String xmlContent = createMessage(54, 1000, 9876543, RegDate.get(2007, 9, 18), 111);

		// Le premier doit passer sans probleme
		{
			StringBuffer errorMsg = new StringBuffer();
			evenementCivilUnitaireMDP.insertRegroupeAndTraite(xmlContent, errorMsg);
		}

		{
			List<EvenementCivilUnitaire> evenements = evenementCivilUnitaireDAO.getAll();
			assertEquals(1, evenements.size());
		}

		// Le deuxieme doit aussi passer sans etre inséré
		{
			StringBuffer errorMsg = new StringBuffer();
			evenementCivilUnitaireMDP.insertRegroupeAndTraite(xmlContent, errorMsg);
		}

		{
			List<EvenementCivilUnitaire> evenements = evenementCivilUnitaireDAO.getAll();
			assertEquals(1, evenements.size());
		}
	}

	@Test
	public void testWhenRegroupeFail() throws Exception {

		int id1 = 54;
		int id2 = 55;

		// Insertion du premier
		{
			String xmlContent = createMessage(id1, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE.getId(), 123, RegDate.get(2007, 9, 18), 5586);
			StringBuffer errorMsg = new StringBuffer();
			evenementCivilUnitaireMDP.insertRegroupeAndTraite(xmlContent, errorMsg);
		}

		// Teste le premier
		{
			List<EvenementCivilUnitaire> ecu = evenementCivilUnitaireDAO.getAll();
			assertEquals(1, ecu.size());
			assertEquals(EtatEvenementCivil.TRAITE, ecu.get(0).getEtat());
			List<EvenementCivilRegroupe> ecr = evenementCivilRegroupeDAO.getAll();
			assertEquals(1, ecr.size());
			assertEquals(EtatEvenementCivil.EN_ERREUR, ecr.get(0).getEtat());
		}

		// Insertion du 2eme
		{
			String xmlContent = createMessage(id2, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE.getId(), 124, RegDate.get(2007, 9, 18), 5586);
			StringBuffer errorMsg = new StringBuffer();
			evenementCivilUnitaireMDP.insertRegroupeAndTraite(xmlContent, errorMsg);
		}

		// Teste le 2eme
		{
			List<EvenementCivilUnitaire> ecu = evenementCivilUnitaireDAO.getAll();
			assertEquals(2, ecu.size());
			for (EvenementCivilUnitaire e : ecu) {
				assertEquals(EtatEvenementCivil.TRAITE, e.getEtat());
			}
			List<EvenementCivilRegroupe> ecr = evenementCivilRegroupeDAO.getAll();
			assertEquals(1, ecr.size());
			assertEquals(EtatEvenementCivil.EN_ERREUR, ecr.get(0).getEtat());
		}
	}

}

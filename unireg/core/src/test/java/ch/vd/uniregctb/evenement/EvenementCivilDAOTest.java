package ch.vd.uniregctb.evenement;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * @author
 *
 */
public class EvenementCivilDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(EvenementCivilDAOTest.class);

	private static final String DAO_NAME = "evenementCivilDAO";

	private static final String DB_UNIT_DATA_FILE = "EvenementCivilDAOTest.xml";

	/**
	 * Le DAO.
	 */
	EvenementCivilDAO dao;

	public EvenementCivilDAOTest() throws Exception {

	}

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(EvenementCivilDAO.class, DAO_NAME);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	/**
	 * Teste la methode getAll.
	 */
	@Test
	public void testGetAll() throws Exception {

		List<EvenementCivilData> list = dao.getAll();
		assertNotNull(list);
		assertEquals(2, list.size());
	}

	/**
	 * Teste la methode getEvenementCivilsNonTraites.
	 */
	@Test
	public void testGetEvenementCivilsNonTraites() throws Exception {

		List<Long> list = dao.getEvenementCivilsNonTraites();
		assertNotNull(list);
		assertEquals(1, list.size());
		for (Long id : list) {
			EvenementCivilData evt = dao.get(id);
			Assert.isTrue( !EtatEvenementCivil.TRAITE.equals( evt.getEtat() ), "un évenement traité a été récupéré");
		}
	}

	/**
	 *
	 * Teste la methode findByNumeroIndividu.
	 */
	@Test
	public void testFind() throws Exception {

		EvenementCriteria evenementCriteria = new EvenementCriteria();
		evenementCriteria.setType(TypeEvenementCivil.MARIAGE);
		ParamPagination pagination = new ParamPagination(1, 50, "dateEvenement", true);
		List<EvenementCivilData> list = dao.find(evenementCriteria, pagination);
		assertNotNull(list);
		assertEquals(1, list.size());

		// Evt
		EvenementCivilData evt = list.get(0);
		assertEquals(new Long(12345L), evt.getNumeroIndividuPrincipal());
		assertEquals(new Long(23456L), evt.getNumeroIndividuConjoint());

		// Principal
		assertEquals(new Long(6789L), evt.getHabitantPrincipalId());
		// Conjoint
		assertEquals(new Long(5678L), evt.getHabitantConjointId());
	}

}

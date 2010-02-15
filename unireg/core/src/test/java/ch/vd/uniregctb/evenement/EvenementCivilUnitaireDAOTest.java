package ch.vd.uniregctb.evenement;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.CoreDAOTest;

/**
 * @author
 *
 */
public class EvenementCivilUnitaireDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(EvenementCivilUnitaireDAOTest.class);

	private static final String DAO_NAME = "evenementCivilUnitaireDAO";

	private static final String DB_UNIT_DATA_FILE = "EvenementCivilUnitaireDAOTest.xml";

	/**
	 * Le DAO.
	 */
	EvenementCivilUnitaireDAO dao;

	public EvenementCivilUnitaireDAOTest() throws Exception {

	}

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(EvenementCivilUnitaireDAO.class, DAO_NAME);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	/**
	 *
	 * Teste la methode findByNumeroIndividu.
	 */
	@Test
	public void testGetAll() throws Exception {

		List<EvenementCivilUnitaire> list = dao.getAll();
		assertNotNull(list);
		assertFalse(list.isEmpty());
	}

}

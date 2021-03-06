package ch.vd.unireg.evenement.civil.ech;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.civil.EvenementCivilCriteria;
import ch.vd.unireg.type.TypeEvenementCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc", "unchecked"})
public class EvenementCivilEchDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilEchDAOTest.class);

	private static final String DAO_NAME = "evenementCivilEchDAO";

	private static final String DB_UNIT_DATA_FILE = "EvenementCivilEchDAOTest.xml";

	EvenementCivilEchDAO dao;

	public EvenementCivilEchDAOTest() throws Exception {

	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(EvenementCivilEchDAO.class, DAO_NAME);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAll() throws Exception {

		List<EvenementCivilEch> list = dao.getAll();
		assertNotNull(list);
		assertEquals(2, list.size());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFind() throws Exception {

		EvenementCivilCriteria evenementCriteria = new EvenementCivilCriteria();
		evenementCriteria.setType(TypeEvenementCivil.MARIAGE);
		ParamPagination pagination = new ParamPagination(1, 50, "dateEvenement", true);
		List<EvenementCivilEch> list = dao.find(evenementCriteria, pagination);
		assertNotNull(list);
		assertEquals(1, list.size());

		// Evt
		EvenementCivilEch evt = list.get(0);
		assertEquals(Long.valueOf(12345L), evt.getNumeroIndividu());
	}

	/**
	 *
	 * Teste la méthode find avec un objet pagination null
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindPaginationNull() throws Exception {

		EvenementCivilCriteria evenementCriteria = new EvenementCivilCriteria();
		evenementCriteria.setType(TypeEvenementCivil.MARIAGE);
		List<EvenementCivilEch> list = dao.find(evenementCriteria, null);
		assertNotNull(list);
		assertEquals(1, list.size());

		// Evt
		EvenementCivilEch evt = list.get(0);
		assertEquals(Long.valueOf(12345L), evt.getNumeroIndividu());
	}

}

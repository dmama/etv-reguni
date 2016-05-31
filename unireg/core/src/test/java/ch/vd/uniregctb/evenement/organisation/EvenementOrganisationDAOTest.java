package ch.vd.uniregctb.evenement.organisation;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc", "unchecked"})
public class EvenementOrganisationDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationDAOTest.class);

	private static final String DAO_NAME = "evenementOrganisationDAO";

	private static final String DB_UNIT_DATA_FILE = "EvenementOrganisationDAOTest.xml";

	EvenementOrganisationDAO dao;

	public EvenementOrganisationDAOTest() throws Exception {

	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(EvenementOrganisationDAO.class, DAO_NAME);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAll() throws Exception {

		List<EvenementOrganisation> list = dao.getAll();
		assertNotNull(list);
		assertEquals(2, list.size());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFind() throws Exception {

		EvenementOrganisationCriteria evenementCriteria = new EvenementOrganisationCriteria();
		evenementCriteria.setType(TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE);
		ParamPagination pagination = new ParamPagination(1, 50, "dateEvenement", true);
		List<EvenementOrganisation> list = dao.find(evenementCriteria, pagination);
		assertNotNull(list);
		assertEquals(1, list.size());

		// Evt
		EvenementOrganisation evt = list.get(0);
		assertEquals(123454321L, evt.getNoOrganisation());
	}

	/**
	 *
	 * Teste la méthode find avec un objet pagination null
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindPaginationNull() throws Exception {

		EvenementOrganisationCriteria evenementCriteria = new EvenementOrganisationCriteria();
		evenementCriteria.setType(TypeEvenementOrganisation.FOSC_DISSOLUTION_ENTREPRISE);
		List<EvenementOrganisation> list = dao.find(evenementCriteria, null);
		assertNotNull(list);
		assertEquals(1, list.size());

		// Evt
		EvenementOrganisation evt = list.get(0);
		assertEquals(123454321L, evt.getNoOrganisation());
	}
}

package ch.vd.uniregctb.evenement;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneCriteria;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneDAO;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author
 *
 */
public class EvenementCivilDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(EvenementCivilDAOTest.class);

	private static final String DAO_NAME = "evenementCivilExterneDAO";

	private static final String DB_UNIT_DATA_FILE = "EvenementCivilDAOTest.xml";

	/**
	 * Le DAO.
	 */
	EvenementCivilExterneDAO dao;

	public EvenementCivilDAOTest() throws Exception {

	}

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(EvenementCivilExterneDAO.class, DAO_NAME);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	/**
	 * Teste la methode getAll.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAll() throws Exception {

		List<EvenementCivilExterne> list = dao.getAll();
		assertNotNull(list);
		assertEquals(2, list.size());
	}

	/**
	 * Teste la methode getEvenementCivilsNonTraites.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEvenementCivilsNonTraites() throws Exception {

		final List<Long> list = dao.getEvenementCivilsNonTraites();
		assertNotNull(list);
		assertEquals(1, list.size());
		for (Long id : list) {
			final EvenementCivilExterne evt = dao.get(id);
			Assert.isTrue( !evt.getEtat().isTraite(), "un évenement traité a été récupéré");
		}
	}

	/**
	 *
	 * Teste la methode findByNumeroIndividu.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFind() throws Exception {

		EvenementCivilExterneCriteria evenementCriteria = new EvenementCivilExterneCriteria();
		evenementCriteria.setType(TypeEvenementCivil.MARIAGE);
		ParamPagination pagination = new ParamPagination(1, 50, "dateEvenement", true);
		List<EvenementCivilExterne> list = dao.find(evenementCriteria, pagination);
		assertNotNull(list);
		assertEquals(1, list.size());

		// Evt
		EvenementCivilExterne evt = list.get(0);
		assertEquals(new Long(12345L), evt.getNumeroIndividuPrincipal());
		assertEquals(new Long(23456L), evt.getNumeroIndividuConjoint());

		// Principal
		assertEquals(new Long(6789L), evt.getHabitantPrincipalId());
		// Conjoint
		assertEquals(new Long(5678L), evt.getHabitantConjointId());
	}

}

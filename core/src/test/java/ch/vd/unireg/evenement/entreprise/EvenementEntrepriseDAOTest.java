package ch.vd.unireg.evenement.entreprise;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc", "unchecked"})
public class EvenementEntrepriseDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseDAOTest.class);

	private static final String DAO_NAME = "evenementEntrepriseDAO";

	private static final String DB_UNIT_DATA_FILE = "EvenementEntrepriseDAOTest.xml";

	EvenementEntrepriseDAO dao;

	public EvenementEntrepriseDAOTest() throws Exception {

	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(EvenementEntrepriseDAO.class, DAO_NAME);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAll() throws Exception {

		List<EvenementEntreprise> list = dao.getAll();
		assertNotNull(list);
		assertEquals(6, list.size());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFind() throws Exception {

		EvenementEntrepriseCriteria evenementCriteria = new EvenementEntrepriseCriteria();
		evenementCriteria.setType(TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE);
		ParamPagination pagination = new ParamPagination(1, 50, "dateEvenement", true);
		List<EvenementEntreprise> list = dao.find(evenementCriteria, pagination);
		assertNotNull(list);
		assertEquals(2, list.size());

		// Evt
		EvenementEntreprise evt = list.get(0);
		assertEquals(123454321L, evt.getNoEntrepriseCivile());
	}

	/**
	 *
	 * Teste la méthode find avec un objet pagination null
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindPaginationNull() throws Exception {

		EvenementEntrepriseCriteria evenementCriteria = new EvenementEntrepriseCriteria();
		evenementCriteria.setType(TypeEvenementEntreprise.FOSC_AUTRE_MUTATION);
		List<EvenementEntreprise> list = dao.find(evenementCriteria, null);
		assertNotNull(list);
		assertEquals(4, list.size());

		// Evt
		EvenementEntreprise evt = list.get(0);
		assertEquals(123454321L, evt.getNoEntrepriseCivile());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEvenementsEntrepriseNonTraitesNonTraites() {
		final List<EvenementEntreprise> evtsSucces = dao.getEvenementsNonTraites(123454321L);
		assertEquals(2, evtsSucces.size());
		assertEquals(19003L, evtsSucces.get(0).getNoEvenement());
		assertEquals(19004L, evtsSucces.get(1).getNoEvenement());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEvenementsApresDate() {
		{
			final List<EvenementEntreprise> evts = dao.getEvenementsApresDateNonAnnules(98989898L, date(2015, 9, 7));
			Collections.sort(evts, getByIdEvtEntrepriseComparator());
			assertEquals(2, evts.size());
			assertEquals(20001L, evts.get(0).getNoEvenement());
			assertEquals(20002L, evts.get(1).getNoEvenement());
		}
		{
			final List<EvenementEntreprise> evts = dao.getEvenementsApresDateNonAnnules(98989898L, date(2015, 9, 8));
			Collections.sort(evts, getByIdEvtEntrepriseComparator());
			assertEquals(1, evts.size());
			assertEquals(20002L, evts.get(0).getNoEvenement());
		}
		{
			final List<EvenementEntreprise> evts = dao.getEvenementsApresDateNonAnnules(98989898L, date(2015, 9, 9));
			Collections.sort(evts, getByIdEvtEntrepriseComparator());
			assertEquals(0, evts.size());
		}
	}

	@NotNull
	protected Comparator<EvenementEntreprise> getByIdEvtEntrepriseComparator() {
		return new Comparator<EvenementEntreprise>() {
			@Override
			public int compare(EvenementEntreprise o1, EvenementEntreprise o2) {
				return Long.compare(o1.getId(), o2.getId());
			}
		};
	}
}

package ch.vd.uniregctb.tiers;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TacheDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(TacheDAOTest.class);

	private static final String DAO_NAME = "tacheDAO";

	private static final String DB_UNIT_DATA_FILE = "TacheDAOTest.xml";

	private TacheDAO dao;

	/**
	 * @throws Exception
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(TacheDAO.class, DAO_NAME);
	}

	@Test
	public void testFindParTypeEtatTache() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		// Tâches en instance
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(5), list.get(1).getId());
		}

		// Tâches traitées
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.TRAITE);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(Long.valueOf(2), list.get(0).getId());
			assertEquals(Long.valueOf(3), list.get(1).getId());
		}

		// Tâches en cours
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.EN_COURS);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(4), list.get(0).getId());
		}
	}

	@Test
	public void testFindParTypeTache() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		// Type envoi di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(5), list.get(1).getId());
		}

		// Type annulation di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(2), list.get(0).getId());
		}

		// Type contrôle dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheControleDossier);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(3), list.get(0).getId());
		}

		// Type transmission dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheTransmissionDossier);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(4), list.get(0).getId());
		}
	}

	@Test
	public void testFindParTypeTacheInverse() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		// Type envoi di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(3, list.size());
			assertEquals(Long.valueOf(2), list.get(0).getId());
			assertEquals(Long.valueOf(3), list.get(1).getId());
			assertEquals(Long.valueOf(4), list.get(2).getId());
		}

		// Type annulation di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(3), list.get(1).getId());
			assertEquals(Long.valueOf(4), list.get(2).getId());
			assertEquals(Long.valueOf(5), list.get(3).getId());
		}

		// Type contrôle dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheControleDossier);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(4), list.get(2).getId());
			assertEquals(Long.valueOf(5), list.get(3).getId());
		}

		// Type transmission dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheTransmissionDossier);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(3), list.get(2).getId());
			assertEquals(Long.valueOf(5), list.get(3).getId());
		}
	}

	@Test
	public void testFindParDateCreation() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2007, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2007, 1, 31));
			final List<Tache> list = dao.find(criterion);
			assertEmpty(list);
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 1, 31));
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 2, 29));
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 3, 31));
			final List<Tache> list = dao.find(criterion);
			assertEquals(3, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(3), list.get(2).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 4, 30));
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(3), list.get(2).getId());
			assertEquals(Long.valueOf(4), list.get(3).getId());
		}
	}

	@Test
	public void testFindParAnnee() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2007);
			final List<Tache> list = dao.find(criterion);
			assertEmpty(list);
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2008);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2009);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(5), list.get(0).getId());
		}
	}

	@Test
	public void testFindParContribuable() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final Contribuable gomez = (Contribuable) dao.getHibernateTemplate().get(Contribuable.class, 12600003L);
		assertNotNull(gomez);

		final Contribuable pelcrus = (Contribuable) dao.getHibernateTemplate().get(Contribuable.class, 12600456L);
		assertNotNull(pelcrus);

		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setContribuable(pelcrus);
			criterion.setAnnee(2008);
			final List<Tache> list = dao.find(criterion);
			assertEmpty(list);
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setContribuable(gomez);
			criterion.setAnnee(2008);
			final List<Tache> list = dao.find(criterion);
			assertEquals(5, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(3), list.get(2).getId());
			assertEquals(Long.valueOf(4), list.get(3).getId());
			assertEquals(Long.valueOf(5), list.get(4).getId());
		}
	}


	@Test
	public void testFindAvecPagination() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		ParamPagination paramPagination = new ParamPagination(1, 1, null, false);
		TacheCriteria tacheCriteria = new TacheCriteria();
		tacheCriteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		List<Tache> taches = dao.find(tacheCriteria, paramPagination);
		assertEquals(1, taches.size());

		paramPagination = new ParamPagination(2, 1, null, false);
		tacheCriteria = new TacheCriteria();
		taches = dao.find(tacheCriteria, paramPagination);
		assertEquals(1, taches.size());
	}
}

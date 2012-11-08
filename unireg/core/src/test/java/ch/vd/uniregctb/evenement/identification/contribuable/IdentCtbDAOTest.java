package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.ParamPagination;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class IdentCtbDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(IdentCtbDAOTest.class);

	private static final String DAO_NAME = "identCtbDAO";

	private static final String DB_UNIT_DATA_FILE = "IdentCtbDAOTest.xml";

	/**
	 * Le DAO.
	 */
	private IdentCtbDAO dao;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(IdentCtbDAO.class, DAO_NAME);
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	/**
	 * Teste la methode find simple
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFind() throws Exception {
		IdentificationContribuableCriteria identificationContribuableCriteria = new IdentificationContribuableCriteria();
		Calendar cal = new GregorianCalendar();
		cal.set(2000, 0, 1);
		identificationContribuableCriteria.setDateMessageDebut(cal.getTime());
		cal.set(2020, 0, 1);
		identificationContribuableCriteria.setDateMessageFin(cal.getTime());
		identificationContribuableCriteria.setDateNaissance(RegDate.get(1973, 7, 11));
		identificationContribuableCriteria.setEmetteurId("Test");
		identificationContribuableCriteria.setEtatMessage("EXCEPTION");
		identificationContribuableCriteria.setNAVS13("1234567890123");
		identificationContribuableCriteria.setNom("Larousse");
		identificationContribuableCriteria.setPeriodeFiscale(Integer.valueOf(2008));
		identificationContribuableCriteria.setPrenoms("Lora");
		identificationContribuableCriteria.setPrioriteEmetteur("NON_PRIORITAIRE");
		identificationContribuableCriteria.setTypeMessage("ssk-3001-000101");

		final ParamPagination paramPagination = new ParamPagination(1, 100, null, true);
		final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false,false);
		assertNotNull(list);
		assertEquals(1, list.size());
	}

	/**
	 * Teste la methode find avec le type de demande
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindTypeDemande() throws Exception {
		final IdentificationContribuableCriteria identificationContribuableCriteria = new IdentificationContribuableCriteria();
		final Calendar cal = new GregorianCalendar();
		cal.set(2000, Calendar.JANUARY, 1);
		identificationContribuableCriteria.setDateMessageDebut(cal.getTime());
		cal.set(2020, Calendar.JANUARY, 1);
		identificationContribuableCriteria.setDateMessageFin(cal.getTime());
		identificationContribuableCriteria.setDateNaissance(RegDate.get(1973, 7, 11));
		identificationContribuableCriteria.setEmetteurId("Test");
		identificationContribuableCriteria.setEtatMessage("EXCEPTION");
		identificationContribuableCriteria.setNAVS13("1234567890123");
		identificationContribuableCriteria.setNom("Larousse");
		identificationContribuableCriteria.setPeriodeFiscale(2008);
		identificationContribuableCriteria.setPrenoms("Lora");
		identificationContribuableCriteria.setPrioriteEmetteur("NON_PRIORITAIRE");

		final ParamPagination paramPagination = new ParamPagination(1, 100, null, true);

		// tous les types...
		{
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false);
			assertNotNull(list);
			assertEquals(3, list.size());
		}
		// tous les types...
		{
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false, (TypeDemande[]) null);
			assertNotNull(list);
			assertEquals(3, list.size());
		}
		// tous les types...
		{
			@SuppressWarnings("RedundantArrayCreation")
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false,false, new TypeDemande[] {});
			assertNotNull(list);
			assertEquals(3, list.size());
		}
		// aucun type
		{
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false,false, new TypeDemande[] { null });
			assertNotNull(list);
			assertEquals(0, list.size());
		}
		// seulement un à chaque fois
		{
			for (TypeDemande type : EnumSet.of(TypeDemande.IMPOT_SOURCE, TypeDemande.MELDEWESEN, TypeDemande.NCS)) {
				final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false, type);
				assertNotNull(type.toString(), list);
				assertEquals(type.toString(), 1, list.size());
			}
		}
		// deux ensemble
		{
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false, TypeDemande.IMPOT_SOURCE, TypeDemande.NCS);
			assertNotNull(list);
			assertEquals(2, list.size());
		}
	}
}

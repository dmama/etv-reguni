package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.Calendar;
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
	IdentCtbDAO dao;

	public IdentCtbDAOTest() throws Exception {

	}

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
	 *
	 * Teste la methode find
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

		ParamPagination paramPagination = new ParamPagination(1, 100, null, true);
		List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false,false, TypeDemande.MELDEWESEN);
		assertNotNull(list);
		assertEquals(1, list.size());
	}
}

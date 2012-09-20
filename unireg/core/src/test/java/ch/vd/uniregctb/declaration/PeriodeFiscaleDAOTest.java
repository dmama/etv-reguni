package ch.vd.uniregctb.declaration;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.CoreDAOTest;

import static junit.framework.Assert.assertEquals;

@SuppressWarnings({"JavaDoc"})
public class PeriodeFiscaleDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(PeriodeFiscaleDAOTest.class);

	private static final String DAO_NAME = "periodeFiscaleDAO";

	private static final String DB_UNIT_DATA_FILE = "PeriodeFiscaleDAOTest.xml";

	/**
	 * Le DAO.
	 */
	private PeriodeFiscaleDAO periodeFiscaleDAO;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, DAO_NAME);
	}

	/**
	 * Teste la methode qui recherche les LRs suivant certains criteres
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAllDesc() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		List<PeriodeFiscale> periodes = periodeFiscaleDAO.getAllDesc();
		assertEquals(3, periodes.size());
		final Integer anneeAttendue = 2007;
		final Integer annee = periodes.get(0).getAnnee();
		assertEquals(anneeAttendue, annee);
		
	}

	public PeriodeFiscaleDAO getPeriodeFiscaleDAO() {
		return periodeFiscaleDAO;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

}

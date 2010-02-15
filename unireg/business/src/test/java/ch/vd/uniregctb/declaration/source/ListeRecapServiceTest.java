package ch.vd.uniregctb.declaration.source;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAOImpl;

public class ListeRecapServiceTest extends BusinessTest {

	private ListeRecapService lrService = null;
	private TiersDAO tiersDAO;
	private final static String DB_UNIT_FILE = "ListeRecapServiceTest.xml";


	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		loadDatabase(DB_UNIT_FILE);
		lrService = getBean(ListeRecapServiceImpl.class, "lrService");
		tiersDAO = getBean(TiersDAOImpl.class, "tiersDAO");

	}

	/**
	 * Teste la methode findLRsManquantes
	 *
	 * @throws Exception
	 */
	@Test
	public void testFindLRsManquantes() throws Exception {
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(Long.valueOf(12500001));
		RegDate dateFinPeriode = RegDate.get(2010, 12, 31);
		List<DateRange> lrTrouvees = new ArrayList<DateRange>();
		List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
		assertEquals(16, lrManquantes.size());
		assertEquals(1, lrTrouvees.size());
		DateRange firstRange = lrManquantes.get(0);
		assertEquals(RegDate.get(2007, 1, 1), firstRange.getDateDebut());
	}


}

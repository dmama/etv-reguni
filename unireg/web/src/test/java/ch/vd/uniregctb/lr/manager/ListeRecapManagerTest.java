package ch.vd.uniregctb.lr.manager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;

public class ListeRecapManagerTest extends WebTest {

	private ListeRecapEditManager lrEditManager = null;

	private final static String DB_UNIT_FILE = "ListeRecapManagerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		lrEditManager = getBean(ListeRecapEditManager.class, "lrEditManager");

		loadDatabase(DB_UNIT_FILE);
	}

	/**
	 * Teste la methode creerLr
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreerLr() throws Exception {

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500001));
			RegDate dateDebutPeriodeAttendue = RegDate.get(2008, 2, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2008, 2, 29);
			assertNotNull(lrView);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500002));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2008, 3, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2008, 3, 31);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500003));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2007, 3, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2007, 3, 31);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}
	}

	/**
	 * Teste la methode testCreerLrForPeriodiciteUnique
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreerLrForPeriodiciteUnique() throws Exception {

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500004));
			RegDate dateDebutPeriodeAttendue = RegDate.get(2007, 4, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2007, 4, 30);
			assertNotNull(lrView);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500005));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2008, 4, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2008, 6, 30);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500006));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2007, 7, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2007, 12, 31);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500007));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2008, 1, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2008, 12, 31);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}
	}


}

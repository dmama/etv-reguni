package ch.vd.uniregctb.tiers.manager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;

public class TiersVisuManagerTest extends WebTest {

	private TiersVisuManager tiersVisuManager;

	private final static String DB_UNIT_FILE = "TiersVisuManagerTest.xml";

	/**
	 * @see ch.vd.uniregctb.common.AbstractCoreDAOTest#onSetUp()
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addIndividu(282312, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);

			}
		});

		loadDatabase(DB_UNIT_FILE);
		tiersVisuManager = getBean(TiersVisuManager.class, "tiersVisuManager");

	}

	/**
	 * Teste la methode getView
	 */

	@Test
	public void testGetViewHabitant() throws Exception{

		WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
		TiersVisuView view = tiersVisuManager.getView((long) 6789, true, true, true, false, webParamPagination);
		Tiers tiers = view.getTiers();
		PersonnePhysique hab = (PersonnePhysique) tiers;
		assertNotNull(hab);
		assertEquals("Bolomey", view.getIndividu().getNom());
	}

	@Test
	public void testGetViewNonHabitant() throws Exception {
		WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
		TiersVisuView view = tiersVisuManager.getView((long) 12600002, true, true, true, false, webParamPagination);
		Tiers tiers = view.getTiers();
		PersonnePhysique nonHab = (PersonnePhysique) tiers;
		assertNotNull(nonHab);
		assertEquals("Kamel", nonHab.getNom());

	}

	@Test
	public void testGetAdressesHistoriques() throws Exception {
		WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
		TiersVisuView view = tiersVisuManager.getView((long) 6789, true, true, true, false, webParamPagination);
		List<AdresseView> adresses = view.getHistoriqueAdresses();
		/*
		 * 2 * courrier
		 * 2 * representation (1 fiscale + 1 défaut)
		 * 2 * poursuite (1 défaut)
		 */
		assertEquals(6, adresses.size());

	}

	public TiersVisuManager getTiersVisuManager() {
		return tiersVisuManager;
	}

	public void setTiersVisuManager(TiersVisuManager tiersVisuManager) {
		this.tiersVisuManager = tiersVisuManager;
	}

}

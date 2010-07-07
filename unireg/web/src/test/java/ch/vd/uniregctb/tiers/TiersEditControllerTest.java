package ch.vd.uniregctb.tiers;

import static junit.framework.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersEditControllerTest extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "tiersEditController";

	private final static String DB_UNIT_FILE = "TiersEditControllerTest.xml";

	public final static String AIGUILLAGE_ANNULER_ADRESSE = "annulerAdresse";

	public final static String TIERS_ID_PARAMETER_NAME = "id";

	public final static String TIERS_NATURE_PARAMETER_NAME = "nature";

	private TiersEditController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		controller = getBean(TiersEditController.class, CONTROLLER_NAME);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				Individu individu1 = addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				Individu individu2 = addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu1, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.setMethod("GET");
		request.addParameter(TIERS_ID_PARAMETER_NAME, "6789");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

	@Test
	public void testOnSubmitWithNom() throws Exception {

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		request.addParameter("tiers.nom", "Kamel");
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, Tiers.NATURE_NONHABITANT);
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);


		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();

		List<Tiers> l = tiersDAO.getAll();
		assertEquals(1, l.size());
		PersonnePhysique nh = (PersonnePhysique)l.get(0);
		assertEquals("Kamel", nh.getNom());

		Assert.assertTrue(model != null);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitWithDateNaissancePartielle() throws Exception {

		String nom = "TestKamel";
		RegDate dateN = RegDate.get(1956, 11);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		request.addParameter("tiers.nom", nom);
		request.addParameter("tiers.dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, Tiers.NATURE_NONHABITANT);
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);

		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		List<Tiers> l = tiersDAO.getAll();
		assertEquals(1, l.size());
		PersonnePhysique nh = (PersonnePhysique)l.get(0);
		assertEquals(nom, nh.getNom());
		assertEquals(dateN, nh.getDateNaissance());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitWithWrongDateNaissance() throws Exception {

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		request.addParameter("tiers.dateNaissance", "12/*/.2008");
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, Tiers.NATURE_NONHABITANT);
		//request.addParameter(AbstractTiersController.ONGLET_PARAMETER_NAME, Onglet.civilTab.toString());
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		List<Tiers> l = tiersDAO.getAll();
		assertEquals(0, l.size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitWithDateNaissance() throws Exception {

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		request.addParameter("tiers.nom", "TestKamel");
		RegDate dateN = RegDate.get(2008, 04, 12);
		request.addParameter("tiers.dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, Tiers.NATURE_NONHABITANT);
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);

		request.setMethod("POST");
		controller.handleRequest(request, response);
		List<Tiers> l = tiersDAO.getAll();
		assertEquals(1, l.size());
		PersonnePhysique nh = (PersonnePhysique)l.get(0);
		assertEquals(dateN, nh.getDateNaissance());



	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testModifyNonHabitant() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		request.addParameter("tiers.nom", "Kamel");
		request.addParameter("tiers.numero", "12600002");
		request.addParameter("tiers.prenom", "toto");
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, Tiers.NATURE_NONHABITANT);
		//request.addParameter(AbstractTiersController.ONGLET_PARAMETER_NAME, Onglet.civilTab.toString());
		Tiers tiers = tiersDAO.get(new Long(12600002));
		PersonnePhysique nh = (PersonnePhysique)tiers;
		assertEquals("Kamel", nh.getNom());
		assertEquals(null, nh.getPrenom());
		request.setMethod("POST");
		controller.handleRequest(request, response);
		tiers = tiersDAO.get(new Long(12600002));
		nh = (PersonnePhysique)tiers;
		assertEquals("Kamel", nh.getNom());
		assertEquals("toto", nh.getPrenom());

	}


	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitWithAiguillage() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("tiers.nom", "Kamel");
		request.addParameter("aiguillage", "annulerAdresse");
		request.addParameter("idAdresse", "5");
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, Tiers.NATURE_NONHABITANT);
		//request.addParameter(AbstractTiersController.ONGLET_PARAMETER_NAME, Onglet.civilTab.toString());
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}
}

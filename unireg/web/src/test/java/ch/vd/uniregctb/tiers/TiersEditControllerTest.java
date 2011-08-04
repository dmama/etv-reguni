package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;

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

	public final static String TIERS_ID_PARAMETER_NAME = "id";

	private TiersEditController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		controller = getBean(TiersEditController.class, CONTROLLER_NAME);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				final MockIndividu individu2 = addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.setMethod("GET");
		request.addParameter(TIERS_ID_PARAMETER_NAME, "6789");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitWithNom() throws Exception {

		request.addParameter("tiers.nom", "Kamel");
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, NatureTiers.NonHabitant.name());
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);

		request.setMethod("POST");
		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);

		final List<Tiers> l = tiersDAO.getAll();
		assertEquals(1, l.size());
		final PersonnePhysique nh = (PersonnePhysique) l.get(0);
		assertEquals("Kamel", nh.getNom());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitWithDateNaissancePartielle() throws Exception {

		final String nom = "TestKamel";
		final RegDate dateN = RegDate.get(1956, 11);

		request.addParameter("tiers.nom", nom);
		request.addParameter("tiers.dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, NatureTiers.NonHabitant.name());
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);

		request.setMethod("POST");
		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);

		final List<Tiers> l = tiersDAO.getAll();
		assertEquals(1, l.size());
		final PersonnePhysique nh = (PersonnePhysique)l.get(0);
		assertEquals(nom, nh.getNom());
		assertEquals(dateN, nh.getDateNaissance());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitWithWrongDateNaissance() throws Exception {

		request.addParameter("tiers.dateNaissance", "12/*/.2008");
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, NatureTiers.NonHabitant.name());
		request.setMethod("POST");
		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		final List<Tiers> l = tiersDAO.getAll();
		assertEquals(0, l.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitWithDateNaissance() throws Exception {

		request.addParameter("tiers.nom", "TestKamel");
		final RegDate dateN = RegDate.get(2008, 4, 12);
		request.addParameter("tiers.dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, NatureTiers.NonHabitant.name());
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);

		request.setMethod("POST");
		controller.handleRequest(request, response);

		final List<Tiers> l = tiersDAO.getAll();
		assertEquals(1, l.size());
		final PersonnePhysique nh = (PersonnePhysique)l.get(0);
		assertEquals(dateN, nh.getDateNaissance());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifyNonHabitant() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		Tiers tiers = tiersDAO.get(12600002L);
		PersonnePhysique nh = (PersonnePhysique)tiers;
		assertEquals("Kamel", nh.getNom());
		assertEquals(null, nh.getPrenom());

		request.addParameter("tiers.nom", "Kamel");
		request.addParameter("tiers.numero", "12600002");
		request.addParameter("tiers.prenom", "toto");
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, NatureTiers.NonHabitant.name());
		request.setMethod("POST");
		controller.handleRequest(request, response);

		tiers = tiersDAO.get(12600002L);
		nh = (PersonnePhysique)tiers;
		assertEquals("Kamel", nh.getNom());
		assertEquals("toto", nh.getPrenom());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitWithAiguillage() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("tiers.nom", "Kamel");
		request.addParameter("aiguillage", "annulerAdresse");
		request.addParameter("idAdresse", "5");
		request.addParameter(AbstractTiersController.TIERS_NATURE_PARAMETER_NAME, NatureTiers.NonHabitant.name());
		request.setMethod("POST");
		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);
	}
}

package ch.vd.uniregctb.tiers;

import java.util.Map;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;

import static junit.framework.Assert.assertNotNull;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersVisuControllerTest extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "tiersVisuController";

	private final static String DB_UNIT_FILE = "DBUnit4Import/tiers-basic.xml";

	private TiersVisuController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		controller = getBean(TiersVisuController.class, CONTROLLER_NAME);

		servicePM.setUp(new DefaultMockServicePM());

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(333908, RegDate.get(1974, 3, 22), "Cuendet", "Adrienne", true);
				final MockIndividu individu2 = addIndividu(333905, RegDate.get(1974, 3, 22), "Cuendet", "Biloute", true);
				final MockIndividu individu3 = addIndividu(674417, RegDate.get(1974, 3, 22), "Dardare", "Francois", true);
				final MockIndividu individu4 = addIndividu(327706, RegDate.get(1974, 3, 22), "Dardare", "Marcel", true);
				final MockIndividu individu5 = addIndividu(320073, RegDate.get(1952, 3, 21), "ERTEM", "Sabri", true);

				addAdresse(individu1, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, EnumTypeAdresse.COURRIER, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, EnumTypeAdresse.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, EnumTypeAdresse.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu5, EnumTypeAdresse.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);

				addAdresse(individu1, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, EnumTypeAdresse.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, EnumTypeAdresse.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu5, EnumTypeAdresse.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);

			}
		});

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {
		loadDatabase(DB_UNIT_FILE);
		//
		//Tiers Habitant 12300003
		//
		request.setMethod("GET");
		request.addParameter("id", "12300003");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers Habitant 34807810
		//
		request.setMethod("GET");
		request.addParameter("id", "34807810");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers Habitant 12300001
		//
		request.setMethod("GET");
		request.addParameter("id", "12300001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers NonHabitant 12600001
		//
		request.setMethod("GET");
		request.addParameter("id", "12600001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers MenageCommun 86006202
		//
		request.setMethod("GET");
		request.addParameter("id", "86006202");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers DebiteurPrestationImposable 12500001
		//
		request.setMethod("GET");
		request.addParameter("id", "12500001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers Entreprise 12700001
		//
		request.setMethod("GET");
		request.addParameter("id", "12700001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers AutreCommunaute 12800001
		//
		request.setMethod("GET");
		request.addParameter("id", "12800001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmit() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("id", "86006202");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

}

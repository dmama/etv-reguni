package ch.vd.uniregctb.tiers;

import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.TestData;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertNotNull;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersVisuControllerTest extends WebTest {

	private final static String CONTROLLER_NAME = "tiersVisuController";

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

				addAdresse(individu1, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.COURRIER, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, TypeAdresseCivil.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, TypeAdresseCivil.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu5, TypeAdresseCivil.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);

				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu5, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);

			}
		});

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {
		loadDatabase();
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
		//Tiers Entreprise 127001
		//
		request.setMethod("GET");
		request.addParameter("id", "127001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers AutreCommunaute 2800001
		//
		request.setMethod("GET");
		request.addParameter("id", "2800001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmit() throws Exception {
		loadDatabase();
		request.addParameter("id", "86006202");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	@SuppressWarnings({"UnusedAssignment", "unchecked"})
	private void loadDatabase() {
		TestData.loadTiersBasic(hibernateTemplate);
	}
}

package ch.vd.uniregctb.tiers;

import java.util.Map;

import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;

public class TiersRapportControllerTest extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "tiersRapportController";

	private final static String DB_UNIT_FILE = "TiersRapportControllerTest.xml";

	private TiersRapportController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		controller = getBean(TiersRapportController.class, CONTROLLER_NAME);

		servicePM.setUp(new DefaultMockServicePM());

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				Individu individu1 = addIndividu(333908, RegDate.get(1974, 3, 22), "Cuendet", "Adrienne", true);
				Individu individu2 = addIndividu(333905, RegDate.get(1974, 3, 22), "Cuendet", "Biloute", true);
				Individu individu3 = addIndividu(674417, RegDate.get(1974, 3, 22), "Dardare", "Francois", true);
				Individu individu4 = addIndividu(327706, RegDate.get(1974, 3, 22), "Dardare", "Marcel", true);

				addAdresse(individu1, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, EnumTypeAdresse.COURRIER, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, EnumTypeAdresse.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, EnumTypeAdresse.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);

				addAdresse(individu1, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, EnumTypeAdresse.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, EnumTypeAdresse.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);


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
		request.addParameter("idRapport", "1");
		request.addParameter("sens", "OBJET");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmit() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.addParameter("idRapport", "1");
		request.addParameter("sens", "OBJET");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		/*
		 * TODO (FDE) Completer test ? Verifier que ca s'est bien passé
		 */
	}

}

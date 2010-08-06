package ch.vd.uniregctb.tiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test case du controlleur spring du m�me nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersListControllerTest extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "tiersListController";

	private final static String DB_UNIT_FILE = "classpath:DBUnit4Import/tiers-basic.xml";

	private TiersListController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(TiersListController.class, CONTROLLER_NAME);

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

	@SuppressWarnings("unchecked")
	private List<TiersIndexedData> getTiersList(HashMap<String, String> params) throws Exception {

		// onSubmit
		request.setMethod("POST");
		for (String key : params.keySet()) {
			String value = params.get(key);
			request.addParameter(key, value);
		}
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);

		// showForm
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<TiersIndexedData> list = (List<TiersIndexedData>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);

		// for (TiersIndexedData data : list) {
		// long numero = data.getNumero();
		// String nom1 = data.getNom1();
		// String nom2 = data.getNom2();
		// data = null;
		// }

		return list;
	}

	@Test
	public void testRechercheForTous() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		// Recherche tous les fors y compris les inactifs
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("noOfsFor", "5652");
			List<TiersIndexedData> list = getTiersList(params);
			assertEquals(3, list.size());
		}
	}

	@Test
	public void testRechercheForActifs() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		// Recherche seulement les fors actifs
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("noOfsFor", "5652");
		params.put("forPrincipalActif", "true");
		List<TiersIndexedData> list = getTiersList(params);
		assertEquals(1, list.size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("GET");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);

	}

	@Test
	public void testOnSubmitWithCriteresWithNumCTB() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("POST");
		request.addParameter("numeroFormatte", "12300003");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(1, list.size());


	}

	@Test
	public void testRechercheNomContient() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("POST");
		request.addParameter("nomRaison", "Cuendet");
		request.addParameter("typeRechercheDuNom", "CONTIENT");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(3, list.size());

	}

	@Test
	public void testRechercheNomPhonetique() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("POST");
		request.addParameter("nomRaison", "Cuendet");
		request.addParameter("typeRechercheDuNom", "PHONETIQUE");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(3, list.size());

	}

	@Test
	public void testRechercheDateNaissance() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("POST");
		request.addParameter("dateNaissance", "23.01.1970");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(1, list.size());

	}

	@Test
	public void testRechercheLocalite() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("POST");
		request.addParameter("localiteOuPays", "Lausanne");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertTrue(!list.isEmpty());

	}

	@Test
	public void testRechercheNumAVS() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("POST");
		request.addParameter("numeroAVS", "7561234567897");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(3, list.size());

	}

	@Test
	public void testRechercheNumAVSWithDash() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("POST");
		request.addParameter("numeroAVS", "75612.34.567.897");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(3, list.size());

	}

}

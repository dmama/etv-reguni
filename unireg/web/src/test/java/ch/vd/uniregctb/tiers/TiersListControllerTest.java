package ch.vd.uniregctb.tiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.TestData;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Test case du controlleur spring du m�me nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersListControllerTest extends WebTest {

	private final static String CONTROLLER_NAME = "tiersListController";

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

		setWantIndexation(true);
	}

	@SuppressWarnings("unchecked")
	private List<TiersIndexedDataView> getTiersList(HashMap<String, String> params) throws Exception {

		// onSubmit
		request.setMethod("POST");
		for (String key : params.keySet()) {
			String value = params.get(key);
			request.addParameter(key, value);
		}
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);

		// showForm
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<TiersIndexedDataView> list = (List<TiersIndexedDataView>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);

		// for (TiersIndexedData data : list) {
		// long numero = data.getNumero();
		// String nom1 = data.getNom1();
		// String nom2 = data.getNom2();
		// data = null;
		// }

		return list;
	}

	@Test
	@NotTransactional
	public void testRechercheForTous() throws Exception {

		loadDatabase();

		// Recherche tous les fors y compris les inactifs
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("noOfsFor", Integer.toString(MockCommune.Bussigny.getNoOFSEtendu()));
			List<TiersIndexedDataView> list = getTiersList(params);
			assertEquals(3, list.size());
		}
	}

	@Test
	@NotTransactional
	public void testRechercheForActifs() throws Exception {

		loadDatabase();

		// Recherche seulement les fors actifs
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("noOfsFor", Integer.toString(MockCommune.Bussigny.getNoOFSEtendu()));
		params.put("forPrincipalActif", "true");
		List<TiersIndexedDataView> list = getTiersList(params);
		assertEquals(1, list.size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void testShowForm() throws Exception {

		loadDatabase();
		request.setMethod("GET");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);

	}

	@Test
	@NotTransactional
	public void testOnSubmitWithCriteresWithNumCTB() throws Exception {

		loadDatabase();
		request.setMethod("POST");
		request.addParameter("numeroFormatte", "12300003");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(1, list.size());


	}

	@Test
	@NotTransactional
	public void testRechercheNomContient() throws Exception {

		loadDatabase();
		request.setMethod("POST");
		request.addParameter("nomRaison", "Cuendet");
		request.addParameter("typeRechercheDuNom", "CONTIENT");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(3, list.size());

	}

	@Test
	@NotTransactional
	public void testRechercheNomPhonetique() throws Exception {

		loadDatabase();
		request.setMethod("POST");
		request.addParameter("nomRaison", "Cuendet");
		request.addParameter("typeRechercheDuNom", "PHONETIQUE");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		Assert.assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(3, list.size());

	}

	@Test
	@NotTransactional
	public void testRechercheDateNaissance() throws Exception {

		loadDatabase();
		request.setMethod("POST");
		request.addParameter("dateNaissance", "23.01.1970");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(1, list.size()); // il y a 2 ctbs qui ont cette date de naissance, mais un des deux est un i107 qui n'est pas retourné par défaut.
	}

	@Test
	@NotTransactional
	public void testRechercheLocalite() throws Exception {

		loadDatabase();
		request.setMethod("POST");
		request.addParameter("localiteOuPays", "Lausanne");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertTrue(!list.isEmpty());

	}

	@Test
	@NotTransactional
	public void testRechercheNumAVS() throws Exception {

		loadDatabase();
		request.setMethod("POST");
		request.addParameter("numeroAVS", "7561234567897");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(3, list.size());

	}

	@Test
	@NotTransactional
	public void testRechercheNumAVSWithDash() throws Exception {

		loadDatabase();
		request.setMethod("POST");
		request.addParameter("numeroAVS", "75612.34.567.897");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		request.setMethod("GET");
		ModelAndView mav2 = controller.handleRequest(request, response);
		Map<?, ?> model2 = mav2.getModel();
		assertNotNull(model2);
		List<?> list = (List<?>) model2.get(TiersListController.TIERS_LIST_ATTRIBUTE_NAME);
		assertEquals(3, list.size());

	}

	private void loadDatabase() throws Exception {
		globalTiersIndexer.overwriteIndex();
		doInNewTransaction(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				TestData.loadTiersBasic(hibernateTemplate);
				return null;
			}
		});
		globalTiersIndexer.sync();
	}
}

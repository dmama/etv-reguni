package ch.vd.uniregctb.tiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.TestData;
import ch.vd.uniregctb.common.WebMockMvcTest;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;

/**
 * Test case du controlleur spring du m�me nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersListControllerTest extends WebMockMvcTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

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

	@Override
	protected Object[] getControllers() {
		return new Object[] { getBean(TiersListController.class, "tiersListController") };
	}

	@SuppressWarnings("unchecked")
	private List<TiersIndexedDataView> getTiersList(Map<String, String> params) throws Exception {
		final ResultActions resActions = get("/tiers/list.do", params, null);
		final MvcResult result = resActions.andReturn();
		Assert.assertNotNull(result);
		return (List<TiersIndexedDataView>) result.getModelAndView().getModel().get("list");
	}

	@SuppressWarnings("unchecked")
	private List<TiersIndexedDataView> doSearch(Map<String, String> params) throws Exception {
		final MockHttpSession session = new MockHttpSession();
		final ResultActions resActions = post("/tiers/list.do", params, session);
		final MvcResult result = resActions.andReturn();
		Assert.assertNotNull(result);
		if (result.getResponse().getStatus() == 200) {
			return (List<TiersIndexedDataView>) result.getModelAndView().getModel().get("list");
		}
		else if (result.getResponse().getStatus() == 302) {       // redirect
			final String location = result.getResponse().getHeader("Location");
			Assert.assertEquals("/tiers/list.do", location);

			final ResultActions getAction = get("/tiers/list.do", params, session);
			final MvcResult getResult = getAction.andReturn();
			Assert.assertNotNull(getResult);
			return (List<TiersIndexedDataView>) getResult.getModelAndView().getModel().get("list");
		}
		throw new IllegalArgumentException("Wrong status: " + result.getResponse().getStatus());
	}

	@Test
	public void testRechercheForTous() throws Exception {

		loadDatabase();

		// Recherche tous les fors y compris les inactifs
		{
			HashMap<String, String> params = new HashMap<>();
			params.put("noOfsFor", Integer.toString(MockCommune.Bussigny.getNoOFS()));
			List<TiersIndexedDataView> list = getTiersList(params);
			assertEquals(3, list.size());
		}
	}

	@Test
	public void testRechercheForActifs() throws Exception {

		loadDatabase();

		// Recherche seulement les fors actifs
		HashMap<String, String> params = new HashMap<>();
		params.put("noOfsFor", Integer.toString(MockCommune.Bussigny.getNoOFS()));
		params.put("forPrincipalActif", "true");
		List<TiersIndexedDataView> list = getTiersList(params);
		assertEquals(1, list.size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {
		loadDatabase();
		final MvcResult res = get("/tiers/list.do", null, null).andReturn();
		Assert.assertNotNull(res);

		final ModelAndView mav = res.getModelAndView();
		Assert.assertNotNull(mav);

		final Object command = mav.getModel().get("command");
		Assert.assertNotNull(command);
		Assert.assertEquals(TiersCriteriaView.class, command.getClass());
		Assert.assertTrue(((TiersCriteriaView) command).isEmpty());
	}

	@Test
	public void testOnSubmitWithCriteresWithNumCTB() throws Exception {

		loadDatabase();

		final Map<String, String> params = new HashMap<>();
		params.put("numeroFormatte", "12300003");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(1, list.size());
	}

	@Test
	public void testRechercheNomContient() throws Exception {
		loadDatabase();
		final Map<String, String> params = new HashMap<>();
		params.put("nomRaison", "Cuendet");
		params.put("typeRechercheDuNom", "CONTIENT");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(3, list.size());
	}

	@Test
	public void testRechercheNomPhonetique() throws Exception {
		loadDatabase();

		final Map<String, String> params = new HashMap<>();
		params.put("nomRaison", "Cuendet");
		params.put("typeRechercheDuNom", "PHONETIQUE");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(3, list.size());
	}

	@Test
	public void testRechercheDateNaissance() throws Exception {

		loadDatabase();

		final Map<String, String> params = new HashMap<>();
		params.put("dateNaissance", "23.01.1970");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(1, list.size()); // il y a 2 ctbs qui ont cette date de naissance, mais un des deux est un i107 qui n'est pas retourné par défaut.
	}

	@Test
	public void testRechercheLocalite() throws Exception {

		loadDatabase();

		final Map<String, String> params = new HashMap<>();
		params.put("localiteOuPays", "Lausanne");
		{
			final List<TiersIndexedDataView> list = doSearch(params);
			assertEquals(3, list.size());
		}

		params.put("typeTiers", "CONTRIBUABLE");
		{
			final List<TiersIndexedDataView> list = doSearch(params);
			assertEquals(2, list.size());
		}
	}

	@Test
	public void testRechercheNumAVS() throws Exception {

		loadDatabase();

		final Map<String, String> params = new HashMap<>();
		params.put("numeroAVS", "7561234567897");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(3, list.size());
	}

	@Test
	public void testRechercheNumAVSWithDash() throws Exception {

		loadDatabase();

		final Map<String, String> params = new HashMap<>();
		params.put("numeroAVS", "75612.34.567.897");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(3, list.size());
	}

	private void loadDatabase() throws Exception {
		globalTiersIndexer.overwriteIndex();
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				TestData.loadTiersBasic(hibernateTemplate);
				return null;
			}
		});
		globalTiersIndexer.sync();
	}
}

package ch.vd.unireg.tache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.common.WebMockMvcTest;
import ch.vd.unireg.tache.view.NouveauDossierListView;
import ch.vd.unireg.tache.view.TacheListView;

public class TacheControllerTest extends WebMockMvcTest {

	private static final String CONTROLLER_NAME = "tacheController";

	/**
	 * DB unit
	 */
	private static final String DB_UNIT_FILE = "TacheControllerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_FILE);
	}

	@Override
	protected Object[] getControllers() {
		return new Object[] { getBean(TacheController.class, CONTROLLER_NAME) };
	}

	@Test
	public void testShowFormTaches() throws Exception {
		final ResultActions ra = get("/tache/list.do", null, null);
		final MvcResult res = ra.andReturn();
		Assert.assertNotNull(res);
	}

	@Test
	public void testShowFormNouveauxDossiers() throws Exception {
		final ResultActions ra = get("/tache/list-nouveau-dossier.do", null, null);
		final MvcResult res = ra.andReturn();
		Assert.assertNotNull(res);
	}

	@Test
	public void testOnSubmitSearchTaches() throws Exception {

		final Map<String, String> params = new HashMap<>();
		params.put("officeImpot", Integer.toString(22));

		final MockHttpSession session = new MockHttpSession();
		final ResultActions resActions = get("/tache/list.do", params, session);
		final MvcResult result = resActions.andReturn();
		Assert.assertNotNull(result);

		final List<TacheListView> list;
		if (result.getResponse().getStatus() == 200) {
			list = (List<TacheListView>) result.getModelAndView().getModel().get("taches");
		}
		else if (result.getResponse().getStatus() == 302) {       // redirect
			final String location = result.getResponse().getHeader("Location");
			Assert.assertEquals("/tache/list.do", location);

			final ResultActions getAction = get("/tache/list.do", params, session);
			final MvcResult getResult = getAction.andReturn();
			Assert.assertNotNull(getResult);
			list = (List<TacheListView>) getResult.getModelAndView().getModel().get("taches");
		}
		else {
			throw new IllegalArgumentException("Wrong status: " + result.getResponse().getStatus());
		}

		Assert.assertNotNull(list);
	}

	@Test
	public void testOnSubmitSearchNouveauxDossiers() throws Exception {

		final Map<String, String> params = new HashMap<>();
		params.put("officeImpot", Integer.toString(22));

		final MockHttpSession session = new MockHttpSession();
		final ResultActions resActions = post("/tache/list-nouveau-dossier.do", params, session);
		final MvcResult result = resActions.andReturn();
		Assert.assertNotNull(result);

		final List<NouveauDossierListView> list;
		if (result.getResponse().getStatus() == 200) {
			list = (List<NouveauDossierListView>) result.getModelAndView().getModel().get("nouveauxDossiers");
		}
		else if (result.getResponse().getStatus() == 302) {       // redirect
			final String location = result.getResponse().getHeader("Location");
			Assert.assertEquals("/tache/list-nouveau-dossier.do", location);

			final ResultActions getAction = get("/tache/list-nouveau-dossier.do", params, session);
			final MvcResult getResult = getAction.andReturn();
			Assert.assertNotNull(getResult);
			list = (List<NouveauDossierListView>) getResult.getModelAndView().getModel().get("nouveauxDossiers");
		}
		else {
			throw new IllegalArgumentException("Wrong status: " + result.getResponse().getStatus());
		}

		Assert.assertNotNull(list);
	}


}

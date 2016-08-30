package ch.vd.uniregctb.tiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
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
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;

/**
 * Test case du controlleur spring du même nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersListControllerTest extends WebMockMvcTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(333908, RegDate.get(1974, 3, 22), "Cuendet", "Adrienne", true);
				final MockIndividu individu2 = addIndividu(333905, RegDate.get(1974, 3, 22), "Cuendet", "Biloute", true);
				final MockIndividu individu3 = addIndividu(674417, RegDate.get(1974, 3, 22), "Dardare", "Francois", true);
				final MockIndividu individu4 = addIndividu(327706, RegDate.get(1974, 3, 22), "Dardare", "Marcel", true);
				final MockIndividu individu5 = addIndividu(320073, RegDate.get(1952, 3, 21), "ERTEM", "Sabri", true);

				addAdresse(individu1, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, TypeAdresseCivil.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, TypeAdresseCivil.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu5, TypeAdresseCivil.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);

				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu5, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
			}
		});

		setWantIndexationTiers(true);
	}

	@Override
	protected Object[] getControllers() {
		return new Object[] { getBean(TiersListController.class, "tiersListController") };
	}

	@SuppressWarnings("unchecked")
	private List<TiersIndexedDataView> getTiersList(Map<String, String> params, @Nullable MockHttpSession session) throws Exception {
		final ResultActions resActions = get("/tiers/list.do", params, session);
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
			return getTiersList(params, session);
		}
		throw new IllegalArgumentException("Wrong status: " + result.getResponse().getStatus());
	}

	/**
	 * [SIFISC-11341] branchement sur la page de recherche depuis une autre application (avec utilisation de urlRetour) -> le deuxième appel renvoyait toujours les mêmes réponses
	 * que le premier, quels que soient les changements de paramètres opérés entre les deux
	 */
	@Test
	public void testSearchThroughGetAndUrlRetour() throws Exception {
		final MockHttpSession session = new MockHttpSession();      // nécessaire pour que les deux appels soient assimilés à la même session

		loadDatabase(false);

		// premier appel
		{
			final Map<String, String> params = new HashMap<>();
			params.put(TiersListController.URL_RETOUR_PARAMETER_NAME, "http://backHome");     // juste histoire de mettre quelque chose
			params.put(TiersListController.NUMERO_PARAMETER_NAME, "12300003");

			final List<TiersIndexedDataView> list = getTiersList(params, session);
			assertEquals(1, list.size());
			assertEquals((Long) 12300003L, list.get(0).getNumero());
		}

		// deuxième appel
		{
			final Map<String, String> params = new HashMap<>();
			params.put(TiersListController.URL_RETOUR_PARAMETER_NAME, "http://backHome");     // juste histoire de mettre quelque chose
			params.put(TiersListController.NUMERO_PARAMETER_NAME, "1678439");

			final List<TiersIndexedDataView> list = getTiersList(params, session);
			assertEquals(1, list.size());
			assertEquals((Long) 1678439L, list.get(0).getNumero());
		}
	}

	/**
	 * Vérification que l'arrivée sur la page de recherche (en interne dans l'application, i.e. sans "urlRetour") ré-utilise bien les critères précédemment utilisés
	 * même s'ils ne sont plus précisés la deuxième fois
	 */
	@Test
	public void testSearchThroughGetSansUrlRetour() throws Exception {
		final MockHttpSession session = new MockHttpSession();      // nécessaire pour que les deux appels soient assimilés à la même session

		loadDatabase(false);

		// premier appel
		{
			final Map<String, String> params = new HashMap<>();
			params.put(TiersListController.NUMERO_PARAMETER_NAME, "12300003");

			final List<TiersIndexedDataView> list = getTiersList(params, session);
			assertEquals(1, list.size());
			assertEquals((Long) 12300003L, list.get(0).getNumero());
		}

		// deuxième appel dans la même session sans paramètre à la requête cette fois -> les mêmes critères qu'auparavant doivent être utilisés car ils ont été sauvegardés en session
		{
			final Map<String, String> params = new HashMap<>();
			final List<TiersIndexedDataView> list = getTiersList(params, session);
			assertEquals(1, list.size());
			assertEquals((Long) 12300003L, list.get(0).getNumero());
		}
	}

	@Test
	public void testRechercheForTous() throws Exception {

		loadDatabase(false);

		// Recherche tous les fors y compris les inactifs
		{
			HashMap<String, String> params = new HashMap<>();
			params.put(TiersListController.NO_OFS_FOR_PARAMETER_NAME, Integer.toString(MockCommune.Bussigny.getNoOFS()));
			List<TiersIndexedDataView> list = getTiersList(params, null);
			assertEquals(3, list.size());
		}
	}

	@Test
	public void testRechercheForActifs() throws Exception {

		loadDatabase(false);

		// Recherche seulement les fors actifs
		HashMap<String, String> params = new HashMap<>();
		params.put(TiersListController.NO_OFS_FOR_PARAMETER_NAME, Integer.toString(MockCommune.Bussigny.getNoOFS()));
		params.put(TiersListController.FOR_PRINCIPAL_ACTIF_PARAMETER_NAME, "true");
		List<TiersIndexedDataView> list = getTiersList(params, null);
		assertEquals(1, list.size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {
		loadDatabase(false);
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

		loadDatabase(false);

		final Map<String, String> params = new HashMap<>();
		params.put("numeroFormatte", "12300003");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(1, list.size());
	}

	@Test
	public void testRechercheNomContient() throws Exception {
		loadDatabase(false);
		final Map<String, String> params = new HashMap<>();
		params.put("nomRaison", "Cuendet");
		params.put("typeRechercheDuNom", "CONTIENT");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(3, list.size());
	}

	@Test
	public void testRechercheNomPhonetique() throws Exception {
		loadDatabase(false);

		final Map<String, String> params = new HashMap<>();
		params.put("nomRaison", "Cuendet");
		params.put("typeRechercheDuNom", "PHONETIQUE");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(3, list.size());
	}

	@Test
	public void testRechercheDateNaissance() throws Exception {

		loadDatabase(false);

		final Map<String, String> params = new HashMap<>();
		params.put("dateNaissanceInscriptionRC", "23.01.1970");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(1, list.size()); // il y a 2 ctbs qui ont cette date de naissance, mais un des deux est un i107 qui n'est pas retourné par défaut.
	}

	@Test
	public void testRechercheLocalite() throws Exception {

		loadDatabase(true);     // j'aimerais que les Collectivités Administratives pré-existantes soient aussi ré-indexées

		final Map<String, String> params = new HashMap<>();
		params.put("localiteOuPays", "Morges");
		{
			final List<TiersIndexedDataView> list = doSearch(params);
			assertEquals(3, list.size());       // Une collectivité administrative (OID) + 2 PP
		}

		params.put("typeTiers", "CONTRIBUABLE");
		{
			final List<TiersIndexedDataView> list = doSearch(params);
			assertEquals(3, list.size());
		}

		params.put("typeTiers", "CONTRIBUABLE_PP");
		{
			final List<TiersIndexedDataView> list = doSearch(params);
			assertEquals(2, list.size());
		}
	}

	@Test
	public void testRechercheNumAVS() throws Exception {

		loadDatabase(false);

		final Map<String, String> params = new HashMap<>();
		params.put("numeroAVS", "7561234567897");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(3, list.size());
	}

	@Test
	public void testRechercheNumAVSWithDash() throws Exception {

		loadDatabase(false);

		final Map<String, String> params = new HashMap<>();
		params.put("numeroAVS", "75612.34.567.897");
		final List<TiersIndexedDataView> list = doSearch(params);
		assertEquals(3, list.size());
	}

	private void loadDatabase(boolean fullReindex) throws Exception {
		globalTiersIndexer.overwriteIndex();
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				TestData.loadTiersBasic(hibernateTemplate, false);
				return null;
			}
		});
		if (fullReindex) {
			globalTiersIndexer.indexAllDatabase();
		}
		globalTiersIndexer.sync();
	}
}

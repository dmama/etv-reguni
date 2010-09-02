package ch.vd.uniregctb.admin;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.UniregModeHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class TiersImportControllerTest extends WebTest {
	/**
	 * Le nom du controller à tester.
	 */
	private final static String CONTROLLER_NAME = "tiersImportController";

	private final static String DB_UNIT_FILE = "tiers-basic.xml";

	private GlobalTiersSearcher globalTiersSearcher;
	private TiersDAO tiersDAO;

	private TiersImportController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		globalTiersSearcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		servicePM.setUp(new DefaultMockServicePM());

		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(327706, RegDate.get(2005, 2, 21), "EMERY", "Lyah", false);
				final MockIndividu individu2 = addIndividu(674417, RegDate.get(1979, 2, 11), "DESCLOUX", "Pascaline", false);
				final MockIndividu individu3 = addIndividu(333908, RegDate.get(1973, 2, 21), "SCHMID", "Laurent", true);
				final MockIndividu individu4 = addIndividu(333905, RegDate.get(1979, 2, 11), "SCHMID", "Christine", false);
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

		controller = getBean(TiersImportController.class, CONTROLLER_NAME);
	}

	@Test
	public void testGetScriptFilenames() throws Exception {

		List<LoadableFileDescription> files = controller.getScriptFilenames();
		assertEquals(1, files.size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		UniregModeHelper testMode = getBean(UniregModeHelper.class, "uniregModeHelper");
		testMode.setTestMode("true");
		request.setMethod("GET");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull("l'objet model retourné est null", model);

		List<?> list = (List<?>) model.get("scriptFileNames");
		Assert.assertTrue("Aucun script trouvé dans le classpath", list != null && !list.isEmpty());
	}

	/**
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void testOnSubmit() throws Exception {

		request.setMethod("POST");
		request.addParameter("scriptFileName", DB_UNIT_FILE);
		request.addParameter("mode", "CLEAN_INSERT");

		final ModelAndView mav = controller.handleRequest(request, response);
		assertNotNull(mav);

		// on vérifie que l'import est un succès, c'est-à-dire que la vue redirige vers la page qui affiche une prévisualisation de la base de données
		final View view = mav.getView();
		assertNotNull(view);
		assertInstanceOf(RedirectView.class, view);

		final RedirectView rv =(RedirectView) view;
		assertEquals("dbpreview.do", rv.getUrl());

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				
				int nbTiers = tiersDAO.getCount(Tiers.class);
				assertEquals(22, nbTiers);
				int nbInIndex = globalTiersSearcher.getExactDocCount();
				assertEquals(19, nbInIndex); // => les individus 325631 et 325740 n'existent pas

				return null;
			}
		});
	}

}

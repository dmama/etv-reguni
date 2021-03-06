package ch.vd.unireg.admin;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockInfrastructureConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.utils.UniregModeHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TiersImportControllerTest extends WebTestSpring3 {
	/**
	 * Le nom du controller à tester.
	 */
	private static final String CONTROLLER_NAME = "tiersImportController";

	private static final String DB_UNIT_FILE = "tiers-basic.xml";

	private GlobalTiersSearcher globalTiersSearcher;
	private TiersDAO tiersDAO;

	private TiersImportController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		globalTiersSearcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		serviceInfra.setUp(new DefaultMockInfrastructureConnector() {
			@Override
			protected void init() {
				super.init();
				for (int i = 23; i < 100; ++i) {
					if (getCollectivite(i) == null) {
						add(new MockCollectiviteAdministrative(i, "Coll n°" + i));
					}
				}
			}
		});

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(327706, RegDate.get(2005, 2, 21), "EMERY", "Lyah", false);
				final MockIndividu individu2 = addIndividu(674417, RegDate.get(1979, 2, 11), "DESCLOUX", "Pascaline", false);
				final MockIndividu individu3 = addIndividu(333908, RegDate.get(1973, 2, 21), "SCHMID", "Laurent", true);
				final MockIndividu individu4 = addIndividu(333905, RegDate.get(1979, 2, 11), "SCHMID", "Christine", false);
				final MockIndividu individu5 = addIndividu(320073, RegDate.get(1952, 3, 21), "ERTEM", "Sabri", true);

				addIndividu(122937, RegDate.get(1946, 12, 16), "Allora", "Walter", true);
				addIndividu(122938, RegDate.get(1950, 10, 27), "Allora", "Violette", false);
				addIndividu(122939, RegDate.get(1981, 5, 26), "Allora", "Cédric", true);
				addIndividu(857307, RegDate.get(1978, 2, 8), "Allora", "Maude", false);
				addIndividu(1013955, RegDate.get(2011, 11, 4), "Allora", "Jenny", false);

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

		controller = getBean(TiersImportController.class, CONTROLLER_NAME);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetScriptFilenames() throws Exception {

		List<LoadableFileDescription> files = controller.getScriptFilenames();
		assertEquals(1, files.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testListScripts() throws Exception {

		UniregModeHelper testMode = getBean(UniregModeHelper.class, "uniregModeHelper");
		testMode.setTestMode("true");
		request.setMethod("GET");
		request.setRequestURI("/admin/tiersImport/list.do");
		ModelAndView mav = handle(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull("l'objet model retourné est null", model);

		List<?> list = (List<?>) model.get("listFilesName");
		Assert.assertTrue("Aucun script trouvé dans le classpath", list != null && !list.isEmpty());
	}

	@Test
	public void testImportBuiltinScript() throws Exception {

		new UniregModeHelper().setEnvironnement("Hudson");

		// Les paramètres de la requête
		request.setMethod("POST");
		request.addParameter("fileName", DB_UNIT_FILE);
		request.addParameter("action", "CLEAN_INSERT");
		request.setRequestURI("/admin/tiersImport/import.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// on vérifie que l'import est un succès, c'est-à-dire que la vue redirige vers la page qui affiche une prévisualisation de la base de données
		final String view = mav.getViewName();
		assertNotNull(view);
		assertEquals("redirect:/admin/dbpreview.do", view);

		doInNewTransaction(status -> {
			int nbTiers = tiersDAO.getCount(Tiers.class);
			assertEquals(126, nbTiers);
			int nbInIndex = globalTiersSearcher.getExactDocCount();
			assertEquals(123, nbInIndex); // => les individus 325631, 325740 et 333911 n'existent pas;
			return null;
		});
	}

	@Test
	public void testImportUploadedScript() throws Exception {

		new UniregModeHelper().setEnvironnement("Hudson");

		// On créé une requête multipart pour gérer les fichiers
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setSession(session);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		// Les paramètres de la requête
		request.setMethod("POST");
		request.addFile(new MockMultipartFile("scriptData", new FileInputStream(getFile("DBUnit4Import/" + DB_UNIT_FILE))));
		request.addParameter("mode", "CLEAN_INSERT");
		request.setRequestURI("/admin/tiersImport/upload.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// on vérifie que l'import est un succès, c'est-à-dire que la vue redirige vers la page qui affiche une prévisualisation de la base de données
		final String view = mav.getViewName();
		assertNotNull(view);
		assertEquals("redirect:/admin/dbpreview.do", view);

		doInNewTransaction(status -> {
			int nbTiers = tiersDAO.getCount(Tiers.class);
			assertEquals(126, nbTiers);
			int nbInIndex = globalTiersSearcher.getExactDocCount();
			assertEquals(123, nbInIndex); // => les individus 325631, 325740 et 333911 n'existent pas;
			return null;
		});
	}

}

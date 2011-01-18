package ch.vd.uniregctb.tiers;

import java.util.Map;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersAdresseControllerTest extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "tiersAdresseController";

	private final static String DB_UNIT_FILE = "TiersAdresseControllerTest.xml";

	 private final static String NUMERO_CTB_PARAMETER_NAME = "numero";

	private final static String ID_ADRESSE_PARAMETER_NAME = "idAdresse";

	private TiersAdresseController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		controller = getBean(TiersAdresseController.class, CONTROLLER_NAME);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				final MockIndividu individu2 = addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);

				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});
	}

	@Test
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("GET");
		request.addParameter(NUMERO_CTB_PARAMETER_NAME, "6789");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	@Test
	public void testShowFormExistingAdresse() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("GET");
		request.addParameter(NUMERO_CTB_PARAMETER_NAME, "6789");
		request.addParameter(ID_ADRESSE_PARAMETER_NAME, "5");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	@Test
	public void testOnSubmitAdresseEtrangere() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("numCTB", "67895");
		request.addParameter("localiteNpa", "Paris");
		request.addParameter("typeLocalite", "pays");
		request.addParameter("usage", "COURRIER");
		request.addParameter("paysNpa", "France");
		request.addParameter("paysOFS", "8212");
		request.addParameter("dateDebut", "12.02.2002");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get((long) 67895);
		assertEquals(1, tiers.getAdressesTiers().size());
	}


	@Test
	public void testOnSubmitAdresseSuisse() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("numCTB", "67895");
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("usage", "COURRIER");
		request.addParameter("dateDebut", "12.02.2002");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get((long) 67895);
		assertEquals(1, tiers.getAdressesTiers().size());


	}

	@Test
	public void testOnSubmitAdresseSuisseWithNoDateDebut() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.addParameter("numCTB", "67895");
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("usage", "COURRIER");
		request.setMethod("POST");
		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get((long) 67895);
		assertEquals(0, tiers.getAdressesTiers().size());


	}

	@Test
	public void testOnSubmitAdresseEtrangereWithNoDateDebut() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.addParameter("numCTB", "67895");
		request.addParameter("localiteNpa", "Paris");
		request.addParameter("typeLocalite", "pays");
		request.addParameter("usage", "COURRIER");
		request.addParameter("paysNpa", "France");
		request.addParameter("paysOFS", "8212");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get((long) 67895);
		assertEquals(0, tiers.getAdressesTiers().size());
	}

	@Test
	public void testOnSubmitModifyAdresseSuisse() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.addParameter("numCTB", "6789");
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("usage", "COURRIER");
		request.addParameter("dateDebut", "23.08.2008");
		request.addParameter("id", "192");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		AdresseTiersDAO adresseTiersDAO = getBean(AdresseTiersDAO.class, "adresseTiersDAO");
		AdresseTiers adresseTiers = adresseTiersDAO.get((long) 192);
		AdresseSuisse addSuisse = (AdresseSuisse)adresseTiers ;
		assertEquals(165, addSuisse.getNumeroOrdrePoste().intValue());


	}

	@Test
	public void testOnSubmitModifyAdresseEtrangere() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("numCTB", "6789");
		request.addParameter("localiteNpa", "Marseille");
		request.addParameter("typeLocalite", "pays");
		request.addParameter("usage", "COURRIER");
		request.addParameter("paysNpa", "France");
		request.addParameter("paysOFS", "8212");
		request.addParameter("dateDebut", "12.02.2006");
		request.addParameter("id", "5");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		AdresseTiersDAO adresseTiersDAO = getBean(AdresseTiersDAO.class, "adresseTiersDAO");
		AdresseTiers adresseTiers = adresseTiersDAO.get((long) 5);
		AdresseEtrangere addEtrangere = (AdresseEtrangere)adresseTiers ;
		assertEquals("Marseille", addEtrangere.getNumeroPostalLocalite());

	}



}

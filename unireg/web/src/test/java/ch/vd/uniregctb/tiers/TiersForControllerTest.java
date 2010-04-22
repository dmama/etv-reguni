package ch.vd.uniregctb.tiers;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.manager.ForFiscalManager;

public class TiersForControllerTest  extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "tiersForController";

	private final static String DB_UNIT_FILE = "TiersForControllerTest.xml";

	private ForFiscalManager forFiscalManager;

	private TiersForController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		controller = getBean(TiersForController.class, CONTROLLER_NAME);

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.setMethod("GET");
		request.addParameter("id", "1");
		try {
			controller.handleRequest(request, response);
			fail();
		} catch (Exception e){
			logger.debug("exception a ignorée");
		}
		
		request.setMethod("GET");
		request.addParameter("idFor", "1");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitForPrincipalMotifOuverture() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(12600002));
		
		//création d'un for principal sans motif d'ouverture
		request.addParameter("numero", "12600002");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("numeroForFiscalCommune", "5586");
		request.addParameter("dateOuverture", "01.01.2007");
		request.addParameter("dateFermeture", "01.01.2008");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.addParameter("motifFermeture", "DEPART_HC");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?>model = mav.getModel();
		assertNotNull(model);
		assertEquals(3, tiers.getForsFiscaux().size());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitForPrincipalMotifFermeture() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(12600002));
		//création d'un for principal sans motif de fermeture
		request.addParameter("numero", "12600002");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("numeroForFiscalCommune", "5586");
		request.addParameter("dateOuverture", "01.01.2007");
		request.addParameter("dateFermeture", "01.01.2008");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.addParameter("motifOuverture", "ARRIVEE_HC");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?>model = mav.getModel();
		assertNotNull(model);
		assertEquals(3, tiers.getForsFiscaux().size());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitForPrincipalDateDebut() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(12600002));
		//création d'un for principal sans date début
		request.addParameter("numero", "12600002");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("numeroForFiscalCommune", "5586");
		request.addParameter("dateFermeture", "01.01.2008");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.addParameter("motifOuverture", "ARRIVEE_HC");
		request.addParameter("motifFermeture", "DEPART_HC");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?>model = mav.getModel();
		assertNotNull(model);
		assertEquals(3, tiers.getForsFiscaux().size());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitForPrincipalDateDebutFuture() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(12600002));
		//création d'un for principal avec date de début future
		request.addParameter("numero", "12600002");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("numeroForFiscalCommune", "5586");
		request.addParameter("dateOuverture", "01.01.2027");
		request.addParameter("dateFermeture", "01.01.2008");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.addParameter("motifOuverture", "ARRIVEE_HC");
		request.addParameter("motifFermeture", "DEPART_HC");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?>model = mav.getModel();
		assertNotNull(model);
		assertEquals(3, tiers.getForsFiscaux().size());

	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitForPrincipalDateFinFuture() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(12600002));
		//création d'un for principal avec date de fin future
		request.addParameter("numero", "12600002");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("numeroForFiscalCommune", "5586");
		request.addParameter("dateOuverture", "01.01.2007");
		request.addParameter("dateFermeture", "01.01.2028");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.addParameter("motifOuverture", "ARRIVEE_HC");
		request.addParameter("motifFermeture", "DEPART_HC");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?>model = mav.getModel();
		assertNotNull(model);
		assertEquals(3, tiers.getForsFiscaux().size());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitForPrincipalDateError() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(12600002));
		//création d'un for principal avec date de fin avant date de début
		request.addParameter("numero", "12600002");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("numeroForFiscalCommune", "5586");
		request.addParameter("dateOuverture", "01.01.2008");
		request.addParameter("dateFermeture", "01.01.2007");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.addParameter("motifOuverture", "ARRIVEE_HC");
		request.addParameter("motifFermeture", "DEPART_HC");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?>model = mav.getModel();
		assertNotNull(model);
		assertEquals(3, tiers.getForsFiscaux().size());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmitForPrincipal() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(12600002));
		//création d'un for principal
		request.addParameter("numero", "12600002");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("numeroForFiscalCommune", "5586");
		request.addParameter("dateOuverture", "01.01.2007");
		request.addParameter("dateFermeture", "01.01.2008");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.addParameter("motifOuverture", "ARRIVEE_HC");
		request.addParameter("motifFermeture", "DEPART_HC");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?>model = mav.getModel();
		assertNotNull(model);
		assertEquals(4, tiers.getForsFiscaux().size());
	}
	
		/*
		 * TODO (FDE) enrichir test ?
		 */

	public ForFiscalManager getForFiscalManager() {
		return forFiscalManager;
	}

	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}


}

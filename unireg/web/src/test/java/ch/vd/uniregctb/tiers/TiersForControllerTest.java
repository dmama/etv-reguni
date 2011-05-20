package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
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

	@Test
	public void testOnSubmitForPrincipalMotifOuverture() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		Tiers tiers = tiersDAO.get((long) 12600002);
		
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
	
	@Test
	public void testOnSubmitForPrincipalMotifFermeture() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		Tiers tiers = tiersDAO.get((long) 12600002);
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
	
	@Test
	public void testOnSubmitForPrincipalDateDebut() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		Tiers tiers = tiersDAO.get((long) 12600002);
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
	
	@Test
	public void testOnSubmitForPrincipalDateDebutFuture() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		Tiers tiers = tiersDAO.get((long) 12600002);
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
	
	@Test
	public void testOnSubmitForPrincipalDateFinFuture() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		Tiers tiers = tiersDAO.get((long) 12600002);
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
	
	@Test
	public void testOnSubmitForPrincipalDateError() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		Tiers tiers = tiersDAO.get((long) 12600002);
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
	
	@Test
	public void testOnSubmitForPrincipal() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		Tiers tiers = tiersDAO.get((long) 12600002);
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

	/**
	 * [UNIREG-3338] en cas de création d'un nouveau for fiscal, le pays doit être valide
	 */
	@Test
	public void testAddForPrincipalSurPaysInvalide() throws Exception {

		final Long id = doInNewTransactionAndSession(new TxCallback<Long>(){
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		// création d'un for principal sur un pays invalide
		request.addParameter("numero", id.toString());
		request.addParameter("genreImpot", GenreImpot.REVENU_FORTUNE.name());
		request.addParameter("motifRattachement", MotifRattachement.DOMICILE.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.PAYS_HS.name());
		request.addParameter("numeroForFiscalPays", String.valueOf(MockPays.RDA.getNoOFS()));
		request.addParameter("dateOuverture", "01.01.2007");
		request.addParameter("modeImposition", ModeImposition.ORDINAIRE.name());
		request.addParameter("motifOuverture", MotifFor.DEPART_HS.name());
		request.setMethod("POST");

		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		// On vérifie que l'ouverture du for sur le pays invalide a bien été interdit
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(1, bindingResult.getErrorCount());

		final List<?> errors = bindingResult.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("libPays", error.getField());
		assertEquals("error.pays.non.valide", error.getCode());
	}

	/**
	 * [UNIREG-3338] en cas de modification d'un for fiscal existant, le pays peut être invalide
	 */
	@Test
	public void testFermetureForPrincipalSurPaysInvalide() throws Exception {

		class Ids {
			Long pp;
			Long ffp;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1960, 1, 1), MotifFor.ARRIVEE_HS, MockPays.RDA);
				ids.pp = pp.getId();
				ids.ffp = ffp.getId();
				return null;
			}
		});

		// mise-à-jour des dates sur un for principal pré-existant avec un pays invalide
		request.addParameter("id", ids.ffp.toString());
		request.addParameter("numero", ids.pp.toString());
		request.addParameter("genreImpot", GenreImpot.REVENU_FORTUNE.name());
		request.addParameter("motifRattachement", MotifRattachement.DOMICILE.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.PAYS_HS.name());
		request.addParameter("numeroForFiscalPays", String.valueOf(MockPays.RDA.getNoOFS()));
		request.addParameter("dateOuverture", "01.01.1960");
		request.addParameter("dateFermeture", "01.01.1970");
		request.addParameter("modeImposition", ModeImposition.ORDINAIRE.name());
		request.addParameter("motifOuverture", MotifFor.DEPART_HS.name());
		request.addParameter("motifFermeture", MotifFor.ARRIVEE_HS.name());
		request.setMethod("POST");

		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		// On vérifie que la modification du for sur le pays invalide a autorisée
		assertNull(getBindingResult(mav));

		// Vérifie que le for fiscal a bien été mis-à-jour
		final Tiers tiers = tiersDAO.get(ids.pp);
		assertNotNull(tiers);

		final List<ForFiscal> forsFiscaux = new ArrayList<ForFiscal>(tiers.getForsFiscaux());
		assertEquals(1, forsFiscaux.size());

		final ForFiscalPrincipal for0 = (ForFiscalPrincipal) forsFiscaux.get(0);
		assertEquals(TypeAutoriteFiscale.PAYS_HS, for0.getTypeAutoriteFiscale());
		assertEquals(MockPays.RDA.getNoOFS(), for0.getNumeroOfsAutoriteFiscale().intValue());
		assertEquals(date(1970, 1, 1), for0.getDateFin());
	}

	public ForFiscalManager getForFiscalManager() {
		return forFiscalManager;
	}

	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}


}

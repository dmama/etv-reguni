package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.WebTest;
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
	@Transactional(rollbackFor = Throwable.class)
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
	 * [UNIREG-3338] en cas de modification d'un for fiscal existant, le pays peut être invalide
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		request.addParameter("idFor", ids.ffp.toString());
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

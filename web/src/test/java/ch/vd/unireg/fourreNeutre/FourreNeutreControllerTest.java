package ch.vd.unireg.fourreNeutre;

import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.fourreNeutre.view.FourreNeutreView;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FourreNeutreControllerTest extends WebTestSpring3 {

	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu1 = addIndividu(320073L, RegDate.get(1960, 1, 1), "Totor", "Marcel", true);
				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);

			}
		});
	}

	/**
	 * Teste le chargement de l'écran pour impression de fourre neutre
	 */
	@Test
	public void testImprimerFourreNeutreGet() throws Exception {

		final Long tiersId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfred", "Dupontel", date(1960, 5, 12), Sexe.MASCULIN);
			addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HC, MockCommune.Vaulion);
			return pp.getId();
		});

		// affiche la page de création d'une nouvelle DI
		request.setMethod("GET");
		request.addParameter("numero", tiersId.toString());
		request.setRequestURI("/fourre-neutre/imprimer.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il n'y a pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		final FourreNeutreView view = (FourreNeutreView) mav.getModel().get("command");
		assertNotNull(view);
		assertNull(view.getPeriodeFiscale());
		assertEquals(tiersId.longValue(), view.getTiersId());
	}


	/**
	 * Teste l'impression d'une nouvelle fourre neutre
	 */
	@Test
	public void testImprimerFourreNeutrePostAvecMauvaisType() throws Exception {

		final Long tiersId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable d = addDebiteur();
			return d.getId();
		});

		request.setMethod("POST");
		request.addParameter("tiersId", tiersId.toString());
		request.addParameter("periodeFiscale", "2013");
		request.setRequestURI("/fourre-neutre/imprimer.do");

		try {
			// exécution de la requête
			final ModelAndView results = handle(request, response);
		} catch (AccessDeniedException e){
			assertEquals("Le tiers "+tiersId+" n'est pas autorisé pour la génération de fourre neutre",e.getMessage());
		}


	}
}

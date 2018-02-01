package ch.vd.uniregctb.fourreNeutre;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.WebTestSpring3;
import ch.vd.uniregctb.fourreNeutre.view.FourreNeutreView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

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

		final Long tiersId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Dupontel", date(1960, 5, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HC, MockCommune.Vaulion);
				return pp.getId();
			}
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

		final Long tiersId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable d = addDebiteur();
				return d.getId();
			}
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
	/**
	 * Teste l'impression d'une nouvelle fourre neutre
	 */
//	@Test
//	public void testImprimerFourreNeutrePost() throws Exception {
//
//		final Long tiersId = doInNewTransactionAndSession(new TxCallback<Long>() {
//			@Override
//			public Long execute(TransactionStatus status) throws Exception {
//				final PersonnePhysique pp = addNonHabitant("Alfred", "Dupontel", date(1960, 5, 12), Sexe.MASCULIN);
//				return pp.getId();
//			}
//		});
//
//		request.setMethod("POST");
//		request.addParameter("tiersId", tiersId.toString());
//		request.addParameter("periodeFiscale", "2013");
//		request.setRequestURI("/fourre-neutre/imprimer.do");
//
//		// exécution de la requête
//		final ModelAndView results = handle(request, response);
//		assertNull(results); // si le résultat est différent de null, c'est qu'il y a eu une erreur et qu'on a reçu un redirect
//	}

}

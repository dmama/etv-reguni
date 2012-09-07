package ch.vd.uniregctb.decl;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.WebTestSpring3;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DeclarationImpotControllerTest extends WebTestSpring3 {

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
	 * Teste la prévisualisation avant impression d'une nouvelle déclaration d'impôt
	 */
	@Test
	public void testImprimerDIGet() throws Exception {

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
		request.addParameter("tiersId", tiersId.toString());
		request.addParameter("debut", "20070101");
		request.addParameter("fin", "20071231");
		request.addParameter("typeDocument", TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL.toString());
		request.addParameter("delaiRetour", "60");
		request.addParameter("imprimable", "false");
		request.setRequestURI("/di/imprimer.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il n'y a pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		final ImprimerNouvelleDeclarationImpotView view = (ImprimerNouvelleDeclarationImpotView) mav.getModel().get("command");
		assertNotNull(view);
		assertEquals(date(2007, 1, 1), view.getDateDebutPeriodeImposition());
		assertEquals(date(2007, 12, 31), view.getDateFinPeriodeImposition());
	}

	/**
	 * Teste la création et l'impression d'une nouvelle déclaration d'impôt
	 */
	@Test
	public void testImprimerDIPost() throws Exception {

		final Long tiersId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Dupontel", date(1960, 5, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HC, MockCommune.Vaulion);

				final PeriodeFiscale p2007 = addPeriodeFiscale(2007);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, p2007);
				return pp.getId();
			}
		});

		request.setMethod("POST");
		request.addParameter("tiersId", tiersId.toString());
		request.addParameter("dateDebutPeriodeImposition", "01.01.2007");
		request.addParameter("dateFinPeriodeImposition", "31.12.2007");
		request.addParameter("typeDocument", TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL.toString());
		request.addParameter("typeAdresseRetour", TypeAdresseRetour.CEDI.toString());
		request.addParameter("delaiAccorde", "15.10.2012");
		request.setRequestURI("/di/imprimer.do");

		// exécution de la requête
		final ModelAndView results = handle(request, response);
		assertNull(results); // si le résultat est différent de null, c'est qu'il y a eu une erreur et qu'on a reçu un redirect

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(tiersId);
				assertEquals(1, tiers.getDeclarations().size());
			}
		});
	}
}

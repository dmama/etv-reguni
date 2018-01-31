package ch.vd.uniregctb.di;

import javax.servlet.http.HttpSession;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.FlashMessage;
import ch.vd.uniregctb.common.WebTestSpring3;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.di.view.AjouterDelaiDeclarationView;
import ch.vd.uniregctb.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
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
		request.setRequestURI("/di/imprimer-pp.do");

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
		request.addParameter("periodeFiscale", "2007");
		request.addParameter("dateDebutPeriodeImposition", "01.01.2007");
		request.addParameter("dateFinPeriodeImposition", "31.12.2007");
		request.addParameter("typeDocument", TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL.toString());
		request.addParameter("typeAdresseRetour", TypeAdresseRetour.CEDI.toString());
		request.addParameter("delaiAccorde", RegDateHelper.dateToDisplayString(RegDate.get().addMonths(1)));
		request.addParameter("typeContribuable", "PP");
		request.setRequestURI("/di/imprimer.do");

		// exécution de la requête
		final ModelAndView results = handle(request, response);
		assertNull(results); // si le résultat est différent de null, c'est qu'il y a eu une erreur et qu'on a reçu un redirect

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(tiersId);
				assertEquals(1, tiers.getDocumentsFiscaux().size());
			}
		});
	}

	/**
	 * Teste l'impossibilité d'imprimer des duplicatas en cas de problème de période d'imposition
	 */
	@Test
	public void testImprimerDuplicataDIMauvaisePeriode() throws Exception {

		final Long diId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Dupontel", date(1960, 5, 12), Sexe.MASCULIN);
				final EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(pp, null, date(2008, 6, 12), date(2010, 7, 13));

				final MenageCommun menage = ensembleTiersCouple.getMenage();
				addForPrincipal(menage, date(2008, 6, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2010, 7, 13), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				                MockCommune.Vaulion);

				final PeriodeFiscale p2010 = addPeriodeFiscale(2010);
				final ModeleDocument modeleDocument2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, p2010);
				Declaration declaration = addDeclarationImpot(menage, p2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument2010);
				return declaration.getId();
			}
		});

		request.setMethod("POST");
		request.addParameter("idDI", diId.toString());
		request.addParameter("selectedTypeDocument", TypeDocument.DECLARATION_IMPOT_VAUDTAX.toString());
		request.setRequestURI("/di/duplicata-pp.do");

		// exécution de la requête
		final ModelAndView results = handle(request, response);
		assertNotNull(results);

		// on test la présence du message flash
		HttpSession session = request.getSession();
		final FlashMessage flash = (FlashMessage) session.getAttribute("flash");
		assertNotNull(flash);
		final String messageAttendue = "Echec de l'impression du duplicata, le contribuable n'a pas de données valides à la fin de la période de la déclaration d'impôt.";
		assertEquals(messageAttendue, flash.getMessage());
	}

	/**
	 * [SIFISC-6918] Ce test s'assure qu'il n'est pas possible d'ajouter un délai plus court que le délai accordé actuel.
	 */
	@Test
	public void testAjouterDelaiDIPlusCourtQueDejaAccorde() throws Exception {

		final int anneeCourante = RegDate.get().year();

		final Long diId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Dupontel", date(1960, 5, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(anneeCourante, 1, 2), MotifFor.ARRIVEE_HC, MockCommune.Vaulion);

				final PeriodeFiscale periodeFiscale = addPeriodeFiscale(anneeCourante);
				final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periodeFiscale);
				final Declaration declaration = addDeclarationImpot(pp, periodeFiscale, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);
				addDelaiDeclaration(declaration, RegDate.get(), RegDate.get().addMonths(3), EtatDelaiDocumentFiscal.ACCORDE);
				return declaration.getId();
			}
		});

		request.setMethod("POST");
		request.addParameter("idDeclaration", diId.toString());
		request.addParameter("dateDemande", RegDateHelper.dateToDisplayString(RegDate.get()));
		request.addParameter("delaiAccordeAu", RegDateHelper.dateToDisplayString(RegDate.get().addMonths(1)));
		request.setRequestURI("/di/delai/ajouter-pp.do");

		// exécution de la requête
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur le délai accordé trop court a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("delaiAccordeAu", error.getField());
		assertEquals("error.delai.accorde.invalide", error.getCode());

		final AjouterDelaiDeclarationView ajouterView = (AjouterDelaiDeclarationView) mav.getModel().get("command");
		assertNotNull(ajouterView);
	}

	/**
	 * [SIFISC-7486] Ce test s'assure qu'il est possible de quittancer une déclaration d'impôt hors-canton
	 */
	@Test
	public void testQuittancerDIHorsCanton() throws Exception {

		final int annee = RegDate.get().year() - 2;

		final Long diId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Dupontel", date(1960, 5, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 6, 12), MotifFor.ARRIVEE_HC, MockCommune.Geneve);

				final PeriodeFiscale periodeFiscale = addPeriodeFiscale(annee);
				final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periodeFiscale);
				final Declaration declaration = addDeclarationImpot(pp, periodeFiscale, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, modeleDocument);
				addEtatDeclarationEmise(declaration, date(annee + 1, 1, 15));
				return declaration.getId();
			}
		});

		final RegDate dateQuittance = date(annee + 1, 3, 15);

		request.setMethod("POST");
		request.addParameter("id", diId.toString());
		request.addParameter("typeDocument", TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE.name());
		request.addParameter("dateRetour", RegDateHelper.dateToDisplayString(dateQuittance));
		request.setRequestURI("/di/etat/ajouter-quittance.do");

		// exécution de la requête
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il n'y a pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEmpty(result.getAllErrors());

		// On vérifie que la déclaration est bien quittancée et qu'elle est restée de type 'hors canton'
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, diId);
				assertNotNull(di);
				assertEquals(dateQuittance, di.getDateRetour());
				assertEquals(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, di.getTypeDeclaration());
			}
		});
	}

	/**
	 * Ce test s'assure qu'il est possible de quittancer une déclaration d'impôt ordinaire 'batch' sans qu'elle passe à 'local'.
	 */
	@Test
	public void testQuittancerDIOrdinaireBatch() throws Exception {

		final int annee = RegDate.get().year() - 2;

		final Long diId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Dupontel", date(1960, 5, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 6, 12), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);

				final PeriodeFiscale periodeFiscale = addPeriodeFiscale(annee);
				final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periodeFiscale);
				final Declaration declaration = addDeclarationImpot(pp, periodeFiscale, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);
				addEtatDeclarationEmise(declaration, date(annee + 1, 1, 15));
				return declaration.getId();
			}
		});

		final RegDate dateQuittance = date(annee + 1, 3, 15);

		request.setMethod("POST");
		request.addParameter("id", diId.toString());
		request.addParameter("typeDocument", TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL.name());
		request.addParameter("dateRetour", RegDateHelper.dateToDisplayString(dateQuittance));
		request.setRequestURI("/di/etat/ajouter-quittance.do");

		// exécution de la requête
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il n'y a pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEmpty(result.getAllErrors());

		// On vérifie que le type de document est resté 'batch'
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, diId);
				assertNotNull(di);
				assertEquals(dateQuittance, di.getDateRetour());
				assertEquals(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, di.getTypeDeclaration());
			}
		});
	}
}

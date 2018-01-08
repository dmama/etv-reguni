package ch.vd.uniregctb.di;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.AjouterEtatDeclarationView;
import ch.vd.uniregctb.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.ModeleFeuille;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeclarationImpotControllerValidatorTest extends WebTest {

	private DeclarationImpotControllerValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final DeclarationImpotOrdinaireDAO diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		validator = new DeclarationImpotControllerValidator();
		validator.setTiersDAO(tiersDAO);
		validator.setDiDAO(diDAO);
		validator.setTiersService(tiersService);
		validator.setManager(getBean(DeclarationImpotEditManager.class, "diEditManager"));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEditDelai() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique pp = addNonHabitant("Eric", "Masserey", date(1976, 3, 12), Sexe.MASCULIN);
				return pp.getId();
			}
		});

		ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView();
		view.setTiersId(id);

		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(3, allErrors.size());

		final ObjectError error0 = allErrors.get(0);
		assertEquals("error.date.debut.vide", error0.getCode());

		final ObjectError error1 = allErrors.get(1);
		assertEquals("error.date.fin.vide", error1.getCode());

		final ObjectError error2 = allErrors.get(2);
		assertEquals("error.delai.accorde.vide", error2.getCode());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddDateRetourSuiteEmission() throws Exception {
		class Ids {
			Long ericId;
			Long declarationId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
				final ModeleDocument declarationComplete2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2010);

				// Un contribuable quelconque
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(eric, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2010);
				addEtatDeclarationEmise(declaration2010, date(2011, 1, 24));
				ids.declarationId = declaration2010.getId();

				return null;
			}
		});

		//On tente de la quittancer au 20.01.2011 -> Blocage et message d'erreur
		AjouterEtatDeclarationView view = new AjouterEtatDeclarationView();
		view.setId(ids.declarationId);
		view.setDateRetour(date(2011, 1, 20));
		view.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.date.retour.anterieure.date.emission", error.getCode());

		//On la quittance au 26.01.2011 -> date acceptée -> pas de message d'erreur
		view.setDateRetour(date(2011, 1, 26));
		final Errors errors2 = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors2);
		final List<ObjectError> allErrors2 = errors2.getAllErrors();
		assertNotNull(allErrors2);
		assertEquals(0, allErrors2.size());
	}

	//SIFISC-90
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCorrectionRetourAvecDateAnterieurEmission() throws Exception {
		class Ids {
			Long jeanneId;
			Long declarationId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
				final ModeleDocument declarationComplete2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2010);

				// Un contribuable quelconque
				PersonnePhysique jeanne = addNonHabitant("Jeanne", "dupont", date(1965, 4, 13), Sexe.MASCULIN);
				ids.jeanneId = jeanne.getNumero();
				addForPrincipal(jeanne, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(jeanne, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2010);
				addEtatDeclarationEmise(declaration2010, date(2011, 1, 24));
				addEtatDeclarationRetournee(declaration2010, date(2011, 3, 26));
				ids.declarationId = declaration2010.getId();

				return null;
			}
		});

		//On tente de la quittancer au 20.01.2011 -> Blocage et message d'erreur
		AjouterEtatDeclarationView view = new AjouterEtatDeclarationView();
		view.setId(ids.declarationId);
		view.setDateRetour(date(2011, 1, 20));
		view.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());
		final ObjectError error = allErrors.get(0);
		assertEquals("error.date.retour.anterieure.date.emission", error.getCode());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddDateRetourSuiteSommation() throws Exception {
		class Ids {
			Long gustaveId;
			Long declarationId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
				final ModeleDocument declarationComplete2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2010);

				// Un contribuable quelconque
				PersonnePhysique gustave = addNonHabitant("Gustave", "Eiffel", date(1965, 4, 13), Sexe.MASCULIN);
				ids.gustaveId = gustave.getNumero();
				addForPrincipal(gustave, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(gustave, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2010);
				addEtatDeclarationEmise(declaration2010, date(2011, 1, 24));
				addEtatDeclarationSommee(declaration2010, date(2011, 4, 26), date(2011, 4, 28), null);
				ids.declarationId = declaration2010.getId();

				return null;
			}
		});

		//On tente de la quittancer au 20.01.2011 -> Blocage et message d'erreur
		AjouterEtatDeclarationView view = new AjouterEtatDeclarationView();
		view.setId(ids.declarationId);
		view.setDateRetour(date(2011, 4, 24));
		view.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.date.retour.anterieure.date.emission.sommation", error.getCode());

		//On la quittance au 26.08.2011 -> date acceptée -> pas de message d'erreur
		view.setDateRetour(date(2011, 5, 2));
		final Errors errors2 = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors2);
		final List<ObjectError> allErrors2 = errors2.getAllErrors();
		assertNotNull(allErrors2);
		assertEquals(0, allErrors2.size());
	}

	//SIFISC-90
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCorrectionRetourAvecDateAnterieurSommation() throws Exception {
		class Ids {
			Long coridonId;
			Long declarationId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
				final ModeleDocument declarationComplete2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2010);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2010);

				// Un contribuable quelconque
				PersonnePhysique coridon = addNonHabitant("Coridon", "De la Mouette", date(1965, 4, 13), Sexe.MASCULIN);
				ids.coridonId = coridon.getNumero();
				addForPrincipal(coridon, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(coridon, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2010);
				addEtatDeclarationEmise(declaration2010, date(2011, 1, 24));
				addEtatDeclarationSommee(declaration2010, date(2011, 7, 26), date(2011, 7, 28), null);
				ids.declarationId = declaration2010.getId();

				return null;
			}
		});

		//On tente de la quittancer au 20.06.2011 -> Blocage et message d'erreur
		AjouterEtatDeclarationView view = new AjouterEtatDeclarationView();
		view.setId(ids.declarationId);
		view.setDateRetour(date(2011, 4, 26));
		view.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());
		final ObjectError error = allErrors.get(0);
		assertEquals("error.date.retour.anterieure.date.emission.sommation", error.getCode());
	}

	@Test
	public void testChevauchementExerciceCommercial() throws Exception {
		final RegDate dateDebut = date(2000, 7, 1);
		final int annee = RegDate.get().year();

		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto le héros SA");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(entreprise, dateDebut, DayMonth.get(6, 30), 12);          // tous les 30.06
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);
				return entreprise.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// on tente d'ajouter une nouvelle DI qui chevauche la fontière du 30.06 (= fin d'exercice commercial)
				final ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView();
				view.setTiersId(pmId);
				view.setPeriodeFiscale(annee);
				view.setDateDebutPeriodeImposition(date(annee, 1, 1));
				view.setDateFinPeriodeImposition(date(annee, 8, 31));        // 2 mois de trop
				view.setDelaiAccorde(date(annee + 1, 4, 30));
				view.setTypeDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH);

				final Errors errors = new BeanPropertyBindingResult(view, "view");
				validator.validate(view, errors);

				final List<ObjectError> allErrors = errors.getAllErrors();
				assertNotNull(allErrors);
				assertEquals(1, allErrors.size());

				final ObjectError error = allErrors.get(0);
				assertEquals("error.declaration.cheval.plusieurs.exercices.commerciaux", error.getCode());
			}
		});
	}

	@Test
	public void testMauvaisePeriodeFiscale() throws Exception {
		final RegDate dateDebut = date(2000, 7, 1);

		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto le héros SA");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(entreprise, dateDebut, DayMonth.get(6, 30), 12);          // tous les 30.06
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);
				return entreprise.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// on tente d'ajouter une nouvelle DI qui chevauche la fontière du 30.06 (= fin d'exercice commercial)
				final ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView();
				view.setTiersId(pmId);
				view.setPeriodeFiscale(2016);
				view.setDateDebutPeriodeImposition(date(2016, 7, 1));
				view.setDateFinPeriodeImposition(date(2017, 6, 30));        // pas la bonne année !
				view.setDelaiAccorde(RegDate.get().addMonths(2));
				view.setTypeDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH);

				final Errors errors = new BeanPropertyBindingResult(view, "view");
				validator.validate(view, errors);

				final List<ObjectError> allErrors = errors.getAllErrors();
				assertNotNull(allErrors);
				assertEquals(1, allErrors.size());

				final ObjectError error = allErrors.get(0);
				assertEquals("error.date.fin.pas.dans.periode.fiscale", error.getCode());
			}
		});
	}

	@Test
	public void testNouvelleDeclarationChevalAnneesCiviles() throws Exception {
		final RegDate dateDebut = date(2000, 7, 1);

		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto le héros SA");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(entreprise, dateDebut, DayMonth.get(6, 30), 12);          // tous les 30.06
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);
				return entreprise.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// on ajoute une nouvelle DI qui s'arrête juste à la fontière du 30.06 (= fin d'exercice commercial)
				final ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView();
				view.setTiersId(pmId);
				view.setPeriodeFiscale(2016);
				view.setDateDebutPeriodeImposition(date(2015, 7, 1));
				view.setDateFinPeriodeImposition(date(2016, 6, 30));
				view.setDelaiAccorde(RegDate.get().addMonths(2));
				view.setTypeDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH);

				final Errors errors = new BeanPropertyBindingResult(view, "view");
				validator.validate(view, errors);

				// on ne devrait pas y avoir des erreurs
				final List<ObjectError> allErrors = errors.getAllErrors();
				assertNotNull(allErrors);
				assertEquals(0, allErrors.size());
			}
		});
	}

	/**
	 * [SIFISC-21604] date de début vide et autres renseignées -> NPE
	 */
	@Test
	public void testDateDebutDeclarationVideSurDeclarationLibre() throws Exception {
		final RegDate dateDebut = date(2000, 7, 1);
		final RegDate today = RegDate.get();
		final int pf = today.year();

		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto le héros SA");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);          // tous les 31.12
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);
				return entreprise.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// on tente d'ajouter une nouvelle DI qui chevauche la fontière du 30.06 (= fin d'exercice commercial)
				final ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView();
				view.setTiersId(pmId);
				view.setPeriodeFiscale(pf);
				view.setDateDebutPeriodeImposition(null);
				view.setDateFinPeriodeImposition(date(pf, 12, 31));
				view.setDelaiAccorde(date(pf + 1, 12, 31));
				view.setTypeDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH);

				final Errors errors = new BeanPropertyBindingResult(view, "view");
				validator.validate(view, errors);

				final List<ObjectError> allErrors = errors.getAllErrors();
				assertNotNull(allErrors);
				assertEquals(1, allErrors.size());

				final ObjectError error = allErrors.get(0);
				assertEquals("error.date.debut.vide", error.getCode());
			}
		});
	}
}

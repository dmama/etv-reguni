package ch.vd.uniregctb.di.validator;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeclarationImpotEditValidatorTest extends WebTest {
	private DeclarationImpotEditValidator validator;
	private Long idCedi;
	DeclarationImpotOrdinaireDAO diDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		validator = new DeclarationImpotEditValidator();
		validator.setDiDAO(diDAO);

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				idCedi = cedi.getId();
				return null;
			}
		});
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEditDelai() {
		DeclarationImpotDetailView view = new DeclarationImpotDetailView();
		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.delai.accorde.vide", error.getCode());
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
				ModeleDocument declarationComplete2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2010);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2010);

				// Un contribuable quelconque
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
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
		DeclarationImpotDetailView view = new DeclarationImpotDetailView();
		view.setId(ids.declarationId);
		view.setDateRetour(date(2011, 1, 20));
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
				ModeleDocument declarationComplete2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2010);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2010);

				// Un contribuable quelconque
				Contribuable jeanne = addNonHabitant("Jeanne", "dupont", date(1965, 4, 13), Sexe.MASCULIN);
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
		DeclarationImpotDetailView view = new DeclarationImpotDetailView();
		view.setId(ids.declarationId);
		view.setDateRetour(date(2011, 1, 20));
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
				ModeleDocument declarationComplete2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2010);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2010);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2010);

				// Un contribuable quelconque
				Contribuable gustave = addNonHabitant("Gustave", "Eiffel", date(1965, 4, 13), Sexe.MASCULIN);
				ids.gustaveId = gustave.getNumero();
				addForPrincipal(gustave, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(gustave, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2010);
				addEtatDeclarationEmise(declaration2010, date(2011, 1, 24));
				addEtatDeclarationSommee(declaration2010, date(2011, 4, 26), date(2011,4, 28));
				ids.declarationId = declaration2010.getId();

				return null;
			}
		});
		//On tente de la quittancer au 20.01.2011 -> Blocage et message d'erreur
		DeclarationImpotDetailView view = new DeclarationImpotDetailView();
		view.setId(ids.declarationId);
		view.setDateRetour(date(2011, 4, 24));
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
			ModeleDocument declarationComplete2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2010);
			addModeleFeuilleDocument("Déclaration", "210", declarationComplete2010);
			addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2010);
			addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2010);
			addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2010);

			// Un contribuable quelconque
			Contribuable coridon = addNonHabitant("Coridon", "De la Mouette", date(1965, 4, 13), Sexe.MASCULIN);
			ids.coridonId = coridon.getNumero();
			addForPrincipal(coridon, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(coridon, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					declarationComplete2010);
			addEtatDeclarationEmise(declaration2010, date(2011, 1, 24));
			addEtatDeclarationSommee(declaration2010, date(2011, 7, 26), date(2011, 7, 28));
			ids.declarationId = declaration2010.getId();

			return null;
		}
	});
	//On tente de la quittancer au 20.06.2011 -> Blocage et message d'erreur
	DeclarationImpotDetailView view = new DeclarationImpotDetailView();
	view.setId(ids.declarationId);
	view.setDateRetour(date(2011, 4, 26));
	final Errors errors = new BeanPropertyBindingResult(view, "view");
	validator.validate(view, errors);
	final List<ObjectError> allErrors = errors.getAllErrors();
	assertNotNull(allErrors);
	assertEquals(1, allErrors.size());
	final ObjectError error = allErrors.get(0);
	assertEquals("error.date.retour.anterieure.date.emission.sommation", error.getCode());

}


}

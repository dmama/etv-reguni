package ch.vd.uniregctb.fors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.WebTestSpring3;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

public class EditModeImpositionValidatorTest extends WebTestSpring3 {

	private EditModeImpositionValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final AutorisationManager autorisationManager = getBean(AutorisationManager.class, "autorisationManager");
		validator = new EditModeImpositionValidator(hibernateTemplate, autorisationManager);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateChangementModeImpositionSurNonHabitant() throws Exception {

		final PersonnePhysique pp = addNonHabitant("Tartempion", "Bidule", null, Sexe.MASCULIN);
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Chamblon);

		final EditModeImpositionView view = new EditModeImpositionView();
		view.initReadOnlyData(ffp);
		view.setModeImposition(ModeImposition.ORDINAIRE);

		// date vide -> ne devrait pas fonctionner
		{
			view.setDateChangement(null);
			final Errors errors = validate(view);
			Assert.assertNotNull(errors);
			Assert.assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("dateChangement");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.date.changement.vide", error.getCode());
		}

		// date dans le futur -> ne devrait pas fonctionner
		{
			view.setDateChangement(RegDate.get().addDays(1));
			final Errors errors = validate(view);
			Assert.assertNotNull(errors);
			Assert.assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("dateChangement");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.date.changement.posterieure.date.jour", error.getCode());
		}

		// date à aujourd'hui -> devrait fonctionner
		{
			view.setDateChangement(RegDate.get());
			final Errors errors = validate(view);
			Assert.assertNotNull(errors);
			Assert.assertEquals(0, errors.getErrorCount());
		}
	}

	/**
	 * Cas jira SIFISC-3313
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateChangementModeImpositionSurHabitant() throws Exception {

		final long noIndividu = 49841417981L;
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1965, 6, 1), "Tartempion", "Bidule", true);
				addNationalite(individu, MockPays.France, date(1965, 6, 1), null);
			}
		});

		final PersonnePhysique pp = addHabitant(noIndividu);
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Chamblon);

		final EditModeImpositionView view = new EditModeImpositionView();
		view.initReadOnlyData(ffp);
		view.setModeImposition(ModeImposition.ORDINAIRE);

		// date vide -> ne devrait pas fonctionner
		{
			view.setDateChangement(null);
			final Errors errors = validate(view);
			Assert.assertNotNull(errors);
			Assert.assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("dateChangement");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.date.changement.vide", error.getCode());
		}

		// date dans le futur -> ne devrait pas fonctionner
		{
			view.setDateChangement(RegDate.get().addDays(1));
			final Errors errors = validate(view);
			Assert.assertNotNull(errors);
			Assert.assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("dateChangement");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.date.changement.posterieure.date.jour", error.getCode());
		}

		// date à aujourd'hui -> devrait fonctionner
		{
			view.setDateChangement(RegDate.get());
			final Errors errors = validate(view);
			Assert.assertNotNull(errors);
			Assert.assertEquals(0, errors.getErrorCount());
		}
	}

	/**
	 * Méthode de validation, retourne le binding results après passage de la validation
	 *
	 * @param view objet à valider
	 * @return binding results
	 */
	private Errors validate(final EditModeImpositionView view) throws Exception {
		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		return errors;
	}
}

package ch.vd.unireg.fors;

import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AddForSecondaireValidatorTest extends WebTestSpring3 {

	private AddForSecondaireValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		validator = new AddForSecondaireValidator(serviceInfra, hibernateTemplate);
	}


	//SIFISC-25746
	@Test
	public void testAbsenceMultipleSurForSecondaire() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		final long noCtb = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Bidule", "Tartempion", date(1965, 6, 1), Sexe.MASCULIN);

			pp.setNumeroOfsNationalite(MockPays.France.getNoOFS());
			return pp.getNumero();
		});

		final AddForSecondaireView view = new AddForSecondaireView();
		view.setTiersId(noCtb);
		view.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
		view.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);

		// Erreur sur Date d'ouverture,motif d'ouverture et autorité fiscale
		{
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(3, errors.getFieldErrorCount());
		}


	}




	/**
	 * Méthode de validation, retourne le binding results après passage de la validation
	 * @param view objet à valider
	 * @return binding results
	 */
	private Errors validate(final AddForSecondaireView view) throws Exception {
		return doInNewTransactionAndSession(status -> {
			final Errors errors = new BeanPropertyBindingResult(view, "view");
			validator.validate(view, errors);
			return errors;
		});
	}
}

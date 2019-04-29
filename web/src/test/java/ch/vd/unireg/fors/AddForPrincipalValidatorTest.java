package ch.vd.unireg.fors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AddForPrincipalValidatorTest extends WebTestSpring3 {

	private AddForPrincipalValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final AutorisationManager autorisationManager = getBean(AutorisationManager.class, "autorisationManager");
		validator = new AddForPrincipalValidator(serviceInfra, hibernateTemplate, autorisationManager);
	}

	/**
	 * SIFISC-4065
	 */
	@Test
	public void testAbsenceMotifOuvertureForPrincipal() throws Exception {

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

		final AddForPrincipalView view = new AddForPrincipalView();
		view.setTiersId(noCtb);
		view.setModeImposition(ModeImposition.ORDINAIRE);
		view.setMotifRattachement(MotifRattachement.DOMICILE);
		view.setDateDebut(RegDate.get().addMonths(-2));

		// motif d'ouverture absent pour seul for HS -> ok
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			view.setNoAutoriteFiscale(MockPays.France.getNoOFS());
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(0, errors.getFieldErrorCount());
		}

		// motif d'ouverture absent pour seul for HC -> ok
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			view.setNoAutoriteFiscale(MockCommune.Bale.getNoOFS());
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(0, errors.getFieldErrorCount());
		}

		// motif d'ouverture absent pour seul for VD -> nok
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			view.setNoAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("motifDebut");
			assertNotNull(error);
			assertEquals("error.motif.ouverture.vide", error.getCode());
		}

		// maintenant, on ouvre un for
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noCtb);
			assertNotNull(pp);
			addForPrincipal(pp, date(2001, 1, 2), null, MockCommune.Bale);
			return null;
		});

		// motif d'ouverture absent pour for HS qui n'est pas le premier -> nok
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			view.setNoAutoriteFiscale(MockPays.France.getNoOFS());
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("motifDebut");
			assertNotNull(error);
			assertEquals("error.motif.ouverture.vide", error.getCode());
		}

		// motif d'ouverture absent pour for HC qui n'est pas le premier -> nok
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			view.setNoAutoriteFiscale(MockCommune.Bale.getNoOFS());
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("motifDebut");
			assertNotNull(error);
			assertEquals("error.motif.ouverture.vide", error.getCode());
		}

		// motif d'ouverture absent pour for VD qui n'est pas le premier -> nok
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			view.setNoAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("motifDebut");
			assertNotNull(error);
			assertEquals("error.motif.ouverture.vide", error.getCode());
		}

		// motif d'ouverture absent pour for HS qui est le premier -> ok (c'est ici la validation globale sur le tiers qui échouera si le cas se présente...)
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			view.setNoAutoriteFiscale(MockPays.France.getNoOFS());
			view.setDateDebut(date(1990, 1, 1));
			view.setDateFin(date(1990, 12, 31));
			view.setMotifFin(MotifFor.FUSION_COMMUNES);
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(0, errors.getFieldErrorCount());
		}

		// motif d'ouverture absent pour for HC qui est le premier -> ok (c'est ici la validation globale sur le tiers qui échouera si le cas se présente...)
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			view.setNoAutoriteFiscale(MockCommune.Bale.getNoOFS());
			view.setDateDebut(date(1990, 1, 1));
			view.setDateFin(date(1990, 12, 31));
			view.setMotifFin(MotifFor.FUSION_COMMUNES);
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(0, errors.getFieldErrorCount());
		}

		// motif d'ouverture absent pour for VD qui est le premier -> nok
		{
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			view.setNoAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			view.setDateDebut(date(1990, 1, 1));
			view.setDateFin(date(1990, 12, 31));
			view.setMotifFin(MotifFor.FUSION_COMMUNES);
			final Errors errors = validate(view);
			assertNotNull(errors);
			assertEquals(1, errors.getFieldErrorCount());

			final FieldError error = errors.getFieldError("motifDebut");
			assertNotNull(error);
			assertEquals("error.motif.ouverture.vide", error.getCode());
		}
	}

	@Test
	public void testMotifFermetureSiDateFermetureDonnee() throws Exception {

		final long noCtb = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Tartempion", "Bidule", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		final AddForPrincipalView view = new AddForPrincipalView();
		view.setTiersId(noCtb);
		view.setDateDebut(RegDate.get().addMonths(-12));
		view.setMotifDebut(MotifFor.ARRIVEE_HC);
		view.setModeImposition(ModeImposition.ORDINAIRE);
		view.setMotifRattachement(MotifRattachement.DOMICILE);
		view.setDateFin(RegDate.get().addDays(-1));

		// for vaudois
		{
			view.setMotifFin(null);
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			view.setNoAutoriteFiscale(MockCommune.Aigle.getNoOFS());
			final Errors validationManquee = validate(view);
			Assert.assertNotNull(validationManquee);
			Assert.assertEquals(1, validationManquee.getErrorCount());

			final FieldError error = validationManquee.getFieldError("motifFin");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.motif.fermeture.vide", error.getCode());

			view.setMotifFin(MotifFor.FUSION_COMMUNES);
			final Errors validationReussie = validate(view);
			Assert.assertNotNull(validationReussie);
			Assert.assertEquals(0, validationReussie.getErrorCount());
		}

		// for HC
		{
			view.setMotifFin(null);
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			view.setNoAutoriteFiscale(MockCommune.Bern.getNoOFS());
			final Errors validationManquee = validate(view);
			Assert.assertNotNull(validationManquee);
			Assert.assertEquals(1, validationManquee.getErrorCount());

			final FieldError error = validationManquee.getFieldError("motifFin");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.motif.fermeture.vide", error.getCode());

			view.setMotifFin(MotifFor.FUSION_COMMUNES);
			final Errors validationReussie = validate(view);
			Assert.assertNotNull(validationReussie);
			Assert.assertEquals(0, validationReussie.getErrorCount());
		}

		// for HS
		{
			view.setMotifFin(null);
			view.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			view.setNoAutoriteFiscale(MockPays.Albanie.getNoOFS());
			final Errors validationManquee = validate(view);
			Assert.assertNotNull(validationManquee);
			Assert.assertEquals(1, validationManquee.getErrorCount());

			final FieldError error = validationManquee.getFieldError("motifFin");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.motif.fermeture.vide", error.getCode());

			view.setMotifFin(MotifFor.FUSION_COMMUNES);
			final Errors validationReussie = validate(view);
			Assert.assertNotNull(validationReussie);
			Assert.assertEquals(0, validationReussie.getErrorCount());
		}
	}

	/**
	 * Méthode de validation, retourne le binding results après passage de la validation
	 * @param view objet à valider
	 * @return binding results
	 */
	private Errors validate(final AddForPrincipalView view) throws Exception {
		return doInNewTransactionAndSession(status -> {
			final Errors errors = new BeanPropertyBindingResult(view, "view");
			validator.validate(view, errors);
			return errors;
		});
	}
}

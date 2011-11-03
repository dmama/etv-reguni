package ch.vd.uniregctb.tiers.validator;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings({"JavaDoc"})
public class ForFiscalViewValidatorTest extends WebTest {

	private ForFiscalViewValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersService tiersService = getBean(TiersService.class, "tiersService");

		validator = new ForFiscalViewValidator();
		validator.setTiersService(tiersService);
		validator.setInfraService(serviceInfra);
	}

	/**
	 * Méthode de validation, retourne le binding results après passage de la validation
	 * @param view objet à valider
	 * @return binding results
	 */
	private Errors validate(final ForFiscalView view) throws Exception {
		return doInNewTransactionAndSession(new TransactionCallback<Errors>() {
			@Override
			public Errors doInTransaction(TransactionStatus status) {
				final Errors errors = new BeanPropertyBindingResult(view, "view");
				validator.validate(view, errors);
				return errors;
			}
		});
	}

	@Test
	public void testMotifFermetureSiDateFermetureDonnee() throws Exception {

		final long noCtb = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Tartempion", "Bidule", null, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		final ForFiscalView view = new ForFiscalView();
		view.setAnnule(false);
		view.setNumeroCtb(noCtb);
		view.setNatureTiers(NatureTiers.NonHabitant);
		view.setChangementModeImposition(false);
		view.setDateOuverture(RegDate.get().addMonths(-12));
		view.setMotifOuverture(MotifFor.INDETERMINE);
		view.setModeImposition(ModeImposition.ORDINAIRE);
		view.setMotifRattachement(MotifRattachement.DOMICILE);
		view.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		view.setDateFermeture(RegDate.get().addDays(-1));

		// for vaudois
		{
			view.setMotifFermeture(null);
			view.setTypeEtNumeroForFiscal(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aigle.getNoOFSEtendu());
			final Errors validationManquee = validate(view);
			Assert.assertNotNull(validationManquee);
			Assert.assertEquals(1, validationManquee.getErrorCount());

			final FieldError error = validationManquee.getFieldError("motifFermeture");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.motif.fermeture.vide", error.getCode());

			view.setMotifFermeture(MotifFor.DEPART_HC);
			final Errors validationReussie = validate(view);
			Assert.assertNotNull(validationReussie);
			Assert.assertEquals(0, validationReussie.getErrorCount());
		}

		// for HC
		{
			view.setMotifFermeture(null);
			view.setTypeEtNumeroForFiscal(TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Bern.getNoOFSEtendu());
			final Errors validationManquee = validate(view);
			Assert.assertNotNull(validationManquee);
			Assert.assertEquals(1, validationManquee.getErrorCount());

			final FieldError error = validationManquee.getFieldError("motifFermeture");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.motif.fermeture.vide", error.getCode());

			view.setMotifFermeture(MotifFor.ARRIVEE_HC);
			final Errors validationReussie = validate(view);
			Assert.assertNotNull(validationReussie);
			Assert.assertEquals(0, validationReussie.getErrorCount());
		}

		// for HS
		{
			view.setMotifFermeture(null);
			view.setTypeEtNumeroForFiscal(TypeAutoriteFiscale.PAYS_HS, MockPays.Albanie.getNoOFS());
			final Errors validationManquee = validate(view);
			Assert.assertNotNull(validationManquee);
			Assert.assertEquals(1, validationManquee.getErrorCount());

			final FieldError error = validationManquee.getFieldError("motifFermeture");
			Assert.assertNotNull(error);
			Assert.assertEquals("error.motif.fermeture.vide", error.getCode());

			view.setMotifFermeture(MotifFor.ARRIVEE_HS);
			final Errors validationReussie = validate(view);
			Assert.assertNotNull(validationReussie);
			Assert.assertEquals(0, validationReussie.getErrorCount());
		}
	}

	@Test
	public void testDateChangementModeImposition() throws Exception {

		final long noCtb = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Tartempion", "Bidule", null, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		final ForFiscalView view = new ForFiscalView();
		view.setAnnule(false);
		view.setChangementModeImposition(true);
		view.setNumeroCtb(noCtb);
		view.setNatureTiers(NatureTiers.NonHabitant);
		view.setModeImposition(ModeImposition.ORDINAIRE);

		// date vide -> ne devrait pas fonctionner
		{
			view.setDateChangement((RegDate) null);
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
}

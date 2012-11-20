package ch.vd.uniregctb.tiers.validator;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
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
		validator.setSecurityProvider(getBean(SecurityProviderInterface.class, "securityProviderInterface"));
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
	public void testDateChangementModeImpositionSurNonHabitant() throws Exception {

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

	/**
	 * Cas jira SIFISC-3313
	 */
	@Test
	public void testDateChangementModeImpositionSurHabitant() throws Exception {

		final long noIndividu = 49841417981L;
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1965, 6, 1), "Tartempion", "Bidule", true);
				addNationalite(individu, MockPays.France, date(1965, 6, 1), null);
			}
		});

		final long noCtb = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		final ForFiscalView view = new ForFiscalView();
		view.setAnnule(false);
		view.setChangementModeImposition(true);
		view.setNumeroCtb(noCtb);
		view.setNatureTiers(NatureTiers.Habitant);
		view.setModeImposition(ModeImposition.ORDINAIRE);
		view.setTypeEtNumeroForFiscal(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu());
		view.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		view.setMotifRattachement(MotifRattachement.DOMICILE);

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

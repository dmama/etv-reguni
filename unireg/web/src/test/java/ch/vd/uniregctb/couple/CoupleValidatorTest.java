package ch.vd.uniregctb.couple;

import java.util.List;

import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ValidatorHelperImpl;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class CoupleValidatorTest extends WebTest {

	private CoupleValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final SituationFamilleService situFamilleService = getBean(SituationFamilleService.class, "situationFamilleService");
		final MetierService metierService = getBean(MetierService.class, "metierService");
		final MessageSource messageSource = getBean(MessageSource.class, "messageSource");

		final CoupleManagerImpl coupleManager = new CoupleManagerImpl();
		coupleManager.setTiersService(tiersService);
		coupleManager.setTiersDAO(tiersDAO);
		coupleManager.setMetierService(metierService);

		final ValidatorHelperImpl validatorHelper = new ValidatorHelperImpl();
		validatorHelper.setSituationFamilleService(situFamilleService);
		validatorHelper.setTiersService(tiersService);
		validatorHelper.setMessageSource(messageSource);

		validator = new CoupleValidator();
		validator.setTiersDAO(tiersDAO);
		validator.setCoupleManager(coupleManager);
		validator.setMetierService(metierService);
		validator.setValidatorHelper(validatorHelper);
		validator.setTiersService(tiersService);
	}

	/**
	 * [UNIREG-1595] on doit pouvoir marier un non-habitant connu au civil même s'il est séparé là-bas
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMariageNonHabitantSepareAuCivil() throws Exception {

		final long noIndividu = 123542L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 5, 7), "Petipoint", "Justin", true);
				addEtatCivil(individu, date(1970, 5, 7), TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(individu, date(1990, 10, 1), TypeEtatCivil.MARIE);
				addEtatCivil(individu, date(2000, 2, 1), TypeEtatCivil.SEPARE);
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				tiersService.changeHabitantenNH(pp);
				addForPrincipal(pp, date(2001, 5, 1), MotifFor.DEPART_HS, MockPays.Allemagne);
				return pp.getNumero();
			}
		});

		// création du couple
		final CoupleView view = new CoupleView();
		view.setDateDebut(date(2009, 6, 8));
		view.setPp1Id(ppId);
		view.setMarieSeul(true);

		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		assertEquals(0, errors.getErrorCount());
	}

	/**
	 * [UNIREG-1595] on ne doit pas pouvoir marier un non-habitant qui a une surcharge fiscale de la situation
	 * de famille incompatible avec un mariage
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMariageNonHabitantSituationFamilleIncompatible() throws Exception {

		final long noIndividu = 123542L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 5, 7), "Petipoint", "Justin", true);
				addEtatCivil(individu, date(1970, 5, 7), TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(individu, date(1990, 10, 1), TypeEtatCivil.MARIE);
				addEtatCivil(individu, date(2000, 2, 1), TypeEtatCivil.SEPARE);
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				tiersService.changeHabitantenNH(pp);
				addForPrincipal(pp, date(2001, 5, 1), MotifFor.DEPART_HS, MockPays.Allemagne);

				final SituationFamille situationFamille = addSituation(pp, date(2001, 5, 1), null, 0);
				situationFamille.setEtatCivil(EtatCivil.SEPARE);

				return pp.getNumero();
			}
		});

		// création du couple
		final CoupleView view = new CoupleView();
		view.setDateDebut(date(2009, 6, 8));
		view.setPp1Id(ppId);
		view.setMarieSeul(true);

		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		assertEquals(1, errors.getErrorCount());

		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals(String.format("Le contribuable n° %s ne peut pas se marier fiscalement car il(elle) est séparé(e) au fiscal", FormatNumeroHelper.numeroCTBToDisplay(ppId)), error.getDefaultMessage());
	}

	/**
	 * [SIFISC-504] Le validator ne doit pas crasher (NPE) lorsque le numéro de troisième tiers n'existe pas.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValideTiers3Inexistant() throws Exception {

		class Ids {
			long tiers1;
			long tiers2;
			long tiersInexistant = 12345432L;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique tiers1 = addNonHabitant("Alfred", "Dutuyau", date(1977, 3, 3), Sexe.MASCULIN);
				final PersonnePhysique tiers2 = addNonHabitant("Georgette", "Dutuyau", date(1977, 3, 3), Sexe.FEMININ);
				ids.tiers1 = tiers1.getId();
				ids.tiers2 = tiers2.getId();
				return null;
			}
		});

		// création du couple
		final CoupleView view = new CoupleView();
		view.setNouveauMC(false);
		view.setDateDebut(date(2009, 6, 8));
		view.setPp1Id(ids.tiers1);
		view.setPp2Id(ids.tiers2);
		view.setMcId(ids.tiersInexistant);

		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		assertEquals(1, errors.getErrorCount());

		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.tiers.inexistant", error.getCode());
	}

	/**
	 * [SIFISC-1142] Vérifie que le validator détecte lorsque le numéro de tiers est plus grand le max autorisé.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValideTiers3NumeroTropGrand() throws Exception {

		class Ids {
			long tiers1;
			long tiers2;
			long tiersInexistant = 123454321L;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique tiers1 = addNonHabitant("Alfred", "Dutuyau", date(1977, 3, 3), Sexe.MASCULIN);
				final PersonnePhysique tiers2 = addNonHabitant("Georgette", "Dutuyau", date(1977, 3, 3), Sexe.FEMININ);
				ids.tiers1 = tiers1.getId();
				ids.tiers2 = tiers2.getId();
				return null;
			}
		});

		// création du couple
		final CoupleView view = new CoupleView();
		view.setNouveauMC(false);
		view.setDateDebut(date(2009, 6, 8));
		view.setPp1Id(ids.tiers1);
		view.setPp2Id(ids.tiers2);
		view.setMcId(ids.tiersInexistant);

		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		assertEquals(1, errors.getErrorCount());

		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.numero.tiers.trop.grand", error.getCode());
	}

}

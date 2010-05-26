package ch.vd.uniregctb.couple.validator;

import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.couple.CoupleHelper;
import ch.vd.uniregctb.couple.manager.CoupleRecapManager;
import ch.vd.uniregctb.couple.manager.CoupleRecapManagerImpl;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.couple.view.TypeUnion;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.MotifFor;

public class CoupleRecapValidatorTest extends BusinessTest {

	private CoupleRecapValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final SituationFamilleService situFamilleService = getBean(SituationFamilleService.class, "situationFamilleService");
		final MetierService metierService = getBean(MetierService.class, "metierService");

		final CoupleHelper coupleHelper = new CoupleHelper();
		coupleHelper.setTiersService(tiersService);

		final CoupleRecapManager recapManager = new CoupleRecapManagerImpl();

		validator = new CoupleRecapValidator();
		validator.setCoupleHelper(coupleHelper);
		validator.setCoupleRecapManager(recapManager);
		validator.setMetierService(metierService);
		validator.setSituationFamilleService(situFamilleService);
		validator.setTiersService(tiersService);
	}

	/**
	 * [UNIREG-1595] on doit pouvoir marier un non-habitant connu au civil même s'il est séparé là-bas
	 */
	@Test
	public void testMariageNonHabitantSepareAuCivil() throws Exception {

		final long noIndividu = 123542L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 5, 7), "Petipoint", "Justin", true);
				addEtatCivil(individu, date(1970, 5, 7), EnumTypeEtatCivil.CELIBATAIRE);
				addEtatCivil(individu, date(1990, 10, 1), EnumTypeEtatCivil.MARIE);
				addEtatCivil(individu, date(2000, 2, 1), EnumTypeEtatCivil.SEPARE);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				tiersService.changeHabitantenNH(pp);
				addForPrincipal(pp, date(2001, 5, 1), MotifFor.DEPART_HS, MockPays.Allemagne);
				return pp.getNumero();
			}
		});

		// création du couple
		final CoupleRecapView view = new CoupleRecapView();
		view.setDateDebut(date(2009, 6, 8).asJavaDate());
		view.setPremierePersonne(new TiersGeneralView(ppId));
		view.setTypeUnion(TypeUnion.COUPLE);

		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		Assert.assertEquals(0, errors.getErrorCount());
	}

	/**
	 * [UNIREG-1595] on ne doit pas pouvoir marier un non-habitant qui a une surcharge fiscale de la situation
	 * de famille incompatible avec un mariage
	 */
	@Test
	public void testMariageNonHabitantSituationFamilleIncompatible() throws Exception {

		final long noIndividu = 123542L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 5, 7), "Petipoint", "Justin", true);
				addEtatCivil(individu, date(1970, 5, 7), EnumTypeEtatCivil.CELIBATAIRE);
				addEtatCivil(individu, date(1990, 10, 1), EnumTypeEtatCivil.MARIE);
				addEtatCivil(individu, date(2000, 2, 1), EnumTypeEtatCivil.SEPARE);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				tiersService.changeHabitantenNH(pp);
				addForPrincipal(pp, date(2001, 5, 1), MotifFor.DEPART_HS, MockPays.Allemagne);

				final SituationFamille situationFamille = addSituation(pp, date(2001, 5, 1), null, 0);
				situationFamille.setEtatCivil(EtatCivil.SEPARE);

				return pp.getNumero();
			}
		});

		// création du couple
		final CoupleRecapView view = new CoupleRecapView();
		view.setDateDebut(date(2009, 6, 8).asJavaDate());
		view.setPremierePersonne(new TiersGeneralView(ppId));
		view.setTypeUnion(TypeUnion.COUPLE);

		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		Assert.assertEquals(1, errors.getErrorCount());

		final List<?> fieldErrors = errors.getFieldErrors();
		Assert.assertNotNull(fieldErrors);
		Assert.assertEquals(1, fieldErrors.size());

		final FieldError error = (FieldError) fieldErrors.get(0);
		Assert.assertEquals("error.impossible.marier.contribuable", error.getCode());
	}

}

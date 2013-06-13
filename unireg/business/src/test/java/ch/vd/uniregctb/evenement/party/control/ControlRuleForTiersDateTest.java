package ch.vd.uniregctb.evenement.party.control;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

public class ControlRuleForTiersDateTest extends AbstractControlTaxliabilityTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}


	@Test
	public void testCheckForFiscalVaudois() throws Exception {
		final long noInd = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1956, 3, 12), "Ruppert", "Jerome", Sexe.MASCULIN);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, MockCommune.Moudon);
				return pp.getNumero();
			}
		});

		final RegDate dateControle = RegDate.get(2010,12,3);
		final ControlRuleForTiersDate controlRuleForTiersDate = new ControlRuleForTiersDate(context,idPP,dateControle);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForTiersDate.check();
			}
		});

		assertTiersAssujetti(idPP, result);
	}

	@Test
	public void testCheckForFiscalAbsent() throws Exception {
		final long noInd = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1956, 3, 12), "Ruppert", "Jerome", Sexe.MASCULIN);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		final RegDate dateControle = RegDate.get(2010,12,3);
		final ControlRuleForTiersDate controlRuleForTiersDate = new ControlRuleForTiersDate(context,idPP,dateControle);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForTiersDate.check();
			}
		});

		assertControlNumeroKO(result);

	}

	@Test
	public void testCheckForFiscalHorsCanton() throws Exception {
		final long noInd = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1956, 3, 12), "Ruppert", "Jerome", Sexe.MASCULIN);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, MockCommune.Zurich);
				return pp.getNumero();
			}
		});

		final RegDate dateControle = RegDate.get(2010,12,3);
		final ControlRuleForTiersDate controlRuleForTiersDate = new ControlRuleForTiersDate(context,idPP,dateControle);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForTiersDate.check();
			}
		});

		assertControlNumeroKO(result);
	}




}

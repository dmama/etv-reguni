package ch.vd.uniregctb.evenement.party.control;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

public class ControlRuleForTiersPeriodeTest extends AbstractControlTaxliabilityTest {

	@Test
	public void testCheckAssujetissementStandard() throws Exception {
		final long noInd = 1244;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1983, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2001, 3, 12), MotifFor.MAJORITE, MockCommune.Moudon);
				return pp.getNumero();
			}
		});

		final Integer periode = 2012;
		final ControlRuleForTiersPeriode controlRuleForTiersPeriode = new ControlRuleForTiersPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlRuleForTiersPeriode.check(pp);
			}
		});
		assertTiersAssujetti(idPP, result);
	}

	@Test
	public void testCheckAssujetissementUnJour() throws Exception {
		final long noInd = 1244;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2012, 12, 12), MotifFor.MAJORITE,date(2012,12,12),MotifFor.DEPART_HS, MockCommune.Moudon);
				return pp.getNumero();
			}
		});

		final Integer periode = 2012;
		final ControlRuleForTiersPeriode controlRuleForTiersPeriode = new ControlRuleForTiersPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlRuleForTiersPeriode.check(pp);
			}
		});

		assertTiersAssujetti(idPP, result);
	}

	@Test
	public void testCheckPasAssujetti() throws Exception {
		final long noInd = 1244;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2011, 1, 12), MotifFor.MAJORITE,date(2011,12,12),MotifFor.DEPART_HS, MockCommune.Moudon);
				return pp.getNumero();
			}
		});

		final Integer periode = 2012;
		final ControlRuleForTiersPeriode controlRuleForTiersPeriode = new ControlRuleForTiersPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlRuleForTiersPeriode.check(pp);
			}
		});

		assertControlNumeroKO(result);
	}

}

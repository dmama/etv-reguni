package ch.vd.unireg.evenement.party.control;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ControlRuleForMenageDateTest extends AbstractControlTaxliabilityTest {
	@Test
	public void testCheckTiersNonAssujettiSansMenageCommun() throws Exception {
		final long noInd = 1244;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertDate", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(status -> {
			final PersonnePhysique pp = addHabitant(noInd);
			return pp.getNumero();
		});

		final RegDate dateControle = RegDate.get(2010,12,3);
		final ControlRuleForMenageDate controlRuleForMenageDate = new ControlRuleForMenageDate(dateControle, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
			return controlRuleForMenageDate.check(pp, null);
		});

		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE, result.getEchec().getType());

	}

	@Test
	public void testCheckTiersWithMenageCommunNonAssujetti() throws Exception {
		final long noInd = 1244;
		class Ids {
			Long idpp;
			Long idMc;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertDate", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique pp = addHabitant(noInd);
			ids.idpp = pp.getId();

			EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(2000, 5, 5), null);
			final MenageCommun menage = ensemble.getMenage();
			ids.idMc = menage.getId();

			addForPrincipal(menage, date(2000, 5, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                date(2012, 3, 5), MotifFor.DEPART_HS, MockCommune.Moudon);
			return null;
		});

		final RegDate dateControle = RegDate.get(2012,12,3);

		final ControlRuleForMenageDate controlRuleForMenageDate = new ControlRuleForMenageDate(dateControle, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
			return controlRuleForMenageDate.check(pp, null);
		});

		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES, result.getEchec().getType());
		assertEquals(ids.idMc, result.getEchec().getMenageCommunIds().get(0));
	}


	@Test
	public void testCheckTiersWithUnMenageCommunAssujetti() throws Exception {
		final long noInd = 1244;
		class Ids {
			Long idpp;
			Long idMc;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique pp = addHabitant(noInd);
			ids.idpp = pp.getId();

			EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(2000, 5, 5), null);
			final MenageCommun menage = ensemble.getMenage();
			ids.idMc = menage.getId();

			addForPrincipal(menage, date(2000, 5, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);
			return null;
		});

		final RegDate dateControle = RegDate.get(2012,12,3);

		final ControlRuleForMenageDate controlRuleForMenageDate = new ControlRuleForMenageDate(dateControle, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
			return controlRuleForMenageDate.check(pp, null);
		});

		assertTiersAssujetti(ids.idMc, result);
	}
}

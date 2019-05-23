package ch.vd.unireg.evenement.party.control;

import org.junit.Test;

import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ControlRuleForMenagePeriodeTest extends AbstractControlTaxliabilityTest {

	@Test
	public void testCheckTiersNonAssujettiSansMenageCommun() throws Exception {
		final long noInd = 1244;

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(status -> {
			final PersonnePhysique pp = addHabitant(noInd);
			return pp.getNumero();
		});

		final Integer periode = 2012;
		final ControlRuleForMenagePeriode controlRuleForMenagePeriode = new ControlRuleForMenagePeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
			return controlRuleForMenagePeriode.check(pp, null);
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

		serviceCivil.setUp(new MockIndividuConnector() {
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
			ids.idMc = ensemble.getMenage().getId();
			return null;
		});

		final Integer periode = 2012;
		final ControlRuleForMenagePeriode controlRuleForMenagePeriode = new ControlRuleForMenagePeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
			return controlRuleForMenagePeriode.check(pp, null);
		});

		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES, result.getEchec().getType());
		assertEquals(ids.idMc, result.getEchec().getMenageCommunIds().get(0));

	}

	@Test
	public void testCheckTiersWithPlusieursMenageCommunNonAssujetti() throws Exception {
		final long noInd = 1244;
		class Ids {
			Long idpp;
			Long idMc1;
			Long idMc2;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique pp = addHabitant(noInd);
			ids.idpp = pp.getId();

			EnsembleTiersCouple ensemble1 = addEnsembleTiersCouple(pp, null, date(2000, 5, 5), date(2012, 3, 5));
			ids.idMc1 = ensemble1.getMenage().getId();

			EnsembleTiersCouple ensemble2 = addEnsembleTiersCouple(pp, null, date(2012, 6, 5), null);
			ids.idMc2 = ensemble2.getMenage().getId();
			return null;
		});

		final Integer periode = 2012;
		final ControlRuleForMenagePeriode controlRuleForMenagePeriode = new ControlRuleForMenagePeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
			return controlRuleForMenagePeriode.check(pp, null);
		});

		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES, result.getEchec().getType());
		assertEquals(2, result.getEchec().getMenageCommunIds().size());

	}

	@Test
	public void testCheckTiersWithPlusieursMenageCommunAssujetti() throws Exception {
		final long noInd = 1244;
		class Ids {
			Long idpp;
			Long idMc1;
			Long idMc2;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique pp = addHabitant(noInd);
			ids.idpp = pp.getId();

			EnsembleTiersCouple ensemble1 = addEnsembleTiersCouple(pp, null, date(2000, 5, 5), date(2012, 3, 5));
			final MenageCommun menage1 = ensemble1.getMenage();
			addForPrincipal(menage1, date(2012, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                date(2012, 3, 5), MotifFor.VEUVAGE_DECES, MockCommune.Moudon);
			ids.idMc1 = menage1.getId();

			EnsembleTiersCouple ensemble2 = addEnsembleTiersCouple(pp, null, date(2012, 6, 5), null);
			final MenageCommun menage2 = ensemble2.getMenage();
			addForPrincipal(menage2, date(2012, 6, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);
			ids.idMc2 = menage2.getId();
			return null;
		});

		final Integer periode = 2012;
		final ControlRuleForMenagePeriode controlRuleForMenagePeriode = new ControlRuleForMenagePeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
			return controlRuleForMenagePeriode.check(pp, null);
		});

		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.PLUSIEURS_MC_ASSUJETTI_TROUVES, result.getEchec().getType());
		assertEquals(2,result.getEchec().getMenageCommunIds().size());

	}

	@Test
	public void testCheckTiersWithUnMenageCommunPlusieursAssujettissements() throws Exception {
		final long noInd = 1244;
		class Ids {
			Long idpp;
			Long idMc;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockIndividuConnector() {
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
			addForPrincipal(menage, date(2012, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                date(2012, 3, 5), MotifFor.DEPART_HS, MockCommune.Moudon);

			addForPrincipal(menage, date(2012, 6, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);
			return null;
		});

		final Integer periode = 2012;
		final ControlRuleForMenagePeriode controlRuleForMenagePeriode = new ControlRuleForMenagePeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
			return controlRuleForMenagePeriode.check(pp, null);
		});

		assertTiersAssujetti(ids.idMc, result);

	}


	@Test
	public void testCheckTiersWithUnMenageCommunAssujetti() throws Exception {
		final long noInd = 1244;
		class Ids {
			Long idpp;
			Long idMc;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockIndividuConnector() {
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

		final Integer periode = 2012;
		final ControlRuleForMenagePeriode controlRuleForMenagePeriode = new ControlRuleForMenagePeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
			return controlRuleForMenagePeriode.check(pp, null);
		});

		assertTiersAssujetti(ids.idMc, result);

	}


}

package ch.vd.unireg.evenement.party.control;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;

public class ControlRuleForParentPeriodeTest extends AbstractControlTaxliabilityTest {

	@Test
	public void testCheckTiersWithAucunParent() throws Exception {
		final long noInd = 1244;


		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(2005, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(status -> {
			final PersonnePhysique pp = addHabitant(noInd);
			return pp.getNumero();
		});

		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertPasDeParent(result);

	}


	@Test
	public void testCheckTiersWithParentNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(fille, parent, null, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppParent = addHabitant(noIndParent);
			ids.idFille = ppFille.getId();
			ids.idPere = ppParent.getId();
			addParente(ppFille, ppParent, dateNaissance, null);
			return null;
		});

		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertUnParentNonAssujetti(ids.idPere, result);

	}


	@Test
	public void testCheckTiersWithParentAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(fille, parent, null, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppParent = addHabitant(noIndParent);
			ids.idFille = ppFille.getId();
			ids.idPere = ppParent.getId();
			addForPrincipal(ppParent, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);
			addParente(ppFille, ppParent, dateNaissance, null);
			return null;
		});

		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertTiersAssujetti(ids.idPere, result);

	}


	@Test
	public void testCheckTiersWithParentWithMenageCommunNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
			Long idMenagePere;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(fille, parent, null, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppPere = addHabitant(noIndParent);
			ids.idFille = ppFille.getId();
			ids.idPere = ppPere.getId();
			addParente(ppFille, ppPere, dateNaissance, null);

			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ppPere, null, date(2000, 5, 5), null);
			final MenageCommun menage = ensemble.getMenage();
			ids.idMenagePere = menage.getId();
			return null;
		});

		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertUnParentWithMCNonAssujetti(ids.idPere, ids.idMenagePere, result);

	}


	@Test
	public void testCheckTiersWithParentWithMenageCommunAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
			Long idMenagePere;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(fille, parent, null, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppPere = addHabitant(noIndParent);
			ids.idFille = ppFille.getId();
			ids.idPere = ppPere.getId();
			addParente(ppFille, ppPere, dateNaissance, null);

			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ppPere, null, date(2000, 1, 5), null);
			final MenageCommun menage = ensemble.getMenage();
			ids.idMenagePere = menage.getId();
			addForPrincipal(menage, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);
			return null;
		});

		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertTiersAssujetti(ids.idMenagePere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;

		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertPeriode", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(fille, pere, mere, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppPere = addHabitant(noIndPere);
			final PersonnePhysique ppMere = addHabitant(noIndMere);
			addParente(ppFille, ppPere, dateNaissance, null);
			addParente(ppFille, ppMere, dateNaissance, null);

			ids.idFille = ppFille.getId();
			ids.idPere = ppPere.getId();
			ids.idMere = ppMere.getId();
			return null;
		});
		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertDeuxParentsNonAssujettis(ids.idPere, ids.idMere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentUnMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenagePere;

		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertPeriode", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(fille, pere, mere, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppPere = addHabitant(noIndPere);
			final PersonnePhysique ppMere = addHabitant(noIndMere);
			addParente(ppFille, ppPere, dateNaissance, null);
			addParente(ppFille, ppMere, dateNaissance, null);

			ids.idFille = ppFille.getId();
			ids.idPere = ppPere.getId();
			ids.idMere = ppMere.getId();
			final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere, null, date(2006, 7, 8), null);
			final MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();
			ids.idMenagePere = menageCommunPere.getId();
			return null;
		});
		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertDeuxParentUnMCNonAssujetti(ids.idPere, ids.idMere, ids.idMenagePere, result);

	}

	@Test
	public void testCheckTiersWithDeuxParentDeuxMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenagePere;
			Long idMenageMere;

		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertPeriode", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(fille, pere, mere, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppPere = addHabitant(noIndPere);
			final PersonnePhysique ppMere = addHabitant(noIndMere);
			addParente(ppFille, ppPere, dateNaissance, null);
			addParente(ppFille, ppMere, dateNaissance, null);
			ids.idFille = ppFille.getId();
			ids.idPere = ppPere.getId();
			ids.idMere = ppMere.getId();

			final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere, null, date(2006, 7, 8), null);
			final MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();
			ids.idMenagePere = menageCommunPere.getId();

			final EnsembleTiersCouple ensembleTiersCoupleMere = addEnsembleTiersCouple(ppMere, null, date(2006, 7, 8), null);
			final MenageCommun menageCommunMere = ensembleTiersCoupleMere.getMenage();
			ids.idMenageMere = menageCommunMere.getId();
			return null;
		});
		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertDeuxParentsDeuxMCNonAssujetti(ids.idPere, ids.idMere, ids.idMenagePere, ids.idMenageMere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentDeuxMCDifferentsAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenagePere;
			Long idMenageMere;

		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertPeriode", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(fille, pere, mere, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppPere = addHabitant(noIndPere);
			final PersonnePhysique ppMere = addHabitant(noIndMere);
			addParente(ppFille, ppPere, dateNaissance, null);
			addParente(ppFille, ppMere, dateNaissance, null);
			ids.idFille = ppFille.getId();
			ids.idPere = ppPere.getId();
			ids.idMere = ppMere.getId();

			final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere, null, date(2010, 1, 5), null);
			final MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();
			ids.idMenagePere = menageCommunPere.getId();
			addForPrincipal(menageCommunPere, date(2010, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);

			final EnsembleTiersCouple ensembleTiersCoupleMere = addEnsembleTiersCouple(ppMere, null, date(2009, 1, 5), null);
			final MenageCommun menageCommunMere = ensembleTiersCoupleMere.getMenage();
			addForPrincipal(menageCommunMere, date(2009, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			ids.idMenageMere = menageCommunMere.getId();
			return null;
		});
		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertDeuxPArentsWithDeuxMenagesAssujetti(ids.idPere, ids.idMere, ids.idMenagePere, ids.idMenageMere, result);
	}


	@Test
	public void testCheckTiersWithDeuxParentMemeMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenage;

		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertPeriode", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(fille, pere, mere, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppPere = addHabitant(noIndPere);
			final PersonnePhysique ppMere = addHabitant(noIndMere);
			addParente(ppFille, ppMere, dateNaissance, null);
			addParente(ppFille, ppPere, dateNaissance, null);
			ids.idFille = ppFille.getId();
			ids.idPere = ppPere.getId();
			ids.idMere = ppMere.getId();

			final EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(ppPere, ppMere, date(2006, 7, 8), null);
			final MenageCommun menageCommun = ensembleTiersCouple.getMenage();
			ids.idMenage = menageCommun.getId();
			addForPrincipal(menageCommun, date(2006, 7, 8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2012, 2, 1), MotifFor.DEPART_HC, MockCommune.Moudon);
			return null;
		});
		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertDeuxParentsWithUnMCNonAssujetti(ids.idPere, ids.idMere, ids.idMenage, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentMemeMCAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);
		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenage;

		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertPeriode", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(fille, pere, mere, dateNaissance, null);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique ppFille = addHabitant(noIndFille);
			final PersonnePhysique ppPere = addHabitant(noIndPere);
			final PersonnePhysique ppMere = addHabitant(noIndMere);
			addParente(ppFille, ppPere, dateNaissance, null);
			addParente(ppFille, ppMere, dateNaissance, null);
			ids.idFille = ppFille.getId();
			ids.idPere = ppPere.getId();
			ids.idMere = ppMere.getId();

			final EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(ppPere, ppMere, date(2006, 7, 8), null);
			final MenageCommun menageCommun = ensembleTiersCouple.getMenage();
			ids.idMenage = menageCommun.getId();
			addForPrincipal(menageCommun, date(2006, 7, 8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);
			return null;
		});
		final Integer periode = 2012;
		final ControlRuleForParentPeriode controlRuleForParentPeriode = new ControlRuleForParentPeriode(periode, tiersService, assujettissementService);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
			return controlRuleForParentPeriode.check(pp, null);
		});

		assertTiersAssujetti(ids.idMenage, result);
	}
}

package ch.vd.uniregctb.evenement.party.control;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

public class ControlRuleForParentDateTest extends AbstractControlTaxliabilityTest {

	@Test
	public void testCheckTiersWithAucunParent() throws Exception {
		final long noInd = 1244;


		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(2005, 3, 12), "RuppertDate", "Jeroma", Sexe.FEMININ);
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

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertPasDeParent(result);

	}

	@Test
	public void testCheckTiersWithParentNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				addLienVersParent(fille, parent, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
		}

		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppParent = addHabitant(noIndParent);
				addParente(ppFille, ppParent, dateNaissance, null);
				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppParent.getId();
				return ids;
			}
		});

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertUnParentNonAssujetti(ids.idPere, result);
	}

	@Test
	public void testCheckTiersWithParentAsujetti() throws Exception {

		final long noIndFille = 1244;
		final long noIndParent = 1245;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				addLienVersParent(fille, parent, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
		}

		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppParent = addHabitant(noIndParent);
				addParente(ppFille, ppParent, dateNaissance, null);
				addForPrincipal(ppParent, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppParent.getId();
				return ids;
			}
		});

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertTiersAssujetti(ids.idPere, result);
	}

	@Test
	public void testCheckTiersWithParentWithMenageCommunNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				addLienVersParent(fille, parent, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
			Long idMenagePere;
		}

		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndParent);
				addParente(ppFille, ppPere, dateNaissance, null);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ppPere, null, date(2000, 5, 5), null);
				final MenageCommun menage = ensemble.getMenage();

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMenagePere = menage.getId();
				return ids;
			}
		});

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertUnParentWithMCNonAssujetti(ids.idPere,ids.idMenagePere, result);
	}

	@Test
	public void testCheckTiersWithParentWithMenageCommunAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				addLienVersParent(fille, parent, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
			Long idMenagePere;
		}

		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndParent);
				addParente(ppFille, ppPere, dateNaissance, null);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ppPere, null, date(2000, 1, 5), null);
				final MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMenagePere = menage.getId();
				return ids;
			}
		});

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertTiersAssujetti(ids.idMenagePere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLienVersParent(fille, pere, dateNaissance, null);
				addLienVersParent(fille, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;

		}
		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				addParente(ppFille, ppPere, dateNaissance, null);
				addParente(ppFille, ppMere, dateNaissance, null);

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				return ids;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertDeuxParentsNonAssujettis(ids.idPere,ids.idMere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentUnMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLienVersParent(fille, pere, dateNaissance, null);
				addLienVersParent(fille, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenagePere;

		}
		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				addParente(ppFille, ppPere, dateNaissance, null);
				addParente(ppFille, ppMere, dateNaissance, null);

				final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere, null, date(2006, 7, 8), null);
				final MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				ids.idMenagePere = menageCommunPere.getId();
				return ids;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertDeuxParentUnMCNonAssujetti(ids.idPere,ids.idMere,ids.idMenagePere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentDeuxMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLienVersParent(fille, pere, dateNaissance, null);
				addLienVersParent(fille, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenagePere;
			Long idMenageMere;

		}

		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				addParente(ppFille, ppPere, dateNaissance, null);
				addParente(ppFille, ppMere, dateNaissance, null);

				final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere, null, date(2006, 7, 8), null);
				final MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();
				final EnsembleTiersCouple ensembleTiersCoupleMere = addEnsembleTiersCouple(ppMere, null, date(2006, 7, 8), null);
				final MenageCommun menageCommunMere = ensembleTiersCoupleMere.getMenage();

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				ids.idMenagePere = menageCommunPere.getId();
				ids.idMenageMere = menageCommunMere.getId();
				return ids;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertDeuxParentsDeuxMCNonAssujetti(ids.idPere,ids.idMere,ids.idMenagePere,ids.idMenageMere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentDeuxMCDifferentsAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLienVersParent(fille, pere, dateNaissance, null);
				addLienVersParent(fille, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenagePere;
			Long idMenageMere;
		}

		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				addParente(ppFille, ppPere, dateNaissance, null);
				addParente(ppFille, ppMere, dateNaissance, null);

				final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere,null,date(2010, 1, 5),null);
				final MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();
				addForPrincipal(menageCommunPere, date(2010, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);

				final EnsembleTiersCouple ensembleTiersCoupleMere = addEnsembleTiersCouple(ppMere,null,date(2009, 1, 5),null);
				final MenageCommun menageCommunMere = ensembleTiersCoupleMere.getMenage();
				addForPrincipal(menageCommunMere, date(2009, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				ids.idMenagePere = menageCommunPere.getId();
				ids.idMenageMere = menageCommunMere.getId();
				return ids;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertDeuxPArentsWithDeuxMenagesAssujetti(ids.idPere, ids.idMere, ids.idMenagePere, ids.idMenageMere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentMemeMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLienVersParent(fille, pere, dateNaissance, null);
				addLienVersParent(fille, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenage;

		}

		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				addParente(ppFille, ppPere, dateNaissance, null);
				addParente(ppFille, ppMere, dateNaissance, null);

				final EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(ppPere,ppMere,date(2006,7,8),null);
				final MenageCommun menageCommun = ensembleTiersCouple.getMenage();
				addForPrincipal(menageCommun, date(2006, 7,8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2012,2,1),MotifFor.DEPART_HC,MockCommune.Moudon);

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				ids.idMenage = menageCommun.getId();
				return ids;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertDeuxParentsWithUnMCNonAssujetti(ids.idPere,ids.idMere,ids.idMenage, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentMemeMCAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLienVersParent(fille, pere, dateNaissance, null);
				addLienVersParent(fille, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;
			Long idMenage;

		}
		// on crée un habitant vaudois ordinaire
		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				addParente(ppFille, ppPere, dateNaissance, null);
				addParente(ppFille, ppMere, dateNaissance, null);

				final EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(ppPere,ppMere,date(2006,7,8),null);
				final MenageCommun menageCommun = ensembleTiersCouple.getMenage();
				addForPrincipal(menageCommun, date(2006, 7,8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);

				final Ids ids = new Ids();
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				ids.idMenage = menageCommun.getId();
				return ids;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(dateDemande, tiersService);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlRuleForParentDate.check(pp, null);
			}
		});

		assertTiersAssujetti(ids.idMenage, result);
	}
}

package ch.vd.uniregctb.evenement.party.control;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
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
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, idPP, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertPasDeParent(result);

	}




	@Test
	public void testCheckTiersWithParentNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		class Ids {
			Long idFille;
			Long idPere;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(parent, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppParent = addHabitant(noIndParent);
				ids.idFille = ppFille.getId();
				ids.idPere = ppParent.getId();

				return null;
			}
		});

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertUnParentNonAssujetti(ids.idPere, result);

	}




	@Test
	public void testCheckTiersWithParentAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		class Ids {
			Long idFille;
			Long idPere;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(parent, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppParent = addHabitant(noIndParent);
				ids.idFille = ppFille.getId();
				ids.idPere = ppParent.getId();
				addForPrincipal(ppParent, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);

				return null;
			}
		});

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertTiersAssujetti(ids.idPere, result);

	}



	@Test
	public void testCheckTiersWithParentWithMenageCommunNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		class Ids {
			Long idFille;
			Long idPere;
			Long idMenagePere;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(parent, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndParent);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ppPere, null, date(2000, 5, 5), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.idMenagePere = menage.getId();

				return null;
			}
		});

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertUnParentWithMCNonAssujetti(ids.idPere,ids.idMenagePere, result);

	}




	@Test
	public void testCheckTiersWithParentWithMenageCommunAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndParent = 1245;
		class Ids {
			Long idFille;
			Long idPere;
			Long idMenagePere;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(parent, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndParent);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ppPere, null, date(2000, 1, 5), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.idMenagePere = menage.getId();
				addForPrincipal(menage, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);

				return null;
			}
		});

		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertTiersAssujetti(ids.idMenagePere, result);

	}



	@Test
	public void testCheckTiersWithDeuxParentNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
		class Ids {
			Long idFille;
			Long idPere;
			Long idMere;

		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(pere, fille, dateNaissance, null);
				addLiensFiliation(mere, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				return null;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertDeuxParentsNonAssujettis(ids.idPere,ids.idMere, result);

	}


	@Test
	public void testCheckTiersWithDeuxParentUnMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
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
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(pere, fille, dateNaissance, null);
				addLiensFiliation(mere, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere,null,date(2006,7,8),null);
				MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();
				ids.idMenagePere = menageCommunPere.getId();
				return null;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertDeuxParentUnMCNonAssujetti(ids.idPere,ids.idMere,ids.idMenagePere, result);

	}



	@Test
	public void testCheckTiersWithDeuxParentDeuxMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
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
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(pere, fille, dateNaissance, null);
				addLiensFiliation(mere, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere,null,date(2006,7,8),null);
				MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();
				ids.idMenagePere = menageCommunPere.getId();

				final EnsembleTiersCouple ensembleTiersCoupleMere = addEnsembleTiersCouple(ppMere,null,date(2006,7,8),null);
				MenageCommun menageCommunMere = ensembleTiersCoupleMere.getMenage();
				ids.idMenageMere = menageCommunMere.getId();
				return null;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertDeuxParentsDeuxMCNonAssujetti(ids.idPere,ids.idMere,ids.idMenagePere,ids.idMenageMere, result);
	}



	@Test
	public void testCheckTiersWithDeuxParentDeuxMCDifferentsAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
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
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(pere, fille, dateNaissance, null);
				addLiensFiliation(mere, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				final EnsembleTiersCouple ensembleTiersCouplePere = addEnsembleTiersCouple(ppPere,null,date(2010, 1, 5),null);
				MenageCommun menageCommunPere = ensembleTiersCouplePere.getMenage();
				ids.idMenagePere = menageCommunPere.getId();
				addForPrincipal(menageCommunPere, date(2010, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);

				final EnsembleTiersCouple ensembleTiersCoupleMere = addEnsembleTiersCouple(ppMere,null,date(2009, 1, 5),null);
				MenageCommun menageCommunMere = ensembleTiersCoupleMere.getMenage();
				addForPrincipal(menageCommunMere, date(2009, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.idMenageMere = menageCommunMere.getId();
				return null;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertDeuxPArentsWithDeuxMenagesAssujetti(ids.idPere, ids.idMere, ids.idMenagePere, ids.idMenageMere, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentMemeMCNonAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
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
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(pere, fille, dateNaissance, null);
				addLiensFiliation(mere, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				final EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(ppPere,ppMere,date(2006,7,8),null);
				MenageCommun menageCommun = ensembleTiersCouple.getMenage();
				ids.idMenage = menageCommun.getId();
				addForPrincipal(menageCommun, date(2006, 7,8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2012,2,1),MotifFor.DEPART_HC,MockCommune.Moudon);

				return null;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertDeuxParentsWithUnMCNonAssujetti(ids.idPere,ids.idMere,ids.idMenage, result);
	}

	@Test
	public void testCheckTiersWithDeuxParentMemeMCAsujetti() throws Exception {
		final long noIndFille = 1244;
		final long noIndPere = 1245;
		final long noIndMere = 1246;
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
				final RegDate dateNaissance = date(2005, 3, 12);
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertDate", "Jeroma", Sexe.FEMININ);
				MockIndividu pere = addIndividu(noIndPere, date(1974, 8, 16), "RuppertDate", "PereJeroma", Sexe.MASCULIN);
				MockIndividu mere = addIndividu(noIndMere, date(1978, 10, 19), "RuppertDate", "MereJeroma", Sexe.FEMININ);
				addLiensFiliation(pere, fille, dateNaissance, null);
				addLiensFiliation(mere, fille, dateNaissance, null);


			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndPere);
				final PersonnePhysique ppMere = addHabitant(noIndMere);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				ids.idMere = ppMere.getId();
				final EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(ppPere,ppMere,date(2006,7,8),null);
				MenageCommun menageCommun = ensembleTiersCouple.getMenage();
				ids.idMenage = menageCommun.getId();
				addForPrincipal(menageCommun, date(2006, 7,8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);

				return null;
			}
		});
		final RegDate dateDemande = RegDate.get(2012,2,2);
		final ControlRuleForParentDate controlRuleForParentDate = new ControlRuleForParentDate(context, ids.idFille, dateDemande);
		final TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlRuleForParentDate.check();
			}
		});

		assertTiersAssujetti(ids.idMenage, result);
	}
}

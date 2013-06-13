package ch.vd.uniregctb.evenement.party.control;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlEchecType;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlManager;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;

public class TaxliabilityControlManagerTest extends AbstractControlTaxliabilityTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	//N0A1.1 - OK
	@Test
	public void testRunControlAssujettissementTiersOK() throws Exception {

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
		final RegDate date = null;
		final boolean periodic = true;
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxliabilityControlManager controlManager = new TaxliabilityControlManager(context);

		TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {

				return controlManager.runControlOnPeriode(idPP, periode,rechercheMenageCommun, rechercheParent);
			}
		});


		assertTiersAssujetti(idPP, result);

	}

	//N0A1.1 - KO
	@Test
	public void testRunControlAssujettissementTiersKO() throws Exception {

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
				return pp.getNumero();
			}
		});
		final Integer periode = 2012;
		final RegDate date = null;
		final boolean periodic = true;
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxliabilityControlManager controlManager = new TaxliabilityControlManager(context);

		TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlManager.runControlOnPeriode(idPP, periode, rechercheMenageCommun, rechercheParent);
			}
		});


		assertControlNumeroKO(result);

	}

	//N0A1.2a - OK
	@Test
	public void testRunControlAssujettissementTiersAvecMenageOK() throws Exception {

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
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				ids.idpp = pp.getId();

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(2000, 5, 5), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.idMc = menage.getId();
				addForPrincipal(menage, date(2000, 5, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);

				return null;

			}
		});
		final Integer periode = 2012;
		final RegDate date = null;
		final boolean periodic = true;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxliabilityControlManager controlManager = new TaxliabilityControlManager(context);

		TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {

				return controlManager.runControlOnPeriode(ids.idpp, periode, rechercheMenageCommun, rechercheParent);
			}
		});


		assertTiersAssujetti(ids.idMc, result);

	}

	//N0A1.2a - KO
	@Test
	public void testRunControlAssujettissementTiersAvecMenageKO() throws Exception {

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
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				ids.idpp = pp.getId();

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(2000, 5, 5), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.idMc = menage.getId();

				return null;

			}
		});
		final Integer periode = 2012;
		final RegDate date = null;
		final boolean periodic = true;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxliabilityControlManager controlManager = new TaxliabilityControlManager(context);

		TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {

				return controlManager.runControlOnPeriode(ids.idpp, periode, rechercheMenageCommun, rechercheParent);
			}
		});


		assertEquals(TaxliabilityControlEchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES, result.getEchec().getType());
		assertEquals(ids.idMc, result.getEchec().getMenageCommunIds().get(0));
	}

	//N0A1.2a - KO
	@Test
	public void testRunControlAssujettissementTiersSansMenageKO() throws Exception {

		final long noInd = 1244;
		class Ids {
			Long idpp;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				ids.idpp = pp.getId();


				return null;

			}
		});
		final Integer periode = 2012;
		final RegDate date = null;
		final boolean periodic = true;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxliabilityControlManager controlManager = new TaxliabilityControlManager(context);

		TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {

				return controlManager.runControlOnPeriode(ids.idpp, periode, rechercheMenageCommun, rechercheParent);
			}
		});


		assertEquals(TaxliabilityControlEchecType.AUCUN_MC_ASSOCIE_TROUVE, result.getEchec().getType());
	}

	//N0A1.2b - KO
	//Un tiers non assujetti sans parent sans menage, la requete demande une recherche sur le ménage et le parent
	@Test
	public void testRunControlAssujettissementTiersKOSansMenageSansParent() throws Exception {

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
				return pp.getNumero();
			}
		});
		final Integer periode = 2012;
		final RegDate date = null;
		final boolean periodic = true;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = true;

		final TaxliabilityControlManager controlManager = new TaxliabilityControlManager(context);

		TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlManager.runControlOnPeriode(idPP, periode, rechercheMenageCommun, rechercheParent);
			}
		});

		// le controle sur le tiers est ko, par les règles sur le tiers et sur le ménage, la règle sur les parents n'a pas été chargées
		//car la personne est majeur sans méange commun
		assertEquals(TaxliabilityControlEchecType.AUCUN_MC_ASSOCIE_TROUVE,result.getEchec().getType());

	}

	//N0A1.2b - KO
	//Un tiers non assujetti avec parent sans menage, la requete demande une recherche sur le ménage et le parent
	@Test
	public void testRunControlAssujettissementTiersSansMenageAvecParent() throws Exception {

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
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
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
				//addForPrincipal(ppParent, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);

				return null;
			}
		});
		final Integer periode = 2012;
		final RegDate date = null;
		final boolean periodic = true;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = true;

		final TaxliabilityControlManager controlManager = new TaxliabilityControlManager(context);

		TaxliabilityControlResult result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlManager.runControlOnPeriode(ids.idFille, periode, rechercheMenageCommun, rechercheParent);
			}
		});

		// le controle sur le tiers est ko, par les règles sur le tiers et sur le ménage, la règle sur les parents n'a pas été chargées
		//car la personne est majeur sans méange commun
		assertEquals(TaxliabilityControlEchecType.CONTROLE_SUR_PARENTS_KO,result.getEchec().getType());
		assertEquals(ids.idPere,result.getEchec().getParentsIds().get(0));


		//On ne demande pas le parent mais que le menage commun
		 result = doInNewTransaction(new TxCallback<TaxliabilityControlResult>() {
			@Override
			public TaxliabilityControlResult execute(TransactionStatus status) throws Exception {
				return controlManager.runControlOnPeriode(ids.idFille, periode, rechercheMenageCommun, false);
			}
		});

		//Meme si le tiers assujetti est mineur, on fait quand même une recherche sur les ménages communs tel que demandé dans le controle.
		assertEquals(TaxliabilityControlEchecType.AUCUN_MC_ASSOCIE_TROUVE,result.getEchec().getType());


	}
}

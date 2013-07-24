package ch.vd.uniregctb.evenement.party.control;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TaxLiabilityControlServiceTest extends AbstractControlTaxliabilityTest {

	private TaxLiabilityControlServiceImpl controlService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controlService = new TaxLiabilityControlServiceImpl();
		controlService.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		controlService.setTiersService(tiersService);
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
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent);
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
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent);
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
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent);
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
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent);
			}
		});

		assertEquals(TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES, result.getEchec().getType());
		assertEquals(ids.idMc, result.getEchec().getMenageCommunIds().get(0));
	}

	//N0A1.2a - KO
	@Test
	public void testRunControlAssujettissementTiersSansMenageKO() throws Exception {

		final long noInd = 1244;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1994, 3, 12), "RuppertPeriode", "Jeroma", Sexe.FEMININ);
			}
		});

		// on crée un habitant vaudois ordinaire
		final long ppId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});
		final Integer periode = 2012;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent);
			}
		});

		assertEquals(TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE, result.getEchec().getType());
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
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = true;

		final TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent);
			}
		});

		// le controle sur le tiers est ko, par les règles sur le tiers et sur le ménage, la règle sur les parents n'a pas été chargées
		//car la personne est majeur sans méange commun
		assertEquals(TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE,result.getEchec().getType());
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
		TaxLiabilityControlResult result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlService.doControlOnPeriod(pp, periode, true, true);
			}
		});

		// le controle sur le tiers est ko, par les règles sur le tiers et sur le ménage, la règle sur les parents n'a pas été chargées
		//car la personne est majeur sans méange commun
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO,result.getEchec().getType());
		assertEquals(ids.idPere,result.getEchec().getParentsIds().get(0));

		//On ne demande pas le parent mais que le menage commun
		result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlService.doControlOnPeriod(pp, periode, true, false);
			}
		});

		//Meme si le tiers assujetti est mineur, on fait quand même une recherche sur les ménages communs tel que demandé dans le controle.
		assertEquals(TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE, result.getEchec().getType());
	}

	@Test
	public void testControleAssujettissementPersonneMorale() throws Exception {

		// mise en place service PM
		servicePM.setUp(new DefaultMockServicePM());

		// mise en place fiscale
		final long idPm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntreprise(MockPersonneMorale.BCV.getNumeroEntreprise());
				return pm.getNumero();
			}
		});

		final int periode = 2012;
		final TaxLiabilityControlResult res = doInNewTransactionAndSession(new TxCallback<TaxLiabilityControlResult>() {
			@Override
			public TaxLiabilityControlResult execute(TransactionStatus status) throws Exception {
				final Entreprise pm = (Entreprise) tiersDAO.get(idPm);
				return controlService.doControlOnPeriod(pm, periode, true, true);
			}
		});

		assertNotNull(res);
		assertNotNull(res.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, res.getEchec().getType());
	}

	/**
	 * Cas du SIFISC-9339 : demande de contrôle d'assujettissement à une date postérieure au décès FISCAL du père d'un mineur,
	 * sachant que ces parents avaient déjà divorcé dans l'année.
	 * L'auteur du cas s'attendait à trouver la mère comme seule réponse du contrôle d'assujettissement mais le service
	 * a renvoyé une incertitude entre les deux parents
	 */
	@Test
	public void testControleAssujettissementMineurAvecParentSeparesPuisPereDecede() throws Exception {

		final long noIndPapa = 874841L;
		final long noIndMaman = 46451L;
		final long noIndMineur = 4512154L;
		final RegDate dateMariage = date(2000, 1, 1);
		final RegDate dateNaissanceMineur = date(2001, 9, 12);
		final RegDate dateDivorce = date(2012, 6, 23);
		final RegDate dateDecesPapa = date(2012, 8, 4);
		final RegDate dateDemandeControle = date(2012, 8, 15);

		// mise ne place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu papa = addIndividu(noIndPapa, null, "Potter", "James", Sexe.MASCULIN);
				final MockIndividu maman = addIndividu(noIndMaman, null, "Potter", "Lilly", Sexe.FEMININ);
				final MockIndividu mineur = addIndividu(noIndMineur, dateNaissanceMineur, "Potter", "Harry", Sexe.MASCULIN);

				marieIndividus(papa, maman, dateMariage);
				divorceIndividus(papa, maman, dateDivorce);

				addLiensFiliation(papa, mineur, dateNaissanceMineur, null);
				addLiensFiliation(maman, mineur, dateNaissanceMineur, null);
			}
		});

		final class Ids {
			long idPapa;
			long idMaman;
			long idMenage;
			long idMineur;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique papa = addHabitant(noIndPapa);
				papa.setDateDeces(dateDecesPapa);

				final PersonnePhysique maman = addHabitant(noIndMaman);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(papa, maman, dateMariage, dateDivorce.getOneDayBefore());
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aubonne);
				addForPrincipal(papa, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateDecesPapa, MotifFor.VEUVAGE_DECES, MockCommune.Aigle);
				addForPrincipal(maman, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aubonne);

				final PersonnePhysique mineur = addHabitant(noIndMineur);

				final Ids ids = new Ids();
				ids.idPapa = papa.getNumero();
				ids.idMaman = maman.getNumero();
				ids.idMenage = mc.getNumero();
				ids.idMineur = mineur.getNumero();
				return ids;
			}
		});

		// demande de contrôle
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mineur = (PersonnePhysique) tiersDAO.get(ids.idMineur);
				final TaxLiabilityControlResult res = controlService.doControlOnDate(mineur, dateDemandeControle, true, true);
				assertNotNull(res);
				assertNull(res.getEchec());
				assertEquals((Long) ids.idMaman, res.getIdTiersAssujetti());
				return null;
			}
		});
	}
}

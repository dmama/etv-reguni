package ch.vd.unireg.evenement.party.control;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
				addForPrincipal(pp, date(2001, 3, 12), MotifFor.MAJORITE, MockCommune.Moudon, ModeImposition.MIXTE_137_1);
				return pp.getNumero();
			}
		});
		final int periode = 2012;
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, null);
			}
		});

		assertTiersAssujetti(idPP, result);

		final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);
		final TaxLiabilityControlResult<TypeAssujettissement> resOK = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, toReject);
			}
		});

		assertTiersAssujetti(idPP, resOK);
	}

	@Test
	public void testRunControlAssujettissementTiersKOToReject() throws Exception {

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
				addForPrincipal(pp, date(2001, 3, 12), MotifFor.MAJORITE, MockCommune.Moudon, ModeImposition.MIXTE_137_1);
				return pp.getNumero();
			}
		});
		final int periode = 2012;
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final Set<TypeAssujettissement> toRejectMixte = EnumSet.of(TypeAssujettissement.MIXTE_137_1);
		final TaxLiabilityControlResult<TypeAssujettissement> resPeriode = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, toRejectMixte);
			}
		});

		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, resPeriode.getEchec().getType());
		assertTrue(resPeriode.getEchec().isAssujetissementNonConforme());

		final RegDate date= date(2012, 3, 1);
		final Set<ModeImposition> modeToReject = EnumSet.of(ModeImposition.MIXTE_137_1);
		final TaxLiabilityControlResult<ModeImposition> resDate = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnDate(pp, date, rechercheMenageCommun, rechercheParent, false, modeToReject);
			}
		});

		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, resDate.getEchec().getType());
		assertTrue(resDate.getEchec().isAssujetissementNonConforme());
	}

	@Test
	public void testRunControlAssujettissementV2PeriodeFuture() throws Exception {

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
		final int periode = RegDate.get().year()+1;
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, null);
			}
		});

		assertTiersAssujetti(idPP, result);
	}

	@Test
	public void testRunControlAssujettissementV2DateFuture() throws Exception {

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
		final int periode = RegDate.get().year()+1;
		final RegDate dateRef = date(periode,1, 21);
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnDate(pp, dateRef, rechercheMenageCommun, rechercheParent, false,null);
			}
		});

		assertTiersAssujetti(idPP, result);
	}

	@Test
	public void testRunControlAssujettissementV3KOPeriodeFuture() throws Exception {

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
		final int periode = RegDate.get().year()+1;
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, true, null);
			}
		});

		assertDatePeriodeDansFutur(result);
	}

	@Test
	public void testRunControlAssujettissementV3KODateFuture() throws Exception {

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
		final int periode = RegDate.get().year()+1;
		final RegDate dateRef = date(periode,1, 21);
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnDate(pp, dateRef, rechercheMenageCommun, rechercheParent, true,null);
			}
		});

		assertDatePeriodeDansFutur(result);
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
		final int periode = 2012;
		final boolean rechercheMenageCommun = false;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, null);
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
		final int periode = 2012;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, null);
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
		final int periode = 2012;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, null);
			}
		});

		assertEquals(TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES, result.getEchec().getType());
		assertEquals(ids.idMc, result.getEchec().getMenageCommunIds().get(0));
	}


	@Test
	public void testRunControlAssujettissementPPSouricerPurObtenantPermisC() throws Exception {

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
				addForPrincipalSource(pp, date(2013, 9, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2014, 3, 26), MotifFor.PERMIS_C_SUISSE, MockCommune.Moudon.getNoOFS());
				addForPrincipal(pp, date(2014, 3, 27), MotifFor.PERMIS_C_SUISSE, MockCommune.Moudon);


				return null;

			}
		});
		final int periode = 2014;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;
		final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);

				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, toReject);
			}
		});

		assertTiersAssujetti(ids.idpp,result);
	}

	@Test
	public void testRunControlAssujettissementPPSouricerAvecMenageAssuejettiOrdinaire() throws Exception {

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
				addForPrincipalSource(pp, date(2013, 9, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2014, 3, 26), MotifFor.PERMIS_C_SUISSE, MockCommune.Moudon.getNoOFS());

				addForPrincipal(pp, date(2014, 3, 27), MotifFor.PERMIS_C_SUISSE,date(2014, 8, 13), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);

				final RegDate dateMariage = date(2014, 8, 14);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage,dateMariage,MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,MockCommune.Moudon);
				ids.idMc = menage.getId();


				return null;

			}
		});
		final int periode = 2014;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;
		final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, toReject);
			}
		});

		assertTiersAssujetti(ids.idMc,result);
	}




	@Test
	public void testRunControlAssujettissementPPSouricerAvecDemandeMenageCommun() throws Exception {

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
				addForPrincipalSource(pp, date(2013, 9, 5), MotifFor.ARRIVEE_HS, null, null, MockCommune.Moudon.getNoOFS());





				return null;

			}
		});
		final int periode = 2014;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;
		final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, toReject);
			}
		});

		assertAssujetissmentModeImpositionNonConforme(result);
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, result.getEchec().getType());
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
		final int periode = 2012;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, null);
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
		final int periode = 2012;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = true;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, null);
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
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(fille, parent, null, dateNaissance, null);
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
				addParente(ppFille, ppParent, dateNaissance, null);
				//addForPrincipal(ppParent, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon);

				return null;
			}
		});

		final int periode = 2012;
		TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlService.doControlOnPeriod(pp, periode, true, true, false, null);
			}
		});

		// le controle sur le tiers est ko, par les règles sur le tiers et sur le ménage, la règle sur les parents n'a pas été chargées
		//car la personne est majeur sans méange commun
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO,result.getEchec().getType());
		assertEquals(ids.idPere,result.getEchec().getParentsIds().get(0));

		//On ne demande pas le parent mais que le menage commun
		result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlService.doControlOnPeriod(pp, periode, true, false, false, null);
			}
		});

		//Meme si le tiers assujetti est mineur, on fait quand même une recherche sur les ménages communs tel que demandé dans le controle.
		assertEquals(TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE, result.getEchec().getType());
	}

	//Rejet Sur Periode et sur Date
	@Test
	public void testRunControlAssujettissementTiersSansMenageAvecParentEtRejet() throws Exception {

		final long noIndFille = 1244;
		final long noIndParent = 1245;
		class Ids {
			Long idFille;
			Long idPere;
		}
		final Ids ids = new Ids();
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(fille, parent, null, dateNaissance, null);
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
				addParente(ppFille, ppParent, dateNaissance, null);
				addForPrincipal(ppParent, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon,ModeImposition.SOURCE);

				return null;
			}
		});

		//test sur ériode
		final int periode = 2012;
		final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE, TypeAssujettissement.MIXTE_137_1, TypeAssujettissement.MIXTE_137_2);
		final TaxLiabilityControlResult<TypeAssujettissement> resultPeriode = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlService.doControlOnPeriod(pp, periode, true, true, false, toReject);
			}
		});

		// le controle sest KO sur le parent a cause des assujetissements a rejeter
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, resultPeriode.getEchec().getType());
		assertEquals(ids.idPere, resultPeriode.getEchec().getParentsIds().get(0));
		assertTrue(resultPeriode.getEchec().isAssujetissementNonConforme());

		//Test sur Date
		final RegDate date =date(2012,2,12);
		final Set<ModeImposition> toRejectMode = EnumSet.of(ModeImposition.SOURCE, ModeImposition.MIXTE_137_1, ModeImposition.MIXTE_137_2);
		final TaxLiabilityControlResult<ModeImposition> resultDate = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlService.doControlOnDate(pp, date, true, true, false, toRejectMode);
			}
		});

		// le controle sest KO sur le parent a cause des mode d'imposition a rejeter
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, resultDate.getEchec().getType());
		assertEquals(ids.idPere, resultDate.getEchec().getParentsIds().get(0));
		assertTrue(resultDate.getEchec().isAssujetissementNonConforme());

	}

	//Rejet sur Date
	@Test
	public void testRunControlAssujettissementTiersSansMenageAvecParentDate() throws Exception {

		final long noIndFille = 1244;
		final long noIndParent = 1245;
		class Ids {
			Long idFille;
			Long idPere;
		}
		final Ids ids = new Ids();
		final RegDate dateNaissance = date(2005, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu fille = addIndividu(noIndFille, dateNaissance, "RuppertPeriode", "Jeroma", Sexe.FEMININ);
				MockIndividu parent = addIndividu(noIndParent, date(1974, 8, 16), "RuppertPeriode", "PereJeroma", Sexe.MASCULIN);
				addLiensFiliation(fille, parent, null, dateNaissance, null);
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
				addParente(ppFille, ppParent, dateNaissance, null);
				addForPrincipal(ppParent, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon,ModeImposition.SOURCE);

				return null;
			}
		});

		final RegDate date =date(2012,2,12);
		final Set<ModeImposition> toReject = EnumSet.of(ModeImposition.SOURCE, ModeImposition.MIXTE_137_1, ModeImposition.MIXTE_137_2);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				return controlService.doControlOnDate(pp, date, true, true, false, toReject);
			}
		});

		// le controle sur le tiers est ko, par les règles sur le tiers et sur le ménage, la règle sur les parents n'a pas été chargées
		//car la personne est majeur sans méange commun
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO,result.getEchec().getType());
		assertEquals(ids.idPere,result.getEchec().getParentsIds().get(0));
		assertTrue(result.getEchec().isAssujetissementNonConforme());

	}


	@Test
	public void testControleAssujettissementPersonneMorale() throws Exception {

		// mise en place service PM
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.BCV);
			}
		});

		// mise en place fiscale
		final long idPm = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BCV.getNumeroEntreprise());
			return pm.getNumero();
		});

		final int periode = 2012;
		final TaxLiabilityControlResult<TypeAssujettissement> res = doInNewTransactionAndSession(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final Entreprise pm = (Entreprise) tiersDAO.get(idPm);
				return controlService.doControlOnPeriod(pm, periode, true, true, false, null);
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

				addLiensFiliation(mineur, papa, maman, dateNaissanceMineur, null);
			}
		});

		final class Ids {
			long idPapa;
			long idMaman;
			long idMenage;
			long idMineur;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, status -> {
			final PersonnePhysique papa = addHabitant(noIndPapa);
			papa.setDateDeces(dateDecesPapa);

			final PersonnePhysique maman = addHabitant(noIndMaman);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(papa, maman, dateMariage, dateDivorce.getOneDayBefore());
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aubonne);
			addForPrincipal(papa, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateDecesPapa, MotifFor.VEUVAGE_DECES, MockCommune.Aigle);
			addForPrincipal(maman, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aubonne);

			final PersonnePhysique mineur = addHabitant(noIndMineur);

			final Ids ids1 = new Ids();
			ids1.idPapa = papa.getNumero();
			ids1.idMaman = maman.getNumero();
			ids1.idMenage = mc.getNumero();
			ids1.idMineur = mineur.getNumero();
			return ids1;
		});

		// demande de contrôle
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mineur = (PersonnePhysique) tiersDAO.get(ids.idMineur);
				final TaxLiabilityControlResult<ModeImposition> res = controlService.doControlOnDate(mineur, dateDemandeControle, true, true, false, null);
				assertNotNull(res);
				if (res.getEchec() != null) {
					fail(res.getEchec().toString());
				}
				assertEquals((Long) ids.idMaman, res.getIdTiersAssujetti());
				return null;
			}
		});
	}

	/**
	 * Pour le moment, les contrôles d'assujettissement sur les PM doivent toujours être négatifs
	 */
	@Test
	public void testControlePM() throws Exception {

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.BCV);
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BCV.getNumeroEntreprise());
			return pm.getNumero();
		});

		// demande de contrôle apériodique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Entreprise pm = (Entreprise) tiersDAO.get(pmId);
				final TaxLiabilityControlResult<ModeImposition> res = controlService.doControlOnDate(pm, date(2013, 5, 12), true, true, false,null);
				assertNotNull(res);
				assertNotNull(res.getEchec());
				assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, res.getEchec().getType());
				assertNull(res.getIdTiersAssujetti());
				return null;
			}
		});

		// demande de contrôle périodique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Entreprise pm = (Entreprise) tiersDAO.get(pmId);
				final TaxLiabilityControlResult<TypeAssujettissement> res = controlService.doControlOnPeriod(pm, 2013, true, true, false, null);
				assertNotNull(res);
				assertNotNull(res.getEchec());
				assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, res.getEchec().getType());
				assertNull(res.getIdTiersAssujetti());
				return null;
			}
		});
	}

	/**
	 * Pour le moment, les contrôles d'assujettissement sur les DPI doivent toujours être négatifs
	 */
	@Test
	public void testControleDPI() throws Exception {

		// mise en place fiscale
		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfredo", "Malaga", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.ChateauDoex);

			final DebiteurPrestationImposable dpi = addDebiteur("Débiteur", pp, date(2009, 1, 1));
			addForDebiteur(dpi, date(2009, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);
			return dpi.getNumero();
		});

		// demande de contrôle apériodique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final TaxLiabilityControlResult<ModeImposition> res = controlService.doControlOnDate(dpi, date(2013, 5, 12), true, true, false,null);
				assertNotNull(res);
				assertNotNull(res.getEchec());
				assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, res.getEchec().getType());
				assertNull(res.getIdTiersAssujetti());
				return null;
			}
		});

		// demande de contrôle périodique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final TaxLiabilityControlResult<TypeAssujettissement> res = controlService.doControlOnPeriod(dpi, 2013, true, true, false, null);
				assertNotNull(res);
				assertNotNull(res.getEchec());
				assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, res.getEchec().getType());
				assertNull(res.getIdTiersAssujetti());
				return null;
			}
		});
	}

	//SIFISC-12507
	@Test
	public void testRunControlAssujettissementTiersWithAssujToRejectWithMenageCommun() throws Exception {

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
				addForPrincipal(pp, date(2013, 1, 1), MotifFor.MAJORITE, MockCommune.Moudon, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});
		final int periode = 2013;
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE, TypeAssujettissement.MIXTE_137_1, TypeAssujettissement.MIXTE_137_2);
				return controlService.doControlOnPeriod(pp, periode, rechercheMenageCommun, rechercheParent, false, toReject);
			}
		});

		assertTrue(result.getEchec().isAssujetissementNonConforme());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, result.getEchec().getType());
	}

	@Test
	public void testRunControlAssujettissementMenageRejet() throws Exception {
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

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp,null,date(2000,5,5),null);
				final MenageCommun menage = ensemble.getMenage();
				ids.idMc = menage.getId();

				addForPrincipal(menage, date(2000, 5, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,MockCommune.Moudon, ModeImposition.MIXTE_137_1);

				return null;

			}
		});

		final RegDate dateControle = RegDate.get(2012,12,3);
		final boolean rechercheMenageCommun = true;
		final boolean rechercheParent = false;

		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				final Set<ModeImposition> toReject = EnumSet.of(ModeImposition.SOURCE, ModeImposition.MIXTE_137_1, ModeImposition.MIXTE_137_2);
				return controlService.doControlOnDate(pp, dateControle, rechercheMenageCommun, rechercheParent, false, toReject);
			}
		});

		assertAssujetissmentModeImpositionNonConforme(result);
		assertEquals(Collections.singletonList(ids.idMc), result.getEchec().getMenageCommunIds());
		assertEquals(TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES, result.getEchec().getType());
	}

	@Test
	public void testRunControlRejetParent() throws Exception {
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
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppParent = addHabitant(noIndParent);
				ids.idFille = ppFille.getId();
				ids.idPere = ppParent.getId();
				addForPrincipal(ppParent, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon, ModeImposition.SOURCE);
				addParente(ppFille, ppParent, dateNaissance, null);
				return null;
			}
		});

		final int periode = 2012;
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);
				return controlService.doControlOnPeriod(pp, periode, false, true, false, toReject);
			}
		});

		assertAssujetissmentModeImpositionNonConforme(result);
		assertEquals(Collections.singletonList(ids.idPere), result.getEchec().getParentsIds());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
	}

	@Test
	public void testRunControleRejetMenageParent() throws Exception {
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
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ppFille = addHabitant(noIndFille);
				final PersonnePhysique ppPere = addHabitant(noIndParent);
				ids.idFille = ppFille.getId();
				ids.idPere = ppPere.getId();
				addParente(ppFille, ppPere, dateNaissance, null);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ppPere, null, date(2000, 1, 5), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.idMenagePere = menage.getId();
				addForPrincipal(menage, date(2000, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Moudon, ModeImposition.MIXTE_137_1);

				return null;
			}
		});

		final int periode = 2012;
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.MIXTE_137_1);
				return controlService.doControlOnPeriod(pp, periode, false, true, false, toReject);
			}
		});

		assertAssujetissmentModeImpositionNonConforme(result);
		assertEquals(Collections.singletonList(ids.idMenagePere), result.getEchec().getMenageCommunParentsIds());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
	}

	@Test
	public void testRunControleRejetMenageCommunDeuxParents() throws Exception {
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
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
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
				addForPrincipal(menageCommun, date(2006, 7, 8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon, ModeImposition.SOURCE);

				return null;
			}
		});

		final int periode = 2012;
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idFille);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);
				return controlService.doControlOnPeriod(pp, periode, false, true, false, toReject);
			}
		});

		assertAssujetissmentModeImpositionNonConforme(result);
		assertEquals(Collections.singletonList(ids.idMenage), result.getEchec().getMenageCommunParentsIds());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
	}

	@Test
	public void testRunControleDateSansForPresentMaisRejetPresent() throws Exception {
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
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				final Set<ModeImposition> toReject = EnumSet.of(ModeImposition.SOURCE);
				return controlService.doControlOnDate(pp, dateControle, false, true, false, toReject);
			}
		});

		assertControlNumeroKO(result);
		assertFalse(result.getEchec().isAssujetissementNonConforme());
	}

	@Test
	public void testRunControleDateAvecForPresentPasConcerneParRejets() throws Exception {
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
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle);  // ORDINAIRE
				return pp.getNumero();
			}
		});

		final RegDate dateControle = RegDate.get(2010,12,3);
		final TaxLiabilityControlResult<ModeImposition> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<ModeImposition>>() {
			@Override
			public TaxLiabilityControlResult<ModeImposition> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				final Set<ModeImposition> toReject = EnumSet.of(ModeImposition.SOURCE);
				return controlService.doControlOnDate(pp, dateControle, false, true, false, toReject);
			}
		});

		assertTiersAssujetti(idPP, result);
		assertEquals(TaxLiabilityControlResult.Origine.INITIAL, result.getOrigine());
		assertEquals(EnumSet.of(ModeImposition.ORDINAIRE), result.getSourceAssujettissements());
	}

	@Test
	public void testRunControlePeriodeNonAssujettiAvecRejets() throws Exception {
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
				return pp.getNumero();
			}
		});

		final int periode = 2012;
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);
				return controlService.doControlOnPeriod(pp, periode, false, true, false, toReject);
			}
		});

		assertControlNumeroKO(result);
	}

	@Test
	public void testRunControlePeriodeAssujettiNonConcerneParRejets() throws Exception {
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
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);
				return pp.getNumero();
			}
		});

		final int periode = 2012;
		final TaxLiabilityControlResult<TypeAssujettissement> result = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);
				return controlService.doControlOnPeriod(pp, periode, false, true, false, toReject);
			}
		});

		assertTiersAssujetti(idPP, result);
		assertEquals(TaxLiabilityControlResult.Origine.INITIAL, result.getOrigine());
		assertEquals(EnumSet.of(TypeAssujettissement.MIXTE_137_2), result.getSourceAssujettissements());
	}

	/**
	 * [SIFISC-12861] il suffit d'une période d'imposition non-rejetée sur la période fiscale pour que le contrôle soit OK
	 */
	@Test
	public void testControleAssujetissementPeriodeMultiplesTypesAvecRejet() throws Exception {
		final long noInd = 1244;
		final int periode = 2012;

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
				addForPrincipal(pp, date(2010, 1, 1), MotifFor.ARRIVEE_HS, date(periode, 5, 13), MotifFor.PERMIS_C_SUISSE, MockCommune.Aubonne, ModeImposition.SOURCE);
				addForPrincipal(pp, date(periode, 5, 14), MotifFor.PERMIS_C_SUISSE, MockCommune.Aubonne, ModeImposition.ORDINAIRE);
				return pp.getNumero();
			}
		});

		//
		// premier test -> on refuse les sourciers purs (contrôle OK car le tiers est aussi à l'ordinaire sur la période)
		//
		final TaxLiabilityControlResult<TypeAssujettissement> resultSourceRejete = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.SOURCE_PURE);
				return controlService.doControlOnPeriod(pp, periode, false, true, false, toReject);
			}
		});

		assertTiersAssujetti(idPP, resultSourceRejete);
		assertEquals(TaxLiabilityControlResult.Origine.INITIAL, resultSourceRejete.getOrigine());
		assertEquals(EnumSet.of(TypeAssujettissement.VAUDOIS_ORDINAIRE, TypeAssujettissement.SOURCE_PURE), resultSourceRejete.getSourceAssujettissements());

		//
		// deuxième test -> on refuse les vaudois ordinaires (contrôle OK car le tiers est aussi sourcier pur sur la période)
		//
		final TaxLiabilityControlResult<TypeAssujettissement> resultOrdinaireRejete = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.VAUDOIS_ORDINAIRE);
				return controlService.doControlOnPeriod(pp, periode, false, true, false, toReject);
			}
		});

		assertTiersAssujetti(idPP, resultOrdinaireRejete);
		assertEquals(TaxLiabilityControlResult.Origine.INITIAL, resultOrdinaireRejete.getOrigine());
		assertEquals(EnumSet.of(TypeAssujettissement.VAUDOIS_ORDINAIRE, TypeAssujettissement.SOURCE_PURE), resultOrdinaireRejete.getSourceAssujettissements());

		//
		// troisième test -> on refuse les sourciers purs ET les vaudois ordinaires (contrôle KO)
		//
		final TaxLiabilityControlResult<TypeAssujettissement> resultSourceEtOrdinaireRejetes = doInNewTransaction(new TxCallback<TaxLiabilityControlResult<TypeAssujettissement>>() {
			@Override
			public TaxLiabilityControlResult<TypeAssujettissement> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				final Set<TypeAssujettissement> toReject = EnumSet.of(TypeAssujettissement.VAUDOIS_ORDINAIRE, TypeAssujettissement.SOURCE_PURE);
				return controlService.doControlOnPeriod(pp, periode, false, true, false, toReject);
			}
		});

		assertAssujetissmentModeImpositionNonConforme(resultSourceEtOrdinaireRejetes);
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, resultSourceEtOrdinaireRejetes.getEchec().getType());
	}
}

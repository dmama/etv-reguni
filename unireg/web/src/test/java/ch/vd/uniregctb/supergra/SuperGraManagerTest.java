package ch.vd.uniregctb.supergra;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.common.WebTestSpring3;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TarifImpotSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class SuperGraManagerTest extends WebTestSpring3 {

	private SuperGraManager manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.manager = getBean(SuperGraManager.class, "superGraManager");
	}

	@Test
	public void testTransformePp2Mc() throws Exception {

		final long noInd = 123456;
		final Long idPrincipal = 12300001L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1966,3,12), "Paul", "Trohion", Sexe.MASCULIN);
			}
		});

		// création de la personne physique à transformer en ménage
		final Long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Paul", "Trohion", date(1965, 3, 12), Sexe.MASCULIN);
				addSituation(pp, date(1976,1,12), null, 0);
				addIdentificationPersonne(pp, CategorieIdentifiant.CH_ZAR_RCE, "WERT");
				return pp.getNumero();
			}
		});

		// création du futur principal
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				addHabitant(idPrincipal, noInd);
			}
		});

		// transformation en ménage-commun
		manager.transformPp2Mc(id, date(1976,1,12), null, idPrincipal, null);

		// on vérifie qu'on a bien un ménage
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final MenageCommun mc = hibernateTemplate.get(MenageCommun.class, id);
				assertNotNull(mc);

				final Set<RapportEntreTiers> rapports = mc.getRapportsObjet();
				assertNotNull(rapports);
				assertEquals(1, rapports.size());

				final RapportEntreTiers rapport0 = rapports.iterator().next();
				assertInstanceOf(AppartenanceMenage.class, rapport0);
				final AppartenanceMenage appart0=(AppartenanceMenage) rapport0;

				final PersonnePhysique principal = hibernateTemplate.get(PersonnePhysique.class, appart0.getSujetId());
				assertNotNull(principal);
				assertEquals(idPrincipal, principal.getNumero());
			}
		});
	}

	@Test
	public void testTransformeMc2Pp() throws Exception {

		final long noInd = 123456;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1966,3,12), "Paul", "Trohion", Sexe.MASCULIN);
			}
		});

		// création du ménage commun à transformer en personne physique
		final Long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Paul", "Trohion", date(1965, 3, 12), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(1976, 1, 12), null);
				final MenageCommun menage = ensemble.getMenage();
				addSituation(menage, date(1976, 1, 12), null, 0, TarifImpotSource.NORMAL);
				return menage.getNumero();
			}
		});

		// transformation en ménage-commun
		manager.transformMc2Pp(id, noInd);

		// on vérifie qu'on a bien une personne physique
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
				assertNotNull(pp);
				assertEmpty(pp.getRapportsSujet());
				assertEmpty(pp.getRapportsObjet());
			}
		});
	}
}

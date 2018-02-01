package ch.vd.unireg.annulation.deces.manager;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.unireg.common.BusinessTestingConstants;
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@ContextConfiguration(locations = BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES)       // je veux le véritable tache-service !
public class AnnulationDecesRecapManagerTest extends WebTest {

	private AnnulationDecesRecapManager manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		manager = getBean(AnnulationDecesRecapManager.class, "annulationDecesRecapManager");
	}

	/**
	 * [SIFISC-13407] Crash pour org.hibernate.StaleObjectStateException à l'annulation d'un décès quand le flag
	 * de "majorité traitée" était différent de FALSE
	 */
	@Test
	public void testAnnulationDecesMajoriteTraiteeFalse() throws Exception {

		final long noIndividu = 347878L;
		final RegDate dateDeces = date(2014, 7, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1935, 11, 4), "Vaisselle", "Jehan", Sexe.MASCULIN);
				addEtatCivil(individu, date(1989, 10, 23), TypeEtatCivil.VEUF);
				individu.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(1976, 1, 7), MotifFor.INDETERMINE, date(1992, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(1992, 2, 1), MotifFor.DEMENAGEMENT_VD, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				pp.setMajoriteTraitee(false);
				return pp.getNumero();
			}
		});

		// annulation de décès comme fait dans le contrôleur ad'hoc
		final AnnulationDecesRecapView annulationDecesView = manager.get(ppId);
		manager.save(annulationDecesView);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getDateDeces());

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(date(1992, 2, 1), ffp.getDateDebut());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
		});
	}

	/**
	 * [SIFISC-13407] Crash pour org.hibernate.StaleObjectStateException à l'annulation d'un décès quand le flag
	 * de "majorité traitée" était différent de FALSE
	 */
	@Test
	public void testAnnulationDecesMajoriteTraiteeTrue() throws Exception {

		final long noIndividu = 347878L;
		final RegDate dateDeces = date(2014, 7, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1935, 11, 4), "Vaisselle", "Jehan", Sexe.MASCULIN);
				addEtatCivil(individu, date(1989, 10, 23), TypeEtatCivil.VEUF);
				individu.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(1976, 1, 7), MotifFor.INDETERMINE, date(1992, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(1992, 2, 1), MotifFor.DEMENAGEMENT_VD, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				pp.setMajoriteTraitee(true);
				return pp.getNumero();
			}
		});

		// annulation de décès comme fait dans le contrôleur ad'hoc
		final AnnulationDecesRecapView annulationDecesView = manager.get(ppId);
		manager.save(annulationDecesView);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getDateDeces());

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(date(1992, 2, 1), ffp.getDateDebut());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
		});
	}

	/**
	 * [SIFISC-13407] Crash pour org.hibernate.StaleObjectStateException à l'annulation d'un décès quand le flag
	 * de "majorité traitée" était différent de FALSE
	 */
	@Test
	public void testAnnulationDecesMajoriteTraiteeNull() throws Exception {

		final long noIndividu = 347878L;
		final RegDate dateDeces = date(2014, 7, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1935, 11, 4), "Vaisselle", "Jehan", Sexe.MASCULIN);
				addEtatCivil(individu, date(1989, 10, 23), TypeEtatCivil.VEUF);
				individu.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(1976, 1, 7), MotifFor.INDETERMINE, date(1992, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(1992, 2, 1), MotifFor.DEMENAGEMENT_VD, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				pp.setMajoriteTraitee(null);
				return pp.getNumero();
			}
		});

		// annulation de décès comme fait dans le contrôleur ad'hoc
		final AnnulationDecesRecapView annulationDecesView = manager.get(ppId);
		manager.save(annulationDecesView);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getDateDeces());

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(date(1992, 2, 1), ffp.getDateDebut());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
		});
	}

	/**
	 * [SIFISC-13197] crash à la tentative d'annulation du décès d'un marié seul veuf (décès le lendemain du veuvage)
	 */
	@Test
	public void testAnnulationDecesSurMarieSeulVeufDecedePeuApres() throws Exception {

		final RegDate dateDebut = date(1990, 8, 31);
		final RegDate dateMariage = date(2000, 2, 5);
		final RegDate dateDeces = RegDate.get();
		final RegDate dateVeuvage = dateDeces.getOneDayBefore();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Tartempion", date(1935, 3, 4), Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, dateVeuvage.getOneDayBefore());
				pp.setDateDeces(dateDeces);

				final MenageCommun mc = couple.getMenage();
				addForPrincipal(pp, dateDebut, MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle, ModeImposition.SOURCE);
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateVeuvage.getOneDayBefore(), MotifFor.VEUVAGE_DECES, MockCommune.Aigle, ModeImposition.SOURCE);
				addForPrincipal(pp, dateVeuvage, MotifFor.VEUVAGE_DECES, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aigle, ModeImposition.SOURCE);

				addSituation(pp, dateMariage, dateVeuvage.getOneDayBefore(), 0, EtatCivil.MARIE);
				addSituation(pp, dateVeuvage, dateDeces, 0, EtatCivil.VEUF);

				return pp.getNumero();
			}
		});

		// annulation de décès comme fait dans le contrôleur
		final AnnulationDecesRecapView annulationDecesView = manager.get(ppId);
		manager.save(annulationDecesView);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getDateDeces());

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateVeuvage, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
		});

		// et si maintenant on refait la même chose, ce sera une annulation de veuvage
		final AnnulationDecesRecapView annulationVeuvageView = manager.get(ppId);
		manager.save(annulationVeuvageView);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getDateDeces());

				{
					final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
					Assert.assertNotNull(ffp);
					Assert.assertEquals(dateDebut, ffp.getDateDebut());
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertEquals(dateMariage.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture());
					Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);

				final MenageCommun mc = couple.getMenage();
				Assert.assertNotNull(mc);
				{
					final ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
					Assert.assertNotNull(ffp);
					Assert.assertEquals(dateMariage, ffp.getDateDebut());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}
			}
		});
	}

	/**
	 * [SIFISC-17422] crash à la tentative d'annulation du décès d'un marié seul veuf
	 */
	@Test
	public void testAnnulationDecesSurMarieSeul() throws Exception {

		final RegDate dateDebut = date(1990, 8, 31);
		final RegDate dateMariage = date(2000, 2, 5);
		final RegDate dateDeces =  date(2005, 7, 10);
		final RegDate dateVeuvage = dateDeces.getOneDayAfter();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Tartempion", date(1935, 3, 4), Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, dateDeces);
				//pp.setDateDeces(dateDeces);

				final MenageCommun mc = couple.getMenage();
				addForPrincipal(pp, dateDebut, MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle, ModeImposition.SOURCE);
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aigle, ModeImposition.SOURCE);
				addForPrincipal(pp, dateVeuvage, MotifFor.VEUVAGE_DECES, null, null, MockCommune.Aigle, ModeImposition.SOURCE);

				addSituation(pp, dateMariage, dateDeces.getOneDayBefore(), 0, EtatCivil.MARIE);
				addSituation(pp, dateDeces, null, 0, EtatCivil.VEUF);

				return pp.getNumero();
			}
		});

		// annulation de Veuvage comme fait dans le contrôleur
		final AnnulationDecesRecapView annulationDecesView = manager.get(ppId);
		manager.save(annulationDecesView);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				//Assert.assertNull(pp.getDateDeces());

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDebut, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
				Assert.assertEquals(dateMariage.getOneDayBefore(),ffp.getDateFin());
				Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,ffp.getMotifFermeture());
				Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
		});

	}
}

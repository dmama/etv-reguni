package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DecesEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testDeces() throws Exception {

		final long noIndividu = 267813451L;
		final RegDate dateNaissance = RegDate.get().addMonths(-1).addYears(-30);
		final RegDate dateDeces = RegDate.get().addMonths(-1);

		// mise en place de son vivant
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Quatre", "Jessica", false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// décès civil
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// événement de décès
		final long decesId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(67235L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDeces);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DECES);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(decesId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);
				Assert.assertEquals((Long) ppId, pp.getNumero());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());
				Assert.assertEquals(dateDeces, ffp.getDateFin());
				return null;
			}
		});
	}

	/**
	 * Problème soulevé par [SIFISC-4877] :
	 *
	 * Si l'événement de changement d'état civil du survivant arrive avant l'evenement de décés alors
	 * celui-ci est en erreur alors qu'il serait plus judicieux qu'il soit redondant si sont for est
	 * cohérent (fermé pour motif décès).
	 *
	 */
	@Test(timeout = 10000L)
	public void testEvenementDecesArriveApresEvenementChangementEtatCivilDuSurvivant () throws Exception {

		// 1. Création d'un couple marié
		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateDecesMonsieur = RegDate.get(2012, 4, 27);

		// Dans le civil
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1924, 8, 1), "Lisette", "Bouton", false);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addNationalite(madame, MockPays.Suisse, date(1924, 8, 1), null);
				marieIndividus(monsieur, madame, RegDate.get(1947,7,14));
				veuvifieIndividu(madame, dateDecesMonsieur, false);
			}
		});

		// Dans le fiscal
		final Long[] ids = doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Long[]>() {
			@Override
			public Long[] execute(TransactionStatus status) throws Exception {
				PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, date(1947, 7, 13), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addAdresseSuisse(monsieur, TypeAdresseTiers.DOMICILE, date(1943, 2, 12), null, MockRue.Echallens.GrandRue);
				PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1944, 8, 1), MotifFor.MAJORITE, date(1947, 7, 13), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);
				addAdresseSuisse(madame, TypeAdresseTiers.DOMICILE, date(1944, 8, 1), date(1947, 7, 13), MockRue.Chamblon.RueDesUttins);
				addAdresseSuisse(madame, TypeAdresseTiers.DOMICILE, date(1947, 7, 14), null, MockRue.Echallens.GrandRue);
				EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(monsieur, madame, date(1947, 7, 14), null);
				addForPrincipal(ensembleTiersCouple.getMenage(), date(1947, 7, 14), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				return new Long[] {ensembleTiersCouple.getMenage().getId(), ensembleTiersCouple.getPrincipal().getId(), ensembleTiersCouple.getConjoint().getId()};
			}
		});
		final long menageId = ids[0];
		final long monsieurId = ids[1];
		final long madameId = ids[2];

		// 2. Création d'un evenement de "changement d'état civil" pour la survivante
		final long veuvageId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDecesMonsieur);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// 3. Traitement de l'évenement
		traiterEvenements(noMadame);

		// 4. Contrôle de cohérence des fors pour les 2 membres du couple
		//    - for du couple fermé pour motif déces à la date de l'événement
		//    - ouverture d'un for sur le survivant

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(veuvageId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique madame = (PersonnePhysique) tiersDAO.get(madameId);
				assertNotNull(madame);
				assertEquals((Long) noMadame, madame.getNumeroIndividu());

				// Verification que madame n'est plus en couple
				assertNull(tiersService.getEnsembleTiersCouple(madame, null));

				final MenageCommun menage  = (MenageCommun)tiersService.getTiers(menageId);

				final ForFiscalPrincipal ancienffp = menage.getForFiscalPrincipalAt(dateDecesMonsieur);
				assertNotNull(ancienffp);
				assertEquals(dateDecesMonsieur, ancienffp.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ancienffp.getMotifFermeture());

				final ForFiscalPrincipal ffp = madame.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateDecesMonsieur.getOneDayAfter() , ffp.getDateDebut());
				assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifOuverture());
				assertNull(ffp.getDateFin());
				assertNull(ffp.getMotifFermeture());

				return null;
			}
		});

		// 5. Création d'un évenemement "Décés" pour Monsieur (paix à son âme)
		final long decesId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563457L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDecesMonsieur);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.DECES);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// 6. Traitement de l'evenement
		traiterEvenements(noMonsieur);

		// 7. Verification de l'événement en erreur.
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(decesId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
				return null;
			}
		});

	}
}

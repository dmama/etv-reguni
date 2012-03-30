package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Ignore;
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
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DivorceEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testDivorce() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
				divorceIndividus(monsieur, madame, dateDivorce);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				final PersonnePhysique madame = addHabitant(noMadame);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDivorce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.DIVORCE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// on vérifie que le ménage-commun a bien été divorcé dans le fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(divorceId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final AppartenanceMenage appartenanceMonsieur = (AppartenanceMenage) monsieur.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMonsieur);
				assertEquals(dateMariage, appartenanceMonsieur.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), appartenanceMonsieur.getDateFin());

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final AppartenanceMenage appartenanceMadame = (AppartenanceMenage) madame.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMadame);
				assertEquals(dateMariage, appartenanceMadame.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), appartenanceMadame.getDateFin());

				assertNull(tiersService.getEnsembleTiersCouple(madame, dateDivorce));
				return null;
			}
		});
	}



	/**
	 *  [SIFISC-4641] Erreur lorsque traitement d'événement de divorce d'un couple
	 *
	 *  Erreur lorsque traitement d'événement de divorce pour un couple qui sont mariés. Dans RCPers, le liens marital existe pour la madame, mais il n'existe pas pour le monsieur.
	 *
	 * @throws Exception ..
	 */
	@Test
	public void testDivorce2MariésSeulsQuiAuraitDuEtreUnMenageCommun () throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);


		// création de 2 individus mariés dans le civil, madame "sait" qu'elle est marié avec monsieur.
		// Monsieur ne sait pas avec qui il est marié (c'est un cas normal d'apres l'équipe Rcpers)
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923,2,12), null);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1974, 8, 1), null);
				addNationalite(madame, MockPays.Suisse, date(1974, 8, 1), null);

				// Mariage: Seule madame "sait" qu'elle est mariée avec Monsieur
				marieIndividu(madame, dateMariage);
				marieIndividu(monsieur, dateMariage);
				addRelationConjoint(madame, monsieur, dateMariage);
			}
		});

		// Création de 2 contribuables mariés seuls dans le fiscal
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

				final PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

				final EnsembleTiersCouple monsieurMarieSeul = addEnsembleTiersCouple(monsieur, null, dateMariage, null);
				addForPrincipal(monsieurMarieSeul.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

				final EnsembleTiersCouple madameMarieSeul = addEnsembleTiersCouple(madame, null, dateMariage, null);
				addForPrincipal(madameMarieSeul.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);
				return null;
			}
		});

		// Divorce de monsieur dans le civil
		doModificationIndividu(noMonsieur, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.divorceIndividu(individu, dateDivorce);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMonsieurId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDivorce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.DIVORCE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement de monsieur
		traiterEvenements(noMonsieur);

		// Divorce de madame dans le civil
		doModificationIndividu(noMadame, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.divorceIndividu(individu, dateDivorce);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMadameId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563457L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDivorce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.DIVORCE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement de madame
		traiterEvenements(noMadame);

		// on vérifie que les ménages communs ont bien été divorcé dans le fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final EvenementCivilEch evtDivorceMonsieur = evtCivilDAO.get(divorceMonsieurId);
				assertNotNull(evtDivorceMonsieur);
				assertEquals(EtatEvenementCivil.TRAITE, evtDivorceMonsieur.getEtat());

				final EvenementCivilEch evtDivorceMadame = evtCivilDAO.get(divorceMadameId);
				assertNotNull(evtDivorceMadame);
				assertEquals(EtatEvenementCivil.TRAITE, evtDivorceMadame.getEtat());

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final AppartenanceMenage appartenanceMonsieur = (AppartenanceMenage) monsieur.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMonsieur);
				assertEquals(dateMariage, appartenanceMonsieur.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), appartenanceMonsieur.getDateFin());

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final AppartenanceMenage appartenanceMadame = (AppartenanceMenage) madame.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMadame);
				assertEquals(dateMariage, appartenanceMadame.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), appartenanceMadame.getDateFin());
				assertNull(tiersService.getEnsembleTiersCouple(madame, dateDivorce));
				return null;
			}
		});
	}

	/**
	 *  Test du divorce d'un ménage commun dont les données du civil sont incomplètes
	 *  Relation entre les Individus unidirectionnelle.
	 * @throws Exception ..
	 */
	@Test
	@Ignore
	public void testDivorceMenageCommunAvecRelationDeMariageUnidirectionnelDansLeCivil () throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);


		// création de 2 individus mariés dans le civil, madame "sait" qu'elle est marié avec monsieur.
		// Monsieur ne sait pas avec qui il est marié (c'est un cas normal d'apres l'équipe Rcpers)
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923,2,12), null);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1974, 8, 1), null);
				addNationalite(madame, MockPays.Suisse, date(1974, 8, 1), null);

				// Mariage: Seule madame "sait" qu'elle est mariée avec Monsieur
				marieIndividu(madame, dateMariage);
				marieIndividu(monsieur, dateMariage);
				addRelationConjoint(madame, monsieur, dateMariage);
			}
		});

		// Création de 2 contribuables et d'un ménage commun
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

				final PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

				return null;
			}
		});

		// Divorce de monsieur dans le civil
		doModificationIndividu(noMonsieur, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.divorceIndividu(individu, dateDivorce);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMonsieurId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDivorce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.DIVORCE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement de monsieur
		traiterEvenements(noMonsieur);

		// Divorce de madame dans le civil
		doModificationIndividu(noMadame, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.divorceIndividu(individu, dateDivorce);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMadameId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563457L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDivorce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.DIVORCE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement de madame
		traiterEvenements(noMadame);

		// on vérifie que le mEnages commun A bien été divorcé dans le fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final EvenementCivilEch evtDivorceMonsieur = evtCivilDAO.get(divorceMonsieurId);
				assertNotNull(evtDivorceMonsieur);
				assertEquals(EtatEvenementCivil.TRAITE, evtDivorceMonsieur.getEtat());

				final EvenementCivilEch evtDivorceMadame = evtCivilDAO.get(divorceMadameId);
				assertNotNull(evtDivorceMadame);
				assertEquals(EtatEvenementCivil.TRAITE, evtDivorceMadame.getEtat());

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final AppartenanceMenage appartenanceMonsieur = (AppartenanceMenage) monsieur.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMonsieur);
				assertEquals(dateMariage, appartenanceMonsieur.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), appartenanceMonsieur.getDateFin());

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final AppartenanceMenage appartenanceMadame = (AppartenanceMenage) madame.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMadame);
				assertEquals(dateMariage, appartenanceMadame.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), appartenanceMadame.getDateFin());
				assertNull(tiersService.getEnsembleTiersCouple(madame, dateDivorce));
				return null;
			}
		});
	}
}

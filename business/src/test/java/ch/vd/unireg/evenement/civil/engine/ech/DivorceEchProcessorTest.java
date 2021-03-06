package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypeRapportEntreTiers;

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

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
				divorceIndividus(monsieur, madame, dateDivorce);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			final PersonnePhysique madame = addHabitant(noMadame);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceId = genereEvenementDivorce(454563456L, noMonsieur, dateDivorce);

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// on vérifie que le ménage-commun a bien été divorcé dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(divorceId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			assertDivorce(noMonsieur, dateMariage, dateDivorce);
			assertDivorce(noMadame, dateMariage, dateDivorce);
			return null;
		});
	}


	/**
	 *  [SIFISC-4641] Erreur lorsque traitement d'événement de divorce d'un couple
	 *
	 *  Erreur lorsque traitement d'événement de divorce pour un couple qui sont mariés. Dans RCPers, le liens marital existe pour la madame, mais il n'existe pas pour le monsieur.
	 *
	 * @throws Exception ..
	 */
	@Test(timeout = 10000L)
	public void testDivorce2MariésSeulsQuiAuraitDuEtreUnMenageCommun () throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);


		// création de 2 individus mariés dans le civil, madame "sait" qu'elle est marié avec monsieur.
		// Monsieur ne sait pas avec qui il est marié (c'est un cas normal d'apres l'équipe Rcpers)
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923, 2, 12), null);
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
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

			final PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

			final EnsembleTiersCouple monsieurMarieSeul = addEnsembleTiersCouple(monsieur, null, dateMariage, null);
			addForPrincipal(monsieurMarieSeul.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

			final EnsembleTiersCouple madameMarieSeul = addEnsembleTiersCouple(madame, null, dateMariage, null);
			addForPrincipal(madameMarieSeul.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);
			return null;
		});

		// Divorce de monsieur dans le civil
		doModificationIndividu(noMonsieur, individu -> MockIndividuConnector.divorceIndividu(individu, dateDivorce));

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMonsieurId = genereEvenementDivorce(454563456L, noMonsieur, dateDivorce);

		// traitement synchrone de l'événement de monsieur
		traiterEvenements(noMonsieur);

		// Divorce de madame dans le civil
		doModificationIndividu(noMadame, individu -> MockIndividuConnector.divorceIndividu(individu, dateDivorce));

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMadameId = genereEvenementDivorce(454563457L, noMadame, dateDivorce);

		// traitement synchrone de l'événement de madame
		traiterEvenements(noMadame);

		// on vérifie que les ménages communs ont bien été divorcé dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtDivorceMonsieur = evtCivilDAO.get(divorceMonsieurId);
			assertNotNull(evtDivorceMonsieur);
			assertEquals(EtatEvenementCivil.TRAITE, evtDivorceMonsieur.getEtat());

			final EvenementCivilEch evtDivorceMadame = evtCivilDAO.get(divorceMadameId);
			assertNotNull(evtDivorceMadame);
			assertEquals(EtatEvenementCivil.TRAITE, evtDivorceMadame.getEtat());

			assertDivorce(noMonsieur, dateMariage, dateDivorce);
			assertDivorce(noMadame, dateMariage, dateDivorce);
			return null;
		});
	}

	/**
	 *  Test du divorce d'un ménage commun dont les données du civil sont incomplètes
	 *  Relation entre les Individus unidirectionnelle.
	 * @throws Exception ..
	 */
	@Test(timeout = 10000L)
	public void testDivorceMenageCommunAvecRelationDeMariageUnidirectionnelDansLeCivil () throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);

		// création de 2 individus mariés dans le civil, madame "sait" qu'elle est marié avec monsieur.
		// Monsieur ne sait pas avec qui il est marié (c'est un cas normal d'apres l'équipe Rcpers)
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923, 2, 12), null);
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
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

			final PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			return null;
		});

		// Divorce de monsieur dans le civil
		doModificationIndividu(noMonsieur, individu -> MockIndividuConnector.divorceIndividu(individu, dateDivorce));

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMonsieurId = genereEvenementDivorce(454563456L, noMonsieur, dateDivorce);

		// traitement synchrone de l'événement de monsieur
		traiterEvenements(noMonsieur);

		// Verification que l'evenement est en erreur car madame n'est pas encore divorcée dans le civil.
		// Ce comportement est succeptible et sans doute devrait changer dans le futur...
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtDivorceMonsieur = evtCivilDAO.get(divorceMonsieurId);
			assertNotNull(evtDivorceMonsieur);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evtDivorceMonsieur.getEtat());
			return null;
		});

		// Divorce de madame dans le civil
		doModificationIndividu(noMadame, individu -> MockIndividuConnector.divorceIndividu(individu, dateDivorce));

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMadameId = genereEvenementDivorce(454563457L, noMadame, dateDivorce);

		// traitement synchrone de l'événement de madame
		traiterEvenements(noMadame);

		// Retraitement de l'evenement de monsieur
		traiterEvenements(noMonsieur);

		// Verification que l'evenement est traité madame et monsieur étant desormais tout 2 divorcé dans le civil
		// Il devrait ne plus y avoir de problème, l'évenement de Madame devrait être traité et celui de Monsieur redondant
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtDivorceMadame = evtCivilDAO.get(divorceMadameId);
			assertNotNull(evtDivorceMadame);
			assertEquals(EtatEvenementCivil.TRAITE, evtDivorceMadame.getEtat());

			final EvenementCivilEch evtDivorceMonsieur = evtCivilDAO.get(divorceMonsieurId);
			assertNotNull(evtDivorceMonsieur);
			assertEquals(EtatEvenementCivil.REDONDANT, evtDivorceMonsieur.getEtat());
			return null;
		});

		// on vérifie que le menage commun ait bien été divorcé dans le fiscal
		doInNewTransactionAndSession(status -> {
			assertDivorce(noMonsieur, dateMariage, dateDivorce);
			assertDivorce(noMadame, dateMariage, dateDivorce);
			return null;
		});
	}


	@Test(timeout = 10000L)
	public void testDivorceConjointNonHabitant() throws Exception {

		final long noMadame = 46215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1974, 8, 1), null);
				marieIndividu(madame, dateMariage);
				divorceIndividu(madame, dateDivorce);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addNonHabitant("Crispus", "Santacorpus", date(1923, 2, 12), Sexe.MASCULIN);
			monsieur.setNumeroOfsNationalite(MockPays.Suisse.getNoOfsEtatSouverain());
			final PersonnePhysique madame = addHabitant(noMadame);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceId = genereEvenementDivorce(454563456L, noMadame, dateDivorce);

		// traitement synchrone de l'événement
		traiterEvenements(noMadame);

		// on vérifie que le ménage-commun a bien été divorcé dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(divorceId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			assertDivorce(noMadame, dateMariage, dateDivorce);
			return null;
		});
	}


	/**
	 * 	SIFISC-4719
	 *
	 *  Test du divorce d'un couple dont l'un des 2 membres et un ancien habitant (donc existant dans RCPERS)
	 *  Mais dont l'état civil est célibataire ! (le mariage ayant eu lieu alors qu'il était hors canton,
	 *  RCPERS ne mets pas à jour son état civil)
	 *
	 * @throws Exception ..
	 */
	@Test(timeout = 10000L)
	public void testDivorceHabitantEtAncienHabitantCelibataireDansLeCivilMarieSeul() throws Exception {
		// On fait le test avec un marié seul: evt -> OK
		testDivorceHabitantEtAncienHabitantCelibataireDansLeCivil(false);
	}

	@Test(timeout = 10000L)
	public void testDivorceHabitantEtAncienHabitantCelibataireDansLeCivilMenageNormal() throws Exception {
		// On fait le test avec un menage "normal" : evt -> KO
		testDivorceHabitantEtAncienHabitantCelibataireDansLeCivil(true);
	}

	private void testDivorceHabitantEtAncienHabitantCelibataireDansLeCivil (final boolean menageNormal) throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);
		final RegDate dateDepartMadameHC = date(2003, 11, 23);

		// création de 2 individus mariés dans le civil, madame "sait" qu'elle est marié avec monsieur.
		// Monsieur ne sait pas avec qui il est marié (c'est un cas normal d'apres l'équipe Rcpers)
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923, 2, 12), null);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1974, 8, 1), null);
				addNationalite(madame, MockPays.Suisse, date(1974, 8, 1), null);

				// Mariage: Seule monsieur "sait" qu'il est mariée avec Madame (elle étant hors canton)
				// Pour le civil madame est célibataire...
				marieIndividu(monsieur, dateMariage);
				addRelationConjoint(monsieur, madame, dateMariage);
			}
		});

		// Création de 2 contribuables et d'un ménage
		// Dans le cas du ménage commun, les 2 sont dans le menage sinon Monsieur est marié seul
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

			final PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(
					madame, date(1992, 8, 1),
					MotifFor.MAJORITE, dateDepartMadameHC,
					menageNormal ? MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION : MotifFor.DEPART_HC,
					MockCommune.Chamblon);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(monsieur, menageNormal ? madame : null, dateMariage, null);
			addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			return null;
		});

		// Divorce de monsieur dans le civil
		doModificationIndividu(noMonsieur, individu -> MockIndividuConnector.divorceIndividu(individu, dateDivorce));

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceMonsieurId = genereEvenementDivorce(454563456L, noMonsieur, dateDivorce);

		// traitement synchrone de l'événement de monsieur
		traiterEvenements(noMonsieur);

		// Verification du traitement de l'evt:
		//    - si en ménage commun alors EN_ERREUR car Madame est célibataire dans le civil
		//    - si Monsieur est marié seul c'est ok (on ne cherche plus a savoir qui est l'eventuelle conjoint dans le civil voir SIFISC-4641)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtDivorceMonsieur = evtCivilDAO.get(divorceMonsieurId);
			assertNotNull(evtDivorceMonsieur);
			assertEquals(menageNormal ? EtatEvenementCivil.EN_ERREUR : EtatEvenementCivil.TRAITE, evtDivorceMonsieur.getEtat());
			return null;
		});

		if (!menageNormal) {
			// on vérifie que le menage commun ait bien été divorcé dans le fiscal
			doInNewTransactionAndSession(status -> {
				assertDivorce(noMonsieur, dateMariage, dateDivorce);
				return null;
			});
		}
	}

	@Test(timeout = 10000L)
	public void testDivorceAvecDecisionPrincipal() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
				divorceIndividus(monsieur, madame, dateDivorce);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			final PersonnePhysique madame = addHabitant(noMadame);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addDecisionAci(monsieur, dateMariage, null, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceId = genereEvenementDivorce(454563456L, noMonsieur, dateDivorce);

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// on vérifie que le ménage-commun a bien été divorcé dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(divorceId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			Assert.assertNotNull(monsieur);
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(monsieur.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementDivorceAvantDecisionPrincipal() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);
		final RegDate dateDebutDecision = date(2009, 11, 23);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
				divorceIndividus(monsieur, madame, dateDivorce);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			final PersonnePhysique madame = addHabitant(noMadame);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addDecisionAci(monsieur, dateDebutDecision, null, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceId = genereEvenementDivorce(454563456L, noMonsieur, dateDivorce);

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// on vérifie que le ménage-commun a bien été divorcé dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(divorceId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			Assert.assertNotNull(monsieur);
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(monsieur.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}
	@Test(timeout = 10000L)
	public void testDivorceAvecDecisionConjoint() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
				divorceIndividus(monsieur, madame, dateDivorce);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			final PersonnePhysique madame = addHabitant(noMadame);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addDecisionAci(madame, dateMariage, null, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceId = genereEvenementDivorce(454563456L, noMonsieur, dateDivorce);

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// on vérifie que le ménage-commun a bien été divorcé dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(divorceId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			Assert.assertNotNull(monsieur);
			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			Assert.assertNotNull(madame);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(monsieur.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDivorceAvecDecisionCouple() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);
		class Ids {
			Long couple;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
				divorceIndividus(monsieur, madame, dateDivorce);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			final PersonnePhysique madame = addHabitant(noMadame);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addDecisionAci(ensemble.getMenage(), dateMariage, null, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			ids.couple = ensemble.getMenage().getId();
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceId = genereEvenementDivorce(454563456L, noMonsieur, dateDivorce);

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// on vérifie que le ménage-commun a bien été divorcé dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(divorceId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			Assert.assertNotNull(monsieur);
			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			Assert.assertNotNull(madame);

			final MenageCommun couple = (MenageCommun) tiersDAO.get(ids.couple);
			Assert.assertNotNull(couple);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(monsieur.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}


	private long genereEvenementDivorce(final long noEvt, final long noIndiv, final RegDate dateDivorce) throws Exception {
		return doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(noEvt);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDivorce);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndiv);
			evt.setType(TypeEvenementCivilEch.DIVORCE);
			return hibernateTemplate.merge(evt).getId();
		});
	}

	private void assertDivorce(long noIndiv, RegDate dateMariage, RegDate dateDivorce) {
		final PersonnePhysique indiv = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndiv);
		assertNotNull(indiv);
		final AppartenanceMenage appartenanceMenage = (AppartenanceMenage) indiv.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		assertNotNull(appartenanceMenage);
		assertEquals(dateMariage, appartenanceMenage.getDateDebut());
		assertEquals(dateDivorce.getOneDayBefore(), appartenanceMenage.getDateFin());
		assertNull(tiersService.getEnsembleTiersCouple(indiv, dateDivorce));
	}

}

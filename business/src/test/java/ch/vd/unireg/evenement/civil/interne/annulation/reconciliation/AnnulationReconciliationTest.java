package ch.vd.unireg.evenement.civil.interne.annulation.reconciliation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AnnulationReconciliationTest extends AbstractEvenementCivilInterneTest {

	private static final Log LOGGER = LogFactory.getLog(AnnulationReconciliationTest.class);
	
	@Test
	public void testAnnulationReconciliationMarieSeul() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'annulation de réconciliation d'une personne mariée seule");

		final RegDate dateMariage = date(1986, 4, 8);
		final RegDate dateSeparation = date(2000, 9, 11);
		final RegDate dateReconciliation = RegDate.get(2000, 11, 3);
		final long noIndividu = 12345;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil());

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividu);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, null, dateMariage, dateSeparation);
			final MenageCommun mc = couple.getMenage();
			addAppartenanceMenage(mc, lui, dateReconciliation, null, false);

			addForPrincipal(lui, date(1980, 1, 3), MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne, ModeImposition.SOURCE);
			addForPrincipal(lui, dateSeparation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconciliation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne,
			                ModeImposition.SOURCE);
			addForPrincipal(mc, dateReconciliation, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);
			return null;
		});

		// lancement du traitement de l'annulation de réconciliation
		doInNewTransactionAndSession(status -> {
			final Individu individu = serviceCivil.getIndividu(noIndividu, date(2008, 12, 31));
			final AnnulationReconciliation annulation = createAnnulationReconciliation(individu, dateReconciliation);

			final MessageCollector collector = buildMessageCollector();
			annulation.validate(collector, collector);
			assertEmpty("Une erreur est survenue lors du validate de l'annulation de réconciliation", collector.getErreurs());
			annulation.handle(collector);
			return null;
		});

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(noIndividu);
			assertNotNull("Pierre n'as pas été trouvé", pierre);

			// Vérification des fors fiscaux
			assertNotNull("Pierre doit avoir un for principal actif après l'annulation de réconciliation", pierre.getForFiscalPrincipalAt(null));
			for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
				if (forFiscal.getDateFin() != null && dateReconciliation.getOneDayBefore().equals(forFiscal.getDateFin()) &&
						(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
					assertEquals("Les fors fiscaux fermés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			// Vérification de la présence d'un tiers MenageCommun annulé
			MenageCommun menageCommun = null;
			int nbMenagesCommuns = 0;
			for (RapportEntreTiers rapport : pierre.getRapportsSujet()) {
				if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && dateReconciliation.equals(rapport.getDateDebut())) {
					nbMenagesCommuns++;
					menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
					assertEquals("Tous les rapports ménage devraient être fermés ou annulés", true, rapport.isAnnule());
				}
			}
			assertEquals("Il aurait dû y avoir 1 rapport entre tiers annulé", 1, nbMenagesCommuns);

			// Vérification du for principal du tiers MenageCommun
			ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
			assertNull("Aucun for fiscal principal actif aurait dû être trouvé sur le tiers MenageCommun", forCommun);
			for (ForFiscal forFiscal : menageCommun.getForsFiscaux()) {
				if (forFiscal.getDateFin() == null && dateReconciliation.equals(forFiscal.getDateDebut())) {
					assertEquals("Les fors fiscaux du ménage créés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			/*
			 * Evénements fiscaux devant être générés :
			 *  - annulation du for fermé
			 *  - réouverture for fiscal principal de Pierre
			 */
			assertEquals(2, eventSender.getCount());
			assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(pierre).size());
			return null;
		});
	}

	@Test
	public void testAnnulationReconciliationCouple() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de réconciliation d'une personne mariée et dont le conjoint est connu");

		final RegDate dateMariage = date(1986, 4, 8);
		final RegDate dateSeparation = date(2004, 3, 1);
		final RegDate dateReconciliation = date(2005, 7, 15);
		final long noIndividuMarie = 54321; // momo
		final long noIndividuConjoint = 23456; // béa

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil());

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuMarie);
			final PersonnePhysique elle = addHabitant(noIndividuConjoint);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateSeparation);
			final MenageCommun mc = couple.getMenage();
			addAppartenanceMenage(mc, lui, dateReconciliation, null, false);
			addAppartenanceMenage(mc, elle, dateReconciliation, null, false);

			addForPrincipal(lui, date(1977, 3, 1), MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			addForPrincipal(elle, date(1975, 2, 1), MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

			addForPrincipal(lui, dateSeparation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconciliation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			addForPrincipal(elle, dateSeparation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconciliation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			addForPrincipal(mc, dateReconciliation, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			return null;
		});

		// lancement du traitement de l'annulation de réconciliation
		doInNewTransactionAndSession(status -> {
			final Individu individu = serviceCivil.getIndividu(noIndividuMarie, date(2008, 12, 31));
			final Individu conjoint = serviceCivil.getIndividu(noIndividuConjoint, date(2008, 12, 31));
			final AnnulationReconciliation annulation = createAnnulationReconciliation(individu, conjoint, dateReconciliation);

			final MessageCollector collector = buildMessageCollector();
			annulation.validate(collector, collector);
			assertEmpty("Une erreur est survenue lors du validate de l'annulation de réconciliation", collector.getErreurs());
			annulation.handle(collector);
			return null;
		});

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(noIndividuMarie);
			assertNotNull("Le tiers n'as pas été trouvé", momo);
			// Vérification des fors fiscaux de momo
			assertNotNull("Maurice doit avoir un for principal actif après l'annulation de réconciliation", momo.getForFiscalPrincipalAt(null));
			for (ForFiscal forFiscal : momo.getForsFiscaux()) {
				if (forFiscal.getDateFin() != null && dateReconciliation.getOneDayBefore().equals(forFiscal.getDateFin()) &&
						(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
					assertEquals("Les fors fiscaux fermés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(noIndividuConjoint);
			assertNotNull("Le tiers n'as pas été trouvé", bea);
			// Vérification des fors fiscaux de bea
			assertNotNull("Béatrice doit avoir un for principal actif après l'annulation de réconciliation", bea.getForFiscalPrincipalAt(null));
			for (ForFiscal forFiscal : bea.getForsFiscaux()) {
				if (forFiscal.getDateFin() != null && dateReconciliation.getOneDayBefore().equals(forFiscal.getDateFin()) &&
						(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
					assertEquals("Les fors fiscaux fermés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			// Vérification de la présence d'un tiers MenageCommun annulé
			MenageCommun menageCommun = null;
			int nbMenagesCommuns = 0;
			for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
				if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && dateReconciliation.equals(rapport.getDateDebut())) {
					nbMenagesCommuns++;
					menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
					assertEquals("Tous les rapports ménage devraient être fermés ou annulés", true, rapport.isAnnule());
				}
			}
			assertEquals("Il aurait dû y avoir 1 rapport entre tiers annulé", 1, nbMenagesCommuns);

			// Vérification du for principal du tiers MenageCommun
			ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
			assertNull("Aucun for fiscal principal actif aurait dû être trouvé sur le tiers MenageCommun", forCommun);
			for (ForFiscal forFiscal : menageCommun.getForsFiscaux()) {
				if (forFiscal.getDateFin() == null && dateReconciliation.equals(forFiscal.getDateDebut())) {
					assertEquals("Les fors fiscaux du ménage créés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			/*
			 * Evénements fiscaux devant être générés :
			 *  - annulation des fors fermés (1 pour chacun)
			 *  - réouverture for fiscal principal de Maurice
			 *  - réouverture for fiscal principal de Béatrice
			 */
			assertEquals(4, eventSender.getCount());
			assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(momo).size());
			assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(bea).size());
			return null;
		});
	}

	@Test
	public void testAnnulationMariageCelibataire() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de mariage d'une personne non mariée (cas d'erreur).");

		final RegDate dateFictive = RegDate.get(2008, 1, 1);
		final long noIndividu = 6789;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil());

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(1998, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne, ModeImposition.SOURCE);
			return null;
		});

		// lancement du processus d'annulation de réconciliation
		doInNewTransactionAndSession(status -> {
			final Individu individu = serviceCivil.getIndividu(noIndividu, date(2008, 12, 31));
			final AnnulationReconciliation annulation = createAnnulationReconciliation(individu, dateFictive);

			final MessageCollector collector = buildMessageCollector();
			boolean errorFound = false;
			String errorMessage = null;
			try {
				annulation.validate(collector, collector);
			}
			catch (Exception ex) {
				errorFound = true;
				errorMessage = ex.getMessage();
			}
			assertTrue("Une erreur aurait dû se produire car cette personne n'est pas réconciliée", errorFound);
			assertEquals("L'erreur n'est pas la bonne", "Le tiers ménage commun n'a pu être trouvé", errorMessage);
			return null;
		});
	}


	private AnnulationReconciliation createAnnulationReconciliation(Individu individu, RegDate date) {
		return new AnnulationReconciliation(individu, null, date, 5652, context);
	}

	private AnnulationReconciliation createAnnulationReconciliation(Individu individu, Individu conjoint, RegDate date) {
		return new AnnulationReconciliation(individu, conjoint, date, 5652, context);
	}
}

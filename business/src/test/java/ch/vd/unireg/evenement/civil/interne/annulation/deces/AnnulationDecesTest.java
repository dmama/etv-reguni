package ch.vd.unireg.evenement.civil.interne.annulation.deces;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class AnnulationDecesTest extends AbstractEvenementCivilInterneTest {

	/**
	 * Le numéro d'individu du marié seul.
	 */
	private static final long NO_INDIVIDU_MARIE_SEUL = 92647;

	/**
	 * Le numéro d'individu du marié.
	 */
	private static final long NO_INDIVIDU_MARIE = 54321;
	private static final long NO_INDIVIDU_MARIE_CONJOINT = 23456;
	private static final RegDate DATE_MARIAGE = RegDate.get(1986, 4, 8);

	/**
	 * Le numéro d'individu du célibataire.
	 */
	private static final long NO_INDIVIDU_CELIBATAIRE = 6789;


	/**
	 * La date de décès.
	 */
	private static final RegDate DATE_DECES = RegDate.get(2008, 1, 1);

	@Test
	public void testAnnulationDecesCelibataire() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 2, 25);
				final MockIndividu andre = addIndividu(NO_INDIVIDU_CELIBATAIRE, dateNaissance, "Girard", "André", true);
				addNationalite(andre, MockPays.Suisse, dateNaissance, null);
				addAdresse(andre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, date(1995, 4, 19), null);
			}
		});

		// mise en place fiscale
		final Long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_INDIVIDU_CELIBATAIRE);
			addForPrincipal(pp, date(1995, 4, 19), MotifFor.ARRIVEE_HC, DATE_DECES, MotifFor.VEUVAGE_DECES, MockCommune.Fraction.LeLieu);
			return pp.getNumero();
		});

		// envoi de l'événement civil
		doInNewTransactionAndSession(status -> {
			final Individu ind = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, date(2008, 12, 31));
			final AnnulationDeces annulation = createValidAnnulationDeces(ind);

			final MessageCollector collector = buildMessageCollector();
			annulation.validate(collector, collector);
			assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", collector.getErreurs());

			annulation.handle(collector);
			return null;
		});

		// test du résultat
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_CELIBATAIRE);
			assertNotNull("Le tiers n'a pas été trouvé", pp);
			assertEquals(ppId, pp.getNumero());

			// Vérification des fors fiscaux
			assertNotNull("André doit avoir un for principal actif après l'annulation de décès", pp.getForFiscalPrincipalAt(null));
			for (ForFiscal forFiscal : pp.getForsFiscaux()) {
				if (forFiscal.getDateFin() != null && DATE_DECES.equals(forFiscal.getDateFin())) {
					assertEquals("Les fors fiscaux fermés lors du décès doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			/*
			 * Evénements fiscaux devant être générés :
			 *  - annulation du for fermé
			 *  - réouverture for fiscal principal sur l'ex-défunte
			 */
			assertEquals(2, eventSender.getCount());
			assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(pp).size());
			return null;
		});
	}

	@Test
	public void testAnnulationDecesCelibataireAvecDecisionAci() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 2, 25);
				final MockIndividu andre = addIndividu(NO_INDIVIDU_CELIBATAIRE, dateNaissance, "Girard", "André", true);
				addNationalite(andre, MockPays.Suisse, dateNaissance, null);
				addAdresse(andre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, date(1995, 4, 19), null);
			}
		});

		// mise en place fiscale
		final Long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_INDIVIDU_CELIBATAIRE);
			addForPrincipal(pp, date(1995, 4, 19), MotifFor.ARRIVEE_HC, DATE_DECES, MotifFor.VEUVAGE_DECES, MockCommune.Fraction.LeLieu);
			addDecisionAci(pp, date(1995, 4, 19), null, MockCommune.Aubonne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, "bof");
			return pp.getNumero();
		});

		// envoi de l'événement civil
		doInNewTransactionAndSession(status -> {
			final Individu ind = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, date(2008, 12, 31));
			final AnnulationDeces annulation = createValidAnnulationDeces(ind);

			final MessageCollector collector = buildMessageCollector();

			try {
				annulation.validate(collector, collector);
				fail("L'événement n'aurait pas dû valider : l'individu possède une décision ACI");
			}
			catch (EvenementCivilException e) {
				final String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI", FormatNumeroHelper.numeroCTBToDisplay(ppId));
				assertEquals(message, e.getMessage());
			}
			return null;
		});


	}

	@Test
	public void testAnnulationDecesMarieSeul() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu andre = addIndividu(NO_INDIVIDU_MARIE_SEUL, date(1956, 2, 25), "Girard", "André", true);
				addAdresse(andre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Lausanne, date(1980, 3, 1), null);
				marieIndividu(andre, DATE_MARIAGE);
			}
		});

		class Ids {
			long ppId;
			long mcId;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_INDIVIDU_MARIE_SEUL);
			addForPrincipal(pp, date(1980, 3, 1), MotifFor.ARRIVEE_HC, DATE_MARIAGE.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, DATE_MARIAGE, DATE_DECES);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, DATE_MARIAGE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, DATE_DECES, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);

			final Ids ids1 = new Ids();
			ids1.ppId = pp.getNumero();
			ids1.mcId = mc.getNumero();
			return ids1;
		});

		// envoi de l'événement civil
		doInNewTransactionAndSession(status -> {
			final Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_SEUL, date(2008, 12, 31));
			final AnnulationDeces annulation = createValidAnnulationDeces(individu);

			final MessageCollector collector = buildMessageCollector();
			annulation.validate(collector, collector);
			assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", collector.getErreurs());

			annulation.handle(collector);
			return null;
		});

		// vérification du résultat
		doInNewTransactionAndSession(status -> {

			final PersonnePhysique andre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_SEUL);
			assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", andre);
			assertEquals(ids.ppId, (long) andre.getNumero());

			// Vérification des fors fiscaux
			assertNull("André ne doit pas avoir de for principal actif après l'annulation de décès", andre.getForFiscalPrincipalAt(null));
			for (ForFiscal forFiscal : andre.getForsFiscaux()) {
				if (forFiscal.getDateFin() == null && DATE_DECES.getOneDayAfter().equals(forFiscal.getDateDebut())) {
					assertEquals("Les fors fiscaux créés lors du décès doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			// Vérification de la présence d'un tiers MenageCommun
			MenageCommun menageCommun = null;
			int nbMenagesCommuns = 0;
			for (RapportEntreTiers rapport : andre.getRapportsSujet()) {
				if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
					++nbMenagesCommuns;
					menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
					assertEquals(ids.mcId, (long) menageCommun.getNumero());
				}
			}
			assertEquals("Il aurait dû y avoir 2 rapports entre tiers: 1 annulé et 1 rouvert", 2, nbMenagesCommuns);
			assertNotNull(menageCommun);

			// Vérification du for principal du tiers MenageCommun
			final ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
			assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
			assertNull("Le for fiscal principal précédent devrait être rouvert (date null)", forCommun.getDateFin());
			assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)", forCommun.getMotifFermeture());

			/*
			 * Evénements fiscaux devant être générés :
			 *  - annulation du for fermé
			 *  - réouverture for fiscal principal sur le ménage de l'ex-défunt
			 */
			assertEquals(2, eventSender.getCount());
			assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
			return null;
		});
	}

	@Test
	public void testAnnulationDecesMarie() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ppal = addIndividu(NO_INDIVIDU_MARIE, date(1950, 3, 12), "Tartempion", "Momo", true);
				addAdresse(ppal, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Lausanne, date(1980, 3, 1), null);
				final MockIndividu conjoint = addIndividu(NO_INDIVIDU_MARIE_CONJOINT, date(1952, 7, 14), "Tartempion", "Béa", false);
				addAdresse(conjoint, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Lausanne, date(1984, 1, 1), null);
				marieIndividus(ppal, conjoint, DATE_MARIAGE);
			}
		});

		class Ids {
			long idM;
			long idMme;
			long idMc;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(NO_INDIVIDU_MARIE);
			final PersonnePhysique mme = addHabitant(NO_INDIVIDU_MARIE_CONJOINT);

			addForPrincipal(m, date(1980, 6, 30), MotifFor.ARRIVEE_HC, DATE_MARIAGE.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			addForPrincipal(mme, date(1984, 1, 1), MotifFor.ARRIVEE_HC, DATE_MARIAGE.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, DATE_MARIAGE, DATE_DECES);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, DATE_MARIAGE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, DATE_DECES, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);

			addForPrincipal(mme, DATE_DECES.getOneDayAfter(), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);

			final Ids ids1 = new Ids();
			ids1.idM = m.getNumero();
			ids1.idMme = mme.getNumero();
			ids1.idMc = mc.getNumero();
			return ids1;
		});

		// envoi de l'événement civil
		doInNewTransactionAndSession(status -> {
			final Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_MARIE, date(2008, 12, 31));
			final Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_CONJOINT, date(2008, 12, 31));
			final AnnulationDeces annulation = createValidAnnulationDeces(individu, conjoint);

			final MessageCollector collector = buildMessageCollector();
			annulation.validate(collector, collector);
			assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", collector.getErreurs());

			annulation.handle(collector);
			return null;
		});

		// test des résultats
		doInNewTransactionAndSession(status -> {
			/*
			 * Test de récupération du tiers defunt
			 */
			final PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE);
			assertNotNull("Le tiers n'a pas été trouvé", momo);
			assertEquals(ids.idM, (long) momo.getNumero());

			// Vérification des fors fiscaux
			assertNull("Maurice ne doit pas avoir de for principal actif après l'annulation de décès", momo.getForFiscalPrincipalAt(null));
			for (ForFiscal forFiscal : momo.getForsFiscaux()) {
				if (forFiscal.getDateFin() == null && DATE_DECES.getOneDayAfter().equals(forFiscal.getDateDebut())) {
					assertEquals("Les fors fiscaux créés lors du décès doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			final PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_CONJOINT);
			assertNotNull("Le tiers n'a pas été trouvé", bea);
			assertEquals(ids.idMme, (long) bea.getNumero());

			// Vérification des fors fiscaux
			assertNull("Béatrice ne doit pas avoir de for principal actif après l'annulation de décès", bea.getForFiscalPrincipalAt(null));
			for (ForFiscal forFiscal : bea.getForsFiscaux()) {
				if (forFiscal.getDateFin() == null && DATE_DECES.getOneDayAfter().equals(forFiscal.getDateDebut())) {
					assertEquals("Les fors fiscaux créés lors du décès doivent êtres annulés", true, forFiscal.isAnnule());
				}
			}

			// Vérification de la présence d'un tiers MenageCommun
			MenageCommun menageCommun = null;
			int nbMenagesCommuns = 0;
			for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
				if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
					nbMenagesCommuns++;
					menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
					assertEquals(ids.idMc, (long) menageCommun.getNumero());
				}
			}
			assertEquals("Il aurait dû y avoir 2 rapports entre tiers: 1 annulé et 1 rouvert", 2, nbMenagesCommuns);
			assertNotNull(menageCommun);

			// Vérification du for principal du tiers MenageCommun
			ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
			assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
			assertEquals("Le for fiscal principal du ménage n'a pas la bonne date de début", DATE_MARIAGE, forCommun.getDateDebut());
			assertNull("Le for fiscal principal précédent devrait être rouvert (date null)", forCommun.getDateFin());
			assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)", forCommun.getMotifFermeture());

			/*
			 * Evénements fiscaux devant être générés :
			 *  - annulation du for fermé
			 *  - réouverture for fiscal principal sur le ménage de l'ex-défunt
			 */
			assertEquals(2, eventSender.getCount());
			assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
			return null;
		});
	}

	private AnnulationDeces createValidAnnulationDeces(Individu individu) {
		return new AnnulationDeces(individu, null, DATE_DECES, 5652, context);
	}

	private AnnulationDeces createValidAnnulationDeces(Individu individu, Individu conjoint) {
		return new AnnulationDeces(individu, conjoint, DATE_DECES, 5652, context);
	}
}

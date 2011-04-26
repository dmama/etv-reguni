package ch.vd.uniregctb.evenement.civil.interne.annulation.deces;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

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
	@NotTransactional
	public void testAnnulationDecesCelibataire() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 2, 25);
				final MockIndividu andre = addIndividu(NO_INDIVIDU_CELIBATAIRE, dateNaissance, "Girard", "André", true);
				addNationalite(andre, MockPays.Suisse, dateNaissance, null, 1);
			}
		});

		// mise en place fiscale
		final Long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(NO_INDIVIDU_CELIBATAIRE);
				addForPrincipal(pp, date(1995, 4, 19), MotifFor.ARRIVEE_HC, DATE_DECES, MotifFor.VEUVAGE_DECES, MockCommune.Fraction.LeLieu);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final Individu ind = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, 2008);
				final AnnulationDeces annulation = createValidAnnulationDeces(ind);

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				annulation.checkCompleteness(erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de décès", erreurs);

				annulation.validate(erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", erreurs);

				annulation.handle(warnings);
				return null;
			}
		});

		// test du résultat
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

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
				 *  - réouverture for fiscal principal sur l'ex-défunte
				 */
				assertEquals(1, eventSender.count);
				assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(pp).size());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testAnnulationDecesMarieSeul() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu andre = addIndividu(NO_INDIVIDU_MARIE_SEUL, date(1956, 2, 25), "Girard", "André", true);
				marieIndividu(andre, DATE_MARIAGE);
			}
		});

		class Ids {
			long ppId;
			long mcId;
		}

		// mise en place fiscale
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(NO_INDIVIDU_MARIE_SEUL);
				addForPrincipal(pp, date(1980, 3, 1), MotifFor.ARRIVEE_HC, DATE_MARIAGE.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, DATE_MARIAGE, DATE_DECES);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, DATE_MARIAGE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, DATE_DECES, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.mcId = mc.getNumero();
				return ids;
			}
		});

		// envoi de l'événement civil
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_SEUL, 2008);
				final AnnulationDeces annulation = createValidAnnulationDeces(individu);

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				annulation.checkCompleteness(erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de décès", erreurs);

				annulation.validate(erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", erreurs);

				annulation.handle(warnings);
				return null;
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

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
						++ nbMenagesCommuns;
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
				 *  - réouverture for fiscal principal sur le ménage de l'ex-défunt
				 */
				assertEquals(1, eventSender.count);
				assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testAnnulationDecesMarie() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ppal = addIndividu(NO_INDIVIDU_MARIE, date(1950, 3, 12), "Tartempion", "Momo", true);
				final MockIndividu conjoint = addIndividu(NO_INDIVIDU_MARIE_CONJOINT, date(1952, 7, 14), "Tartempion", "Béa", false);
				marieIndividus(ppal, conjoint, DATE_MARIAGE);
			}
		});

		class Ids {
			long idM;
			long idMme;
			long idMc;
		}

		// mise en place fiscale
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(NO_INDIVIDU_MARIE);
				final PersonnePhysique mme = addHabitant(NO_INDIVIDU_MARIE_CONJOINT);

				addForPrincipal(m, date(1980, 6, 30), MotifFor.ARRIVEE_HC, DATE_MARIAGE.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(mme, date(1984, 1, 1), MotifFor.ARRIVEE_HC, DATE_MARIAGE.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, DATE_MARIAGE, DATE_DECES);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, DATE_MARIAGE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, DATE_DECES, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);

				addForPrincipal(mme, DATE_DECES.getOneDayAfter(), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);

				final Ids ids = new Ids();
				ids.idM = m.getNumero();
				ids.idMme = mme.getNumero();
				ids.idMc = mc.getNumero();
				return ids;
			}
		});

		// envoi de l'événement civil
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_MARIE, 2008);
				final Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_CONJOINT, 2008);
				final AnnulationDeces annulation = createValidAnnulationDeces(individu, conjoint);

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				annulation.checkCompleteness(erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de décès", erreurs);

				annulation.validate(erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", erreurs);

				annulation.handle(warnings);
				return null;
			}
		});

		// test des résultats
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

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
				 *  - réouverture for fiscal principal sur le ménage de l'ex-défunt
				 */
				assertEquals(1, eventSender.count);
				assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
				return null;
			}
		});
	}

	private AnnulationDeces createValidAnnulationDeces(Individu individu) {
		final Long principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), true);
		return new AnnulationDeces(individu, principalPPId, null, null, DATE_DECES, 5652, context);
	}

	private AnnulationDeces createValidAnnulationDeces(Individu individu, Individu conjoint) {
		return new AnnulationDeces(individu, conjoint, DATE_DECES, 5652, context);
	}
}

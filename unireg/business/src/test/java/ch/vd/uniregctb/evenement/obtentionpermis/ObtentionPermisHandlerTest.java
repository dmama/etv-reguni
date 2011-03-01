package ch.vd.uniregctb.evenement.obtentionpermis;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * @author Ludovic BERTIN <mailto:ludovic.bertin@gmail.com>
 * <a>
 */
public class ObtentionPermisHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(ObtentionPermisHandlerTest.class);

	/**
	 * Le numéro d'individu celibataire.
	 */
	private static final Long NO_INDIVIDU_SOURCIER_CELIBATAIRE = 6789L;

	/**
	 * Le numéro d'individu marié seul.
	 */
	private static final Long NO_INDIVIDU_SOURCIER_MARIE_SEUL = 12345L;

	/**
	 * Numéros des individus mariés.
	 */
	private static final Long NO_INDIVIDU_SOURCIER_MARIE = 54321L;
	private static final Long NO_INDIVIDU_SOURCIER_MARIE_CONJOINT = 23456L;

	/**
	 * Les dates d'obtention du permis
	 */
	private static final RegDate DATE_OBTENTION_PERMIS = RegDate.get(1986, 4, 8);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "ObtentionPermisTest.xml";

	private void setupServiceCivilAndLoadDatabase() throws Exception {
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionPermisHandlerSourcierCelibataire() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, 2007);
		ObtentionPermis obtentionPermis = createValidObtentionPermis(celibataire, DATE_OBTENTION_PERMIS, 5586);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		obtentionPermis.checkCompleteness(erreurs, warnings);
		obtentionPermis.validate(erreurs, warnings);
		obtentionPermis.handle(warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertTrue("Une erreur est survenue lors du traitement d'obtention de permis de célibataire.", erreurs.isEmpty());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique julie  = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", julie);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de Julie ont disparu", julie.getForsFiscaux());
		Assert.assertNotNull("Julie devrait encore avoir un for principal actif après l'obtention de permis", julie.getForFiscalPrincipalAt(null));
		Assert.assertEquals(ModeImposition.ORDINAIRE, julie.getForFiscalPrincipalAt(null).getModeImposition());

		Assert.assertEquals(date(1986, 5, 1), julie.getReindexOn()); // [UNIREG-1979] permis C -> réindexation au début du mois suivant l'obtention
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionPermisHandlerSourcierCelibataireMaisPermisNonC() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, 2007);
		ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(celibataire, DATE_OBTENTION_PERMIS, 4848, TypePermis.COURTE_DUREE);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		obtentionPermis.checkCompleteness(erreurs, warnings);
		obtentionPermis.validate(erreurs, warnings);
		obtentionPermis.handle(warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertTrue("Une erreur est survenue lors du traitement d'obtention de permis de célibataire.", erreurs.isEmpty());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique julie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", julie);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de Julie ont disparus", julie.getForsFiscaux());
		Assert.assertNotNull("Julie devrait encore avoir un for principal actif après l'obtention de permis", julie.getForFiscalPrincipalAt(null));
		Assert.assertEquals("Julie devrait encore son for principal actif inchangé après l'obtention de permis autre que C", ModeImposition.SOURCE, julie.getForFiscalPrincipalAt(null).getModeImposition());

		Assert.assertNull(julie.getReindexOn()); // [UNIREG-1979] pas de permis C, pas de réindexation dans le futur
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionPermisHandlerSourcierMarieSeul() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de marié seul.");
		Individu marieSeul = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL, 2007);
		ObtentionPermis obtentionPermis = createValidObtentionPermis(marieSeul, DATE_OBTENTION_PERMIS, 5586);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		obtentionPermis.checkCompleteness(erreurs, warnings);
		obtentionPermis.validate(erreurs, warnings);
		obtentionPermis.handle(warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		assertEmpty("Une erreur est survenue lors du traitement d'obtention de permis de marié seul.", erreurs);

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique pierre = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", pierre);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertEquals("Les for fiscaux de Pierre ont disparus", 1, pierre.getForsFiscaux().size());
		Assert.assertNull("Pierre ne doit toujours pas avoir de for principal actif après l'obtention de nationalité suisse", pierre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : pierre.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		Assert.assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		// 25-06-2009 (PBO) : selon la spéc la date d'ouverture du nouveau for doit être faite le 1er jour du mois qui suit l'événement
		Assert.assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date de l'obtention du permis", DATE_OBTENTION_PERMIS, forCommun.getDateDebut());
		Assert.assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());

		Assert.assertEquals(date(1986, 5, 1), menageCommun.getReindexOn()); // [UNIREG-1979] permis C -> réindexation au début du mois suivant l'obtention
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionPermisHandlerSourcierMarieADeux() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de marié à deux.");
		Individu marieADeux = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE, 2007);
		ObtentionPermis obtentionPermis = createValidObtentionPermis(marieADeux, DATE_OBTENTION_PERMIS, 5586);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		obtentionPermis.checkCompleteness(erreurs, warnings);
		obtentionPermis.validate(erreurs, warnings);
		obtentionPermis.handle(warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertTrue("Une erreur est survenue lors du traitement d'obtention de permis de marié seul.", erreurs.isEmpty());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", momo);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNull("Momo ne doit toujours pas avoir de for principal actif après l'obtention de permis", momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Test de récupération du Conjoint
		 */
		final PersonnePhysique bea = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_CONJOINT);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", bea);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNull("Béa ne doit toujours pas avoir de for principal actif après  l'obtention de permis",  bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : momo.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		Assert.assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		final ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",  forCommun);
		// 25-06-2009 (PBO) : selon la spéc la date d'ouverture du nouveau for doit être faite le 1er jour du mois qui suit l'événement
		Assert.assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date de l'obtention du permis", DATE_OBTENTION_PERMIS, forCommun.getDateDebut());
		Assert.assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());

		Assert.assertEquals(date(1986, 5, 1), menageCommun.getReindexOn()); // [UNIREG-1979] permis C -> réindexation au début du mois suivant l'obtention
	}

	/**
	 *
	 * @param individu
	 * @param dateObtentionPermis
	 * @param noOfsCommunePrincipale
	 * @return
	 */
	private ObtentionPermis createValidObtentionPermis(Individu individu, RegDate dateObtentionPermis, int noOfsCommunePrincipale) {

		final MockObtentionPermis obtentionPermis = new MockObtentionPermis(individu, null, dateObtentionPermis, 5586, noOfsCommunePrincipale, TypePermis.ETABLISSEMENT);
		obtentionPermis.setHandler(evenementCivilHandler);
		return obtentionPermis;
	}

	/**
	 *
	 * @param individu
	 * @param dateObtentionPermis
	 * @param noOfsCommunePrincipale
	 * @param typePermis
	 * @return
	 */
	private ObtentionPermis createValidObtentionPermisNonC(Individu individu, RegDate dateObtentionPermis, int noOfsCommunePrincipale, TypePermis typePermis) {
		final MockObtentionPermis obtentionPermis = new MockObtentionPermis(individu, null, dateObtentionPermis, 4848, noOfsCommunePrincipale, typePermis);
		obtentionPermis.setHandler(evenementCivilHandler);
		return obtentionPermis;
	}

	@Test
	@NotTransactional
	public void testObtentionPermisEtablissementPourAncienHabitant() throws Exception {

		final RegDate dateNaissance = date(1977, 5, 23);
		final RegDate dateObtentionPermis = date(2005, 6, 12);
		final RegDate dateDepart = date(2002, 1, 1);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu julie = addIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, dateNaissance, "Goux", "Julie", false);
				addNationalite(julie, MockPays.France, dateNaissance, null, 1);
				addPermis(julie, TypePermis.ANNUEL, dateNaissance, dateObtentionPermis.getOneDayBefore(), 1, false);
				addPermis(julie, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, 2, false);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateDepart, null);
			}
		});

		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
				pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
				Assert.assertNull(pp.getCategorieEtranger());
				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, 2007);
				final ObtentionPermis obtentionPermis = createValidObtentionPermis(julie, dateObtentionPermis, MockCommune.Neuchatel.getNoOFS());

				final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				obtentionPermis.checkCompleteness(erreurs, warnings);
				org.junit.Assert.assertTrue(erreurs.isEmpty());
				org.junit.Assert.assertTrue(warnings.isEmpty());

				obtentionPermis.validate(erreurs, warnings);
				org.junit.Assert.assertTrue(erreurs.isEmpty());
				org.junit.Assert.assertTrue(warnings.isEmpty());

				obtentionPermis.handle(warnings);
				org.junit.Assert.assertTrue(warnings.isEmpty());

				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				org.junit.Assert.assertNotNull(pp);
				org.junit.Assert.assertFalse(pp.isHabitantVD());
				org.junit.Assert.assertEquals(CategorieEtranger._03_ETABLI_C, pp.getCategorieEtranger());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testObtentionPermisNonEtablissementPourAncienHabitant() throws Exception {

		final RegDate dateNaissance = date(1977, 5, 23);
		final RegDate dateObtentionPermis = date(2005, 6, 12);
		final RegDate dateDepart = date(2002, 1, 1);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu julie = addIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, dateNaissance, "Goux", "Julie", false);
				addNationalite(julie, MockPays.France, dateNaissance, null, 1);
				addPermis(julie, TypePermis.COURTE_DUREE, dateNaissance, dateObtentionPermis.getOneDayBefore(), 1, false);
				addPermis(julie, TypePermis.ANNUEL, dateObtentionPermis, null, 2, false);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateDepart, null);
			}
		});

		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
				pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
				Assert.assertNull(pp.getCategorieEtranger());
				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, 2007);
				final ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(julie, dateObtentionPermis, MockCommune.Neuchatel.getNoOFS(), TypePermis.ANNUEL);

				final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				obtentionPermis.checkCompleteness(erreurs, warnings);
				org.junit.Assert.assertTrue(erreurs.isEmpty());
				org.junit.Assert.assertTrue(warnings.isEmpty());

				obtentionPermis.validate(erreurs, warnings);
				org.junit.Assert.assertTrue(erreurs.isEmpty());
				org.junit.Assert.assertTrue(warnings.isEmpty());

				obtentionPermis.handle(warnings);
				org.junit.Assert.assertTrue(warnings.isEmpty());

				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				org.junit.Assert.assertNotNull(pp);
				org.junit.Assert.assertFalse(pp.isHabitantVD());
				org.junit.Assert.assertEquals(CategorieEtranger._02_PERMIS_SEJOUR_B, pp.getCategorieEtranger());
				return null;
			}
		});
	}
}

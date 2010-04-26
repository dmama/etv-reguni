package ch.vd.uniregctb.evenement.obtentionpermis;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Test du handler des événements d'obtention de nationalité.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionNationaliteHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(ObtentionNationaliteHandlerTest.class);

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
	 * Les dates d'obtention de la nationalité
	 */
	private static final RegDate DATE_OBTENTION_NATIONALITE = RegDate.get(1986, 4, 8);

	/**
	 * Le fichier de données de test. C'est le meme que pour l'obtention de
	 * permis, les règles de traitement similiaires.
	 */
	private static final String DB_UNIT_DATA_FILE = "ObtentionPermisTest.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionNationaliteHandlerSourcierCelibataire() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'obtention de nationalité de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, 2007);
		ObtentionNationalite obtentionNationalite = createValidObtentionNationalite(celibataire, DATE_OBTENTION_NATIONALITE);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(obtentionNationalite, erreurs, warnings);
		evenementCivilHandler.validate(obtentionNationalite, erreurs, warnings);
		evenementCivilHandler.handle(obtentionNationalite, warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement d'obtention de nationalité de célibataire.");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique julie = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		Assert.isTrue(julie != null, "Plusieurs habitants trouvés avec le même numéro individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull(julie.getForsFiscaux(), "Les for fiscaux de Julie ont disparus");
		Assert.notNull(julie.getForFiscalPrincipalAt(null), "Julie devrait encore avoir un for principal actif après l'obtention de nationalité suisse");
		Assert.isTrue(julie.getForFiscalPrincipalAt(null).getModeImposition().equals(ModeImposition.ORDINAIRE));
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionNationaliteHandlerSourcierCelibataireMaisNationaliteNonSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'obtention de nationalité non suisse de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, 2007);
		ObtentionNationalite obtentionNationalite = createValidObtentionNationaliteNonSuisse(celibataire, DATE_OBTENTION_NATIONALITE);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(obtentionNationalite, erreurs, warnings);
		evenementCivilHandler.validate(obtentionNationalite, erreurs, warnings);
		evenementCivilHandler.handle(obtentionNationalite, warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement d'obtention de nationalité de célibataire.");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique julie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		Assert.isTrue(julie != null, "Plusieurs habitants trouvés avec le même numéro individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull(julie.getForsFiscaux(), "Les for fiscaux de Julie ont disparus");
		Assert.notNull(julie.getForFiscalPrincipalAt(null), "Julie devrait encore avoir un for principal actif après l'obtention de nationalité non suisse");
		Assert.isTrue(!julie.getForFiscalPrincipalAt(null).getModeImposition().equals(ModeImposition.ORDINAIRE),
				"Julie devrait encore avoir son for principal actif inchangé après l'obtention de nationalité autre que suisse");
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionNationaliteHandlerSourcierMarieSeul() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'obtention de nationalité de marié seul.");
		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				Individu marieSeul = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL, 2007);
				ObtentionNationalite obtentionNationalite = createValidObtentionNationalite(marieSeul, DATE_OBTENTION_NATIONALITE);

				evenementCivilHandler.checkCompleteness(obtentionNationalite, erreurs, warnings);
				evenementCivilHandler.validate(obtentionNationalite, erreurs, warnings);
				evenementCivilHandler.handle(obtentionNationalite, warnings);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		assertEmpty("Une erreur est survenue lors du traitement d'obtention de nationalité de marié seul.", erreurs);

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique pierre = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL);
		assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", pierre);

		/*
		 * Vérification des fors fiscaux
		 */
		assertEquals("Les for fiscaux de Pierre ont disparus", 1, pierre.getForsFiscaux().size());
		assertNull("Pierre ne doit toujours pas avoir de for principal actif après l'obtention de nationalité suisse", pierre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : pierre.getRapportsSujet()) {
			if (rapport.getType().equals(TypeRapportEntreTiers.APPARTENANCE_MENAGE)) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date de l'obtention de la nationalité suisse",
				DATE_OBTENTION_NATIONALITE, forCommun.getDateDebut());
		assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionNationaliteHandlerSourcierMarieADeux() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'obtention de nationalité de marié à deux.");
		Individu marieADeux = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE, 2007);
		ObtentionNationalite obtentionNationalite = createValidObtentionNationalite(marieADeux, DATE_OBTENTION_NATIONALITE);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(obtentionNationalite, erreurs, warnings);
		evenementCivilHandler.validate(obtentionNationalite, erreurs, warnings);
		evenementCivilHandler.handle(obtentionNationalite, warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement d'obtention de nationalité de marié seul.");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE);
		Assert.isTrue(momo != null, "Plusieurs habitants trouvés avec le même numéro individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.isNull(momo.getForFiscalPrincipalAt(null), "Momo ne doit toujours pas avoir de for principal actif après l'obtention de nationalité suisse");
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull(forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé");
		}

		/*
		 * Test de récupération du Conjoint
		 */
		PersonnePhysique bea = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_CONJOINT);
		Assert.isTrue(bea != null, "Plusieurs habitants trouvés avec le même numéro individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.isNull(bea.getForFiscalPrincipalAt(null), "Béa ne doit toujours pas avoir de for principal actif après  l'obtention de nationalité suisse");
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull(forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé");
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
			if (rapport.getType().equals(TypeRapportEntreTiers.APPARTENANCE_MENAGE)) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		Assert.isTrue(nbMenagesCommuns == 1, "Plusieurs ou aucun tiers MenageCommun ont été trouvés");

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.notNull(forCommun, "Aucun for fiscal principal trouvé sur le tiers MenageCommun");
		Assert.isTrue(DATE_OBTENTION_NATIONALITE.equals(forCommun.getDateDebut()),
				"La date d'ouverture du nouveau for ne correspond pas a la date de l'obtention de la nationalité suisse");
		Assert.isTrue(forCommun.getModeImposition().equals(ModeImposition.ORDINAIRE));
	}

	/**
	 *
	 * @param individu
	 * @param dateobtentionNationalite
	 * @return
	 */
	private ObtentionNationalite createValidObtentionNationalite(Individu individu, RegDate dateObtentionNationalite) {

		MockObtentionNationalite obtentionNationalite = new MockObtentionNationalite();
		obtentionNationalite.setIndividu(individu);

		obtentionNationalite.setNumeroOfsCommuneAnnonce(5586);
		obtentionNationalite.setNumeroOfsEtenduCommunePrincipale(5586);
		obtentionNationalite.setDate(dateObtentionNationalite);
		obtentionNationalite.setType(TypeEvenementCivil.NATIONALITE_SUISSE);

		return obtentionNationalite;
	}

	/**
	 *
	 * @param individu
	 * @param dateobtentionNationalite
	 * @return
	 */
	private ObtentionNationalite createValidObtentionNationaliteNonSuisse(Individu individu, RegDate dateObtentionNationalite) {
		MockObtentionNationalite obtentionNationalite = new MockObtentionNationalite();
		obtentionNationalite.setIndividu(individu);

		obtentionNationalite.setNumeroOfsCommuneAnnonce(5586);
		obtentionNationalite.setNumeroOfsEtenduCommunePrincipale(5586);
		obtentionNationalite.setDate(dateObtentionNationalite);
		obtentionNationalite.setType(TypeEvenementCivil.NATIONALITE_NON_SUISSE);

		return obtentionNationalite;
	}

}

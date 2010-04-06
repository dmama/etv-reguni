package ch.vd.uniregctb.evenement.obtentionpermis;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
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
	public void testObtentionPermisHandlerSourcierCelibataire() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, 2007);
		ObtentionPermis obtentionPermis = createValidObtentionPermis(celibataire, DATE_OBTENTION_PERMIS);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(obtentionPermis, erreurs, warnings);
		evenementCivilHandler.validate(obtentionPermis, erreurs, warnings);
		evenementCivilHandler.handle(obtentionPermis, warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement d'obtention de permis de célibataire.");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique julie  = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		Assert.isTrue(julie != null, "Plusieurs habitants trouvés avec le même numéro individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull( julie.getForsFiscaux(), "Les for fiscaux de Julie ont disparus" );
		Assert.notNull( julie.getForFiscalPrincipalAt(null), "Julie devrait encore avoir un for principal actif après l'obtention de permis" );
		Assert.isTrue( julie.getForFiscalPrincipalAt(null).getModeImposition().equals(ModeImposition.ORDINAIRE));
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionPermisHandlerSourcierCelibataireMaisPermisNonC() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, 2007);
		ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(celibataire, DATE_OBTENTION_PERMIS);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(obtentionPermis, erreurs, warnings);
		evenementCivilHandler.validate(obtentionPermis, erreurs, warnings);
		evenementCivilHandler.handle(obtentionPermis, warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement d'obtention de permis de célibataire.");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique julie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		Assert.isTrue(julie != null, "Plusieurs habitants trouvés avec le même numéro individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull( julie.getForsFiscaux(), "Les for fiscaux de Julie ont disparus" );
		Assert.notNull( julie.getForFiscalPrincipalAt(null), "Julie devrait encore avoir un for principal actif après l'obtention de permis" );
		Assert.isTrue( ! julie.getForFiscalPrincipalAt(null).getModeImposition().equals(ModeImposition.ORDINAIRE)
						, "Julie devrait encore son for principal actif inchangé après l'obtention de permis autre que C");
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionPermisHandlerSourcierMarieSeul() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de marié seul.");
		Individu marieSeul = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL, 2007);
		ObtentionPermis obtentionPermis = createValidObtentionPermis(marieSeul, DATE_OBTENTION_PERMIS);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(obtentionPermis, erreurs, warnings);
		evenementCivilHandler.validate(obtentionPermis, erreurs, warnings);
		evenementCivilHandler.handle(obtentionPermis, warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		assertEmpty("Une erreur est survenue lors du traitement d'obtention de permis de marié seul.", erreurs);

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
		for ( RapportEntreTiers rapport : pierre.getRapportsSujet() ) {
			if ( rapport.getType().equals( TypeRapportEntreTiers.APPARTENANCE_MENAGE ) ) {
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
		// 25-06-2009 (PBO) : selon la spéc la date d'ouverture du nouveau for doit être faite le 1er jour du mois qui suit l'événement
		assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date de l'obtention du permis",
				DATE_OBTENTION_PERMIS, forCommun.getDateDebut());
		assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testObtentionPermisHandlerSourcierMarieADeux() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de marié à deux.");
		Individu marieADeux = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE, 2007);
		ObtentionPermis obtentionPermis = createValidObtentionPermis(marieADeux, DATE_OBTENTION_PERMIS);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(obtentionPermis, erreurs, warnings);
		evenementCivilHandler.validate(obtentionPermis, erreurs, warnings);
		evenementCivilHandler.handle(obtentionPermis, warnings);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement d'obtention de permis de marié seul.");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE);
		Assert.isTrue(momo != null, "Plusieurs habitants trouvés avec le même numéro individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.isNull( momo.getForFiscalPrincipalAt(null), "Momo ne doit toujours pas avoir de for principal actif après l'obtention de permis" );
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull( forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé" );
		}

		/*
		 * Test de récupération du Conjoint
		 */
		PersonnePhysique bea = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_CONJOINT);
		Assert.isTrue(bea != null, "Plusieurs habitants trouvés avec le même numéro individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.isNull( bea.getForFiscalPrincipalAt(null), "Béa ne doit toujours pas avoir de for principal actif après  l'obtention de permis" );
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull( forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé" );
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : momo.getRapportsSujet() ) {
			if ( rapport.getType().equals( TypeRapportEntreTiers.APPARTENANCE_MENAGE ) ) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		Assert.isTrue( nbMenagesCommuns == 1, "Plusieurs ou aucun tiers MenageCommun ont été trouvés");

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.notNull( forCommun, "Aucun for fiscal principal trouvé sur le tiers MenageCommun" );
		// 25-06-2009 (PBO) : selon la spéc la date d'ouverture du nouveau for doit être faite le 1er jour du mois qui suit l'événement
		Assert.isTrue( DATE_OBTENTION_PERMIS.equals(forCommun.getDateDebut()), "La date d'ouverture du nouveau for ne correspond pas a la date de l'obtention du permis" );
		Assert.isTrue( forCommun.getModeImposition().equals(ModeImposition.ORDINAIRE));
	}

	/**
	 *
	 * @param individu
	 * @param dateObtentionPermis
	 * @return
	 */
	private ObtentionPermis createValidObtentionPermis(Individu individu, RegDate dateObtentionPermis) {

		MockObtentionPermis obtentionPermis = new MockObtentionPermis();
		obtentionPermis.setIndividu(individu);

		obtentionPermis.setNumeroOfsCommuneAnnonce(5586);
		obtentionPermis.setNumeroOfsEtenduCommunePrincipale(5586);
		obtentionPermis.setDate( dateObtentionPermis );
		obtentionPermis.setTypePermis( EnumTypePermis.ETABLLISSEMENT );

		return obtentionPermis;
	}

	/**
	 *
	 * @param individu
	 * @param dateObtentionPermis
	 * @return
	 */
	private ObtentionPermis createValidObtentionPermisNonC(Individu individu, RegDate dateObtentionPermis) {
		MockObtentionPermis obtentionPermis = new MockObtentionPermis();
		obtentionPermis.setIndividu(individu);

		obtentionPermis.setNumeroOfsCommuneAnnonce(4848);
		obtentionPermis.setNumeroOfsEtenduCommunePrincipale(4848);
		obtentionPermis.setDate( dateObtentionPermis );
		obtentionPermis.setTypePermis( EnumTypePermis.COURTE_DUREE );

		return obtentionPermis;
	}

}

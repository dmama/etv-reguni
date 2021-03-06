package ch.vd.unireg.evenement.civil.interne.annulationpermis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test du handler d'annulation de permis:
 * ---------------------------------------
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationPermis2Test extends AbstractEvenementCivilInterneTest {

	private static final Log LOGGER = LogFactory.getLog(AnnulationPermis2Test.class);

	/**
	 * Le numéro d'individu du celibataire.
	 */
	private static final Long NO_INDIVIDU_CELIBATAIRE = 6789L;

	/**
	 * Le numéro d'individu du marié seul.
	 */
	private static final Long NO_INDIVIDU_MARIE_SEUL = 92647L;

	/**
	 * Le numéro d'individu du marié à deux.
	 */
	private static final Long NO_INDIVIDU_MARIE = 78321L;
	private static final Long NO_INDIVIDU_MARIE_CONJOINT = 87321L;

	/**
	 * Les dates d'obtention du permis
	 */
	private static final RegDate DATE_OBTENTION_PERMIS = RegDate.get(2008, 9, 8);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationPermisTest.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				super.init();

				// Complement d'informations sur Julie pour ces tests
				MockIndividu julie = getIndividu(NO_INDIVIDU_CELIBATAIRE);
				addNationalite(julie, MockPays.France, RegDate.get(1976, 4, 19), null);
				addPermis(julie, TypePermis.SUISSE_SOURCIER, DATE_OBTENTION_PERMIS, null, false);

				// Nouveau individu pour les tests
				MockIndividu andre = addIndividu(NO_INDIVIDU_MARIE_SEUL, RegDate.get(1956, 2, 25), "Girard", "André", true);
				addDefaultAdressesTo(andre);
				marieIndividu(andre, RegDate.get(1982, 12, 4));
				addNationalite(andre, MockPays.France, andre.getDateNaissance(), null);
				addPermis(andre, TypePermis.SUISSE_SOURCIER, DATE_OBTENTION_PERMIS, null, false);

				// Nouveau couple pour les tests
				MockIndividu roger = addIndividu(NO_INDIVIDU_MARIE, RegDate.get(1943, 7, 3), "Dupont", "Roger", true);
				MockIndividu laure = addIndividu(NO_INDIVIDU_MARIE_CONJOINT, RegDate.get(1952, 11, 1), "Dupont", "Laure", false);

				addDefaultAdressesTo(roger);
				addDefaultAdressesTo(laure);

				marieIndividus(roger, laure, RegDate.get(1972, 2, 1));

				addOrigine(laure, MockCommune.Orbe);
				addNationalite(laure, MockPays.Suisse, laure.getDateNaissance(), null);

				addNationalite(roger, MockPays.France, roger.getDateNaissance(), null);
				addPermis(roger, TypePermis.ETABLISSEMENT, DATE_OBTENTION_PERMIS, null, false);
			}
		});
		loadDatabase(DB_UNIT_DATA_FILE);

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationPermisHandlerCelibataire() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement d'annulation de permis C de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, date(2008, 12, 31));
		AnnulationPermis annulationPermis = createValidAnnulationPermis(celibataire, DATE_OBTENTION_PERMIS);

		final MessageCollector collector = buildMessageCollector();
		annulationPermis.validate(collector, collector);
		assertFalse("Une erreur est survenue lors du validate de l'annulation.", collector.hasErreurs());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationPermisHandlerCelibataireMaisPermisNonC() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement d'annulation de permis non C de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, date(2008, 12, 31));
		AnnulationPermis annulationPermis = createValidAnnulationPermisNonC(celibataire, DATE_OBTENTION_PERMIS);

		final MessageCollector collector = buildMessageCollector();
		annulationPermis.validate(collector, collector);
		assertFalse("Une erreur est survenue lors du validate de l'annulation.", collector.hasErreurs());
		annulationPermis.handle(collector);

		// Test de récupération du Tiers
		PersonnePhysique julie  = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_CELIBATAIRE);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)",
				julie);

		// Vérification des fors fiscaux
		assertNotNull("Les for fiscaux de Julie ont disparus",
				julie.getForsFiscaux());
		assertNotNull("Julie devrait encore avoir un for principal actif après l'annulation de permis C",
				julie.getForFiscalPrincipalAt(null));
		assertEquals("Julie devrait être imposée a la source",
				ModeImposition.ORDINAIRE, julie.getForFiscalPrincipalAt(null).getModeImposition());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationPermisHandlerMarieSeul() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement d'annulation de permis de marié seul.");
		Individu marieSeul = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_SEUL, date(2008, 12, 31));
		AnnulationPermis annulationPermis = createValidAnnulationPermisNonC(marieSeul, DATE_OBTENTION_PERMIS);

		final MessageCollector collector = buildMessageCollector();
		annulationPermis.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation.", collector.getErreurs());
		annulationPermis.handle(collector);

		// Test de récupération du Tiers
		PersonnePhysique andre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_SEUL);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)",
				andre);

		// Vérification des fors fiscaux
		assertEquals("Les for fiscaux de André ont disparus", 1, andre.getForsFiscaux().size());
		assertNull("André ne doit toujours pas avoir de for principal actif après l'annulation de permis",
				andre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : andre.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé",
						forFiscal.getDateFin());
		}

		// Vérification de la présence d'un tiers MenageCommun
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : andre.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		assertNull("Le for fiscal principal précédent devrait être rouvert (date null)", forCommun.getDateFin());
		assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)", forCommun.getMotifFermeture());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPermisHandlerMarieADeux() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement d'annulation de permis de marié à deux.");
		Individu marie = serviceCivil.getIndividu(NO_INDIVIDU_MARIE, date(2008, 12, 31));
		AnnulationPermis annulationPermis = createValidAnnulationPermis(marie, DATE_OBTENTION_PERMIS);

		final MessageCollector collector = buildMessageCollector();
		annulationPermis.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation.", collector.getErreurs());
		annulationPermis.handle(collector);

		// Test de récupération du Tiers
		PersonnePhysique roger = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_MARIE);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)",
				roger);

		// Vérification des fors fiscaux
		assertNull("Momo ne doit toujours pas avoir de for principal actif après l'obtention de nationalité suisse",
				roger.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : roger.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé",
						forFiscal.getDateFin());
		}

		// Test de récupération du Conjoint
		PersonnePhysique laure = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_MARIE_CONJOINT);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", laure);

		// Vérification des fors fiscaux
		assertNull("Béa ne doit toujours pas avoir de for principal actif après  l'obtention de nationalité suisse",
				laure.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : laure.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé",
						forFiscal.getDateFin());
		}

		// Vérification de la présence d'un tiers MenageCommun
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : roger.getRapportsSujet()) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipalPP forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",
				forCommun);
		assertNull("Le for fiscal principal précédent devrait être rouvert (date null)",
				forCommun.getDateFin());
		assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)",
				forCommun.getMotifFermeture());
		assertEquals("Le mode d'imposition doit être SOURCE",
				forCommun.getModeImposition(), ModeImposition.SOURCE);
	}

	private AnnulationPermis createValidAnnulationPermis(Individu individu, RegDate dateObtentionPermis) {
		return new AnnulationPermis(individu, null, dateObtentionPermis, 5586, TypePermis.ETABLISSEMENT, context);
	}

	private AnnulationPermis createValidAnnulationPermisNonC(Individu individu, RegDate dateObtentionPermis) {
		return new AnnulationPermis(individu, null, dateObtentionPermis, 5586, TypePermis.COURTE_DUREE, context);
	}
}

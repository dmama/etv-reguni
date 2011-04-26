package ch.vd.uniregctb.evenement.civil.interne.veuvage;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VeuvageTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(VeuvageTest.class);
	
	/**
	 * Le numéro d'individu du veuf.
	 */
	private static final long NO_INDIVIDU_VEUF = 12345;

	/**
	 * Numéro du ménage du veuf
	 */
	private static final long NO_MENAGE_COMMUN_INDIVIDU_VEUF = 7001;
	
	/**
	 * Le numéro d'individu du veuf étranger.
	 */
	private static final long NO_INDIVIDU_VEUF_ETRANGER = 34897;

	/**
	 * Numéro du ménage du veuf
	 */
	private static final long NO_MENAGE_COMMUN_INDIVIDU_VEUF_ETRANGER = 7003;
	
	/**
	 * Le numéro d'individu du veuf marié
	 */
	private static final long NO_INDIVIDU_VEUF_MARIE = 89123;
	
	/**
	 * Le numéro d'un individu marié seul avec un for principal ouvert après la date de veuvage.
	 */
	private static final long NO_INDIVIDU_VEUF_AVEC_FOR = 908234;
	
	/**
	 * La date de veuvage.
	 */
	private static final RegDate DATE_VEUVAGE = RegDate.get(2008, 1, 1);
	
	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "VeuvageTest.xml";
	
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			
			@Override
			protected void init() {
				super.init();
				
				final RegDate dateNaissanceMikkel = RegDate.get(1961, 3, 12);
				final MockIndividu mikkel = addIndividu(NO_INDIVIDU_VEUF_ETRANGER, dateNaissanceMikkel, "Hirst", "Mikkel", true);
				addDefaultAdressesTo(mikkel);
				marieIndividu(mikkel, RegDate.get(1986, 4, 8));
				addOrigine(mikkel, MockPays.Danemark, null, dateNaissanceMikkel);
				addNationalite(mikkel, MockPays.Danemark, dateNaissanceMikkel, null, 0);
				addEtatCivil(mikkel, DATE_VEUVAGE, TypeEtatCivil.VEUF);
				
				final RegDate dateNaissanceRyan = RegDate.get(1961, 3, 1);
				final MockIndividu ryan = addIndividu(NO_INDIVIDU_VEUF_AVEC_FOR, dateNaissanceRyan, "Bolomé", "Ryan", true);
				addDefaultAdressesTo(ryan);
				marieIndividu(ryan, RegDate.get(1986, 4, 8));
				addOrigine(ryan, MockPays.Suisse, MockCommune.Renens, dateNaissanceRyan);
				addNationalite(ryan, MockPays.Suisse, dateNaissanceRyan, null, 0);
				addEtatCivil(ryan, DATE_VEUVAGE, TypeEtatCivil.VEUF);

				final MockIndividu pierre = getIndividu(NO_INDIVIDU_VEUF);
				addEtatCivil(pierre, DATE_VEUVAGE, TypeEtatCivil.VEUF);
			}
		});
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testVeuvageSuisse() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement de veuvage d'un suisse marié seul.");
		
		Individu veufSuisse = serviceCivil.getIndividu(NO_INDIVIDU_VEUF, 2008);
		Veuvage veuvage = createVeuvage(veufSuisse);
	
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
	
		veuvage.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du traitement de veuvage", erreurs);
		veuvage.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du traitement de veuvage", erreurs);
		veuvage.handle(warnings);
		assertEmpty("Une erreur est survenue lors du traitement de veuvage", erreurs);
		
		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_VEUF);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", pierre);
		
		/*
		 * Ses for principaux actifs doivent avoir été ouverts
		 */
		ForFiscalPrincipal ffp = pierre.getForFiscalPrincipalAt(null);
		assertNotNull("Le for principal du veuf n'a pas été ouvert", ffp);
		assertEquals("Le mode d'imposition du veuf devrait être ORDINAIRE", ModeImposition.ORDINAIRE, ffp.getModeImposition());
		assertEquals("Date d'ouverture de for incorrecte", DATE_VEUVAGE.getOneDayAfter(), ffp.getDateDebut());
		
		/*
		 * Test de récupération du tiers menageCommun
		 */
		Contribuable menageCommun = tiersDAO.getContribuableByNumero(NO_MENAGE_COMMUN_INDIVIDU_VEUF);
		assertNotNull("Le tiers correspondant au ménage commun n'a pas été trouvé", menageCommun);
		
		/*
		 * Test sur le menage commun, il ne doit plus rester de for principal ouvert
		 */
		assertNull("Le for principal du tiers ménage n'a pas été fermé", menageCommun.getForFiscalPrincipalAt(null));
		final RapportEntreTiers rapportDivorces = pierre.getRapportsSujet().toArray(new RapportEntreTiers[0])[0];
		assertEquals("Le ménage du veuf aurait dû être fermé", DATE_VEUVAGE, rapportDivorces.getDateFin());
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur le veuf
		 */
		assertEquals(2, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(pierre).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}

	@Test
	public void testVeuvageNonSuisse() {
		
		LOGGER.debug("Test de traitement d'un événement de veuvage d'un non suisse marié seul.");
		
		Individu veufEtranger = serviceCivil.getIndividu(NO_INDIVIDU_VEUF_ETRANGER, 2008);
		Veuvage veuvage = createVeuvage(veufEtranger);
		
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
	
		veuvage.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du traitement de veuvage", erreurs);
		veuvage.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du traitement de veuvage", erreurs);
		veuvage.handle(warnings);
		assertEmpty("Une erreur est survenue lors du traitement de veuvage", erreurs);
		
		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_VEUF_ETRANGER);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", pierre);

		/*
		 * Ses for principaux actifs doivent avoir été ouverts
		 */
		ForFiscalPrincipal ffp = pierre.getForFiscalPrincipalAt(null);
		assertNotNull("Le for principal du veuf n'a pas été ouvert", ffp);
		assertEquals("Le mode d'imposition du veuf devrait être DEPENSE", ModeImposition.DEPENSE, ffp.getModeImposition());
		assertEquals("Date d'ouverture de for incorrecte", DATE_VEUVAGE.getOneDayAfter(), ffp.getDateDebut());
		
		/*
		 * Test de récupération du tiers menageCommun
		 */
		Contribuable menageCommun = tiersDAO.getContribuableByNumero(NO_MENAGE_COMMUN_INDIVIDU_VEUF_ETRANGER);
		assertNotNull("Le tiers correspondant au ménage commun n'a pas été trouvé", menageCommun);
		
		/*
		 * Test sur le menage commun, il ne doit plus rester de for principal ouvert
		 */
		assertNull("Le for principal du tiers ménage n'a pas été fermé", menageCommun.getForFiscalPrincipalAt(null));
		final RapportEntreTiers rapportDivorces = pierre.getRapportsSujet().toArray(new RapportEntreTiers[0])[0];
		assertEquals("Le ménage du veuf aurait dû être fermé", DATE_VEUVAGE, rapportDivorces.getDateFin());
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur le veuf
		 */
		assertEquals(2, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(pierre).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}
	
	@Test
	public void testVeuvageMarieAvecHabitant() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement de veuvage d'un habitant marié.");
		
		Individu veufMarie = serviceCivil.getIndividu(NO_INDIVIDU_VEUF_MARIE, 2008);
		Veuvage veuvage = createVeuvage(veufMarie);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		veuvage.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du traitement de veuvage", erreurs);
		veuvage.validate(erreurs, warnings);
		assertTrue("Le validate doit échouer car l'individu est marié", erreurs.size() > 0);
		
	}
	
	@Test
	public void testVeuvageMarieSeulAvecForPosterieur() {
		
		LOGGER.debug("Test de traitement d'un événement de veuvage d'un habitant marié seul ayant un for fiscal principal ouvert après la date de veuvage.");
		
		Individu veuf = serviceCivil.getIndividu(NO_INDIVIDU_VEUF_AVEC_FOR, 2008);
		Veuvage veuvage = createVeuvage(veuf);
		
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		veuvage.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du traitement de veuvage", erreurs);
		veuvage.validate(erreurs, warnings);
		assertTrue("Le validate doit échouer car l'individu possède un for principal ouvert après la date de veuvage.", erreurs.size() > 0);
	}
	
	protected Veuvage createVeuvage(Individu individu) {
		return new Veuvage(individu, null, DATE_VEUVAGE, 5652, context);
	}
}

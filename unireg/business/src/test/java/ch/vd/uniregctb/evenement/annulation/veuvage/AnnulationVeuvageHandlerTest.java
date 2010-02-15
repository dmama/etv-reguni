package ch.vd.uniregctb.evenement.annulation.veuvage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class AnnulationVeuvageHandlerTest extends AbstractEvenementHandlerTest {

	private static final Log LOGGER = LogFactory.getLog(AnnulationVeuvageHandlerTest.class);

	/**
	 * Le numero d'individu du marié seul.
	 */
	private static final long NO_INDIVIDU_MARIE_SEUL = 92647;

	/**
	 * Le numero d'individu du marié seul.
	 */
	private static final long NO_INDIVIDU_NON_VEUF = 54321;

	/**
	 * La date de veuvage.
	 */
	private static final RegDate DATE_VEUVAGE = RegDate.get(2008, 1, 1);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationVeuvageHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {

		final RegDate dateMariageAndre = RegDate.get(1982, 12, 4);

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				super.init();

				MockIndividu andre = addIndividu(NO_INDIVIDU_MARIE_SEUL, RegDate.get(1956, 2, 25), "Girard", "André", true);
				addDefaultAdressesTo(andre);
				marieIndividu(andre, dateMariageAndre);
				addOrigine(andre, MockPays.France, null, andre.getDateNaissance());
				addNationalite(andre, MockPays.France, andre.getDateNaissance(), null, 0);
				addPermis(andre, EnumTypePermis.FRONTALIER, RegDate.get(2008, 9, 8), null, 0, false);
			}
		});

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testAnnulationVeuvageMarieSeul() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de veuvage d'un marié seul.");

		Individu veuf = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_SEUL, 2008);
		AnnulationVeuvage annulation = createValidAnnulationVeuvage(veuf);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de veuvage", erreurs);

		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de veuvage", erreurs);

		evenementCivilHandler.handle(annulation, warnings);

		PersonnePhysique andre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_SEUL);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", andre);

		// Vérification des fors fiscaux
		assertNull("André ne doit pas avoir de for principal actif après l'annulation de veuvage", andre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : andre.getForsFiscaux()) {
			if (forFiscal.getDateFin() == null && DATE_VEUVAGE.getOneDayAfter().equals(forFiscal.getDateDebut())) {
				assertEquals("Les fors fiscaux créés lors du veuvage doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		// Vérification de la présence d'un tiers MenageCommun
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : andre.getRapportsSujet() ) {
			if ( rapport.getType().equals( TypeRapportEntreTiers.APPARTENANCE_MENAGE ) ) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) rapport.getObjet();
			}
		}
		assertEquals("Il aurait dû y avoir 2 rapports entre tiers: 1 annulé et 1 rouvert", 2, nbMenagesCommuns);

		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",
				forCommun);
		assertNull("Le for fiscal principal précédent devrait être rouvert (date null)",
				forCommun.getDateFin());
		assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)",
				forCommun.getMotifFermeture());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal du ménage André
		 */
		assertEquals(1, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}

	@Test
	public void testAnnulationVeuvageNonVeuf() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de veuvage d'une personne non veuve.");

		Individu fauxVeuf = serviceCivil.getIndividu(NO_INDIVIDU_NON_VEUF, 2008);
		AnnulationVeuvage annulation = createValidAnnulationVeuvage(fauxVeuf);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de veuvage", erreurs);

		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEquals("Une erreur aurait dû se produire lors du validate de l'annulation.", 1, erreurs.size());
	}

	private AnnulationVeuvage createValidAnnulationVeuvage(Individu individu) {

		MockAnnulationVeuvage annulation = new MockAnnulationVeuvage();
		annulation.setIndividu(individu);
		annulation.setNumeroOfsCommuneAnnonce(5652);
		annulation.setDate(DATE_VEUVAGE);
		return annulation;
	}

}

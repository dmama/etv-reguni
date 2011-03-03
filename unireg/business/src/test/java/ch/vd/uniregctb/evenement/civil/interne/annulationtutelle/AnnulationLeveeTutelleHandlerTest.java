package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.type.TypeTutelle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnnulationLeveeTutelleHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(AnnulationLeveeTutelleHandlerTest.class);

	final static private RegDate DATE_ANNULATION_TUTELLE = RegDate.get(2008, 11, 07);

	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR = 54321L;
	final static private long NO_INDIVIDU_TUTEUR = 12345L;

	final static private long NO_INDIVIDU_PUPILLE_AVEC_ERREUR = 89123;
	final static private long NO_INDIVIDU_TUTEUR_AVEC_ERREUR = 78912;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationLeveeTutelleHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {

			@Override
			protected void init() {
				MockIndividu momo = addIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				MockIndividu pierre = addIndividu(NO_INDIVIDU_TUTEUR, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				MockIndividu helene = addIndividu(NO_INDIVIDU_PUPILLE_AVEC_ERREUR, RegDate.get(1963, 8, 20), "Duval", "Hélène", false);
				MockIndividu leon = addIndividu(NO_INDIVIDU_TUTEUR_AVEC_ERREUR, RegDate.get(1953, 11, 2), "Dupont", "Léon", true);

				/* Adresses */
				addDefaultAdressesTo(momo);
				addDefaultAdressesTo(pierre);
				addDefaultAdressesTo(helene);
				addDefaultAdressesTo(leon);

				/* tutelles */
				setTutelle(momo, pierre, null, TypeTutelle.CONSEIL_LEGAL);
				setTutelle(leon, helene, null, TypeTutelle.TUTELLE);
			}
		});

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testAnnulationLeveeTutelleAvecTuteur() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'annulation de levée de tutelle avec un tuteur.");

		Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, 2008);
		AnnulationLeveeTutelleAdapter annulationLeveeTutelle = createAnnulationLeveeTutelle(pupille);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		annulationLeveeTutelle.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de levée de tutelle.", erreurs);

		annulationLeveeTutelle.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de levée de tutelle.", erreurs);

		annulationLeveeTutelle.handle(warnings);

		// Récupération du tiers pupille
		PersonnePhysique tiersPupille = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR);
		Assert.notNull(tiersPupille, "Impossible de récupérer l'habitant correspondant au pupille");

		// Récupération du tiers tuteur
		PersonnePhysique tiersTuteur = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_TUTEUR);
		Assert.notNull(tiersTuteur, "Impossible de récupérer l'habitant correspondant au tuteur");

		// Test du rapport entre tiers depuis le pupille
		Set<RapportEntreTiers> rapports = tiersPupille.getRapportsSujet();
		Assert.isTrue(rapports.size() == 2, "les rapports de tutelle entre le pupille et le tuteur n'existent pas");
		int nombreRapportOuverts = 0;
		for (RapportEntreTiers rapport : rapports) {
			Assert.isTrue(rapport.getType() == TypeRapportEntreTiers.TUTELLE, "Le rapport n'est pas de type tutelle");
			Assert.isTrue(rapport.getObjetId().equals(tiersTuteur.getId()), "Le rapport n'est pas avec le tuteur");
			if (rapport.getDateFin() == null &&
					RegDateHelper.isBetween(DATE_ANNULATION_TUTELLE, rapport.getDateDebut(), rapport.getDateFin(), null)) {
				nombreRapportOuverts++;
			}
		}
		assertEquals("La levée de tutelle n'a pas pu être annulée", 1, nombreRapportOuverts);
	}

	@Test
	public void testAnnulationLeveeTutelleAvecErreur() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de levée de tutelle avec des données erronées.");

		Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_ERREUR, 2008);
		AnnulationLeveeTutelleAdapter annulationLeveeTutelle = createAnnulationLeveeTutelle(pupille);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		annulationLeveeTutelle.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de levée de tutelle.", erreurs);

		annulationLeveeTutelle.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de levée de tutelle.", erreurs);

		boolean errorFound = false;
		try {
			annulationLeveeTutelle.handle(warnings);
		}
		catch (EvenementCivilHandlerException eche) {
			errorFound = true;
		}
		assertTrue("Une erreur aurait dû se produire", errorFound);
	}

	private AnnulationLeveeTutelleAdapter createAnnulationLeveeTutelle(Individu pupille) {
		return new AnnulationLeveeTutelleAdapter(pupille, null, DATE_ANNULATION_TUTELLE, 4848, context);
	}
}

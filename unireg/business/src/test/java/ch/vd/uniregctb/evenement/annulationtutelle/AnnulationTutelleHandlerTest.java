package ch.vd.uniregctb.evenement.annulationtutelle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.type.TypeTutelle;

/**
 * Test du handler d'annulation de tutelle:
 * ----------------------------------------
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationTutelleHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(AnnulationTutelleHandlerTest.class);

	final static private RegDate DATE_ANNULATION_TUTELLE = RegDate.get(2008, 11, 07);

	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR = 54321L;
	final static private long NO_INDIVIDU_TUTEUR = 12345L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationTutelleHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {

			@Override
			protected void init() {
				MockIndividu momo = addIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				MockIndividu pierre = addIndividu(NO_INDIVIDU_TUTEUR, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				/* Adresses */
				addDefaultAdressesTo(momo);
				addDefaultAdressesTo(pierre);

				/* tutelles */
				setTutelle(momo, pierre, null, TypeTutelle.CONSEIL_LEGAL);
			}
		});

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testHandlerAvecTuteur() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de tutelle avec un tuteur.");

		Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, 2008);
		AnnulationTutelle annulationTutelle = createAnnulationTutelle(pupille);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		annulationTutelle.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de tutelle.", erreurs);

		annulationTutelle.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de tutelle.", erreurs);

		annulationTutelle.handle(warnings);

		// Récupération du tiers pupille
		PersonnePhysique tiersPupille = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR);
		Assert.notNull(tiersPupille, "Impossible de récupérer l'habitant correspondant au pupille");

		// Récupération du tiers tuteur
		PersonnePhysique tiersTuteur = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_TUTEUR);
		Assert.notNull(tiersTuteur, "Impossible de récupérer l'habitant correspondant au tuteur");

		// Test du rapport entre tiers depuis le pupille
		Set<RapportEntreTiers> rapports = tiersPupille.getRapportsSujet();
		Assert.isTrue(rapports.size() == 1, "le rapport de tutelle entre le pupille et le tuteur n'existe pas");
		for (RapportEntreTiers rapport : rapports) {
			Assert.isTrue(rapport.getType() == TypeRapportEntreTiers.TUTELLE, "Le rapport n'est pas de type tutelle");
			Assert.isTrue(rapport.getObjetId().equals(tiersTuteur.getId()), "Le rapport n'est pas avec le tuteur");
			Assert.isTrue(rapport.isAnnule(), "La tutelle n'a pas pu être annulée");
		}
	}

	private MockAnnulationTutelle createAnnulationTutelle(Individu pupille) {
		MockAnnulationTutelle annulationTutelle = new MockAnnulationTutelle(pupille, null, DATE_ANNULATION_TUTELLE, 4848);
		annulationTutelle.setHandler(evenementCivilHandler);
		return annulationTutelle;
	}
}

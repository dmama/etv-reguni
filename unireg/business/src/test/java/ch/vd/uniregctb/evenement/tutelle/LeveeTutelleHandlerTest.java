package ch.vd.uniregctb.evenement.tutelle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeTutelle;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Test du handler de levée de tutelle:
 * ------------------------------------
 *
 * @author Pavel BLANCO
 *
 */
public class LeveeTutelleHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(LeveeTutelleHandlerTest.class);

	final static private RegDate DATE_LEVEE_TUTELLE = RegDate.get(2008, 11, 07);

	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR = 54321L;
	final static private long NO_INDIVIDU_TUTEUR = 12345L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "LeveeTutelleHandlerTest.xml";


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
				setTutelle(momo, pierre, EnumTypeTutelle.CONSEIL_LEGAL);
			}
		});

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testLeveeTutelleAvecTuteur() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de levée de tutelle avec un tuteur.");

		Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, 2008);
		LeveeTutelle leveeTutelle = createLeveeTutelle(pupille);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(leveeTutelle, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la levée de tutelle.", erreurs);

		evenementCivilHandler.validate(leveeTutelle, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de la levée de tutelle.", erreurs);

		evenementCivilHandler.handle(leveeTutelle, warnings);

		/*
		 * Récupération du tiers pupille
		 */
		PersonnePhysique tiersPupille = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR);
		Assert.notNull(tiersPupille, "Impossible de récupérer l'habitant correspondant au pupille");

		/*
		 * Récupération du tiers tuteur
		 */
		PersonnePhysique tiersTuteur = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_TUTEUR);
		Assert.notNull(tiersTuteur, "Impossible de récupérer l'habitant correspondant au tuteur");

		/*
		 * Test du rapport entre tiers depuis le pupille
		 */
		Set<RapportEntreTiers> rapports = tiersPupille.getRapportsSujet();
		Assert.isTrue(rapports.size() == 1, "le rapport de tutelle entre le pupille et le tuteur n'a pas été créé");
		for (RapportEntreTiers rapport : rapports) {
			Assert.isTrue(rapport.getType().equals(TypeRapportEntreTiers.TUTELLE), "Le rapport n'est pas de type tutelle");
			Assert.isTrue(rapport.getObjet().equals(tiersTuteur), "Le rapport n'est pas avec le tuteur");
			Assert.isTrue(DATE_LEVEE_TUTELLE.equals(rapport.getDateFin()), "La date de levée de tutelle n'est pas valide");
		}

	}

	private MockLeveeTutelle createLeveeTutelle(Individu pupille) {
		MockLeveeTutelle leveeTutelle = new MockLeveeTutelle();
		leveeTutelle.setIndividu(pupille);
		leveeTutelle.setNumeroOfsCommuneAnnonce(4848);
		leveeTutelle.setDate(DATE_LEVEE_TUTELLE);
		return leveeTutelle;
	}

}

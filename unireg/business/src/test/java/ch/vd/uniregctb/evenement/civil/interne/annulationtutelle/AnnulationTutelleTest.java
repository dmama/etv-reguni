package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
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
public class AnnulationTutelleTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(AnnulationTutelleTest.class);

	final static private RegDate DATE_ANNULATION_TUTELLE = RegDate.get(2008, 11, 7);

	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR = 54321L;
	final static private long NO_INDIVIDU_TUTEUR = 12345L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationTutelleTest.xml";

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
	@Transactional(rollbackFor = Throwable.class)
	public void testHandlerAvecTuteur() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement d'annulation de tutelle avec un tuteur.");

		Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, date(2008, 12, 31));
		AnnulationTutelle annulationTutelle = createAnnulationTutelle(pupille);

		final MessageCollector collector = buildMessageCollector();
		annulationTutelle.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de tutelle.", collector.getErreurs());

		annulationTutelle.handle(collector);

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

	private AnnulationTutelle createAnnulationTutelle(Individu pupille) {
		return new AnnulationTutelle(pupille, null, DATE_ANNULATION_TUTELLE, 4848, context);
	}
}

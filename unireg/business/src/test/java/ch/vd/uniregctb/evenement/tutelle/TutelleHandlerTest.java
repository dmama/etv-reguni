package ch.vd.uniregctb.evenement.tutelle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeTutelle;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockTuteurGeneral;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Test unitaire du handler d'une mise sous tutelle.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 *
 */
public class TutelleHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(TutelleHandlerTest.class);

	final static private RegDate DATE_TUTELLE = RegDate.get(2008, 01, 01);

	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR = 54321L;
	final static private long NO_INDIVIDU_TUTEUR = 12345L;
	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL = 45678L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "TutelleHandlerTest.xml";


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				MockIndividu pierre = addIndividu(NO_INDIVIDU_TUTEUR, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu david = addIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL, RegDate.get(1964, 1, 23), "Dagobert",
						"David", true);
				MockIndividu julien = addIndividu(56789, RegDate.get(1966, 11, 2), "Martin", "Julien", true);

				/* Adresses */
				addDefaultAdressesTo(momo);
				addDefaultAdressesTo(pierre);
				addDefaultAdressesTo(david);
				addDefaultAdressesTo(julien);

				/* tutelles */
				setTutelle(momo, pierre, EnumTypeTutelle.CONSEIL_LEGAL);
				setTutelle(david, EnumTypeTutelle.TUTELLE);
			}
		});
	}

	/**
	 * Test du traitement de mise en tutelle d'un individu avec un autre individu.
	 *
	 * @throws Exception
	 */
	@Test
	public void testHandleTutelleAvecTuteur() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de mise sous tutelle avec un tuteur.");

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, 2007);
				Individu tuteur = serviceCivil.getIndividu(NO_INDIVIDU_TUTEUR, 2007);
				Tutelle tutelle = createTutelle(pupille, tuteur, null);

				List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				evenementCivilHandler.checkCompleteness(tutelle, erreurs, warnings);
				evenementCivilHandler.validate(tutelle, erreurs, warnings);
				evenementCivilHandler.handle(tutelle, warnings);
				Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement de la mise sous tutelle");
				return null;
			}
		});

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
			Assert.isTrue(rapport.getType().equals(TypeRapportEntreTiers.TUTELLE), "Le rapport créé n'est pas de type tutelle");
			Assert.isTrue(rapport.getObjetId().equals(tiersTuteur.getId()), "Le rapport créé ne l'a pas été avec le tuteur");
		}

		/*
		 * Test du rapport entre tiers depuis le tuteur
		 */
		rapports = tiersTuteur.getRapportsObjet();
		Assert.isTrue(rapports.size() == 1, "le rapport de tutelle entre le tuteur et le pupille n'a pas été créé");
		for (RapportEntreTiers rapport : rapports) {
			Assert.isTrue(rapport.getType().equals(TypeRapportEntreTiers.TUTELLE), "Le rapport créé n'est pas de type tutelle");
			Assert.isTrue(rapport.getSujetId().equals(tiersPupille.getId()), "Le rapport créé ne l'a pas été avec le pupille");
		}
	}

	/**
	 * Test du traitement de mise en tutelle d'un individu avec l'office du tuteur general.
	 *
	 * @throws Exception
	 */
	@Test
	public void testHandleTutelleAvecTuteurGeneral() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de mise sous tutelle avec l'OTG.");

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL, 2007);
				Tutelle tutelle = createTutelle(pupille, null, new MockTuteurGeneral());
				List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				evenementCivilHandler.checkCompleteness(tutelle, erreurs, warnings);
				evenementCivilHandler.validate(tutelle, erreurs, warnings);
				evenementCivilHandler.handle(tutelle, warnings);

				Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement de la mise sous tutelle");
				return null;
			}
		});

		/*
		 * Récupération du tiers pupille
		 */
		PersonnePhysique tiersPupille = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL);
		Assert.notNull(tiersPupille, "Impossible de récupérer l'habitant correspondant au pupille");

		/*
		 * Récupération du tiers tuteur
		 */
		CollectiviteAdministrative tiersOTG = tiersDAO
				.getCollectiviteAdministrativesByNumeroTechnique(ServiceInfrastructureService.noTuteurGeneral);
		Assert.notNull(tiersOTG, "Impossible de récupérer le tiers correspondant a l'OTG");

		/*
		 * Test du rapport entre tiers depuis le pupille
		 */
		Set<RapportEntreTiers> rapports = tiersPupille.getRapportsSujet();
		Assert.isTrue(rapports.size() == 1, "le rapport de tutelle entre le pupille et le tuteur n'a pas été créé");
		for (RapportEntreTiers rapport : rapports) {
			Assert.isTrue(rapport.getType().equals(TypeRapportEntreTiers.TUTELLE), "Le rapport créé n'est pas de type tutelle");
			Assert.isTrue(rapport.getObjetId().equals(tiersOTG.getId()), "Le rapport créé ne l'a pas été avec le tuteur");
		}

		/*
		 * Test du rapport entre tiers depuis le tuteur
		 */
		rapports = tiersOTG.getRapportsObjet();
		Assert.isTrue(rapports.size() == 1, "le rapport de tutelle entre le tuteur et le pupille n'a pas été créé");
		for (RapportEntreTiers rapport : rapports) {
			Assert.isTrue(rapport.getType().equals(TypeRapportEntreTiers.TUTELLE), "Le rapport créé n'est pas de type tutelle");
			Assert.isTrue(rapport.getSujetId().equals(tiersPupille.getId()), "Le rapport créé ne l'a pas été avec le pupille");
		}
	}

	private MockTutelle createTutelle(Individu pupille, Individu tuteur, TuteurGeneral tuteurGeneral) {
		MockTutelle tutelle = new MockTutelle();
		tutelle.setIndividu(pupille);
		tutelle.setNumeroOfsCommuneAnnonce(4848);
		tutelle.setDate(DATE_TUTELLE);
		tutelle.setTypeTutelle(TypeTutelle.TUTELLE);
		tutelle.setTuteur(tuteur);
		tutelle.setTuteurGeneral(tuteurGeneral);
		tutelle.init(tiersDAO);
		return tutelle;
	}

}

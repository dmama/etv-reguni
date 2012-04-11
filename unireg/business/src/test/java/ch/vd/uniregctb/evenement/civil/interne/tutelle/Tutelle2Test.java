package ch.vd.uniregctb.evenement.civil.interne.tutelle;

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockTuteurGeneral;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.type.TypeTutelle;

/**
 * Test unitaire du handler d'une mise sous tutelle.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 *
 */
public class Tutelle2Test extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(Tutelle2Test.class);

	final static private RegDate DATE_TUTELLE = RegDate.get(2008, 1, 1);

	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR = 54321L;
	final static private long NO_INDIVIDU_TUTEUR = 12345L;
	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL = 45678L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "Tutelle2Test.xml";


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
				setTutelle(momo, pierre, null, TypeTutelle.CONSEIL_LEGAL);
				setTutelle(david, null, TypeTutelle.TUTELLE);
			}
		});
	}

	/**
	 * Test du traitement de mise en tutelle d'un individu avec un autre individu.
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleTutelleAvecTuteur() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de mise sous tutelle avec un tuteur.");

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, date(2007, 12, 31));
				Individu tuteur = serviceCivil.getIndividu(NO_INDIVIDU_TUTEUR, date(2007, 12, 31));
				Tutelle tutelle = createTutelle(pupille, tuteur, null, MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud);

				final MessageCollector collector = buildMessageCollector();
				tutelle.validate(collector, collector);
				tutelle.handle(collector);
				Assert.assertFalse("Une erreur est survenue lors du traitement de la mise sous tutelle", collector.hasErreurs());
				return null;
			}
		});

		/*
		 * Récupération du tiers pupille
		 */
		PersonnePhysique tiersPupille = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR);
		Assert.assertNotNull("Impossible de récupérer l'habitant correspondant au pupille", tiersPupille);

		/*
		 * Récupération du tiers tuteur
		 */
		PersonnePhysique tiersTuteur = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_TUTEUR);
		Assert.assertNotNull("Impossible de récupérer l'habitant correspondant au tuteur", tiersTuteur);

		/*
		 * Test du rapport entre tiers depuis le pupille
		 */
		Set<RapportEntreTiers> rapports = tiersPupille.getRapportsSujet();
		Assert.assertEquals("le rapport de tutelle entre le pupille et le tuteur n'a pas été créé", 1, rapports.size());
		for (RapportEntreTiers rapport : rapports) {
			Assert.assertEquals("Le rapport créé n'est pas de type tutelle", TypeRapportEntreTiers.TUTELLE, rapport.getType());
			Assert.assertEquals("Le rapport créé ne l'a pas été avec le tuteur", tiersTuteur.getId(), rapport.getObjetId());

			final RepresentationLegale tutelle = (RepresentationLegale) rapport;
			final CollectiviteAdministrative justiceDePaix = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNoColAdm());
			Assert.assertNotNull(justiceDePaix);
			Assert.assertEquals("L'autorité tutélaire n'a pas été associée à la tutelle", justiceDePaix.getId(), tutelle.getAutoriteTutelaireId());
		}

		/*
		 * Test du rapport entre tiers depuis le tuteur
		 */
		rapports = tiersTuteur.getRapportsObjet();
		Assert.assertEquals("le rapport de tutelle entre le tuteur et le pupille n'a pas été créé", 1, rapports.size());
		for (RapportEntreTiers rapport : rapports) {
			Assert.assertEquals("Le rapport créé n'est pas de type tutelle", TypeRapportEntreTiers.TUTELLE, rapport.getType());
			Assert.assertEquals("Le rapport créé ne l'a pas été avec le pupille", tiersPupille.getId(), rapport.getSujetId());

			final RepresentationLegale tutelle = (RepresentationLegale) rapport;
			final CollectiviteAdministrative justiceDePaix = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNoColAdm());
			Assert.assertNotNull(justiceDePaix);
			Assert.assertEquals("L'autorité tutélaire n'a pas été associée à la tutelle", justiceDePaix.getId(), tutelle.getAutoriteTutelaireId());
		}
	}

	/**
	 * Test du traitement de mise en tutelle d'un individu avec l'office du tuteur general.
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleTutelleAvecTuteurGeneral() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de mise sous tutelle avec l'OTG.");

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu pupille = serviceCivil.getIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL, date(2007, 12, 31));
				Tutelle tutelle = createTutelle(pupille, null, new MockTuteurGeneral(), MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud);

				final MessageCollector collector = buildMessageCollector();
				tutelle.validate(collector, collector);
				tutelle.handle(collector);

				Assert.assertFalse("Une erreur est survenue lors du traitement de la mise sous tutelle", collector.hasErreurs());
				return null;
			}
		});

		/*
		 * Récupération du tiers pupille
		 */
		PersonnePhysique tiersPupille = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL);
		Assert.assertNotNull("Impossible de récupérer l'habitant correspondant au pupille", tiersPupille);

		/*
		 * Récupération du tiers tuteur
		 */
		CollectiviteAdministrative tiersOTG = tiersDAO
				.getCollectiviteAdministrativesByNumeroTechnique(ServiceInfrastructureService.noTuteurGeneral);
		Assert.assertNotNull("Impossible de récupérer le tiers correspondant a l'OTG", tiersOTG);

		/*
		 * Test du rapport entre tiers depuis le pupille
		 */
		Set<RapportEntreTiers> rapports = tiersPupille.getRapportsSujet();
		Assert.assertEquals("le rapport de tutelle entre le pupille et le tuteur n'a pas été créé", 1, rapports.size());
		for (RapportEntreTiers rapport : rapports) {
			Assert.assertEquals("Le rapport créé n'est pas de type tutelle", TypeRapportEntreTiers.TUTELLE, rapport.getType());
			Assert.assertEquals("Le rapport créé ne l'a pas été avec le tuteur", tiersOTG.getId(), rapport.getObjetId());

			final RepresentationLegale tutelle = (RepresentationLegale) rapport;
			final CollectiviteAdministrative justiceDePaix = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNoColAdm());
			Assert.assertNotNull(justiceDePaix);
			Assert.assertEquals("L'autorité tutélaire n'a pas été associée à la tutelle", justiceDePaix.getId(), tutelle.getAutoriteTutelaireId());
		}

		/*
		 * Test du rapport entre tiers depuis le tuteur
		 */
		rapports = tiersOTG.getRapportsObjet();
		Assert.assertEquals("le rapport de tutelle entre le tuteur et le pupille n'a pas été créé", 1, rapports.size());
		for (RapportEntreTiers rapport : rapports) {
			Assert.assertEquals("Le rapport créé n'est pas de type tutelle", TypeRapportEntreTiers.TUTELLE, rapport.getType());
			Assert.assertEquals("Le rapport créé ne l'a pas été avec le pupille", tiersPupille.getId(), rapport.getSujetId());

			final RepresentationLegale tutelle = (RepresentationLegale) rapport;
			final CollectiviteAdministrative justiceDePaix = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNoColAdm());
			Assert.assertNotNull(justiceDePaix);
			Assert.assertEquals("L'autorité tutélaire n'a pas été associée à la tutelle", justiceDePaix.getId(), tutelle.getAutoriteTutelaireId());
		}
	}

	private Tutelle createTutelle(Individu pupille, Individu tuteur, TuteurGeneral tuteurGeneral, ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative autoriteTutelaire) {
		return new Tutelle(pupille, null, DATE_TUTELLE, 4848, tuteur, tuteurGeneral, TypeTutelle.TUTELLE, autoriteTutelaire, context);
	}

}

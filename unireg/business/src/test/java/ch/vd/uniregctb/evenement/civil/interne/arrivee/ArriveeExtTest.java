package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class ArriveeExtTest extends AbstractEvenementCivilInterneTest {

	public ArriveeExtTest() {
		setWantIndexation(true);
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCompletenessIndividuSeul() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		/*
		 * Construit un événement d'arrivée minimal et valide
		 */
		ArriveePrincipale arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), null, MockCommune.Cossonay, anciennesAdresses.principale,
				                                          nouvellesAdresses.principale, context);

		/*
		 * Vérification de l'intégrité de l'événement minimal
		 */
		final MessageCollector collector = buildMessageCollector();
		arrivee.checkCompleteness(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		/*
		 * Détection d'erreurs pour les différents cas d'événements invalides.
		 */
		ArriveePrincipale sansIndividu = new ArriveePrincipale(null, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), null, MockCommune.Cossonay, anciennesAdresses.principale,
															   nouvellesAdresses.principale, context);
		sansIndividu.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		ArriveePrincipale sansNouvelleAdressePrincipal = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), null, MockCommune.Cossonay, anciennesAdresses.principale,
						null, context);
		sansNouvelleAdressePrincipal.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		ArriveePrincipale sansNouvelleCommunePrincipal = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, 0, null, null, anciennesAdresses.principale, nouvellesAdresses.principale, context);
		sansNouvelleCommunePrincipal.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCompletenessIndividuMarieSeul() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				// marie l'individu, mais seul
				marieIndividu(pierre, RegDate.get(1985, 7, 11));
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		/*
		 * Construit un événement d'arrivée minimal et valide
		 */
		ArriveePrincipale arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), null, MockCommune.Cossonay, null, nouvellesAdresses.principale, context);

		/*
		 * Vérification de l'intégrité de l'événement minimal
		 */
		final MessageCollector collector = buildMessageCollector();
		arrivee.checkCompleteness(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		/*
		 * Détection d'erreurs pour les différents cas d'événements invalides.
		 */
		ArriveePrincipale sansIndividu = new ArriveePrincipale(null, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), null, MockCommune.Cossonay, null, nouvellesAdresses.principale, context);
		sansIndividu.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		ArriveePrincipale sansNouvelleAdressePrincipal = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), null, MockCommune.Cossonay, null,
				null, context);
		sansNouvelleAdressePrincipal.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		ArriveePrincipale sansNouvelleCommunePrincipal = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, 0, null, null, null, nouvellesAdresses.principale, context);
		sansNouvelleCommunePrincipal.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCompletenessIndividuMarie() throws Exception {

		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final long noIndividuHorsCouple = 3;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1977, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.LesClees.ChampDuRaffour, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.LesClees.ChampDuRaffour, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.LesClees.ChampDuRaffour, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.LesClees.ChampDuRaffour, null, RegDate.get(1980,
						1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 7, 11));

				// individu hors-couple
				MockIndividu sophie = addIndividu(noIndividuHorsCouple, RegDate.get(1964, 4, 8), "Dupuis", "Sophie", false);
				addAdresse(sophie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1982, 3, 2), null);
				addAdresse(sophie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1982, 3, 2), null);
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee);
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee);
		final Individu individuHorsCouple = serviceCivil.getIndividu(noIndividuHorsCouple, dateArrivee);
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee, false));

		/*
		 * Construit un événement d'arrivée minimal et valide
		 */
		ArriveePrincipale arrivee = new ArriveePrincipale(individuPrincipal, individuConjoint, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Lausanne.getNoOFS(), null, MockCommune.Lausanne, null,
														  nouvellesAdresses.principale, context);

		/*
		 * Vérification de l'intégrité de l'événement minimal
		 */
		final MessageCollector collector = buildMessageCollector();
		arrivee.checkCompleteness(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		/*
		 * Détection d'erreurs pour les différents cas d'événements invalides.
		 */
		ArriveePrincipale sansIndividu = new ArriveePrincipale(null, individuConjoint, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Lausanne.getNoOFS(), null, MockCommune.Lausanne, null,
															   nouvellesAdresses.principale, context);
		sansIndividu.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		ArriveePrincipale sansConjoint = new ArriveePrincipale(individuPrincipal, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Lausanne.getNoOFS(), null, MockCommune.Lausanne, null,
				nouvellesAdresses.principale, context);
		sansConjoint.checkCompleteness(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		ArriveePrincipale sansNouvelleAdressePrincipal =
				new ArriveePrincipale(individuPrincipal, individuConjoint, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Lausanne.getNoOFS(), null, MockCommune.Lausanne, null,
						null, context);
		sansNouvelleAdressePrincipal.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		ArriveePrincipale sansNouvelleCommunePrincipal =
				new ArriveePrincipale(individuPrincipal, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, 0, null, null, null, nouvellesAdresses.principale, context);
		sansNouvelleCommunePrincipal.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		ArriveePrincipale avecConjointHorsCouple =
				new ArriveePrincipale(individuPrincipal, individuHorsCouple, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Lausanne.getNoOFS(), null, MockCommune.Lausanne, null,
						nouvellesAdresses.principale, context);
		avecConjointHorsCouple.checkCompleteness(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDateDemenagement() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.Suisse, date(1953, 11, 2), null);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		final MessageCollector collector = buildMessageCollector();

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			tiersDAO.save(habitant);
		}
		hibernateTemplate.flush();

		/*
		 * Ok : événement d'arrivée à date courante
		 */
		ArriveePrincipale arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne, MockCommune.Cossonay,
				anciennesAdresses.principale, nouvellesAdresses.principale, context);

		arrivee.validate(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();

		/*
		 * Ok : événement d'arrivée rétro-actif sans information d'ancienne adresse
		 */
		ArriveePrincipale arriveeRetroActiveDeNullePart =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), null, MockCommune.Cossonay, null, nouvellesAdresses.principale, context);

		arriveeRetroActiveDeNullePart.validate(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		arriveeRetroActiveDeNullePart.handle(collector);
		assertFalse(collector.hasErreurs());
		assertEquals(1, collector.getWarnings().size());
		collector.clear();

		/*
		 * Erreur: évenement d'arrivée rétro-actif situé avant la date de début de validité de l'ancienne adresse
		 */
		ArriveePrincipale arriveeRetroActive =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, RegDate.get(1945, 1, 1), MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne, MockCommune.Cossonay,
						anciennesAdresses.principale, nouvellesAdresses.principale, context);

		arriveeRetroActive.validate(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();
	}

	/**
	 * Teste qu'une arrivée hors canton génère bien une exception.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateAdressePrincipaleHorsCanton() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel, dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel, dateArrivee, null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		final MessageCollector collector = buildMessageCollector();

		/*
		 * Erreur: nouvelle adresse hors-canton
		 */
		ArriveePrincipale arriveeHorsCanton =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Neuchatel.getNoOFS(), MockCommune.Lausanne, MockCommune.Neuchatel,
						anciennesAdresses.principale, nouvellesAdresses.principale, context);

		arriveeHorsCanton.validate(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();
	}

	/**
	 * Teste qu'une arrivée sur une commune composée de fractions (L'Abbaye, Le Chenit et le Lieu) génère bien une exception pour
	 * insuffisance d'information (on a besoin de la fraction précise).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateAdressePrincipaleCommunesComposeesFractions() throws Exception {

		final long noIndividuLAbbaye = 1;
		final long noIndividuLeChenit = 2;
		final long noIndividuLeLieu = 3;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuLAbbaye, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LePont, dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, null, MockLocalite.LePont, dateArrivee, null);

				MockIndividu jacques = addIndividu(noIndividuLeChenit, RegDate.get(1970, 4, 20), "Magnenat", "Jacques", true);

				// adresses avant l'arrivée
				addAdresse(jacques, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LePont, RegDate.get(1988, 3, 7), veilleArrivee);
				addAdresse(jacques, TypeAdresseCivil.COURRIER, null, MockLocalite.LePont, RegDate.get(1988, 3, 7), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(jacques, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeSentier, dateArrivee, null);
				addAdresse(jacques, TypeAdresseCivil.COURRIER, null, MockLocalite.LeSentier, dateArrivee, null);

				MockIndividu jean = addIndividu(noIndividuLeLieu, RegDate.get(1952, 1, 23), "Meylan", "Jean", true);

				// adresses avant l'arrivée
				addAdresse(jean, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeSentier, RegDate.get(1952, 1, 23), veilleArrivee);
				addAdresse(jean, TypeAdresseCivil.COURRIER, null, MockLocalite.LeSentier, RegDate.get(1952, 1, 23), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(jean, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LesCharbonnieres, dateArrivee, null);
				addAdresse(jean, TypeAdresseCivil.COURRIER, null, MockLocalite.LesCharbonnieres, dateArrivee, null);
			}
		});

		// Crée les habitants correspondant
		{
			PersonnePhysique pierre = newHabitant(noIndividuLAbbaye);
			PersonnePhysique jacques = newHabitant(noIndividuLeChenit);
			PersonnePhysique jean = newHabitant(noIndividuLeLieu);
			tiersDAO.save(pierre);
			tiersDAO.save(jacques);
			tiersDAO.save(jean);
		}
		hibernateTemplate.flush();

		final MessageCollector collector = buildMessageCollector();

		/*
		 * Erreur: nouvelle adresse sur la commune composée du Chenit
		 */
		{
			final Individu individu = serviceCivil.getIndividu(noIndividuLAbbaye, dateArrivee);
			final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
			final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

			// erreur: devrait être MockCommune.Fraction.LAbbaye ou ...
			ArriveePrincipale arrivee =
					new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.LeChenit.getNoOFS(), MockCommune.Lausanne, MockCommune.Fraction.LeSentier,
							anciennesAdresses.principale, nouvellesAdresses.principale, context);

			arrivee.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertEquals(1, collector.getWarnings().size());
			assertEquals("arrivée dans la fraction de commune du Sentier: veuillez vérifier la fraction de commune du for principal",
			             collector.getWarnings().get(0).getMessage());
			collector.clear();
		}

		/*
		 * Ok: nouvelle adresse sur la commune du Lieu (qui anciennement était une commune avec fractions mais qui ne l'est plus)
		 */
		{
			final Individu individu = serviceCivil.getIndividu(noIndividuLeLieu, dateArrivee);
			final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
			final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

			// erreur: devrait être MockCommune.Fraction.LeLieu
			ArriveePrincipale arrivee =
					new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.LeLieu.getNoOFS(), MockCommune.Fraction.LeSentier, MockCommune.LeLieu,
							anciennesAdresses.principale, nouvellesAdresses.principale, context);

			arrivee.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());
			collector.clear();
		}
	}

	/**
	 * Teste qu'une arrivée sur les fractions de communes du Sentier et du Lieu génère bien une exception pour traitement manuel.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateAdressePrincipaleFractionsSentierEtLieu() throws Exception {

		final long noIndividuLeSentier = 1;
		final long noIndividuLeLieu = 2;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuLeSentier, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeSentier, dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, null, MockLocalite.LeSentier, dateArrivee, null);

				MockIndividu jean = addIndividu(noIndividuLeLieu, RegDate.get(1952, 1, 23), "Meylan", "Jean", true);

				// adresses avant l'arrivée
				addAdresse(jean, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LePont, RegDate.get(1952, 1, 23), veilleArrivee);
				addAdresse(jean, TypeAdresseCivil.COURRIER, null, MockLocalite.LePont, RegDate.get(1952, 1, 23), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(jean, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.LeLieu, dateArrivee, null);
				addAdresse(jean, TypeAdresseCivil.COURRIER, null, MockLocalite.LeLieu, dateArrivee, null);
			}
		});

		final MessageCollector collector = buildMessageCollector();

		/*
		 * Erreur: nouvelle adresse sur la commune du Chenit
		 */
		{
			final Individu individu = serviceCivil.getIndividu(noIndividuLeSentier, dateArrivee);
			final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
			final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

			Arrivee arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.LeChenit.getNoOFS(), MockCommune.Lausanne, MockCommune.LeChenit,
					anciennesAdresses.principale, nouvellesAdresses.principale, context);

			arrivee.validate(collector, collector);
			assertTrue(collector.hasErreurs());
			assertFalse(collector.hasWarnings());
			collector.clear();
		}

		/*
		 * Erreur: nouvelle adresse sur la commune du Lieu
		 */
		{
			final Individu individu = serviceCivil.getIndividu(noIndividuLeLieu, dateArrivee);
			final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
			final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

			Arrivee arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.LeLieu.getNoOFS(), MockCommune.LAbbaye, MockCommune.LeLieu,
					anciennesAdresses.principale, nouvellesAdresses.principale, context);

			arrivee.validate(collector, collector);
			assertTrue(collector.hasErreurs());
			assertFalse(collector.hasWarnings());
			collector.clear();
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateAdresseSecondaire() throws Exception {

		final RegDate toutDebut = RegDate.get(1980, 2, 2);
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veille = dateArrivee.getOneDayBefore();

		/*
		 * Ok: arrivée en adresse secondaire
		 */
		final Adresse ancienne = MockServiceCivil.newAdresse(TypeAdresseCivil.SECONDAIRE, null, MockLocalite.Neuchatel, toutDebut, veille);
		final Adresse nouvelle = MockServiceCivil.newAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);

		final ArriveeSecondaire arrivee = new ArriveeSecondaire(null, null, dateArrivee, MockCommune.Lausanne.getNoOFS(), null, MockCommune.Lausanne, ancienne, nouvelle, context);
		final MessageCollector collector = buildMessageCollector();
		arrivee.validateArriveeAdresseSecondaire(collector);
		assertTrue(collector.getErreurs().isEmpty());
		collector.clear();

		/*
		 * Erreur: arrivée avant le début de validité de l'adresse principale
		 */
		final ArriveeSecondaire arriveeRetroActive = new ArriveeSecondaire(null, null, RegDate.get(1902, 10, 11), MockCommune.Lausanne.getNoOFS(), null, MockCommune.Lausanne, ancienne, nouvelle, context);
		arriveeRetroActive.validateArriveeAdresseSecondaire(collector);
		assertFalse(collector.getErreurs().isEmpty());
		collector.clear();

		/*
		 * Erreur: arrivée en adresse secondaire hors canton
		 */
		final Adresse nouvelleHorsCanton = MockServiceCivil.newAdresse(TypeAdresseCivil.SECONDAIRE, null, MockLocalite.Neuchatel3Serrieres, dateArrivee, null);
		final ArriveeSecondaire arriveeHorsCanton = new ArriveeSecondaire(null, null, dateArrivee, MockCommune.Neuchatel.getNoOFS(), null, MockCommune.Neuchatel, ancienne, nouvelleHorsCanton, context);
		arriveeHorsCanton.validateArriveeAdresseSecondaire(collector);
		assertFalse(collector.getErreurs().isEmpty());
		collector.clear();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateIndividuSeul() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		final MessageCollector collector = buildMessageCollector();

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);

			tiersDAO.save(habitant);
		}
		hibernateTemplate.flush();

		/*
		 * Ok : événement d'arrivée standard
		 */
		Arrivee arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne, MockCommune.Cossonay,
				anciennesAdresses.principale, nouvellesAdresses.principale, context);

		arrivee.validate(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul suisse depuis une commune hors-canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveePrincipaleIndividuSeulSuisseDeHorsCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				addNationalite(pierre, MockPays.Suisse, RegDate.get(1953, 11, 2), null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		{
			/*
			 * L'événement d'arrivée
			 */
			Arrivee arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Neuchatel, MockCommune.Cossonay,
					anciennesAdresses.principale, nouvellesAdresses.principale, context);

			/*
			 * Vérification que l'individu n'existe pas en base avant son arrivée
			 */
			assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

			/*
			 * Arrivée
			 */
			arrivee.handle(buildMessageCollector());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 1, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			ForFiscalPrincipal ff = habitant.getForFiscalPrincipalAt(null);
			assertNotNull(ff);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), ff.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, ff.getDateDebut());
			assertNull(null, ff.getDateFin());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul suisse depuis une commune du canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveePrincipaleIndividuSeulSuisseDeCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);

			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			assertEquals(habitant, tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

		}
		hibernateTemplate.flush();

		{
			/*
			 * L'événement d'arrivée
			 */
			Arrivee arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne, MockCommune.Cossonay,
					anciennesAdresses.principale, nouvellesAdresses.principale, context);

			/*
			 * Arrivée
			 */
			arrivee.handle(buildMessageCollector());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			List<ForFiscal> list = habitant.getForsFiscauxSorted();
			ForFiscalPrincipal lausanne = (ForFiscalPrincipal) list.get(0);
			assertNotNull(lausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), lausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(RegDate.get(1980, 1, 1), lausanne.getDateDebut());
			assertEquals(veilleArrivee, lausanne.getDateFin());

			ForFiscalPrincipal cossonay = (ForFiscalPrincipal) list.get(1);
			assertNotNull(cossonay);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), cossonay.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, cossonay.getDateDebut());
			assertNull(null, cossonay.getDateFin());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul étranger depuis une commune hors-canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveePrincipaleIndividuSeulEtrangerDeHorsCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.France, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.FRONTALIER, RegDate.get(1963, 8, 20), null, false);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		{
			/*
			 * L'événement d'arrivée
			 */
			Arrivee arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Neuchatel, MockCommune.Cossonay,
					anciennesAdresses.principale, nouvellesAdresses.principale, context);

			/*
			 * Vérification que l'individu n'existe pas en base avant son arrivée
			 */
			assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

			/*
			 * Arrivée
			 */
			arrivee.handle(buildMessageCollector());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 1, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			ForFiscalPrincipal ff = habitant.getForFiscalPrincipalAt(null);
			assertNotNull(ff);
			assertEquals(ModeImposition.SOURCE, ff.getModeImposition());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul réfugié depuis une commune hors-canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveePrincipaleIndividuSeulRefugieDeHorsCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.France, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.ETRANGER_ADMIS_PROVISOIREMENT, RegDate.get(1962, 9, 2), RegDate.get(1963, 1, 14), false);
				addPermis(pierre, TypePermis.REQUERANT_ASILE, RegDate.get(1963, 1, 15), RegDate.get(1963, 8, 19), false);
				addPermis(pierre, TypePermis.SEJOUR, RegDate.get(1963, 8, 20), null, false);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		{
			/*
			 * L'événement d'arrivée
			 */
			Arrivee arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Neuchatel, MockCommune.Cossonay,
					anciennesAdresses.principale, nouvellesAdresses.principale, context);

			/*
			 * Vérification que l'individu n'existe pas en base avant son arrivée
			 */
			assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

			/*
			 * Arrivée
			 */
			arrivee.handle(buildMessageCollector());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 1, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			ForFiscalPrincipal ff = habitant.getForFiscalPrincipalAt(null);
			assertNotNull(ff);
			// [SIFISC-3680] Les réfugiés politiques reconnus sont dorénavant imposés à la source par défaut (et passé manuellement à l'ordinaire par l'ACI si besoin est).
			assertEquals(ModeImposition.SOURCE, ff.getModeImposition());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul étranger depuis une commune du canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveePrincipaleIndividuSeulEtrangerDeCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.France, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.FRONTALIER, RegDate.get(1963, 8, 20), null, false);

			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			f.setModeImposition(ModeImposition.SOURCE);
			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			assertEquals(habitant, tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

		}
		hibernateTemplate.flush();

		{
			/*
			 * L'événement d'arrivée
			 */
			Arrivee arrivee = new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne, MockCommune.Cossonay,
					anciennesAdresses.principale, nouvellesAdresses.principale, context);

			/*
			 * Arrivée
			 */
			arrivee.handle(buildMessageCollector());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			List<ForFiscal> list = habitant.getForsFiscauxSorted();
			ForFiscalPrincipal lausanne = (ForFiscalPrincipal) list.get(0);
			assertNotNull(lausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), lausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(RegDate.get(1980, 1, 1), lausanne.getDateDebut());
			assertEquals(veilleArrivee, lausanne.getDateFin());
			assertEquals(ModeImposition.SOURCE, lausanne.getModeImposition());

			ForFiscalPrincipal cossonay = (ForFiscalPrincipal) list.get(1);
			assertNotNull(cossonay);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), cossonay.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, cossonay.getDateDebut());
			assertNull(null, cossonay.getDateFin());
			assertEquals(ModeImposition.SOURCE, cossonay.getModeImposition());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un couple depuis une commune hors-canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveePrincipaleCoupleDeHorsCanton() throws Exception {
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();
		final RegDate dateMariage = RegDate.get(1985, 7, 11);

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);

				// nationalité
				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);
				addOrigine(julie, MockCommune.Lausanne);
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(julie, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);

				// marie les individus
				marieIndividus(pierre, julie, dateMariage);
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee);
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee, false));

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				Arrivee arrivee = new ArriveePrincipale(individuPrincipal, individuConjoint, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Lausanne.getNoOFS(), MockCommune.Neuchatel,
						MockCommune.Lausanne, anciennesAdresses.principale, nouvellesAdresses.principale, context);

				/*
				 * Vérification que les individus n'existent pas en base avant leurs arrivées
				 */
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal));
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint));

				/*
				 * Arrivée
				 */
				arrivee.handle(buildMessageCollector());
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 1, tiersDAO.getCount(ForFiscal.class));
		}

		{
			/*
			 * Vérification du couple
			 */
			PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal);
			assertEquals(new Long(noIndividuPrincipal), habitantPrincipal.getNumeroIndividu());
			PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint);
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());

			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantPrincipal, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitantPrincipal.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitantPrincipal.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			MenageCommun menage = couple.getMenage();
			assertNotNull(menage);

			final AppartenanceMenage rapportMenagePrincipal = (AppartenanceMenage) habitantPrincipal.getRapportSujetValidAt(dateArrivee, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertNotNull(rapportMenagePrincipal);
			assertEquals(dateMariage, rapportMenagePrincipal.getDateDebut());
			assertNull(rapportMenagePrincipal.getDateFin());

			final AppartenanceMenage rapportMenageConjoint = (AppartenanceMenage) habitantConjoint.getRapportSujetValidAt(dateArrivee, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertNotNull(rapportMenageConjoint);
			assertEquals(dateMariage, rapportMenageConjoint.getDateDebut());
			assertNull(rapportMenageConjoint.getDateFin());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitantPrincipal.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(habitantConjoint.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(menage.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			assertNull(habitantPrincipal.getForFiscalPrincipalAt(null));
			assertNull(habitantConjoint.getForFiscalPrincipalAt(null));
			ForFiscalPrincipal ff = menage.getForFiscalPrincipalAt(null);
			assertNotNull(ff);

			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), ff.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, ff.getDateDebut());
			assertNull(null, ff.getDateFin());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un couple depuis une commune du canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveePrincipaleCoupleDeCanton() throws Exception {
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();
		final RegDate dateMariage = RegDate.get(1979, 7, 11);

		/*
		 * Création des données du mock service civil
		 */
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveInitiale, veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveInitiale, veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				// nationalité
				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);
				addOrigine(julie, MockCommune.Lausanne);
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(julie, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);

				// marie les individus
				marieIndividus(pierre, julie, dateMariage);
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee);
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee, false));

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * Création des habitants et de leurs situations avant l'arrivée
				 */
				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(newHabitant(noIndividuPrincipal),
						newHabitant(noIndividuConjoint), dateMariage, null);

				MenageCommun menage = ensemble.getMenage();
				ForFiscalPrincipal f = addForPrincipal(menage, MockCommune.Lausanne, dateArriveInitiale, null);
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				menage = (MenageCommun) tiersDAO.save(menage);
				assertNotNull(menage);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				Arrivee arrivee = new ArriveePrincipale(individuPrincipal, individuConjoint, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne,
						MockCommune.Cossonay, anciennesAdresses.principale, nouvellesAdresses.principale, context);

				/*
				 * Arrivée
				 */
				arrivee.handle(buildMessageCollector());
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			/*
			 * Vérification du couple
			 */
			final PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal);
			final PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint);
			assertEquals(new Long(noIndividuPrincipal), habitantPrincipal.getNumeroIndividu());
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());

			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantConjoint, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitantPrincipal.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitantPrincipal.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			final MenageCommun menage = couple.getMenage();
			assertNotNull(menage);

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitantPrincipal.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(habitantConjoint.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(menage.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			assertNull(habitantConjoint.getForFiscalPrincipalAt(null));
			assertNull(habitantConjoint.getForFiscalPrincipalAt(null));

			final List<ForFiscal> list = menage.getForsFiscauxSorted();
			final ForFiscalPrincipal lausanne = (ForFiscalPrincipal) list.get(0);
			assertNotNull(lausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), lausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArriveInitiale, lausanne.getDateDebut());
			assertEquals(veilleArrivee, lausanne.getDateFin());

			final ForFiscalPrincipal cossonay = (ForFiscalPrincipal) list.get(1);
			assertNotNull(cossonay);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), cossonay.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, cossonay.getDateDebut());
			assertNull(null, cossonay.getDateFin());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un couple étranger depuis une commune du canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveePrincipaleCoupleEtrangerDeCanton() throws Exception {
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();
		final RegDate dateMariage = RegDate.get(1979, 7, 11);

		/*
		 * Création des données du mock service civil
		 */
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveInitiale, veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveInitiale, veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				// nationalité
				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.France, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.FRONTALIER, RegDate.get(1963, 8, 20), null, false);
				addOrigine(julie, MockCommune.Lausanne);
				addNationalite(julie, MockPays.France, RegDate.get(1963, 8, 20), null);
				addPermis(julie, TypePermis.FRONTALIER, RegDate.get(1963, 8, 20), null, false);

				// marie les individus
				marieIndividus(pierre, julie, dateMariage);
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee);
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee, false));

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * Création de l'habitant et de sa situation avant l'arrivée
				 */
				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(newHabitant(noIndividuPrincipal),
						newHabitant(noIndividuConjoint), dateMariage, null);

				MenageCommun menage = ensemble.getMenage();
				ForFiscalPrincipal f = addForPrincipal(menage, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				f.setModeImposition(ModeImposition.SOURCE);
				menage = (MenageCommun) tiersDAO.save(menage);
				assertNotNull(menage);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				Arrivee arrivee = new ArriveePrincipale(individuPrincipal, individuConjoint, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne,
						MockCommune.Cossonay, anciennesAdresses.principale, nouvellesAdresses.principale, context);

				/*
				 * Arrivée
				 */
				arrivee.handle(buildMessageCollector());
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal);
			assertEquals(new Long(noIndividuPrincipal), habitant.getNumeroIndividu());
			PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint);
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitant, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitant.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitant.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			final MenageCommun menage = couple.getMenage();
			assertNotNull(menage);
			List<ForFiscal> listMenage = menage.getForsFiscauxSorted();
			ForFiscalPrincipal lausanne = (ForFiscalPrincipal) listMenage.get(0);
			assertNotNull(lausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), lausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(RegDate.get(1980, 1, 1), lausanne.getDateDebut());
			assertEquals(veilleArrivee, lausanne.getDateFin());
			assertEquals(ModeImposition.SOURCE, lausanne.getModeImposition());

			ForFiscalPrincipal cossonay = (ForFiscalPrincipal) listMenage.get(1);
			assertNotNull(cossonay);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), cossonay.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, cossonay.getDateDebut());
			assertNull(null, cossonay.getDateFin());
			assertEquals(ModeImposition.SOURCE, cossonay.getModeImposition());
		}
	}

	/**
	 * Test de l'arrivée d'un individu seul avec une adresse fiscale courrier surchargée.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveeIndividuAvecAdresseFiscale() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(2004, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						dateArrivee, null);

				// nationalité
				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);

			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			{
				AdresseSuisse adresse = new AdresseSuisse();
				adresse.setDateDebut(RegDate.get(2000, 3, 20));
				adresse.setDateFin(null);
				adresse.setPermanente(true);
				adresse.setUsage(TypeAdresseTiers.COURRIER);
				adresse.setNumeroMaison("3");
				adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
				adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
				habitant.addAdresseTiers(adresse);
			}

			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			assertEquals(habitant, tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));
		}
		hibernateTemplate.flush();

		{
			/*
			 * L'événement d'arrivée
			 */
			Arrivee arrivee =
					new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne, MockCommune.Cossonay,
							anciennesAdresses.principale, nouvellesAdresses.principale, context);

			/*
			 * Arrivée
			 */
			arrivee.handle(buildMessageCollector());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 1, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			final Set<AdresseTiers> adressesTiers = habitant.getAdressesTiers();
			assertNotNull(adressesTiers);
			assertEquals(1, adressesTiers.size());
			AdresseTiers adresseCourrier = (AdresseTiers) adressesTiers.toArray()[0];
			assertNotNull(adresseCourrier);
			assertEquals(RegDate.get(2000, 3, 20), adresseCourrier.getDateDebut());
			assertNull(adresseCourrier.getDateFin()); // l'adresse courrier ne doit pas être fermée
		}
	}

	/**
	 * Test de l'arrivée d'un couple avec une adresse fiscale courrier surchargée.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveeCoupleAvecAdresseFiscale() throws Exception {
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateArrivee = RegDate.get(2004, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();
		final RegDate dateMariage =  RegDate.get(1979, 7, 11);

		/*
		 * Création des données du mock service civil
		 */
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveInitiale, veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveInitiale, veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveInitiale, veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveInitiale, veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);

				// nationalité
				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);
				addOrigine(julie, MockCommune.Lausanne);
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(julie, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);

				// marie les individus
				marieIndividus(pierre, julie, dateMariage);
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee);
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee, false));

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * Création des habitants et de leurs situations avant l'arrivée
				 */
				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(newHabitant(noIndividuPrincipal), newHabitant(noIndividuConjoint), dateMariage, null);
				MenageCommun menage = ensemble.getMenage();

				ForFiscalPrincipal f = addForPrincipal(menage, MockCommune.Lausanne, dateArriveInitiale, null);
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				{
					AdresseSuisse adresse = new AdresseSuisse();
					adresse.setDateDebut(RegDate.get(2000, 3, 20));
					adresse.setDateFin(null);
					adresse.setPermanente(true);
					adresse.setUsage(TypeAdresseTiers.COURRIER);
					adresse.setNumeroMaison("3");
					adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
					adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
					menage.addAdresseTiers(adresse);
				}
				{
					AdresseSuisse adresse = new AdresseSuisse();
					adresse.setDateDebut(RegDate.get(2000, 3, 20));
					adresse.setDateFin(null);
					adresse.setPermanente(false);
					adresse.setUsage(TypeAdresseTiers.DOMICILE);
					adresse.setNumeroMaison("3");
					adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
					adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
					menage.addAdresseTiers(adresse);
				}

				menage = (MenageCommun) tiersDAO.save(menage);
				assertNotNull(menage);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				Arrivee arrivee = new ArriveePrincipale(individuPrincipal, individuConjoint, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Cossonay.getNoOFS(), MockCommune.Lausanne,
						MockCommune.Cossonay, anciennesAdresses.principale, nouvellesAdresses.principale, context);

				/*
				 * Arrivée
				 */
				arrivee.handle(buildMessageCollector());
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 2, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			/*
			 * Vérification du couple
			 */
			final PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal);
			final PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint);
			assertEquals(Long.valueOf(noIndividuPrincipal), habitantPrincipal.getNumeroIndividu());
			assertEquals(Long.valueOf(noIndividuConjoint), habitantConjoint.getNumeroIndividu());

			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantConjoint, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitantPrincipal.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitantPrincipal.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			final MenageCommun menage = couple.getMenage();
			assertNotNull(menage);

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitantPrincipal.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(habitantConjoint.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			final Set<AdresseTiers> adressesTiers = menage.getAdressesTiers();
			assertNotNull(adressesTiers);
			assertEquals(2, adressesTiers.size());
			AdresseTiers adresse1 = (AdresseTiers) adressesTiers.toArray()[0];
			AdresseTiers adresse2 = (AdresseSuisse) adressesTiers.toArray()[1];
			assertNotNull(adresse1);
			assertNotNull(adresse2);
			AdresseTiers adresseCourrier;
			AdresseTiers adresseDomicile;
			if (adresse1.getUsage() == TypeAdresseTiers.COURRIER) {
				adresseCourrier = adresse1;
				adresseDomicile = adresse2;
			}
			else {
				adresseCourrier = adresse2;
				adresseDomicile = adresse1;
				assertEquals(TypeAdresseTiers.DOMICILE, adresse1.getUsage());
			}
			assertEquals(RegDate.get(2000, 3, 20), adresseCourrier.getDateDebut());
			assertNull(adresseCourrier.getDateFin()); // l'adresse courrier ne doit pas être fermée car elle est permanante
			assertNotNull(adresseDomicile.getDateFin()); // l'adresse domicile doit être fermée car elle est temporaire
		}
	}

	/**
	 * Test de l'arrivée hors-canton d'un individu avec recherche du non-habitant correspondant dans unireg.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveeIndividuAvecNonHabitantDeHorsCanton() throws Exception {
		
		final RegDate dateArrivee = RegDate.get(2007, 11, 19);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();
		final long numeroIndividu = 254879;

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = RegDate.get(1987, 5, 1);
				
				MockIndividu bea = addIndividu(numeroIndividu, dateNaissance, "Duval", "Béatrice", false);
				
				// adresses avant l'arrivée
				addAdresse(bea, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(bea, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(bea, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(bea, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				
				addOrigine(bea, MockCommune.Lausanne);
				addNationalite(bea, MockPays.Suisse, dateNaissance, null);
			}
		});
		
		final Individu individu = serviceCivil.getIndividu(numeroIndividu, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee, false));
		
		final long numeroCTB = doInNewTransaction(new TxCallback<Long>() {
			@SuppressWarnings("deprecation")
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				/*
				 * Création du non-habitant
				 */
				final RegDate dateNaissanceBea = RegDate.get(1987, 5, 1);
				
				PersonnePhysique nonHabitant = newNonHabitant("Duval", "Béatrice", dateNaissanceBea, Sexe.FEMININ);
				
				// for principal
				{
					final ForFiscalPrincipal f = addForPrincipal(nonHabitant, MockCommune.Bern, dateArrivee.addYears(-1), null);
					f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
					f.setMotifOuverture(MotifFor.INDETERMINE);
					f.setMotifRattachement(MotifRattachement.DOMICILE);
				}
				// for secondaire
				{
					final ForFiscalSecondaire f = addForSecondaire(nonHabitant, MockCommune.Lausanne, dateArrivee.addYears(-1), null);
					f.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
					f.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
				}
				nonHabitant = (PersonnePhysique) tiersDAO.save(nonHabitant);
				
				return nonHabitant.getNumero();
			}

		});
		
		indexer.sync();
		
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				Arrivee arrivee =
						new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Lausanne.getNoOFS(), MockCommune.Neuchatel, MockCommune.Lausanne,
								anciennesAdresses.principale, nouvellesAdresses.principale, context);

				/*
				 * Vérification que l'individu n'existe pas en base avant son arrivée
				 */
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu));

				/*
				 * Arrivée
				 */
				arrivee.handle(buildMessageCollector());
				
				return null;
			}
		});
		
		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 3, tiersDAO.getCount(ForFiscal.class));
		}
		
		{
			PersonnePhysique habitant = tiersDAO.getPPByNumeroIndividu(numeroIndividu);
			assertNotNull(habitant);
			assertTrue(habitant.isHabitantVD());
			assertEquals(new Long(numeroIndividu), habitant.getNumeroIndividu());
			assertEquals(new Long(numeroCTB), habitant.getNumero());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			ForFiscalPrincipal ff = habitant.getForFiscalPrincipalAt(null);
			assertNotNull(ff);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), ff.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, ff.getDateDebut());
			assertNull(null, ff.getDateFin());
		}
		
	}
	
	/**
	 * Test de l'arrivée hors-canton d'un individu avec recherche du non-habitant correspondant dans unireg.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveeCoupleNonHabitantsDeHorsCanton() throws Exception {
		
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateDepart = RegDate.get(2005, 1, 1);
		final RegDate dateArrivee = RegDate.get(2008, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();
		final RegDate dateMariage = RegDate.get(1979, 7, 11);

		long noCTBPrincipal;
		long noCTBConjoint;
		
		final RegDate dateNaissancePierre = RegDate.get(1953, 11, 2);
		final RegDate dateNaissanceJulie = RegDate.get(1957, 4, 19);
		
		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, dateNaissancePierre, "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, dateNaissanceJulie, "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(julie, TypeAdresseCivil.COURRIER, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee,
						null);

				// nationalité
				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);
				addOrigine(julie, MockCommune.Lausanne);
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addPermis(julie, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);

				// marie les individus
				marieIndividus(pierre, julie, dateMariage);
			}
		});
		
		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee);
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee);
		final AdressesCiviles anciennesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee, false));
		final AdressesCiviles nouvellesAdresses = new AdressesCiviles(serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee, false));
		
		final class Couple {
			final long idPrincipal;
			final long idConjoint;
			
			public Couple(long idPrincipal, long idConjoint) {
				this.idPrincipal = idPrincipal;
				this.idConjoint = idConjoint;
			}
		}
		
		Couple coupleContribuables = doInNewTransaction(new TxCallback<Couple>() {
			@Override
			public Couple execute(TransactionStatus status) throws Exception {
				/*
				 * Création des non-habitants
				 */
				final PersonnePhysique principal = (PersonnePhysique) tiersDAO.save(newNonHabitant("Dupont", "Pierre", dateNaissancePierre, Sexe.MASCULIN));
				
				final PersonnePhysique conjoint = (PersonnePhysique) tiersDAO.save(newNonHabitant("Goux", "Julie", dateNaissanceJulie, Sexe.FEMININ));
				
				/*
				 * Création des habitants et de leurs situations avant l'arrivée
				 */
				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(principal, conjoint, dateMariage, null);

				MenageCommun menage = ensemble.getMenage();
				final ForFiscalPrincipal f = addForPrincipal(menage, MockCommune.Vevey, dateArriveInitiale, dateDepart);
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				f.setMotifFermeture(MotifFor.DEPART_HC);
				
				menage = (MenageCommun) tiersDAO.save(menage);
				assertNotNull(menage);
				
				return new Couple(principal.getNumero(), conjoint.getNumero());
			}

		});
		
		assertNotNull(coupleContribuables);
		noCTBPrincipal = coupleContribuables.idPrincipal;
		noCTBConjoint = coupleContribuables.idConjoint;
		
		/*
		 * Indexation des données fiscales
		 */
		indexData();
		
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				Arrivee arrivee =
						new ArriveePrincipale(individuPrincipal, individuConjoint, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, dateArrivee, MockCommune.Lausanne.getNoOFS(), MockCommune.Neuchatel,
								MockCommune.Lausanne, anciennesAdresses.principale, nouvellesAdresses.principale, context);

				/*
				 * Vérification que les individus n'existent pas en base avant leur arrivée
				 */
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal));
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint));

				/*
				 * Arrivée
				 */
				arrivee.handle(buildMessageCollector());
				
				return null;
			}
		});
		
		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}
		
		{
			PersonnePhysique habitantPrincipal = tiersDAO.getPPByNumeroIndividu(noIndividuPrincipal);
			assertNotNull(habitantPrincipal);
			assertTrue(habitantPrincipal.isHabitantVD());
			assertEquals(new Long(noIndividuPrincipal), habitantPrincipal.getNumeroIndividu());
			assertEquals(new Long(noCTBPrincipal), habitantPrincipal.getNumero());

			PersonnePhysique habitantConjoint = tiersDAO.getPPByNumeroIndividu(noIndividuConjoint);
			assertNotNull(habitantConjoint);
			assertTrue(habitantConjoint.isHabitantVD());
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());
			assertEquals(new Long(noCTBConjoint), habitantConjoint.getNumero());
			
			final AppartenanceMenage rapportMenagePrincipal = (AppartenanceMenage) habitantPrincipal.getRapportSujetValidAt(dateArrivee, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertNotNull(rapportMenagePrincipal);
			assertEquals(dateMariage, rapportMenagePrincipal.getDateDebut());
			assertNull(rapportMenagePrincipal.getDateFin());

			final AppartenanceMenage rapportMenageConjoint = (AppartenanceMenage) habitantConjoint.getRapportSujetValidAt(dateArrivee, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertNotNull(rapportMenageConjoint);
			assertEquals(dateMariage, rapportMenageConjoint.getDateDebut());
			assertNull(rapportMenageConjoint.getDateFin());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitantPrincipal.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantPrincipal, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitantPrincipal.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitantPrincipal.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			final MenageCommun menage = couple.getMenage();
			assertNotNull(menage);
			
			List<ForFiscal> listForsMenage = menage.getForsFiscauxSorted();
			
			ForFiscalPrincipal forVevey = (ForFiscalPrincipal) listForsMenage.get(0);
			assertNotNull(forVevey);
			assertEquals(new Integer(MockCommune.Vevey.getNoOFSEtendu()), forVevey.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArriveInitiale, forVevey.getDateDebut());
			assertEquals(dateDepart, forVevey.getDateFin());

			ForFiscalPrincipal forLausanne = (ForFiscalPrincipal) listForsMenage.get(1);
			assertNotNull(forLausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), forLausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, forLausanne.getDateDebut());
			assertNull(null, forLausanne.getDateFin());
		}
		
	}

	/**
	 * [UNIREG-2145]
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveVaudoiseSourcierMixteAvecPermisB() throws Exception {

		final long noInd = 1;
		final long noTiers = 10000001;

		// Crée un habitant actuellement dans le canton et avec un mode d'imposition source-mixte

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(noInd, date(1950, 1, 1), "Pouly", "Mohamed", true);
				addNationalite(ind, MockPays.Colombie, date(1950, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(1980, 1, 1), null, false);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitant = addHabitant(noTiers, noInd);
				ForFiscalPrincipal ffp = addForPrincipal(habitant, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2005, 6, 30), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.MIXTE_137_1);
				ffp = addForPrincipal(habitant, date(2005, 7, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny);
				ffp.setModeImposition(ModeImposition.MIXTE_137_1);
				return null;
			}
		});

		final RegDate dateArrivee = date(2010, 3, 24);

		// Crée un événement d'arrivée de Bussigny à Lausanne au 24 mars 2010
		MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);
		final Individu individu = serviceCivil.getIndividu(noInd, null);

		Arrivee arrivee =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, dateArrivee, MockCommune.Lausanne.getNoOFSEtendu(), null, MockCommune.Lausanne, null, nouvelleAdresse, context);

		// Traite l'événement d'arrivée
		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);
		arrivee.handle(collector);
		assertEmpty(collector.getErreurs());

		// [UNIREG-2145] On vérifique que la for principal ouvert possède le même mode d'imposition (source-mixte) que le précédent
		final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(noTiers);
		assertNotNull(hab);

		final List<ForFiscal> fors = hab.getForsFiscauxSorted();
		assertEquals(3, fors.size());
		assertForPrincipal(date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2005, 6, 30), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(),
				MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_1, (ForFiscalPrincipal) fors.get(0));
		assertForPrincipal(date(2005, 7, 1), MotifFor.DEMENAGEMENT_VD, dateArrivee.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFSEtendu(),
				MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_1, (ForFiscalPrincipal) fors.get(1));
		assertForPrincipal(dateArrivee, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_1,
				(ForFiscalPrincipal) fors.get(2));
	}

	/**
	 * [UNIREG-2145]
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveHCAncienSourcierMixteAvecPermisB() throws Exception {

		final long noInd = 1;
		final long noTiers = 10000001;

		// Crée un habitant actuellement hors-canton et avec un mode d'imposition source-mixte

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(noInd, date(1950, 1, 1), "Pouly", "Mohamed", true);
				addNationalite(ind, MockPays.Colombie, date(1950, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(1980, 1, 1), null, false);
			}
		});

		doInNewTransactionAndSessionWithoutValidation(new TxCallback<Object>() { // [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitant = addHabitant(noTiers, noInd);
				ForFiscalPrincipal ffp = addForPrincipal(habitant, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2005, 6, 30), MotifFor.DEPART_HC, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.MIXTE_137_1);
				ffp = addForPrincipal(habitant, date(2005, 7, 1), MotifFor.DEPART_HC, MockCommune.Bern);
				ffp.setModeImposition(ModeImposition.MIXTE_137_1);
				return null;
			}
		});

		final RegDate dateArrivee = date(2010, 3, 24);

		// Crée un événement d'arrivée de HC à Lausanne au 24 mars 2010
		final Individu individu = serviceCivil.getIndividu(noInd, null);
		MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);

		Arrivee arrivee =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, dateArrivee, MockCommune.Lausanne.getNoOFSEtendu(), null, MockCommune.Lausanne, null, nouvelleAdresse, context);

		// Traite l'événement d'arrivée
		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);

		try {
			arrivee.handle(collector);
			fail();
		}
		catch (ValidationException e) {
			// [SIFISC-57] le tiers ne valide pas, et l'événement ne peut pas passer avant que le mode d'imposition du fors existant soit corrigé
			assertEquals("PersonnePhysique #10000001 - 1 erreur(s) - 0 warning(s):\n" +
					" [E] Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger, les modes d'imposition possibles sont \"ordinaire\" ou \"source\".\n", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModeImpositionArriveeHCEtrangerNonEtabliAvecImmeuble() throws Exception {

		final long noInd = 123456;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(noInd, date(1950, 1, 1), "Pouly", "Mohamed", true);
				addNationalite(ind, MockPays.Colombie, date(1950, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(1980, 1, 1), null, false);
			}
		});

		final RegDate dateAchat = date(2009, 4, 10);

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique habitant = addNonHabitant("Mohamed", "Pouly", date(1950, 1, 1), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(habitant, MockCommune.Bern, dateAchat, null);
				ffp.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
				ffp.setModeImposition(ModeImposition.ORDINAIRE);

				final ForFiscalSecondaire ffs = addForSecondaire(habitant, MockCommune.Aubonne, dateAchat, null);
				ffs.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
				return null;
			}
		});

		globalTiersIndexer.sync();

		final RegDate dateArrivee = date(2009, 12, 1);
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);
		final Individu individu = serviceCivil.getIndividu(noInd, null);

		final Arrivee arrivee =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, dateArrivee, MockCommune.Lausanne.getNoOFSEtendu(), null, MockCommune.Lausanne, null, nouvelleAdresse,
						context);

		// Traite l'événement d'arrivée
		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);
		arrivee.handle(collector);
		assertEmpty(collector.getErreurs());

		final PersonnePhysique hab = tiersService.getPersonnePhysiqueByNumeroIndividu(noInd);
		assertNotNull(hab);

		// on vérifie les fors fiscaux, en particulier le for principal créé par l'arrivée : son mode d'imposition doit être MIXTE_1
		final List<ForFiscal> fors = hab.getForsFiscauxSorted();
		assertEquals(3, fors.size());
		assertForPrincipal(dateAchat, MotifFor.ACHAT_IMMOBILIER, dateArrivee.addDays(-1), MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Bern.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
		assertForSecondaire(dateAchat, MotifFor.ACHAT_IMMOBILIER, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE, (ForFiscalSecondaire) fors.get(1));
		assertForPrincipal(dateArrivee, MotifFor.ARRIVEE_HC, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_1, (ForFiscalPrincipal) fors.get(2));
	}

	@Ignore(value = "Cas très rare, on y réfléchira plus tard")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	/**
	 * Scénario :
	 * - M. et Mme vivaient heureux à lausanne jusqu'à leur séparation (fiscale seulement)
	 * - Ils se sont donc séparés, fiscalement, puis sont tous les deux partis hors du territoire cantonal : lui à Genève, elle à Neuchâtel
	 * - Mme revient
	 *
	 * Quelques questions se posent...
	 *  1. la séparation fiscale est-elle toujours valable ? je ne pense pas... si elle n'a pas été suivie d'une séparation civile, il
	 *     faudra refaire la demande à l'administration fiscale...
	 *  --> donc on ouvre un nouveau for sur le couple, et on ré-ouvre les rapports entre tiers...
	 *
	 *  2. A quelle date faut-il ré-ouvrir les rapports entre tiers ? On va dire la date d'arrivée...
	 */
	public void testRetourHCdeCoupleSepareFiscalementLorsDuPremierSejour() throws Exception {

		final long noIndividuPierre = 1;
		final long noIndividuJulie = 2;
		final RegDate dateMariage =  RegDate.get(1979, 7, 11);
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateSeparationFiscale = RegDate.get(1999, 5, 31);
		final RegDate dateDepart = RegDate.get(2006, 6, 30);
		final RegDate lendemainDepart = dateDepart.getOneDayAfter();
		final RegDate dateRetour = RegDate.get(2008, 7, 1);
		final RegDate veilleRetour = dateRetour.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu pierre = addIndividu(noIndividuPierre, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				final MockIndividu julie = addIndividu(noIndividuJulie, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant le départ
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveInitiale, dateDepart);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveInitiale, dateDepart);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveInitiale, dateDepart);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveInitiale, dateDepart);

				// adresses hors du canton
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, lendemainDepart, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, lendemainDepart, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, lendemainDepart, veilleRetour);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Neuchatel.RueDesBeauxArts, null, lendemainDepart, veilleRetour);

				// adresse après retour de madame
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateRetour, null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateRetour, null);

				// nationalité
				addOrigine(pierre, MockCommune.Lausanne);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
				addOrigine(julie, MockCommune.Lausanne);
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null);

				// marie les individus
				marieIndividus(pierre, julie, dateMariage);
			}
		});

		class Ids {
			long noCtbPierre;
			long noCtbJulie;
			long noCtbMenage;
		}
		final Ids ids = new Ids();

		// création des fors
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// tiers habitants (en fait, monsieur ne l'est plus)
				final PersonnePhysique pierre = addNonHabitant("Pierre", "Dupont", RegDate.get(1953, 11, 2), Sexe.MASCULIN);
				pierre.setNumeroIndividu(noIndividuPierre);

				final PersonnePhysique julie = addHabitant(noIndividuJulie);
				final EnsembleTiersCouple menage = addEnsembleTiersCouple(pierre, julie, dateMariage, dateSeparationFiscale);
				addForPrincipal(menage.getMenage(), dateArriveInitiale, MotifFor.ARRIVEE_HS, dateSeparationFiscale, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

				addForPrincipal(pierre, dateSeparationFiscale.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
				addForPrincipal(pierre, lendemainDepart, MotifFor.DEPART_HC, MockCommune.Geneve);

				addForPrincipal(julie, dateSeparationFiscale.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
				addForPrincipal(julie, lendemainDepart, MotifFor.DEPART_HC, MockCommune.Neuchatel);

				ids.noCtbJulie = julie.getNumero();
				ids.noCtbPierre = pierre.getNumero();
				ids.noCtbMenage = menage.getMenage().getNumero();
				return null;
			}
		});

		// retour de Julie
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Crée un événement d'arrivée de HC à Lausanne au 24 mars 2010
				final Individu individu = serviceCivil.getIndividu(noIndividuJulie, null);
				final MockAdresse nouvelleAdresse = new MockAdresse();
				nouvelleAdresse.setDateDebutValidite(dateRetour);

				final Arrivee arrivee =
						new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, dateRetour, MockCommune.Lausanne.getNoOFSEtendu(), null, MockCommune.Lausanne, null,
								nouvelleAdresse, context);

				// Traite l'événement d'arrivée
				final MessageCollector collector = buildMessageCollector();
				arrivee.validate(collector, collector);
				arrivee.handle(collector);
				assertEmpty(collector.getErreurs());

				return null;
			}
		});

		// vérification des fors et rapports d'appartenance ménage
		final MenageCommun menage = (MenageCommun) tiersDAO.get(ids.noCtbMenage);
		assertNotNull(menage);

		final Set<RapportEntreTiers> pps = menage.getRapportsObjet();
		assertNotNull(pps);
		assertEquals(4, pps.size());
	}

	/**
	 * [UNIREG-2145]
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveHSAncienSourcierMixteAvecPermisB() throws Exception {

		final long noInd = 1;
		final long noTiers = 10000001;

		// Crée un habitant actuellement hors-Suisse et avec un mode d'imposition source-mixte

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(noInd, date(1950, 1, 1), "Pouly", "Mohamed", true);
				addNationalite(ind, MockPays.Colombie, date(1950, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(1980, 1, 1), null, false);
			}
		});

		doInNewTransactionAndSessionWithoutValidation(new TxCallback<Object>() { // [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitant = addHabitant(noTiers, noInd);
				ForFiscalPrincipal ffp = addForPrincipal(habitant, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2005, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.MIXTE_137_1);
				ffp = addForPrincipal(habitant, date(2005, 7, 1), MotifFor.DEPART_HS, MockPays.Colombie);
				ffp.setModeImposition(ModeImposition.MIXTE_137_1);
				return null;
			}
		});

		final RegDate dateArrivee = date(2010, 3, 24);

		// Crée un événement d'arrivée de HS à Lausanne au 24 mars 2010
		final Individu individu = serviceCivil.getIndividu(noInd, null);
		MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);

		Arrivee arrivee =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, dateArrivee, MockCommune.Lausanne.getNoOFSEtendu(), null, MockCommune.Lausanne, null, nouvelleAdresse, context);

		// Traite l'événement d'arrivée
		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);

		try {
			arrivee.handle(collector);
			fail();
		}
		catch (ValidationException e) {
			// [SIFISC-57] le tiers ne valide pas, et l'événement ne peut pas passer avant que le mode d'imposition du fors existant soit corrigé
			assertEquals("PersonnePhysique #10000001 - 1 erreur(s) - 0 warning(s):\n" +
					" [E] Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger, les modes d'imposition possibles sont \"ordinaire\" ou \"source\".\n", e.getMessage());
		}
	}


	/**
	 * [UNIREG-2212] Vérifie qu'un déménagement vaudois au 20 décembre ouvre bien un nouveau for fiscal au 20 décembre sur la nouvelle commune (règle de fin d'année non-active)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementVaudois20Decembre() throws Exception {

		final long noInd = 1;
		final long noTiers = 10000001;

		// Crée un habitant actuellement hors-Suisse et avec un mode d'imposition source-mixte

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1950, 1, 1), "Descombaz", "Louis", true);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitant = addHabitant(noTiers, noInd);
				addForPrincipal(habitant, date(1970, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				return null;
			}
		});

		final RegDate dateArrivee = date(2009, 12, 20);

		// Crée un événement d'arrivée de HS à Bussigny le 20 décembre 2008
		final Individu individu = serviceCivil.getIndividu(noInd, null);
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);

		final Arrivee arrivee =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, dateArrivee, MockCommune.Bussigny.getNoOFSEtendu(), null, MockCommune.Bussigny, null,
						nouvelleAdresse, context);

		// Traite l'événement d'arrivée
		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);
		arrivee.handle(collector);
		assertEmpty(collector.getErreurs());

		// On vérifique que la for principal sur Bussigny a été ouvert le 1er janvier 2009
		final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(noTiers);
		assertNotNull(hab);

		final List<ForFiscal> fors = hab.getForsFiscauxSorted();
		assertEquals(2, fors.size());
		assertForPrincipal(date(1970, 1, 1), MotifFor.MAJORITE, dateArrivee.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
		assertForPrincipal(dateArrivee, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
				(ForFiscalPrincipal) fors.get(1));
	}

	/**
	 * [UNIREG-2212] Vérifie qu'un déménagement vaudois au 21 décembre ouvre bien un nouveau for fiscal au 1er janvier de l'année suivante sur la nouvelle commune (règle de fin d'année activée).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementVaudois21Decembre() throws Exception {

		final long noInd = 1;
		final long noTiers = 10000001;

		// Crée un habitant actuellement hors-Suisse et avec un mode d'imposition source-mixte

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1950, 1, 1), "Descombaz", "Louis", true);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitant = addHabitant(noTiers, noInd);
				addForPrincipal(habitant, date(1970, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				return null;
			}
		});

		final RegDate dateArrivee = date(2009, 12, 21);

		// Crée un événement d'arrivée de HS à Bussigny le 20 décembre 2008
		final Individu individu = serviceCivil.getIndividu(noInd, null);
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);

		final Arrivee arrivee =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, dateArrivee, MockCommune.Bussigny.getNoOFSEtendu(), null, MockCommune.Bussigny, null,
						nouvelleAdresse, context);

		// Traite l'événement d'arrivée
		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);
		arrivee.handle(collector);
		assertEmpty(collector.getErreurs());

		// On vérifique que la for principal sur Bussigny a été ouvert le 1er janvier 2009
		final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(noTiers);
		assertNotNull(hab);

		final List<ForFiscal> fors = hab.getForsFiscauxSorted();
		assertEquals(2, fors.size());
		assertForPrincipal(date(1970, 1, 1), MotifFor.MAJORITE, date(2009, 12, 31), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
		assertForPrincipal(date(2010, 1, 1), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(1));
	}

	/**
	 * [UNIREG-2212] Vérifie qu'une arrivée de hors-Canton au 21 décembre ouvre bien un nouveau for fiscal au 21 décembre sur la nouvelle commune (pas de règle de fin d'année)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveeDeHorsCanton21Decembre() throws Exception {

		final long noInd = 1;
		final long noTiers = 10000001;

		// Crée un habitant actuellement hors-Suisse et avec un mode d'imposition source-mixte

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(noInd, date(1950, 1, 1), "Descombaz", "Louis", true);
				addNationalite(ind, MockPays.Suisse, date(1950, 1, 1), null);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitant = addHabitant(noTiers, noInd);
				addForPrincipal(habitant, date(1970, 1, 1), MotifFor.MAJORITE, MockCommune.Zurich);
				return null;
			}
		});

		final RegDate dateArrivee = date(2009, 12, 20);

		// Crée un événement d'arrivée de HS à Bussigny le 20 décembre 2008
		final Individu individu = serviceCivil.getIndividu(noInd, null);
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);

		final Arrivee arrivee =
				new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, dateArrivee, MockCommune.Bussigny.getNoOFSEtendu(), null, MockCommune.Bussigny, null, nouvelleAdresse, context);

		// Traite l'événement d'arrivée
		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);
		arrivee.handle(collector);
		assertEmpty(collector.getErreurs());

		// On vérifique que la for principal sur Bussigny a été ouvert le 1er janvier 2009
		final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(noTiers);
		assertNotNull(hab);

		final List<ForFiscal> fors = hab.getForsFiscauxSorted();
		assertEquals(2, fors.size());
		assertForPrincipal(date(1970, 1, 1), MotifFor.MAJORITE, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, 
				MockCommune.Zurich.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
		assertForPrincipal(dateArrivee, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
				(ForFiscalPrincipal) fors.get(1));
	}

	/**
	 * [UNIREG-3073] Vérifie que l'arrivée d'un individu provoque bien la recherche d'un éventuel non-habitant, et que si ce dernier ne possède pas de date de naissance, l'événement part en erreur.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleArriveeNonHabitantSansDateNaissance() throws Exception {

		setWantIndexation(true);
		removeIndexData();

		//
		// Crée un individu
		//
		class ServiceCivil extends MockServiceCivil {

			private MockIndividu roger;

			@Override
			protected void init() {
				roger = addIndividu(585858, date(1960, 1, 1), "Roger", "Dupneu", true);
				addNationalite(roger, MockPays.France, date(1960, 1, 1), null);
			}
		}

		final ServiceCivil civil = new ServiceCivil();
		serviceCivil.setUp(civil);

		//
		// Crée un non-habitant avec le même prénom/nom que l'individu qui arrive, mais sans date naissance
		//
		class Ids {
			Long roger;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique roger = addNonHabitant("Roger", "Dupneu", null, Sexe.MASCULIN);
				addForPrincipal(roger, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.roger = roger.getId();
				return null;
			}
		});

		globalTiersIndexer.sync();

		//
		// Traite l'événement d'arrivée
		//

		final Arrivee arrivee = new ArriveePrincipale(civil.roger, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, null, null, null, null, (Adresse) null, null, context);

		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);

		try {
			arrivee.handle(collector);
			Assert.fail("L'événement d'arrivée aurait dû lever une erreur parce que le non-habitant trouvé n'est pas complet");
		}
		catch (EvenementCivilException e) {
			final String message = "Un non-habitant (n°" + ids.roger + ") qui possède le même prénom/nom que l'individu a été trouvé, " +
					"mais la date de naissance n'est pas renseignée. Veuillez vérifier manuellement.";
			assertEquals(message, e.getMessage());
		}
	}

	private static PersonnePhysique newHabitant(long noIndividuPrincipal) throws Exception {

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividuPrincipal);
		return habitant;
	}

	private static PersonnePhysique newNonHabitant(String nom, String prenom, RegDate dateNaissance, Sexe sexe) {
		PersonnePhysique nonHabitant = new PersonnePhysique(false);
		nonHabitant.setPrenom(prenom);
		nonHabitant.setNom(nom);
		nonHabitant.setSexe(sexe);
		nonHabitant.setDateNaissance(dateNaissance);
		return nonHabitant;
	}
	
	private static ForFiscalPrincipal addForPrincipal(Contribuable contribuable, Commune commune, RegDate ouverture, RegDate fermeture) {
		final TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		final ForFiscalPrincipal ffp = new ForFiscalPrincipal(ouverture, fermeture, commune.getNoOFS(), type, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		contribuable.addForFiscal(ffp);
		return ffp;
	}
	
	private static ForFiscalSecondaire addForSecondaire(Contribuable contribuable, Commune commune, RegDate ouverture, RegDate fermeture) {
		final ForFiscalSecondaire ffs = new ForFiscalSecondaire(ouverture, fermeture, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
		contribuable.addForFiscal(ffs);
		return ffs;
	}
}

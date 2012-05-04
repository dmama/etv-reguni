package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.evenement.civil.interne.demenagement.DemenagementTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockBatiment;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.validation.fors.ForFiscalValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <b>Note:</b> La plupart des tests de l'arrivée handler sont dans la class {@link ArriveeExtTest}.
 */
@SuppressWarnings({"JavaDoc"})
public class ArriveeTest extends AbstractEvenementCivilInterneTest {

	private static final String DB_UNIT_DATA_FILE = "ArriveeTest.xml";

	private static final Long NUMERO_INDIVIDU_INCONNU = 9999l;
	private static final Long NUMERO_INDIVIDU_SEUL = 34567L;
	private static final Long NUMERO_INDIVIDU_MARIE_SEUL = 12345L;

	private static final RegDate DATE_VALIDE = RegDate.get(2007, 11, 19);
	private static final RegDate DATE_FUTURE = RegDate.get(2020, 11, 19);
	private static final RegDate DATE_ANCIENNE_ADRESSE = RegDate.get(1970, 11, 19);
	private static final RegDate DATE_ANTERIEURE_ANCIENNE_ADRESSE = RegDate.get(1940, 11, 19);

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInitIndividuSeul() throws Exception {

		final long numeroIndividu = 12345L;
		final RegDate dateArrivee = RegDate.get(2002, 3, 15);
		final RegDate dateVeilleArrivee = dateArrivee.getOneDayBefore();

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(numeroIndividu);

		// Crée l'événement
		EvenementCivilRegPP
				evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateArrivee, numeroIndividu, 0L, 1234, null);

		// Prend le mock infrastructure par défaut
		ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new MockServiceInfrastructureService() {
			@Override
			protected void init() {
				// Pays
				pays.add(MockPays.Suisse);

				// Cantons
				cantons.add(MockCanton.Vaud);

				// Communes
				communesVaud.add(MockCommune.Lausanne);
				communesVaud.add(MockCommune.Cossonay);

				// Localités
				localites.add(MockLocalite.Lausanne);
				localites.add(MockLocalite.CossonayVille);

				// Rues
				rues.add(MockRue.CossonayVille.CheminDeRiondmorcel);
				rues.add(MockRue.Lausanne.AvenueDeBeaulieu);
			}
		});

		// Crée les données du mock service civil
		ServiceCivilService serviceCivil = new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(numeroIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), dateVeilleArrivee);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), dateVeilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
			}
		};

		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, tiersDAO);
		final ArriveePrincipale adapter = new ArriveePrincipale(evenement, context, options);

		assertEquals(MockLocalite.Lausanne.getNomAbregeMinuscule(), adapter.getAncienneAdresse().getLocalite());
		assertEquals(MockCommune.Cossonay.getNomMinuscule(), adapter.getNouvelleCommune().getNomMinuscule());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	/**
	 * Teste les différents scénarios devant échouer au test de complétude de l'arrivée.
	 */
	public void testCheckCompleteness() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final MessageCollector collector = buildMessageCollector();

		// 1er test : individu seul
		final Individu individuSeul = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, date(2000, 12, 31));
		ArriveePrincipale arrivee = createValidArrivee(individuSeul, DATE_VALIDE);
		arrivee.checkCompleteness(collector, collector);
		Assert.isTrue(collector.getErreurs().isEmpty(), "individu célibataire : ca n'aurait pas du causer une erreur");

		// 2ème test : individu marié seul
		collector.clear();
		final Individu individuMarieSeul = serviceCivil.getIndividu(NUMERO_INDIVIDU_MARIE_SEUL, date(2000, 12, 31));
		arrivee = createValidArrivee(individuMarieSeul, DATE_VALIDE);
		arrivee.checkCompleteness(collector, collector);
		Assert.isTrue(collector.getErreurs().isEmpty(), "individu célibataire marié seul : ca n'aurait pas du causer une erreur");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	/**
	 * Teste les différents scénarios devant échouer à la validation.
	 */
	public void testValidate() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final MessageCollector collector = buildMessageCollector();

		// 1er test : événement avec une date dans le futur
		Arrivee arrivee = createValidArrivee(serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, date(2000, 12, 31)), DATE_FUTURE);
		arrivee.validate(collector, collector);
		Assert.notEmpty(collector.getErreurs(), "Une date future pour l'événement aurait dû renvoyer une erreur");

		// 2ème test : arrivée antérieur à la date de début de validité de
		// l'ancienne adresse
		collector.clear();

		// Ancienne adresse
		MockAdresse ancienneAdresse = new MockAdresse();
		ancienneAdresse.setDateDebutValidite(DATE_ANCIENNE_ADRESSE);
		ancienneAdresse.setPays(MockPays.Suisse);
		final MockCommune ancienneCommune = MockCommune.Cossonay;
		ancienneAdresse.setCommuneAdresse(ancienneCommune);

		// Nouvelle adresse
		final MockCommune commune = MockCommune.Lausanne;
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(DATE_ANTERIEURE_ANCIENNE_ADRESSE);

		arrivee = new ArriveePrincipale(serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, date(2000, 12, 31)), null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS,
				DATE_ANTERIEURE_ANCIENNE_ADRESSE, commune.getNoOFSEtendu(), ancienneCommune, commune, ancienneAdresse, nouvelleAdresse, context);
		arrivee.validate(collector, collector);
		Assert.notEmpty(collector.getErreurs(), "L'arrivée est antérieur à la date de début de validité de l'ancienne adresse, une erreur aurait du être déclenchée");

		// 3ème test : arrivée hors canton
		collector.clear();
		arrivee = createValidArrivee(serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, date(2000, 12, 31)), MockCommune.Neuchatel, DATE_VALIDE);
		arrivee.validate(collector, collector);
		Assert.notEmpty(collector.getErreurs(), "L'arrivée est hors canton, une erreur aurait du être déclenchée");

		// 4ème test : commune du Sentier -> traitement manuel dans tous les cas
		collector.clear();
		arrivee = createValidArrivee(serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, date(2000, 12, 31)), MockCommune.LeChenit, MockCommune.Fraction.LeSentier);
		arrivee.validate(collector, collector);
		Assert.isTrue(collector.getWarnings().size() == 1, "L'arrivée est dans la commune du sentier, un warning aurait du être déclenchée");
	}

	/**
	 * Teste les différentes exceptions acceptées pour le traitement d'une arrivée
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateException() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final MessageCollector collector = buildMessageCollector();

		final long NUMERO_INDIVIDU_CONJOINT = 43321L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu inconnu = addIndividu(NUMERO_INDIVIDU_INCONNU, date(1953, 11, 2), "Johan", "Rackham", true);
				addAdresse(inconnu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, DATE_VALIDE, null);
				final MockIndividu conjoint = addIndividu(NUMERO_INDIVIDU_CONJOINT, date(1957, 1, 12), "Adèle", "Lerouge", false);
				addAdresse(conjoint, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, DATE_VALIDE, null);
			}
		});

		final Individu inconnu = serviceCivil.getIndividu(NUMERO_INDIVIDU_INCONNU, null, AttributeIndividu.ADRESSES);
		assertNotNull(inconnu);
		final Individu conjoint = serviceCivil.getIndividu(NUMERO_INDIVIDU_CONJOINT, null);
		assertNotNull(conjoint);

		final MockCommune commune = MockCommune.Lausanne;
		final Adresse nouvelleAdressePrincipale = inconnu.getAdresses().iterator().next();

		/*
		 * 1er test : événement avec le tiers correspondant à l'individu manquant
		 */
		Arrivee arrivee = new ArriveePrincipale(inconnu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, commune.getNoOFSEtendu(), null, commune, null, nouvelleAdressePrincipale, context);
		arrivee.validate(collector, collector);
		Assert.isTrue(collector.getErreurs().isEmpty(), "Le tiers rattaché à l'individu n'existe pas, mais ceci est un cas valide et aucune erreur n'aurait dû être déclenchée");

		/*
		 * 2ème test : événement avec le tiers correspondant au conjoint manquant
		 */
		collector.clear();
		arrivee = new ArriveePrincipale(inconnu, conjoint, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, commune.getNoOFSEtendu(), null, commune, null, nouvelleAdressePrincipale, context);
		arrivee.validate(collector, collector);
		Assert.isTrue(collector.getErreurs().isEmpty(), "Le tiers rattaché au conjoint n'existe pas, mais ceci est un cas valide et aucune erreur n'aurait dû être déclenchée");

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandle() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, date(2000, 12, 31));
		Arrivee arrivee = createValidArrivee(individu, DATE_VALIDE);

		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);
		arrivee.handle(collector);

		Assert.isTrue(collector.getErreurs().isEmpty(), "Une erreur est survenue lors du traitement de l'arrivée");

		PersonnePhysique tiers = tiersDAO.getHabitantByNumeroIndividu(arrivee.getNoIndividu());
		assertTrue(tiers != null);

		Set<AdresseTiers> adresses = tiers.getAdressesTiers();
		assertFalse(adresses.isEmpty());
		for (AdresseTiers adresse : adresses) {
			assertFalse(adresse instanceof AdresseCivile);
		}
		Set<ForFiscal> forsFiscaux = tiers.getForsFiscaux();
		assertFalse("Le contribuable n'a aucun for fiscal", forsFiscaux.isEmpty());

		ForFiscal forFiscalPrincipal = null;
		for (ForFiscal forFiscal : forsFiscaux) {
			if (forFiscal instanceof ForFiscalPrincipal) {
				forFiscalPrincipal = forFiscal;
				break;
			}
		}
		assertNotNull("Aucun for princpal trouvé", forFiscalPrincipal);

		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(tiers).size());
	}

	private ArriveePrincipale createValidArrivee(Individu individu, RegDate dateArrivee) {

		// Anciennes adresses
		/*MockAdresse ancienneAdresse = new MockAdresse();
		ancienneAdresse.setDateDebutValidite(DATE_ANCIENNE_ADRESSE);
		ancienneAdresse.setPays(MockPays.France);
		arrivee.setAncienneAdressePrincipale(ancienneAdresse);*/

		// Nouvelles adresses
		final MockCommune commune = MockCommune.Lausanne;

		return createValidArrivee(individu, commune, dateArrivee);
	}

	private ArriveePrincipale createValidArrivee(Individu individu, MockCommune commune, RegDate dateArrivee) {
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);

		return new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, dateArrivee, commune.getNoOFSEtendu(), null, commune, null, nouvelleAdresse, context);
	}

	private Arrivee createValidArrivee(Individu individu, MockCommune communeAnnonce, MockCommune nouvelleCommune) {
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(DATE_VALIDE);

		return new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, communeAnnonce.getNoOFSEtendu(), null, nouvelleCommune, null, nouvelleAdresse, context);
	}

	/**
	 * [UNIREG-1603] Teste les différents cas de recherche de non-habitants
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
    @ExpectedException(EvenementCivilException.class)
	public void testFindNonHabitants() throws Exception {

		class Ids {
			Long jeanNomPrenom;
			Long jeanNomPrenomDate;
			Long jeanNomPrenomDateSexe;
			Long jeanNomPrenomSexe;
			Long jeanNomPrenomAssujetti;
			Long jeanNomPrenomDateAssujetti;
			Long jeanNomPrenomDateSexeAssujetti;
			Long jeanNomPrenomSexeAssujetti;
			Long jeanNomPrenomAutreDateSexeAssujetti;
			Long jeanNomPrenomDateAutreSexeAssujetti;
			Long jacquesNomPrenomDateSexeAssujetti;
			Long rogerNomPrenomSexeAssujetti;
			Long cedricNonHabitantAvecNoIndividu;
		}
		final Ids ids = new Ids();

		setWantIndexation(true);
		removeIndexData();

		doInNewTransaction(new TransactionCallback<Object>() {
			@Nullable
            @Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique jeanNomPrenom = addNonHabitant("Jean", "Dupneu", null, null);
				final PersonnePhysique jeanNomPrenomDate = addNonHabitant("Jean", "Dupneu", date(1960, 1, 1), null);
				final PersonnePhysique jeanNomPrenomDateSexe = addNonHabitant("Jean", "Dupneu", date(1960, 1, 1), Sexe.MASCULIN);
				final PersonnePhysique jeanNomPrenomSexe = addNonHabitant("Jean", "Dupneu", null, Sexe.MASCULIN);

				final PersonnePhysique jeanNomPrenomAssujetti = addNonHabitant("Jean", "Dupneu", null, null);
				addForPrincipal(jeanNomPrenomAssujetti, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);
				final PersonnePhysique jeanNomPrenomDateAssujetti = addNonHabitant("Jean", "Dupneu", date(1960, 1, 1), null);
				addForPrincipal(jeanNomPrenomDateAssujetti, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);
				final PersonnePhysique jeanNomPrenomDateSexeAssujetti = addNonHabitant("Jean", "Dupneu", date(1960, 1, 1), Sexe.MASCULIN);
				addForPrincipal(jeanNomPrenomDateSexeAssujetti, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);
				final PersonnePhysique jeanNomPrenomSexeAssujetti = addNonHabitant("Jean", "Dupneu", null, Sexe.MASCULIN);
				addForPrincipal(jeanNomPrenomSexeAssujetti, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);

				final PersonnePhysique jeanNomPrenomAutreDateSexeAssujetti = addNonHabitant("Jean", "Dupneu", date(1965, 5, 17), Sexe.MASCULIN);
				addForPrincipal(jeanNomPrenomAutreDateSexeAssujetti, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);

				final PersonnePhysique jeanNomPrenomDateAutreSexeAssujetti = addNonHabitant("Jean", "Dupneu", date(1960, 1, 1), Sexe.FEMININ);
				addForPrincipal(jeanNomPrenomDateAutreSexeAssujetti, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);

				final PersonnePhysique jacquesNomPrenomDateSexeAssujetti = addNonHabitant("Jacques", "Dupneu", date(1960, 1, 1), Sexe.MASCULIN);
				addForPrincipal(jacquesNomPrenomDateSexeAssujetti, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);

				final PersonnePhysique rogerNomPrenomSexeAssujetti = addNonHabitant("Roger", "Dupneu", null, Sexe.MASCULIN);
				addForPrincipal(rogerNomPrenomSexeAssujetti, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);

				final PersonnePhysique cedricNonHabitantAvecNoIndividu = addNonHabitant("Cédric", "Dupneu", date(1960, 1, 1), Sexe.MASCULIN);
				cedricNonHabitantAvecNoIndividu.setNumeroIndividu(375342L);
				addForPrincipal(cedricNonHabitantAvecNoIndividu, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Renens);

				ids.jeanNomPrenom = jeanNomPrenom.getId();
				ids.jeanNomPrenomDate = jeanNomPrenomDate.getId();
				ids.jeanNomPrenomDateSexe = jeanNomPrenomDateSexe.getId();
				ids.jeanNomPrenomSexe = jeanNomPrenomSexe.getId();
				ids.jeanNomPrenomAssujetti = jeanNomPrenomAssujetti.getId();
				ids.jeanNomPrenomDateAssujetti = jeanNomPrenomDateAssujetti.getId();
				ids.jeanNomPrenomDateSexeAssujetti = jeanNomPrenomDateSexeAssujetti.getId();
				ids.jeanNomPrenomSexeAssujetti = jeanNomPrenomSexeAssujetti.getId();
				ids.jeanNomPrenomAutreDateSexeAssujetti = jeanNomPrenomAutreDateSexeAssujetti.getId();
				ids.jeanNomPrenomDateAutreSexeAssujetti = jeanNomPrenomDateAutreSexeAssujetti.getId();
				ids.jacquesNomPrenomDateSexeAssujetti = jacquesNomPrenomDateSexeAssujetti.getId();
				ids.rogerNomPrenomSexeAssujetti = rogerNomPrenomSexeAssujetti.getId();
				ids.cedricNonHabitantAvecNoIndividu = cedricNonHabitantAvecNoIndividu.getId();

                // ajout de 110 M. Jean Pierre Non Habitant
                RegDate date = date(1960, 1, 1);
                for (int i = 0; i < 110; i++) {
                    addNonHabitant("Jean", "Pierre", date, Sexe.MASCULIN);
                }
				return null;
			}
		});

		globalTiersIndexer.sync();

		class ServiceCivil extends MockServiceCivil {

			private MockIndividu jean;
			private MockIndividu jacques;
			private MockIndividu roger;
			private MockIndividu cedric;
            private MockIndividu pierreJean;

			@Override
			protected void init() {
				jean = addIndividu(343434, date(1960, 1, 1), "Jean", "Dupneu", true);
				jacques = addIndividu(747474, date(1960, 1, 1), "Jacques", "Dupneu", true);
				roger = addIndividu(585858, date(1960, 1, 1), "Roger", "Dupneu", true);
				cedric = addIndividu(9191919, date(1960, 1, 1), "Cédric", "Dupneu", true);
                pierreJean = addIndividu(9191918, date(1960, 1, 1), "Pierre", "Jean", true);

			}
		}

		final ServiceCivil civil = new ServiceCivil();
		serviceCivil.setUp(civil);


		// Si on recherche un Jean Dupneu né le 1er janvier 1960 et de sexe masculin, on doit trouver tous les Jean Dupneu assujettis nés un 1er janvier 1960 <b>ou</b>
		// de date de naissance inconnue et de sexe masculin <b>ou</b> de sexe inconnu. On ne doit pas trouver les Jean Dupneu nés un autre jour ou avec un autre sexe.
		{
			final List<PersonnePhysique> list = Arrivee.findNonHabitants(context.getTiersService(), civil.jean, true);
			assertEquals(4, list.size());
			assertListContains(list, ids.jeanNomPrenomAssujetti, ids.jeanNomPrenomDateAssujetti, ids.jeanNomPrenomDateSexeAssujetti, ids.jeanNomPrenomSexeAssujetti);
		}

		// Si on recherche un Jaques Dupneu né le 1er janvier 1960 et de sexe masculin, on doit le trouver puisqu'il y en a qu'un et qu'il est complet.
		{
			final List<PersonnePhysique> list = Arrivee.findNonHabitants(context.getTiersService(), civil.jacques, true);
			assertEquals(1, list.size());
			assertListContains(list, ids.jacquesNomPrenomDateSexeAssujetti);
		}

		// [UNIREG-3073] Si on recherche un Roger Dupneu né le 1er janvier 1960 et de sexe masculin, on doit trouver le seul candidat malgré le fait qu'il ne possède pas de date de naissance
		{
			final List<PersonnePhysique> list = Arrivee.findNonHabitants(context.getTiersService(), civil.roger, true);
			assertEquals(1, list.size());
			assertListContains(list, ids.rogerNomPrenomSexeAssujetti);
		}

		// Si on recherche un Cédric Dupneu né le 1er janvier 1960 et de sexe masculin, on ne doit pas le trouver parce que
		// le candidat possède un numéro d'individu (malgré le fait que tous les critères de recherche correspondent bien)
		{
			final List<PersonnePhysique> list = Arrivee.findNonHabitants(context.getTiersService(), civil.cedric, true);
			assertEmpty(list);
		}

        // [SIFISC-4876] Si on recherche Jean Pierre, il y en a bcp trop pour l'indexeur; qui doit lever une exception catchée et réemballée dans
        // dans une EvenementCivilException avec un joli message compréhensible pour l'utilisateur
        {
            Arrivee.findNonHabitants(context.getTiersService(), civil.pierreJean, true);
            fail("Le dernier appel doit lever une exception");
        }

    }

	private void assertListContains(List<PersonnePhysique> list, Long... ids) {
		assertNotNull(list);
		final Set<Long> set = new HashSet<Long>();
		for (PersonnePhysique pp : list) {
			set.add(pp.getId());
		}
		for (Long id : ids) {
			assertTrue("L'id = [" + id + "] n'est pas contenu dans la liste", set.contains(id));
		}
	}

	/**
	 * Vérifie que le for fiscal est bien ouvert sur la commune de Grandvaux (= dépends de l'egid du bâtiment) lorsqu'un habitant arrive à Grandvaux avant la période où les communes sont
	 * fusionnées au civil.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveeDansUneCommunePasEncoreFusionneeAuCivilNiAuFiscal() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2009, 9, 1); // fusion des communes au 1 juillet 2010

		// Crée un individu qui arrive dans une commune pas encore fusionnée au civil ni au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Zbinden", "Arnold", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);
		hibernateTemplate.flush();

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe d'arrivée doit être traduit en un événement fiscal interne d'arrivée, pas de surprise ici.
		final EvenementCivilInterne interne = new ArriveeTranslationStrategy().create(externe, context, options);
		org.junit.Assert.assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Grandvaux est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Grandvaux.getDateFinValidite().addMonths(-1));
		try {
			final Arrivee arrivee = (Arrivee) interne;

			final MessageCollector collector = buildMessageCollector();
			arrivee.validate(collector, collector);
			arrivee.handle(collector);

			if (collector.hasErreurs()) {
				fail("Une ou plusieurs erreurs sont survenues lors du traitement de l'arrivée : \n" + Arrays.toString(collector.getErreurs().toArray()));
			}

			final List<ForFiscal> fors = pp.getForsFiscauxSorted();
			assertNotNull(fors);
			assertEquals(2, fors.size());
			assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
			assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandvaux.getNoOFSEtendu(), MotifRattachement.DOMICILE,
					ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(1));
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}

	/**
	 * Vérifie que le for fiscal est bien ouvert sur la commune de Grandvaux (= dépends de l'egid du bâtiment) lorsqu'un habitant arrive dans Bourg-en-Lavaux pendant la période où les communes sont
	 * fusionnées au civil, mais pas encore au fiscal.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveeDansUneCommuneFusionneeAuCivilMaisPasAuFiscal() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2010, 9, 1); // fusion des communes au 1 juillet 2010

		// Crée un individu qui arrive dans une commune fusionnée pendant la période où elles sont fusionnées au civil, mais pas encore au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Zbinden", "Arnold", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				// note: la localité de Grandvaux fait partie de la commune fusionnée Bourg-en-Lavaux
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);
		hibernateTemplate.flush();

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe d'arrivée doit être traduit en un événement fiscal interne d'arrivée, pas de surprise ici.
		final EvenementCivilInterne interne = new ArriveeTranslationStrategy().create(externe, context, options);
		org.junit.Assert.assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Grandvaux est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Grandvaux.getDateFinValidite().addMonths(-1));
		try {
			final Arrivee arrivee = (Arrivee) interne;

			final MessageCollector collector = buildMessageCollector();
			arrivee.validate(collector, collector);
			arrivee.handle(collector);

			if (collector.hasErreurs()) {
				fail("Une ou plusieurs erreurs sont survenues lors du traitement de l'arrivée : \n" + Arrays.toString(collector.getErreurs().toArray()));
			}

			final List<ForFiscal> fors = pp.getForsFiscauxSorted();
			assertNotNull(fors);
			assertEquals(2, fors.size());
			assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
			assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandvaux.getNoOFSEtendu(), MotifRattachement.DOMICILE,
					ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(1));
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}

	/**
	 * Vérifie que un événement d'arrivée génère bien un warning si le contribuable arrive à dans Bourg-en-Lavaux pendant la période où les communes sont
	 * fusionnées au civil, mais pas encore au fiscal et que l'egid n'est pas mentionné dans l'adresse.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveeDansUneCommuneFusionneeAuCivilMaisPasAuFiscalSansEgid() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2010, 9, 1); // fusion des communes au 1 juillet 2010

		// Crée un individu qui arrive dans une commune fusionnée pendant la période où elles sont fusionnées au civil, mais pas encore au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Zbinden", "Arnold", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				// note: la localité de Grandvaux fait partie de la commune fusionnée Bourg-en-Lavaux
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Riex.RueDuCollege, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);
		hibernateTemplate.flush();

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe d'arrivée doit être traduit en un événement fiscal interne d'arrivée, pas de surprise ici.
		final EvenementCivilInterne interne = new ArriveeTranslationStrategy().create(externe, context, options);
		org.junit.Assert.assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		final Arrivee arrivee = (Arrivee) interne;

		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);
		try {
			arrivee.handle(collector);
			fail("Il aurait dû y avoir une erreur de validation sur la date de validité du for créé");
		}
		catch (ValidationException e) {
			assertEquals(1, e.getErrors().size());

			// la commune d'annonce est maintenant ignorée (car les événements civils RCPers ne connaissent pas cette notion...)
			// -> le for est tentativement créé sur la commune de Riex (et non plus sur la commune de Bourg-en-Lavaux)

			final ValidationMessage erreur = e.getErrors().get(0);
			assertEquals("La période de validité du for fiscal ForFiscalPrincipal (01.09.2010 - ?) dépasse " +
					"la période de validité de la commune Riex (5608) à laquelle il est assigné (? - 30.12.2010)", erreur.getMessage());
		}
	}

	/**
	 * Vérifie que le for fiscal est bien ouvert sur la commune de Bourg-en-Lavaux lorsqu'un habitant arrive dans Bourg-en-Lavaux après la date de fusion fiscale des communes,
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveeDansUneCommuneFusionneeAuCivilEtAuFiscal() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2011, 1, 1); // fusion des communes au 1 juillet 2010

		// Crée un individu qui arrive dans une commune fusionnée après la date de fusion fiscale
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Zbinden", "Arnold", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				// note: la localité de Grandvaux fait partie de la commune fusionnée Bourg-en-Lavaux
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);
		hibernateTemplate.flush();

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe d'arrivée doit être traduit en un événement fiscal interne d'arrivée, pas de surprise ici.
		final EvenementCivilInterne interne = new ArriveeTranslationStrategy().create(externe, context, options);
		org.junit.Assert.assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		final Arrivee arrivee = (Arrivee) interne;

		final MessageCollector collector = buildMessageCollector();
		arrivee.validate(collector, collector);
		arrivee.handle(collector);

		if (collector.hasErreurs()) {
			fail("Une ou plusieurs erreurs sont survenues lors du traitement de l'arrivée : \n" + Arrays.toString(collector.getErreurs().toArray()));
		}

		final List<ForFiscal> fors = pp.getForsFiscauxSorted();
		assertNotNull(fors);
		assertEquals(2, fors.size());
		assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
		assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.BourgEnLavaux.getNoOFSEtendu(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(1));
	}

	/**
	 * Vérifie que le for fiscal est bien ouvert sur la commune de Grandvaux (= dépends de l'egid du bâtiment) lorsqu'un habitant de Village déménage dans Bourg-en-Lavaux pendant la période où les
	 * communes sont fusionnées au civil, mais pas encore au fiscal.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveeEntreCommunesFusionneesAuCivilMaisPasAuFiscal() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2010, 9, 1);

		// Crée un individu qui déménage entre deux communes fusionnées pendant la période où elles sont fusionnées au civil, mais pas encore au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Hutter", "Marcel", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Villette.BatimentCheminDeCreuxBechet, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Grandvaux est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Grandvaux.getDateFinValidite().addMonths(-1));
		try {
			final PersonnePhysique pp = addHabitant(noInd);
			addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Villette);
			hibernateTemplate.flush();

			// Simule un événement de déménagement de la part de la commune fusionnée
			final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
					MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

			// L'événement fiscal externe de déménagement doit être traduit en un événement fiscal interne d'arrivée,
			// parce que - du point de vue fiscal - les communes n'ont pas encore fusionné.
			final EvenementCivilInterne interne = new DemenagementTranslationStrategy().create(externe, context, options);
			org.junit.Assert.assertNotNull(interne);
			assertInstanceOf(Arrivee.class, interne);

			final Arrivee arrivee = (Arrivee) interne;

			final MessageCollector collector = buildMessageCollector();
			arrivee.validate(collector, collector);
			arrivee.handle(collector);

			if (collector.hasErreurs()) {
				fail("Une ou plusieurs erreurs sont survenues lors du traitement de l'arrivée : \n" + Arrays.toString(collector.getErreurs().toArray()));
			}

			final List<ForFiscal> fors = pp.getForsFiscauxSorted();
			assertNotNull(fors);
			assertEquals(2, fors.size());
			assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MockCommune.Villette.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
			assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandvaux.getNoOFSEtendu(), MotifRattachement.DOMICILE,
					ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(1));

			assertEquals("Traité comme une arrivée car les communes Villette et Grandvaux ne sont pas encore fusionnées du point-de-vue fiscal.", externe.getCommentaireTraitement());
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}
}

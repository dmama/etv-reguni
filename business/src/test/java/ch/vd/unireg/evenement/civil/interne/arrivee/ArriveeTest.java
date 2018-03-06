package ch.vd.unireg.evenement.civil.interne.arrivee;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.unireg.adresse.AdresseCivile;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.evenement.civil.interne.demenagement.DemenagementTranslationStrategy;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCanton;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceCivilImpl;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MockTiersDAO;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.validation.fors.ForFiscalValidator;

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

	private static final Long NUMERO_INDIVIDU_INCONNU = 9999L;
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
	public void testInitIndividuSeul() throws Exception {

		final long numeroIndividu = 12345L;
		final RegDate dateArrivee = RegDate.get(2002, 3, 15);
		final RegDate dateVeilleArrivee = dateArrivee.getOneDayBefore();

		// Crée l'événement
		final EvenementCivilRegPP evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateArrivee, numeroIndividu, 0L, 1234, null);

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
		}, new MockTiersDAO());

		// Crée les données du mock service civil
		ServiceCivilService serviceCivil = new ServiceCivilImpl(infrastructureService, new MockServiceCivil() {
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
		});

		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, tiersDAO);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final ArriveePrincipale adapter = new ArriveePrincipale(evenement, context, options);

				assertEquals(MockLocalite.Lausanne.getNomAbrege(), adapter.getAncienneAdresse().getLocalite());
				assertEquals(MockCommune.Cossonay.getNomOfficiel(), adapter.getNouvelleCommune().getNomOfficiel());
			}
		});
	}

	@Test
	/**
	 * Teste les différents scénarios devant échouer au test de complétude de l'arrivée.
	 */
	public void testCheckCompleteness() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final MessageCollector collector = buildMessageCollector();

		// 1er test : individu seul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Individu individuSeul = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, date(2000, 12, 31));
				final ArriveePrincipale arrivee = createValidArrivee(individuSeul, DATE_VALIDE);
				arrivee.checkCompleteness(collector, collector);
				Assert.isTrue(collector.getErreurs().isEmpty(), "individu célibataire : ca n'aurait pas du causer une erreur");
			}
		});

		// 2ème test : individu marié seul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				collector.clear();
				final Individu individuMarieSeul = serviceCivil.getIndividu(NUMERO_INDIVIDU_MARIE_SEUL, date(2000, 12, 31));
				final ArriveePrincipale arrivee = createValidArrivee(individuMarieSeul, DATE_VALIDE);
				arrivee.checkCompleteness(collector, collector);
				Assert.isTrue(collector.getErreurs().isEmpty(), "individu célibataire marié seul : ca n'aurait pas du causer une erreur");
			}
		});
	}

	@Test
	/**
	 * Teste les différents scénarios devant échouer à la validation.
	 */
	public void testValidate() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final MessageCollector collector = buildMessageCollector();

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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
				                                DATE_ANTERIEURE_ANCIENNE_ADRESSE, commune.getNoOFS(), ancienneCommune, commune, ancienneAdresse, nouvelleAdresse, context);
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
		});
	}

	/**
	 * Teste les différentes exceptions acceptées pour le traitement d'une arrivée
	 */
	@Test
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
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Arrivee arrivee =
						new ArriveePrincipale(inconnu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, commune.getNoOFS(), null, commune, null, nouvelleAdressePrincipale, context);
				arrivee.validate(collector, collector);
				Assert.isTrue(collector.getErreurs().isEmpty(), "Le tiers rattaché à l'individu n'existe pas, mais ceci est un cas valide et aucune erreur n'aurait dû être déclenchée");
			}
		});

		/*
		 * 2ème test : événement avec le tiers correspondant au conjoint manquant
		 */
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				collector.clear();
				final ArriveePrincipale arrivee =
						new ArriveePrincipale(inconnu, conjoint, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, commune.getNoOFS(), null, commune, null, nouvelleAdressePrincipale, context);
				arrivee.validate(collector, collector);
				Assert.isTrue(collector.getErreurs().isEmpty(), "Le tiers rattaché au conjoint n'existe pas, mais ceci est un cas valide et aucune erreur n'aurait dû être déclenchée");
			}
		});

	}

	@Test
	public void testHandle() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, date(2000, 12, 31));

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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

				assertEquals(1, eventSender.getCount());
				assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(tiers).size());
			}
		});
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

		return new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, dateArrivee, commune.getNoOFS(), null, commune, null, nouvelleAdresse, context);
	}

	private Arrivee createValidArrivee(Individu individu, MockCommune communeAnnonce, MockCommune nouvelleCommune) {
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(DATE_VALIDE);

		return new ArriveePrincipale(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, communeAnnonce.getNoOFS(), null, nouvelleCommune, null, nouvelleAdresse, context);
	}

	/**
	 * [UNIREG-1603] Teste les différents cas de recherche de non-habitants
	 */
	@Test
	public void testFindNonHabitants() throws Exception {

		class Ids {
			Long jeanNomPrenomAnnule;
			Long jeanNomPrenomDesactive;
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

		setWantIndexationTiers(true);
		removeTiersIndexData();

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

				// autre cédric homonyme
				addIndividu(375342, date(1960, 1, 1), "Cédric", "Dupneu", true);
			}
		}

		final ServiceCivil civil = new ServiceCivil();
		serviceCivil.setUp(new ServiceCivil());

		doInNewTransaction(new TransactionCallback<Object>() {
			@Nullable
            @Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique jeanNomPrenomAnnule = addNonHabitant("Jean", "Dupneu", null, null);
				jeanNomPrenomAnnule.setAnnule(true);

				final PersonnePhysique jeanNomPrenomDesactive = addNonHabitant("Jean", "Dupneu", null, null);
				addForPrincipal(jeanNomPrenomDesactive, date(1980, 1, 1), MotifFor.MAJORITE, date(2012, 12, 31), MotifFor.ANNULATION, MockCommune.Renens);

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

				ids.jeanNomPrenomAnnule = jeanNomPrenomAnnule.getId();
				ids.jeanNomPrenomDesactive = jeanNomPrenomDesactive.getId();
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

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// Si on recherche un Jean Dupneu né le 1er janvier 1960 et de sexe masculin, on doit trouver tous les Jean Dupneu assujettis nés un 1er janvier 1960 <b>ou</b>
				// de date de naissance inconnue et de sexe masculin <b>ou</b> de sexe inconnu. On ne doit pas trouver les Jean Dupneu nés un autre jour ou avec un autre sexe.
				{
					final List<PersonnePhysique> list = Arrivee.findNonHabitants(context.getTiersService(), civil.jean, null).getValue();
					assertEquals(8, list.size());
					assertListContains(list, ids.jeanNomPrenomAssujetti, ids.jeanNomPrenomDateAssujetti, ids.jeanNomPrenomDateSexeAssujetti, ids.jeanNomPrenomSexeAssujetti,
					                   ids.jeanNomPrenom, ids.jeanNomPrenomDate, ids.jeanNomPrenomDateSexe, ids.jeanNomPrenomSexe);
				}

				// Si on recherche un Jaques Dupneu né le 1er janvier 1960 et de sexe masculin, on doit le trouver puisqu'il y en a qu'un et qu'il est complet.
				{
					final List<PersonnePhysique> list = Arrivee.findNonHabitants(context.getTiersService(), civil.jacques, null).getValue();
					assertEquals(1, list.size());
					assertListContains(list, ids.jacquesNomPrenomDateSexeAssujetti);
				}

				// [UNIREG-3073] Si on recherche un Roger Dupneu né le 1er janvier 1960 et de sexe masculin, on doit trouver le seul candidat malgré le fait qu'il ne possède pas de date de naissance
				{
					final List<PersonnePhysique> list = Arrivee.findNonHabitants(context.getTiersService(), civil.roger, null).getValue();
					assertEquals(1, list.size());
					assertListContains(list, ids.rogerNomPrenomSexeAssujetti);
				}

				// Si on recherche un Cédric Dupneu né le 1er janvier 1960 et de sexe masculin, on ne doit pas le trouver parce que
				// le candidat possède un numéro d'individu (malgré le fait que tous les critères de recherche correspondent bien)
				{
					final List<PersonnePhysique> list = Arrivee.findNonHabitants(context.getTiersService(), civil.cedric, null).getValue();
					assertEmpty(list);
				}

				// [SIFISC-4876] Si on recherche Jean Pierre, il y en a bcp trop pour l'indexeur; qui doit lever une exception catchée et réemballée dans
				// dans une EvenementCivilException avec un joli message compréhensible pour l'utilisateur
				try {
					Arrivee.findNonHabitants(context.getTiersService(), civil.pierreJean, null);
					fail("Le dernier appel doit lever une exception");
				}
				catch (EvenementCivilException e) {
					assertEquals("Trop de non-habitants (110 au total) correspondent à: Jean Pierre", e.getMessage());
				}
			}
		});
    }

	private void assertListContains(List<PersonnePhysique> list, Long... ids) {
		assertNotNull(list);
		final Set<Long> set = new HashSet<>();
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
	public void testArriveeDansUneCommunePasEncoreFusionneeAuCivilNiAuFiscal() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2009, 9, 1); // fusion des communes au 1 juillet 2010

		// Crée un individu qui arrive dans une commune pas encore fusionnée au civil ni au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Zbinden", "Arnold", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);
				return pp.getNumero();
			}
		});

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFS(), null);

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Grandvaux est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Grandvaux.getDateFinValidite().addMonths(-1));
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws Exception {
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

					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
					final List<ForFiscal> fors = pp.getForsFiscauxSorted();
					assertNotNull(fors);
					assertEquals(2, fors.size());
					assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					                   MockCommune.Echallens.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) fors.get(0));
					assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandvaux.getNoOFS(), MotifRattachement.DOMICILE,
					                   ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) fors.get(1));
				}
			});
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
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);
				return pp.getNumero();
			}
		});

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFS(), null);

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Grandvaux est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Grandvaux.getDateFinValidite().addMonths(-1));
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws Exception {
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

					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
					final List<ForFiscal> fors = pp.getForsFiscauxSorted();
					assertNotNull(fors);
					assertEquals(2, fors.size());
					assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					                   MockCommune.Echallens.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) fors.get(0));
					assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandvaux.getNoOFS(), MotifRattachement.DOMICILE,
					                   ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) fors.get(1));
				}
			});
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

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);
				return pp.getNumero();
			}
		});

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFS(), null);

		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws Exception {
					// L'événement fiscal externe d'arrivée doit être traduit en un événement fiscal interne d'arrivée, pas de surprise ici.
					final EvenementCivilInterne interne = new ArriveeTranslationStrategy().create(externe, context, options);
					org.junit.Assert.assertNotNull(interne);
					assertInstanceOf(Arrivee.class, interne);

					final Arrivee arrivee = (Arrivee) interne;

					final MessageCollector collector = buildMessageCollector();
					arrivee.validate(collector, collector);
					arrivee.handle(collector);
				}
			});
			fail("Il aurait dû y avoir une erreur de validation sur la date de validité du for créé");
		}
		catch (ValidationException e) {
			assertEquals(1, e.getErrors().size());

			// la commune d'annonce est maintenant ignorée (car les événements civils RCPers ne connaissent pas cette notion...)
			// -> le for est tentativement créé sur la commune de Riex (et non plus sur la commune de Bourg-en-Lavaux)

			final ValidationMessage erreur = e.getErrors().get(0);
			assertEquals("Le for fiscal ForFiscalPrincipalPP (01.09.2010 - ?) a une période de validité qui dépasse " +
					             "la période de validité [ ; 31.12.2010] de la commune Riex (5608) depuis le 01.01.2011", erreur.getMessage());
		}
	}

	/**
	 * Vérifie que le for fiscal est bien ouvert sur la commune de Bourg-en-Lavaux lorsqu'un habitant arrive dans Bourg-en-Lavaux après la date de fusion fiscale des communes,
	 */
	@Test
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
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);
				return pp.getNumero();
			}
		});

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFS(), null);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<ForFiscal> fors = pp.getForsFiscauxSorted();
				assertNotNull(fors);
				assertEquals(2, fors.size());
				assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                   MockCommune.Echallens.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) fors.get(0));
				assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.BourgEnLavaux.getNoOFS(), MotifRattachement.DOMICILE,
				                   ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) fors.get(1));
			}
		});
	}

	/**
	 * Vérifie que le for fiscal est bien ouvert sur la commune de Grandvaux (= dépends de l'egid du bâtiment) lorsqu'un habitant de Village déménage dans Bourg-en-Lavaux pendant la période où les
	 * communes sont fusionnées au civil, mais pas encore au fiscal.
	 */
	@Test
	public void testArriveeEntreCommunesFusionneesAuCivilMaisPasAuFiscal() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2010, 9, 1);

		// Crée un individu qui déménage entre deux communes fusionnées pendant la période où elles sont fusionnées au civil, mais pas encore au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Hutter", "Marcel", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Villette.BatimentCheminDeCreuxBechet, null, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		final long ppId = doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Villette);
				return pp.getNumero();
			}
		});

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Grandvaux est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Grandvaux.getDateFinValidite().addMonths(-1));
		try {
			// Simule un événement de déménagement de la part de la commune fusionnée
			final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
					MockCommune.BourgEnLavaux.getNoOFS(), null);

			// L'événement fiscal externe de déménagement doit être traduit en un événement fiscal interne d'arrivée,
			// parce que - du point de vue fiscal - les communes n'ont pas encore fusionné.
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws Exception {
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

					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
					final List<ForFiscal> fors = pp.getForsFiscauxSorted();
					assertNotNull(fors);
					assertEquals(2, fors.size());
					assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					                   MockCommune.Villette.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) fors.get(0));
					assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandvaux.getNoOFS(), MotifRattachement.DOMICILE,
					                   ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) fors.get(1));

					assertEquals("Traité comme une arrivée car les communes Villette et Grandvaux ne sont pas encore fusionnées du point de vue fiscal.", externe.getCommentaireTraitement());
				}
			});
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}
}

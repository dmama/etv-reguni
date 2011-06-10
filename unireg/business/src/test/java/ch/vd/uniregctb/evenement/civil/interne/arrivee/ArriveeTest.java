package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.demenagement.DemenagementTranslationStrategy;
import ch.vd.uniregctb.interfaces.model.Adresse;
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
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
	public void testInitIndividuSeul() throws Exception {

		final long numeroIndividu = 12345L;
		final RegDate dateArrivee = RegDate.get(2002, 3, 15);
		final RegDate dateVeilleArrivee = dateArrivee.getOneDayBefore();

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(numeroIndividu);

		// Crée l'événement
		EvenementCivilExterne
				evenement = new EvenementCivilExterne(1L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateArrivee, numeroIndividu, habitant, 0L, null, 1234, null);

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

		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService);
		Arrivee adapter = new Arrivee(evenement, context, options);

		assertEquals(MockLocalite.Lausanne.getNomAbregeMinuscule(), adapter.getAncienneAdressePrincipale().getLocalite());
		assertEquals(MockCommune.Cossonay.getNomMinuscule(), adapter.getNouvelleCommunePrincipale().getNomMinuscule());
	}

	@Test
	/**
	 * Teste les différents scénarios devant échouer au test de complétude de l'arrivée.
	 */
	public void testCheckCompleteness() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		// 1er test : individu seul
		final Individu individuSeul = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000);
		Arrivee arrivee = createValidArrivee(individuSeul, DATE_VALIDE);
		arrivee.checkCompleteness(erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "individu célibataire : ca n'aurait pas du causer une erreur");

		// 2ème test : individu marié seul
		final Individu individuMarieSeul = serviceCivil.getIndividu(NUMERO_INDIVIDU_MARIE_SEUL, 2000);
		arrivee = createValidArrivee(individuMarieSeul, DATE_VALIDE);
		arrivee.checkCompleteness(erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "individu célibataire marié seul : ca n'aurait pas du causer une erreur");
	}

	@Test
	/**
	 * Teste les différents scénarios devant échouer à la validation.
	 */
	public void testValidate() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		// 1er test : événement avec une date dans le futur
		Arrivee arrivee = createValidArrivee(serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000), DATE_FUTURE);
		arrivee.validate(erreurs, warnings);
		Assert.notEmpty(erreurs, "Une date future pour l'événement aurait dû renvoyer une erreur");

		// 2ème test : arrivée antérieur à la date de début de validité de
		// l'ancienne adresse
		erreurs.clear();
		warnings.clear();

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

		arrivee = new Arrivee(serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000), null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS,
				DATE_ANTERIEURE_ANCIENNE_ADRESSE, commune.getNoOFSEtendu(), ancienneCommune, commune, ancienneAdresse, nouvelleAdresse, context);
		arrivee.validate(erreurs, warnings);
		Assert.notEmpty(erreurs,
				"L'arrivée est antérieur à la date de début de validité de l'ancienne adresse, une erreur aurait du être déclenchée");

		// 3ème test : arrivée hors canton
		erreurs.clear();
		warnings.clear();
		arrivee = createValidArrivee(serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000), MockCommune.Neuchatel, DATE_VALIDE);
		arrivee.validate(erreurs, warnings);
		Assert.notEmpty(erreurs, "L'arrivée est hors canton, une erreur aurait du être déclenchée");

		// 4ème test : commune du Sentier -> traitement manuel dans tous les cas
		erreurs.clear();
		warnings.clear();
		arrivee = createValidArrivee(serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000), MockCommune.LeChenit, MockCommune.Fraction.LeSentier);
		arrivee.validate(erreurs, warnings);
		Assert.isTrue(warnings.size() == 1, "L'arrivée est dans la commune du sentier, un warning aurait du être déclenchée");
	}

	/**
	 * Teste les différentes exceptions acceptées pour le traitement d'une arrivée
	 */
	@Test
	public void testValidateException() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		/*
		 * 1er test : événement avec le tiers correspondant à l'individu manquant
		 */
		MockIndividu inconnu = new MockIndividu();
		inconnu.setDateNaissance(RegDate.get(1953, 11, 2));
		inconnu.setNoTechnique(NUMERO_INDIVIDU_INCONNU);
		final MockCommune commune = MockCommune.Lausanne;
		Arrivee arrivee = new Arrivee(inconnu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, commune.getNoOFSEtendu(), null, commune, null, (Adresse)null, context);
		arrivee.validate(erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "Le tiers rattaché à l'individu n'existe pas, mais ceci est un cas valide et aucune erreur n'aurait dû être déclenchée");

		/*
		 * 2ème test : événement avec le tiers correspondant au conjoint manquant
		 */
		erreurs.clear();
		warnings.clear();
		MockIndividu individu = new MockIndividu();
		individu.setConjoint(inconnu);
		individu.setDateNaissance(RegDate.get(1953, 11, 2));
		arrivee = new Arrivee(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, commune.getNoOFSEtendu(), null, commune, null, (Adresse)null, context);
		arrivee.validate(erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "Le tiers rattaché au conjoint n'existe pas, mais ceci est un cas valide et aucune erreur n'aurait dû être déclenchée");

	}

	@Test
	public void testHandle() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000);
		Arrivee arrivee = createValidArrivee(individu, DATE_VALIDE);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		arrivee.checkCompleteness(erreurs, warnings);
		arrivee.validate(erreurs, warnings);
		arrivee.handle(warnings);

		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement de l'arrivée");

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

	private Arrivee createValidArrivee(Individu individu, RegDate dateArrivee) {

		// Anciennes adresses
		/*MockAdresse ancienneAdresse = new MockAdresse();
		ancienneAdresse.setDateDebutValidite(DATE_ANCIENNE_ADRESSE);
		ancienneAdresse.setPays(MockPays.France);
		arrivee.setAncienneAdressePrincipale(ancienneAdresse);*/

		// Nouvelles adresses
		final MockCommune commune = MockCommune.Lausanne;

		return createValidArrivee(individu, commune, dateArrivee);
	}

	private Arrivee createValidArrivee(Individu individu, MockCommune commune, RegDate dateArrivee) {
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateArrivee);

		return new Arrivee(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, dateArrivee, commune.getNoOFSEtendu(), null, commune, null, nouvelleAdresse, context);
	}

	private Arrivee createValidArrivee(Individu individu, MockCommune communeAnnonce, MockCommune nouvelleCommune) {
		final MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(DATE_VALIDE);

		return new Arrivee(individu, null, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, DATE_VALIDE, communeAnnonce.getNoOFSEtendu(), null, nouvelleCommune, null, nouvelleAdresse, context);
	}

	/**
	 * [UNIREG-1603] Teste les différents cas de recherche de non-habitants
	 */
	@Test
	public void testFindNonHabitants() throws Exception {

		final Arrivee arrivee = new Arrivee(null, null, null, null, null, null, null, null, null, context);

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
				return null;
			}
		});

		globalTiersIndexer.sync();

		class ServiceCivil extends MockServiceCivil {

			private MockIndividu jean;
			private MockIndividu jacques;
			private MockIndividu roger;
			private MockIndividu cedric;

			@Override
			protected void init() {
				jean = addIndividu(343434, date(1960, 1, 1), "Jean", "Dupneu", true);
				jacques = addIndividu(747474, date(1960, 1, 1), "Jacques", "Dupneu", true);
				roger = addIndividu(585858, date(1960, 1, 1), "Roger", "Dupneu", true);
				cedric = addIndividu(9191919, date(1960, 1, 1), "Cédric", "Dupneu", true);
			}
		}

		final ServiceCivil civil = new ServiceCivil();
		serviceCivil.setUp(civil);


		// Si on recherche un Jean Dupneu né le 1er janvier 1960 et de sexe masculin, on doit trouver tous les Jean Dupneu assujettis nés un 1er janvier 1960 <b>ou</b>
		// de date de naissance inconnue et de sexe masculin <b>ou</b> de sexe inconnu. On ne doit pas trouver les Jean Dupneu nés un autre jour ou avec un autre sexe.
		{
			final List<PersonnePhysique> list = arrivee.findNonHabitants(civil.jean, true);
			assertEquals(4, list.size());
			assertListContains(list, ids.jeanNomPrenomAssujetti, ids.jeanNomPrenomDateAssujetti, ids.jeanNomPrenomDateSexeAssujetti, ids.jeanNomPrenomSexeAssujetti);
		}

		// Si on recherche un Jaques Dupneu né le 1er janvier 1960 et de sexe masculin, on doit le trouver puisqu'il y en a qu'un et qu'il est complet.
		{
			final List<PersonnePhysique> list = arrivee.findNonHabitants(civil.jacques, true);
			assertEquals(1, list.size());
			assertListContains(list, ids.jacquesNomPrenomDateSexeAssujetti);
		}

		// [UNIREG-3073] Si on recherche un Roger Dupneu né le 1er janvier 1960 et de sexe masculin, on doit trouver le seul candidat malgré le fait qu'il ne possède pas de date de naissance
		{
			final List<PersonnePhysique> list = arrivee.findNonHabitants(civil.roger, true);
			assertEquals(1, list.size());
			assertListContains(list, ids.rogerNomPrenomSexeAssujetti);
		}

		// Si on recherche un Cédric Dupneu né le 1er janvier 1960 et de sexe masculin, on ne doit pas le trouver parce que
		// le candidat possède un numéro d'individu (malgré le fait que tous les critères de recherche correspondent bien)
		{
			final List<PersonnePhysique> list = arrivee.findNonHabitants(civil.cedric, true);
			assertEmpty(list);
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
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null, 1);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilExterne externe = new EvenementCivilExterne(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, pp, null, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe d'arrivée doit être traduit en un événement fiscal interne d'arrivée, pas de surprise ici.
		final EvenementCivilInterne interne = new ArriveeTranslationStrategy().create(externe, context, options);
		org.junit.Assert.assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		final Arrivee arrivee = (Arrivee) interne;

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		arrivee.checkCompleteness(erreurs, warnings);
		arrivee.validate(erreurs, warnings);
		arrivee.handle(warnings);

		if (!erreurs.isEmpty()) {
			fail("Une ou plusieurs erreurs sont survenues lors du traitement de l'arrivée : \n" + Arrays.toString(erreurs.toArray()));
		}

		final List<ForFiscal> fors = pp.getForsFiscauxSorted();
		assertNotNull(fors);
		assertEquals(2, fors.size());
		assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
		assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandvaux.getNoOFSEtendu(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(1));
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
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null, 1);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilExterne externe = new EvenementCivilExterne(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, pp, null, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe d'arrivée doit être traduit en un événement fiscal interne d'arrivée, pas de surprise ici.
		final EvenementCivilInterne interne = new ArriveeTranslationStrategy().create(externe, context, options);
		org.junit.Assert.assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		final Arrivee arrivee = (Arrivee) interne;

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		arrivee.checkCompleteness(erreurs, warnings);
		arrivee.validate(erreurs, warnings);
		arrivee.handle(warnings);

		if (!erreurs.isEmpty()) {
			fail("Une ou plusieurs erreurs sont survenues lors du traitement de l'arrivée : \n" + Arrays.toString(erreurs.toArray()));
		}

		final List<ForFiscal> fors = pp.getForsFiscauxSorted();
		assertNotNull(fors);
		assertEquals(2, fors.size());
		assertForPrincipal(date(1990, 1, 1), MotifFor.MAJORITE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(0));
		assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandvaux.getNoOFSEtendu(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, (ForFiscalPrincipal) fors.get(1));
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
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, dateDemenagement, null);
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null, 1);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Echallens);

		// Simule un événement d'arrivée de la part de la commune fusionnée
		final EvenementCivilExterne externe = new EvenementCivilExterne(0L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, pp, null, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe d'arrivée doit être traduit en un événement fiscal interne d'arrivée, pas de surprise ici.
		final EvenementCivilInterne interne = new ArriveeTranslationStrategy().create(externe, context, options);
		org.junit.Assert.assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		final Arrivee arrivee = (Arrivee) interne;

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		arrivee.checkCompleteness(erreurs, warnings);
		arrivee.validate(erreurs, warnings);
		arrivee.handle(warnings);

		if (!erreurs.isEmpty()) {
			fail("Une ou plusieurs erreurs sont survenues lors du traitement de l'arrivée : \n" + Arrays.toString(erreurs.toArray()));
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
				addNationalite(ind, MockPays.Suisse, date(1970, 1, 1), null, 1);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Villette);

		// Simule un événement de déménagement de la part de la commune fusionnée
		final EvenementCivilExterne externe = new EvenementCivilExterne(0L, TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, pp, null, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe de déménagement doit être traduit en un événement fiscal interne d'arrivée,
		// parce que - du point de vue fiscal - les communes n'ont pas encore fusionné.
		final EvenementCivilInterne interne = new DemenagementTranslationStrategy().create(externe, context, options);
		org.junit.Assert.assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		final Arrivee arrivee = (Arrivee) interne;

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		arrivee.checkCompleteness(erreurs, warnings);
		arrivee.validate(erreurs, warnings);
		arrivee.handle(warnings);

		if (!erreurs.isEmpty()) {
			fail("Une ou plusieurs erreurs sont survenues lors du traitement de l'arrivée : \n" + Arrays.toString(erreurs.toArray()));
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
}

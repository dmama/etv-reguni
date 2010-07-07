package ch.vd.uniregctb.evenement.arrivee;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * <b>Note:</b> La plupart des tests de l'arrivée handler sont dans la class {@link ArriveeHandlerExtTest}.
 */
@SuppressWarnings({"JavaDoc"})
public class ArriveeHandlerTest extends AbstractEvenementHandlerTest {

	private static final String DB_UNIT_DATA_FILE = "ArriveeHandlerTest.xml";

	/**
	 *
	 */
	private static final Long NUMERO_INDIVIDU = 12345l;
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
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	/**
	 * Teste les différents scénarios devant échouer au test de complétude de l'arrivée.
	 */
	public void testCheckCompleteness() throws Exception {

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		// 1er test : individu seul
		MockIndividu individuSeul = (MockIndividu) serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000);
		MockArrivee arrivee = createValidArrivee(individuSeul);
		evenementCivilHandler.checkCompleteness(arrivee, erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "individu célibataire : ca n'aurait pas du causer une erreur");

		// 2ème test : individu marié seul
		MockIndividu individuMarieSeul = (MockIndividu) serviceCivil.getIndividu(NUMERO_INDIVIDU_MARIE_SEUL, 2000);
		arrivee = createValidArrivee(individuMarieSeul);
		evenementCivilHandler.checkCompleteness(arrivee, erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "individu célibataire marié seul : ca n'aurait pas du causer une erreur");
	}

	@Test
	/**
	 * Teste les différents scénarios devant échouer à la validation.
	 */
	public void testValidate() throws Exception {

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		// 1er test : évènement avec une date dans le futur
		MockArrivee arrivee = createValidArrivee((MockIndividu) serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000));
		arrivee.setDate(DATE_FUTURE);
		evenementCivilHandler.validate(arrivee, erreurs, warnings);
		Assert.notEmpty(erreurs, "Une date future pour l'évènement aurait du renvoyer une erreur");

		// 2ème test : arrivée antérieur à la date de début de validité de
		// l'ancienne adresse
		erreurs.clear();
		warnings.clear();
		arrivee = createValidArrivee((MockIndividu) serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000));
		MockAdresse ancienneAdresse = new MockAdresse();
		ancienneAdresse.setDateDebutValidite(DATE_ANCIENNE_ADRESSE);
		ancienneAdresse.setPays(MockPays.Suisse);
		final MockCommune ancienneCommune = MockCommune.Cossonay;
		ancienneAdresse.setCommuneAdresse(ancienneCommune);
		arrivee.setAncienneAdressePrincipale(ancienneAdresse);
		arrivee.setAncienneCommunePrincipale(ancienneCommune);
		arrivee.setDate(DATE_ANTERIEURE_ANCIENNE_ADRESSE);
		evenementCivilHandler.validate(arrivee, erreurs, warnings);
		Assert.notEmpty(erreurs,
				"L'arrivée est antérieur à la date de début de validité de l'ancienne adresse, une erreur aurait du être déclenchée");

		// 3ème test : arrivée hors canton
		erreurs.clear();
		warnings.clear();
		arrivee = createValidArrivee((MockIndividu) serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000));
		arrivee.setNouvelleCommunePrincipale(MockCommune.Neuchatel);
		arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Neuchatel.getNoOFSEtendu());
		evenementCivilHandler.validate(arrivee, erreurs, warnings);
		Assert.notEmpty(erreurs, "L'arrivée est hors canton, une erreur aurait du être déclenchée");

		// 4ème test : commune du Sentier -> traitement manuel dans tous les cas
		erreurs.clear();
		warnings.clear();
		arrivee = createValidArrivee((MockIndividu) serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000));
		arrivee.setNouvelleCommunePrincipale(MockCommune.Fraction.LeSentier);
		arrivee.setNumeroOfsCommuneAnnonce(MockCommune.LeChenit.getNoOFSEtendu());
		evenementCivilHandler.validate(arrivee, erreurs, warnings);
		Assert.isTrue(warnings.size() == 1, "L'arrivée est dans la commune du sentier, un warning aurait du être déclenchée");
	}

	/**
	 * Teste les différentes exceptions acceptées pour le traitement d'une arrivée
	 */
	@Test
	public void testValidateException() throws Exception {

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		/*
		 * 1er test : évènement avec le tiers correspondant à l'individu manquant
		 */
		MockIndividu inconnu = new MockIndividu();
		inconnu.setDateNaissance(RegDate.get(1953, 11, 2));
		inconnu.setNoTechnique(NUMERO_INDIVIDU_INCONNU);
		MockArrivee arrivee = createValidArrivee(inconnu);
		arrivee.setAdresseCourrier(null);
		arrivee.setAdressePrincipale(null);
		evenementCivilHandler.validate(arrivee, erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "Le tiers rattaché à l'individu n'existe pas, mais ceci est un cas valide et aucune erreur n'aurait dû être déclenchée");

		/*
		 * 2ème test : évènement avec le tiers correspondant au conjoint manquant
		 */
		erreurs.clear();
		warnings.clear();
		MockIndividu individu = new MockIndividu();
		individu.setConjoint(inconnu);
		individu.setDateNaissance(RegDate.get(1953, 11, 2));
		arrivee = createValidArrivee(individu);
		arrivee.setAdresseCourrier(null);
		arrivee.setAdressePrincipale(null);
		evenementCivilHandler.validate(arrivee, erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "Le tiers rattaché au conjoint n'existe pas, mais ceci est un cas valide et aucune erreur n'aurait dû être déclenchée");

	}

	@Test
	public void testHandle() throws Exception {


		final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000);
		Arrivee arrivee = createValidArrivee(individu);
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(arrivee, erreurs, warnings);
		evenementCivilHandler.validate(arrivee, erreurs, warnings);
		evenementCivilHandler.handle(arrivee, warnings);

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
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(tiers).size());
	}

	private MockArrivee createValidArrivee(MockIndividu individu) {

		MockArrivee arrivee = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		if (individu.getNoTechnique() == 0) {
			individu.setNoTechnique(NUMERO_INDIVIDU);
		}
		arrivee.setIndividu(individu);

		// Anciennes adresses
		/*MockAdresse ancienneAdresse = new MockAdresse();
		ancienneAdresse.setDateDebutValidite(DATE_ANCIENNE_ADRESSE);
		ancienneAdresse.setPays(MockPays.France);
		arrivee.setAncienneAdressePrincipale(ancienneAdresse);*/

		// Nouvelles adresses
		MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(DATE_VALIDE);
		arrivee.setNouvelleAdressePrincipale(nouvelleAdresse);

		final MockCommune commune = MockCommune.Lausanne;
		arrivee.setNouvelleCommunePrincipale(commune);

		arrivee.setNumeroOfsCommuneAnnonce(commune.getNoOFSEtendu());
		arrivee.setDate(DATE_VALIDE);
		arrivee.setType(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS);

		return arrivee;
	}

}

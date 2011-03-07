package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.evenement.AbstractEvenementAdapterTest;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests unitaires du handler du déménagement.
 *
 * @author Céline GRAND
 *
 */
@ContextConfiguration(locations = {
	BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class DemenagementAdapterTest extends AbstractEvenementAdapterTest {

	private static final Logger LOGGER = Logger.getLogger(DemenagementAdapterTest.class);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "DemenagementAdapterTest.xml";

	/**
	 *
	 */
	private static final Long NUMERO_INDIVIDU_SEUL = 34567L; //sophie
	private static final Long NUMERO_INDIVIDU_MARIE_SEUL = 12345L; //pierre
	private static final Long NUMERO_INDIVIDU_MARIE1 = 54321L; //momo
	private static final Long NUMERO_INDIVIDU_MARIE2 = 23456L; //bea

	private static final RegDate DATE_VALIDE = RegDate.get(2007, 11, 19);
	private static final RegDate DATE_LIMITE = RegDate.get(2007, 12, 28);
	private static final RegDate DATE_JANVIER = RegDate.get(2008, 01, 01);
	private static final RegDate DATE_ANCIENNE_ADRESSE = RegDate.get(1970, 11, 19);
	private static final RegDate DATE_ANTERIEURE_ANCIENNE_ADRESSE = RegDate.get(1940, 11, 19);

	private static final String CANTON_GENEVE = "GE";

	private TacheDAO tacheDAO;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		tacheDAO = getBean(TacheDAO.class, "tacheDAO");

		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	/**
	 * Teste les différents scénarios devant échouer à la validation.
	 */
	public void testValidate() throws Exception {
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		// 1er test : déménagement hors canton
		LOGGER.debug("Test déménagement hors canton...");
		erreurs.clear();
		warnings.clear();
		DemenagementAdapter demenagement = createValidDemenagement(new MockIndividu(), null, DATE_VALIDE, MockCommune.Neuchatel);
		demenagement.validate(erreurs, warnings);
		Assert.notEmpty(erreurs, "Le déménagement est hors canton, une erreur aurait du être déclenchée");
		LOGGER.debug("Test déménagement hors canton : OK");

		// 2ème test : déménagement antérieur à la date de début de validité de
		// l'ancienne adresse
		LOGGER.debug("Test déménagement antérieur à la date de début de validité de l'ancienne adresse...");
		erreurs.clear();
		warnings.clear();
		demenagement = createValidDemenagement(new MockIndividu(), null, DATE_ANTERIEURE_ANCIENNE_ADRESSE, MockCommune.Fraction.LePont);
		demenagement.validate(erreurs, warnings);
		Assert.notEmpty(erreurs,
				"Le déménagement est antérieur à la date de début de validité de l'ancienne adresse, une erreur aurait du être déclenchée");
		LOGGER.debug("Test déménagement antérieur à la date de début de validité de l'ancienne adresse : OK");

	}

	@Test
	public void testIndividuSeulHandle() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de déménagement individu seul.");
		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000);
		DemenagementAdapter demenagement = createValidDemenagement(individu, null, DATE_VALIDE, MockCommune.Fraction.LePont);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		demenagement.checkCompleteness(erreurs, warnings);
		demenagement.validate(erreurs, warnings);
		demenagement.handle(warnings);

		assertEmpty("Une erreur est survenue lors du traitement de déménagement", erreurs);

		PersonnePhysique sophie = tiersDAO.getHabitantByNumeroIndividu(demenagement.getNoIndividu());
		assertNotNull(sophie);
		assertEquals("Sophie doit avoir deux fors fiscaux", 2, sophie.getForsFiscaux().size());
		assertNotNull("Sophie devrait encore avoir un for principal actif après le déménagement", sophie.getForFiscalPrincipalAt(null));
		assertEquals("date d'ouverture du for incorrecte", 0, sophie.getForFiscalPrincipalAt(null).getDateDebut().compareTo(DATE_VALIDE) );

		LOGGER.debug("Test de traitement d'un événement de déménagement individu seul OK");
	}

	@Test
	public void testIndividuSeulFinDecembreHandle() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de déménagement fin décembre individu seul.");
		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000);
		DemenagementAdapter demenagement = createValidDemenagement(individu, null, DATE_LIMITE, MockCommune.Fraction.LePont);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		demenagement.checkCompleteness(erreurs, warnings);
		demenagement.validate(erreurs, warnings);
		demenagement.handle(warnings);

		assertEmpty("Une erreur est survenue lors du traitement de déménagement", erreurs);

		PersonnePhysique sophie = tiersDAO.getHabitantByNumeroIndividu(demenagement.getNoIndividu());
		assertTrue(sophie != null);
		assertEquals("Sophie doit avoir deux fors fiscaux", 2, sophie.getForsFiscaux().size());
		assertNotNull("Sophie devrait encore avoir un for principal actif après le déménagement", sophie.getForFiscalPrincipalAt(null));
		assertEquals("date d'ouverture du for incorrecte", 0, sophie.getForFiscalPrincipalAt(null).getDateDebut().compareTo(DATE_JANVIER));

		LOGGER.debug("Test de traitement d'un événement de déménagement fin décembre individu seul OK");
	}

	@Test
	public void testIndividuMarieSeulHandle() throws Exception {
		LOGGER.debug("Test de traitement d'un événement de déménagement individu marié seul.");
		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_MARIE_SEUL, 2000);
		DemenagementAdapter demenagement = createValidDemenagement(individu, null, DATE_VALIDE, MockCommune.Fraction.LePont);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		demenagement.checkCompleteness(erreurs, warnings);
		demenagement.validate(erreurs, warnings);
		demenagement.handle(warnings);

		assertEmpty("Une erreur est survenue lors du traitement de déménagement", erreurs);

		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(demenagement.getNoIndividu());
		assertNotNull(pierre);
		assertEquals("Pierre ne doit pas posséder de fors fiscaux", 0, pierre.getForsFiscaux().size());
		assertNull("Pierre ne devrait pas avoir un for principal actif", pierre.getForFiscalPrincipalAt(null));

		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : pierre.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);
		assertEquals("le ménage commun de Pierre devrait posséder 3 fors fiscaux" , 3, menageCommun.getForsFiscaux().size());
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("le for fiscal du ménage commun de Pierre n'existe plus", forCommun);
		assertEquals("date d'ouverture du for incorrecte" , 0, forCommun.getDateDebut().compareTo(DATE_VALIDE));

		LOGGER.debug("Test de traitement d'un événement de déménagement individu marié seul OK");
	}

	@Test
	public void testIndividuMarieHandle() throws Exception {
		LOGGER.debug("Test de traitement d'un événement de déménagement individu marié .");
		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_MARIE1, 2000);
		final Individu individu2 = serviceCivil.getIndividu(NUMERO_INDIVIDU_MARIE2, 2000);
		DemenagementAdapter demenagement = createValidDemenagement(individu, individu2, DATE_VALIDE, MockCommune.Fraction.LePont);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		demenagement.checkCompleteness(erreurs, warnings);
		demenagement.validate(erreurs, warnings);
		demenagement.handle(warnings);

		assertEmpty("Une erreur est survenue lors du traitement de déménagement", erreurs);

		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(demenagement.getNoIndividu());
		assertNotNull(momo);
		assertEquals("Maurice ne doit pas posséder de fors fiscaux", 0, momo.getForsFiscaux().size());
		assertNull("Maurice ne devrait pas avoir un for principal actif", momo.getForFiscalPrincipalAt(null));

		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : momo.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);
		assertEquals("le ménage commun de Maurice devrait posséder 2 fors fiscaux" , 2, menageCommun.getForsFiscaux().size());
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("le for fiscal du ménage commun de Maurice n'existe plus", forCommun);
		assertEquals("date d'ouverture du for incorrecte" , 0, forCommun.getDateDebut().compareTo(DATE_VALIDE));

		LOGGER.debug("Test de traitement d'un événement de déménagement individu marié OK");
	}

	@Test
	public void testDemenagementVaudoisSansChangementOfficeImpot() {
		LOGGER.debug("Test de traitement d'un événement de déménagement vaudois sans changement d'office d'impot.");
		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000);
		DemenagementAdapter demenagement = createValidDemenagement(individu, null, DATE_VALIDE, MockCommune.Fraction.LePont);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		int tachesAvant;
		{
			PersonnePhysique sophie = tiersDAO.getHabitantByNumeroIndividu(demenagement.getNoIndividu());
			assertNotNull(sophie);
			tachesAvant = tacheDAO.count(sophie.getNumero());
		}
		demenagement.checkCompleteness(erreurs, warnings);
		demenagement.validate(erreurs, warnings);
		demenagement.handle(warnings);

		assertEmpty("Une erreur est survenue lors du traitement de déménagement", erreurs);

		PersonnePhysique sophie = tiersDAO.getHabitantByNumeroIndividu(demenagement.getNoIndividu());
		assertNotNull(sophie);
		assertEquals("Sophie doit avoir deux fors fiscaux", 2, sophie.getForsFiscaux().size());
		assertNotNull("Sophie devrait encore avoir un for principal actif après le déménagement", sophie.getForFiscalPrincipalAt(null));
		assertEquals("date d'ouverture du for incorrecte", 0, sophie.getForFiscalPrincipalAt(null).getDateDebut().compareTo(DATE_VALIDE) );

		int tachesApres = tacheDAO.count(sophie.getNumero());
		assertEquals("Il aurait dû y avoir une nouvelle tâche de contrôle de dossier", 1, tachesApres - tachesAvant);

	}


@Test
	public void testDemenagementVaudoisAvecChangementOfficeImpot() {
		LOGGER.debug("Test de traitement d'un événement de déménagement vaudois sans changement d'office d'impot.");
		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_SEUL, 2000);
		DemenagementAdapter demenagement = createValidDemenagement(individu, null, DATE_VALIDE, MockCommune.Fraction.LePont);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		int tachesAvant;
		{
			PersonnePhysique sophie = tiersDAO.getHabitantByNumeroIndividu(demenagement.getNoIndividu());
			assertNotNull(sophie);
			tachesAvant = tacheDAO.count(sophie.getNumero());
		}
		demenagement.checkCompleteness(erreurs, warnings);
		demenagement.validate(erreurs, warnings);
		demenagement.handle(warnings);

		assertEmpty("Une erreur est survenue lors du traitement de déménagement", erreurs);

		PersonnePhysique sophie = tiersDAO.getHabitantByNumeroIndividu(demenagement.getNoIndividu());
		assertNotNull(sophie);
		assertEquals("Sophie doit avoir deux fors fiscaux", 2, sophie.getForsFiscaux().size());
		assertNotNull("Sophie devrait encore avoir un for principal actif après le déménagement", sophie.getForFiscalPrincipalAt(null));
		assertEquals("date d'ouverture du for incorrecte", 0, sophie.getForFiscalPrincipalAt(null).getDateDebut().compareTo(DATE_VALIDE) );

		int tachesApres = tacheDAO.count(sophie.getNumero());
		assertEquals("Il aurait dû y avoir une nouvelle tâche de contrôle de dossier", 1, tachesApres - tachesAvant);
        List<Tache> mesTaches = tacheDAO.find(sophie.getNumero());
        CollectiviteAdministrative oidLausanne;
        oidLausanne = tiersService.getCollectiviteAdministrative(MockCommune.Lausanne.getOfficeImpot().getNoColAdm());


        assertTrue("une tache de contrôle de dossier doit être rattachée à l'OID précédent",existTacheControlePourAncienOID(mesTaches,oidLausanne));

	}


	private DemenagementAdapter createValidDemenagement(Individu individu, Individu conjoint, RegDate dateEvenement, MockCommune nouvelleCommune) {

		// Anciennes adresses
		MockAdresse ancienneAdresse = new MockAdresse();
		ancienneAdresse.setDateDebutValidite(DATE_ANCIENNE_ADRESSE);
		ancienneAdresse.setDateFinValidite(dateEvenement.getOneDayBefore());
		ancienneAdresse.setCasePostale("1342 Le Pont");
		ancienneAdresse.setCommuneAdresse(nouvelleCommune);
		ancienneAdresse.setNumeroOrdrePostal(1342);

		// Nouvelles adresses
		MockAdresse nouvelleAdresse = new MockAdresse();
		nouvelleAdresse.setDateDebutValidite(dateEvenement);
		nouvelleAdresse.setCasePostale("1342 Le Pont");
		nouvelleAdresse.setNumeroOrdrePostal(1342);

		final Long principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), true);
		final Long conjointPPId = (conjoint == null ? null : tiersDAO.getNumeroPPByNumeroIndividu(conjoint.getNoTechnique(), true));

		return new DemenagementAdapter(individu, principalPPId, conjoint, conjointPPId, dateEvenement, nouvelleCommune.getNoOFSEtendu(), nouvelleCommune, ancienneAdresse, nouvelleAdresse, context);
	}

	private boolean existTacheControlePourAncienOID(List<Tache> mesTaches,CollectiviteAdministrative collectiviteAttendue){

        for(Tache tache :mesTaches){
            CollectiviteAdministrative maCollectivite = tache.getCollectiviteAdministrativeAssignee();
            if(maCollectivite!=null){
              if(collectiviteAttendue.equalsTo(maCollectivite) && tache instanceof TacheControleDossier){
                 return true;
              }
            }
        }

        return false;
    }
}

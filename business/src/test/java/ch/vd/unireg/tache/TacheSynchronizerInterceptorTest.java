package ch.vd.unireg.tache;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.BusinessTestingConstants;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.evenement.retourdi.EvenementRetourDiEsbMessageHandler;
import ch.vd.unireg.evenement.retourdi.RetourDiHandler;
import ch.vd.unireg.evenement.retourdi.pp.EvenementCediService;
import ch.vd.unireg.evenement.retourdi.pp.V1Handler;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tache.sync.AddDIPP;
import ch.vd.unireg.tache.sync.SynchronizeAction;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class TacheSynchronizerInterceptorTest extends BusinessTest {

	private TacheService tacheService;
	private TacheDAO tacheDAO;

	private ParametreAppService parametreAppService;
	private Integer oldPremierePeriodeFiscaleDeclarationsPersonnesMorales;

	private EvenementRetourDiEsbMessageHandler cediListener;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tacheService = getBean(TacheService.class, "tacheService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");

		final EvenementCediService cediHandler = getBean(EvenementCediService.class, "evenementCediService");
		final V1Handler v1Handler = new V1Handler();
		v1Handler.setEvenementCediService(cediHandler);

		cediListener = new EvenementRetourDiEsbMessageHandler();
		cediListener.setHandlers(Collections.<RetourDiHandler<?>>singletonList(v1Handler));
		cediListener.setHibernateTemplate(hibernateTemplate);
		cediListener.afterPropertiesSet();

		// pour permettre la gestion de tâches avant 2016 tant qu'on n'est pas encore en 2016
		oldPremierePeriodeFiscaleDeclarationsPersonnesMorales = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(2013);
	}

	@Override
	public void onTearDown() throws Exception {
		// restauration de la valeur en base une fois les tests terminés
		if (oldPremierePeriodeFiscaleDeclarationsPersonnesMorales != null) {
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(oldPremierePeriodeFiscaleDeclarationsPersonnesMorales);
		}
		super.onTearDown();
	}

	/**
	 * [UNIREG-2894] Teste que la synchronisation des tâches après l'arrivée d'un message Cedi se déroule correctement et notamment qu'il n'y a pas de  problème d'authentification nulle.
	 */
	@Test
	public void testSynchronizeTachesOnEsbMessage() throws Exception {

		final long CTB_ID = 12500001L;

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/tache/event_retour_di.xml");
		final String texte = FileUtils.readFileToString(file);

		// crée un contribuable assujetti entre 2008 et 2009, avec une seule déclaration 2009 (celle de 2008 manque) et qui ne possède pas de tâche d'envoi de DI (= création automatique à la prochaine modification)
		setWantSynchroTache(false); // on désactive l'intercepteur pour éviter de créer les tâches d'envoi automatiquement
		doInNewTransaction(status -> {
			// les périodes fiscales qui vont bien
			for (int annee = 2003; annee < 2009; annee++) {
				final PeriodeFiscale periode = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			}
			final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2009);
			final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2009);

			// et enfin le contribuable
			final PersonnePhysique ralf = addNonHabitant(CTB_ID, "Ralf", "Leboet", date(1960, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ralf, date(2008, 1, 1), MotifFor.ARRIVEE_HC, date(2010, 6, 30), MotifFor.DEPART_HC, MockCommune.Aubonne);
			addDeclarationImpot(ralf, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);
			return null;
		});

		// on s'assure qu'il manque bien une tâche d'envoi de DI pour 2008 sur le contribuable
		doInNewTransaction(status -> {
			final PersonnePhysique ralf = hibernateTemplate.get(PersonnePhysique.class, CTB_ID);
			assertNotNull(ralf);
			try {
				final List<SynchronizeAction> list = tacheService.determineSynchronizeActionsForDIs(ralf);
				assertNotNull(list);
				assertEquals(1, list.size());
				final AddDIPP action = (AddDIPP) list.get(0);
				assertEquals(date(2008, 1, 1), action.periodeImposition.getDateDebut());
				assertEquals(date(2008, 12, 31), action.periodeImposition.getDateFin());
			}
			catch (AssujettissementException e) {
				throw new RuntimeException(e);
			}
			return null;
		});

		setWantSynchroTache(true); // on l'intercepteur pour maintenant crééer les tâches d'envoi automatiquement

		assertTrue(AuthenticationHelper.hasCurrentPrincipal());
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		AuthenticationHelper.resetAuthentication();
		try {
			// on s'assure qu'on est dans le même mode que dans l'environnement web : dans une transaction et sans authentification
			assertFalse(AuthenticationHelper.hasCurrentPrincipal());

			// simule l'envoi d'un événement de retour de DI sur le contribuable n°12500001
			// (cet événement de retour de DI est normalement englobé dans une transaction JTA gérée par Géronimo)
			doInNewTransaction(status -> {
				try {
					final EsbMessage message = EsbMessageFactory.createMessage();
					message.setBody(texte);
					cediListener.onEsbMessage(message);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			});
		}
		finally {
			AuthenticationHelper.pushPrincipal(principal);
		}

		// on s'assure que la tâche a bien été créée
		doInNewTransaction(status -> {
			final List<Tache> list = tacheDAO.find(CTB_ID);
			assertNotNull(list);
			assertEquals(1, list.size());

			final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) list.get(0);
			assertNotNull(tache);
			assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
			assertEquals(date(2008, 1, 1), tache.getDateDebut());
			assertEquals(date(2008, 12, 31), tache.getDateFin());
			assertEquals("AutoSynchro", tache.getLogCreationUser());
			return null;
		});
	}

	/**
	 * Vérification que la catégorie d'entreprise est bien mise à jour sur une tâche d'envoi de DI PM
	 */
	@Test
	public void testModificationTacheEnvoiDeclarationPM() throws Exception {

		final RegDate dateDebut = date(2014, 1, 1);
		final RegDate dateFin = date(2014, 12, 31);

		// création d'une entreprise avec une tâche d'envoi de DI dans laquelle la catégorie d'entreprise n'est pas la bonne
		final long idpm = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Truc machin SA");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne);

			// une SA est de catégorie PM, pas SP...
			addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, null, dateDebut, dateFin, dateDebut, dateFin, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.SP, oipm);
			return entreprise.getNumero();
		});

		// normalement, la synchronisation a été lancée et a corrigé la catégorie d'entreprise dans la tâche
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(idpm);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
			assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
			assertFalse(tache.isAnnule());

			final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
			assertEquals(dateDebut, tacheEnvoi.getDateDebut());
			assertEquals(dateFin, tacheEnvoi.getDateFin());
			assertEquals(dateDebut, tacheEnvoi.getDateDebutExercice());
			assertEquals(dateFin, tacheEnvoi.getDateFinExercice());
			assertEquals(TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi.getTypeDocument());
			assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tacheEnvoi.getTypeContribuable());
			assertEquals(CategorieEntreprise.PM, tacheEnvoi.getCategorieEntreprise());          // valeur corrigée !;
			return null;
		});
	}

	/**
	 * [SIFISC-17927] Si on demande un recalcul des tâches après avoir changé la date de début du premier exercice commercial
	 * à avant la date d'ouverture du premier for (VD) ouvert pour arrivée HC, alors la période d'imposition attachée à la tâche
	 * d'envoi de DI doit être mise-à-jour
	 */
	@Test
	public void testRecalculTacheEnvoiDIApresChangementDateDebutExerciceCommercialArriveeHC() throws Exception {

		final RegDate dateDebutPremierExerciceCommercial = date(2014, 1, 14);       // véritable date de création HC, en fait
		final RegDate dateArriveeHC = date(2014, 4, 1);
		final RegDate dateFin = date(2014, 12, 31);

		// création d'une entreprise avec une tâche d'envoi de DI dans laquelle la catégorie d'entreprise n'est pas la bonne
		final long idpm = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, status -> {
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateArriveeHC, null, "Truc machin SA");
			addFormeJuridique(entreprise, dateArriveeHC, null, FormeJuridiqueEntreprise.SA);
			addBouclement(entreprise, dateArriveeHC, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(entreprise, dateArriveeHC, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateArriveeHC, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateArriveeHC, MotifFor.ARRIVEE_HC, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne);

			// la tâche débute à une date qui n'est a priori pas la bonne (correspond à l'arrivée hors-canton, alors que l'exercice commercial avait peut-être déjà commencé - sauf
			// qu'on ne le sait pas encore...)
			addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, null, dateArriveeHC, dateFin, dateArriveeHC, dateFin, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
			return entreprise.getNumero();
		});

		// modification de la date de début de l'exercice commercial
		doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			// vérification de la tâche existante
			final List<Tache> taches = tacheDAO.find(idpm);
			assertEquals(1, taches.size());
			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
			assertFalse(tache.isAnnule());
			assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
			assertEquals(dateArriveeHC, ((TacheEnvoiDeclarationImpotPM) tache).getDateDebut());

			// ok, maintenant, on connait la date de début de l'exercice commercial HC
			final Entreprise e = (Entreprise) tache.getContribuable();
			e.setDateDebutPremierExerciceCommercial(dateDebutPremierExerciceCommercial);

			// et le flush de la session va causer un recalcul des tâches

			return null;
		});

		// normalement, la synchronisation a été lancée et a tout corrigé
		// (comme la période d'imposition a changé, on se trouve face à une annulation/re-création de tâche)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(idpm);
			assertNotNull(taches);
			assertEquals(2, taches.size());     // l'ancienne tâche annulée et la nouvelle, triées par id croissant

			{
				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
				assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				assertTrue(tache.isAnnule());

				final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
				assertEquals(dateArriveeHC, tacheEnvoi.getDateDebut());
				assertEquals(dateFin, tacheEnvoi.getDateFin());
				assertEquals(dateArriveeHC, tacheEnvoi.getDateDebutExercice());
				assertEquals(dateFin, tacheEnvoi.getDateFinExercice());
				assertEquals(TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi.getTypeDocument());
				assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tacheEnvoi.getTypeContribuable());
				assertEquals(CategorieEntreprise.PM, tacheEnvoi.getCategorieEntreprise());
			}
			{
				final Tache tache = taches.get(1);
				assertNotNull(tache);
				assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
				assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				assertFalse(tache.isAnnule());

				final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
				assertEquals(dateDebutPremierExerciceCommercial, tacheEnvoi.getDateDebut());
				assertEquals(dateFin, tacheEnvoi.getDateFin());
				assertEquals(dateDebutPremierExerciceCommercial, tacheEnvoi.getDateDebutExercice());
				assertEquals(dateFin, tacheEnvoi.getDateFinExercice());
				assertEquals(TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi.getTypeDocument());
				assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tacheEnvoi.getTypeContribuable());
				assertEquals(CategorieEntreprise.PM, tacheEnvoi.getCategorieEntreprise());
			}
			return null;
		});
	}

	/**
	 * [SIFISC-17927] Si on demande un recalcul des tâches après avoir changé la date de début du premier exercice commercial
	 * à avant la date d'ouverture du premier for (VD) ouvert pour autre chose que arrivée HC, alors la période d'imposition attachée à la tâche
	 * d'envoi de DI n'est pas mise-à-jour (mais l'exercice commercial rattaché, oui...)
	 */
	@Test
	public void testRecalculTacheEnvoiDIApresChangementDateDebutExerciceCommercialNonArriveeHC() throws Exception {

		final RegDate dateDebutPremierExerciceCommercial = date(2014, 1, 14);       // véritable date de création HC, en fait
		final RegDate dateArriveeHC = date(2014, 4, 1);
		final RegDate dateFin = date(2014, 12, 31);

		// création d'une entreprise avec une tâche d'envoi de DI dans laquelle la catégorie d'entreprise n'est pas la bonne
		final long idpm = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, status -> {
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateArriveeHC, null, "Truc machin SA");
			addFormeJuridique(entreprise, dateArriveeHC, null, FormeJuridiqueEntreprise.SA);
			addBouclement(entreprise, dateArriveeHC, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(entreprise, dateArriveeHC, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateArriveeHC, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateArriveeHC, MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne);       // le motif est faux...

			// la tâche débute à une date qui n'est a priori pas la bonne (correspond à l'arrivée hors-canton, alors que l'exercice commercial avait peut-être déjà commencé - sauf
			// qu'on ne le sait pas encore...)
			addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, null, dateArriveeHC, dateFin, dateArriveeHC, dateFin, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
			return entreprise.getNumero();
		});

		// modification de la date de début de l'exercice commercial
		doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			// vérification de la tâche existante
			final List<Tache> taches = tacheDAO.find(idpm);
			assertEquals(1, taches.size());
			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
			assertFalse(tache.isAnnule());
			assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);

			final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
			assertEquals(dateArriveeHC, tacheEnvoi.getDateDebut());
			assertEquals(dateFin, tacheEnvoi.getDateFin());
			assertEquals(dateArriveeHC, tacheEnvoi.getDateDebutExercice());
			assertEquals(dateFin, tacheEnvoi.getDateFinExercice());
			assertEquals(TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi.getTypeDocument());
			assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tacheEnvoi.getTypeContribuable());
			assertEquals(CategorieEntreprise.PM, tacheEnvoi.getCategorieEntreprise());

			// ok, maintenant, on connait la date de début de l'exercice commercial HC
			final Entreprise e = (Entreprise) tache.getContribuable();
			e.setDateDebutPremierExerciceCommercial(dateDebutPremierExerciceCommercial);

			// et le flush de la session va causer un recalcul des tâches

			return null;
		});

		// normalement, la synchronisation a été lancée et a corrigé la date de début de l'exercice commercial dans la tâche existante
		// (la période d'imposition, en revanche, ne change pas)
		doInNewTransactionAndSession(status -> {
			final List<Tache> taches = tacheDAO.find(idpm);
			assertNotNull(taches);
			assertEquals(1, taches.size());     // la tâche a été modifiée en place

			{
				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
				assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				assertFalse(tache.isAnnule());

				final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
				assertEquals(dateArriveeHC, tacheEnvoi.getDateDebut());
				assertEquals(dateFin, tacheEnvoi.getDateFin());
				assertEquals(dateDebutPremierExerciceCommercial, tacheEnvoi.getDateDebutExercice());
				assertEquals(dateFin, tacheEnvoi.getDateFinExercice());
				assertEquals(TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi.getTypeDocument());
				assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tacheEnvoi.getTypeContribuable());
				assertEquals(CategorieEntreprise.PM, tacheEnvoi.getCategorieEntreprise());
			}
			return null;
		});
	}

	/**
	 * [SIFISC-18358] Quand on créait manuellement une DI (sur la PF courante, en l'occurrence) avec des dates différentes des dates
	 * de la période d'imposition calculée, la DI était ré-alignée sur ces dates de période d'imposition par l'intercepteur
	 */
	@Test
	public void testCreationDILibreAvecDatesDifferentesDePeriodeTheorique() throws Exception {

		final int thisYear = RegDate.get().year();
		final RegDate dateDebut = date(thisYear, 1, 1);

		// création d'une entreprise sans DI
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Truc machin SA");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);
			return entreprise.getNumero();
		});

		// création d'une DI "libre"
		doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final PeriodeFiscale pf = addPeriodeFiscale(thisYear);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
			addDeclarationImpot(entreprise, pf, dateDebut.addDays(1), date(thisYear, 11, 30), dateDebut, date(thisYear, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			return null;
		});

		// vérifions que la DI a toujours les mêmes dates et ne s'est pas vue "réaligner"
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			final List<Declaration> declarations = entreprise.getDeclarationsTriees();
			assertNotNull(declarations);
			assertEquals(1, declarations.size());
			final Declaration declaration = declarations.get(0);
			assertNotNull(declaration);
			assertFalse(declaration.isAnnule());
			assertEquals(dateDebut.addDays(1), declaration.getDateDebut());
			assertEquals(date(thisYear, 11, 30), declaration.getDateFin());
			return null;
		});
	}

	/**
	 * [SIFISC-18579] Adaptation des dates de DI entre 2009 (date de début du calcul des périodes d'imposition) et la première
	 * période fiscale d'envoi des DI elles-mêmes
	 */
	@Test
	public void testRealignementDatesDeclarationEntrepriseAvant2016() throws Exception {

		final int year = 2011;
		final RegDate dateDebut = date(year, 1, 1);

		// PM avec DI 2011 avec mauvaise date
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Truc machin SA");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PeriodeFiscale pf = addPeriodeFiscale(year);
			addDeclarationImpot(entreprise, pf, dateDebut.addDays(15), date(year, 12, 31), dateDebut.addDays(15), date(year, 12, 31), null, null, null);
			return entreprise.getNumero();
		});


		// maintenant on ajoute le for fiscal avec la bonne date de début
		doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			assertNotNull(entreprise);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);
			return null;
		});

		// vérification des tâches générées (normalement aucune avant la première période de DI - 2013 dans ces tests) et les dates de la DI pré-existante
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			assertNotNull(entreprise);

			// seules les tâches récentes doivent avoir été générées
			final List<Tache> taches = tacheDAO.find(pmId);
			assertNotNull(taches);
			assertEquals(RegDate.get().year() - 2013, taches.size());
			for (Tache tache : taches) {
				assertNotNull(tache);
				assertFalse(tache.isAnnule());
				assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());

				final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
				assertTrue(Integer.toString(tacheEnvoi.getDateFinExercice().year()), tacheEnvoi.getDateFinExercice().year() >= 2013);
			}

			// pourtant, les dates de la déclaration 2011 doivent avoir été modifiées...
			final List<DeclarationImpotOrdinairePM> dipms = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true);
			assertNotNull(dipms);
			assertEquals(1, dipms.size());

			final DeclarationImpotOrdinairePM di = dipms.get(0);
			assertNotNull(di);
			assertFalse(di.isAnnule());
			assertEquals((Integer) year, di.getPeriode().getAnnee());
			assertEquals(dateDebut, di.getDateDebut());
			assertEquals(dateDebut, di.getDateDebutExerciceCommercial());
			assertEquals(date(year, 12, 31), di.getDateFin());
			assertEquals(date(year, 12, 31), di.getDateFinExerciceCommercial());
			assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, di.getTypeContribuable());     // mis à jour en même temps que les dates
			assertNull(di.getTypeDeclaration());
			return null;
		});
	}
}

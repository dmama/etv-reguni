package ch.vd.uniregctb.tache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tache.sync.AddDI;
import ch.vd.uniregctb.tache.sync.AnnuleTache;
import ch.vd.uniregctb.tache.sync.DeleteDI;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tache.sync.UpdateDI;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Classe de tests pour TacheService
 *
 * @author xcifde
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
	BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class TacheServiceTest extends BusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(TacheServiceTest.class);

	/**
	 * Les tâches sont par défaut créées avec une date d'échéance au prochain dimanche...
	 * @param ref date de départ
	 * @return le dimanche suivant le jour donné en référence (même jour si déjà dimanche)
	 */
	private static RegDate getNextSunday(RegDate ref) {
		assertNotNull(ref);

		// j'utilise volontairement un algorithme très basique et différent de celui
		// qui est implémenté dans le tacheService, pour également tester cet algo-là
		RegDate candidate = ref;
		while (candidate.getWeekDay() != RegDate.WeekDay.SUNDAY) {
			candidate = candidate.addDays(1);
		}
		assertEquals(RegDate.WeekDay.SUNDAY, candidate.getWeekDay());
		return candidate;
	}

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "TacheServiceTest.xml";

	private TacheService tacheService;
	private TiersService tiersService;
	private MetierService metierService;
	private TacheDAO tacheDAO;
	private DeclarationImpotOrdinaireDAO  diDAO;
	private PeriodeImpositionService periodeImpositionService;
	private PeriodeFiscaleDAO pfDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		tacheService = getBean(TacheService.class, "tacheService");
		metierService = getBean(MetierService.class, "metierService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		pfDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(333908, RegDate.get(1974, 3, 22), "Cuendet", "Adrienne", true);
				addAdresse(individu1, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);

				final MockIndividu individu2 = addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);

			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				for (MockCollectiviteAdministrative ca : MockCollectiviteAdministrative.getAll()) {
					addCollAdm(ca);
				}
				for (int pf = 2003 ; pf <= RegDate.get().year() ; ++ pf) {
					addPeriodeFiscale(pf);
				}
				return null;
			}
		});

		setWantSynchroTache(true);
	}

	@Override
	protected boolean useTiersServiceToCreateDeclarationImpot() {
		return false;
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereArriveeHSDepuisOuvertureForPrincipal() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero(11111111L);
				hab.setNumeroIndividu(333908L);
				hab = hibernateTemplate.merge(hab);

				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), MotifFor.ARRIVEE_HS, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab.addForFiscal(forFiscalPrincipal);
				tacheService.genereTacheDepuisOuvertureForPrincipal(hab, forFiscalPrincipal, null);
				return null;
			}
		});

		TacheCriteria criterion = new TacheCriteria();
		//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
		verifieTacheNouveauDossier(criterion, 0);

		assertTachesEnvoi(criterion, false);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereArriveeHSDepuisOuvertureForPrincipalSourceUNIREG1888() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero(11111111L);
				hab.setNumeroIndividu(333908L);
				hab = hibernateTemplate.merge(hab);

				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), MotifFor.ARRIVEE_HS, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				hab.addForFiscal(forFiscalPrincipal);
				tacheService.genereTacheDepuisOuvertureForPrincipal(hab, forFiscalPrincipal, null);
				return null;
			}
		});

		assertEmpty(tacheDAO.getAll());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereArriveeHCDepuisOuvertureForPrincipal() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero(11111111L);
				hab.setNumeroIndividu(333908L);
				hab = hibernateTemplate.merge(hab);

				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), MotifFor.ARRIVEE_HC, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab.addForFiscal(forFiscalPrincipal);
				tacheService.genereTacheDepuisOuvertureForPrincipal(hab, forFiscalPrincipal, null);
				return null;
			}
		});

		TacheCriteria criterion = new TacheCriteria();

		//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
		verifieTacheNouveauDossier(criterion, 0);

		assertTachesEnvoi(criterion, true);

	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereDemenagementVDDepuisOuvertureForPrincipal() throws Exception {

		final int periodeCourante = RegDate.get().year();
		final int periodeEchue = periodeCourante - 2;

		// déménagement dans la période fiscale courante => pas de tâche de contrôle de dossier
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero(11111111L);
				hab.setNumeroIndividu(333908L);
				hab = hibernateTemplate.merge(hab);

				final ForFiscalPrincipal forFiscalPrincipalDepart = new ForFiscalPrincipal(RegDate.get(2008, 6, 12), MotifFor.ARRIVEE_HC, RegDate.get(periodeCourante, 1, 2), MotifFor.DEMENAGEMENT_VD, 5586,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab.addForFiscal(forFiscalPrincipalDepart);

				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(periodeCourante, 1, 3), MotifFor.DEMENAGEMENT_VD, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab.addForFiscal(forFiscalPrincipal);

				tacheService.genereTacheDepuisOuvertureForPrincipal(hab, forFiscalPrincipal, null);
				return null;
			}
		});

		TacheCriteria criterion = new TacheCriteria();
		verifierTacheControleDossier(criterion, 0);

		// déménagement dans la période fiscale échue => il doit y avoir une tâche de contrôle de dossier
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique hab2 = new PersonnePhysique(true);
				hab2.setNumero(11111112L);
				hab2.setNumeroIndividu(333904L);
				hab2 = hibernateTemplate.merge(hab2);

				final ForFiscalPrincipal forFiscalPrincipalDepart2 = new ForFiscalPrincipal(RegDate.get(2007, 6, 12), MotifFor.ARRIVEE_HC, RegDate.get(periodeEchue, 6, 11), MotifFor.DEMENAGEMENT_VD, 5586,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab2.addForFiscal(forFiscalPrincipalDepart2);

				final ForFiscalPrincipal forFiscalPrincipal2 = new ForFiscalPrincipal(RegDate.get(periodeEchue, 6, 12), MotifFor.DEMENAGEMENT_VD, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab2.addForFiscal(forFiscalPrincipal2);

				tacheService.genereTacheDepuisOuvertureForPrincipal(hab2, forFiscalPrincipal2, null);
				return null;
			}
		});

		TacheCriteria criterion2 = new TacheCriteria();
		verifierTacheControleDossier(criterion2, 1);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereMariageDepuisOuvertureForPrincipal() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
				final PersonnePhysique hab2 = (PersonnePhysique) tiersService.getTiers(12300002);

				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(hab1, hab2, RegDate.get(2006, 6, 12), null);
				final MenageCommun menage = ensemble.getMenage();

				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				menage.addForFiscal(forFiscalPrincipal);

				tacheService.genereTacheDepuisOuvertureForPrincipal(menage, forFiscalPrincipal, null);
				return null;
			}
		});

		TacheCriteria criterion = new TacheCriteria();

		//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
		verifieTacheNouveauDossier(criterion, 0);

		assertTachesEnvoi(criterion, true);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereDecesDepuisOuvertureForPrincipal() throws Exception {

		// Etat 2008
		final Long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				loadDatabase(DB_UNIT_DATA_FILE);

				PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
				PersonnePhysique hab2 = (PersonnePhysique) tiersService.getTiers(12300002);

				// Etat avant veuvage
				EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(hab1, hab2, RegDate.get(2000, 6, 12), null);
				MenageCommun menage = ensemble.getMenage();
				assertNotNull(menage);

				PeriodeFiscale pf2006 = pfDAO.getPeriodeFiscaleByYear(2006);
				PeriodeFiscale pf2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2006);
				ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2007);
				addDeclarationImpot(menage, pf2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(menage, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				return menage.getNumero();
			}
		});

		// Evénement de décès
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
				// Etat après veuvage
				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), MotifFor.VEUVAGE_DECES, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab1.addForFiscal(forFiscalPrincipal);

				MenageCommun menage = (MenageCommun) tiersService.getTiers(idMenage);
				Set<RapportEntreTiers> rapport = menage.getRapportsObjet();
				for (RapportEntreTiers aRapport : rapport) {
					aRapport.setDateFin(RegDate.get(2006, 6, 11));
				}

				// Génération de la tâche
				tacheService.genereTacheDepuisOuvertureForPrincipal(hab1, forFiscalPrincipal, null);
				return null;
			}
		});

		TacheCriteria criterion = new TacheCriteria();

		//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
		verifieTacheNouveauDossier(criterion, 0);
	}

	/**
	 * [UNIREG-1111] Vérifie qu'un décès dans l'année courante génère bien une tâche de transmission de dossier + une tâche d'émission de déclaration d'impôt (voir spécification
	 * SCU-EngendrerUneTacheEnInstance.doc §3.1.15).
	 * <p/>
	 * [UNIREG-1305] Le décés ne génère plus de tache d'emission de DI
	 * <p/>
	 * [UNIREG-1956] Les DI des années postérieures au décès doivent avoir une tâche d'annulation
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheApresDeces() throws Exception {

		// Etat 2010
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				final PeriodeFiscale periode2008 = pfDAO.getPeriodeFiscaleByYear(2008);
				final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				final PeriodeFiscale periode2009 = pfDAO.getPeriodeFiscaleByYear(2009);
				final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2009);

				PersonnePhysique pp = addNonHabitant("Hubert", "Duchemole", date(1922, 7, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(2007, 9, 21), MotifFor.ARRIVEE_HC, MockCommune.Leysin);

				addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
				addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
				addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);

				return pp.getNumero();
			}
		});

		// [UNIREG-1956] date de décès explicitement déplacée de 2009 à 2008 pour vérifier que la DI 2009 (et pas la 2008) est annulée
		final RegDate dateDeces = date(2008, 12, 5);
		final int anneeCourante = RegDate.get().year();

		// Evénement de décès
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
				tiersService.closeForFiscalPrincipal(pp, dateDeces, MotifFor.VEUVAGE_DECES);
				return null;
			}
		});

		final List<Tache> taches = tacheDAO.getAll();
		assertNotNull(taches);
		assertEquals(2 + anneeCourante - 2010, taches.size()); // tâche de transmission de dossier + tâche d'annulation de la DI 2009
															   // + les tâches d'envoi de DIs pour [2010..année courante[ qui doivent être annulées suite au décès

		TacheTransmissionDossier tacheTransmission = null;
		TacheAnnulationDeclarationImpot tacheAnnulationDeclaration = null;
		for (Tache t : taches) {
			if (t instanceof TacheEnvoiDeclarationImpot) {
				final TacheEnvoiDeclarationImpot tacheEnvoi = (TacheEnvoiDeclarationImpot) t;
				assertTrue(tacheEnvoi.isAnnule());
			}
			else if (t instanceof TacheTransmissionDossier) {
				if (tacheTransmission != null) {
					fail("Trouvé plusieurs tâches de transmission de dossier");
				}
				tacheTransmission = (TacheTransmissionDossier) t;
			}
			else if (t instanceof TacheAnnulationDeclarationImpot) {
				if (tacheAnnulationDeclaration != null) {
					fail("Trouvé plusieurs tâches d'annulation de DI");
				}
				tacheAnnulationDeclaration = (TacheAnnulationDeclarationImpot) t;
			}
			else {
				fail("type de tâche non attendu : " + t.getClass().getName());
			}
		}

		// [UNIREG-1305]
		// la tâche d'envoi de DI
		// assertTache(TypeEtatTache.EN_INSTANCE, dateDeces.addDays(30), date(2009, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
		//		TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL,TypeAdresseRetour.ACI, tacheEnvoi);

		// la tâche de transmission de dossier
		assertNotNull(tacheTransmission);
		assertEquals(TypeEtatTache.EN_INSTANCE, tacheTransmission.getEtat());
		assertEquals(getNextSunday(RegDate.get()), tacheTransmission.getDateEcheance());

		// la tâche d'annulation de di
		assertNotNull(tacheAnnulationDeclaration);
		assertEquals(TypeEtatTache.EN_INSTANCE, tacheAnnulationDeclaration.getEtat());
		assertEquals(2009, (int) tacheAnnulationDeclaration.getDeclarationImpotOrdinaire().getPeriode().getAnnee());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheEmissionDiApresDeces() throws Exception {

		// [UNIREG-1956] date de décès explicitement déplacée de 2009 à 2008 pour vérifier que la DI 2009 (et pas la 2008) est annulée
		final RegDate dateDeces = date(2008, 12, 5);

		final CollectiviteAdministrative aciSuccessions = doInNewTransaction(new TxCallback<CollectiviteAdministrative>() {
			@Override
			public CollectiviteAdministrative execute(TransactionStatus status) throws Exception {
				return  tiersService.getOrCreateCollectiviteAdministrative(serviceInfra.getACISuccessions().getNoColAdm());
			}
		});

		// Etat 2010
		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				final PeriodeFiscale periode2008 = pfDAO.getPeriodeFiscaleByYear(2008);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

				final PersonnePhysique pp = addNonHabitant("Hubert", "Duchemole", date(1922, 7, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(2007, 9, 21), MotifFor.ARRIVEE_HC, MockCommune.Leysin);
				addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				// Evénement de décès
				tiersService.closeForFiscalPrincipal(pp, dateDeces, MotifFor.VEUVAGE_DECES);

				return pp.getNumero();
			}
		});

		final List<Tache> taches = tacheDAO.getAll();
		assertNotNull(taches);
		assertEquals(2, taches.size());   // tâche de transmission de dossier + tâche d'envoi de DI 2008

		TacheEnvoiDeclarationImpot tacheEnvoi = null;

		TacheTransmissionDossier tacheTransmission = null;

		for (Tache t : taches) {
			if (t instanceof TacheEnvoiDeclarationImpot) {
				tacheEnvoi = (TacheEnvoiDeclarationImpot) t;
				continue;
			}
			if (t instanceof TacheTransmissionDossier) {
				if (tacheTransmission != null) {
					fail("Trouvé plusieurs tâches de transmission de dossier");
				}
				tacheTransmission = (TacheTransmissionDossier) t;
			}
			else {
				fail("type de tâche non attendu : " + t.getClass().getName());
			}
		}


		// [UNIREG-1305]
		// la tâche d'envoi de DI
		 assertTache(TypeEtatTache.EN_INSTANCE, dateDeces.addDays(30), date(2008, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,TypeAdresseRetour.ACI,aciSuccessions, tacheEnvoi);

		// la tâche de transmission de dossier
		assertNotNull(tacheTransmission);
		assertEquals(TypeEtatTache.EN_INSTANCE, tacheTransmission.getEtat());
		assertEquals(getNextSunday(RegDate.get()), tacheTransmission.getDateEcheance());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereDivorceDepuisOuvertureForPrincipal() throws Exception {

		// Etat 2008
		final Long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				loadDatabase(DB_UNIT_DATA_FILE);

				PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
				PersonnePhysique hab2 = (PersonnePhysique) tiersService.getTiers(12300002);

				EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(hab1, hab2, RegDate.get(2006, 6, 12), null);
				MenageCommun menage = ensemble.getMenage();
				assertNotNull(menage);

				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				menage.addForFiscal(forFiscalPrincipal);

				PeriodeFiscale pf2006 = pfDAO.getPeriodeFiscaleByYear(2006);
				PeriodeFiscale pf2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2006);
				ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2007);
				addDeclarationImpot(menage, pf2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(menage, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				return menage.getNumero();
			}
		});

		// Divorce au 10 octobre 2007
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
				PersonnePhysique hab2 = (PersonnePhysique) tiersService.getTiers(12300002);
				MenageCommun menage = (MenageCommun) tiersService.getTiers(idMenage);

				ForFiscalPrincipal forFiscalPrincipal = menage.getDernierForFiscalPrincipal();
				forFiscalPrincipal.setDateFin(RegDate.get(2007, 10, 10));
				forFiscalPrincipal.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);

				hab1.getRapportsSujet().iterator().next().setDateFin(forFiscalPrincipal.getDateFin());
				hab2.getRapportsSujet().iterator().next().setDateFin(forFiscalPrincipal.getDateFin());

				ForFiscalPrincipal ffp1 = new ForFiscalPrincipal(forFiscalPrincipal.getDateFin().getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab1.addForFiscal(ffp1);

				ForFiscalPrincipal ffp2 = new ForFiscalPrincipal(forFiscalPrincipal.getDateFin().getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab2.addForFiscal(ffp2);

				tacheService.genereTacheDepuisOuvertureForPrincipal(hab1, ffp1, null);
				tacheService.genereTacheDepuisOuvertureForPrincipal(hab2, ffp2, null);
				return null;
			}
		});

		TacheCriteria criterion = new TacheCriteria();

		//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
		verifieTacheNouveauDossier(criterion, 0);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheDepuisOuvertureForSecondaireActivite() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero((long) 11111111);
				hab.setNumeroIndividu((long) 333908);
				hab = hibernateTemplate.merge(hab);

				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), MotifFor.DEBUT_EXPLOITATION, null, null, 8201,
						TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				final ForFiscalSecondaire forFiscalSecondaire = new ForFiscalSecondaire(RegDate.get(2006, 6, 12), MotifFor.DEBUT_EXPLOITATION, null, null, 5652,
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ACTIVITE_INDEPENDANTE);

				hab.addForFiscal(forFiscalPrincipal);
				forFiscalSecondaire.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
				hab.addForFiscal(forFiscalSecondaire);

				tacheService.genereTacheDepuisOuvertureForSecondaire(hab, forFiscalSecondaire);
				return null;
			}
		});

		TacheCriteria criterion = new TacheCriteria();
		verifieTacheNouveauDossier(criterion, 1);

		assertTachesEnvoi(criterion, false);

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheDepuisOuvertureForSecondaireImmeuble() throws Exception {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero((long) 11111111);
		hab.setNumeroIndividu((long) 333908);
		hab = hibernateTemplate.merge(hab);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), MotifFor.ACHAT_IMMOBILIER, null, null, 8201,
				TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		ForFiscalSecondaire forFiscalSecondaire = new ForFiscalSecondaire(RegDate.get(2006, 6, 12), MotifFor.ACHAT_IMMOBILIER, null, null, 5652,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);

		hab.addForFiscal(forFiscalPrincipal);
		forFiscalSecondaire.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
		hab.addForFiscal(forFiscalSecondaire);

		tacheService.genereTacheDepuisOuvertureForSecondaire(hab, forFiscalSecondaire);

		TacheCriteria criterion = new TacheCriteria();
		verifieTacheNouveauDossier(criterion, 1);

	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheDepartHSDepuisFermetureForPrincipal() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale periode2006 = pfDAO.getPeriodeFiscaleByYear(2006);
				final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				final PeriodeFiscale periode2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

				final PersonnePhysique pp = addNonHabitant("François", "Dardare-style", date(1977, 1, 1), Sexe.MASCULIN);
				addDeclarationImpot(pp, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				final ForFiscalPrincipal forFiscalPrincipal = addForPrincipal(pp, date(2004, 6, 12), MotifFor.ARRIVEE_HC, date(2006, 6, 12), MotifFor.DEPART_HS, MockCommune.VillarsSousYens);

				tacheService.genereTacheDepuisFermetureForPrincipal(pp, forFiscalPrincipal);

				return pp.getNumero();
			}
		});

		TacheCriteria criterion = new TacheCriteria();
		verifieControleDossier(criterion);
		verifieTachesAnnulation(criterion, 1, false);
	}

	/**
	 * Scénario : un contribuable vaudois par hors-Suisse en 2005, mais on ne reçoit l'événement de départ qu'en 2008
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheDepartHSArriveeTardiveEvenement() throws Exception {

		class Ids {
			Long raoulId;
			Long jeanDanielId;
			Long oidCedi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
				ids.oidCedi = cedi.getId();

				PeriodeFiscale pf2003 = pfDAO.getPeriodeFiscaleByYear(2003);
				PeriodeFiscale pf2004 = pfDAO.getPeriodeFiscaleByYear(2004);
				PeriodeFiscale pf2005 = pfDAO.getPeriodeFiscaleByYear(2005);
				PeriodeFiscale pf2006 = pfDAO.getPeriodeFiscaleByYear(2006);
				PeriodeFiscale pf2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				PeriodeFiscale pf2008 = pfDAO.getPeriodeFiscaleByYear(2008);
				PeriodeFiscale pf2009 = pfDAO.getPeriodeFiscaleByYear(2009);
				ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2003);
				addModeleFeuilleDocument("Déclaration", "210", modele2003);
				addModeleFeuilleDocument("Annexe 1", "220", modele2003);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2003);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2003);
				ModeleDocument modele2004 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2004);
				addModeleFeuilleDocument("Déclaration", "210", modele2004);
				addModeleFeuilleDocument("Annexe 1", "220", modele2004);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2004);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2004);
				ModeleDocument modele2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2005);
				addModeleFeuilleDocument("Déclaration", "210", modele2005);
				addModeleFeuilleDocument("Annexe 1", "220", modele2005);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2005);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2005);
				ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2006);
				addModeleFeuilleDocument("Déclaration", "210", modele2006);
				addModeleFeuilleDocument("Annexe 1", "220", modele2006);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2006);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2006);
				ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2007);
				addModeleFeuilleDocument("Déclaration", "210", modele2007);
				addModeleFeuilleDocument("Annexe 1", "220", modele2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2007);
				ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2008);
				addModeleFeuilleDocument("Déclaration", "210", modele2008);
				addModeleFeuilleDocument("Annexe 1", "220", modele2008);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2008);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2008);
				ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2009);
				addModeleFeuilleDocument("Déclaration", "210", modele2009);
				addModeleFeuilleDocument("Annexe 1", "220", modele2009);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2009);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2009);
				addModeleFeuilleDocument("Annexe 1-1", "310", modele2009);

				// Contribuable vaudois depuis 1998 avec des DIs jusqu'en 2007
				Contribuable raoul = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1998, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

				addDeclarationImpot(raoul, pf2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);
				addDeclarationImpot(raoul, pf2004, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2004);
				addDeclarationImpot(raoul, pf2005, date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2005);
				addDeclarationImpot(raoul, pf2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(raoul, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				// Un autre contribuable vaudois depuis 1998 avec des DIs jusqu'en 2009
				Contribuable jeanDaniel = addNonHabitant("Jean-Daniel", "Lavanchy", date(1962, 10, 4), Sexe.MASCULIN);
				ids.jeanDanielId = jeanDaniel.getNumero();
				addForPrincipal(jeanDaniel, date(1998, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

				addDeclarationImpot(jeanDaniel, pf2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2003);
				addDeclarationImpot(jeanDaniel, pf2004, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2004);
				addDeclarationImpot(jeanDaniel, pf2005, date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2005);
				addDeclarationImpot(jeanDaniel, pf2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2006);
				addDeclarationImpot(jeanDaniel, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2007);
				addDeclarationImpot(jeanDaniel, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2008);
				addDeclarationImpot(jeanDaniel, pf2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2009);

				return null;
			}
		});

		// Raoul part fin juin 2005, mais on ne reçoit son départ qu'aujourd'hui.
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
				ForFiscalPrincipal forPrincipal = raoul.getForFiscalPrincipalAt(date(2005, 6, 30));
				forPrincipal.setDateFin(date(2005, 6, 30));
				forPrincipal.setMotifFermeture(MotifFor.DEPART_HS);
				tacheService.genereTacheDepuisFermetureForPrincipal(raoul, forPrincipal);
				return null;
			}
		});

		{
			final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
			TacheCriteria criterion = new TacheCriteria();
			criterion.setContribuable(raoul);

			// il doit y avoir 1 tâche de contrôle de dossier
			criterion.setTypeTache(TypeTache.TacheControleDossier);
			List<Tache> taches = tacheDAO.find(criterion);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			// il doit y avoir 2 tâches d'annulation de DIs
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			taches = tacheDAO.find(criterion);
			assertNotNull(taches);
			assertEquals(2, taches.size()); // 2006 et 2007

			// la date de fin de la déclaration 2005 doit avoir été mis-à-jour
			final List<Declaration> declarations2005 = raoul.getDeclarationsForPeriode(2005, false);
			assertNotNull(declarations2005);
			assertEquals(1, declarations2005.size());

			final Declaration declaration2005 = declarations2005.get(0);
			assertNotNull(declaration2005);
			assertEquals(date(2005, 1, 1), declaration2005.getDateDebut());
			assertEquals(date(2005, 6, 30), declaration2005.getDateFin());
		}

		{
			final Contribuable jeanDaniel = (Contribuable) tiersService.getTiers(ids.jeanDanielId);
			TacheCriteria criterion = new TacheCriteria();
			criterion.setContribuable(jeanDaniel);

			// il doit y avoir 0 tâche de contrôle de dossier
			criterion.setTypeTache(TypeTache.TacheControleDossier);
			List<Tache> taches = tacheDAO.find(criterion);
			assertEmpty(taches);

			// il doit y 0 tâches d'annulation de DIs
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			taches = tacheDAO.find(criterion);
			assertEmpty(taches);

			// la déclaration 2005 doit avoir une période inchangée
			Declaration declaration = jeanDaniel.getDeclarationActive(date(2005, 6, 30));
			assertDI(date(2005, 1, 1), date(2005, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, declaration);
		}
	}

	/**
	 * Vérifie la création des tâches en cas de fermeture tardive du for fiscal principal où le contribuable repart hors-canton la même année de son arrivée.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheDepartHCDepuisFermetureForPrincipal() throws Exception {

		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale periode2006 = pfDAO.getPeriodeFiscaleByYear(2006);
				final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				final PeriodeFiscale periode2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

				final PersonnePhysique pp = addNonHabitant("François", "Dardare-style", date(1977, 1, 1), Sexe.MASCULIN);
				addDeclarationImpot(pp, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				final ForFiscalPrincipal forFiscalPrincipal = addForPrincipal(pp, date(2006, 6, 12), MotifFor.ARRIVEE_HC, date(2006, 6, 12), MotifFor.DEPART_HC, MockCommune.VillarsSousYens);

				// simule la fermeture tardive (= événement de départ reçu en 2010, par exemple)
				tacheService.genereTacheDepuisFermetureForPrincipal(pp, forFiscalPrincipal);
				return pp.getNumero();
			}
		});

		final Contribuable pp = hibernateTemplate.get(Contribuable.class, id);
		assertNotNull(pp);

		TacheCriteria criterion = new TacheCriteria();
		criterion.setContribuable(pp);

		// il doit y avoir 0 tâche de contrôle de dossier
		criterion.setTypeTache(TypeTache.TacheControleDossier);
		List<Tache> taches = tacheDAO.find(criterion);
		assertEmpty(taches);

		// il doit y 2 tâches d'annulation de DIs
		criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
		taches = tacheDAO.find(criterion);
		assertEquals(2, taches.size()); // 2006 et 2007

		// il doit y avoir 0 tâche de contrôle d'envoi de DI
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		taches = tacheDAO.find(criterion);
		assertEmpty(taches);
	}

	/**
	 * [UNIREG-2266] Vérifie que les nouvelles tâches possède bien une collectivité administrative assignée, même si l'OID du tiers ne change pas suite au changement de for.
	 */
	@Test
	public void testGenereTacheDepartHCTardif() throws Exception {

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		// Création de la situation initiale : contribuable vaudois ordinaire dans le canton depuis 2008 avec une DI pour 2008
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = pfDAO.getPeriodeFiscaleByYear(2008);
				final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

				final Contribuable raoul = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(2008, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Orbe);

				final DeclarationImpotOrdinaire di = addDeclarationImpot(raoul, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
				addEtatDeclarationEmise(di, date(2009, 1, 15));
				addEtatDeclarationRetournee(di, date(2009, 2, 21));

				return null;
			}
		});

		// Saisie d'un départ hors-canton au mileu 2008 (départ entrée tardivement, donc)
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
				assertNotNull(raoul);

				tiersService.closeForFiscalPrincipal(raoul, date(2008, 5, 23), MotifFor.DEPART_HC);
				tiersService.openForFiscalPrincipal(raoul, date(2008, 5, 24), MotifRattachement.DOMICILE, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.ORDINAIRE,
						MotifFor.DEPART_HC);

				return null;
			}
		});

		// Vérifie qu'il y a bien une tâche d'annulation de la DI 2008 et qu'elle est bien assignée à l'OID d'Orbe
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
				assertNotNull(raoul);
				assertEquals(Integer.valueOf(MockOfficeImpot.OID_ORBE.getNoColAdm()), raoul.getOfficeImpotId());

				final CollectiviteAdministrative oidOrbe = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_ORBE.getNoColAdm());
				assertNotNull(oidOrbe);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(raoul);
				criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);

				final List<Tache> list = tacheDAO.find(criterion);
				assertEquals(1, list.size());

				final Tache tache = list.get(0);
				assertInstanceOf(TacheAnnulationDeclarationImpot.class, tache);

				final TacheAnnulationDeclarationImpot annulDI = (TacheAnnulationDeclarationImpot) tache;
				assertSame(oidOrbe, annulDI.getCollectiviteAdministrativeAssignee());

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheDepartHCDepuisFermetureForPrincipaleFinPeriode() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique hab = (PersonnePhysique) tiersService.getTiers(12300001);

				final ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2007, 6, 12), MotifFor.INDETERMINE, RegDate.get(2007, 12, 31), MotifFor.DEPART_HC,
						5652, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

				tacheService.genereTacheDepuisFermetureForPrincipal(hab, forFiscalPrincipal);
				return null;
			}
		});


		TacheCriteria criterion = new TacheCriteria();
		verifieControleDossier(criterion, 0);

		verifieAbsenceDIAnnulee(new Long("12300001"), 2007);

	}

	/**
	 * [UNIREG-1110] Tâches en instance : fermeture du dernier for secondaire pour un HS / HC dans une période échue. Les taches d'annulation de DI n'étaient pas générées si le motif de départ été
	 * different de depart HS ou HC hors elles doivent l'être si le for secondaire est le dernier actif.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testClotureDuDernierForSecondaireUNIREG1110() throws Exception {

		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale periode2006 = pfDAO.getPeriodeFiscaleByYear(2006);
				final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				final PeriodeFiscale periode2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

				final PersonnePhysique pp = addNonHabitant("François", "Dardare-style", date(1977, 1, 1), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 12), MotifFor.DEBUT_EXPLOITATION, MockPays.Allemagne);
				addDeclarationImpot(pp, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				final ForFiscalSecondaire forFiscalSecondaire = addForSecondaire(pp, date(2005, 6, 12), MotifFor.DEBUT_EXPLOITATION, date(2006, 6, 11), MotifFor.FIN_EXPLOITATION, MockCommune.Fraction.LeLieu.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				tacheService.genereTacheDepuisFermetureForSecondaire(pp, forFiscalSecondaire);
				return pp.getNumero();
			}
		});

		final Contribuable pp = hibernateTemplate.get(Contribuable.class, id);
		assertNotNull(pp);

		TacheCriteria criterion = new TacheCriteria();
		criterion.setContribuable(pp);

		// il doit y avoir 1 tâche de contrôle de dossier
		criterion.setTypeTache(TypeTache.TacheControleDossier);
		List<Tache> taches = tacheDAO.find(criterion);
		assertEquals(1, taches.size());

		// il doit y 1 tâches d'annulation de DIs
		criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
		taches = tacheDAO.find(criterion);
		assertEquals(1, taches.size()); // 2007

		// la date de fin dela déclaration 2006 doit avoir été mise-à-jour
		final List<Declaration> declarations2006 = pp.getDeclarationsForPeriode(2006, false);
		assertNotNull(declarations2006);
		assertEquals(1, declarations2006.size());

		final DeclarationImpotOrdinaire declaration2006 = (DeclarationImpotOrdinaire)declarations2006.get(0);
		assertNotNull(declaration2006);
		assertEquals(date(2006, 1, 1), declaration2006.getDateDebut());
		assertEquals(date(2006, 6, 11), declaration2006.getDateFin());
		assertEquals(TypeContribuable.HORS_SUISSE, declaration2006.getTypeContribuable());
	}


	/**
	 * L'ouverture d'un for principal sur un contribuable ordinaire doit générer un envoi de DIs et l'ouverture d'un dossier
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOuvertureForPrincipalImpositionOrdinaire() throws Exception {
		ouvreForPrincipal(ModeImposition.ORDINAIRE, MotifFor.ARRIVEE_HS, RegDate.get(2006, 6, 12));
		//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
		assertEquals(0, getNouveauDossierCount());
		assertEquals(RegDate.get().year() - 2006, getTacheCount());
	}

	/**
	 * L'ouverture d'un for principal sur un contribuable sourcier ne doit pas générer d'envoi de DIs, ni d'ouverture de dossier
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOuvertureForPrincipalImpositionSourcier() throws Exception {
		ouvreForPrincipal(ModeImposition.SOURCE, MotifFor.ARRIVEE_HS, RegDate.get(2006, 6, 12));
		assertEquals(0, getNouveauDossierCount());
		assertEquals(0, getTacheCount());
	}

	/**
	 * UNIREG-533: on teste que la fermeture d'un for fiscal principal avec une date de fermeture qui précède la date de début génère bien une exception et que celle-ci est une exception de validation.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCloseForFiscalPrincipalDateFermetureAvantDateDebut() throws Exception {

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Contribuable vaudois depuis 1998 avec des DIs jusqu'en 2007
				Contribuable raoul = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1998, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				return null;
			}
		});

		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);

		try {
			tiersService.closeForFiscalPrincipal(raoul, date(1990, 12, 31), MotifFor.DEPART_HS);
			fail();
		}
		catch (ValidationException e) {
			List<ValidationMessage> errors = e.getErrors();
			assertEquals(1, errors.size());
			assertEquals("La date de fermeture (31.12.1990) est avant la date de début (01.01.1998) du for fiscal actif", errors.get(0).getMessage());
		}
	}

	/**
	 * Vérifie que la fermeture d'un for principal hors-Suisse ne génère aucune tâche.
	 * <p/>
	 * <b>Note:</b> ce test reproduit un cas réel (voir ctb n°52108102) où le départ HS à été entré comme déménagement VD, ce qui est évidemment incorrecte.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFermetureForPrincipalHorsSuisse() throws Exception {

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Contribuable vaudois parti en France dès 1999
				Contribuable raoul = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
				ids.raoulId = raoul.getNumero();

				addForPrincipal(raoul, date(1998, 1, 1), MotifFor.MAJORITE, date(1999, 6, 30), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(raoul, date(1999, 7, 1), MotifFor.DEMENAGEMENT_VD, MockPays.France);
				return null;
			}
		});

		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);

		tiersService.closeForFiscalPrincipal(raoul, date(2005, 12, 31), MotifFor.DEPART_HS);

		// fermeture for principal hors-Suisse -> rien à faire
		assertEmpty(tacheDAO.getAll());
	}

	/**
	 * [UNIREG-1102] Teste que la fin d'activité indépendante en cours d'année (pour un contribuable hors-Suisse) génère bien une tâche d'émission de DI limitée à la période d'activité indépendante.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheDIFinActiviteIndependanteCtbHS() throws Exception {

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2003 = pfDAO.getPeriodeFiscaleByYear(2003);
				final ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);
				final PeriodeFiscale periode2004 = pfDAO.getPeriodeFiscaleByYear(2004);
				final ModeleDocument modele2004 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2004);

				// Contribuable français
				Contribuable raoul = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
				addForPrincipal(raoul, date(1990, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockPays.France);
				ids.raoulId = raoul.getNumero();

				addDeclarationImpot(raoul, periode2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);
				addDeclarationImpot(raoul, periode2004, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2004);

				// début d'exploitation au 1er mai 1990
				addForSecondaire(raoul, date(1990, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(),
						MotifRattachement.ACTIVITE_INDEPENDANTE);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
				assertNotNull(raoul);

				final List<ForFiscalSecondaire> forsSecondaires = raoul.getForsParType(false).secondaires;
				assertNotNull(forsSecondaires);
				assertEquals(1, forsSecondaires.size());

				final ForFiscalSecondaire forSecondaire = forsSecondaires.get(0);
				assertNotNull(forSecondaire);

				// fin d'exploitation au 1er février 2005
				tiersService.closeForFiscalSecondaire(raoul, forSecondaire, date(2005, 2, 1), MotifFor.FIN_EXPLOITATION);
				return null;
			}
		});

		// fin d'activité indépendante ctb HS -> émission de la tâche d'émission de DI du 1er janvier à la date de fermeture + une tâche de contrôle de dossier

		final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.raoulId);
		assertNotNull(pp);

		final TacheCriteria criterion = new TacheCriteria();
		criterion.setContribuable(pp);

		// il doit y avoir 1 tâche de contrôle de dossier
		criterion.setTypeTache(TypeTache.TacheControleDossier);
		List<Tache> taches = tacheDAO.find(criterion);
		assertEquals(1, taches.size());

		// il doit y 0 tâches d'annulation de DIs
		criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
		taches = tacheDAO.find(criterion);
		assertEmpty(taches);

		// il doit y avoir 1 tâche d'envoi de DI pour 2005
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		taches = tacheDAO.find(criterion);
		assertEquals(1, taches.size());

		final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) taches.get(0);
		assertNotNull(tache);
		// activité indépendante -> type contribuable = vaudois ordinaire
		assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(RegDate.get()), date(2005, 1, 1), date(2005, 2, 1), TypeContribuable.HORS_SUISSE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, null, tache);
	}

	/**
	 * Vérifie qu'il y a le nombre correct d'annulation de DIs et d'émission de DIs générées lors du mariage de deux contribuables pour une période passée (= événement de mariage reçu en retard).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerateTacheMariagePeriodePassee() throws Exception {

		final RegDate dateMariage = date(2007, 11, 11);
		final RegDate today = RegDate.get();
		final RegDate nextSunday = getNextSunday(today);
		final RegDate nextFinEnvoiEnMasse = RegDate.get(today.year(), 1, 31);

		class Ids {
			Long monsieurId;
			Long madameId;
			Long menageId;
		}
		final Ids ids = new Ids();

		final long idIndividuMonsieur = 253828;
		final long idIndividuMadame = 157837;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				MockIndividu monsieur = addIndividu(idIndividuMonsieur, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
				addAdresse(monsieur, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null);

				MockIndividu madame = addIndividu(idIndividuMadame, RegDate.get(1968, 3, 27), "Lavanchy", "Laurence", false);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1968, 3, 27), null);
				addNationalite(madame, MockPays.Suisse, date(1963, 1, 1), null);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Crée les personnes célibataires
				PersonnePhysique monsieur = addHabitant(idIndividuMonsieur);
				addForPrincipal(monsieur, date(1981, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.monsieurId = monsieur.getNumero();

				PersonnePhysique madame = addHabitant(idIndividuMadame);
				addForPrincipal(madame, date(1986, 3, 27), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.madameId = madame.getNumero();

				// Ajoute les déclarations qui vont bien
				for (int i = 2003; i <= 2008; ++i) {
					PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(monsieur, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
					addDeclarationImpot(madame, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique monsieur = (PersonnePhysique) tiersService.getTiers(ids.monsieurId);
				assertNotNull(monsieur);
				PersonnePhysique madame = (PersonnePhysique) tiersService.getTiers(ids.madameId);
				assertNotNull(madame);

				// mariage au 11 novembre 2007
				MenageCommun menage = metierService.marie(dateMariage, monsieur, madame, "", EtatCivil.MARIE, null);
				ids.menageId = menage.getNumero();

				return null;
			}
		});

		// Vérifie que des tâches d'annulation des DIs 2007 et 2008 sont générées sur chacunes des personnes physiques.

		{
			final List<TacheAnnulationDeclarationImpot> annulations = tacheDAO.listTaches(ids.monsieurId,
					TypeTache.TacheAnnulationDeclarationImpot);
			assertNotNull(annulations);
			assertEquals(2, annulations.size());

			sortTachesAnnulation(annulations);
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), annulations.get(0));
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2008, 1, 1), date(2008, 12, 31), annulations.get(1));
		}

		{
			final List<TacheAnnulationDeclarationImpot> annulations = tacheDAO.listTaches(ids.madameId,
					TypeTache.TacheAnnulationDeclarationImpot);
			assertNotNull(annulations);
			assertEquals(2, annulations.size());

			sortTachesAnnulation(annulations);
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), annulations.get(0));
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2008, 1, 1), date(2008, 12, 31), annulations.get(1));
		}

		// Vérifie que des tâches d'émission des DIs 2007 et 2008 sont générées sur le ménage.

		final List<TacheEnvoiDeclarationImpot> envois = tacheDAO.listTaches(ids.menageId, TypeTache.TacheEnvoiDeclarationImpot);
		assertNotNull(envois);
		assertEquals(today.year() - 2007, envois.size());       // 2007 et 2008 en 2009, plus 2009 en 2010...

		sortTachesEnvoi(envois);

		// pour 2007, on ne trouve pas de DI mais on considère le contribuable (ménage) comme nouvel assujetti (n-1 n'a pas d'assujettissement du tout), donc VAUD_TAX
		assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI,
				envois.get(0));

		// après 2007, on ne trouve pas de DI et on ne considère *pas* le contribuable (ménage) comme nouvel assujetti (n-1 a un assujettissement ordinaire), donc COMPLETE
		for (int i = 2008; i < today.year(); ++i) {
			final TacheEnvoiDeclarationImpot tache = envois.get(i - 2007);
			final RegDate dateEcheance = determineDateEcheance(today, nextSunday, nextFinEnvoiEnMasse, tache.getDateDebut().year());
			assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					TypeAdresseRetour.CEDI, tache);
		}
	}

	private static RegDate determineDateEcheance(RegDate today, RegDate nextSunday, RegDate nextFinEnvoiEnMasse, int periodeTache) {
		RegDate dateEcheance = nextSunday;
		if (periodeTache + 1 == today.year() && dateEcheance.isBefore(nextFinEnvoiEnMasse)) {
			// [SIFISC-7332] pour les DIs de l'année précédente, la date d'échéance est poussée - au minimum - jusqu'au 31 janvier de l'année courante.
			dateEcheance = nextFinEnvoiEnMasse;
		}
		return dateEcheance;
	}

	/**
	 * [UNIREG-1105] Vérifie qu'il y a le nombre correct d'annulation de DIs et d'émission de DIs générées lors du divorce de deux contribuables pour une période passée (= événement de divorce reçu en
	 * retard).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerateTacheDivorcePeriodePassee() throws Exception {

		final RegDate dateMariage = date(2002, 5, 1);
		final RegDate dateDivorce = date(2007, 11, 11);
		final RegDate today = RegDate.get();
		final RegDate nextSunday = getNextSunday(today);
		final RegDate nextFinEnvoiEnMasse = RegDate.get(today.year(), 1, 31);

		class Ids {
			Long monsieurId;
			Long madameId;
			Long menageId;
		}
		final Ids ids = new Ids();

		final long idIndividuMonsieur = 253828;
		final long idIndividuMadame = 157837;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				MockIndividu monsieur = addIndividu(idIndividuMonsieur, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
				addAdresse(monsieur, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null);

				MockIndividu madame = addIndividu(idIndividuMadame, RegDate.get(1968, 3, 27), "Lavanchy", "Laurence", false);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1968, 3, 27), null);
				addNationalite(madame, MockPays.Suisse, date(1963, 1, 1), null);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Crée le ménage
				PersonnePhysique monsieur = addHabitant(idIndividuMonsieur);
				addForPrincipal(monsieur, date(1981, 1, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.monsieurId = monsieur.getNumero();

				PersonnePhysique madame = addHabitant(idIndividuMadame);
				addForPrincipal(madame, date(1986, 3, 27), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.madameId = madame.getNumero();

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.menageId = menage.getNumero();

				// Ajoute les déclarations qui vont bien
				for (int i = 2003; i <= 2008; ++i) {
					PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(menage, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				// Divorce au 11 novembre 2007
				metierService.separe(menage, dateDivorce, "", EtatCivil.DIVORCE, null);

				return null;
			}
		});

		// Ménage-commun
		{
			// Vérifie que des tâches d'annulation des DIs 2007 et 2008 sont générées sur le ménage
			final List<TacheAnnulationDeclarationImpot> annulations = tacheDAO.listTaches(ids.menageId, TypeTache.TacheAnnulationDeclarationImpot);
			assertNotNull(annulations);
			assertEquals(2, annulations.size());

			sortTachesAnnulation(annulations);
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), annulations.get(0));
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2008, 1, 1), date(2008, 12, 31), annulations.get(1));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheControleDossier));
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheEnvoiDeclarationImpot));
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheNouveauDossier));
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheTransmissionDossier));
		}

		// Monsieur
		{
			// Vérifie que des tâches d'émission des DIs 2007 et 2008 sont générées
			final List<TacheEnvoiDeclarationImpot> envois = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheEnvoiDeclarationImpot);
			assertNotNull(envois);
			assertEquals(RegDate.get().year() - 2007, envois.size());

			sortTachesEnvoi(envois);

			// pour 2007, on ne trouve pas de DI mais on considère le contribuable comme nouvel assujetti (n-1 n'a pas d'assujettissement du tout), donc VAUD_TAX
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI,
					envois.get(0));

			// après 2007, on ne trouve pas de DI et on ne considère pas le contribuable comme nouvel assujetti (n-1 a un assujettissement ordinaire), donc COMPLETE
			for (int i = 2008; i < RegDate.get().year(); ++i) {
				final TacheEnvoiDeclarationImpot tache = envois.get(i - 2007);
				final RegDate dateEcheance = determineDateEcheance(today, nextSunday, nextFinEnvoiEnMasse, tache.getDateDebut().year());
				assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, tache);
			}

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")
//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
//			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier);
//			assertNotNull(nouveaux);
//			assertEquals(1, nouveaux.size());
//			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheAnnulationDeclarationImpot));
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheControleDossier));
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheTransmissionDossier));
		}

		// Madame
		{
			// Vérifie que des tâches d'émission des DIs 2007 et 2008 sont générées
			final List<TacheEnvoiDeclarationImpot> envois = tacheDAO.listTaches(ids.madameId, TypeTache.TacheEnvoiDeclarationImpot);
			assertNotNull(envois);
			assertEquals(RegDate.get().year() - 2007, envois.size());

			sortTachesEnvoi(envois);

			// pour 2007, on ne trouve pas de DI mais on considère le contribuable comme nouvel assujetti (n-1 n'a pas d'assujettissement du tout), donc VAUD_TAX
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI,
					envois.get(0));

			// après 2007, on ne trouve pas de DI et on ne considère pas le contribuable comme nouvel assujetti (n-1 a un assujettissement ordinaire), donc COMPLETE
			for (int i = 2008; i < RegDate.get().year(); ++i) {
				final TacheEnvoiDeclarationImpot tache = envois.get(i - 2007);
				final RegDate dateEcheance = determineDateEcheance(today, nextSunday, nextFinEnvoiEnMasse, tache.getDateDebut().year());
				assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, tache);
			}

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")

			//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois

//			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier);
//			assertNotNull(nouveaux);
//			assertEquals(1, nouveaux.size());
//			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheAnnulationDeclarationImpot));
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheControleDossier));
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheTransmissionDossier));
		}

	}


	/**
	 * [UNIREG-1105] Vérifie qu'il y a bien une tâche de contrôle de dossier lorsqu'un for secondaire existe sur le ménage qui se divorce.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerateTacheDivorceAvecForSecondaire() throws Exception {

		final RegDate dateMariage = date(2002, 5, 1);
		final RegDate dateDivorce = date(2007, 11, 11);
		final RegDate today = RegDate.get();
		final RegDate nextSunday = getNextSunday(today);
		final RegDate nextFinEnvoiEnMasse = RegDate.get(today.year(), 1, 31);

		class Ids {
			Long monsieurId;
			Long madameId;
			Long menageId;
		}
		final Ids ids = new Ids();

		final long idIndividuMonsieur = 253828;
		final long idIndividuMadame = 157837;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				MockIndividu monsieur = addIndividu(idIndividuMonsieur, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
				addAdresse(monsieur, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null);

				MockIndividu madame = addIndividu(idIndividuMadame, RegDate.get(1968, 3, 27), "Lavanchy", "Laurence", false);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1968, 3, 27), null);
				addNationalite(madame, MockPays.Suisse, date(1963, 1, 1), null);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Crée le ménage
				PersonnePhysique monsieur = addHabitant(idIndividuMonsieur);
				addForPrincipal(monsieur, date(1981, 1, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.monsieurId = monsieur.getNumero();

				PersonnePhysique madame = addHabitant(idIndividuMadame);
				addForPrincipal(madame, date(1986, 3, 27), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.madameId = madame.getNumero();

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.menageId = menage.getNumero();

				addForSecondaire(menage, dateMariage, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);

				// Ajoute les déclarations qui vont bien
				for (int i = 2003; i <= 2008; ++i) {
					PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(menage, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				// Divorce au 11 novembre 2007
				metierService.separe(menage, dateDivorce, "", EtatCivil.DIVORCE, null);

				return null;
			}
		});

		// Ménage-commun
		{
			// Vérifie que des tâches d'annulation des DIs 2007 et 2008 sont générées sur le ménage
			final List<TacheAnnulationDeclarationImpot> annulations = tacheDAO.listTaches(ids.menageId, TypeTache.TacheAnnulationDeclarationImpot);
			assertNotNull(annulations);
			assertEquals(2, annulations.size());

			sortTachesAnnulation(annulations);
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), annulations.get(0));
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2008, 1, 1), date(2008, 12, 31), annulations.get(1));

			// Vérifie qu'il y a bien une tâche de contrôle de dossier sur le ménage
			final List<TacheControleDossier> controles = tacheDAO.listTaches(ids.menageId, TypeTache.TacheControleDossier);
			assertNotNull(controles);
			assertEquals(1, controles.size());
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, controles.get(0));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheEnvoiDeclarationImpot));
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheNouveauDossier));
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheTransmissionDossier));
		}

		// Monsieur
		{
			// Vérifie que des tâches d'émission des DIs 2007 et 2008 sont générées
			final List<TacheEnvoiDeclarationImpot> envois = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheEnvoiDeclarationImpot);
			assertNotNull(envois);
			assertEquals(RegDate.get().year() - 2007, envois.size());

			sortTachesEnvoi(envois);

			// pour 2007, on ne trouve pas de DI mais on considère le contribuable comme nouvel assujetti (n-1 n'a pas d'assujettissement du tout), donc VAUD_TAX
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI,
					envois.get(0));

			// après 2007, on ne trouve pas de DI et on ne considère pas le contribuable comme nouvel assujetti (n-1 a un assujettissement ordinaire), donc COMPLETE
			for (int i = 2008; i < RegDate.get().year(); ++i) {
				final TacheEnvoiDeclarationImpot tache = envois.get(i - 2007);
				final RegDate dateEcheance = determineDateEcheance(today, nextSunday, nextFinEnvoiEnMasse, tache.getDateDebut().year());
				assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, tache);
			}

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")
			//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
//			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier);
//			assertNotNull(nouveaux);
//			assertEquals(1, nouveaux.size());
//			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheAnnulationDeclarationImpot));
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheControleDossier));
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheTransmissionDossier));
		}

		// Madame
		{
			// Vérifie que des tâches d'émission des DIs 2007 et 2008 sont générées
			final List<TacheEnvoiDeclarationImpot> envois = tacheDAO.listTaches(ids.madameId, TypeTache.TacheEnvoiDeclarationImpot);
			assertNotNull(envois);
			assertEquals(RegDate.get().year() - 2007, envois.size());

			sortTachesEnvoi(envois);

			// pour 2007, on ne trouve pas de DI mais on considère le contribuable comme nouvel assujetti (n-1 n'a pas d'assujettissement du tout), donc VAUD_TAX
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI,
					envois.get(0));

			// après 2007, on ne trouve pas de DI et on ne considère pas le contribuable comme nouvel assujetti (n-1 a un assujettissement ordinaire), donc COMPLETE
			for (int i = 2008; i < RegDate.get().year(); ++i) {
				final TacheEnvoiDeclarationImpot tache = envois.get(i - 2007);
				final RegDate dateEcheance = determineDateEcheance(today, nextSunday, nextFinEnvoiEnMasse, tache.getDateDebut().year());
				assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, tache);
			}

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")

			//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
//			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier);
//			assertNotNull(nouveaux);
//			assertEquals(1, nouveaux.size());
//			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheAnnulationDeclarationImpot));
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheControleDossier));
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheTransmissionDossier));
		}

	}

	/**
	 * [UNIREG-1112] Vérifie qu'il y a le nombre correct d'annulation de DIs et d'émission de DIs générées lors du décès d'un des composants d'un ménage commun pour une période passée (= événement de
	 * décès reçu en retard).
	 * <p/>
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerateTacheDecesCouplePeriodePassee() throws Exception {

		final RegDate dateMariage = date(2002, 5, 1);
		final RegDate dateDeces = date(2007, 11, 11);
		final RegDate nextSunday = getNextSunday(RegDate.get());

		class Ids {
			Long monsieurId;
			Long madameId;
			Long menageId;
		}
		final Ids ids = new Ids();

		final long idIndividuMonsieur = 253828;
		final long idIndividuMadame = 157837;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				MockIndividu monsieur = addIndividu(idIndividuMonsieur, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
				addAdresse(monsieur, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null);

				MockIndividu madame = addIndividu(idIndividuMadame, RegDate.get(1968, 3, 27), "Lavanchy", "Laurence", false);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1968, 3, 27), null);
				addNationalite(madame, MockPays.Suisse, date(1963, 1, 1), null);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Crée le ménage
				PersonnePhysique monsieur = addHabitant(idIndividuMonsieur);
				addForPrincipal(monsieur, date(1981, 1, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.monsieurId = monsieur.getNumero();

				PersonnePhysique madame = addHabitant(idIndividuMadame);
				addForPrincipal(madame, date(1986, 3, 27), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.madameId = madame.getNumero();

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.menageId = menage.getNumero();

				// Ajoute les déclarations qui vont bien
				for (int i = 2003; i <= 2008; ++i) {
					PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(menage, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				// Décès de monsieur au 11 novembre 2007
				metierService.deces(monsieur, dateDeces, "", null);

				return null;
			}
		});

		// Ménage
		{

			//[UNIREG-1305]
			// Vérifie qu'une tâche d'émission de DIs pour 2007 (période partielle) est générée sur le ménage.
			final List<TacheEnvoiDeclarationImpot> envois = tacheDAO.listTaches(ids.menageId, TypeTache.TacheEnvoiDeclarationImpot);
			assertNotNull(envois);
			//La DI 2007 existe, pas de tache d'envoi de déclaration a générer
			assertEquals(0, envois.size());


			// Vérifie qu'une tâche d'annulation de DI est générée sur le ménage pour 2008
			final List<TacheAnnulationDeclarationImpot> annulations = tacheDAO.listTaches(ids.menageId, TypeTache.TacheAnnulationDeclarationImpot);
			assertNotNull(annulations);
			assertEquals(1, annulations.size());

			sortTachesAnnulation(annulations);
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2008, 1, 1), date(2008, 12, 31), annulations.get(0));

			// Vérifie qu'une tache de transmission de dossier pour l'office d'impôt du contribuable a été générée
			final List<TacheTransmissionDossier> transmissions = tacheDAO.listTaches(ids.menageId, TypeTache.TacheTransmissionDossier);
			assertNotNull(transmissions);
			assertEquals(1, transmissions.size());
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, transmissions.get(0));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheControleDossier));
			assertEmpty(tacheDAO.listTaches(ids.menageId, TypeTache.TacheNouveauDossier));
		}

		// Monsieur (décédé)
		{
			// Vérifier qu'aucune tâche d'émission de DIs n'existe sur le tiers décédé
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheEnvoiDeclarationImpot));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheAnnulationDeclarationImpot));
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheControleDossier));
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier));
			assertEmpty(tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheTransmissionDossier));
		}

		// Madame (survivante)
		{
			// Vérifie que des tâches d'émission des DIs 2007 et 2008 sont générées sur le tiers survivant.
			final List<TacheEnvoiDeclarationImpot> envois = tacheDAO.listTaches(ids.madameId, TypeTache.TacheEnvoiDeclarationImpot);
			assertNotNull(envois);
			// [UNIREG-1265] Plus de création de tâche de génération de DI pour les décès
			//assertEquals(0, envois.size());

			sortTachesEnvoi(envois);
			assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(RegDate.get()), dateDeces.getOneDayAfter(), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(0));
			assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(RegDate.get()), date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, envois.get(1));

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")
			//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
//			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.madameId, TypeTache.TacheNouveauDossier);
//			assertNotNull(nouveaux);
//			assertEquals(1, nouveaux.size());
//			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

			// Vérifie qu'il n'y a aucune autre tâche
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheAnnulationDeclarationImpot));
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheControleDossier));
			assertEmpty(tacheDAO.listTaches(ids.madameId, TypeTache.TacheTransmissionDossier));

		}
	}

	/**
	 * [UNIREG-1327] Vérifie que la tâche générée par l'arrivée de HS tardive (= événement traité en 2009 pour une arrivée en 2005, par exemple) d'un contribuable qui possède déjà un immeuble est
	 * correcte. Et tout spécialement que la période d'imposition calculée s'étend bien du 1er janvier (= assujetti par l'immeuble) au 31 décembre (= assujetti comme vaudois).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTachesDepuisArriveeHSContribuablePossedantDejaImmeuble() throws Exception {

		final RegDate dateArrivee = date(2005, 5, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(100000, RegDate.get(1963, 1, 1), "Duplot", "Simon", true);
				addAdresse(monsieur, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null);
			}
		});

		class Ids {
			Long simonId;
			Long oidCedi;
		}
		final Ids ids = new Ids();

		/*
		 * Un contribuable domicilié au Danemark et qui possède un immeuble depuis 2000 à Cossonay
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
				ids.oidCedi = cedi.getId();

				final Contribuable simon = addHabitant(100000);
				ids.simonId = simon.getNumero();
				addForPrincipal(simon, date(1981, 1, 1), MotifFor.MAJORITE, MockPays.Danemark);
				addForSecondaire(simon, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				for (int i = 2003; i < RegDate.get().year(); ++i) {
					PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(simon, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.HORS_SUISSE, modele);
				}

				// Arrivée de hors-Suisse traitée tardivement
				tiersService.closeForFiscalPrincipal(simon, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS);
				tiersService.openForFiscalPrincipal(simon, dateArrivee, MotifRattachement.DOMICILE, MockCommune.Lausanne.getNoOFS(),
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE, MotifFor.ARRIVEE_HS);
				return null;
			}
		});

		final Contribuable simon = (Contribuable) tiersService.getTiers(ids.simonId);
		assertNotNull(simon);

		// [UNIREG-2305] Arrivé de hors-Suisse enregistrée tardivement -> l'assujettissement passe de hors-Suisse à ordinaire à partir du 1er janvier 2005,
		// mais comme le type de document reste le même (déclaration ordinaire), le type de contribuable est simplement mis-à-jour sur les déclarations et
		// aucune tâche n'est créée.

		// aucune tâche de manipulation de DIs ne doit être créée
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setContribuable(simon);
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		assertEmpty(tacheDAO.find(criterion));
		criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
		assertEmpty(tacheDAO.find(criterion));

		// les types de contribuables doivent avoir été mis-à-jour
		for (int i = 2005; i < RegDate.get().year(); ++i) {
			final List<Declaration> declarations = simon.getDeclarationsForPeriode(i, false);
			assertNotNull(declarations);
			assertEquals(1, declarations.size());
			assertDI(date(i, 1, 1), date(i, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					ids.oidCedi, null, declarations.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTachesDepuisAnnulationDeFor() throws Exception {

		// 1 Contribuable avec 1 for courant sur 2008 + 1 declaration 2008
		// On annule le for : la tache de annulation de di doit etre creer

		final RegDate nextSunday = getNextSunday(RegDate.get());

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				serviceCivil.setUp(new MockServiceCivil() {
					@Override
					protected void init() {

						MockIndividu monsieur = addIndividu(100000, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
						addAdresse(monsieur, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
								.get(1963, 1, 1), null);
						addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null);
					}
				});
				Contribuable raoul = addHabitant(100000);
				addForPrincipal(raoul, date(2008, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.raoulId = raoul.getNumero();
				PeriodeFiscale pf2008 = pfDAO.getPeriodeFiscaleByYear(2008);
				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2008);
				addDeclarationImpot(raoul, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				tiersService.annuleForFiscal(raoul.getForFiscalPrincipalAt(date(2008, 1, 1)));
				return null;
			}
		});

		// Annulation du for fiscal -> Une tache d'annulation pour la DI 2008 doit etre generée
		List<Tache> taches = tacheDAO.getAll();
		assertEquals(1, taches.size());
		assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2008, 1, 1), date(2008, 12, 31), (TacheAnnulationDeclarationImpot) taches.get(0));

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenereTacheDepuisFermetureForPrincipalUNIREG1303() throws Exception {

		// 1 Contribuable qui décéde avec 1 déclaration active : la période de la DI doit etre ajusté
		// et aucune tache d'émission de DI ne doit être émise.

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				serviceCivil.setUp(new MockServiceCivil() {
					@Override
					protected void init() {

						MockIndividu monsieur = addIndividu(100000, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
						addAdresse(monsieur, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
								.get(1963, 1, 1), null);
						addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null);
					}
				});
				Contribuable raoul = addHabitant(100000);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				for (int i = 2003; i <= 2008; i++) {
					PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(raoul, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele).getId();
				}

				tiersService.closeForFiscalPrincipal(raoul, date(2008, 11, 1), MotifFor.VEUVAGE_DECES);
				return null;
			}
		});

		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);

		final Declaration di2008 = raoul.getDeclarationActive(date(2008, 1, 1));
		assertEquals(date(2008, 11, 1), di2008.getDateFin());
		// Annulation du for fiscal -> Une tache d'annulation pour la DI 2008 doit etre generée
		for (Tache t : tacheDAO.getAll()) {
			if (t.getTypeTache() == TypeTache.TacheEnvoiDeclarationImpot) {
				fail("Une tache d'envoi de DI n'aurait pas du être émise");
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testgenereTacheDepuisFermetureForPrincipalUNIREG1305() throws Exception {

		// 1 Contribuable qui décéde avec sans déclarartion active : une tache d'émission de DI doit être généré.

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				serviceCivil.setUp(new MockServiceCivil() {
					@Override
					protected void init() {

						MockIndividu monsieur = addIndividu(100000, date(1963, 1, 1), "Lavanchy", "Raoul", true);
						addAdresse(monsieur, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
								.get(1963, 1, 1), null);
						addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null);
					}
				});
				Contribuable raoul = addHabitant(100000);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				return null;
			}
		});

		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);
		tiersService.closeForFiscalPrincipal(raoul, date(2008, 11, 1), MotifFor.VEUVAGE_DECES);
		// Annulation du for fiscal -> Une tache d'annulation pour la DI 2008 doit etre generée
		boolean trouve = false;
		for (Tache t : tacheDAO.getAll()) {
			if (TypeTache.TacheEnvoiDeclarationImpot == t.getTypeTache()) {
				trouve=true;
				break;
			}

		}
		assertTrue("Pas de Di généré",trouve);
	}

	/**
	 * [UNIREG-1218] Vérifie que l'annulation d'un contribuable annule bien toutes les tâches associées à ce contribuable (à l'exception des tâches d'annulation de DIs qui restent valables).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnAnnulationContribuableSansTache() throws Exception {

		// Un contribuable normal avec des tâches associées.

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu raoul = addIndividu(100000, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
				addAdresse(raoul, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1963, 1, 1), null);
				addNationalite(raoul, MockPays.Suisse, date(1963, 1, 1), null);
			}
		});

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Contribuable raoul = addHabitant(100000);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

				// annulation du tiers
				tiersService.annuleTiers(raoul);
				return null;
			}
		});

		final List<Tache> taches = tacheDAO.find(ids.raoulId);
		assertEmpty(taches);
	}

	/**
	 * [UNIREG-1218] Vérifie que l'annulation d'un contribuable annule bien toutes les tâches associées à ce contribuable (à l'exception des tâches d'annulation de DIs qui restent valables).
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnAnnulationContribuableAvecTaches() throws Exception {

		// Un contribuable normal sans aucune tâche associée.

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu raoul = addIndividu(100000, date(1963, 1, 1), "Lavanchy", "Raoul", true);
				addAdresse(raoul, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1963, 1, 1), null);
				addNationalite(raoul, MockPays.Suisse, date(1963, 1, 1), null);
			}
		});

		class Ids {
			Long raoulId;
			Long tacheAnnulDI;
			Long tacheControl;
			Long tacheEnvoi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				assertNotNull(colAdm);

				Contribuable raoul = addHabitant(100000);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

				// une tâche d'annulation de DI
				PeriodeFiscale periode = addPeriodeFiscale(2074);
				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				DeclarationImpotOrdinaire declaration = addDeclarationImpot(raoul, periode, date(2074, 1, 1), date(2074, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				final TacheAnnulationDeclarationImpot annulDI = addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, date(2000, 1, 1), declaration,
						raoul, colAdm);
				ids.tacheAnnulDI = annulDI.getId();

				// tâche de contrôle de dossier
				final TacheControleDossier tacheControl = addTacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2000, 1, 1), raoul, colAdm);
				ids.tacheControl = tacheControl.getId();

				// tâche d'envoi de DI
				final TacheEnvoiDeclarationImpot tacheEnvoi = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2000, 1, 1),
						date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						raoul, null, null, colAdm);
				ids.tacheEnvoi = tacheEnvoi.getId();

				// annulation du tiers
				tiersService.annuleTiers(raoul);
				return null;
			}
		});

		final List<Tache> taches = tacheDAO.find(ids.raoulId);
		assertNotNull(taches);
		assertEquals(3, taches.size());

		final Tache tacheAnnul = tacheDAO.get(ids.tacheAnnulDI);
		assertNotNull(tacheAnnul);
		assertFalse("La tâche d'annulation de DI ne devrait pas être annulée après l'annulation du tiers", tacheAnnul.isAnnule());

		final Tache tacheControl = tacheDAO.get(ids.tacheControl);
		assertNotNull(tacheControl);
		assertTrue("La tâche de contrôle devrait être annulée après l'annulation du tiers", tacheControl.isAnnule());

		final Tache tacheEnvoi = tacheDAO.get(ids.tacheEnvoi);
		assertNotNull(tacheEnvoi);
		assertTrue("La tâche d'envoi devrait être annulée après l'annulation du tiers", tacheEnvoi.isAnnule());
	}

	private void sortTachesEnvoi(final List<TacheEnvoiDeclarationImpot> envois) {
		Collections.sort(envois, new Comparator<TacheEnvoiDeclarationImpot>() {
			@Override
			public int compare(TacheEnvoiDeclarationImpot o1, TacheEnvoiDeclarationImpot o2) {
				return o1.getDateDebut().compareTo(o2.getDateDebut());
			}
		});
	}

	private void sortTachesAnnulation(final List<TacheAnnulationDeclarationImpot> annulations) {
		Collections.sort(annulations, new Comparator<TacheAnnulationDeclarationImpot>() {
			@Override
			public int compare(TacheAnnulationDeclarationImpot o1, TacheAnnulationDeclarationImpot o2) {
				return o1.getDeclarationImpotOrdinaire().getDateDebut().compareTo(o2.getDeclarationImpotOrdinaire().getDateDebut());
			}
		});
	}

	private void ouvreForPrincipal(final ModeImposition modeImposition, final MotifFor motifOuverture, final RegDate dateOuverture) throws Exception {
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero((long) 11111111);
				hab.setNumeroIndividu((long) 333908);
				hab = hibernateTemplate.merge(hab);

				final ForFiscalPrincipal f = new ForFiscalPrincipal(dateOuverture, motifOuverture, null, null, 5652, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, modeImposition);
				hab.addForFiscal(f);
				tacheService.genereTacheDepuisOuvertureForPrincipal(hab, f, null);
				return null;
			}
		});
	}

	/**
	 * Verification de la tache controler dossier
	 *
	 * @param criterion
	 */
	private void verifieControleDossier(TacheCriteria criterion) {
		verifieControleDossier(criterion, 1);
	}

	private void verifieControleDossier(TacheCriteria criterion, int nombreResultats) {
		criterion.setTypeTache(TypeTache.TacheControleDossier);
		List<Tache> taches = tacheDAO.find(criterion);
		assertNotNull(taches);
		assertEquals(nombreResultats, taches.size());
	}

	/**
	 * Verification de la tache transmission dossier
	 *
	 * @param criterion
	 */
	@SuppressWarnings("unused")
	private void verifieTransmissionDossier(TacheCriteria criterion) {
		criterion.setTypeTache(TypeTache.TacheTransmissionDossier);
		List<Tache> taches = tacheDAO.find(criterion);
		assertNotNull(taches);
		assertEquals(1, taches.size());
	}

	/**
	 * Verification des taches nouveau dossier
	 */
	private void verifieTacheNouveauDossier(TacheCriteria criterion, int nombreResultats) {

		criterion.setTypeTache(TypeTache.TacheNouveauDossier);
		List<Tache> taches = tacheDAO.find(criterion);
		assertNotNull(taches);
		assertEquals(nombreResultats, taches.size());

	}

	private int getNouveauDossierCount() {
		TacheCriteria c = new TacheCriteria();
		c.setTypeTache(TypeTache.TacheNouveauDossier);
		return tacheDAO.count(c);
	}

	private int getTacheCount() {
		TacheCriteria c = new TacheCriteria();
		c.setTypeTache(TypeTache.TacheNouveauDossier);
		c.setInvertTypeTache(true);
		return tacheDAO.count(c);
	}

	/**
	 * Verification des tâches de contrôle de dossier
	 *
	 */
	private void verifierTacheControleDossier(TacheCriteria criterion, int nombreResultats) {

		criterion.setTypeTache(TypeTache.TacheControleDossier);
		List<Tache> taches = tacheDAO.find(criterion);
		assertNotNull(taches);
		assertEquals(nombreResultats, taches.size());


	}

	/**
	 * Verification des taches d'envoi DI
	 *
	 * @param criterion
	 * @param debutAnnee
	 */
	private void assertTachesEnvoi(TacheCriteria criterion, boolean debutAnnee) {
		{
			List<Tache> taches;
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2006);
			taches = tacheDAO.find(criterion);
			assertNotNull(taches);
			assertEquals(1, taches.size());
			assertTrue(taches.get(0) instanceof TacheEnvoiDeclarationImpot);
			TacheEnvoiDeclarationImpot tacheEnvoi = (TacheEnvoiDeclarationImpot) taches.get(0);
			assertNotNull(tacheEnvoi);
			if (debutAnnee) {
				assertEquals(RegDate.get(2006, 1, 1), tacheEnvoi.getDateDebut());
			}
			else {
				assertEquals(RegDate.get(2006, 6, 12), tacheEnvoi.getDateDebut());
			}
		}

		for (int i = 2007 ; i < RegDate.get().year() ; ++ i) {
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2007);
			final List<Tache> taches = tacheDAO.find(criterion);
			assertNotNull(taches);
			assertEquals(1, taches.size());
			assertTrue(taches.get(0) instanceof TacheEnvoiDeclarationImpot);
			final TacheEnvoiDeclarationImpot tacheEnvoi = (TacheEnvoiDeclarationImpot) taches.get(0);
			assertNotNull(tacheEnvoi);
			assertEquals(RegDate.get(2007, 1, 1), tacheEnvoi.getDateDebut());
		}

		{
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(RegDate.get().year());
			final List<Tache> taches = tacheDAO.find(criterion);
			assertEmpty(taches);
		}
	}

	/**
	 * Verification des taches d'annulation DI
	 *
	 * @param criterion
	 * @param nombreResultats
	 */
	private void verifieTachesAnnulation(TacheCriteria criterion, int nombreResultats, boolean checkPremiereAnnee) {
		if (checkPremiereAnnee) {
			verifieTachesAnnulation(criterion, nombreResultats, 2006);
		}
		verifieTachesAnnulation(criterion, nombreResultats, 2007);
		verifieTachesAnnulation(criterion, 0, 2008);
	}

	private void verifieTachesAnnulation(TacheCriteria criterion, int nombreResultats, int annee) {
		List<Tache> taches;
		TacheAnnulationDeclarationImpot tacheAnnulation;
		criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
		criterion.setAnnee(annee);
		taches = tacheDAO.find(criterion);
		assertNotNull(taches);
		int size = taches.size();
		assertEquals(nombreResultats, size);
		for (Tache tache : taches) {
			assertTrue(tache instanceof TacheAnnulationDeclarationImpot);
			tacheAnnulation = (TacheAnnulationDeclarationImpot) tache;
			assertNotNull(tacheAnnulation);
		}
	}


	private void verifieAbsenceDIAnnulee(long noContribuable,int annee) {
		List<DeclarationImpotOrdinaire> declarations;

		DeclarationImpotCriteria criterion = new DeclarationImpotCriteria();
		criterion.setAnnee(annee);
		criterion.setContribuable(noContribuable);
		declarations = diDAO.find(criterion);
		for (DeclarationImpotOrdinaire declarationImpotOrdinaire : declarations) {
			assertNull(declarationImpotOrdinaire.getAnnulationDate());
		}


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testChangementModeImpositionQuitteSourceVersMixte() throws Exception {
		final List<Tache> taches = genereChangementImposition(ModeImposition.SOURCE, ModeImposition.MIXTE_137_2);
		//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(RegDate.get().year() - 2006, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testChangementModeImpositionQuitteSourceVersDepense() throws Exception {
		final List<Tache> taches = genereChangementImposition(ModeImposition.SOURCE, ModeImposition.DEPENSE);
		//[SIFISC-3357] Plus de tâche nouveau dossier pour les contribuables vaudois
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(RegDate.get().year() - 2006, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testChangementModeImpositionOrdinaireVersIndigent() throws Exception {
		final List<Tache> taches = genereChangementImposition(ModeImposition.ORDINAIRE, ModeImposition.INDIGENT);
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(0, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testChangementModeImpositionIndigentVersOrdinaire() throws Exception {
		final List<Tache> taches = genereChangementImposition(ModeImposition.INDIGENT, ModeImposition.ORDINAIRE);
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(0, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testChangementModeImpositionMixteVersOrdinaire() throws Exception {
		final List<Tache> taches = genereChangementImposition(ModeImposition.MIXTE_137_2, ModeImposition.ORDINAIRE);
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(0, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testChangementModeImpositionMixteVersDepense() throws Exception {
		final List<Tache> taches = genereChangementImposition(ModeImposition.MIXTE_137_2, ModeImposition.DEPENSE);
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		// annulation et ré-envoi des DIs 2006..[année précédente] parce que les types de document ont changé
		// [UNIREG-3281] les types de document sont mis-à-jour automatiquement dorénavant -> pas de tâche créée
		assertEquals(0, countTaches(TypeTache.TacheAnnulationDeclarationImpot, taches));
		assertEquals(0, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	/**
	 * [UNIREG-2806] Vérifie que l'ouverture d'un for principal avec motif d'ouverture 'permis C/Suisse' planifie une réindexation pour le 1er du mois suivant.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddForPrincipalDateReindexationFutur1() throws Exception {

		final PersonnePhysique pp = addNonHabitant("Philippe", "Macaron", date(1970, 1, 1), Sexe.MASCULIN);
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
		ffp.setModeImposition(ModeImposition.SOURCE);

		// état initial : pas de réindexation prévue dans le futur
		assertNull(pp.getReindexOn());

		tiersService.addForPrincipal(pp, date(2010, 11, 23), MotifFor.PERMIS_C_SUISSE, null, null, MotifRattachement.DOMICILE, MockCommune.Chamblon.getNoOFS(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE);

		// après changement du mode d'imposition : réindexation prévue pour le 1er du mois suivant
		assertEquals(date(2010, 12, 1), pp.getReindexOn());
	}

	/**
	 * [UNIREG-2806] Vérifie que l'ouverture d'un for principal avec motif d'ouverture 'changement mode d'imposition' planifie une réindexation pour le 1er du mois suivant.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddForPrincipalDateReindexationFutur2() throws Exception {

		final PersonnePhysique pp = addNonHabitant("Philippe", "Macaron", date(1970, 1, 1), Sexe.MASCULIN);
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
		ffp.setModeImposition(ModeImposition.SOURCE);

		// état initial : pas de réindexation prévue dans le futur
		assertNull(pp.getReindexOn());

		tiersService.addForPrincipal(pp, date(2010, 11, 23), MotifFor.CHGT_MODE_IMPOSITION, null, null, MotifRattachement.DOMICILE, MockCommune.Chamblon.getNoOFS(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE);

		// après changement du mode d'imposition : réindexation prévue pour le 1er du mois suivant
		assertEquals(date(2010, 12, 1), pp.getReindexOn());
	}

	/**
	 * [UNIREG-2806] Vérifie qu'un changement du mode d'imposition planifie une réindexation pour le 1er du mois suivant.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testChangeModeImpositionDateReindexationFutur() throws Exception {

		final PersonnePhysique pp = addNonHabitant("Philippe", "Macaron", date(1970, 1, 1), Sexe.MASCULIN);
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
		ffp.setModeImposition(ModeImposition.SOURCE);

		// état initial : pas de réindexation prévue dans le futur
		assertNull(pp.getReindexOn());

		tiersService.changeModeImposition(pp, date(2010, 11, 23), ModeImposition.MIXTE_137_2, MotifFor.CHGT_MODE_IMPOSITION);

		// après changement du mode d'imposition : réindexation prévue pour le 1er du mois suivant
		assertEquals(date(2010, 12, 1), pp.getReindexOn());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationForSansOidGestion() throws Exception {

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Marcelin", "Emile", date(1946, 1, 5), Sexe.MASCULIN);
				final RegDate dateArrivee = date(2008, 4, 1);
				addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, dateArrivee, null, MockRue.Bex.RouteDuBoet);

				final ForFiscalPrincipal ffp = addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bex);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// vérification qu'il n'y a pas d'OID associé au contribuable, puis annulation du for
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				final CollectiviteAdministrative oid = tiersService.getOfficeImpotAt(pp, null);
				assertNull(oid);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
				tiersService.annuleForFiscal(ffp);

				return null;
			}
		});

		// aucune tâche générée ?
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNull(ffp);

				final List<Tache> taches = tacheDAO.find(ppId);
				assertNotNull(taches);
				assertEquals(0, taches.size());

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSeparationSurContribuableHS() throws Exception {

		class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Achille", "Talon", date(1965, 12, 5), Sexe.MASCULIN);
				lui.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final PersonnePhysique elle = addNonHabitant("Géraldine", "Talon", date(1966, 4, 12), Sexe.FEMININ);
				elle.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(1990, 5, 1), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(1995, 1, 10), MotifFor.INDETERMINE, MockPays.France);

				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = mc.getNumero();
				return null;
			}
		});

		// séparation
		final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.idMenage);
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, null);

		metierService.separe(mc, date(2009, 6, 12), "Test", EtatCivil.SEPARE, null);

		// vérification des tâches générées : pour lui
		{
			final TacheCriteria criteria = new TacheCriteria();
			criteria.setContribuable(couple.getPrincipal());
			final List<Tache> taches = tacheDAO.find(criteria);
			assertNotNull(taches);
			assertEquals(0, taches.size());
		}
		// vérification des tâches générées : pour elle
		{
			final TacheCriteria criteria = new TacheCriteria();
			criteria.setContribuable(couple.getConjoint());
			final List<Tache> taches = tacheDAO.find(criteria);
			assertNotNull(taches);
			assertEquals(0, taches.size());
		}
		// vérification des tâches générées : pour le ménage
		{
			final TacheCriteria criteria = new TacheCriteria();
			criteria.setContribuable(mc);
			final List<Tache> taches = tacheDAO.find(criteria);
			assertNotNull(taches);
			assertEquals(0, taches.size());
		}
	}

	/**
	 * [SIFISC-60] Vérifie que la séparation d'un couple HS avec immeuble ne génère pas de tâche d'envoi de DIs suite à la pseudo-fermeture du dernier immeuble.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSeparationSurContribuableHSAvecImmeuble() throws Exception {

		class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Achille", "Talon", date(1965, 12, 5), Sexe.MASCULIN);
				lui.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final PersonnePhysique elle = addNonHabitant("Géraldine", "Talon", date(1966, 4, 12), Sexe.FEMININ);
				elle.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(1990, 5, 1), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(1995, 1, 10), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(mc, date(1995, 1, 10), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = mc.getNumero();
				return null;
			}
		});

		// séparation
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.idMenage);
				metierService.separe(mc, date(2009, 6, 12), "Test", EtatCivil.SEPARE, null);
				return null;
			}
		});

		final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.idLui);
		assertNotNull(lui);
		final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.idElle);
		assertNotNull(elle);
		final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
		assertNotNull(mc);

		// vérification des tâches générées : pour lui
		{
			// toujours pas de tâche, car on ne sait pas sur quel contribuable mettre le for secondaire
			// (mais une tâche de contrôle de dossier doit avoir été ouverte sur le couple)

			final TacheCriteria criteria = new TacheCriteria();
			criteria.setContribuable(lui);
			final List<Tache> taches = tacheDAO.find(criteria);
			assertNotNull(taches);
			assertEquals(0, taches.size());
		}
		// vérification des tâches générées : pour elle
		{
			// toujours pas de tâche, car on ne sait pas sur quel contribuable mettre le for secondaire
			// (mais une tâche de contrôle de dossier doit avoir été ouverte sur le couple)

			final TacheCriteria criteria = new TacheCriteria();
			criteria.setContribuable(elle);
			final List<Tache> taches = tacheDAO.find(criteria);
			assertNotNull(taches);
			assertEquals(0, taches.size());
		}
		// vérification des tâches générées : pour le ménage
		{
			final TacheCriteria criteria = new TacheCriteria();
			criteria.setContribuable(mc);
			final List<Tache> taches = tacheDAO.find(criteria);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(TacheControleDossier.class, tache.getClass());
		}
	}

	/**
	 * [UNIREG-2439] Vérifie qu'aucune tâche d'envoi de DIs n'est émise lors du divorce d'un ménage commun de sourciers purs.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDivorceMenageCommunSourcePur() throws Exception {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ilya = addIndividu(915112, date(1967, 8, 1), "Eigenbrot", "Ilya", true);
				addAdresse(ilya, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2008, 8, 22), null);
				final MockIndividu katharine = addIndividu(915113, date(1969, 8, 15), "Darling", "Katharine", false);
				addAdresse(katharine, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2008, 8, 22), null);
			}
		});

		class Ids {
			long ilya;
			long katharine;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple de sourciers arrivés de hors-Suisse en 2008
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique ilya = addHabitant(915112);
				ids.ilya = ilya.getNumero();

				final PersonnePhysique katharine = addHabitant(915113);
				ids.katharine = katharine.getNumero();

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ilya, katharine, date(2004, 4, 24), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.menage = menage.getNumero();

				final ForFiscalPrincipal ffp = addForPrincipal(menage, date(2008, 8, 22), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return null;
			}
		});

		// Vérifie qu'il n'y a aucune tâche
		assertEmpty(tacheDAO.find(ids.ilya));
		assertEmpty(tacheDAO.find(ids.katharine));
		assertEmpty(tacheDAO.find(ids.menage));

		// Effectue un divorce
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun menage = hibernateTemplate.get(MenageCommun.class, ids.menage);
				metierService.separe(menage, date(2008, 11, 15), null, EtatCivil.DIVORCE, null);
				return null;
			}
		});

		// Vérifie qu'ils sont bien divorcés
		{
			final PersonnePhysique ilya = hibernateTemplate.get(PersonnePhysique.class, ids.ilya);
			final ForFiscalPrincipal dernier = ilya.getDernierForFiscalPrincipal();
			assertNotNull(dernier);
			assertEquals(date(2008, 11, 15), dernier.getDateDebut());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dernier.getMotifOuverture());
		}

		{
			final PersonnePhysique katharine = hibernateTemplate.get(PersonnePhysique.class, ids.katharine);
			final ForFiscalPrincipal dernier = katharine.getDernierForFiscalPrincipal();
			assertNotNull(dernier);
			assertEquals(date(2008, 11, 15), dernier.getDateDebut());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dernier.getMotifOuverture());
		}

		{
			final MenageCommun menage = hibernateTemplate.get(MenageCommun.class, ids.menage);
			final ForFiscalPrincipal dernier = menage.getDernierForFiscalPrincipal();
			assertNotNull(dernier);
			assertEquals(date(2008, 11, 14), dernier.getDateFin());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dernier.getMotifFermeture());
		}

		// Vérifie qu'il y a toujours aucune tâche
		assertEmpty(tacheDAO.find(ids.ilya));
		assertEmpty(tacheDAO.find(ids.katharine));
		assertEmpty(tacheDAO.find(ids.menage));
	}

	// Note: ce test était initialement dans la classe TiersServiceTest

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFermetureForFiscalPrincipalUNIREG1888() throws Exception {
		final RegDate dateOuverture = RegDate.get(2006, 7, 1);

		// création des DI 2006 et 2007 avant la notification du départ en 2007
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale pf2006 = pfDAO.getPeriodeFiscaleByYear(2006);
				final PeriodeFiscale pf2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2006);
				addModeleFeuilleDocument("Déclaration", "210", modele2006);
				addModeleFeuilleDocument("Annexe 1", "220", modele2006);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2006);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2006);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2007);
				addModeleFeuilleDocument("Déclaration", "210", modele2007);
				addModeleFeuilleDocument("Annexe 1", "220", modele2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2007);

				// Contribuable vaudois depuis 1998 avec des DIs jusqu'en 2007
				final PersonnePhysique pp = addNonHabitant("Gérald", "Bolomey", date(1945, 2, 23), Sexe.MASCULIN);
				addForPrincipal(pp, dateOuverture, MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				addDeclarationImpot(pp, pf2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(pp, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
				return pp.getNumero();
			}
		});

		// départ en 2007
		final RegDate depart = date(2007, 10, 31);
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.closeForFiscalPrincipal(ffp, depart, MotifFor.DEPART_HS);
				return null;
			}
		});

		// la DI 2007 doit avoir une durée de validité réduite suite au départ
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
				final Declaration di = pp.getDeclarationActive(date(2007, 6, 30));
				assertEquals("La date de fin de la DI 2007 n'a pas été ramené suite au départ HS", depart, di.getDateFin());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsOptionnelles() throws Exception {

		// Contribuable hors-Suisse avec un immeuble dans le canton
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.Albanie);
		addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// (précondition) La déclaration d'impôt est optionnelle
		final List<PeriodeImposition> periodes = periodeImpositionService.determine(pp, 2005);
		assertNotNull(periodes);
		assertEquals(1, periodes.size());

		final PeriodeImposition periode0 = periodes.get(0);
		assertNotNull(periode0);
		assertTrue(periode0.isOptionnelle());
		assertFalse(periode0.isRemplaceeParNote());
		assertFalse(periode0.isDiplomateSuisseSansImmeuble());

		// On vérifie que même s'il n'y a pas de déclaration, aucune tâche n'est créée (puisque les déclarations sont toutes optionnelles)
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsRemplaceesParNote() throws Exception {

		// Contribuable hors-Canton qui commence une activité indépendante à Renens et l'arrête la même année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2005, 1, 1), MotifFor.DEBUT_EXPLOITATION, date(2005, 8, 1), MotifFor.FIN_EXPLOITATION, MockCommune.Geneve);
		addForSecondaire(pp, date(2005, 1, 1), MotifFor.DEBUT_EXPLOITATION, date(2005, 8, 1), MotifFor.FIN_EXPLOITATION, MockCommune.Renens.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

		// (précondition) La déclaration d'impôt est remplacée par une note à l'administration fiscale de l'autre canton
		final List<PeriodeImposition> periodes = periodeImpositionService.determine(pp, 2005);
		assertNotNull(periodes);
		assertEquals(1, periodes.size());

		final PeriodeImposition periode0 = periodes.get(0);
		assertNotNull(periode0);
		assertFalse(periode0.isOptionnelle());
		assertTrue(periode0.isRemplaceeParNote());
		assertFalse(periode0.isDiplomateSuisseSansImmeuble());

		// On vérifie que même s'il n'y a pas de déclaration, aucune tâche n'est créée (puisque la déclaration est remplacée par une note)
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsCasDuDiplomateSuisse() throws Exception {

		// Contribuable du diplomate suisse basé à l'étranger
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2005, 1, 1), MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DIPLOMATE_SUISSE);

		// (précondition) La déclaration d'impôt est envoyée par la confédération, ce n'est pas la responsabilité du canton
		final List<PeriodeImposition> periodes = periodeImpositionService.determine(pp, 2005);
		assertNotNull(periodes);
		assertEquals(1, periodes.size());

		final PeriodeImposition periode0 = periodes.get(0);
		assertNotNull(periode0);
		assertFalse(periode0.isOptionnelle());
		assertFalse(periode0.isRemplaceeParNote());
		assertTrue(periode0.isDiplomateSuisseSansImmeuble());

		// On vérifie que même s'il n'y a pas de déclaration, aucune tâche n'est créée (puisque la déclaration ne doit pas être envoyée par le canton)
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsSansDIAvecTacheEnvoiPreexistante() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		// une tâche d'envoi en instance qui correspond parfaitement à la déclaration manquante
		addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2006, 1, 1), date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_VAUDTAX, pp, Qualification.AUTOMATIQUE, 0, cedi);

		// On vérifie qu'aucune nouvelle tâche n'est créée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsSansDIAvecTacheEnvoiPreexistanteMaisIncorrecte() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		// une tâche d'envoi en instance qui ne possède pas le bon type de contribuable
		TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2006, 1, 1), date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE,
				TypeDocument.DECLARATION_IMPOT_DEPENSE, pp, Qualification.AUTOMATIQUE, 0, cedi);

		// On vérifie que :
		// - la tâche incorrecte est annulée
		// - une nouvelle tâche est créée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(2, actions.size());
		assertAddDI(date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
		assertAnnuleTache(tache, actions.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsSansDISansForsAvecTacheEnvoi() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		// une tâche d'envoi en instance qui est incorrect
		TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2006, 1, 1), date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE,
				TypeDocument.DECLARATION_IMPOT_DEPENSE, pp, Qualification.AUTOMATIQUE, 0, cedi);

		// On vérifie que la tâche est annulée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertAnnuleTache(tache, actions.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsUneDI() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration qui couvre toute l'année (incorrect) avec un type de contribuable HS (incorrect aussi)
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);

		// On vérifie que la déclaration est mise-à-jour
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertUpdateDI(di, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsUneDIHorsPeriode() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration qui couvre le premier mois de l'année (incorrect) avec un type de contribuable vaudois ordinaire (correct)
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 1, 31), TypeContribuable.HORS_SUISSE, modele);

		// On vérifie que :
		// - la déclaration est annulée (le dates n'ont rien à voir avec la période d'imposition)
		// - une nouvelle déclaration est créée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(2, actions.size());
		assertAddDI(date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
		assertDeleteDI(di, false, actions.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsUneDITypeContribuableIncompatible() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode);
		// une déclaration qui couvre tout l'année (incorrect) avec un type de contribuable vaudois dépense (incorrect)
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modele);

		// On vérifie que :
		// - [UNIREG-3281] les types de document sont mis-à-jour automatiquement -> une action de mise-à-jour est créée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertUpdateDI(di, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsUneDIAvecTacheAnnulationIncorrecte() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration qui est correcte
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

		// une tâche d'annulation de la déclaration (incorrecte car la déclaration correspond bien à la période d'imposition)
		final TacheAnnulationDeclarationImpot tache = addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, RegDate.get(), di, pp, cedi);

		// On vérifie que :
		// - la tache incorrecte est annulée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertAnnuleTache(tache, actions.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsUneDIAvecTacheAnnulationCorrecte() throws Exception {

		final int anneeCourante = RegDate.get().year();
		final int anneePrecedente = anneeCourante - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneeCourante);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
		// une déclaration incorrecte (mauvaise date de début) qui ne peut pas être corrigée car elle va jusqu'à la fin de l'année
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneeCourante, 5, 12), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modele);

		// une tâche d'annulation de la déclaration
		addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, RegDate.get(), di, pp, cedi);

		// On vérifie que :
		// - la tache correcte n'est pas annulée
		// - une tâche de création de DI est créée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertAddDI(date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsUneDISansForFiscal() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration émise seulement
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);
		addEtatDeclarationEmise(di, date(anneePrecedente + 1, 1, 15));

		// On vérifie que la déclaration est bien annulée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertDeleteDI(di, true, actions.get(0)); // direct=true -> parce que la déclaration est seulement émise
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsUneDISansForFiscalAvecTacheAnnulation() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);

		// une tâche d'annulation de la déclaration qui va bien
		addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, RegDate.get(), di, pp, cedi);

		// On vérifie qu'aucune nouvelle tâche n'est créée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsUneDIDejaAnnuleeSansForFiscalAvecTacheAnnulation() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration déjà annulée
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);
		di.setAnnule(true);

		// une tâche d'annulation de la déclaration (obsolète car la déclaration est déjà annulée)
		final TacheAnnulationDeclarationImpot tache = addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, RegDate.get(), di, pp, cedi);

		// On vérifie que :
		// - la tache obsolète est annulée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertAnnuleTache(tache, actions.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsDeuxDIsQuiSeChevauchent() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année, avec deux DIs se chevauchant pour cette période
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une première déclaration qui couvre toute l'année (incorrect) avec un type de contribuable HS (incorrect aussi)
		final DeclarationImpotOrdinaire di0 = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);
		// une seconde déclaration qui couvre la période en Suisse (correct) avec un type de contribuable HS (incorrect)
		final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, periode, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);

		// On vérifie que :
		// - la première déclaration est annulée
		// - le type de contribuable de la deuxième déclaration est mise-à-jour (car elle correspond parfaitement aux dates de la période)
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(2, actions.size());
		assertUpdateDI(di1, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
		assertDeleteDI(di0, false, actions.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsDeuxDIsQuiSeChevauchentEtTypeContribuableIncompatible() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année, avec deux DIs se chevauchant pour cette période
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modeleComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		final ModeleDocument modeleDepense = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode);
		// une première déclaration qui couvre la période en Suisse (correct) avec un type de contribuable dépense (incorrect)
		final DeclarationImpotOrdinaire di0 = addDeclarationImpot(pp, periode, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modeleDepense);
		// une seconde déclaration qui couvre toute l'année (incorrect) avec un type de contribuable HS (incorrect aussi)
		final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modeleComplete);

		// On vérifie que :
		// - la première déclaration est mise-à-jour (type)
		// - la deuxième déclaration est annulée
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(2, actions.size());
		assertUpdateDI(di0, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
		assertDeleteDI(di1, false, actions.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsDeuxDIsQuiSeChevauchentSansCorrespondanceParfaiteDesDates() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année, avec deux DIs se chevauchant pour cette période
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une première déclaration qui couvre toute l'année (incorrect) avec un type de contribuable HS (incorrect aussi)
		final DeclarationImpotOrdinaire di0 = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);
		// une seconde déclaration qui couvre une période de trois mois (incorrect) avec un type de contribuable HS (incorrect aussi)
		final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, periode, date(anneePrecedente, 3, 1), date(anneePrecedente, 5, 31), TypeContribuable.HORS_SUISSE, modele);

		// On vérifie que :
		// - la première déclaration est mise-à-jour (parce qu'elle est simplement la première et que les dates de la deuxième ne correspondent pas parfaitement avec les dates théoriques)
		// - la deuxième déclaration est annulée
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(2, actions.size());
		assertUpdateDI(di0, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
		assertDeleteDI(di1, false, actions.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsDeuxDIsSansForFiscalAvecUneTacheAnnulation() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une première déclaration
		final DeclarationImpotOrdinaire di0 = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 6, 30), TypeContribuable.HORS_SUISSE, modele);
		// une seconde déclaration
		final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, periode, date(anneePrecedente, 7, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);

		// une tâche d'annulation de la première déclaration
		addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, RegDate.get(), di0, pp, cedi);

		// On vérifie que :
		//  - aucune tâche n'a été créée pour la première déclaration
		//  - une tâche d'annulation a éét créée pour la deuxième déclaration
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertDeleteDI(di1, false, actions.get(0));
	}

	/**
	 * [UNIREG-1456] Cas du contribuable qui arrive de HS et repart HS la même année et dont on reçoit les événements par après : l'arrivée provoque la création d'une tâche d'envoi de DI sur la période
	 * [date arrivée; 31 décembre]. Le départ provoque la création d'une nouvelle tâche d'envoi de DI sur la période [date arrivée; date départ]. Ce test vérifie que le première tâche d'envoi est bien annulée automatiquement.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsArriveeHSEtDepartHSMemeAnnee() throws Exception {

		final RegDate aujourdhui = RegDate.get();
		final RegDate nextSunday = getNextSunday(aujourdhui);
		final RegDate nextFinEnvoiEnMasse = RegDate.get(aujourdhui.year(), 1, 31);
		final int anneePrecedente = aujourdhui.year() - 1;

		// Arrivée de hors-Suisse traitée tardivement
		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gédéon", "Glincarnés", date(1972, 1, 3), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(anneePrecedente, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				tacheService.genereTacheDepuisOuvertureForPrincipal(pp, ffp, null);
				return pp.getNumero();
			}
		});

		// Il devrait maintenant y avoir une tâche d'envoi de DI
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				criterion.setInclureTachesAnnulees(true);

				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final TacheEnvoiDeclarationImpot tache0 = (TacheEnvoiDeclarationImpot) taches.get(0);
				final RegDate dateEcheance = determineDateEcheance(aujourdhui, nextSunday, nextFinEnvoiEnMasse, tache0.getDateDebut().year());
				assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(anneePrecedente, 3, 12), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, tache0);
				return null;
			}
		});

		// Départ de hors-Suisse traité tardivement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
				final ForFiscalPrincipal ffp = pp.getForFiscalPrincipalAt(null);

				ffp.setDateFin(date(anneePrecedente, 7, 23));
				ffp.setMotifFermeture(MotifFor.DEPART_HS);
				tacheService.genereTacheDepuisFermetureForPrincipal(pp, ffp);
				return null;
			}
		});

		// Il devrait maintenant y avoir deux tâches d'envoi de DI : le première annulée et une nouvelle avec la bonne période
		{
			final TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setInclureTachesAnnulees(true);

			final List<Tache> taches = tacheDAO.find(criterion);
			assertNotNull(taches);
			assertEquals(2, taches.size());

			final RegDate dateEcheance = determineDateEcheance(aujourdhui, nextSunday, nextFinEnvoiEnMasse, anneePrecedente);

			final TacheEnvoiDeclarationImpot tache0 = (TacheEnvoiDeclarationImpot) taches.get(0);
			assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(anneePrecedente, 3, 12), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, tache0);
			assertTrue(tache0.isAnnule());

			final TacheEnvoiDeclarationImpot tache1 = (TacheEnvoiDeclarationImpot) taches.get(1);
			assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(anneePrecedente, 3, 12), date(anneePrecedente, 7, 23), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, tache1);
			assertFalse(tache1.isAnnule());
		}
	}

	/**
	 * [UNIREG-1653] Vérifie que l'ouverture dans une période passée d'un for fiscal pour motif veuvage génère bien une tâche d'envoi de DI.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsVeuvageTraiteTardivement() throws Exception {

		final RegDate aujourdhui = RegDate.get();
		final int anneePrecedente = aujourdhui.year() - 1;
		final RegDate nextSunday = getNextSunday(aujourdhui);
		final RegDate nextFinEnvoiEnMasse = RegDate.get(aujourdhui.year(), 1, 31);

		// Veuvage traité tardivement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Michelle", "Mabelle", date(1972, 1, 3), Sexe.FEMININ);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(anneePrecedente, 11, 2), MotifFor.VEUVAGE_DECES, MockCommune.Cossonay);
				tacheService.genereTacheDepuisOuvertureForPrincipal(pp, ffp, null);
				return null;
			}
		});

		// Il devrait maintenant y avoir une tâche d'envoi de DI
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		criterion.setInclureTachesAnnulees(true);

		final List<Tache> taches = tacheDAO.find(criterion);
		assertNotNull(taches);
		assertEquals(1, taches.size());
		final RegDate dateEcheance = determineDateEcheance(aujourdhui, nextSunday, nextFinEnvoiEnMasse, anneePrecedente);

		final TacheEnvoiDeclarationImpot tache0 = (TacheEnvoiDeclarationImpot) taches.get(0);
		assertTache(TypeEtatTache.EN_INSTANCE, dateEcheance, date(anneePrecedente, 11, 2), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
		            TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, tache0);
	}

	/**
	 * [UNIREG-2685] Vérifique que les déclarations valides pour l'année courante (= DIs libres) ne sont pas annulées automatiquement.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsLibres() throws Exception {

		final int anneeCourante = RegDate.get().year();

		// Un contribuable assujetti depuis le début de l'année avec déjà une déclaration
		final PersonnePhysique pp = addNonHabitant("Michelle", "Mabelle", date(1972, 1, 3), Sexe.FEMININ);
		addForPrincipal(pp, date(anneeCourante, 1, 2), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);

		final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(anneeCourante);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode);
		addDeclarationImpot(pp, periode, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions); // pas d'annulation de la déclaration courante
	}

	/**
	 * [UNIREG-3028] Vérifie que l'annulation d'un for fiscale HC activité indépendante et son remplacement par un for ordinaire VD (cas du ctb n°833.153.01)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsAnnulationForActiviteIndependenteHCEtRemplacementForOrdinaireVD() throws Exception {

		final int anneeCourante = RegDate.get().year();
		final int anneeAvant = anneeCourante - 1;
		final int anneeAvantAvant = anneeCourante - 2;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		class Ids {
			long ctb;
			long ffp;
			long ffs;
			long di1;
			long di2;
		}
		final Ids ids = new Ids();

		//
		// Etape 1 : contribuable hors-canton avec une activité indépendente dans le canton
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Jean-Louis", "Ruedi", date(1954, 11, 23), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(anneeAvantAvant, 1, 7), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
				final ForFiscalSecondaire ffs =
						addForSecondaire(pp, date(anneeAvantAvant, 1, 7), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				final PeriodeFiscale periode1 = pfDAO.getPeriodeFiscaleByYear(anneeAvantAvant);
				final ModeleDocument modele1 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode1);
				final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, periode1, date(anneeAvantAvant, 1, 1), date(anneeAvantAvant, 12, 31), TypeContribuable.HORS_CANTON, modele1);
				addEtatDeclarationEmise(di1, date(anneeAvant, 1, 15));
				addEtatDeclarationRetournee(di1, date(anneeAvant, 4, 19));

				final PeriodeFiscale periode2 = pfDAO.getPeriodeFiscaleByYear(anneeAvant);
				final ModeleDocument modele2 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2);
				final DeclarationImpotOrdinaire di2 = addDeclarationImpot(pp, periode2, date(anneeAvant, 1, 1), date(anneeAvant, 12, 31), TypeContribuable.HORS_CANTON, modele2);
				addEtatDeclarationEmise(di2, date(anneeCourante, 1, 15));
				addEtatDeclarationRetournee(di2, date(anneeCourante, 4, 19));

				ids.ctb = pp.getId();
				ids.ffp = ffp.getId();
				ids.ffs = ffs.getId();
				ids.di1 = di1.getId();
				ids.di2 = di2.getId();
				return null;
			}
		});

		// On vérifie qu'aucune tâche n'est générée (le contribuable est dans un état correct) :
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertEmpty(tacheDAO.find(ids.ctb));
				return null;
			}
		});

		//
		// Etape 2 : annulation du for secondaire
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final ForFiscal ffs = hibernateTemplate.get(ForFiscal.class, ids.ffs);
				assertNotNull(ffs);
				ffs.setAnnule(true);
				return null;
			}
		});

		// On vérifie que le contribuable n'est plus assujetti et que des tâches d'annulation des déclarations existantes sont générées
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<Tache> taches = tacheDAO.find(ids.ctb);
				assertEquals(2, taches.size());
				assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.di1, false, (TacheAnnulationDeclarationImpot) taches.get(0));
				assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.di2, false, (TacheAnnulationDeclarationImpot) taches.get(1));
				return null;
			}
		});

		//
		// Etape 3 : annulation du for principal
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final ForFiscal ffp = hibernateTemplate.get(ForFiscal.class, ids.ffp);
				assertNotNull(ffp);
				ffp.setAnnule(true);
				return null;
			}
		});

		// On vérifie que rien ne change au niveau des tâches
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<Tache> taches = tacheDAO.find(ids.ctb);
				assertEquals(2, taches.size());
				assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.di1, false, (TacheAnnulationDeclarationImpot) taches.get(0));
				assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.di2, false, (TacheAnnulationDeclarationImpot) taches.get(1));
				return null;
			}
		});

		//
		// Etape 4 : ajout d'un for principal vaudois ordinaire en remplacement du for hors-canton
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.ctb);
				assertNotNull(pp);
				addForPrincipal(pp, date(anneeAvantAvant, 1, 7), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return null;
			}
		});

		// On vérifie que :
		//  - les tâches d'annulation introduites à l'étape 2 sont annulées (le contribuable est de nouveau assujetti)
		//  - le type de contribuable des déclarations pré-existantes est mis-à-jour (car les types de documents sont compatibles)
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<Tache> taches = tacheDAO.find(ids.ctb);
				assertEquals(2, taches.size());
				assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.di1, true, (TacheAnnulationDeclarationImpot) taches.get(0));
				assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.di2, true, (TacheAnnulationDeclarationImpot) taches.get(1));

				final List<DeclarationImpotOrdinaire> dis = diDAO.findByNumero(ids.ctb);
				assertEquals(2, dis.size());
				Collections.sort(dis, new DateRangeComparator<DeclarationImpotOrdinaire>());
				assertDI(date(anneeAvantAvant, 1, 1), date(anneeAvantAvant, 12, 31), TypeEtatDeclaration.RETOURNEE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						cedi.getNumero(), null, dis.get(0));
				assertDI(date(anneeAvant, 1, 1), date(anneeAvant, 12, 31), TypeEtatDeclaration.RETOURNEE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						cedi.getNumero(), null, dis.get(1));
				return null;
			}
		});
	}

	/**
	 * [UNIREG-3028] Vérifie que les tâches d'annulation sur une déclaration d'impôt est bien annulée lorsqu'un nouveau for est créée sur un contribuable et que la nouvelle période d'imposition est
	 * partielle (cas du contribuable n°100.104.57).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsAnnulationForIndigentEtRemplacementForOrdinaire() throws Exception {

		final int anneeCourante = RegDate.get().year();

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		class Ids {
			long ctb;
			long ffp1;
			long ffp2;
			List<Long> dis = new ArrayList<>();
		}
		final Ids ids = new Ids();

		//
		// Etape 1 : contribuable hors-canton avec une activité indépendente dans le canton
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Bashkim", "Muji", date(1983, 3, 24), Sexe.MASCULIN);

				final ForFiscalPrincipal ffp1 = addForPrincipal(pp, date(2004, 3, 1), MotifFor.INDETERMINE, date(2008, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle);
				ffp1.setModeImposition(ModeImposition.INDIGENT);
				final ForFiscalPrincipal ffp2 = addForPrincipal(pp, date(2009, 1, 1), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle);

				for (int annee = 2004; annee < anneeCourante; ++annee) {
					final PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(annee);
					final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
					final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, periode, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
					addEtatDeclarationEmise(di1, date(annee - 1, 1, 15));
					addEtatDeclarationRetournee(di1, date(annee - 1, 4, 19));
					ids.dis.add(di1.getId());
				}

				ids.ctb = pp.getId();
				ids.ffp1 = ffp1.getId();
				ids.ffp2 = ffp2.getId();
				return null;
			}
		});

		// On vérifie qu'aucune tâche n'est générée (le contribuable est dans un état correct) :
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertEmpty(tacheDAO.find(ids.ctb));
				return null;
			}
		});

		//
		// Etape 2 : annulation du dernier for principal
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final ForFiscal ffp2 = hibernateTemplate.get(ForFiscal.class, ids.ffp2);
				assertNotNull(ffp2);
				tiersService.annuleForFiscal(ffp2);
				return null;
			}
		});

		// On vérifie que le contribuable est toujours assujetti et qu'aucune nouvelle tâche n'a été créée
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertEmpty(tacheDAO.find(ids.ctb));
				return null;
			}
		});

		//
		// Etape 3 : annulation du for principal
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final ForFiscal ffp1 = hibernateTemplate.get(ForFiscal.class, ids.ffp1);
				assertNotNull(ffp1);
				tiersService.annuleForFiscal(ffp1);
				return null;
			}
		});

		// On vérifie que le contribuable n'est plus assujetti et que des tâches d'annulation des déclarations existantes sont générées
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<Tache> taches = tacheDAO.find(ids.ctb);
				assertEquals(anneeCourante - 2004, taches.size());
				for (int i = 0; i < anneeCourante - 2004 - 1; ++i) {
					assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.dis.get(i), false, (TacheAnnulationDeclarationImpot) taches.get(i));
				}
				return null;
			}
		});

		//
		// Etape 4 : ajout d'un for principal vaudois ordinaire en remplacement des fors annulés
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.ctb);
				assertNotNull(pp);
				addForPrincipal(pp, date(2004, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				return null;
			}
		});

		// On vérifie que :
		//  - les tâches d'annulation introduites à l'étape 2 sont annulées (le contribuable est de nouveau assujetti)
		//  - le type de contribuable des déclarations pré-existantes est mis-à-jour (car les types de documents sont compatibles)
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<Tache> taches = tacheDAO.find(ids.ctb);
				assertEquals(anneeCourante - 2004, taches.size());
				for (int i = 0; i < anneeCourante - 2004 - 1; ++i) {
					assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.dis.get(i), true, (TacheAnnulationDeclarationImpot) taches.get(i));
				}

				final List<DeclarationImpotOrdinaire> dis = diDAO.findByNumero(ids.ctb);
				assertEquals(anneeCourante - 2004, dis.size());
				Collections.sort(dis, new DateRangeComparator<DeclarationImpotOrdinaire>());
				assertDI(date(2004, 3, 1), date(2004, 12, 31), TypeEtatDeclaration.RETOURNEE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, cedi.getNumero(), null,
						dis.get(0));

				for (int annee = 2005; annee < anneeCourante - 1; ++annee) {
					assertDI(date(annee, 1, 1), date(annee, 12, 31), TypeEtatDeclaration.RETOURNEE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, cedi.getNumero(), null,
							dis.get(annee - 2004));
				}
				return null;
			}
		});
	}

	@Test
	public void testRecalculTachesAvecDiSansTypeContribuable() throws Exception {

		final long noInd = 333908L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, RegDate.get(1974, 3, 22), "Cuendet", "Adrienne", false);
				addAdresse(ind, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addNationalite(ind, MockPays.Suisse, RegDate.get(1974, 3, 22), null);
			}
		});

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final int anneeDerniere = anneeCourante - 1;

				final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get(anneeDerniere, 1, 1), MotifFor.ARRIVEE_HS, aujourdhui, MotifFor.DEPART_HS, MockCommune.Lausanne);

				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeDerniere);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addDeclarationImpot(pp, pf, RegDate.get(anneeDerniere, 1, 1), RegDate.get(anneeDerniere, 12, 31), null, modele);

				tacheService.genereTacheDepuisFermetureForPrincipal(pp, ffp);
				return pp.getId();
			}
		});

		// test
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				// il s'agit d'un départ HS dans l'année courante, il y aura donc une tâche d'émission de DI
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				criterion.setInclureTachesAnnulees(true);

				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) taches.get(0);
				assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(aujourdhui), date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, tache);
				return null;
			}
		});
	}

	// [SIFISC-1288]
	@Test
	public void testRecalculTachesAvecDiSansTypeContribuableEtDeuxTachesAnnulationEtEnvoi() throws Exception {

		final long noInd = 333908L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, RegDate.get(1974, 3, 22), "Corbaz", "Magali", false);
				addAdresse(ind, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addNationalite(ind, MockPays.Suisse, RegDate.get(1974, 3, 22), null);
			}
		});

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// Crée un contribuable assujettissement sur l'année 2008 comme hors-canton immeuble, avec une déclaration à laquelle il manque le type de contribuable et
		// avec deux tâches : une d'annulation de la DI et une autre d'émission d'une DI de remplacement.
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 1), MotifFor.DEPART_HC, MockCommune.Orbe);
				addForPrincipal(pp, date(2008, 5, 2), MotifFor.DEPART_HC, date(2009, 2, 28), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
				addForPrincipal(pp, date(2009, 3, 1), MotifFor.ARRIVEE_HC, MockCommune.Orbe);
				addForSecondaire(pp, date(2008, 5, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Orbe.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				// déclaration 2005-2007 (rien de spécial)
				for (int annee = 2005; annee < 2008; annee++) {
					final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(annee);
					final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
					addDeclarationImpot(pp, pf, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				// déclaration 2008 (manque le type de contribuable)
				final PeriodeFiscale pf2008 = pfDAO.getPeriodeFiscaleByYear(2008);
				final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2008);
				final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, pf2008, RegDate.get(2008, 1, 1), RegDate.get(2008, 12, 31), null, modele2008);


				// déclaration 2009-année courante (rien de spécial)
				for (int annee = 2009; annee < anneeCourante; annee++) {
					final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(annee);
					final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
					addDeclarationImpot(pp, pf, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				// les deux tâches
				final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.ACI.getNoColAdm());
				assertNotNull(aci);
				addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, date(2008, 7, 1), declaration2008, pp, aci);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 7, 1), date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pp,
						Qualification.MANUEL, 0, aci);

				return pp.getId();
			}
		});

		// après le recalcul des tâches, la déclaration devrait avoir sont type de contribuable renseigné et les deux tâches préexistantes devraient avoir été annulées
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final List<Declaration> declarations = pp.getDeclarationsForPeriode(2008, false);
				assertNotNull(declarations);
				assertEquals(1, declarations.size());

				// le type de contribuable doit être maintenant renseigné
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) declarations.get(0);
				assertEquals(TypeContribuable.HORS_CANTON, di.getTypeContribuable());

				// la tâche d'annulation doit être annulée
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
					criterion.setInclureTachesAnnulees(true);

					final List<Tache> taches = tacheDAO.find(criterion);
					assertNotNull(taches);
					assertEquals(1, taches.size());
					assertTrue(taches.get(0).isAnnule());
				}

				// la tâche d'envoi doit être annulée
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
					criterion.setInclureTachesAnnulees(true);

					final List<Tache> taches = tacheDAO.find(criterion);
					assertNotNull(taches);
					assertEquals(1, taches.size());
					assertTrue(taches.get(0).isAnnule());
				}
				return null;
			}
		});
	}

	@Test
	public void testRecalculTachesApresDepartHSAvecImmeuble() throws Exception {

		final long noInd = 333908L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, RegDate.get(1974, 3, 22), "Cuendet", "Adrienne", false);
				addAdresse(ind, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addNationalite(ind, MockPays.Suisse, RegDate.get(1974, 3, 22), null);
			}
		});

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final int anneeDerniere = anneeCourante - 1;

				// j'essaie de ne pas trop coller à la fin de l'année
				final RegDate dateDepart;
				if (aujourdhui.addDays(20).year() != aujourdhui.year()) {
					dateDepart = aujourdhui.addMonths(-2);
				}
				else {
					dateDepart = aujourdhui.getOneDayBefore();
				}

				addForPrincipal(pp, RegDate.get(anneeDerniere, 1, 1), MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Albanie);
				addForSecondaire(pp, RegDate.get(anneeDerniere, 5, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeDerniere);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addDeclarationImpot(pp, pf, RegDate.get(anneeDerniere, 1, 1), RegDate.get(anneeDerniere, 12, 31), null, modele);

				return pp.getId();
			}
		});

		// test
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				// il s'agit d'un départ HS dans l'année courante avec poursuite d'assujettissement, il ne devrait donc pas y avoir d'émission de DI
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				criterion.setInclureTachesAnnulees(true);

				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());
				return null;
			}
		});
	}

	/**
	 * [UNIREG-2305] Vérifie que le décès d'un contribuable génère bien une tâche d'envoi de DI assignée à l'OID des successions <b>mais</b> que les tâches d'envoi de DIs des années précédentes
	 * (rattrapage) sont assignées à l'OID courant.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSynchronizeActionsForDIsDecesAvecRattrapage() throws Exception {

		final RegDate aujourdhui = RegDate.get();
		final int anneePrecedente = aujourdhui.year() - 1;
		final RegDate dateDeces = date(anneePrecedente, 11, 2);

		// Décès
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Michelle", "Mabelle", date(1972, 1, 3), Sexe.FEMININ);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(anneePrecedente - 2, 6, 12), MotifFor.ARRIVEE_HC, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Cossonay);
				tacheService.genereTacheDepuisFermetureForPrincipal(pp, ffp);
				return null;
			}
		});

		// Il devrait maintenant y avoir :
		//  - deux tâches d'envoi de DIs assignées à l'OID de Cossonay pour les années (anneePrecedente - 2) et (anneePrecedente - 1)
		//  - une tâche d'envoi de DI assignées à l'OID des successions pour les années pour (anneePrecedente) 
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		criterion.setInclureTachesAnnulees(true);

		final List<Tache> taches = tacheDAO.find(criterion);
		assertNotNull(taches);
		assertEquals(3, taches.size());

		final TacheEnvoiDeclarationImpot tache0 = (TacheEnvoiDeclarationImpot) taches.get(0);
		assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(aujourdhui), date(anneePrecedente - 2, 1, 1), date(anneePrecedente - 2, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, tache0);

		final TacheEnvoiDeclarationImpot tache1 = (TacheEnvoiDeclarationImpot) taches.get(1);
		assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(aujourdhui), date(anneePrecedente - 1, 1, 1), date(anneePrecedente - 1, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, tache1);

		final TacheEnvoiDeclarationImpot tache2 = (TacheEnvoiDeclarationImpot) taches.get(2);
		assertTache(TypeEtatTache.EN_INSTANCE, dateDeces.addDays(30), date(anneePrecedente, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				TypeAdresseRetour.ACI, tache2);
	}

	private static void assertAddDI(RegDate debut, RegDate fin, TypeContribuable typeContribuable, SynchronizeAction action) {
		assertNotNull(action);
		assertInstanceOf(AddDI.class, action);

		final AddDI add = (AddDI) action;
		assertEquals(debut, add.periodeImposition.getDateDebut());
		assertEquals(fin, add.periodeImposition.getDateFin());
		assertEquals(typeContribuable, add.periodeImposition.getTypeContribuable());
	}

	private static void assertUpdateDI(DeclarationImpotOrdinaire di, RegDate debut, RegDate fin, TypeContribuable typeContribuable, SynchronizeAction action) {
		assertNotNull(action);
		assertInstanceOf(UpdateDI.class, action);

		final UpdateDI update = (UpdateDI) action;
		assertEquals(di.getId(), update.diId);
		assertEquals(debut, update.periodeImposition.getDateDebut());
		assertEquals(fin, update.periodeImposition.getDateFin());
		assertEquals(typeContribuable, update.periodeImposition.getTypeContribuable());
	}

	private static void assertDeleteDI(DeclarationImpotOrdinaire di, boolean direct, SynchronizeAction action) {
		assertNotNull(action);
		assertInstanceOf(DeleteDI.class, action);

		final DeleteDI delete = (DeleteDI) action;
		assertEquals(di.getId(), delete.diId);
		assertEquals(direct, delete.directAnnulation);
	}

	private static void assertAnnuleTache(Tache tache, SynchronizeAction action) {
		assertNotNull(action);
		assertInstanceOf(AnnuleTache.class, action);

		final AnnuleTache annule = (AnnuleTache) action;
		assertSame(tache, annule.tache);
	}

	private List<Tache> genereChangementImposition(final ModeImposition ancienMode, final ModeImposition nouveauMode) throws Exception {

		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Léo", "Bidule", date(1960, 1, 1), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setModeImposition(ancienMode);

				if (ancienMode != ModeImposition.SOURCE) {
					final int anneeCourante = RegDate.get().year();
					final TypeDocument typeD = (ancienMode == ModeImposition.DEPENSE ? TypeDocument.DECLARATION_IMPOT_DEPENSE : TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
					final TypeContribuable typeC = (ancienMode == ModeImposition.DEPENSE ? TypeContribuable.VAUDOIS_DEPENSE : TypeContribuable.VAUDOIS_ORDINAIRE);
					for (int i = 2003; i < anneeCourante; i++) {
						PeriodeFiscale periode = pfDAO.getPeriodeFiscaleByYear(i);
						ModeleDocument modele = addModeleDocument(typeD, periode);
						addDeclarationImpot(pp, periode, date(i, 1, 1), date(i, 12, 31), typeC, modele);
					}
				}

				final RegDate dateChangement = date(2006, 1, 1);
				final ForFiscalPrincipal nffp = tiersService.changeModeImposition(pp, dateChangement, nouveauMode, MotifFor.CHGT_MODE_IMPOSITION);
				assertNotNull(nffp);
				assertEquals(nouveauMode, nffp.getModeImposition());
				assertEquals(dateChangement, nffp.getDateDebut());

				return pp.getNumero();
			}
		});

		return tacheDAO.find(id);
	}

	private static int countTaches(TypeTache type, List<Tache> taches) {
		int count = 0;
		for (Tache tache : taches) {
			if (tache.getTypeTache() == type) {
				++ count;
			}
		}
		return count;
	}

	/**
	 * Cas du Jira UNIREG-2735
	 */
	@Test
	public void testDepartHorsSuisseDansPeriodeEnCours() throws Exception {

		final long noIndividu = 12345678L;
		final int anneeCourante = RegDate.get().year();
		final RegDate dateArrivee = date(anneeCourante - 1, 5, 12);
		final RegDate dateDepart = date(anneeCourante, 1, 4);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(true) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1971, 4, 12), "Weasley", "Ronald", true);
				addNationalite(individu, MockPays.Suisse, date(1971, 4, 12), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

				final PeriodeFiscale pfPrecedente = pfDAO.getPeriodeFiscaleByYear(anneeCourante - 1);
				final ModeleDocument modelePfPrecedente = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pfPrecedente);
				addDeclarationImpot(pp, pfPrecedente, dateArrivee, date(anneeCourante - 1, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modelePfPrecedente);

				return pp.getNumero();
			}
		});

		// vérification des fors et qu'aucune tâche n'a encore été générée
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final Set<ForFiscal> fors = pp.getForsFiscaux();
				assertNotNull(fors);
				assertEquals(1, fors.size());

				final ForFiscal ff = fors.iterator().next();
				assertNotNull(ff);
				assertEquals(dateArrivee, ff.getDateDebut());
				assertNull(ff.getDateFin());

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);

				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());
				return null;
			}
		});

		// départ HS dans la période courante : une tâche de contrôle de dossier et une tâche d'envoi de DI devraient être générées
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				tiersService.closeForFiscalPrincipal(pp, dateDepart, MotifFor.DEPART_HS);
				tiersService.openForFiscalPrincipal(pp, dateDepart.getOneDayAfter(), MotifRattachement.DOMICILE, MockPays.Albanie.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, ModeImposition.ORDINAIRE, MotifFor.DEPART_HS);
				return null;
			}
		});

		// vérification des fors et des tâches créées
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final List<ForFiscalPrincipal> fors = pp.getForsFiscauxPrincipauxActifsSorted();
				assertNotNull(fors);
				assertEquals(2, fors.size());

				// for vaudois
				{
					final ForFiscalPrincipal ffp = fors.get(0);
					assertNotNull(ffp);
					assertEquals(dateArrivee, ffp.getDateDebut());
					assertEquals(dateDepart, ffp.getDateFin());
					assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				}

				// for étranger
				{
					final ForFiscalPrincipal ffp = fors.get(1);
					assertNotNull(ffp);
					assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
					assertNull(ffp.getDateFin());
					assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				}

				// les tâches, maintenant...
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					final List<Tache> taches = tacheDAO.find(criterion);
					assertNotNull(taches);
					assertEquals(2, taches.size());
				}

				// une tâche de contrôle de dossier
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheControleDossier);
					final List<Tache> taches = tacheDAO.find(criterion);
					assertNotNull(taches);
					assertEquals(1, taches.size());

					final TacheControleDossier tache = (TacheControleDossier) taches.get(0);
					assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(RegDate.get()), tache);
				}

				// et une tâche d'envoi de DI pour la période en cours jusqu'à la date du départ
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
					final List<Tache> taches = tacheDAO.find(criterion);
					assertNotNull(taches);
					assertEquals(1, taches.size());

					final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) taches.get(0);
					assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(RegDate.get()), date(anneeCourante, 1, 1), dateDepart, TypeContribuable.VAUDOIS_ORDINAIRE,
							TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, tache);
				}

				return null;
			}
		});
	}

	/**
	 * Cas du Jira UNIREG-2735
	 */
	@Test
	public void testDecesDansPeriodeEnCours() throws Exception {

		final long noIndividu = 12345678L;
		final int anneeCourante = RegDate.get().year();
		final RegDate dateArrivee = date(anneeCourante - 1, 5, 12);
		final RegDate dateDeces = date(anneeCourante, 1, 2);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(true) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1971, 4, 12), "Weasley", "Ronald", true);
				addNationalite(individu, MockPays.Suisse, date(1971, 4, 12), null);
				individu.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

				final PeriodeFiscale pfPrecedente = pfDAO.getPeriodeFiscaleByYear(anneeCourante - 1);
				final ModeleDocument modelePfPrecedente = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pfPrecedente);
				addDeclarationImpot(pp, pfPrecedente, dateArrivee, date(anneeCourante - 1, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modelePfPrecedente);

				return pp.getNumero();
			}
		});

		// vérification des fors et qu'aucune tâche n'a encore été générée
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final Set<ForFiscal> fors = pp.getForsFiscaux();
				assertNotNull(fors);
				assertEquals(1, fors.size());

				final ForFiscal ff = fors.iterator().next();
				assertNotNull(ff);
				assertEquals(dateArrivee, ff.getDateDebut());
				assertNull(ff.getDateFin());

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);

				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());
				return null;
			}
		});

		// décès dans la période courante : une tâche de transmission de dossier et une tâche d'envoi de DI devraient être générées
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				metierService.deces(pp, dateDeces, null, null);
				return null;
			}
		});

		// vérification des fors et des tâches créées
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final List<ForFiscalPrincipal> fors = pp.getForsFiscauxPrincipauxActifsSorted();
				assertNotNull(fors);
				assertEquals(1, fors.size());

				final ForFiscalPrincipal ffp = fors.get(0);
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertEquals(dateDeces, ffp.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());

				// les tâches, maintenant...
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					final List<Tache> taches = tacheDAO.find(criterion);
					assertNotNull(taches);
					assertEquals(2, taches.size());
				}

				// une tâche de transmission de dossier
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheTransmissionDossier);
					final List<Tache> taches = tacheDAO.find(criterion);
					assertNotNull(taches);
					assertEquals(1, taches.size());

					final TacheTransmissionDossier tache = (TacheTransmissionDossier) taches.get(0);
					assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(RegDate.get()), tache);
				}

				// et une tâche d'envoi de DI pour la période en cours jusqu'à la date du décès
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(pp);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
					final List<Tache> taches = tacheDAO.find(criterion);
					assertNotNull(taches);
					assertEquals(1, taches.size());

					final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) taches.get(0);
					assertTache(TypeEtatTache.EN_INSTANCE, dateDeces.addDays(30), date(anneeCourante, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
								TypeAdresseRetour.ACI, tache);
				}

				return null;
			}
		});
	}

	/**
	 * [UNIREG-3223] Vérifie que la fermeture d'un for principal pour cause de décès sur un sourcier pur parti hors-canton ne génère pas de tâche de transmission de dossier
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDecesSourcierPur() throws Exception {

		final PersonnePhysique marcel = addNonHabitant("Marcel", "Longuesmanches", date(1923, 4, 22), Sexe.MASCULIN);
		final ForFiscalPrincipal ffp = addForPrincipal(marcel, date(1956, 6, 17), MotifFor.ARRIVEE_HS, date(1977, 4, 12), MotifFor.DEPART_HC, MockCommune.Aigle);
		ffp.setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(marcel, date(1977, 4, 13), MotifFor.DEPART_HC, MockCommune.Neuchatel);

		tiersService.closeForFiscalPrincipal(marcel, date(2000, 3, 22), MotifFor.VEUVAGE_DECES);
		assertEmpty(tacheDAO.getAll());
	}

	@Test
	public void testAnnulationDeclarationDansPeriodeEnCours() throws Exception {

		// par exemple sur une annulation de décès :
		// - le décès (cette année) a créé une DI pour l'année en cours
		// - le décès est annulé -> le for est ré-ouvert

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Severus", "Snape", date(1945, 8, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(anneeCourante, 1, 1), MotifFor.ARRIVEE_HS, aujourdhui, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne);
				pp.setDateDeces(aujourdhui);

				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addDeclarationImpot(pp, pf, date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				return pp.getNumero();
			}
		});

		// vérification qu'aucune tâche n'a encore été générée (= tout est à jour)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());
				return null;
			}
		});

		// maintenant on ré-ouvre le for (et non, il n'est pas mort !)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(aujourdhui, ffp.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());

				// ré-ouverture du for
				ffp.setDateFin(null);
				ffp.setMotifFermeture(null);
				pp.setDateDeces(null);
				return null;
			}
		});

		// il devrait maintenant y avoir une tâche d'annulation de DI
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertEquals(TypeTache.TacheAnnulationDeclarationImpot, tache.getTypeTache());
				assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(aujourdhui), date(anneeCourante, 1, 1), aujourdhui, (TacheAnnulationDeclarationImpot) tache);
				return null;
			}
		});
	}

	@Test
	public void testAnnulationTacheEnvoiDeclarationDansPeriodeEnCours() throws Exception {

		// par exemple sur une annulation de décès :
		// - le décès (cette année) a créé une tâche d'envoi de DI pour l'année en cours
		// - le décès est annulé -> le for est ré-ouvert

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Severus", "Snape", date(1945, 8, 12), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(anneeCourante, 1, 1), MotifFor.ARRIVEE_HS, aujourdhui, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne);
				pp.setDateDeces(aujourdhui);

				tacheService.genereTacheDepuisFermetureForPrincipal(pp, ffp);
				return pp.getNumero();
			}
		});

		// vérification qu'une seule tâche d'envoi de DI existe (il y a aussi une tâche de transmission de dossier, mais ce n'est pas l'objet de ce test)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheTransmissionDossier); // toutes les tâches qui ne sont pas des transmissions de dossier
				criterion.setInvertTypeTache(true);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertTache(TypeEtatTache.EN_INSTANCE, aujourdhui.addDays(30), date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.ACI, (TacheEnvoiDeclarationImpot) tache);
				return null;
			}
		});

		// maintenant on ré-ouvre le for (et non, il n'est pas mort !)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(aujourdhui, ffp.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());

				// ré-ouverture du for
				ffp.setDateFin(null);
				ffp.setMotifFermeture(null);
				pp.setDateDeces(null);
				return null;
			}
		});

		// la tâche précédemment existante devrait avoir été annulée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheTransmissionDossier);     // toutes les tâches qui ne sont pas des transmissions de dossier
				criterion.setInvertTypeTache(true);
				criterion.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertTrue(tache.isAnnule());
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertTache(TypeEtatTache.EN_INSTANCE, aujourdhui.addDays(30), date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.ACI, (TacheEnvoiDeclarationImpot) tache);
				return null;
			}
		});
	}

	/**
	 * Encore un cas du UNIREG-2735
	 */
	@Test
	public void testTacheAnnulationDIQuittanceeSurAnnulationDepartHS() throws Exception {

		// exemple:
		// - départ dans la période courante (= hier)
		// - une DI est émise (et quittancée) pour le début de l'année passée sur le sol helvétique
		// - en fait, le départ est annulé
		// - on devrait donc avoir une tâche d'annulation de DI

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Severus", "Snape", date(1945, 8, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(anneeCourante, 1, 1), MotifFor.ARRIVEE_HS, aujourdhui.getOneDayBefore(), MotifFor.DEPART_HS, MockCommune.Bussigny);
				addForPrincipal(pp, aujourdhui, MotifFor.DEPART_HS, MockPays.EtatsUnis);

				// le contribuable avait déclaré son départ, mais la date n'était pas la date du véritable départ
				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(anneeCourante, 1, 1), date(anneeCourante, 1, 2), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				di.addEtat(new EtatDeclarationEmise(aujourdhui));
				di.addEtat(new EtatDeclarationRetournee(aujourdhui, "TEST"));
				di.setLibre(true);

				return pp.getNumero();
			}
		});

		// on vérifie que la DI est bien là quittancée, et qu'aucune tâche n'existe encore sur ce contribuable
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final List<Declaration> dis = pp.getDeclarationsForPeriode(anneeCourante, false);
				assertNotNull(dis);
				assertEquals(1, dis.size());

				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) dis.get(0);
				assertNotNull(di);
				assertFalse(di.isAnnule());
				assertFalse(di.isLibre());      // la DI a été adaptée au for
				assertEquals(date(anneeCourante, 1, 1), di.getDateDebut());
				assertEquals(aujourdhui.getOneDayBefore(), di.getDateFin());
				assertEquals(TypeEtatDeclaration.RETOURNEE, di.getDernierEtat().getEtat());

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());

				return null;
			}
		});

		// maintenant on va annuler le for HS (et donc ré-ouvrir le for vaudois)
		// -> une tâche d'annulation de DI devrait être générée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal forHS = pp.getDernierForFiscalPrincipal();
				assertNotNull(forHS);
				assertEquals(aujourdhui, forHS.getDateDebut());
				assertNull(forHS.getDateFin());
				assertEquals(TypeAutoriteFiscale.PAYS_HS, forHS.getTypeAutoriteFiscale());

				tiersService.annuleForFiscal(forHS);
				return null;
			}
		});

		// vérification de l'existence de catte tâche d'annulation de DI
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal forVD = pp.getDernierForFiscalPrincipal();
				assertNotNull(forVD);
				assertEquals(date(anneeCourante, 1, 1), forVD.getDateDebut());
				assertNull(forVD.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forVD.getTypeAutoriteFiscale());
				assertEquals(MockCommune.Bussigny.getNoOFS(), (int) forVD.getNumeroOfsAutoriteFiscale());

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertInstanceOf(TacheAnnulationDeclarationImpot.class, tache);
				assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				assertFalse(tache.isAnnule());

				final TacheAnnulationDeclarationImpot annulation = (TacheAnnulationDeclarationImpot) tache;
				final DeclarationImpotOrdinaire diAAnnuler = annulation.getDeclarationImpotOrdinaire();
				assertNotNull(diAAnnuler);
				assertFalse(diAAnnuler.isAnnule());
				assertFalse(diAAnnuler.isLibre());      // la DI a été adaptée au for
				assertEquals(date(anneeCourante, 1, 1), diAAnnuler.getDateDebut());
				assertEquals(aujourdhui.getOneDayBefore(), diAAnnuler.getDateFin());
				assertEquals(TypeEtatDeclaration.RETOURNEE, diAAnnuler.getDernierEtat().getEtat());

				return null;
			}
		});
	}

	@Test
	public void testModificationDateFinAssujettissementDansPeriodeCourante() throws Exception {

		// par exemple :
		// - décès dans la période courante (= aujourdhui)
		// - une tâche d'émission de DI a été générée
		// - mais en fait, le décès a eu lieu un autre jour (toujours dans la période courante !)

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Severus", "Snape", date(1945, 8, 12), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(anneeCourante, 1, 1), MotifFor.ARRIVEE_HS, aujourdhui, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne);
				pp.setDateDeces(aujourdhui);

				tacheService.genereTacheDepuisFermetureForPrincipal(pp, ffp);
				return pp.getNumero();
			}
		});

		// vérification qu'une seule tâche d'envoi de DI existe (il y a aussi une tâche de transmission de dossier, mais ce n'est pas l'objet de ce test)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheTransmissionDossier); // toutes les tâches qui ne sont pas des transmissions de dossier
				criterion.setInvertTypeTache(true);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
				assertTache(TypeEtatTache.EN_INSTANCE, aujourdhui.addDays(30), date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.ACI, (TacheEnvoiDeclarationImpot) tache);
				return null;
			}
		});

		final RegDate nouvelleDateDeces;
		if (aujourdhui.addDays(-7).year() == anneeCourante) {
			nouvelleDateDeces = aujourdhui.addDays(-7);
		}
		else {
			nouvelleDateDeces = aujourdhui.addDays(4);
		}

		// maintenant on change la date de fermeture du for, tout en restant dans la même période fiscale
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(aujourdhui, ffp.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());

				// changement de la date de décès (c'est super-GRA, ça, non ?)
				ffp.setDateFin(nouvelleDateDeces);
				pp.setDateDeces(nouvelleDateDeces);
				return null;
			}
		});

		// la tâche précédemment existante devrait avoir été annulée et une nouvelle générée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheTransmissionDossier);     // toutes les tâches qui ne sont pas des transmissions de dossier
				criterion.setInvertTypeTache(true);
				criterion.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(2, taches.size());

				boolean trouveeAnnulee = false;
				boolean trouveeNonAnnulee = false;
				for (Tache tache : taches) {
					assertNotNull(tache);
					assertEquals(TypeTache.TacheEnvoiDeclarationImpot, tache.getTypeTache());
					if (tache.isAnnule()) {
						assertFalse("Deuxième tâche annulée trouvée", trouveeAnnulee);
						trouveeAnnulee = true;

						assertTache(TypeEtatTache.EN_INSTANCE, aujourdhui.addDays(30), date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE,
								TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.ACI, (TacheEnvoiDeclarationImpot) tache);
					}
					else {
						assertFalse("Deuxième tâche non-annulée trouvée", trouveeNonAnnulee);
						trouveeNonAnnulee = true;

						assertTache(TypeEtatTache.EN_INSTANCE, nouvelleDateDeces.addDays(30), date(anneeCourante, 1, 1), nouvelleDateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
								TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.ACI, (TacheEnvoiDeclarationImpot) tache);
					}
				}
				assertTrue(trouveeAnnulee);
				assertTrue(trouveeNonAnnulee);
				return null;
			}
		});
	}

	@Test
	public void testModificationDateFinAssujettissementDansPeriodeCouranteAvecDI() throws Exception {

		// par exemple :
		// - décès dans la période courante (= aujourdhui)
		// - une tâche d'émission de DI a été générée, et même une DI, en fait
		// - mais en fait, le décès a eu lieu un autre jour (toujours dans la période courante !)

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Severus", "Snape", date(1945, 8, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(anneeCourante, 1, 1), MotifFor.ARRIVEE_HS, aujourdhui, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne);
				pp.setDateDeces(aujourdhui);

				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addDeclarationImpot(pp, pf, date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				return pp.getNumero();
			}
		});

		// vérification qu'aucune tâche n'a encore été générée (= tout est à jour)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());
				return null;
			}
		});

		final RegDate nouvelleDateDeces;
		if (aujourdhui.addDays(-7).year() == anneeCourante) {
			nouvelleDateDeces = aujourdhui.addDays(-7);
		}
		else {
			nouvelleDateDeces = aujourdhui.addDays(4);
		}

		// maintenant on change la date de fermeture du for, tout en restant dans la même période fiscale
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(aujourdhui, ffp.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());

				// changement de la date de décès (c'est super-GRA, ça, non ?)
				ffp.setDateFin(nouvelleDateDeces);
				pp.setDateDeces(nouvelleDateDeces);
				return null;
			}
		});

		// il ne devrait maintenant y avoir aucune nouvelle tâche, mais la période de la DI devrait avoir été adaptée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());

				final Declaration di = pp.getDeclarationActive(date(anneeCourante, 1, 1));
				assertNotNull(di);
				assertEquals(date(anneeCourante, 1, 1), di.getDateDebut());
				assertEquals(nouvelleDateDeces, di.getDateFin());
				return null;
			}
		});
	}

	@Test
	public void testHorsSuisseVenteDernierImmeubleDansPeriodeCourante() throws Exception {

		// cas du contribuable HS qui possède un immeuble et qui le vend dans la période courante
		// ->   il y a une fin d'assujettissement en cours d'année, une tâche d'émission de DI
		//      doit être générée immédiatement

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();
		final RegDate dateAchat = date(anneeCourante - 1, 5, 6);

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Minerva", "McGonagall", date(1970, 8, 12), Sexe.FEMININ);
				addForPrincipal(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, aujourdhui, MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// il doit y avoir une tâche d'émission de DI en raison de la vente du dernier immeuble et donc de la fin d'assujettissement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TacheEnvoiDeclarationImpot.class, tache.getClass());

				final TacheEnvoiDeclarationImpot tacheEnvoi = (TacheEnvoiDeclarationImpot) tache;
				assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(aujourdhui), date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, tacheEnvoi);
				return null;
			}
		});
	}

	@Test
	public void testHorsSuisseFinActiviteIndependanteDansPeriodeCourante() throws Exception {

		// cas du contribuable HS qui a une activité indépendante et qui l'arrête dans la période courante
		// ->   il y a une fin d'assujettissement en cours d'année, une tâche d'émission de DI
		//      doit être générée immédiatement

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();
		final RegDate dateDebutActivite = date(anneeCourante - 1, 5, 6);

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Minerva", "McGonagall", date(1970, 8, 12), Sexe.FEMININ);
				addForPrincipal(pp, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, MockPays.RoyaumeUni);
				addForSecondaire(pp, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, aujourdhui, MotifFor.FIN_EXPLOITATION, MockCommune.Bussigny.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				// on crée déjà la DI de l'an dernier
				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante - 1);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addDeclarationImpot(pp, pf, dateDebutActivite, date(anneeCourante - 1, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				return pp.getNumero();
			}
		});

		// il ne devrait pas y avoir de tâche d'émission de DI malgré la fin d'activité indépendante et donc la fin d'assujettissement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertEquals(TacheEnvoiDeclarationImpot.class, tache.getClass());

				final TacheEnvoiDeclarationImpot tacheEnvoi = (TacheEnvoiDeclarationImpot) tache;
				assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(aujourdhui), date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, tacheEnvoi);
				return null;
			}
		});
	}

	@Test
	public void testHorsCantonVenteDernierImmeubleDansPeriodeCourante() throws Exception {

		// cas du contribuable HC qui possède un immeuble et qui le vend dans la période courante
		// ->   bien qu'il y ait une fin d'assujettissement en cours d'année, aucune tâche d'émission de DI
		//      ne doit être générée immédiatement (elle est remplacée par une note)

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();
		final RegDate dateAchat = date(anneeCourante - 1, 5, 6);

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Minerva", "McGonagall", date(1970, 8, 12), Sexe.FEMININ);
				addForPrincipal(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, aujourdhui, MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				// on crée déjà la DI de l'an dernier
				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante - 1);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				addDeclarationImpot(pp, pf, dateAchat, date(anneeCourante - 1, 12, 31), TypeContribuable.HORS_CANTON, modele);

				return pp.getNumero();
			}
		});

		// il ne devrait pas y avoir de tâche d'émission de DI malgré la vente du dernier immeuble (DI remplacée par une note)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-1653] cas du contribuable VD qui possède un immeuble, qui part HC puis vend son immeuble dans la même période fiscale et pour qui une tâche d'envoi de déclaration d'impôt a été créée ->
	 * bien qu'il y ait une fin d'assujettissement en cours d'année, la tâche d'émission de DI doit être annulée car la déclaration d'impôt correspondante est remplacée par une note.
	 */
	@Test
	public void testHorsCantonVenteDernierImmeubleDansMemePeriodeAvecTacheEnvoiErronee() throws Exception {

		final RegDate dateArrivee = date(2007, 11, 9);
		final RegDate dateDepartHC = date(2009, 4, 1);
		final RegDate dateVente = date(2009, 11, 9);

		class Ids {
			long pp;
			long tache;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Minerva", "McGonagall", date(1970, 8, 12), Sexe.FEMININ);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HC, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne);
				addForPrincipal(pp, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Bern);
				addForSecondaire(pp, dateArrivee, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				// on crée déjà les DIs 2007 et 2008
				final PeriodeFiscale pf2007 = pfDAO.getPeriodeFiscaleByYear(2007);
				addDeclarationImpot(pp, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2007));
				final PeriodeFiscale pf2008 = pfDAO.getPeriodeFiscaleByYear(2008);
				addDeclarationImpot(pp, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2008));

				final CollectiviteAdministrative oi = tiersService.getCollectiviteAdministrative(15, true);
				final TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2011, 6, 19), date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON,
						TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pp, Qualification.COMPLEXE_1, 1, oi);

				ids.pp = pp.getNumero();
				ids.tache = tache.getId();
				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.pp);
				assertNotNull(pp);

				// la tâche d'émission de DI en 2009 devrait être annulée
				final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) tacheDAO.get(ids.tache);
				assertNotNull(tache);
				assertTrue(tache.isAnnule());

				// il ne devrait pas y avoir de tâche d'émission de DI en 2009 malgré la vente du dernier immeuble (car la DI est remplacée par une note)
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setInclureTachesAnnulees(false);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());

				return null;
			}
		});
	}

	@Test
	public void testHorsCantonFinActiviteIndependanteDansPeriodeCourante() throws Exception {

		// cas du contribuable HC qui a une activité indépendante et qui l'arrête dans la période courante
		// ->   bien qu'il y ait une fin d'assujettissement en cours d'année, aucune tâche d'émission de DI
		//      ne doit être générée immédiatement (on attendra le batch de début d'année pour ça)

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();
		final RegDate dateDebutActivite = date(anneeCourante - 1, 5, 6);

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Minerva", "McGonagall", date(1970, 8, 12), Sexe.FEMININ);
				addForPrincipal(pp, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bern);
				addForSecondaire(pp, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, aujourdhui, MotifFor.FIN_EXPLOITATION, MockCommune.Bussigny.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				// on crée déjà la DI de l'an dernier
				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante - 1);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addDeclarationImpot(pp, pf, date(anneeCourante - 1, 1, 1), date(anneeCourante - 1, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				return pp.getNumero();
			}
		});

		// il ne devrait pas y avoir de tâche d'émission de DI malgré la fin d'activité indépendante et donc la fin d'assujettissement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());
				return null;
			}
		});
	}

	@Test
	public void testDiLibre() throws Exception {

		// contribuable assujetti depuis le début de l'année qui se fait faire une DI libre en annonçant son départ

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Viktor", "Krum", date(1980, 10, 25), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(anneeCourante, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Renens);

				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				di.setLibre(true);

				return pp.getId();
			}
		});

		// la DI doit avoir été acceptée, non annulée et il ne doit y avoir aucune tâche d'annulation de DI en instance
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) pp.getDeclarationActive(aujourdhui);
				assertNotNull(di);
				assertFalse(di.isAnnule());
				assertEquals(RegDate.get(anneeCourante, 1, 1), di.getDateDebut());
				assertEquals(aujourdhui, di.getDateFin());

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(0, taches.size());

				return null;
			}
		});
	}

	@Test
	public void testNouvelleFinAssujettissementAvecDICouranteDejaEmise() throws Exception {

		// cas d'un contribuable dont on apprend aujourd'hui seulement le départ HS
		// l'année dernière, alors que sa DI de la période courante a déjà été émise

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		// mise en place
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Viktor", "Krum", date(1980, 10, 25), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(anneeCourante - 2, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

				// on crée déjà la DI de l'an dernier et de l'année d'avant
				{
					final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante - 2);
					final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
					addDeclarationImpot(pp, pf, date(anneeCourante - 2, 1, 1), date(anneeCourante - 2, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}
				{
					final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante - 1);
					final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
					addDeclarationImpot(pp, pf, date(anneeCourante - 1, 1, 1), date(anneeCourante - 1, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				// et aussi celle de cette année (DI libre)
				{
					final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(anneeCourante);
					final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
					final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(anneeCourante, 1, 1), aujourdhui, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
					di.setLibre(true);
				}

				return pp.getNumero();
			}
		});

		// ensuite, on ferme le for l'année dernière, les deux DI devaient disparaître
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNull(ffp.getDateFin());
				assertNull(ffp.getMotifFermeture());

				ffp.setDateFin(RegDate.get(anneeCourante - 1, 7, 12));
				ffp.setMotifFermeture(MotifFor.DEPART_HC);
				return null;
			}
		});

		// plus de di !
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final List<Declaration> di = pp.getDeclarationsForPeriode(anneeCourante, false);
				assertNotNull(di);
				assertEquals(1, di.size());
				assertFalse(di.get(0).isAnnule());      // pas annulée, mais une tâche d'annulation doit être présente

				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(pp);
				criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
				criterion.setAnnee(anneeCourante);
				final List<Tache> taches = tacheDAO.find(criterion);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final TacheAnnulationDeclarationImpot tache = (TacheAnnulationDeclarationImpot) taches.get(0);
				assertNotNull(tache);
				assertEquals(di.get(0).getId(), tache.getDeclarationImpotOrdinaire().getId());

				return null;
			}
		});
	}

	/**
	 * [UNIREG-3285] Vérifie les différents cas de figure pour obtenir l'office d'impôt d'un contribuable.
	 */
	@Rollback
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetOfficeImpot() throws Exception {

		final TacheServiceImpl service = new TacheServiceImpl();
		service.setTiersService(tiersService);

		// Contribuable sans aucun for fiscal
		try {
			final PersonnePhysique pp = new PersonnePhysique(false);
			pp.setNumero(22333444L);
			service.getOfficeImpot(pp);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Impossible de trouver un for fiscal convenable pour la détermination de l'OID du contribuable n°22333444", e.getMessage());
		}

		// Contribuable avec un for fiscal vaudois ordinaire
		{
			final PersonnePhysique pp = addNonHabitant("Paul", "Effe", date(1948,1,1), Sexe.MASCULIN);
			addForPrincipal(pp, date(1968,1,1), MotifFor.MAJORITE, MockCommune.Lausanne);
			final CollectiviteAdministrative officeImpot = service.getOfficeImpot(pp);
			assertNotNull(officeImpot);
			assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), officeImpot.getNumeroCollectiviteAdministrative().intValue());
		}

		// Contribuable avec un for fiscal vaudois ordinaire annulé-> le for principal (même annulé) est pris en compte à défaut d'autres fors
		{
			final PersonnePhysique pp = addNonHabitant("Paul", "Effe", date(1948,1,1), Sexe.MASCULIN);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1968, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
			ffp.setAnnule(true);
			final CollectiviteAdministrative officeImpot = service.getOfficeImpot(pp);
			assertNotNull(officeImpot);
			assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), officeImpot.getNumeroCollectiviteAdministrative().intValue());
		}

		// Contribuable avec un for fiscal vaudois source -> le for principal (même source) est pris en compte à défaut d'autres fors
		{
			final PersonnePhysique pp = addNonHabitant("Paul", "Effe", date(1948,1,1), Sexe.MASCULIN);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1968,1,1), MotifFor.MAJORITE, MockCommune.Lausanne);
			ffp.setModeImposition(ModeImposition.SOURCE);
			final CollectiviteAdministrative officeImpot = service.getOfficeImpot(pp);
			assertNotNull(officeImpot);
			assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), officeImpot.getNumeroCollectiviteAdministrative().intValue());
		}

		// Contribuable avec un for fiscal vaudois ordinaire annulé -> le for principal source (même annulé) est pris en compte à défaut d'autres fors
		{
			final PersonnePhysique pp = addNonHabitant("Paul", "Effe", date(1948,1,1), Sexe.MASCULIN);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1968, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
			ffp.setModeImposition(ModeImposition.SOURCE);
			ffp.setAnnule(true);
			final CollectiviteAdministrative officeImpot = service.getOfficeImpot(pp);
			assertNotNull(officeImpot);
			assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), officeImpot.getNumeroCollectiviteAdministrative().intValue());
		}

		// Contribuable avec un for fiscal vaudois source et un autre ordinaire mais annulé -> le principal ordinaire (même annulé) primer sur le for principal source
		{
			final PersonnePhysique pp = addNonHabitant("Paul", "Effe", date(1948,1,1), Sexe.MASCULIN);
			final ForFiscalPrincipal ffp0 = addForPrincipal(pp, date(1968,1,1), MotifFor.MAJORITE, MockCommune.Lausanne);
			ffp0.setAnnule(true);
			final ForFiscalPrincipal ffp1 = addForPrincipal(pp, date(1968,1,1), MotifFor.MAJORITE, MockCommune.Morges);
			ffp1.setModeImposition(ModeImposition.SOURCE);
			final CollectiviteAdministrative officeImpot = service.getOfficeImpot(pp);
			assertNotNull(officeImpot);
			assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), officeImpot.getNumeroCollectiviteAdministrative().intValue());
		}

		// Contribuable avec un for fiscal vaudois principal source et un for secondaire annulé -> le for secondaire (même annulé) prime sur le for principal source
		{
			final PersonnePhysique pp = addNonHabitant("Paul", "Effe", date(1948, 1, 1), Sexe.MASCULIN);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1968, 1, 1), MotifFor.MAJORITE, MockCommune.Morges);
			ffp.setModeImposition(ModeImposition.SOURCE);
			final ForFiscalSecondaire ffs = addForSecondaire(pp, date(1990, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
			ffs.setAnnule(true);
			final CollectiviteAdministrative officeImpot = service.getOfficeImpot(pp);
			assertNotNull(officeImpot);
			assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), officeImpot.getNumeroCollectiviteAdministrative().intValue());
		}
	}

	/**
	 * [UNIREG-820] [SIFISC-8197]
	 */
	@Test
	public void testTypeDeclarationImpotDansTacheEnvoi() throws Exception {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne... où sont-ils tous partis ? sommes-nous passés dans la quatrième dimension ?
			}
		});

		// mise en place fiscale
		final RegDate dateDebutActivite = date(RegDate.get().year() - 2, 6, 15);
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Galinette", "Sandré", null, Sexe.FEMININ);
				addForPrincipal(pp, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, MockPays.Allemagne);
				addForSecondaire(pp, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
				return pp.getNumero();
			}
		});

		// vérification des tâches d'envoi
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criteria = new TacheCriteria();
				criteria.setContribuable(pp);
				criteria.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				final List<Tache> taches = tacheDAO.find(criteria);
				assertNotNull(taches);
				assertEquals(2, taches.size());

				for (Tache tache : taches) {
					assertNotNull(tache);

					final TacheEnvoiDeclarationImpot tacheDi = (TacheEnvoiDeclarationImpot) tache;
					assertEquals(DateRangeHelper.toDisplayString(tacheDi), TypeContribuable.HORS_SUISSE, tacheDi.getTypeContribuable());
					assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					assertFalse(tache.isAnnule());

					// [UNIREG-820] dit ceci:
					// la première année, pas de DI avant, ni assujettissement -> VAUDTAX
					// la seconde année, toujours pas de DI avant, mais assujettissement l'année précédente -> COMPLETE
					final TypeDocument typeDocumentAttendu;
					if (tacheDi.getDateDebut().year() == dateDebutActivite.year()) {
						typeDocumentAttendu = TypeDocument.DECLARATION_IMPOT_VAUDTAX;
					}
					else {
						typeDocumentAttendu = TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH;
					}
					assertEquals(DateRangeHelper.toDisplayString(tacheDi), typeDocumentAttendu, tacheDi.getTypeDocument());
				}
				return null;
			}
		});

		// maintenant, on rajoute la DI de la première année
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);
				final PeriodeFiscale pf = pfDAO.getPeriodeFiscaleByYear(dateDebutActivite.year());
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addDeclarationImpot(pp, pf, dateDebutActivite, date(dateDebutActivite.year(), 12, 31), TypeContribuable.HORS_SUISSE, md);
				return null;
			}
		});

		// et on vérifie les tâches résultantes (il ne devrait en rester qu'une en instance, dont le type de document a été recalculé en fonction de la nouvelle DI créée)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				assertNotNull(pp);

				final TacheCriteria criteria = new TacheCriteria();
				criteria.setContribuable(pp);
				criteria.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
				criteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
				criteria.setInclureTachesAnnulees(false);
				final List<Tache> taches = tacheDAO.find(criteria);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final TacheEnvoiDeclarationImpot tacheDi = (TacheEnvoiDeclarationImpot) taches.get(0);
				assertEquals(date(dateDebutActivite.year() + 1, 1, 1), tacheDi.getDateDebut());
				assertEquals(date(dateDebutActivite.year() + 1, 12, 31), tacheDi.getDateFin());
				assertEquals(TypeContribuable.HORS_SUISSE, tacheDi.getTypeContribuable());
				assertEquals(TypeDocument.DECLARATION_IMPOT_VAUDTAX, tacheDi.getTypeDocument());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-11939] Tâche de contrôle de dossier générée au départ HC dans la période fiscale courante
	 */
	@Test
	public void testControleDossierPourDepartHorsCantonPeriodeFiscaleCourante() throws Exception {

		final long noIndividu = 565798L;
		final RegDate dateDepart = RegDate.get();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Pasquier", "Baudouin", Sexe.MASCULIN);
				final MockAdresse adresseVd = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2000, 4, 1), dateDepart);
				adresseVd.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 4, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		// fermeture du for pour le départ
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				ffp.setDateFin(dateDepart);
				ffp.setMotifFermeture(MotifFor.DEPART_HC);
				tacheService.genereTacheDepuisFermetureForPrincipal(pp, ffp);
			}
		});

		// récupération de la tâche de contrôle de dossier (il doit y en avoir une et une seule)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final TacheCriteria criteria = new TacheCriteria();
				criteria.setNumeroCTB(ppId);
				verifieControleDossier(criteria, 1);
			}
		});
	}

	/**
	 * [SIFISC-11939] Tâche de contrôle de dossier générée au départ HC dans la période fiscale antérieure à la période fiscale courante
	 */
	@Test
	public void testControleDossierPourDepartHorsCantonPeriodeFiscaleJusteAvantCourante() throws Exception {

		final long noIndividu = 565798L;
		final RegDate dateDepart = RegDate.get().addYears(-1);          // l'année dernière

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Pasquier", "Baudouin", Sexe.MASCULIN);
				final MockAdresse adresseVd = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2000, 4, 1), dateDepart);
				adresseVd.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 4, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		// fermeture du for pour le départ
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				ffp.setDateFin(dateDepart);
				ffp.setMotifFermeture(MotifFor.DEPART_HC);
				tacheService.genereTacheDepuisFermetureForPrincipal(pp, ffp);
			}
		});

		// récupération de la tâche de contrôle de dossier (il doit y en avoir une et une seule)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final TacheCriteria criteria = new TacheCriteria();
				criteria.setNumeroCTB(ppId);
				verifieControleDossier(criteria, 1);
			}
		});
	}

	/**
	 * [SIFISC-11939] Aucune tâche de contrôle de dossier générée au départ HC dans la période fiscale deux ans avant la période courante
	 */
	@Test
	public void testControleDossierPourDepartHorsCantonPeriodeFiscaleDeuxAnsAvantCourante() throws Exception {

		final long noIndividu = 565798L;
		final RegDate dateDepart = RegDate.get().addYears(-2);          // il y a deux ans

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Pasquier", "Baudouin", Sexe.MASCULIN);
				final MockAdresse adresseVd = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2000, 4, 1), dateDepart);
				adresseVd.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 4, 1), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		// fermeture du for pour le départ
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				ffp.setDateFin(dateDepart);
				ffp.setMotifFermeture(MotifFor.DEPART_HC);
				tacheService.genereTacheDepuisFermetureForPrincipal(pp, ffp);
			}
		});

		// récupération de la tâche de contrôle de dossier (il ne doit pas y en avoir)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final TacheCriteria criteria = new TacheCriteria();
				criteria.setNumeroCTB(ppId);
				verifieControleDossier(criteria, 0);
			}
		});
	}
}

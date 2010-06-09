package ch.vd.uniregctb.tache;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.Assert;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.tache.sync.AddDI;
import ch.vd.uniregctb.tache.sync.AnnuleTache;
import ch.vd.uniregctb.tache.sync.DeleteDI;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tache.sync.UpdateDI;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
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
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
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

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		tacheService = getBean(TacheService.class, "tacheService");
		metierService = getBean(MetierService.class, "metierService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				Individu individu1 = addIndividu(333908, RegDate.get(1974, 3, 22), "Cuendet", "Adrienne", true);
				addAdresse(individu1, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1980, 1, 1), null);

				Individu individu2 = addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu2, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1980, 1, 1), null);

			}
		});

		doInNewTransactionAndSession(new TxCallback(){
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				for (MockCollectiviteAdministrative ca : MockCollectiviteAdministrative.getAll()) {
					addCollAdm(ca);
				}
				return null;
			}
		});
	}

	@Test
	public void testGenereArriveeHSDepuisOuvertureForPrincipal() {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(11111111L);
		hab.setNumeroIndividu(333908L);
		hab = (PersonnePhysique) hibernateTemplate.merge(hab);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), null, 5652,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		hab.addForFiscal(forFiscalPrincipal);
		forFiscalPrincipal.setMotifOuverture(MotifFor.ARRIVEE_HS);
		tacheService.genereTacheDepuisOuvertureForPrincipal(hab, forFiscalPrincipal, null);

		TacheCriteria criterion = new TacheCriteria();
		verifieTacheNouveauDossier(criterion, 1);

		assertTachesEnvoi(criterion, false);
	}

	@Test
	public void testGenereArriveeHSDepuisOuvertureForPrincipalSourceUNIREG1888() {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(11111111L);
		hab.setNumeroIndividu(333908L);
		hab = (PersonnePhysique) hibernateTemplate.merge(hab);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), null, 5652,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
		hab.addForFiscal(forFiscalPrincipal);
		forFiscalPrincipal.setMotifOuverture(MotifFor.ARRIVEE_HS);
		tacheService.genereTacheDepuisOuvertureForPrincipal(hab, forFiscalPrincipal, null);
		assertEmpty(tacheDAO.getAll());
	}

	@Test
	public void testGenereArriveeHCDepuisOuvertureForPrincipal() {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(11111111L);
		hab.setNumeroIndividu(333908L);
		hab = (PersonnePhysique) hibernateTemplate.merge(hab);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), null, 5652,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		hab.addForFiscal(forFiscalPrincipal);
		forFiscalPrincipal.setMotifOuverture(MotifFor.ARRIVEE_HC);
		tacheService.genereTacheDepuisOuvertureForPrincipal(hab, forFiscalPrincipal, null);

		TacheCriteria criterion = new TacheCriteria();
		verifieTacheNouveauDossier(criterion, 1);

		assertTachesEnvoi(criterion, true);

	}


	@Test
	public void testGenereDemenagementVDDepuisOuvertureForPrincipal() {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(11111111L);
		hab.setNumeroIndividu(333908L);
		hab = (PersonnePhysique) hibernateTemplate.merge(hab);




		ForFiscalPrincipal forFiscalPrincipalDepart = new ForFiscalPrincipal(RegDate.get(2008, 6, 12), RegDate.get(2009, 6, 11), 5586,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		forFiscalPrincipalDepart.setMotifOuverture(MotifFor.ARRIVEE_HC);
		forFiscalPrincipalDepart.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		hab.addForFiscal(forFiscalPrincipalDepart);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2009, 6, 12), null, 5652,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		forFiscalPrincipal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		hab.addForFiscal(forFiscalPrincipal);
		
		tacheService.genereTacheDepuisOuvertureForPrincipal(hab, forFiscalPrincipal, null);

		TacheCriteria criterion = new TacheCriteria();
		verifierTacheControleDossier(criterion, 0);

		PersonnePhysique hab2 = new PersonnePhysique(true);
		hab2.setNumero(11111112L);
		hab2.setNumeroIndividu(333904L);
		hab2 = (PersonnePhysique) hibernateTemplate.merge(hab2);

		ForFiscalPrincipal forFiscalPrincipalDepart2 = new ForFiscalPrincipal(RegDate.get(2007, 6, 12), RegDate.get(2008, 6, 11), 5586,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		forFiscalPrincipalDepart2.setMotifOuverture(MotifFor.ARRIVEE_HC);
		forFiscalPrincipalDepart2.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		hab2.addForFiscal(forFiscalPrincipalDepart2);

		ForFiscalPrincipal forFiscalPrincipal2 = new ForFiscalPrincipal(RegDate.get(2008, 6, 12), null, 5652,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		forFiscalPrincipal2.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		hab2.addForFiscal(forFiscalPrincipal2);

		tacheService.genereTacheDepuisOuvertureForPrincipal(hab2, forFiscalPrincipal2, null);

		TacheCriteria criterion2 = new TacheCriteria();
		verifierTacheControleDossier(criterion2, 1);

	}

	@Test
	public void testGenereMariageDepuisOuvertureForPrincipal() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
		PersonnePhysique hab2 = (PersonnePhysique) tiersService.getTiers(12300002);

		EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(hab1, hab2, RegDate.get(2006, 6, 12), null);
		MenageCommun menage = ensemble.getMenage();

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), null, new Integer(5652),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		menage.addForFiscal(forFiscalPrincipal);

		forFiscalPrincipal.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		tacheService.genereTacheDepuisOuvertureForPrincipal(menage, forFiscalPrincipal, null);

		TacheCriteria criterion = new TacheCriteria();
		verifieTacheNouveauDossier(criterion, 1);

		assertTachesEnvoi(criterion, true);
	}

	@Test
	public void testGenereDecesDepuisOuvertureForPrincipal() throws Exception {

		// Etat 2008
		final Long idMenage = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				loadDatabase(DB_UNIT_DATA_FILE);

				PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
				PersonnePhysique hab2 = (PersonnePhysique) tiersService.getTiers(12300002);

				// Etat avant veuvage
				EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(hab1, hab2, RegDate.get(2000, 6, 12), null);
				MenageCommun menage = ensemble.getMenage();
				assertNotNull(menage);

				PeriodeFiscale pf2006 = (PeriodeFiscale) tacheDAO.getHibernateTemplate().get(PeriodeFiscale.class, 6L);
				PeriodeFiscale pf2007 = (PeriodeFiscale) tacheDAO.getHibernateTemplate().get(PeriodeFiscale.class, 7L);
				ModeleDocument modele2006 = (ModeleDocument) tacheDAO.getHibernateTemplate().get(ModeleDocument.class, 1L);
				ModeleDocument modele2007 = (ModeleDocument) tacheDAO.getHibernateTemplate().get(ModeleDocument.class, 5L);
				addDeclarationImpot(menage, pf2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(menage, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				return menage.getNumero();
			}
		});

		// Evénement de décès
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
				// Etat après veuvage
				ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), null, new Integer(5652),
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				hab1.addForFiscal(forFiscalPrincipal);
				forFiscalPrincipal.setMotifOuverture(MotifFor.VEUVAGE_DECES);

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
		verifieTacheNouveauDossier(criterion, 1);
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
	public void testGenereTacheApresDeces() throws Exception {

		// Etat 2010
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
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

		// Evénement de décès
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
				tiersService.closeForFiscalPrincipal(pp, dateDeces, MotifFor.VEUVAGE_DECES);
				return null;
			}
		});

		final List<Tache> taches = tacheDAO.getAll();
		assertNotNull(taches);
		assertEquals(2, taches.size());     // tâche de transmission de dossier + tâche d'annulation de la DI 2009

		// [UNIREG-1305]
		 TacheEnvoiDeclarationImpot tacheEnvoi = null;

		TacheTransmissionDossier tacheTransmission = null;
		TacheAnnulationDeclarationImpot tacheAnnulationDeclaration = null;
		for (Tache t : taches) {
			// [UNIREG-1305]
			//			if (t instanceof TacheEnvoiDeclarationImpot) {
			//				tacheEnvoi = (TacheEnvoiDeclarationImpot) t;
			//				continue;
			//			}
			if (t instanceof TacheTransmissionDossier) {
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
	public void testGenereTacheEmissionDiApresDeces() throws Exception {

		// Etat 2010
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				

				PersonnePhysique pp = addNonHabitant("Hubert", "Duchemole", date(1922, 7, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(2007, 9, 21), MotifFor.ARRIVEE_HC, MockCommune.Leysin);

				addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);


				return pp.getNumero();
			}
		});

		final CollectiviteAdministrative aciSuccessions = (CollectiviteAdministrative) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return  tiersService.getOrCreateCollectiviteAdministrative(serviceInfra.getACISuccessions().getNoColAdm());

			}
		});

		// [UNIREG-1956] date de décès explicitement déplacée de 2009 à 2008 pour vérifier que la DI 2009 (et pas la 2008) est annulée
		final RegDate dateDeces = date(2008, 12, 5);

		// Evénement de décès
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
				tiersService.closeForFiscalPrincipal(pp, dateDeces, MotifFor.VEUVAGE_DECES);
				return null;
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
	public void testGenereDivorceDepuisOuvertureForPrincipal() throws Exception {

		// Etat 2008
		final Long idMenage = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				loadDatabase(DB_UNIT_DATA_FILE);

				PersonnePhysique hab1 = (PersonnePhysique) tiersService.getTiers(12300001);
				PersonnePhysique hab2 = (PersonnePhysique) tiersService.getTiers(12300002);

				EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(hab1, hab2, RegDate.get(2006, 6, 12), null);
				MenageCommun menage = ensemble.getMenage();
				assertNotNull(menage);

				ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), null, new Integer(5652),
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				forFiscalPrincipal.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
				menage.addForFiscal(forFiscalPrincipal);

				PeriodeFiscale pf2006 = (PeriodeFiscale) tacheDAO.getHibernateTemplate().get(PeriodeFiscale.class, 6L);
				PeriodeFiscale pf2007 = (PeriodeFiscale) tacheDAO.getHibernateTemplate().get(PeriodeFiscale.class, 7L);
				ModeleDocument modele2006 = (ModeleDocument) tacheDAO.getHibernateTemplate().get(ModeleDocument.class, 1L);
				ModeleDocument modele2007 = (ModeleDocument) tacheDAO.getHibernateTemplate().get(ModeleDocument.class, 5L);
				addDeclarationImpot(menage, pf2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(menage, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				return menage.getNumero();
			}
		});

		// Divorce au 10 octobre 2007
		doInNewTransaction(new TxCallback() {
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

				ForFiscalPrincipal ffp1 = new ForFiscalPrincipal(forFiscalPrincipal.getDateFin().getOneDayAfter(), null, new Integer(5652),
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				ffp1.setMotifOuverture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				hab1.addForFiscal(ffp1);

				ForFiscalPrincipal ffp2 = new ForFiscalPrincipal(forFiscalPrincipal.getDateFin().getOneDayAfter(), null, new Integer(5652),
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
				ffp2.setMotifOuverture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				hab2.addForFiscal(ffp2);

				tacheService.genereTacheDepuisOuvertureForPrincipal(hab1, ffp1, null);
				tacheService.genereTacheDepuisOuvertureForPrincipal(hab2, ffp2, null);
				return null;
			}
		});

		TacheCriteria criterion = new TacheCriteria();
		verifieTacheNouveauDossier(criterion, 2);
	}

	@Test
	public void testGenereTacheDepuisOuvertureForSecondaireActivite() throws Exception {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(new Long(11111111));
		hab.setNumeroIndividu(new Long(333908));
		hab = (PersonnePhysique) hibernateTemplate.merge(hab);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), null, new Integer(8201),
				TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		ForFiscalSecondaire forFiscalSecondaire = new ForFiscalSecondaire(RegDate.get(2006, 6, 12), null, new Integer(5652),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ACTIVITE_INDEPENDANTE);

		hab.addForFiscal(forFiscalPrincipal);
		forFiscalSecondaire.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
		hab.addForFiscal(forFiscalSecondaire);

		tacheService.genereTacheDepuisOuvertureForSecondaire(hab, forFiscalSecondaire);

		TacheCriteria criterion = new TacheCriteria();
		verifieTacheNouveauDossier(criterion, 1);

		assertTachesEnvoi(criterion, false);

	}

	@Test
	public void testGenereTacheDepuisOuvertureForSecondaireImmeuble() throws Exception {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(new Long(11111111));
		hab.setNumeroIndividu(new Long(333908));
		hab = (PersonnePhysique) hibernateTemplate.merge(hab);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2006, 6, 12), null, new Integer(8201),
				TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		ForFiscalSecondaire forFiscalSecondaire = new ForFiscalSecondaire(RegDate.get(2006, 6, 12), null, new Integer(5652),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);

		hab.addForFiscal(forFiscalPrincipal);
		forFiscalSecondaire.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
		hab.addForFiscal(forFiscalSecondaire);

		tacheService.genereTacheDepuisOuvertureForSecondaire(hab, forFiscalSecondaire);

		TacheCriteria criterion = new TacheCriteria();
		verifieTacheNouveauDossier(criterion, 1);

	}


	@Test
	public void testGenereTacheDepartHSDepuisFermetureForPrincipal() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		PersonnePhysique hab = (PersonnePhysique) tiersService.getTiers(12300001);
		ForFiscalPrincipal forFiscalPrincipal = addForPrincipal(hab, date(2004, 6, 12), MotifFor.ARRIVEE_HC, date(2006, 6, 12), MotifFor.DEPART_HS, MockCommune.VillarsSousYens);

		tacheService.genereTacheDepuisFermetureForPrincipal(hab, forFiscalPrincipal);

		TacheCriteria criterion = new TacheCriteria();
		verifieControleDossier(criterion);

		verifieTachesAnnulation(criterion, 1, false);

	}

	/**
	 * Scénario : un contribuable vaudois par hors-Suisse en 2005, mais on ne reçoit l'événement de départ qu'en 2008
	 */
	@Test
	public void testGenereTacheDepartHSArriveeTardiveEvenement() throws Exception {

		class Ids {
			Long raoulId;
			Long jeanDanielId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale pf2003 = addPeriodeFiscale(2003);
				PeriodeFiscale pf2004 = addPeriodeFiscale(2004);
				PeriodeFiscale pf2005 = addPeriodeFiscale(2005);
				PeriodeFiscale pf2006 = addPeriodeFiscale(2006);
				PeriodeFiscale pf2007 = addPeriodeFiscale(2007);
				PeriodeFiscale pf2008 = addPeriodeFiscale(2008);
				PeriodeFiscale pf2009 = addPeriodeFiscale(2009);
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
		doInNewTransaction(new TxCallback() {
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
			final List<Declaration> declarations2005 = raoul.getDeclarationForPeriode(2005);
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
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ServiceInfrastructureService.noCEDI, null, declaration);
		}
	}

	/**
	 * Vérifie la création des tâches en cas de fermeture tardive du for fiscal principal où le contribuable repart hors-canton la même année de son arrivée.
	 */
	@Test
	public void testGenereTacheDepartHCDepuisFermetureForPrincipal() throws Exception {

		final PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
		final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

		final PersonnePhysique pp = addNonHabitant("François", "Dardare-style", date(1977, 1, 1), Sexe.MASCULIN);
		addDeclarationImpot(pp, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
		addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

		final ForFiscalPrincipal forFiscalPrincipal = addForPrincipal(pp, date(2006, 6, 12), MotifFor.ARRIVEE_HC, date(2006, 6, 12), MotifFor.DEPART_HC, MockCommune.VillarsSousYens);

		// simule la fermeture tardive (= événement de départ reçu en 2010, par exemple)
		tacheService.genereTacheDepuisFermetureForPrincipal(pp, forFiscalPrincipal);

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
	@NotTransactional
	@Test
	public void testGenereTacheDepartHCTardif() throws Exception {

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		// Création de la situation initiale : contribuable vaudois ordinaire dans le canton depuis 2008 avec une DI pour 2008
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

				final Contribuable raoul = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(2008, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Orbe);

				final DeclarationImpotOrdinaire di = addDeclarationImpot(raoul, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
				addEtatDeclaration(di, date(2009, 1, 15), TypeEtatDeclaration.EMISE);
				addEtatDeclaration(di, date(2009, 2, 21), TypeEtatDeclaration.RETOURNEE);
				
				return null;
			}
		});

		// Saisie d'un départ hors-canton au mileu 2008 (départ entrée tardivement, donc)
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
				assertNotNull(raoul);

				tiersService.closeForFiscalPrincipal(raoul, date(2008, 5, 23), MotifFor.DEPART_HC);
				tiersService.openForFiscalPrincipal(raoul, date(2008, 5, 24), MotifRattachement.DOMICILE, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.ORDINAIRE,
						MotifFor.DEPART_HC, true);

				return null;
			}
		});

		// Vérifie qu'il y a bien une tâche d'annulation de la DI 2008 et qu'elle est bien assignée à l'OID d'Orbe
		doInNewTransactionAndSession(new TxCallback() {
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
	public void testGenereTacheDepartHCDepuisFermetureForPrincipaleFinPeriode() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
		PersonnePhysique hab = (PersonnePhysique) tiersService.getTiers(12300001);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2007, 6, 12), RegDate.get(2007, 12, 31),
				new Integer(5652), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		forFiscalPrincipal.setMotifFermeture(MotifFor.DEPART_HC);

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
	public void testClotureDuDernierForSecondaireUNIREG1110() throws Exception {

		final PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
		final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

		final PersonnePhysique pp = addNonHabitant("François", "Dardare-style", date(1977, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2005, 6, 12), MotifFor.DEBUT_EXPLOITATION, MockPays.Allemagne);
		addDeclarationImpot(pp, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
		addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

		final ForFiscalSecondaire forFiscalSecondaire =
				addForSecondaire(pp, date(2005, 6, 12), MotifFor.DEBUT_EXPLOITATION, date(2006, 6, 11), MotifFor.FIN_EXPLOITATION, MockCommune.LeLieu.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

		tacheService.genereTacheDepuisFermetureForSecondaire(pp, forFiscalSecondaire);

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
		final List<Declaration> declarations2006 = pp.getDeclarationForPeriode(2006);
		assertNotNull(declarations2006);
		assertEquals(1, declarations2006.size());

		final DeclarationImpotOrdinaire declaration2006 = (DeclarationImpotOrdinaire)declarations2006.get(0);
		assertNotNull(declaration2006);
		assertEquals(date(2006, 1, 1), declaration2006.getDateDebut());
		assertEquals(date(2006, 6, 11), declaration2006.getDateFin());
		assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, declaration2006.getTypeContribuable());
	}


	/**
	 * L'ouverture d'un for principal sur un contribuable ordinaire doit générer un envoi de DIs et l'ouverture d'un dossier
	 */
	@Test
	public void testOuvertureForPrincipalImpositionOrdinaire() {
		ouvreForPrincipal(ModeImposition.ORDINAIRE, MotifFor.ARRIVEE_HS, RegDate.get(2006, 6, 12));
		assertEquals(1, getNouveauDossierCount());
		assertEquals(RegDate.get().year() - 2006, getTacheCount());
	}

	/**
	 * L'ouverture d'un for principal sur un contribuable sourcier ne doit pas générer d'envoi de DIs, ni d'ouverture de dossier
	 */
	@Test
	public void testOuvertureForPrincipalImpositionSourcier() {
		ouvreForPrincipal(ModeImposition.SOURCE, MotifFor.ARRIVEE_HS, RegDate.get(2006, 6, 12));
		assertEquals(0, getNouveauDossierCount());
		assertEquals(0, getTacheCount());
	}

	/**
	 * UNIREG-533: on teste que la fermeture d'un for fiscal principal avec une date de fermeture qui précède la date de début génère bien une exception et que celle-ci est une exception de validation.
	 */
	@Test
	public void testCloseForFiscalPrincipalDateFermetureAvantDateDebut() throws Exception {

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
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
			List<String> errors = e.getErrors();
			assertEquals(1, errors.size());
			assertEquals("La date de fermeture (31.12.1990) est avant la date de début (01.01.1998) du for fiscal actif", errors.get(0));
		}
	}

	/**
	 * Vérifie que la fermeture d'un for principal hors-Suisse ne génère aucune tâche.
	 * <p/>
	 * <b>Note:</b> ce test reproduit un cas réel (voir ctb n°52108102) où le départ HS à été entré comme déménagement VD, ce qui est évidemment incorrecte.
	 */
	@Test
	public void testFermetureForPrincipalHorsSuisse() throws Exception {

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
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
	public void testGenereTacheDIFinActiviteIndependanteCtbHS() throws Exception {

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2003 = addPeriodeFiscale(2003);
				final ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);
				final PeriodeFiscale periode2004 = addPeriodeFiscale(2004);
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

		doInNewTransaction(new TxCallback() {
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
		Assert.notNull(pp);

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
		assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(RegDate.get()), date(2005, 1, 1), date(2005, 2, 1), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, null, tache);
	}

	/**
	 * Vérifie qu'il y a le nombre correct d'annulation de DIs et d'émission de DIs générées lors du mariage de deux contribuables pour une période passée (= événement de mariage reçu en retard).
	 */
	@Test
	public void testGenerateTacheMariagePeriodePassee() throws Exception {

		final RegDate dateMariage = date(2007, 11, 11);
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
				addAdresse(monsieur, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null, 0);

				MockIndividu madame = addIndividu(idIndividuMadame, RegDate.get(1968, 3, 27), "Lavanchy", "Laurence", false);
				addAdresse(madame, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1968, 3, 27), null);
				addNationalite(madame, MockPays.Suisse, date(1963, 1, 1), null, 0);
			}
		});

		doInNewTransaction(new TxCallback() {
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
					PeriodeFiscale periode = addPeriodeFiscale(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(monsieur, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
					addDeclarationImpot(madame, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique monsieur = (PersonnePhysique) tiersService.getTiers(ids.monsieurId);
				assertNotNull(monsieur);
				PersonnePhysique madame = (PersonnePhysique) tiersService.getTiers(ids.madameId);
				assertNotNull(madame);

				// mariage au 11 novembre 2007
				MenageCommun menage = metierService.marie(dateMariage, monsieur, madame, "", EtatCivil.MARIE, true, null);
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
		assertEquals(RegDate.get().year() - 2007, envois.size());       // 2007 et 2008 en 2009, plus 2009 en 2010...

		sortTachesEnvoi(envois);

		// les deux premières années d'assujettissement, on ne trouve pas de DI mais on considère le contribuable (ménage)
		// comme nouvel assujetti (n-1 ou n-2 n'a pas d'assujettissement du tout), donc VAUD_TAX
		for (int i = 2007 ; i < 2009 ; ++ i) {
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
		}

		// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable (ménage)
		// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
		for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, envois.get(i - 2007));
		}
	}

	/**
	 * [UNIREG-1105] Vérifie qu'il y a le nombre correct d'annulation de DIs et d'émission de DIs générées lors du divorce de deux contribuables pour une période passée (= événement de divorce reçu en
	 * retard).
	 */
	@Test
	public void testGenerateTacheDivorcePeriodePassee() throws Exception {

		final RegDate dateMariage = date(2002, 5, 1);
		final RegDate dateDivorce = date(2007, 11, 11);
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
				addAdresse(monsieur, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null, 0);

				MockIndividu madame = addIndividu(idIndividuMadame, RegDate.get(1968, 3, 27), "Lavanchy", "Laurence", false);
				addAdresse(madame, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1968, 3, 27), null);
				addNationalite(madame, MockPays.Suisse, date(1963, 1, 1), null, 0);
			}
		});

		doInNewTransaction(new TxCallback() {
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
					PeriodeFiscale periode = addPeriodeFiscale(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(menage, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				MenageCommun menage = (MenageCommun) tiersService.getTiers(ids.menageId);
				assertNotNull(menage);

				// divorce au 11 novembre 2007
				metierService.separe(menage, dateDivorce, "", EtatCivil.DIVORCE, true, null);
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

			// les deux premières années d'assujettissement, on ne trouve pas de DI mais on considère le contribuable
			// comme nouvel assujetti (n-1 ou n-2 n'a pas d'assujettissement du tout), donc VAUD_TAX
			for (int i = 2007 ; i < 2009 ; ++ i) {
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable
			// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
			for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")
			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier);
			assertNotNull(nouveaux);
			assertEquals(1, nouveaux.size());
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

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

			// les deux premières années d'assujettissement, on ne trouve pas de DI mais on considère le contribuable
			// comme nouvel assujetti (n-1 ou n-2 n'a pas d'assujettissement du tout), donc VAUD_TAX
			for (int i = 2007 ; i < 2009 ; ++ i) {
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable
			// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
			for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")
			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier);
			assertNotNull(nouveaux);
			assertEquals(1, nouveaux.size());
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

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
	public void testGenerateTacheDivorceAvecForSecondaire() throws Exception {

		final RegDate dateMariage = date(2002, 5, 1);
		final RegDate dateDivorce = date(2007, 11, 11);
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
				addAdresse(monsieur, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null, 0);

				MockIndividu madame = addIndividu(idIndividuMadame, RegDate.get(1968, 3, 27), "Lavanchy", "Laurence", false);
				addAdresse(madame, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1968, 3, 27), null);
				addNationalite(madame, MockPays.Suisse, date(1963, 1, 1), null, 0);
			}
		});

		doInNewTransaction(new TxCallback() {
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

				addForSecondaire(menage, dateMariage, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(),
						MotifRattachement.IMMEUBLE_PRIVE);

				// Ajoute les déclarations qui vont bien
				for (int i = 2003; i <= 2008; ++i) {
					PeriodeFiscale periode = addPeriodeFiscale(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(menage, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				MenageCommun menage = (MenageCommun) tiersService.getTiers(ids.menageId);
				assertNotNull(menage);

				// divorce au 11 novembre 2007
				metierService.separe(menage, dateDivorce, "", EtatCivil.DIVORCE, true, null);
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

			// les deux premières années d'assujettissement, on ne trouve pas de DI mais on considère le contribuable
			// comme nouvel assujetti (n-1 ou n-2 n'a pas d'assujettissement du tout), donc VAUD_TAX
			for (int i = 2007 ; i < 2009 ; ++ i) {
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable
			// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
			for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")
			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier);
			assertNotNull(nouveaux);
			assertEquals(1, nouveaux.size());
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

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

			// les deux premières années d'assujettissement, on ne trouve pas de DI mais on considère le contribuable
			// comme nouvel assujetti (n-1 ou n-2 n'a pas d'assujettissement du tout), donc VAUD_TAX
			for (int i = 2007 ; i < 2009 ; ++ i) {
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable
			// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
			for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")
			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.monsieurId, TypeTache.TacheNouveauDossier);
			assertNotNull(nouveaux);
			assertEquals(1, nouveaux.size());
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

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
				addAdresse(monsieur, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null, 0);

				MockIndividu madame = addIndividu(idIndividuMadame, RegDate.get(1968, 3, 27), "Lavanchy", "Laurence", false);
				addAdresse(madame, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1968, 3, 27), null);
				addNationalite(madame, MockPays.Suisse, date(1963, 1, 1), null, 0);
			}
		});

		doInNewTransaction(new TxCallback() {
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
					PeriodeFiscale periode = addPeriodeFiscale(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(menage, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				}

				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique monsieur = (PersonnePhysique) tiersService.getTiers(ids.monsieurId);
				assertNotNull(monsieur);

				// décès de monsieur au 11 novembre 2007
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
			assertTache(TypeEtatTache.EN_INSTANCE, getNextSunday(RegDate.get()), dateDeces.getOneDayAfter(), date(2007, 12, 31),
					TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,TypeAdresseRetour.CEDI, envois.get(0));
			assertTache(TypeEtatTache.EN_INSTANCE,  getNextSunday(RegDate.get()), date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_VAUDTAX,TypeAdresseRetour.CEDI, envois.get(1));


			// Vérifie qu'il y a bien une tâche nouveau dossier (voir spécification SCU-EngendrerUneTacheEnInstance §3.1.14
			// "Fermeture en raison d’une séparation, d’un divorce ou d’une dissolution de partenariat")
			final List<TacheNouveauDossier> nouveaux = tacheDAO.listTaches(ids.madameId, TypeTache.TacheNouveauDossier);
			assertNotNull(nouveaux);
			assertEquals(1, nouveaux.size());
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, nouveaux.get(0));

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
	public void testGenereTachesDepuisArriveeHSContribuablePossedantDejaImmeuble() throws Exception {

		final RegDate dateArrivee = date(2005, 5, 1);
		final RegDate nextSunday = getNextSunday(RegDate.get());

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(100000, RegDate.get(1963, 1, 1), "Duplot", "Simon", true);
				addAdresse(monsieur, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1963, 1, 1), null);
				addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null, 0);
			}
		});

		class Ids {
			Long simonId;
		}
		final Ids ids = new Ids();

		/*
		 * Un contribuable domicilié au Danemark et qui possède un immeuble depuis 2000 à Cossonay
		 */
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable simon = addHabitant(100000);
				ids.simonId = simon.getNumero();
				addForPrincipal(simon, date(1981, 1, 1), MotifFor.MAJORITE, MockPays.Danemark);
				addForSecondaire(simon, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				for (int i = 2003; i < RegDate.get().year(); ++i) {
					PeriodeFiscale periode = addPeriodeFiscale(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(simon, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.HORS_SUISSE, modele);
				}
				return null;
			}
		});

		// Arrivée de hors-Suisse traitée tardivement
		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.simonId);
		assertNotNull(raoul);
		tiersService.closeForFiscalPrincipal(raoul, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS);
		tiersService.openForFiscalPrincipal(raoul, dateArrivee, MotifRattachement.DOMICILE, MockCommune.Lausanne.getNoOFS(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE, MotifFor.ARRIVEE_HS, true);

		// [UNIREG-2305] Arrivé de hors-Suisse enregistrée tardivement -> l'assujettissement passe de hors-Suisse à ordinaire à partir du 1er janvier 2005,
		// mais comme le type de document reste le même (déclaration ordinaire), le type de contribuable est simplement mis-à-jour sur les déclarations et
		// aucune tâche n'est créée.

		// aucune tâche de manipulation de DIs ne doit être créée
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setContribuable(raoul);
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		assertEmpty(tacheDAO.find(criterion));
		criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
		assertEmpty(tacheDAO.find(criterion));

		// les types de contribuables doivent avoir été mis-à-jour
		for (int i = 2005; i < RegDate.get().year(); ++i) {
			final List<Declaration> declarations = raoul.getDeclarationForPeriode(i);
			assertNotNull(declarations);
			assertEquals(1, declarations.size());
			assertDI(date(i, 1, 1), date(i, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					ServiceInfrastructureService.noCEDI, null, declarations.get(0));
		}
	}

	@Test
	public void testGenereTachesDepuisAnnulationDeFor() throws Exception {

		// 1 Contribuable avec 1 for courant sur 2008 + 1 declaration 2008
		// On annule le for : la tache de annulation de di doit etre creer

		final RegDate nextSunday = getNextSunday(RegDate.get());

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				serviceCivil.setUp(new MockServiceCivil() {
					@Override
					protected void init() {

						MockIndividu monsieur = addIndividu(100000, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
						addAdresse(monsieur, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
								.get(1963, 1, 1), null);
						addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null, 0);
					}
				});
				Contribuable raoul = addHabitant(100000);
				addForPrincipal(raoul, date(2008, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.raoulId = raoul.getNumero();
				PeriodeFiscale pf2008 = addPeriodeFiscale(2008);
				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2008);
				addDeclarationImpot(raoul, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				return null;

			}
		});

		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);
		tiersService.annuleForFiscal(raoul.getForFiscalPrincipalAt(date(2008, 1, 1)), true);

		// Annulation du for fiscal -> Une tache d'annulation pour la DI 2008 doit etre generée
		List<Tache> taches = tacheDAO.getAll();
		assertEquals(1, taches.size());
		assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2008, 1, 1), date(2008, 12, 31), (TacheAnnulationDeclarationImpot) taches.get(0));

	}

	@Test
	public void testgenereTacheDepuisFermetureForPrincipalUNIREG1303() throws Exception {

		// 1 Contribuable qui décéde avec 1 déclarartion active : la période de la DI doit etre ajusté
		// et aucune tache d'émission de DI ne doit être émise.

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				serviceCivil.setUp(new MockServiceCivil() {
					@Override
					protected void init() {

						MockIndividu monsieur = addIndividu(100000, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
						addAdresse(monsieur, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
								.get(1963, 1, 1), null);
						addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null, 0);
					}
				});
				Contribuable raoul = addHabitant(100000);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				for (int i = 2003; i <= 2008; i++) {
					PeriodeFiscale periode = addPeriodeFiscale(i);
					ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
					addDeclarationImpot(raoul, periode, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele).getId();
				}
				return null;
			}
		});

		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);
		tiersService.closeForFiscalPrincipal(raoul, date(2008, 11, 1), MotifFor.VEUVAGE_DECES);

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
	public void testgenereTacheDepuisFermetureForPrincipalUNIREG1305() throws Exception {

		// 1 Contribuable qui décéde avec sans déclarartion active : une tache d'émission de DI doit être généré.

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				serviceCivil.setUp(new MockServiceCivil() {
					@Override
					protected void init() {

						MockIndividu monsieur = addIndividu(100000, date(1963, 1, 1), "Lavanchy", "Raoul", true);
						addAdresse(monsieur, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
								.get(1963, 1, 1), null);
						addNationalite(monsieur, MockPays.Suisse, date(1963, 1, 1), null, 0);
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
			if (TypeTache.TacheEnvoiDeclarationImpot.equals(t.getTypeTache())) {
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
	public void testOnAnnulationContribuableSansTache() throws Exception {

		// Un contribuable normal avec des tâches associées.

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu raoul = addIndividu(100000, RegDate.get(1963, 1, 1), "Lavanchy", "Raoul", true);
				addAdresse(raoul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1963, 1, 1), null);
				addNationalite(raoul, MockPays.Suisse, date(1963, 1, 1), null, 0);
			}
		});

		class Ids {
			Long raoulId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Contribuable raoul = addHabitant(100000);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				return null;
			}
		});

		final Tiers raoul = tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);

		// annulation du tiers
		tiersService.annuleTiers(raoul);

		final List<Tache> taches = tacheDAO.find(ids.raoulId);
		assertEmpty(taches);
	}

	/**
	 * [UNIREG-1218] Vérifie que l'annulation d'un contribuable annule bien toutes les tâches associées à ce contribuable (à l'exception des tâches d'annulation de DIs qui restent valables).
	 *
	 * @throws Exception
	 */
	@Test
	public void testOnAnnulationContribuableAvecTaches() throws Exception {

		// Un contribuable normal sans aucune tâche associée.

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu raoul = addIndividu(100000, date(1963, 1, 1), "Lavanchy", "Raoul", true);
				addAdresse(raoul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(
						1963, 1, 1), null);
				addNationalite(raoul, MockPays.Suisse, date(1963, 1, 1), null, 0);
			}
		});

		class Ids {
			Long raoulId;
			Long tacheAnnulDI;
			Long tacheControl;
			Long tacheEnvoi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
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
						raoul, null, colAdm);
				ids.tacheEnvoi = tacheEnvoi.getId();
				return null;
			}
		});

		final Tiers raoul = tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);

		// annulation du tiers
		tiersService.annuleTiers(raoul);

		final List<Tache> taches = tacheDAO.find(ids.raoulId);
		assertNotNull(taches);
		assertEquals(3, taches.size());

		final Tache tacheAnnul = tacheDAO.get(ids.tacheAnnulDI);
		assertNotNull(tacheAnnul);
		assertFalse(tacheAnnul.isAnnule());

		final Tache tacheControl = tacheDAO.get(ids.tacheControl);
		assertNotNull(tacheControl);
		assertTrue(tacheControl.isAnnule());

		final Tache tacheEnvoi = tacheDAO.get(ids.tacheEnvoi);
		assertNotNull(tacheEnvoi);
		assertTrue(tacheEnvoi.isAnnule());
	}

	private void sortTachesEnvoi(final List<TacheEnvoiDeclarationImpot> envois) {
		Collections.sort(envois, new Comparator<TacheEnvoiDeclarationImpot>() {
			public int compare(TacheEnvoiDeclarationImpot o1, TacheEnvoiDeclarationImpot o2) {
				return o1.getDateDebut().compareTo(o2.getDateDebut());
			}
		});
	}

	private void sortTachesAnnulation(final List<TacheAnnulationDeclarationImpot> annulations) {
		Collections.sort(annulations, new Comparator<TacheAnnulationDeclarationImpot>() {
			public int compare(TacheAnnulationDeclarationImpot o1, TacheAnnulationDeclarationImpot o2) {
				return o1.getDeclarationImpotOrdinaire().getDateDebut().compareTo(o2.getDeclarationImpotOrdinaire().getDateDebut());
			}
		});
	}

	private void ouvreForPrincipal(ModeImposition modeImposition, MotifFor motifOuverture, RegDate dateOuverture) {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(new Long(11111111));
		hab.setNumeroIndividu(new Long(333908));
		hab = (PersonnePhysique) hibernateTemplate.merge(hab);

		ForFiscalPrincipal f = new ForFiscalPrincipal(dateOuverture, null, new Integer(5652), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE, modeImposition);
		f.setMotifOuverture(motifOuverture);
		hab.addForFiscal(f);
		tacheService.genereTacheDepuisOuvertureForPrincipal(hab, f, null);
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
			Assert.isTrue(taches.get(0) instanceof TacheEnvoiDeclarationImpot);
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
			Assert.isTrue(taches.get(0) instanceof TacheEnvoiDeclarationImpot);
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
		for (int i = 0; i < size; ++i) {
			Assert.isTrue(taches.get(i) instanceof TacheAnnulationDeclarationImpot);
			tacheAnnulation = (TacheAnnulationDeclarationImpot) taches.get(i);
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
			Assert.isNull(declarationImpotOrdinaire.getAnnulationDate());
		}


	}

	@Test
	public void testChangementModeImpositionQuitteSourceVersMixte() {
		final List<Tache> taches = genereChangementImposition(ModeImposition.SOURCE, ModeImposition.MIXTE_137_2);
		assertEquals(1, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(RegDate.get().year() - 2006, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	public void testChangementModeImpositionQuitteSourceVersDepense() {
		final List<Tache> taches = genereChangementImposition(ModeImposition.SOURCE, ModeImposition.DEPENSE);
		assertEquals(1, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(RegDate.get().year() - 2006, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	public void testChangementModeImpositionOrdinaireVersIndigent() {
		final List<Tache> taches = genereChangementImposition(ModeImposition.ORDINAIRE, ModeImposition.INDIGENT);
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(0, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	public void testChangementModeImpositionIndigentVersOrdinaire() {
		final List<Tache> taches = genereChangementImposition(ModeImposition.INDIGENT, ModeImposition.ORDINAIRE);
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(0, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	public void testChangementModeImpositionMixteVersOrdinaire() {
		final List<Tache> taches = genereChangementImposition(ModeImposition.MIXTE_137_2, ModeImposition.ORDINAIRE);
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		assertEquals(0, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	public void testChangementModeImpositionMixteVersDepense() {
		final List<Tache> taches = genereChangementImposition(ModeImposition.MIXTE_137_2, ModeImposition.DEPENSE);
		assertEquals(0, countTaches(TypeTache.TacheNouveauDossier, taches));
		// annulation et ré-envoi des DIs 2006..[année précédente] parce que les types de document ont changé
		assertEquals(RegDate.get().year() - 2006, countTaches(TypeTache.TacheAnnulationDeclarationImpot, taches));
		assertEquals(RegDate.get().year() - 2006, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	@Test
	public void testAnnulationForSansOidGestion() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
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
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				final CollectiviteAdministrative oid = tiersService.getOfficeImpotAt(pp, null);
				assertNull(oid);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
				tiersService.annuleForFiscal(ffp, false);

				return null;
			}
		});

		// aucune tâche générée ?
		doInNewTransactionAndSession(new TransactionCallback() {
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
	public void testSeparationSurContribuableHS() throws Exception {

		class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransactionAndSession(new TransactionCallback() {
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

		metierService.separe(mc, date(2009, 6, 12), "Test", EtatCivil.SEPARE, false, null);

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

	@Ignore(value = "Après la correction du cas jira UNIREG-2378, on a découvert qu'une tâche d'envoi de DI est générée à tort")
	@Test
	public void testSeparationSurContribuableHSAvecImmeuble() throws Exception {

		// TODO(msi,jde) Enlever le @Ignore une fois le cas UNIREG-2380 corrigé.

		class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Achille", "Talon", date(1965, 12, 5), Sexe.MASCULIN);
				lui.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final PersonnePhysique elle = addNonHabitant("Géraldine", "Talon", date(1966, 4, 12), Sexe.FEMININ);
				elle.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(1990, 5, 1), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(1995, 1, 10), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(mc, date(1995, 1, 10), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = mc.getNumero();
				return null;
			}
		});

		// séparation
		final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.idMenage);
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, null);

		metierService.separe(mc, date(2009, 6, 12), "Test", EtatCivil.SEPARE, false, null);

		// vérification des tâches générées : pour lui
		{
			// toujours pas de tâche, car on ne sait pas sur quel contribuable mettre le for secondaire
			// (mais une tâche de contrôle de dossier doit avoir été ouverte sur le couple)

			final TacheCriteria criteria = new TacheCriteria();
			criteria.setContribuable(couple.getPrincipal());
			final List<Tache> taches = tacheDAO.find(criteria);
			assertNotNull(taches);
			assertEquals(0, taches.size());
		}
		// vérification des tâches générées : pour elle
		{
			// toujours pas de tâche, car on ne sait pas sur quel contribuable mettre le for secondaire
			// (mais une tâche de contrôle de dossier doit avoir été ouverte sur le couple)

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
	public void testDivorceMenageCommunSourcePur() throws Exception {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ilya = addIndividu(915112, date(1967, 8, 1), "Eigenbrot", "Ilya", true);
				addAdresse(ilya, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2008, 8, 22), null);
				final MockIndividu katharine = addIndividu(915113, date(1969, 8, 15), "Darling", "Katharine", false);
				addAdresse(katharine, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2008, 8, 22), null);
			}
		});

		class Ids {
			long ilya;
			long katharine;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple de sourciers arrivés de hors-Suisse en 2008
		doInNewTransactionAndSession(new TransactionCallback() {
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
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final MenageCommun menage = (MenageCommun) hibernateTemplate.get(MenageCommun.class, ids.menage);
				metierService.separe(menage, date(2008, 11, 15), null, EtatCivil.DIVORCE, false, null);
				return null;
			}
		});

		// Vérifie qu'ils sont bien divorcés
		{
			final PersonnePhysique ilya = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.ilya);
			final ForFiscalPrincipal dernier = ilya.getDernierForFiscalPrincipal();
			assertNotNull(dernier);
			assertEquals(date(2008, 11, 15), dernier.getDateDebut());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dernier.getMotifOuverture());
		}

		{
			final PersonnePhysique katharine = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.katharine);
			final ForFiscalPrincipal dernier = katharine.getDernierForFiscalPrincipal();
			assertNotNull(dernier);
			assertEquals(date(2008, 11, 15), dernier.getDateDebut());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dernier.getMotifOuverture());
		}

		{
			final MenageCommun menage = (MenageCommun) hibernateTemplate.get(MenageCommun.class, ids.menage);
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
	public void testFermetureForFiscalPrincipalUNIREG1888() throws Exception {
		final RegDate dateOuverture = RegDate.get(2006, 7, 1);

		// création des DI 2006 et 2007 avant la notification du départ en 2007
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale pf2006 = addPeriodeFiscale(2006);
				final PeriodeFiscale pf2007 = addPeriodeFiscale(2007);
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
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.closeForFiscalPrincipal(ffp, depart, MotifFor.DEPART_HS);
				return null;
			}
		});

		// la DI 2007 doit avoir une durée de validité réduite suite au départ
		doInNewTransaction(new TxCallback() {
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
	public void testDetermineSynchronizeActionsForDIsOptionnelles() throws Exception {

		// Contribuable hors-Suisse avec un immeuble dans le canton
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.Albanie);
		addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// (précondition) La déclaration d'impôt est optionnelle
		final List<PeriodeImposition> periodes = PeriodeImposition.determine(pp, 2005);
		assertNotNull(periodes);
		assertEquals(1, periodes.size());

		final PeriodeImposition periode0 = periodes.get(0);
		assertNotNull(periode0);
		assertTrue(periode0.isOptionnelle());
		assertFalse(periode0.isRemplaceeParNote());
		assertFalse(periode0.isDiplomateSuisse());

		// On vérifie que même s'il n'y a pas de déclaration, aucune tâche n'est créée (puisque les déclarations sont toutes optionnelles)
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	public void testDetermineSynchronizeActionsForDIsRemplaceesParNote() throws Exception {

		// Contribuable hors-Canton qui commence une activité indépendante à Renense et décède la même année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2005, 1, 1), MotifFor.DEBUT_EXPLOITATION, date(2005, 8, 1), MotifFor.VEUVAGE_DECES, MockCommune.Geneve);
		addForSecondaire(pp, date(2005, 1, 1), MotifFor.DEBUT_EXPLOITATION, date(2005, 8, 1), MotifFor.VEUVAGE_DECES, MockCommune.Renens.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

		// (précondition) La déclaration d'impôt est remplacée par une note à l'administration fiscale de l'autre canton
		final List<PeriodeImposition> periodes = PeriodeImposition.determine(pp, 2005);
		assertNotNull(periodes);
		assertEquals(1, periodes.size());

		final PeriodeImposition periode0 = periodes.get(0);
		assertNotNull(periode0);
		assertFalse(periode0.isOptionnelle());
		assertTrue(periode0.isRemplaceeParNote());
		assertFalse(periode0.isDiplomateSuisse());

		// On vérifie que même s'il n'y a pas de déclaration, aucune tâche n'est créée (puisque la déclaration est remplacée par une note)
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	public void testDetermineSynchronizeActionsForDIsCasDuDiplomateSuisse() throws Exception {

		// Contribuable du diplomate suisse basé à l'étranger
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2005, 1, 1), MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DIPLOMATE_SUISSE);

		// (précondition) La déclaration d'impôt est envoyée par la confédération, ce n'est pas la responsabilité du canton
		final List<PeriodeImposition> periodes = PeriodeImposition.determine(pp, 2005);
		assertNotNull(periodes);
		assertEquals(1, periodes.size());

		final PeriodeImposition periode0 = periodes.get(0);
		assertNotNull(periode0);
		assertFalse(periode0.isOptionnelle());
		assertFalse(periode0.isRemplaceeParNote());
		assertTrue(periode0.isDiplomateSuisse());

		// On vérifie que même s'il n'y a pas de déclaration, aucune tâche n'est créée (puisque la déclaration ne doit pas être envoyée par le canton)
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	public void testDetermineSynchronizeActionsForDIsSansDIAvecTacheEnvoiPreexistante() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		// une tâche d'envoi en instance qui correspond parfaitement à la déclaration manquante
		addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2006, 1, 1), date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pp, Qualification.AUTOMATIQUE, cedi);

		// On vérifie qu'aucune nouvelle tâche n'est créée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertEmpty(actions);
	}

	@Test
	public void testDetermineSynchronizeActionsForDIsSansDIAvecTacheEnvoiPreexistanteMaisIncorrecte() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		// une tâche d'envoi en instance qui ne possède pas le bon type de contribuable
		TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2006, 1, 1), date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE,
				TypeDocument.DECLARATION_IMPOT_DEPENSE, pp, Qualification.AUTOMATIQUE, cedi);

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
	public void testDetermineSynchronizeActionsForDIsSansDISansForsAvecTacheEnvoi() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		// une tâche d'envoi en instance qui est incorrect
		TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2006, 1, 1), date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE,
				TypeDocument.DECLARATION_IMPOT_DEPENSE, pp, Qualification.AUTOMATIQUE, cedi);

		// On vérifie que la tâche est annulée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertAnnuleTache(tache, actions.get(0));
	}

	@Test
	public void testDetermineSynchronizeActionsForDIsUneDI() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
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
	public void testDetermineSynchronizeActionsForDIsUneDIHorsPeriode() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
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
	public void testDetermineSynchronizeActionsForDIsUneDITypeContribuableIncompatible() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration qui couvre tout l'année (incorrect) avec un type de contribuable vaudois dépense (incorrect)
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modele);

		// On vérifie que :
		// - la déclaration est annulée (le type de contribuable/document n'est pas compatible)
		// - une nouvelle déclaration est créée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(2, actions.size());
		assertAddDI(date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
		assertDeleteDI(di, false, actions.get(1));
	}

	@Test
	public void testDetermineSynchronizeActionsForDIsUneDIAvecTacheAnnulationIncorrecte() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
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
	public void testDetermineSynchronizeActionsForDIsUneDIAvecTacheAnnulationCorrecte() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable arrivé de hors-Suisse dans l'année
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration qui est incorrecte car le type de contribuable/document est faux
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modele);

		// une tâche d'annulation de la déclaration (correcte car le type de contribuable de la déclaration ne correspond pas à la période d'imposition)
		final TacheAnnulationDeclarationImpot tache = addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, RegDate.get(), di, pp, cedi);

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
	public void testDetermineSynchronizeActionsForDIsUneDISansForFiscal() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une déclaration émise seulement
		final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);
		addEtatDeclaration(di, date(anneePrecedente + 1, 1, 15), TypeEtatDeclaration.EMISE);

		// On vérifie que la déclaration est bien annulée
		hibernateTemplate.flush();
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertDeleteDI(di, true, actions.get(0)); // direct=true -> parce que la déclaration est seulement émise
	}

	@Test
	public void testDetermineSynchronizeActionsForDIsUneDISansForFiscalAvecTacheAnnulation() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
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
	public void testDetermineSynchronizeActionsForDIsUneDIDejaAnnuleeSansForFiscalAvecTacheAnnulation() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
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
	public void testDetermineSynchronizeActionsForDIsDeuxDIsQuiSeChevauchent() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année, avec deux DIs se chevauchant pour cette période
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
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
	public void testDetermineSynchronizeActionsForDIsDeuxDIsQuiSeChevauchentEtTypeContribuableIncompatible() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année, avec deux DIs se chevauchant pour cette période
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		// une première déclaration qui couvre toute l'année (incorrect) avec un type de contribuable HS (incorrect aussi)
		final DeclarationImpotOrdinaire di0 = addDeclarationImpot(pp, periode, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31), TypeContribuable.HORS_SUISSE, modele);
		// une seconde déclaration qui couvre la période en Suisse (correct) avec un type de contribuable dépense (incorrect)
		final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, periode, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modele);

		// On vérifie que :
		// - la première déclaration est mise-à-jour (dates + type)
		// - la deuxième déclaration est annulée (malgré le fait que les dates correspondnt parfaitement : parce le type de contribuable n'est pas compatible avec le type théorique)
		final List<SynchronizeAction> actions = tacheService.determineSynchronizeActionsForDIs(pp);
		assertNotNull(actions);
		assertEquals(2, actions.size());
		assertUpdateDI(di0, date(anneePrecedente, 3, 1), date(anneePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, actions.get(0));
		assertDeleteDI(di1, false, actions.get(1));
	}

	@Test
	public void testDetermineSynchronizeActionsForDIsDeuxDIsQuiSeChevauchentSansCorrespondanceParfaiteDesDates() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		// Contribuable arrivé de hors-Suisse dans l'année, avec deux DIs se chevauchant pour cette période
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(anneePrecedente, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
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
	public void testDetermineSynchronizeActionsForDIsDeuxDIsSansForFiscalAvecUneTacheAnnulation() throws Exception {

		final int anneePrecedente = RegDate.get().year() - 1;

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		assertNotNull(cedi);

		// Contribuable non-assujetti
		final PersonnePhysique pp = addNonHabitant("Paul", "Ogne", date(1954, 11, 23), Sexe.MASCULIN);

		final PeriodeFiscale periode = addPeriodeFiscale(anneePrecedente);
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
		assertSame(di, update.declaration);
		assertEquals(debut, update.periodeImposition.getDateDebut());
		assertEquals(fin, update.periodeImposition.getDateFin());
		assertEquals(typeContribuable, update.periodeImposition.getTypeContribuable());
	}

	private static void assertDeleteDI(DeclarationImpotOrdinaire di, boolean direct, SynchronizeAction action) {
		assertNotNull(action);
		assertInstanceOf(DeleteDI.class, action);

		final DeleteDI delete = (DeleteDI) action;
		assertSame(di, delete.declaration);
		assertEquals(direct, delete.directAnnulation);
	}

	private static void assertAnnuleTache(Tache tache, SynchronizeAction action) {
		assertNotNull(action);
		assertInstanceOf(AnnuleTache.class, action);

		final AnnuleTache annule = (AnnuleTache) action;
		assertSame(tache, annule.tache);
	}

	private List<Tache> genereChangementImposition(ModeImposition ancienMode, ModeImposition nouveauMode) {
		final PersonnePhysique pp = addNonHabitant("Léo", "Bidule", date(1960, 1, 1), Sexe.MASCULIN);
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		ffp.setModeImposition(ancienMode);

		if (ancienMode != ModeImposition.SOURCE) {
			final int anneeCourante = RegDate.get().year();
			final TypeDocument typeD = (ancienMode == ModeImposition.DEPENSE ? TypeDocument.DECLARATION_IMPOT_DEPENSE : TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
			final TypeContribuable typeC = (ancienMode == ModeImposition.DEPENSE ? TypeContribuable.VAUDOIS_DEPENSE : TypeContribuable.VAUDOIS_ORDINAIRE);
			for (int i = 2003; i < anneeCourante; i++) {
				PeriodeFiscale periode = addPeriodeFiscale(i);
				ModeleDocument modele = addModeleDocument(typeD, periode);
				addDeclarationImpot(pp, periode, date(i, 1, 1), date(i, 12, 31), typeC, modele);
			}
		}

		final RegDate dateChangement = date(2006, 1, 1);
		final ForFiscalPrincipal nffp = tiersService.changeModeImposition(pp, dateChangement, nouveauMode, MotifFor.CHGT_MODE_IMPOSITION);
		assertNotNull(nffp);
		assertEquals(nouveauMode, nffp.getModeImposition());
		assertEquals(dateChangement, nffp.getDateDebut());

		return tacheDAO.find(pp.getNumero());
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
}

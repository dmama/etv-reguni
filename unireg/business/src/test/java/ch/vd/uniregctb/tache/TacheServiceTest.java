package ch.vd.uniregctb.tache;

import ch.vd.uniregctb.common.AbstractSpringTest;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;

/**
 * Classe de tests pour TacheService
 *
 * @author xcifde
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TacheServiceTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(TacheServiceTest.class);

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
				addForPrincipal(pp, date(1970, 9, 21), MotifFor.ARRIVEE_HC, MockCommune.Leysin);

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
				addForPrincipal(pp, date(1970, 9, 21), MotifFor.ARRIVEE_HC, MockCommune.Leysin);

				addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);


				return pp.getNumero();
			}
		});

		final CollectiviteAdministrative aci = (CollectiviteAdministrative) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return  tiersService.getOrCreateCollectiviteAdministrative(serviceInfra.getACI().getNoColAdm());

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
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,TypeAdresseRetour.ACI,aci, tacheEnvoi);

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
		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2004, 6, 12), RegDate.get(2006, 6, 12),
				new Integer(5652), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		forFiscalPrincipal.setMotifFermeture(MotifFor.DEPART_HS);

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

				PeriodeFiscale pf1998 = addPeriodeFiscale(1998);
				PeriodeFiscale pf1999 = addPeriodeFiscale(1999);
				PeriodeFiscale pf2000 = addPeriodeFiscale(2000);
				PeriodeFiscale pf2001 = addPeriodeFiscale(2001);
				PeriodeFiscale pf2002 = addPeriodeFiscale(2002);
				PeriodeFiscale pf2003 = addPeriodeFiscale(2003);
				PeriodeFiscale pf2004 = addPeriodeFiscale(2004);
				PeriodeFiscale pf2005 = addPeriodeFiscale(2005);
				PeriodeFiscale pf2006 = addPeriodeFiscale(2006);
				PeriodeFiscale pf2007 = addPeriodeFiscale(2007);
				PeriodeFiscale pf2008 = addPeriodeFiscale(2008);
				PeriodeFiscale pf2009 = addPeriodeFiscale(2009);
				ModeleDocument modele1998 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf1998);
				addModeleFeuilleDocument("Déclaration", "210", modele1998);
				addModeleFeuilleDocument("Annexe 1", "220", modele1998);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele1998);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele1998);
				ModeleDocument modele1999 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf1999);
				addModeleFeuilleDocument("Déclaration", "210", modele1999);
				addModeleFeuilleDocument("Annexe 1", "220", modele1999);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele1999);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele1999);
				ModeleDocument modele2000 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2000);
				addModeleFeuilleDocument("Déclaration", "210", modele2000);
				addModeleFeuilleDocument("Annexe 1", "220", modele2000);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2000);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2000);
				ModeleDocument modele2001 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2001);
				addModeleFeuilleDocument("Déclaration", "210", modele2001);
				addModeleFeuilleDocument("Annexe 1", "220", modele2001);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2001);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2001);
				ModeleDocument modele2002 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2002);
				addModeleFeuilleDocument("Déclaration", "210", modele2002);
				addModeleFeuilleDocument("Annexe 1", "220", modele2002);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele2002);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele2002);
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

				addDeclarationImpot(raoul, pf1998, date(1998, 1, 1), date(1998, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele1998);
				addDeclarationImpot(raoul, pf1999, date(1999, 1, 1), date(1999, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele1999);
				addDeclarationImpot(raoul, pf2000, date(2000, 1, 1), date(2000, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2000);
				addDeclarationImpot(raoul, pf2001, date(2001, 1, 1), date(2001, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2001);
				addDeclarationImpot(raoul, pf2002, date(2002, 1, 1), date(2002, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2002);
				addDeclarationImpot(raoul, pf2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);
				addDeclarationImpot(raoul, pf2004, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2004);
				addDeclarationImpot(raoul, pf2005, date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2005);
				addDeclarationImpot(raoul, pf2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
				addDeclarationImpot(raoul, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);

				// Un autre contribuable vaudois depuis 1998 avec des DIs jusqu'en 2009
				Contribuable jeanDaniel = addNonHabitant("Jean-Daniel", "Lavanchy", date(1962, 10, 4), Sexe.MASCULIN);
				ids.jeanDanielId = jeanDaniel.getNumero();
				addForPrincipal(jeanDaniel, date(1998, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

				addDeclarationImpot(jeanDaniel, pf1998, date(1998, 1, 1), date(1998, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele1998);
				addDeclarationImpot(jeanDaniel, pf1999, date(1999, 1, 1), date(1999, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele1999);
				addDeclarationImpot(jeanDaniel, pf2000, date(2000, 1, 1), date(2000, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2000);
				addDeclarationImpot(jeanDaniel, pf2001, date(2001, 1, 1), date(2001, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2001);
				addDeclarationImpot(jeanDaniel, pf2002, date(2002, 1, 1), date(2002, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2002);
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

			// il doit y avoir 3 tâches d'annulation de DIs
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			taches = tacheDAO.find(criterion);
			assertNotNull(taches);
			assertEquals(2, taches.size()); // 2006 et 2007
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

	@Test
	public void testGenereTacheDepartHCDepuisFermetureForPrincipal() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		PersonnePhysique hab = (PersonnePhysique) tiersService.getTiers(12300001);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2004, 6, 12), RegDate.get(2006, 6, 12),
				new Integer(5652), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		forFiscalPrincipal.setMotifFermeture(MotifFor.DEPART_HC);

		tacheService.genereTacheDepuisFermetureForPrincipal(hab, forFiscalPrincipal);

		TacheCriteria criterion = new TacheCriteria();
		verifieControleDossier(criterion, 0);

		verifieTachesAnnulation(criterion, 0, false);

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

	@Test
	public void testGenereTacheDepartHSDepuisFermetureForSecondaire() throws Exception {
		testClotureDuDernierForSecondaire(MotifFor.DEPART_HS);
	}

	/**
	 * [UNIREG-1110] Tâches en instance : fermeture du dernier for secondaire pour un HS / HC dans une période échue Les taches d'annulation de DI n'étaient pas générées si le motif de départ été
	 * different de depart HS ou HC hors elles doivent l'être si le for secondaire est le dernier actif.
	 */
	@Test
	public void testClotureDuDernierForSecondaireUNIREG1110() throws Exception {
		testClotureDuDernierForSecondaire(MotifFor.FIN_EXPLOITATION);
	}

	private void testClotureDuDernierForSecondaire(MotifFor motifFermeture) throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		PersonnePhysique hab = (PersonnePhysique) tiersService.getTiers(12300001);

		ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal(RegDate.get(2005, 6, 12), null,
				new Integer(8201), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		ForFiscalSecondaire forFiscalSecondaire = new ForFiscalSecondaire(RegDate.get(2005, 6, 12), RegDate.get(2006, 6, 11), new Integer(
				5652), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ACTIVITE_INDEPENDANTE);

		hab.addForFiscal(forFiscalPrincipal);
		forFiscalSecondaire.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
		forFiscalSecondaire.setMotifFermeture(motifFermeture);
		hab.addForFiscal(forFiscalSecondaire);

		tacheService.genereTacheDepuisFermetureForSecondaire(hab, forFiscalSecondaire);

		TacheCriteria criterion = new TacheCriteria();
		verifieControleDossier(criterion);
		verifieTachesAnnulation(criterion, 1, false);

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

				// Contribuable français
				Contribuable raoul = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
				addForPrincipal(raoul, date(1990, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockPays.France);
				ids.raoulId = raoul.getNumero();

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

		// fin d'activité indépendante ctb HS -> émission du tâches d'émission de DI du 1er janvier à la date de fin
		final List<Tache> taches = tacheDAO.getAll();
		assertNotNull(taches);
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
			LOGGER.warn("Vérification année " + i);
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
		}

		// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable (ménage)
		// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
		for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
			LOGGER.warn("Vérification année " + i);
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

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage);
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
				LOGGER.warn("Vérification année " + i);
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable
			// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
			for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
				LOGGER.warn("Vérification année " + i);
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
				LOGGER.warn("Vérification année " + i);
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable
			// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
			for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
				LOGGER.warn("Vérification année " + i);
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

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage);
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
				LOGGER.warn("Vérification année " + i);
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable
			// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
			for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
				LOGGER.warn("Vérification année " + i);
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
				LOGGER.warn("Vérification année " + i);
				assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(i, 1, 1), date(i, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, envois.get(i - 2007));
			}

			// après les deux premières années d'assujettissement, on ne trouve pas de DI et on ne considère pas le contribuable
			// comme nouvel assujetti (n-1 et n-2 ont tous les deux un assujettissement ordinaire), donc COMPLETE
			for (int i = 2009 ; i < RegDate.get().year() ; ++ i) {
				LOGGER.warn("Vérification année " + i);
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

				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage);
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
	 * [UNIREG-1327] Vérifie que la tâche générée par l'arrivée de HS tardive (= événement traité en 2009 pour une arrivée en 2008, par exemple) d'un contribuable qui possède déjà un immeuble est
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
				addForSecondaire(simon, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);
				return null;
			}
		});

		// Arrivée de hors-Suisse traitée tardivement
		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.simonId);
		assertNotNull(raoul);
		tiersService.closeForFiscalPrincipal(raoul, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS);
		tiersService.openForFiscalPrincipal(raoul, dateArrivee, MotifRattachement.DOMICILE, MockCommune.Lausanne.getNoOFS(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE, MotifFor.ARRIVEE_HS, true);

		// Arrivée de hors-Suisse tardive -> une tache d'émission de DIs pour toute l'année 2005 doit être genérée (+ une pour toutes les
		// autres années échues)
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		final List<Tache> taches = tacheDAO.find(criterion);
		assertEquals(RegDate.get().year() - 2005, taches.size());
		assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, null, (TacheEnvoiDeclarationImpot) taches.get(0));

		for (int annee = 2006; annee < RegDate.get().year(); annee++) {
			assertTache(TypeEtatTache.EN_INSTANCE, nextSunday, date(annee, 1, 1), date(annee, 12, 31),
					TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, null, (TacheEnvoiDeclarationImpot) taches
							.get(annee - 2005));
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
				PeriodeFiscale pf2008 = addPeriodeFiscale(2008);
				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2008);
				addDeclarationImpot(raoul, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele).getId();
				addForPrincipal(raoul, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				return null;
			}
		});

		final Contribuable raoul = (Contribuable) tiersService.getTiers(ids.raoulId);
		assertNotNull(raoul);
		tiersService.closeForFiscalPrincipal(raoul, date(2008, 11, 1), MotifFor.VEUVAGE_DECES);
		Declaration di2008 = raoul.getDeclarationActive(date(2008, 1, 1));
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
				Contribuable raoul = addHabitant(100000);
				ids.raoulId = raoul.getNumero();
				addForPrincipal(raoul, date(1980, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

				// une tâche d'annulation de DI
				PeriodeFiscale periode = addPeriodeFiscale(2074);
				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				DeclarationImpotOrdinaire declaration = addDeclarationImpot(raoul, periode, date(2074, 1, 1), date(2074, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				final TacheAnnulationDeclarationImpot annulDI = addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, date(2000, 1, 1), declaration,
						raoul);
				ids.tacheAnnulDI = annulDI.getId();

				// tâche de contrôle de dossier
				final TacheControleDossier tacheControl = addTacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2000, 1, 1), raoul);
				ids.tacheControl = tacheControl.getId();

				// tâche d'envoi de DI
				final TacheEnvoiDeclarationImpot tacheEnvoi = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2000, 1, 1),
						date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						raoul, null);
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
		assertEquals(0, countTaches(TypeTache.TacheEnvoiDeclarationImpot, taches));
	}

	private List<Tache> genereChangementImposition(ModeImposition ancienMode, ModeImposition nouveauMode) {
		PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNom("Bidule");
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
		ffp.setModeImposition(ancienMode);
		pp = (PersonnePhysique) ffp.getTiers();

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

package ch.vd.unireg.norentes.common;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AbstractBusinessTest;
import ch.vd.unireg.common.RequiresNewTransactionDefinition;
import ch.vd.unireg.database.DatabaseService;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.etiquette.ActionAutoEtiquette;
import ch.vd.unireg.etiquette.CorrectionSurDate;
import ch.vd.unireg.etiquette.Decalage;
import ch.vd.unireg.etiquette.DecalageAvecCorrection;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.etiquette.UniteDecalageDate;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.norentes.annotation.AfterCheck;
import ch.vd.unireg.norentes.annotation.AfterEtape;
import ch.vd.unireg.norentes.annotation.BeforeCheck;
import ch.vd.unireg.norentes.annotation.BeforeEtape;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.SituationFamillePersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TarifImpotSource;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeTiersEtiquette;

public abstract class EvenementScenario extends NorentesScenario {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementScenario.class);

	protected GlobalTiersSearcher globalSearcher;
	protected GlobalTiersIndexer globalIndexer;

	protected PlatformTransactionManager transactionManager;
	protected DatabaseService databaseService;
	protected TiersDAO tiersDAO;
	protected TiersService tiersService;
	protected PeriodeFiscaleDAO periodeFiscaleDAO;
	protected ModeleDocumentDAO modeleDocumentDAO;
	private SessionFactory sessionFactory;

	private TransactionStatus tx;

	public static final String CODE_ETIQUETTE_HERITAGE = AbstractBusinessTest.CODE_ETIQUETTE_HERITAGE;
	public static final String CODE_ETIQUETTE_COLLABORATEUR = AbstractBusinessTest.CODE_ETIQUETTE_COLLABORATEUR;

	@Override
	public void onInitialize() {
		super.onInitialize();

		truncateDatabase();
		globalIndexer.indexAllDatabase();

		createAllPfs();
		createAllCollectivitesAdministratives();
	}

	@Override
	public void onFinalize()  {
		super.onFinalize();
	}

	@BeforeEtape
	@BeforeCheck
	public void beforeEtape() throws Exception {
		startNewTransaction();
		final Session session = sessionFactory.getCurrentSession();
		if (session != null) {
			session.clear();
		}
	}


	@AfterEtape
	@AfterCheck
	public void afterEtape() throws Exception {
		endTransaction();
	}

	/**
	 * @throws Exception
	 */
	protected void indexData() throws Exception {
		LOGGER.debug("Indexation de la base de données...");
		globalIndexer.indexAllDatabase(Mode.FULL, 1, null);
		LOGGER.debug("Indexation de la base de données terminée. Nombre de docs: " + globalSearcher.getApproxDocCount());
	}

	protected void truncateDatabase() {
		try {
			databaseService.truncateDatabase();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void createAllPfs() {
		final int firstPf = 2003;
		final int lastPf = RegDate.get().year();

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			for (int pf = firstPf; pf <= lastPf; ++pf) {
				final PeriodeFiscale periode = new PeriodeFiscale();
				periode.setAnnee(pf);
				periode.addAllPeriodeFiscaleParametresPP(RegDate.get(pf + 1, 1, 31), RegDate.get(pf + 1, 3, 31), RegDate.get(pf + 1, 6, 30));
				periodeFiscaleDAO.save(periode);
			}
			return null;
		});
	}

	private void createAllCollectivitesAdministratives() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			final Session currentSession = sessionFactory.getCurrentSession();
			for (MockCollectiviteAdministrative collAdm : MockCollectiviteAdministrative.getAll()) {
				final CollectiviteAdministrative ca = new CollectiviteAdministrative();
				if (collAdm instanceof MockOfficeImpot) {
					ca.setIdentifiantDistrictFiscal(((MockOfficeImpot) collAdm).getIdentifiantDistrict());
					ca.setIdentifiantRegionFiscale(((MockOfficeImpot) collAdm).getIdentifiantRegion());
				}
				ca.setNumeroCollectiviteAdministrative(collAdm.getNoColAdm());
				final CollectiviteAdministrative savedCA = (CollectiviteAdministrative) currentSession.merge(ca);

				// [SIFISC-20149] on va également créer les étiquettes qui vont bien si la bonne collectivité adminstrative est là
				if (collAdm.getNoColAdm() == MockCollectiviteAdministrative.noNouvelleEntite) {
					final Etiquette heritage = new Etiquette(CODE_ETIQUETTE_HERITAGE, "Héritage", true, TypeTiersEtiquette.PP, savedCA);
					heritage.setActionSurDeces(new ActionAutoEtiquette(new Decalage(1, UniteDecalageDate.JOUR),
					                                                   new DecalageAvecCorrection(2, UniteDecalageDate.ANNEE, CorrectionSurDate.FIN_ANNEE)));
					currentSession.merge(heritage);

					final Etiquette collaborateur = new Etiquette(CODE_ETIQUETTE_COLLABORATEUR, "DS Collaborateur", true, TypeTiersEtiquette.PP, savedCA);
					currentSession.merge(collaborateur);
				}

			}
			return null;
		});
	}

	protected ForFiscalPrincipalPP addForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques tiers, Commune commune, RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin) {
		return addForFiscalPrincipal(tiers, commune, debut, fin, motifDebut, motifFin, ModeImposition.ORDINAIRE);
	}

	protected ForFiscalPrincipalPP addForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques tiers, Commune commune, RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin, ModeImposition modeImposition) {
		final TypeAutoriteFiscale typeAutoriteFiscale = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		return addForFiscalPrincipal(tiers, commune.getNoOFS(), typeAutoriteFiscale, debut, fin, motifDebut, motifFin, modeImposition);
	}

	protected ForFiscalPrincipalPP addForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques tiers, Pays pays, RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin) {
		Assert.assertNotNull(pays);
		Assert.assertFalse(pays.isSuisse());
		return addForFiscalPrincipal(tiers, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, debut, fin, motifDebut, motifFin, ModeImposition.ORDINAIRE);
	}

	protected ForFiscalPrincipalPP addForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques tiers, Pays pays, RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin,
	                                                     ModeImposition modeImposition) {
		Assert.assertNotNull(pays);
		Assert.assertFalse(pays.isSuisse());
		return addForFiscalPrincipal(tiers, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, debut, fin, motifDebut, motifFin, modeImposition);
	}

	private ForFiscalPrincipalPP addForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques tiers, int noOFS, TypeAutoriteFiscale typeAutoriteFiscale, RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin, ModeImposition modeImposition) {
		final ForFiscalPrincipalPP ffp = new ForFiscalPrincipalPP();
		ffp.setDateDebut(debut);
		ffp.setDateFin(fin);
		ffp.setMotifOuverture(motifDebut);
		ffp.setMotifFermeture(motifFin);
		ffp.setTypeAutoriteFiscale(typeAutoriteFiscale);
		ffp.setNumeroOfsAutoriteFiscale(noOFS);
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setModeImposition(modeImposition);

		return tiersDAO.addAndSave(tiers, ffp);
	}

	protected ForFiscalSecondaire addForFiscalSecondaire(Tiers tiers, int noOFS, RegDate debut, RegDate fin){
		ForFiscalSecondaire forSec = new ForFiscalSecondaire();
		forSec.setDateDebut(debut);
		forSec.setDateFin(fin);
		forSec.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		forSec.setNumeroOfsAutoriteFiscale(noOFS);
		forSec.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		forSec.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
		forSec.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);

		return tiersDAO.addAndSave(tiers, forSec);
	}

	protected DeclarationImpotOrdinaire addDeclarationImpot(Tiers tiers, RegDate debut, RegDate fin, RegDate dateEmission, int delaiEnJours) {
		assertEquals(debut.year(), fin.year(), "Début et fin devraient être sur la même période fiscale");

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.setDateDebut(debut);
		di.setDateFin(fin);
		di.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);

		final EtatDeclaration etat = new EtatDeclarationEmise();
		etat.setDateObtention(dateEmission);
		etat.setDeclaration(di);
		final Set<EtatDeclaration> etats = new HashSet<>(1);
		etats.add(etat);
		di.setEtatsDeclaration(etats);

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDateDemande(dateEmission);
		delai.setDeclaration(di);
		delai.setDelaiAccordeAu(dateEmission.addDays(delaiEnJours));
		final Set<DelaiDeclaration> delais = new HashSet<>(1);
		di.setDelaisDeclaration(delais);

		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(debut.year());
		di.setPeriode(periode);

		ModeleDocument modeleDocument = new ModeleDocument();
		modeleDocument.setPeriodeFiscale(periode);
		modeleDocument.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		modeleDocument = modeleDocumentDAO.save(modeleDocument);
//		modeleDocument.setModelesFeuilleDocument(theModelesFeuilleDocument)
		di.setModeleDocument(modeleDocument);

		tiers.addDeclaration(di);
		return di;
	}

	protected CollectiviteAdministrative addColAdm(MockCollectiviteAdministrative ca) {
		CollectiviteAdministrative coll = new CollectiviteAdministrative();
		coll.setNumeroCollectiviteAdministrative(ca.getNoColAdm());
		return (CollectiviteAdministrative) tiersDAO.save(coll);
	}

	protected PersonnePhysique addHabitant(long noIndividu) {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(noIndividu);

		return (PersonnePhysique) tiersDAO.save(hab);
	}

	protected PersonnePhysique addNonHabitant(final String nom, final String prenom, RegDate dateNaissance, Sexe sexe) {
		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNom(nom);
		nonHab.setPrenomUsuel(prenom);
		nonHab.setDateNaissance(dateNaissance);
		nonHab.setSexe(sexe);

		return (PersonnePhysique) tiersDAO.save(nonHab);
	}

	protected void addSituationFamille(final PersonnePhysique personne, RegDate debut, RegDate fin, EtatCivil etatCivil,
			int nbEnfants) {
		SituationFamille sit = new SituationFamillePersonnePhysique();
		sit.setDateDebut(debut);
		sit.setDateFin(fin);
		sit.setEtatCivil(etatCivil);
		sit.setNombreEnfants(nbEnfants);

		tiersDAO.addAndSave(personne, sit);
	}

	protected void addSituationFamille(final MenageCommun menage, RegDate debut, RegDate fin, EtatCivil etatCivil,
			int nbEnfants, TarifImpotSource tarifApplicable, Contribuable contribuablePrincipal) {
		Assert.assertTrue(EtatCivil.MARIE == etatCivil || EtatCivil.LIE_PARTENARIAT_ENREGISTRE == etatCivil);
		SituationFamilleMenageCommun sit = new SituationFamilleMenageCommun();
		sit.setDateDebut(debut);
		sit.setDateFin(fin);
		sit.setNombreEnfants(nbEnfants);
		sit.setTarifApplicable(tarifApplicable);
		sit.setContribuablePrincipalId(contribuablePrincipal == null ? null : contribuablePrincipal.getId());
		sit.setEtatCivil(etatCivil);

		tiersDAO.addAndSave(menage, sit);
	}

	protected void startNewTransaction() throws TransactionException {
		RequiresNewTransactionDefinition def = new RequiresNewTransactionDefinition();
		tx = transactionManager.getTransaction(def);
	}

	/**
	 * Uniquement pour mettre des breakpoints
	 */
	protected void endTransaction() {
		Assert.assertNotNull(tx);
		transactionManager.commit(tx);
		tx = null;
	}

	protected void commitAndStartTransaction() {
		endTransaction();
		startNewTransaction();
	}


	public void setGlobalSearcher(GlobalTiersSearcher globalSearcher) {
		this.globalSearcher = globalSearcher;
	}

	public void setGlobalIndexer(GlobalTiersIndexer globalIndexer) {
		this.globalIndexer = globalIndexer;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setDatabaseService(DatabaseService databaseService) {
		this.databaseService = databaseService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}

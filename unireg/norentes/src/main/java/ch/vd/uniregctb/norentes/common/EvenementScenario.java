package ch.vd.uniregctb.norentes.common;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import annotation.AfterCheck;
import annotation.AfterEtape;
import annotation.BeforeCheck;
import annotation.BeforeEtape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.RequiresNewTransactionDefinition;
import ch.vd.uniregctb.database.DatabaseService;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public abstract class EvenementScenario extends NorentesScenario {

	private static final Logger LOGGER = Logger.getLogger(EvenementScenario.class);

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


	@Override
	public void onInitialize() {
		super.onInitialize();

		truncateDatabase();
		globalIndexer.indexAllDatabase();
	}

	@Override
	public void onFinalize()  {
		super.onFinalize();
	}

	@BeforeEtape
	@BeforeCheck
	public void beforeEtape() throws Exception {
		startNewTransaction();
		Session session = SessionFactoryUtils.getSession(sessionFactory, false);
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
		globalIndexer.indexAllDatabaseAsync(null, 1, Mode.FULL, false);
		LOGGER.debug("Indexation de la base de données terminée. Nombre de docs: "+globalIndexer.getApproxDocCount());
	}

	protected void truncateDatabase() {
		try {
			databaseService.truncateDatabase();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ForFiscalPrincipal addForFiscalPrincipal(Tiers tiers, Commune commune, RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin) {
		final TypeAutoriteFiscale typeAutoriteFiscale = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		return addForFiscalPrincipal(tiers, commune.getNoOFSEtendu(), typeAutoriteFiscale, debut, fin, motifDebut, motifFin);
	}

	protected ForFiscalPrincipal addForFiscalprincipal(Tiers tiers, Pays pays, RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin) {
		Assert.notNull(pays);
		Assert.isFalse(pays.isSuisse());
		return addForFiscalPrincipal(tiers, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, debut, fin, motifDebut, motifFin);
	}

	private ForFiscalPrincipal addForFiscalPrincipal(Tiers tiers, int noOFS, TypeAutoriteFiscale typeAutoriteFiscale, RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin) {
		ForFiscalPrincipal ffp = new ForFiscalPrincipal();
		ffp.setDateDebut(debut);
		ffp.setDateFin(fin);
		ffp.setMotifOuverture(motifDebut);
		ffp.setMotifFermeture(motifFin);
		ffp.setTypeAutoriteFiscale(typeAutoriteFiscale);
		ffp.setNumeroOfsAutoriteFiscale(noOFS);
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setModeImposition(ModeImposition.ORDINAIRE);

		return (ForFiscalPrincipal) tiersService.addAndSave(tiers, ffp);
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

		return (ForFiscalSecondaire) tiersService.addAndSave(tiers, forSec);
	}

	protected DeclarationImpotOrdinaire addDeclarationImpot(Tiers tiers, RegDate debut, RegDate fin, RegDate dateEmission, int delaiEnJours) {
		assertEquals(debut.year(), fin.year(), "Début et fin devraient être sur la même période fiscale");

		final DeclarationImpotOrdinaire di = new DeclarationImpotOrdinaire();
		di.setDateDebut(debut);
		di.setDateFin(fin);

		final EtatDeclaration etat = new EtatDeclaration();
		etat.setDateObtention(dateEmission);
		etat.setEtat(TypeEtatDeclaration.EMISE);
		etat.setDeclaration(di);
		final Set<EtatDeclaration> etats = new HashSet<EtatDeclaration>(1);
		etats.add(etat);
		di.setEtats(etats);

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateDemande(dateEmission);
		delai.setDeclaration(di);
		delai.setDelaiAccordeAu(dateEmission.addDays(delaiEnJours));
		final Set<DelaiDeclaration> delais = new HashSet<DelaiDeclaration>(1);
		di.setDelais(delais);

		PeriodeFiscale periode = new PeriodeFiscale();
		periode.setAnnee(debut.year());
		periode = periodeFiscaleDAO.save(periode);
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

	protected PersonnePhysique addNonHabitant(final String nom, final String prenom, RegDate dateNaissance) {
		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNom(nom);
		nonHab.setPrenom(prenom);
		nonHab.setDateNaissance(dateNaissance);

		return (PersonnePhysique) tiersDAO.save(nonHab);
	}

	protected void addSituationFamille(final PersonnePhysique personne, RegDate debut, RegDate fin, EtatCivil etatCivil,
			int nbEnfants) {
		SituationFamille sit = new SituationFamille();
		sit.setDateDebut(debut);
		sit.setDateFin(fin);
		sit.setEtatCivil(etatCivil);
		sit.setNombreEnfants(nbEnfants);

		tiersService.addAndSave(personne, sit);
	}

	protected void addSituationFamille(final MenageCommun menage, RegDate debut, RegDate fin, EtatCivil etatCivil,
			int nbEnfants, TarifImpotSource tarifApplicable, Contribuable contribuablePrincipal) {
		Assert.isTrue(EtatCivil.MARIE.equals(etatCivil) || EtatCivil.LIE_PARTENARIAT_ENREGISTRE.equals(etatCivil));
		SituationFamilleMenageCommun sit = new SituationFamilleMenageCommun();
		sit.setDateDebut(debut);
		sit.setDateFin(fin);
		sit.setNombreEnfants(nbEnfants);
		sit.setTarifApplicable(tarifApplicable);
		sit.setContribuablePrincipal(contribuablePrincipal);
		sit.setEtatCivil(etatCivil);

		tiersService.addAndSave(menage, sit);
	}

	protected void startNewTransaction() throws TransactionException {
		RequiresNewTransactionDefinition def = new RequiresNewTransactionDefinition();
		tx = transactionManager.getTransaction(def);
	}

	/**
	 * Uniquement pour mettre des breakpoints
	 *
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#startNewTransaction()
	 */
	protected void endTransaction() {
		Assert.notNull(tx);
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

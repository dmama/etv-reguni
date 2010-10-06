package ch.vd.uniregctb.common;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Assert;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tache.TacheSynchronizerInterceptor;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

// Surcharge des fichiers de config Spring. Il faut mettre les fichiers
// UT a la fin
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CACHE,
		BusinessTestingConstants.UNIREG_BUSINESS_INTERFACES,
		BusinessTestingConstants.UNIREG_BUSINESS_ESSENTIALS,
		BusinessTestingConstants.UNIREG_BUSINESS_SERVICES,
		BusinessTestingConstants.UNIREG_BUSINESS_JOBS,
		BusinessTestingConstants.UNIREG_BUSINESS_EVT_CIVIL,
		BusinessTestingConstants.UNIREG_BUSINESS_EVT_FISCAL,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_EDITIQUE,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_INTERFACES,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_JMS,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_SERVICES,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CLIENT_WEBSERVICE,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CONFIG
})
public abstract class AbstractBusinessTest extends AbstractCoreDAOTest {

	// private static final Logger LOGGER = Logger.getLogger(AbstractBusinessTest.class);

	private boolean wantIndexation = false;
	private boolean wantSynchroTache = false;
	protected TiersService tiersService;
	protected GlobalTiersIndexer globalTiersIndexer;
	protected GlobalTiersSearcher globalTiersSearcher;
	private TacheSynchronizerInterceptor tacheSynchronizer;

	@Override
	protected void runOnSetUp() throws Exception {
		tiersService = getBean(TiersService.class, "tiersService");
		globalTiersIndexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
		globalTiersSearcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		globalTiersIndexer.setOnTheFlyIndexation(wantIndexation);
		tacheSynchronizer = getBean(TacheSynchronizerInterceptor.class, "tacheSynchronizerInterceptor");
		tacheSynchronizer.setOnTheFlySynchronization(wantSynchroTache);
		super.runOnSetUp();
	}

	@Override
	protected void truncateDatabase() throws Exception {
		super.truncateDatabase();

		if (wantIndexation) {
			removeIndexData();
		}
	}

	@Override
	protected void loadDatabase(String filename) throws Exception {
		super.loadDatabase(filename);

		if (wantIndexation) {
			indexData();
		}
	}

	public void setWantIndexation(boolean wantIndexation) {
		this.wantIndexation = wantIndexation;
		
		if (globalTiersIndexer != null) {
			globalTiersIndexer.setOnTheFlyIndexation(wantIndexation);
		}
	}

	public void setWantSynchroTache(boolean wantSynchroTache) {
		this.wantSynchroTache = wantSynchroTache;

		if (tacheSynchronizer != null) {
			tacheSynchronizer.setOnTheFlySynchronization(wantSynchroTache);
		}
	}

	protected void indexData() throws Exception {
		// globalTiersIndexer.indexAllDatabase();
		// Si on Index en ASYNC (on créée des Threads) tout va bien
		// Sinon, avec indexAllDb(), il y a des problemes de OptimisticLock...
		globalTiersIndexer.indexAllDatabaseAsync(null, 1, GlobalTiersIndexer.Mode.FULL, false);
	}

	protected void removeIndexData() throws Exception {
		globalTiersIndexer.overwriteIndex();
	}

	protected abstract class TestHibernateCallback implements HibernateCallback {
		public abstract Object testInHibernate(Session session) throws Exception;

		public final Object doInHibernate(Session session) throws HibernateException, SQLException {
			try {
				return testInHibernate(session);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected Object doInTransactionAndSession(final TransactionCallback action) throws Exception {
		return doInTransaction(new TransactionCallback() {
			public Object doInTransaction(final TransactionStatus status) {
				return hibernateTemplate.executeWithNewSession(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						return action.doInTransaction(status);
					}
				});
			}
		});
	}

	protected Object doInNewTransactionAndSession(final TransactionCallback action) throws Exception {
		return doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(final TransactionStatus status) {
				return hibernateTemplate.executeWithNewSession(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						return action.doInTransaction(status);
					}
				});
			}
		});
	}

	protected static void assertForAutreImpot(RegDate debut, RegDate fin, TypeAutoriteFiscale taf, MockCommune commune, GenreImpot genreImpot, ForFiscalAutreImpot forFiscal) {
		Assert.assertEquals(debut, forFiscal.getDateDebut());
		Assert.assertEquals(fin, forFiscal.getDateFin());
		Assert.assertEquals(taf, forFiscal.getTypeAutoriteFiscale());
		Assert.assertEquals(commune.getNoOFSEtendu(), forFiscal.getNumeroOfsAutoriteFiscale().intValue());
		Assert.assertEquals(genreImpot, forFiscal.getGenreImpot());
	}

	protected static void assertForAutreElementImposable(RegDate debut, RegDate fin, TypeAutoriteFiscale taf, MockCommune commune, MotifRattachement rattachement,
	                                            ForFiscalAutreElementImposable forFiscal) {
		Assert.assertEquals(debut, forFiscal.getDateDebut());
		Assert.assertEquals(fin, forFiscal.getDateFin());
		Assert.assertEquals(taf, forFiscal.getTypeAutoriteFiscale());
		Assert.assertEquals(commune.getNoOFSEtendu(), forFiscal.getNumeroOfsAutoriteFiscale().intValue());
		Assert.assertEquals(rattachement, forFiscal.getMotifRattachement());
	}
	
	/**
	 * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockCommune commune) {
		TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune.getNoOFSEtendu(), type, MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockCommune commune, MotifRattachement motifRattachement) {
		return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune, motifRattachement);
	}

	/**
	 * Ajoute un for principal fermé sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockCommune commune) {
		return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal fermé sur une commune Suisse sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockCommune commune, MotifRattachement motifRattachement) {
		final TypeAutoriteFiscale type = (commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
		return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune.getNoOFSEtendu(), type, motifRattachement);
	}

	/**
	 * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockPays pays) {
		return addForPrincipal(contribuable, ouverture, motifOuverture, pays, MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockPays pays, MotifRattachement motifRattachement) {
		assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
		return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, motifRattachement);
	}

	/**
	 * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockPays pays) {
		assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
		return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE);
	}

	protected ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable dpi, RegDate debut, RegDate fin, MockCommune commune) {
		ForDebiteurPrestationImposable f = new ForDebiteurPrestationImposable();
		f.setDateDebut(debut);
		f.setDateFin(fin);
		f.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(commune.getNoOFSEtendu());
		if (dpi.getDernierForDebiteur() == null) {
			tiersService.adaptPremierePeriodicite(dpi, debut);
		}
		f = tiersService.addAndSave(dpi, f);
		return f;
	}

	protected AdresseSuisse addAdresseSuisse(Tiers tiers, TypeAdresseTiers usage, RegDate debut, RegDate fin, MockRue rue) {
		AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setUsage(usage);
		adresse.setNumeroRue(rue.getNoRue());
		adresse.setNumeroOrdrePoste(rue.getLocalite().getNPA());
		adresse = (AdresseSuisse) tiersService.addAndSave(tiers, adresse);
		return adresse;
	}

	protected AdresseEtrangere addAdresseEtrangere(Tiers tiers, TypeAdresseTiers usage, RegDate debut, RegDate fin, String rue, String numeroPostalEtLocalite, Pays pays) {
		AdresseEtrangere adresse = new AdresseEtrangere();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setUsage(usage);
		adresse.setRue(rue);
		adresse.setNumeroPostalLocalite(numeroPostalEtLocalite);
		adresse.setNumeroOfsPays(pays.getNoOFS());
		adresse = (AdresseEtrangere) tiersService.addAndSave(tiers, adresse);
		return adresse;
	}

	protected CollectiviteAdministrative addCollAdm(MockCollectiviteAdministrative oid) {
		CollectiviteAdministrative ca = new CollectiviteAdministrative();
		ca.setNumeroCollectiviteAdministrative(oid.getNoColAdm());
		ca = (CollectiviteAdministrative) hibernateTemplate.merge(ca);
		hibernateTemplate.flush();
		return ca;
	}

	protected DeclarationImpotSource addLR(DebiteurPrestationImposable debiteur, RegDate debut, RegDate fin, PeriodeFiscale periode) {

		return addLR(debiteur, debut, fin, periode,null);
	}
	protected DeclarationImpotSource addLR(DebiteurPrestationImposable debiteur, RegDate debut, RegDate fin, PeriodeFiscale periode, TypeEtatDeclaration typeEtat) {
		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setDateDebut(debut);
		lr.setDateFin(fin);
		lr.setPeriode(periode);
		lr.setModeCommunication(ModeCommunication.PAPIER);
		lr.setPeriodicite(debiteur.getPeriodiciteAt(debut).getPeriodiciteDecompte());

		if(typeEtat!=null){
			EtatDeclaration etat = new EtatDeclaration();
			etat.setEtat(typeEtat);
			etat.setDateObtention(debut);
			lr.addEtat(etat);
		}
		lr.setTiers(debiteur);
		lr = (DeclarationImpotSource) hibernateTemplate.merge(lr);
		debiteur.addDeclaration(lr);
		return lr;
	}

	protected DebiteurPrestationImposable addDebiteur(CategorieImpotSource categorie, PeriodiciteDecompte periodicite, RegDate debutValiditePeriodicite) {
		final DebiteurPrestationImposable debiteur = addDebiteur();
		debiteur.setCategorieImpotSource(categorie);
		final PeriodeDecompte periode = (periodicite == PeriodiciteDecompte.UNIQUE ? PeriodeDecompte.A : null);
		debiteur.addPeriodicite(new Periodicite(periodicite, periode, debutValiditePeriodicite, null));
		return debiteur;
	}

	protected ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable dpi, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutorite, MockCommune commune) {
		ForDebiteurPrestationImposable f = new ForDebiteurPrestationImposable();
		f.setTypeAutoriteFiscale(typeAutorite);
		f.setNumeroOfsAutoriteFiscale(commune.getNoOFSEtendu());
		f.setDateDebut(dateDebut);
		f.setDateFin(dateFin);
		return tiersService.addAndSave(dpi, f);
	}

	protected ForFiscalAutreElementImposable addForAutreElementImposable(Contribuable ctb, RegDate dateDebut, RegDate dateFin, MockCommune commune, TypeAutoriteFiscale taf,
	                                                                     MotifRattachement rattachement) {
		ForFiscalAutreElementImposable f = new ForFiscalAutreElementImposable();
		f.setDateDebut(dateDebut);
		f.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
		f.setDateFin(dateFin);
		if (dateFin != null) {
			f.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
		}
		f.setTypeAutoriteFiscale(taf);
		f.setNumeroOfsAutoriteFiscale(commune.getNoOFSEtendu());
		f.setMotifRattachement(rattachement);
		return tiersService.addAndSave(ctb, f);
	}

	/**
	 * Ajoute une déclaration d'impôt ordinaire sur le contribuable spécifié.
	 */
	protected DeclarationImpotOrdinaire addDeclarationImpot(Contribuable tiers, PeriodeFiscale periode, RegDate debut, RegDate fin,
	                                                        TypeContribuable typeC, ModeleDocument modele) {

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		assertNotNull("La collectivité administrative du CEDI n'a pas été définie", cedi);

		return addDeclarationImpot(tiers, periode, debut, fin, cedi, typeC, modele);
	}

	protected SituationFamille addSituation(PersonnePhysique pp, RegDate debut, RegDate fin, Integer nombreEnfants) {
		SituationFamille situation = new SituationFamillePersonnePhysique();
		situation.setDateDebut(debut);
		situation.setDateFin(fin);
		situation.setNombreEnfants(nombreEnfants);
		return tiersService.addAndSave(pp, situation);
	}

	protected SituationFamilleMenageCommun addSituation(MenageCommun menage, RegDate debut, RegDate fin, int nombreEnfants,
			TarifImpotSource tarif) {
		SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
		situation.setDateDebut(debut);
		situation.setDateFin(fin);
		situation.setNombreEnfants(nombreEnfants);
		situation.setTarifApplicable(tarif);
		return (SituationFamilleMenageCommun) tiersService.addAndSave(menage, situation);
	}

	protected IdentificationPersonne addIdentificationPersonne(PersonnePhysique pp, CategorieIdentifiant categorie, String identifiant) {
		IdentificationPersonne ident = new IdentificationPersonne();
		ident.setCategorieIdentifiant(categorie);
		ident.setIdentifiant(identifiant);
		return tiersService.addAndSave(pp, ident);
	}
}

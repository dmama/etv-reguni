package ch.vd.uniregctb.common;

import java.sql.SQLException;
import java.util.Stack;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationHelper;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.security.SecurityProviderInterface;
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
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.validation.ValidationInterceptor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

// Surcharge des fichiers de config Spring. Il faut mettre les fichiers
// UT a la fin
@ContextConfiguration(locations = {
        BusinessTestingConstants.UNIREG_BUSINESS_UT_CACHE,
        BusinessTestingConstants.UNIREG_BUSINESS_ESSENTIALS,
        BusinessTestingConstants.UNIREG_BUSINESS_SERVICES,
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
    private ValidationInterceptor validationInterceptor;

    @Override
    protected void runOnSetUp() throws Exception {
        tiersService = getBean(TiersService.class, "tiersService");
        globalTiersIndexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
        globalTiersSearcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
        globalTiersIndexer.setOnTheFlyIndexation(wantIndexation);
        tacheSynchronizer = getBean(TacheSynchronizerInterceptor.class, "tacheSynchronizerInterceptor");
        tacheSynchronizer.setOnTheFlySynchronization(wantSynchroTache);
        validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");
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
        globalTiersIndexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.FULL, false, false, false);
    }

    protected void removeIndexData() throws Exception {
        globalTiersIndexer.overwriteIndex();
    }

    protected <T> T doInTransactionAndSession(final TransactionCallback<T> action) throws Exception {
        return doInTransaction(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(final TransactionStatus status) {
                return hibernateTemplate.executeWithNewSession(new HibernateCallback<T>() {
                    @Override
                    public T doInHibernate(Session session) throws HibernateException, SQLException {
                        return action.doInTransaction(status);
                    }
                });
            }
        });
    }

    protected <T> T doInNewTransactionAndSession(final TransactionCallback<T> action) throws Exception {
        return doInNewTransaction(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(final TransactionStatus status) {
                return hibernateTemplate.executeWithNewSession(new HibernateCallback<T>() {
                    @Override
                    public T doInHibernate(Session session) throws HibernateException, SQLException {
                        return action.doInTransaction(status);
                    }
                });
            }
        });
    }

    protected static interface ExecuteCallback<T> {
        T execute() throws Exception;
    }

    /**
     * Exécute une portion de code avec la validation des objets métiers désactivée. Cette méthode permet typiquement de stocker des données dans le base de données qui ne valident plus avec les règles
     * de validation actuelles, mais qui validaient par le passé, et donc qui peuvent encore exister actuellement sous cette forme dans la base de données.
     *
     * @param action l'action qui à effecuter
     * @param <T>    le type d'objet retourné par l'action
     * @return l'objet retourné par l'action
     * @throws Exception en case d'exception
     */
    protected <T> T doWithoutValidation(final ExecuteCallback<T> action) throws Exception {
        validationInterceptor.setEnabled(false);
        try {
            return action.execute();
        }
        finally {
            validationInterceptor.setEnabled(true);
        }
    }

    /**
     * Exécute une portion de code dans une nouvelle transaction et une nouvelle session hibernate tout en désactivant la validation des objets métiers. Cette méthode combine donc les méthodes
     * #doInNewTransactionAndSession et #doWithoutValidation en une.
     *
     * @param action l'action à effectuer
     * @param <T>    le type d'objet retourné par l'action
     * @return l'objet retourné par l'action
     * @throws Exception en case d'exception
     */
    protected <T> T doInNewTransactionAndSessionWithoutValidation(final TransactionCallback<T> action) throws Exception {
	    return doWithoutValidation(new ExecuteCallback<T>() {
		    @Override
		    public T execute() throws Exception {
			    return doInNewTransactionAndSession(action);
		    }
	    });
    }

    protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, Commune commune, MotifRattachement motif, ModeImposition modeImposition, ForFiscalPrincipal forPrincipal) {
        assertNotNull(forPrincipal);
        assertEquals(debut, forPrincipal.getDateDebut());
        assertEquals(motifOuverture, forPrincipal.getMotifOuverture());
        assertNull(forPrincipal.getDateFin());
        assertNull(forPrincipal.getMotifFermeture());
        final TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
        assertEquals(type, forPrincipal.getTypeAutoriteFiscale());
        assertEquals(Integer.valueOf(commune.getNoOFS()), forPrincipal.getNumeroOfsAutoriteFiscale());
        assertEquals(motif, forPrincipal.getMotifRattachement());
        assertEquals(modeImposition, forPrincipal.getModeImposition());
    }

    protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, RegDate fin, MotifFor motifFermeture, Commune commune, MotifRattachement motif, ModeImposition modeImposition,
                                             ForFiscalPrincipal forPrincipal) {
        assertNotNull(forPrincipal);
        assertEquals(debut, forPrincipal.getDateDebut());
        assertEquals(motifOuverture, forPrincipal.getMotifOuverture());
        assertEquals(fin, forPrincipal.getDateFin());
        assertEquals(motifFermeture, forPrincipal.getMotifFermeture());
        final TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
        assertEquals(type, forPrincipal.getTypeAutoriteFiscale());
        assertEquals(Integer.valueOf(commune.getNoOFS()), forPrincipal.getNumeroOfsAutoriteFiscale());
        assertEquals(motif, forPrincipal.getMotifRattachement());
        assertEquals(modeImposition, forPrincipal.getModeImposition());
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

    protected static void assertEtatCivil(RegDate debut, @Nullable RegDate fin, TypeEtatCivil type, EtatCivil etatCivil) {
        assertNotNull(etatCivil);
        assertEquals(debut, etatCivil.getDateDebut());
        assertEquals(fin, etatCivil.getDateFin());
        assertEquals(type, etatCivil.getTypeEtatCivil());
    }

    protected static void assertAdresseCivile(@Nullable RegDate debut, @Nullable RegDate fin, String rue, String npa, String localite, Adresse adresse) {
        assertNotNull(adresse);
        assertEquals(debut, adresse.getDateDebut());
        assertEquals(fin, adresse.getDateFin());
        assertEquals(rue, adresse.getRue());
        assertEquals(npa, adresse.getNumeroPostal());
        assertEquals(localite, adresse.getLocalite());
    }

    protected static void assertAdresseCivile(@Nullable RegDate debut, @Nullable RegDate fin, String rue, String npa, String localite, @Nullable Integer egid, Adresse adresse) {
        assertNotNull(adresse);
        assertEquals(debut, adresse.getDateDebut());
        assertEquals(fin, adresse.getDateFin());
        assertEquals(rue, adresse.getRue());
        assertEquals(npa, adresse.getNumeroPostal());
        assertEquals(localite, adresse.getLocalite());
        assertEquals(egid, adresse.getEgid());
    }

    protected static void assertAdresseCivile(@Nullable RegDate debut, @Nullable RegDate fin, String rue, String npa, String localite, @Nullable Integer egid, @Nullable Integer ewid,
                                              Adresse adresse) {
        assertNotNull(adresse);
        assertEquals(debut, adresse.getDateDebut());
        assertEquals(fin, adresse.getDateFin());
        assertEquals(rue, adresse.getRue());
        assertEquals(npa, adresse.getNumeroPostal());
        assertEquals(localite, adresse.getLocalite());
        assertEquals(egid, adresse.getEgid());
        assertEquals(ewid, adresse.getEwid());
    }

    protected static void assertLocalisation(LocalisationType type, Integer noOfs, Localisation localisation) {
        assertNotNull(localisation);
        assertEquals(type, localisation.getType());
        assertEquals(noOfs, localisation.getNoOfs());
    }

    /**
     * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockCommune commune) {
        TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune.getNoOFSEtendu(), type, MotifRattachement.DOMICILE);
    }

    /**
     * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockCommune commune, ModeImposition modeImposition) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune, MotifRattachement.DOMICILE, modeImposition);
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
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, RegDate fermeture, @Nullable MotifFor motifFermeture,
                                                 MockCommune commune) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, MotifRattachement.DOMICILE);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, RegDate fermeture, @Nullable MotifFor motifFermeture,
                                                 MockCommune commune, ModeImposition modeImposition) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, MotifRattachement.DOMICILE, modeImposition);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse sur le contribuable spécifié.
     */
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, @Nullable RegDate fermeture, @Nullable MotifFor motifFermeture, MockCommune commune,
                                                 MotifRattachement motifRattachement) {
        final TypeAutoriteFiscale type = (commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune.getNoOFSEtendu(), type, motifRattachement);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse sur le contribuable spécifié avec le mode d'imposition spécifié
     */
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, @Nullable RegDate fermeture, @Nullable MotifFor motifFermeture,
                                                 MockCommune commune, MotifRattachement motifRattachement, ModeImposition modeImposition) {
        final ForFiscalPrincipal ffp = addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, motifRattachement);
        ffp.setModeImposition(modeImposition);
        return ffp;
    }

    /**
     * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockPays pays) {
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
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, RegDate fermeture, @Nullable MotifFor motifFermeture, MockPays pays) {
        assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE);
    }

	/**
     * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié avec le mode d'imposition spécifié
     */
    protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockPays pays,
                                                 ModeImposition modeImposition) {
        final ForFiscalPrincipal ffp = addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays);
        ffp.setModeImposition(modeImposition);
        return ffp;
    }

    protected ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable dpi, RegDate debut, @Nullable RegDate fin, MockCommune commune) {
        ForDebiteurPrestationImposable f = new ForDebiteurPrestationImposable();
        f.setDateDebut(debut);
        f.setDateFin(fin);
        f.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
        f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
        f.setNumeroOfsAutoriteFiscale(commune.getNoOFSEtendu());
        if (dpi.getDernierForDebiteur() == null) {
            tiersService.adaptPremierePeriodicite(dpi, debut);
        }
        f = tiersDAO.addAndSave(dpi, f);
        return f;
    }

    protected RapportPrestationImposable addRapportPrestationImposable(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RegDate debut, @Nullable RegDate fin, boolean annule) {
        RapportPrestationImposable rpi = new RapportPrestationImposable(debut, fin, sourcier, dpi);
        rpi.setAnnule(annule);
        rpi = hibernateTemplate.merge(rpi);

        dpi.addRapportObjet(rpi);
        sourcier.addRapportSujet(rpi);

        return rpi;
    }

    protected AdresseSuisse addAdresseSuisse(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, MockRue rue) {
        AdresseSuisse adresse = new AdresseSuisse();
        adresse.setDateDebut(debut);
        adresse.setDateFin(fin);
        adresse.setUsage(usage);
        adresse.setNumeroRue(rue.getNoRue());
        adresse.setNumeroOrdrePoste(rue.getLocalite().getNPA());
        adresse = (AdresseSuisse) tiersDAO.addAndSave(tiers, adresse);
        return adresse;
    }

    protected AdresseCivile addAdresseCivil(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, TypeAdresseCivil type) {
	    AdresseCivile adresse = new AdresseCivile();
        adresse.setDateDebut(debut);
        adresse.setDateFin(fin);
        adresse.setUsage(usage);
        adresse.setType(type);
        adresse = (AdresseCivile) tiersDAO.addAndSave(tiers, adresse);
        return adresse;
    }

    protected AdresseEtrangere addAdresseEtrangere(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, @Nullable String rue, @Nullable String numeroPostalEtLocalite,
                                                   Pays pays) {
        AdresseEtrangere adresse = new AdresseEtrangere();
        adresse.setDateDebut(debut);
        adresse.setDateFin(fin);
        adresse.setUsage(usage);
        adresse.setRue(rue);
        adresse.setNumeroPostalLocalite(numeroPostalEtLocalite);
        adresse.setNumeroOfsPays(pays.getNoOFS());
        adresse = (AdresseEtrangere) tiersDAO.addAndSave(tiers, adresse);
        return adresse;
    }

    protected AdresseAutreTiers addAdresseAutreTiers(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, TypeAdresseTiers autreType, Tiers autreTiers) {
        AdresseAutreTiers adresse = new AdresseAutreTiers();
        adresse.setDateDebut(debut);
        adresse.setDateFin(fin);
        adresse.setUsage(usage);
        adresse.setAutreTiersId(autreTiers.getId());
        adresse.setType(autreType);
        adresse = (AdresseAutreTiers) tiersDAO.addAndSave(tiers, adresse);
        return adresse;
    }

    protected CollectiviteAdministrative addCedi() {
        return addCollAdm(ServiceInfrastructureService.noCEDI);
    }

    protected CollectiviteAdministrative addCollAdm(MockCollectiviteAdministrative ca) {
        return addCollAdm(ca, null, null);
    }

    protected CollectiviteAdministrative addCollAdm(MockOfficeImpot oid) {
        return addCollAdm(oid, oid.getIdentifiantDistrict(), oid.getIdentifiantRegion());
    }

    protected CollectiviteAdministrative addCollAdm(MockCollectiviteAdministrative oid, @Nullable Integer identifiantDistrict, @Nullable Integer identifiantRegion) {
        CollectiviteAdministrative ca = new CollectiviteAdministrative();
        ca.setNumeroCollectiviteAdministrative(oid.getNoColAdm());
        if (identifiantDistrict != null) {
            ca.setIdentifiantDistrictFiscal(identifiantDistrict);
        }
        if (identifiantRegion != null) {
            ca.setIdentifiantRegionFiscale(identifiantRegion);
        }
        ca = hibernateTemplate.merge(ca);
        hibernateTemplate.flush();
        return ca;
    }

    protected DeclarationImpotSource addLR(DebiteurPrestationImposable debiteur, RegDate debut, RegDate fin, PeriodeFiscale periode) {
        return addLR(debiteur, debut, fin, periode, TypeEtatDeclaration.EMISE);
    }

    protected DeclarationImpotSource addLR(DebiteurPrestationImposable debiteur, RegDate debut, RegDate fin, PeriodeFiscale periode, TypeEtatDeclaration typeEtat) {
        DeclarationImpotSource lr = new DeclarationImpotSource();
        lr.setDateDebut(debut);
        lr.setDateFin(fin);
        lr.setPeriode(periode);
        lr.setModeCommunication(ModeCommunication.PAPIER);
        lr.setPeriodicite(debiteur.getPeriodiciteAt(debut).getPeriodiciteDecompte());

        // l'état "EMISE" si l'état demandé est autre (il faut au moins l'état "EMISE")
        if (typeEtat != TypeEtatDeclaration.EMISE) {
            final EtatDeclaration etatEmission = new EtatDeclarationEmise();
            etatEmission.setDateObtention(fin);
            lr.addEtat(etatEmission);
        }

        if (typeEtat != null) {
            final EtatDeclaration etat = EtatDeclarationHelper.getInstanceOfEtatDeclaration(typeEtat);
            etat.setDateObtention(fin);
            lr.addEtat(etat);
        }

        lr.setTiers(debiteur);
        lr = hibernateTemplate.merge(lr);
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
        ForDebiteurPrestationImposable f = createForDebiteur(dpi, dateDebut, dateFin, typeAutorite, commune);
        return tiersDAO.addAndSave(dpi, f);
    }

    protected ForDebiteurPrestationImposable createForDebiteur(DebiteurPrestationImposable dpi, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutorite, MockCommune commune) {
        ForDebiteurPrestationImposable f = new ForDebiteurPrestationImposable();
        f.setTiers(dpi);
        f.setTypeAutoriteFiscale(typeAutorite);
        f.setNumeroOfsAutoriteFiscale(commune.getNoOFSEtendu());
        f.setDateDebut(dateDebut);
        f.setDateFin(dateFin);
        return f;
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
        return tiersDAO.addAndSave(ctb, f);
    }

    /**
     * Ajoute une déclaration d'impôt ordinaire sur le contribuable spécifié.
     */
    protected DeclarationImpotOrdinaire addDeclarationImpot(Contribuable tiers, PeriodeFiscale periode, RegDate debut, RegDate fin,
                                                            @Nullable TypeContribuable typeC, ModeleDocument modele) {

        final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
        assertNotNull("La collectivité administrative du CEDI n'a pas été définie", cedi);

        return addDeclarationImpot(tiers, periode, debut, fin, cedi, typeC, modele);
    }

    @Override
    protected DeclarationImpotOrdinaire assignerNumeroSequenceEtSaveDeclarationImpot(Contribuable ctb, DeclarationImpotOrdinaire di) {
        if (useTiersServiceToCreateDeclarationImpot()) {
            return (DeclarationImpotOrdinaire) tiersDAO.addAndSave(ctb, di);
        } else {
            return super.assignerNumeroSequenceEtSaveDeclarationImpot(ctb, di);
        }
    }

    /**
     * L'implémentation du tiersService est très bien, mais elle provoque une sauvegarde du tiers
     * et de la déclaration, ce qui n'est pas aprécié par tous les tests
     */
    protected boolean useTiersServiceToCreateDeclarationImpot() {
        return true;
    }

    protected SituationFamille addSituation(PersonnePhysique pp, RegDate debut, RegDate fin, Integer nombreEnfants) {
        SituationFamille situation = new SituationFamillePersonnePhysique();
        situation.setDateDebut(debut);
        situation.setDateFin(fin);
        situation.setNombreEnfants(nombreEnfants);
        return tiersDAO.addAndSave(pp, situation);
    }

    protected SituationFamilleMenageCommun addSituation(MenageCommun menage, RegDate debut, RegDate fin, int nombreEnfants,
                                                        TarifImpotSource tarif) {
        SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
        situation.setDateDebut(debut);
        situation.setDateFin(fin);
        situation.setNombreEnfants(nombreEnfants);
        situation.setTarifApplicable(tarif);
        return (SituationFamilleMenageCommun) tiersDAO.addAndSave(menage, situation);
    }

    protected IdentificationPersonne addIdentificationPersonne(PersonnePhysique pp, CategorieIdentifiant categorie, String identifiant) {
        IdentificationPersonne ident = new IdentificationPersonne();
        ident.setCategorieIdentifiant(categorie);
        ident.setIdentifiant(identifiant);
        return tiersDAO.addAndSave(pp, ident);
    }

    private Stack<SecurityProviderInterface> securityProviderStack = new Stack<SecurityProviderInterface>();

    protected void pushSecurityProvider(SecurityProviderInterface providerInterface) {
        securityProviderStack.push(SecurityProvider.getProvider());

        final SecurityProvider provider = new SecurityProvider();
        provider.setProvider(providerInterface);
    }

    protected void popSecurityProvider() {
        final SecurityProvider provider = new SecurityProvider();
        provider.setProvider(securityProviderStack.pop());
    }
}

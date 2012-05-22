package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.civil.data.AdoptionReconnaissance;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.EntityKey;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.tiers.Contribuable.FirstForsList;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantProcessor;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurMenagesResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurPersonnesPhysiquesResults;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.ValidationInterceptor;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Service de recherche des tiers. Effectue conjointement la recherche en base et dans le moteur de recherche.
 */
public class TiersServiceImpl implements TiersService {

    private static final Logger LOGGER = Logger.getLogger(TiersServiceImpl.class);

	private TiersDAO tiersDAO;
	private EvenementFiscalService evenementFiscalService;
	private GlobalTiersSearcher tiersSearcher;
	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;
	private EvenementCivilEchDAO evenementCivilEchDAO;
	private ServiceInfrastructureService serviceInfra;
	private ServiceCivilService serviceCivilService;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private TacheService tacheService;
	private SituationFamilleService situationFamilleService;
	private AdresseService adresseService;
	private RemarqueDAO remarqueDAO;
	private ServicePersonneMoraleService servicePM;
	private ValidationService validationService;
	private ValidationInterceptor validationInterceptor;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private AssujettissementService assujettissementService;

    /**
     * Recherche les Tiers correspondants aux critères dans le data model de Unireg
     *
     * @param tiersCriteria les critères de recherche
     * @return la liste des tiers correspondants aux criteres.
     * @throws IndexerException
     */
    @Override
    public List<TiersIndexedData> search(TiersCriteria tiersCriteria) throws IndexerException {
        return tiersSearcher.search(tiersCriteria);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTiersSearcher(GlobalTiersSearcher searcher) {
        this.tiersSearcher = searcher;
    }

    @Override
    public TiersDAO getTiersDAO() {
        return tiersDAO;
    }

    public void setTiersDAO(TiersDAO tiersDAO) {
        this.tiersDAO = tiersDAO;
    }

    @Override
    public ServiceInfrastructureService getServiceInfra() {
        return serviceInfra;
    }

    public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
        this.serviceInfra = serviceInfra;
    }

    @Override
    public ServiceCivilService getServiceCivilService() {
        return serviceCivilService;
    }

    public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
        this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
    }

    public void setServiceCivilService(ServiceCivilService serviceCivil) {
        this.serviceCivilService = serviceCivil;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTacheService(TacheService tacheService) {
        this.tacheService = tacheService;
    }

    public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
        this.hibernateTemplate = hibernateTemplate;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setAssujettissementService(AssujettissementService assujettissementService) {
        this.assujettissementService = assujettissementService;
    }

    public void setAdresseService(AdresseService adresseService) {
        this.adresseService = adresseService;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setRemarqueDAO(RemarqueDAO remarqueDAO) {
        this.remarqueDAO = remarqueDAO;
    }

    public void setServicePM(ServicePersonneMoraleService servicePM) {
        this.servicePM = servicePM;
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
        this.validationInterceptor = validationInterceptor;
    }

    /**
     * Renvoie la personne physique correspondant au numéro d'individu passé en paramètre.
     *
     * @param numeroIndividu le numéro de l'individu.
     * @return la personne physique correspondante au numéro d'individu passé en paramètre, ou <b>null</b> s'il n'existe pas.
     */
    @Override
    public PersonnePhysique getPersonnePhysiqueByNumeroIndividu(long numeroIndividu) {
        return tiersDAO.getPPByNumeroIndividu(numeroIndividu);
    }

    @Override
    public Tiers getTiers(long numeroTiers) {
        return tiersDAO.get(numeroTiers);
    }

    @Override
    public PersonnePhysique changeNHenHabitant(PersonnePhysique nonHabitant, Long numInd, RegDate date) {
        Assert.isFalse(nonHabitant.isHabitantVD(), "changeNHenHabitant : la PP fourni est habitant");
        nonHabitant.setNumeroIndividu(numInd);
        nonHabitant.setNumeroAssureSocial(null);
        nonHabitant.setNom(null);
        nonHabitant.setPrenom(null);
        nonHabitant.setDateNaissance(null);
        nonHabitant.setSexe(null);
        nonHabitant.setNumeroOfsNationalite(null);
        nonHabitant.setCategorieEtranger(null);
        nonHabitant.setDateDebutValiditeAutorisation(null);
        nonHabitant.setIdentificationsPersonnes(null);

        if (date != null) {
            //fermeture de la situation de famille si nbEnfant = 0 ou etatCivil != du civil
            final SituationFamille sitFam = nonHabitant.getSituationFamilleActive();
            if (sitFam != null) {
                Individu ind = getIndividu(nonHabitant);
                if (ind != null) {
                    ch.vd.unireg.interfaces.civil.data.EtatCivil dernierEtatCivil = ind.getEtatCivilCourant();
                    TypeEtatCivil etatCivilDuCivil = dernierEtatCivil == null ? null : dernierEtatCivil.getTypeEtatCivil();
	                if (etatCivilDuCivil != null && (
                            sitFam.getEtatCivil() != EtatCivilHelper.civil2core(etatCivilDuCivil) ||
                                    sitFam.getNombreEnfants() == 0)) {
                        situationFamilleService.closeSituationFamille(nonHabitant, date);
                    }
                }
            }
            //fermeture des adresses fiscales temporaires
            if (nonHabitant.getAdressesTiers() != null) {
                for (AdresseTiers adr : nonHabitant.getAdressesTiers()) {
                    boolean permanente = false;
                    if (adr instanceof AdresseSupplementaire) {
                        final AdresseSupplementaire adrSup = (AdresseSupplementaire) adr;
                        if (adrSup.isPermanente()) {
                            permanente = true;
                        }
                    }
                    if (!adr.isAnnule() && adr.getDateDebut().isAfterOrEqual(date) && !permanente) {
                        adr.setAnnulationDate(date.asJavaDate());
                    } else if (!adr.isAnnule() && adr.getDateFin() == null && !permanente) {
                        adr.setDateFin(date);
                    }
                }
            }
        }
        nonHabitant.setHabitant(true);
        return nonHabitant;
    }

    /**
     * change un habitant en non habitant (en cas de départ HC ou HS)
     *
     * @param habitant la PP de type Habitant
     * @return la même PP de type nonHabitant maintenant
     */
    @Override
    public PersonnePhysique changeHabitantenNH(PersonnePhysique habitant) {

        Assert.isTrue(habitant.isHabitantVD(), "changeHabitantenNH : la PP fourni n'est pas habitant");
        final Individu ind = getIndividu(habitant);
        habitant.setNumeroAssureSocial(ind.getNouveauNoAVS());
        habitant.setNom(ind.getNom());
        habitant.setPrenom(ind.getPrenom());
        habitant.setDateNaissance(ind.getDateNaissance());
        habitant.setDateDeces(ind.getDateDeces());
        if (ind.isSexeMasculin()) {
            habitant.setSexe(Sexe.MASCULIN);
        } else {
            habitant.setSexe(Sexe.FEMININ);
        }

	    final Individu individu = serviceCivilService.getIndividu(habitant.getNumeroIndividu(), null, AttributeIndividu.NATIONALITE, AttributeIndividu.PERMIS);
	    if (individu == null) {
		    throw new IndividuNotFoundException(habitant.getNumeroIndividu());
	    }

        // nationalité (on ne garde qu'une nationalité au hasard, sachant que si l'une est Suisse, elle a priorité)
        final Collection<Nationalite> nationalites = individu.getNationalites();
        if (nationalites != null) {
            Pays pays = null;
            for (Nationalite nationalite : nationalites) {
                if (nationalite.getDateFinValidite() == null) {
                    if (pays == null) {
                        pays = nationalite.getPays();
                    } else if (!pays.isSuisse()) {
                        pays = nationalite.getPays();
                    }
                }
            }
            if (pays != null) {
                habitant.setNumeroOfsNationalite(pays.getNoOFS());
            } else {
                habitant.setNumeroOfsNationalite(null);
            }
        } else {
            habitant.setNumeroOfsNationalite(null);
        }

        //permis
        final Permis dernierPermis = individu.getPermis().getPermisActif(null);
        if (dernierPermis != null) {
            habitant.setCategorieEtranger(CategorieEtranger.enumToCategorie(dernierPermis.getTypePermis()));
            habitant.setDateDebutValiditeAutorisation(dernierPermis.getDateDebut());
        } else {
            habitant.setCategorieEtranger(null);
            habitant.setDateDebutValiditeAutorisation(null);
        }

        // indentification navs11 et numRCE
        setIdentifiantsPersonne(habitant, ind.getNoAVS11(), ind.getNumeroRCE());

        habitant.setHabitant(false);
        return habitant;
    }

    @Override
    public void setIdentifiantsPersonne(PersonnePhysique nonHabitant, String navs11, String numRce) {
        final Set<IdentificationPersonne> set = new HashSet<IdentificationPersonne>(2);

        // numéro avs à 11 positions
        if (StringUtils.isNotEmpty(navs11)) {
            navs11 = FormatNumeroHelper.removeSpaceAndDash(navs11);
            final IdentificationPersonne id = new IdentificationPersonne();
            id.setCategorieIdentifiant(CategorieIdentifiant.CH_AHV_AVS);
            id.setPersonnePhysique(nonHabitant);
            if (navs11.length() == 8) {
                id.setIdentifiant(navs11.concat("000"));
            } else {
                id.setIdentifiant(navs11);
            }
            set.add(id);
        }

        // numéro du registre des étrangers
        if (StringUtils.isNotEmpty(numRce) && !"0".equals(StringUtils.trimToEmpty(numRce))) {
            final IdentificationPersonne id = new IdentificationPersonne();
            id.setCategorieIdentifiant(CategorieIdentifiant.CH_ZAR_RCE);
            id.setPersonnePhysique(nonHabitant);
            id.setIdentifiant(numRce.trim());
            set.add(id);
        }

        // assignation des identifiants
        nonHabitant.setIdentificationsPersonnes(set);
    }

    /* (non-Javadoc)
	 * @see ch.vd.uniregctb.tiers.TiersService#changeNHenMenage(long)
	 */
    // ATTENTION: cette fonction ne doit être appelée que si c'est STRICTEMENT nécessaire!!!
    @Override
    public void changeNHenMenage(final long numeroTiers) {

        final DiscriminatorColumn typeAnnotation = AnnotationUtils.findAnnotation(MenageCommun.class, DiscriminatorColumn.class);
        final DiscriminatorValue discrimatorAnnotation = AnnotationUtils.findAnnotation(MenageCommun.class, DiscriminatorValue.class);

        if (typeAnnotation == null || discrimatorAnnotation == null) {
            throw new RuntimeException("Impossible de changer le type du tiers n° " + numeroTiers + " à ménageCommun");
        }

        final List<VueSituationFamille> histoSF = situationFamilleService.getVueHisto((Contribuable) tiersDAO.get(numeroTiers));

        // effacement des liens d'identification (qui ne concernent qu'une personne physique, pas un ménage commun)
        // [UNIREG-2893] effacement des droits d'accès (qui ne concernent que les personnes physiques)
        hibernateTemplate.execute(new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                {
                    final String deleteQuery = "DELETE FROM IDENTIFICATION_PERSONNE WHERE NON_HABITANT_ID=?";
                    final SQLQuery query = session.createSQLQuery(deleteQuery);
                    query.setLong(0, numeroTiers);
                    query.executeUpdate();
                }
                {
                    final String deleteQuery = "DELETE FROM DROIT_ACCES WHERE TIERS_ID=?";
                    final SQLQuery query = session.createSQLQuery(deleteQuery);
                    query.setLong(0, numeroTiers);
                    query.executeUpdate();
                }
                return null;
            }
        });

        // changement du type de tiers
        hibernateTemplate.execute(new HibernateCallback<Object>() {

            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {

                final String updateQuery = "update TIERS set "
                        + typeAnnotation.name() + "=?, "
                        + "PP_HABITANT=null, "
                        + "ANCIEN_NUMERO_SOURCIER=null, "
                        + "NH_NUMERO_ASSURE_SOCIAL=null, "
                        + "NH_NOM=null, NH_PRENOM=null, "
                        + "NH_DATE_NAISSANCE=null, NH_SEXE=null, "
                        + "NH_NO_OFS_NATIONALITE=null, "
                        + "NH_CAT_ETRANGER=null, "
                        + "NH_DATE_DEBUT_VALID_AUTORIS=null, "
                        + "DATE_DECES=null, COMPLEMENT_NOM=null "
                        + "where NUMERO=?";

                final SQLQuery query = session.createSQLQuery(updateQuery);
                query.setString(0, discrimatorAnnotation.value());
                query.setLong(1, numeroTiers);
                query.executeUpdate();
                return null;
            }
        });

        final DiscriminatorColumn columnTypeSituationFamille = AnnotationUtils.findAnnotation(SituationFamilleMenageCommun.class, DiscriminatorColumn.class);
        final DiscriminatorValue valueTypeSituationFamille = AnnotationUtils.findAnnotation(SituationFamilleMenageCommun.class, DiscriminatorValue.class);

        // annulation et création des situations famille
        for (VueSituationFamille vueSF : histoSF) {

            final Long idSF = vueSF.getId();

            if (columnTypeSituationFamille == null || valueTypeSituationFamille == null) {
                throw new RuntimeException("Impossible de changer le type de la situation famille n° " + idSF + " à SituationFamilleMenageCommun");
            }

            // changement du type de la situation famille en SituationFamilleMenageCommun
            hibernateTemplate.execute(new HibernateCallback<Object>() {

                @Override
                public Object doInHibernate(Session session) throws HibernateException, SQLException {

                    final String updateSFQuery = String.format("update SITUATION_FAMILLE set %1$s=? where ID=?", columnTypeSituationFamille.name());

                    final SQLQuery query = session.createSQLQuery(updateSFQuery);
                    query.setString(0, valueTypeSituationFamille.value());
                    query.setLong(1, idSF);
                    query.executeUpdate();
                    return null;
                }

            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CollectiviteAdministrative getCollectiviteAdministrative(int noTechnique, boolean doNotAutoFlush) {
        return tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(noTechnique, doNotAutoFlush);
    }

    @Override
    public CollectiviteAdministrative getCollectiviteAdministrative(int noTechnique) {
        return tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(noTechnique, false);
    }

    /**
     * Récupère le tiers correspondant à la collectivite administrative avec un numéro donné (crée le tiers s'il n'existe pas).
     *
     * @param noTechnique le numero technique de la collectivite administrative
     * @return le tiers correspondant à la collectivite administrative
     */
    @Override
    public CollectiviteAdministrative getOrCreateCollectiviteAdministrative(int noTechnique) {
        return this.getOrCreateCollectiviteAdministrative(noTechnique, false);
    }

    @Override
    public CollectiviteAdministrative getOrCreateCollectiviteAdministrative(int noTechnique, boolean doNotAutoFlush) {

        CollectiviteAdministrative collectivite = getCollectiviteAdministrative(noTechnique, doNotAutoFlush);

        if (collectivite == null) {
            collectivite = new CollectiviteAdministrative();
            collectivite.setNumeroCollectiviteAdministrative(noTechnique);
            collectivite = (CollectiviteAdministrative) tiersDAO.save(collectivite);
        }

        return collectivite;
    }

    private static interface IndividuProvider {
        Individu getIndividu(PersonnePhysique pp);
    }

    private final IndividuProvider individuProviderWithoutDate = new IndividuProvider() {
        @Override
        public Individu getIndividu(PersonnePhysique pp) {
            return TiersServiceImpl.this.getIndividu(pp);
        }
    };

    private Sexe getSexe(PersonnePhysique pp, IndividuProvider indProvider) {
        if (pp == null) {
            return null;
        }

        final Sexe sexe;
        if (pp.isHabitantVD()) {
            final Individu individu = indProvider.getIndividu(pp);
            if (individu == null) {
                throw new IndividuNotFoundException(pp);
            }
            sexe = (individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ);
        } else {
            sexe = pp.getSexe();
        }
        return sexe;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sexe getSexe(PersonnePhysique pp) {
        return getSexe(pp, individuProviderWithoutDate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMemeSexe(PersonnePhysique pp1, PersonnePhysique pp2) {
        final Sexe sexeHabitant = getSexe(pp1);
        final Sexe sexeConjoint = getSexe(pp2);
        boolean memeSexe = false;
        if (sexeHabitant != null && sexeConjoint != null) {
            memeSexe = sexeHabitant == sexeConjoint;
        }
        return memeSexe;
    }


    /**
     * Détermine si un habitant est étranger sans permis C.
     *
     * @param habitant l'habitant
     * @param date     la date à laquelle on désire se placer
     * @return true si l'habitant est étranger sans permis C
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    private boolean isHabitantEtrangerSansPermisC(PersonnePhysique habitant, RegDate date) throws TiersException {
        return isHabitantEtrangerAvecOuSansPermisC(habitant, false, date);
    }

    /**
     * Détermine si un habitant est étranger avec permis C.
     *
     * @param habitant l'habitant
     * @param date     la date à laquelle on désire se placer
     * @return true si l'habitant est étranger avec permis C
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    @Override
    public boolean isHabitantEtrangerAvecPermisC(PersonnePhysique habitant, RegDate date) throws TiersException {
        if (date == null) date = RegDate.get();
        return isHabitantEtrangerAvecOuSansPermisC(habitant, true, date);
    }


    /**
     * Détermine si un habitant est étranger avec ou sans permis C.
     *
     * @param habitant    l'habitant
     * @param avecPermisC <ul> <li>true pour savoir si l'habitant est etranger avec permis C <li> false pour savoir si l'habitant est etranger sans permis C </ul>
     * @param date        la date à laquelle on désire se placer
     * @return true si l'habitant est étranger avec ou sans permis C
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    private boolean isHabitantEtrangerAvecOuSansPermisC(PersonnePhysique habitant, boolean avecPermisC, RegDate date) throws TiersException {

        /* Récupération de l'individu avec ses permis, ses nationalités et son origine */
        final Individu individu = serviceCivilService.getIndividu(habitant.getNumeroIndividu(), date,
                AttributeIndividu.NATIONALITE, AttributeIndividu.PERMIS, AttributeIndividu.ORIGINE);

        /* A-t-il une nationalité suisse en cours et/ou des nationalites étrangères ? */
        // TODO (msi) utiliser la méthode isSuisse() !
        boolean nationaliteSuisse = false;
        boolean nationalitesEtrangeres = false;
        if (individu.getNationalites() != null) {
            final Collection<Nationalite> nationalites = individu.getNationalites();
            for (Nationalite nationalite : nationalites) {
                if (RegDateHelper.isBeforeOrEqual(nationalite.getDateDebutValidite(), date, NullDateBehavior.EARLIEST)) {
                    if ((nationalite.getDateFinValidite() == null) && (nationalite.getPays().getNoOFS() == ServiceInfrastructureService.noOfsSuisse)) {
                        nationaliteSuisse = true;
                    } else if ((nationalite.getDateFinValidite() == null) && (nationalite.getPays().getNoOFS() != ServiceInfrastructureService.noOfsSuisse)) {
                        nationalitesEtrangeres = true;
                    }
                }
            }
        }

        /* Nationalité suisse : il est suisse */
        if (nationaliteSuisse) {
            return false;
        }

        /* Nationalites etrangeres : il est étranger on vérifie ses permis suisses */
        if (nationalitesEtrangeres) {
            return avecPermisC ? isAvecPermisC(individu, date) : isSansPermisC(individu, false, date);
        }

        /* Nationalité inconnue, on regarde les permis en cours (il doit forcément en avoir un) */
        return avecPermisC ? isAvecPermisC(individu, date) : isSansPermisC(individu, true, date);
    }

    /**
     * L'individu est-t-il sans permis C en cours de validité ?
     *
     * @param individu        l'individu
     * @param permisMustExist si true, alors une exception est lancée si l'individu n'a aucun permis en cours
     * @param date            la date à laquelle on désire se placer
     * @return true si l'individu n'a pas de permis C en cours
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    private boolean isSansPermisC(Individu individu, boolean permisMustExist, RegDate date) throws TiersException {

        final Permis permis = individu.getPermis().getPermisActif(date);
        if (permis == null && permisMustExist) {
            throw new TiersException("Impossible de déterminer la nationalité de l'individu " + individu.getNoTechnique());
        }

        // [UNIREG-1860] les permis annulés ne doivent pas être comptabilisés
        final boolean permisC = (permis != null && permis.getTypePermis() == TypePermis.ETABLISSEMENT && permis.getDateAnnulation() == null);
        return !permisC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvecPermisC(Individu individu) {
        RegDate maintenant = RegDate.get();
        return isAvecPermisC(individu, maintenant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvecPermisC(Individu individu, RegDate date) {
        final Permis permis = individu.getPermis().getPermisActif(date);
        if (permis == null) {
            return false;
        }

        // [UNIREG-1860] les permis annulés ne doivent pas être comptabilisés
        return permis.getTypePermis() == TypePermis.ETABLISSEMENT && permis.getDateAnnulation() == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuisse(PersonnePhysique pp, RegDate date) throws TiersException {

        if (pp.isHabitantVD()) {
            final long numeroIndividu = pp.getNumeroIndividu();
            final Collection<Nationalite> nationalites = getServiceCivilService().getNationalites(numeroIndividu, date);
            if (nationalites != null && !nationalites.isEmpty()) {
                for (Nationalite nationalite : nationalites) {
                    if (RegDateHelper.isBeforeOrEqual(nationalite.getDateDebutValidite(), date, NullDateBehavior.EARLIEST) &&
                            (nationalite.getDateFinValidite() == null || nationalite.getDateFinValidite().isAfterOrEqual(date)) &&
                            nationalite.getPays().getNoOFS() == ServiceInfrastructureService.noOfsSuisse)
                        return true;
                }
            } else {
                throw new TiersException("Impossible de déterminer la nationalité de l'individu n°" + numeroIndividu);
            }
        } else {
            final Integer numeroOfsNationalite = pp.getNumeroOfsNationalite();
            if (numeroOfsNationalite == null) {
                throw new TiersException("La nationalité du contribuable " + FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()) + " est inconnue");
            }
            return (numeroOfsNationalite == ServiceInfrastructureService.noOfsSuisse);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuisseOuPermisC(PersonnePhysique pp, RegDate dateEvenement) throws TiersException {
        return !isEtrangerSansPermisC(pp, dateEvenement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuisse(Individu individu, RegDate date) throws TiersException {
        if (date == null) date = RegDate.get();
        final Collection<Nationalite> nationalites = individu.getNationalites();
        if (nationalites == null || nationalites.isEmpty()) {
            throw new TiersException("Impossible de déterminer la nationalité de l'individu n°" + individu.getNoTechnique());
        }
        for (Nationalite nationalite : nationalites) {
            if (RegDateHelper.isBeforeOrEqual(nationalite.getDateDebutValidite(), date, NullDateBehavior.EARLIEST) &&
                    (nationalite.getDateFinValidite() == null || nationalite.getDateFinValidite().isAfterOrEqual(date))
                    && (nationalite.getPays().isSuisse())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Détermine si une personne physique est étrangère sans permis C.
     *
     * @param pp   la personne physique
     * @param date la date à laquelle on désire se placer
     * @return true si la personne physique est étrangère sans permis C
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    @Override
    public boolean isEtrangerSansPermisC(PersonnePhysique pp, RegDate date) throws TiersException {
        if (date == null) date = RegDate.get();
        if (pp.isHabitantVD()) {
            return isHabitantEtrangerSansPermisC(pp, date);
        } else {
            return isNonHabitantEtrangerSansPermisC(pp);
        }
    }

    /**
     * Détermine si un non habitant est étranger sans permis C.
     *
     * @param nonHabitant le non habitant
     * @return true si le non habitant est étranger sans permis C
     * @throws TiersException si l'habitant ne possède ni nationalité ni permis.
     */
    private boolean isNonHabitantEtrangerSansPermisC(PersonnePhysique nonHabitant) throws TiersException {

        /* cas : numero OFS renseigné */
        if (nonHabitant.getNumeroOfsNationalite() != null) {
            int noOfsPaysOrigine = nonHabitant.getNumeroOfsNationalite();
            /* le non habitant a une origine suisse */
            if (noOfsPaysOrigine == ServiceInfrastructureService.noOfsSuisse) {
                return false;
            } else {
                /* le non habitant a une origine étrangère avec un permis C */
                //noinspection RedundantIfStatement
                if (nonHabitant.getCategorieEtranger() != null && nonHabitant.getCategorieEtranger() == CategorieEtranger._03_ETABLI_C) {
                    return false;
                } else {
                    /* le non habitant est étranger sans permis C */
                    return true;
                }
            }

        } /* autre cas : numero OFS non renseigné =>le non habitant a une origine suisse */ else {
            /* le non habitant a une origine étrangère avec un permis C */
            if (nonHabitant.getCategorieEtranger() != null && nonHabitant.getCategorieEtranger() == CategorieEtranger._03_ETABLI_C) {
                return false;
            } else if (nonHabitant.getCategorieEtranger() != null) {
                /* le non habitant est étranger sans permis C */
                return true;
            } else {//ni nationalité ni permis
                throw new TiersException("Impossible de déterminer la nationalité du contribuable " + nonHabitant.getNumero());
            }
        }
    }

    /**
     * Détermination de l'individidu secondaire <ul> <li>2 personnes de meme sexe : le deuxieme dans l'ordre alphabétique est le secondaire</li> <li>2 personnes de sexe different : la femme est le
     * secondaire</li> </ul>
     */
    @Override
    public PersonnePhysique getPrincipal(PersonnePhysique tiers1, PersonnePhysique tiers2) {

        if (tiers1 == null && tiers2 == null) {
            return null;
        }

        if (tiers1 == null) {
            return tiers2;
        }

        if (tiers2 == null) {
            return tiers1;
        }

        /*
		 * Détermination du sexe et du nom du premier tiers
		 */
        final Sexe tiers1Sexe = getSexe(tiers1);
        final Sexe tiers2Sexe = getSexe(tiers2);

        /*
		 * Cas spécial où le sexe d'un des deux tiers est inconnu: on part du principe que c'est un couple normal (non-pacsé)
		 */
        if (tiers1Sexe == null && tiers2Sexe != null) {
            if (Sexe.MASCULIN == tiers2Sexe) {
                return tiers2;
            } else {
                return tiers1;
            }
        } else if (tiers2Sexe == null && tiers1Sexe != null) {
            if (Sexe.MASCULIN == tiers1Sexe) {
                return tiers1;
            } else {
                return tiers2;
            }
        }

        // Cas général
        final boolean tiers1Masculin = Sexe.MASCULIN == tiers1Sexe;
        final boolean tiers2Masculin = Sexe.MASCULIN == tiers2Sexe;

        /*
		 * Les 2 personnes sont du même sexe
		 */
        if (tiers1Masculin == tiers2Masculin) {

            final String nom1 = getNom(tiers1);
            final String nom2 = getNom(tiers2);

            if (nom1 != null && nom1.compareTo(nom2) < 0) {
                return tiers1;
            } else {
                return tiers2;
            }
        }

        /*
		 * Les 2 personnes sont de sexe différents
		 */
        else {
            if (tiers1Masculin) {
                return tiers1;
            } else {
                return tiers2;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersonnePhysique getPrincipal(MenageCommun menageCommun) {
        final Set<PersonnePhysique> personnes = getPersonnesPhysiques(menageCommun);
        if (!personnes.isEmpty()) {
            final List<PersonnePhysique> liste = new ArrayList<PersonnePhysique>(personnes);
            final PersonnePhysique tiers1 = liste.get(0);
            if (liste.size() > 1) {
                final PersonnePhysique tiers2 = liste.get(1);
                return getPrincipal(tiers1, tiers2);
            } else {
                return tiers1;
            }
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnsembleTiersCouple getEnsembleTiersCouple(MenageCommun menageCommun, @Nullable RegDate date) {

        if (menageCommun == null) {
            return null;
        }

        // Récupération des deux parties du ménage
        final Set<PersonnePhysique> personnes = getComposantsMenage(menageCommun, date);
        return construitEnsembleTiersCouple(menageCommun, personnes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnsembleTiersCouple getEnsembleTiersCouple(MenageCommun menageCommun, int anneePeriode) {

        if (menageCommun == null) {
            return null;
        }

        // Récupération des deux parties du ménage
        final Set<PersonnePhysique> personnes = getComposantsMenage(menageCommun, anneePeriode);
        return construitEnsembleTiersCouple(menageCommun, personnes);
    }

    private EnsembleTiersCouple construitEnsembleTiersCouple(MenageCommun menageCommun, final Set<PersonnePhysique> personnes) {
        final PersonnePhysique principal;
        final PersonnePhysique conjoint;

        if (personnes != null && !personnes.isEmpty()) {
            if (personnes.size() == 1) {
                // Détermination des tiers principal et secondaire
                final Iterator<PersonnePhysique> iter = personnes.iterator();

                principal = iter.next();
                conjoint = null;
                Assert.notNull(principal, "le tiers survivant du ménage commun n'a pu être trouvé");
            } else {
                Assert.isTrue(personnes.size() == 2, "Détecté un ménage commun avec plus de deux personnes");

                // Détermination des tiers principal et secondaire
                final Iterator<PersonnePhysique> iter = personnes.iterator();
                final PersonnePhysique p1 = iter.next();
                final PersonnePhysique p2 = iter.next();

                principal = getPrincipal(p1, p2);
                conjoint = (principal == p1 ? p2 : p1);
                Assert.notNull(principal, "le tiers principal du ménage commun n'a pu être trouvé");
                Assert.notNull(conjoint, "le tiers conjoint du ménage commun n'a pu être trouvé");
            }
        } else {
            /* un ménage commun peut ne posséder aucune personne active en cas de décès d'un des conjoints */
            principal = null;
            conjoint = null;
        }

        // Retour du résultat
        EnsembleTiersCouple ensemble = new EnsembleTiersCouple();
        ensemble.setMenage(menageCommun);
        ensemble.setPrincipal(principal);
        ensemble.setConjoint(conjoint);
        return ensemble;
    }

    /**
     * Contruit l'ensemble des tiers individuels et tiers menage à partir du tiers personne physique.
     *
     * @param personne  le tiers personne physique du menage
     * @param date         la date de référence, ou null pour obtenir tous les composants connus dans l'histoire du ménage.
     * @return un objet EnsembleTiersCouple regroupant l'ensemble des tiers individuels et tiers menage.
     */
    @Override
    public EnsembleTiersCouple getEnsembleTiersCouple(PersonnePhysique personne, @Nullable RegDate date) {

        if (personne == null) {
            return null;
        }

        // Recherche du tiers MenageCommun
        final MenageCommun menageCommun = findMenageCommun(personne, date);
        if (menageCommun == null) {
            return null;
        }

        return getEnsembleTiersCouple(menageCommun, date);
    }

    /**
     * Recherche le ménage commun d'une personne physique à une date donnée.
     *
     * @param personne la personne dont on recherche le ménage.
     * @param date     la date de référence, ou null pour obtenir le ménage courant.
     * @return le ménage common dont la personne est membre à la date donnée, ou <b>null<b> si aucun ménage n'a été trouvé.
     */
    @Override
    public MenageCommun findMenageCommun(PersonnePhysique personne, RegDate date) {

        MenageCommun menageCommun = null;

        final Set<RapportEntreTiers> rapportsSujet = personne.getRapportsSujet();
        if (rapportsSujet != null) {
            for (RapportEntreTiers rapportSujet : rapportsSujet) {
                if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapportSujet.getType() && rapportSujet.isValidAt(date)) {
                    /*
					 * le rapport de l'apartenance a été trouvé, on en déduit donc le tiers ménage
					 */
                    menageCommun = (MenageCommun) tiersDAO.get(rapportSujet.getObjetId());
                    break;
                }
            }

        }

        return menageCommun;
    }

    /**
     * Recherche le dernier ménage commun d'une personne physique.
     *
     * @param personne la personne dont on recherche le ménage.
     * @return le dernier ménage common dont la personne est membre, ou <b>null<b> si aucun ménage n'a été trouvé.
     */
    @Override
    public MenageCommun findDernierMenageCommun(PersonnePhysique personne) {
        MenageCommun menageCommun = null;

        final Set<RapportEntreTiers> rapportsSujet = personne.getRapportsSujet();
        RapportEntreTiers lastRapport = null;
        for (RapportEntreTiers rapportSujet : rapportsSujet) {
            if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapportSujet.getType()) {
                if (lastRapport == null) {
                    lastRapport = rapportSujet;
                } else if (lastRapport.getDateDebut().isBefore(rapportSujet.getDateDebut())) {
                    lastRapport = rapportSujet;
                }
            }
        }
        if (lastRapport != null) {
            menageCommun = (MenageCommun) tiersDAO.get(lastRapport.getObjetId());
        }

        return menageCommun;
    }

    @Override
    public boolean isInMenageCommun(PersonnePhysique personne, RegDate date) {
        return (findMenageCommun(personne, date) != null);
    }

    /**
     * Ajoute l'individu spécifié en tant que tiers du ménage commun, à partir de la date spécifiée. <b>Attention : le menage et le tiers spécifiés seront automatiques sauvés !</b>
     *
     * @param menage    le ménage sur lequel le tiers doit être ajouté
     * @param tiers     le tiers à ajouter au ménage
     * @param dateDebut la date de début de validité de la relation entre tiers
     * @param dateFin   la date de fin de validité de la relation entre tiers (peut être nulle)
     * @return le rapport-entre-tiers avec les références mises-à-jour des objets sauvés
     */
    @Override
    public RapportEntreTiers addTiersToCouple(MenageCommun menage, PersonnePhysique tiers, RegDate dateDebut, RegDate dateFin) {

        RapportEntreTiers rapport = new AppartenanceMenage();
        rapport.setDateDebut(dateDebut);
        rapport.setDateFin(dateFin);

        return addRapport(rapport, tiers, menage);
    }

    /**
     * Ajout d'un rapport de type contact impôt source entre le débiteur et le contribuable
     *
     * @param debiteur
     * @param contribuable
     * @return le rapport
     */
    @Override
    public RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable) {

        return addContactImpotSource(debiteur, contribuable, RegDate.get());
    }

    /**
     * Ajout d'un rapport de type contact impôt source entre le débiteur et le contribuable avec une date de début
     *
     * @param debiteur
     * @param contribuable
     * @param dateDebut
     * @return le rapport
     */
    @Override
    public RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable, RegDate dateDebut) {

        RapportEntreTiers rapport = new ContactImpotSource();
        rapport.setDateDebut(dateDebut);

        return addRapport(rapport, contribuable, debiteur);
    }

    /**
     * Etabli et sauve en base un rapport entre deux tiers.
     *
     * @param rapport le rapport à sauver
     * @param sujet   le tiers sujet considéré
     * @param objet   le tiers objet considéré
     * @return le rapport sauvé en base
     */
    @Override
    public RapportEntreTiers addRapport(RapportEntreTiers rapport, Tiers sujet, Tiers objet) {

        /* sauve les objets tiers avant toute chose */
        objet = tiersDAO.save(objet);
        sujet = tiersDAO.save(sujet);

        /* modifie le nouveau rapport */
        rapport.setObjet(objet);
        rapport.setSujet(sujet);

        // [UNIREG-3011][UNIREG-3168] Désactivation de la validation causée par le flush
        // du save car nous sommes parfois ici dans une situation intermédiaire invalide
        final boolean validationEnabled = validationInterceptor.isEnabled();
        validationInterceptor.setEnabled(false);
        try {
            rapport = tiersDAO.save(rapport);

            /* ajoute le rapport dans les collections qui vont bien comme le ferait Hibernate au load */
            objet.addRapportObjet(rapport);
            sujet.addRapportSujet(rapport);
        } finally {
            validationInterceptor.setEnabled(validationEnabled);
        }

        return rapport;
    }

    /**
     * Clôt l'appartenance menageCommun entre les 2 tiers à la date donnée.
     *
     * @param pp            la pp
     * @param menage        le menage
     * @param dateFermeture la date de fermeture du rapport
     */
    @Override
    public void closeAppartenanceMenage(PersonnePhysique pp, MenageCommun menage, RegDate dateFermeture) throws RapportEntreTiersException {
        for (RapportEntreTiers rapportObjet : menage.getRapportsObjet()) {
            if (rapportObjet.getDateFin() == null && rapportObjet.getSujetId().equals(pp.getId()) && !rapportObjet.isAnnule()) {

                final RegDate dateDebutRapport = rapportObjet.getDateDebut();
                if (!RegDateHelper.isAfterOrEqual(dateFermeture, dateDebutRapport, NullDateBehavior.EARLIEST)) {
                    final String msg = String.format("On ne peut fermer le rapport d'appartenance ménage avant sa date de début (%s)", RegDateHelper.dateToDisplayString(dateDebutRapport));
                    throw new RapportEntreTiersException(msg);
                }

                rapportObjet.setDateFin(dateFermeture);

                // on ne sort pas de la boucle au cas où il y aurait plusieurs rapports à fermer
                // (ce serait un cas d'erreur, on est bien d'accord, mais ne pas sortir de la boucle
                // maintenant pourrait permettre de nettoyer un peu en même temps...)
            }
        }
    }

    /**
     * Clôt tous les rapports du tiers.
     *
     * @param pp            la pp
     * @param dateFermeture la date de fermeture du rapport
     */
    @Override
    public void closeAllRapports(PersonnePhysique pp, RegDate dateFermeture) {
        if (pp.getRapportsSujet() != null) {
            for (RapportEntreTiers rapport : pp.getRapportsSujet()) {
                if (rapport.getDateFin() == null && !rapport.isAnnule()) {
                    rapport.setDateFin(dateFermeture);
                }
            }
        }

        if (pp.getRapportsObjet() != null) {
            for (RapportEntreTiers rapport : pp.getRapportsObjet()) {
                if (rapport.getDateFin() == null && !rapport.isAnnule()) {
                    rapport.setDateFin(dateFermeture);
                }
            }
        }
    }

    /**
     * Ajoute un rapport prestation imposable
     *
     * @param sourcier     le sourcier sur lequel le debiteur doit être ajouté
     * @param debiteur     le debiteur à ajouter au sourcier
     * @param dateDebut    la date de début de validité de la relation entre tiers
     * @param dateFin      la date de fin de validité de la relation entre tiers (peut être nulle)
     * @param typeActivite le type d'activite
     * @param tauxActivite le taux d'activite
     * @return le rapport-prestation-imposable avec les références mises-à-jour des objets sauvés
     */
    // TODO(FDE) : ajouter un test dans TiersServiceTest
    @Override
    public RapportPrestationImposable addRapportPrestationImposable(PersonnePhysique sourcier, DebiteurPrestationImposable debiteur,
                                                                    RegDate dateDebut, RegDate dateFin, TypeActivite typeActivite, Integer tauxActivite) {

        RapportPrestationImposable rapport = new RapportPrestationImposable();
        rapport.setDateDebut(dateDebut);
        rapport.setDateFin(dateFin);
        rapport.setTypeActivite(typeActivite);
        rapport.setTauxActivite(tauxActivite);

        return (RapportPrestationImposable) addRapport(rapport, sourcier, debiteur);
    }

    /**
     * Crée et sauvegarde en base un ménage-commun avec ces deux parties.
     *
     * @param tiers1    un tiers du ménage-commun
     * @param tiers2    l'autre tiers du ménage-commun (peut être nul)
     * @param dateDebut la date de début de validité de la relation entre tiers
     * @param dateFin   la date de fin de validité de la relation entre tiers (peut être nulle)
     * @return l'ensemble tiers-couple sauvé en base avec les références mises-à-jour des objets sauvés.
     */
    @Override
    public EnsembleTiersCouple createEnsembleTiersCouple(PersonnePhysique tiers1, PersonnePhysique tiers2, RegDate dateDebut,
                                                         RegDate dateFin) {

        Assert.notNull(tiers1);
        Assert.notNull(dateDebut);

        // Création du ménage et de la relation avec le premier tiers
        MenageCommun menage = new MenageCommun();
        RapportEntreTiers rapport = addTiersToCouple(menage, tiers1, dateDebut, dateFin);
        menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
        tiers1 = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());

        // Création de la relation avec le second tiers
        if (tiers2 != null) {
            rapport = addTiersToCouple(menage, tiers2, dateDebut, dateFin);
            menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
            tiers2 = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
        }

        // Distinction principal/conjoint
        PersonnePhysique principal = getPrincipal(tiers1, tiers2);
        PersonnePhysique conjoint = (principal == tiers1 ? tiers2 : tiers1);

        return new EnsembleTiersCouple(menage, principal, conjoint);
    }

	/**
	 * Retourne les nom et prénoms de la personne physique spécifiée
	 *
	 * @param pp personne physique dont on veut le nom
	 * @return une pair composée du (ou des) prénom(s) (premier élément) et du nom (deuxième élément) de la personne physique ( ou {@link NomPrenom#VIDE} si la donnée est inconnue)
	 */
	@Override
	public NomPrenom getDecompositionNomPrenom(PersonnePhysique pp) {
		if (pp.isHabitantVD()) {
			final Individu individu = getIndividu(pp);
			if (individu == null) {
				throw new IndividuNotFoundException(pp.getNumeroIndividu());
			}
			return serviceCivilService.getDecompositionNomPrenom(individu);
		}
		else {
			return new NomPrenom(pp.getNom(), pp.getPrenom());
		}
	}

	/**
     * Récupère l'individu correspondant au tiers spécifié.
     */
    @Override
    public Individu getIndividu(@NotNull PersonnePhysique personne) {

        if (personne.getNumeroIndividu() != null && personne.getNumeroIndividu() != 0) {
            Individu individu = (Individu) personne.getIndividuCache();
            if (individu == null) {

                Long noIndividu = personne.getNumeroIndividu();
                if (noIndividu != null) {
                    individu = serviceCivilService.getIndividu(noIndividu, null);
                    personne.setIndividuCache(individu);
                }
            }
            return individu;
        }
        return null;
    }

    @Override
    public Individu getIndividu(PersonnePhysique personne, RegDate date, @Nullable AttributeIndividu... attributes) {

        if (personne.isHabitantVD()) {
            Individu individu = null;
            Long noIndividu = personne.getNumeroIndividu();
            if (noIndividu != null) {
                individu = getServiceCivilService().getIndividu(noIndividu, date, attributes);
            }
            return individu;
        }
        return null;
    }

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilRegPPDAO(EvenementCivilRegPPDAO evenementCivilRegPPDAO) {
		this.evenementCivilRegPPDAO = evenementCivilRegPPDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilEchDAO(EvenementCivilEchDAO evenementCivilEchDAO) {
		this.evenementCivilEchDAO = evenementCivilEchDAO;
	}

	private ForFiscalPrincipal reopenForFiscalPrincipal(ForFiscalPrincipal forFiscalPrincipal, boolean changeHabitantFlag) {
        forFiscalPrincipal.setDateFin(null);
        forFiscalPrincipal.setMotifFermeture(null);
        return openOrReopenForFiscalPrincipal(forFiscalPrincipal, changeHabitantFlag);
    }

    private ForDebiteurPrestationImposable reopenForDebiteur(ForDebiteurPrestationImposable forDebiteur) {
        forDebiteur.setDateFin(null);
        return forDebiteur;
    }

    /**
     * Ouvre un nouveau for fiscal principal sur un contribuable.
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @param modeImposition           le mode d'imposition du for fiscal principal
     * @param motifOuverture           le motif d'ouverture
     * @param changeHabitantFlag       pour indiquer si le flag habitant doit être mis à jour lors de l'opération.
     * @return le nouveau for fiscal principal
     */
    @Override
    public ForFiscalPrincipal openForFiscalPrincipal(Contribuable contribuable, final RegDate dateOuverture,
                                                     MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
                                                     ModeImposition modeImposition, MotifFor motifOuverture, boolean changeHabitantFlag) {

        Assert.isNull(contribuable.getForFiscalPrincipalAt(null), "Le contribuable possède déjà un for principal ouvert");

        // Ouvre un nouveau for à la date d'événement

        ForFiscalPrincipal nouveauForFiscal = new ForFiscalPrincipal();
        nouveauForFiscal.setDateDebut(dateOuverture);
        nouveauForFiscal.setMotifRattachement(motifRattachement);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal.setModeImposition(modeImposition);
        nouveauForFiscal.setMotifOuverture(motifOuverture);
        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            afterForFiscalPrincipalAdded(contribuable, nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);

        return openOrReopenForFiscalPrincipal(nouveauForFiscal, changeHabitantFlag);

    }

    /**
     * Ouvre et ferme un nouveau for fiscal principal sur un contribuable .
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @param modeImposition           le mode d'imposition du for fiscal principal
     * @param motifOuverture           le motif d'ouverture
     * @param dateFermeture            la date de fermeture du for	 *
     * @param motifFermeture           le motif de fermeture
     * @param changeHabitantFlag
     * @return le nouveau for fiscal principal
     */
    @Override
    public ForFiscalPrincipal openAndCloseForFiscalPrincipal(Contribuable contribuable, final RegDate dateOuverture,
                                                             MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
                                                             ModeImposition modeImposition, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
                                                             boolean changeHabitantFlag) {


        // Ouvre un nouveau for à la date d'événement

        ForFiscalPrincipal nouveauForFiscal = new ForFiscalPrincipal();
        nouveauForFiscal.setDateDebut(dateOuverture);
        nouveauForFiscal.setMotifRattachement(motifRattachement);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal.setModeImposition(modeImposition);
        nouveauForFiscal.setMotifOuverture(motifOuverture);
        nouveauForFiscal.setDateFin(dateFermeture);
        nouveauForFiscal.setMotifFermeture(motifFermeture);

        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            afterForFiscalPrincipalAdded(contribuable, nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        nouveauForFiscal = openOrReopenForFiscalPrincipal(nouveauForFiscal, changeHabitantFlag);
        nouveauForFiscal = closeForFiscalPrincipal(contribuable, nouveauForFiscal, dateFermeture, motifFermeture);


        return nouveauForFiscal;

    }

    public ForDebiteurPrestationImposable openAndCloseForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, RegDate dateFermeture, int numeroOfsAutoriteFiscale,
                                                                                     TypeAutoriteFiscale typeAutoriteFiscale) {
        // Ouvre un nouveau for à la date d'événement
        ForDebiteurPrestationImposable nouveauForFiscal = new ForDebiteurPrestationImposable();
        nouveauForFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
        nouveauForFiscal.setDateDebut(dateOuverture);
        nouveauForFiscal.setDateFin(dateFermeture);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal = tiersDAO.addAndSave(debiteur, nouveauForFiscal);

        if (validationService.validate(debiteur).errorsCount() == 0) {
            afterForDebiteurPrestationImposableAdded(debiteur, nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        nouveauForFiscal = closeForDebiteurPrestationImposable(debiteur, nouveauForFiscal, dateFermeture, true);
        return nouveauForFiscal;

    }


    private ForFiscalPrincipal openOrReopenForFiscalPrincipal(ForFiscalPrincipal forFP, boolean changeHabitantFlag) {

        if (!changeHabitantFlag) {
            return forFP;
        }

        //si (re)ouverture d'un for non vaudois et PP.isHabitantVD faire devenir la PP non habitant
        if (forFP.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
            if (forFP.getTiers() instanceof PersonnePhysique) {
                PersonnePhysique pp = (PersonnePhysique) forFP.getTiers();
                if (pp.isHabitantVD()) {
                    changeHabitantenNH(pp);
                }
            } else if (forFP.getTiers() instanceof MenageCommun) {
                MenageCommun menage = (MenageCommun) forFP.getTiers();
                EnsembleTiersCouple ensemble = getEnsembleTiersCouple(menage, null);
                PersonnePhysique principal = (ensemble == null) ? null : ensemble.getPrincipal();
                if (principal != null && principal.isHabitantVD()) {
                    changeHabitantEnNHSiDomicilieHorsDuCanton(principal);
                }
                PersonnePhysique second = (ensemble == null) ? null : ensemble.getConjoint();
                if (second != null && second.isHabitantVD()) {
                    changeHabitantEnNHSiDomicilieHorsDuCanton(second);
                }
            }
        }
        //Si (re)ouverture d'un for vaudois et !PP.isHabitantVD et PP.numInd not null refaire devenir la PP habitante
        if (forFP.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
            if (forFP.getTiers() instanceof PersonnePhysique) {
                final PersonnePhysique pp = (PersonnePhysique) forFP.getTiers();
                if (!pp.isHabitantVD() && pp.getNumeroIndividu() != null && pp.getNumeroIndividu() != 0) {
                    changeNHenHabitant(pp, pp.getNumeroIndividu(), forFP.getDateDebut());
                }
            } else if (forFP.getTiers() instanceof MenageCommun) {
                final MenageCommun menage = (MenageCommun) forFP.getTiers();
                final EnsembleTiersCouple ensemble = getEnsembleTiersCouple(menage, null);
                final PersonnePhysique principal = (ensemble == null) ? null : ensemble.getPrincipal();
                changeNHEnHabitantSiDomicilieDansLeCanton(principal, forFP.getDateDebut());
                final PersonnePhysique conjoint = (ensemble == null) ? null : ensemble.getConjoint();
                changeNHEnHabitantSiDomicilieDansLeCanton(conjoint, forFP.getDateDebut());
            }
        }

        return forFP;
    }

    @Override
    public boolean changeHabitantEnNHSiDomicilieHorsDuCanton(PersonnePhysique pp) {
        boolean change = false;
        if (pp != null && pp.isHabitantVD() && !isDecede(pp)) {
            // on doit vérifier l'adresse de domicile du contribuable,
            // et ne le passer en non-habitant que si cette adresse n'est pas vaudoise...
            final Boolean isDomicileVaudois = isDomicileDansLeCanton(pp, null);
            if (isDomicileVaudois != null && !isDomicileVaudois) {
                changeHabitantenNH(pp);
                change = true;
            }
        }
        return change;
    }

    @Override
    public boolean isDomicileVaudois(PersonnePhysique pp, @Nullable RegDate date) {
        final Boolean isVaudois = isDomicileDansLeCanton(pp, date);
        return isVaudois != null && isVaudois;
    }

    /**
     * @param pp   une personne physique
     * @param date une date de réféence
     * @return <b>true</b> si l'adresse de domicile de la personne donnée à la date donnée est dans le canton; <b>false</b> si elle est hors-canton où hors-Suisse. Retourne <code>null</code> si on ne
     *         sait pas répondre de manière définitive (pas d'adresse de domicile connue, erreurs...)
     */
    private Boolean isDomicileDansLeCanton(PersonnePhysique pp, @Nullable RegDate date) {

        try {
            final AdresseGenerique adresseDomicile = adresseService.getAdresseFiscale(pp, TypeAdresseFiscale.DOMICILE, date, false);
            if (adresseDomicile != null) {
                final Commune commune = serviceInfra.getCommuneByAdresse(adresseDomicile, date);
                if (commune != null && commune.isVaudoise()) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }
        } catch (AdresseException e) {
            // rien à faire...
            LOGGER.warn("Impossible de déterminer l'adresse de domicile du tiers " + pp.getNumero(), e);
        } catch (ServiceInfrastructureException e) {
            // rien à faire...
            LOGGER.warn("Impossible de déterminer la commune de l'adresse de domicile du tiers " + pp.getNumero(), e);
        }

        return null;
    }

    @Override
    public PersonnePhysique getPere(PersonnePhysique pp, RegDate dateValidite) {
        if (pp == null || !pp.isConnuAuCivil()) {
            return null;
        }

        final Long numeroIndividu = pp.getNumeroIndividu();
        Assert.notNull(numeroIndividu);

        final Individu individu = serviceCivilService.getIndividu(numeroIndividu, dateValidite, AttributeIndividu.PARENTS);
        if (individu == null) {
            throw new IndividuNotFoundException(numeroIndividu);
        }

        PersonnePhysique pere = null;

        final List<RelationVersIndividu> relParents = individu.getParents();
        if (relParents != null) {
            for (RelationVersIndividu relation : relParents) {
                final Individu parent = serviceCivilService.getIndividu(relation.getNumeroAutreIndividu(), dateValidite);
                if (parent == null) {
                    throw new IndividuNotFoundException(relation.getNumeroAutreIndividu());
                }
                if (parent.isSexeMasculin()) {
                    pere = tiersDAO.getPPByNumeroIndividu(parent.getNoTechnique(), true);
                    break;
                }
            }
        }

        return pere;
    }

    @Override
    public PersonnePhysique getMere(PersonnePhysique pp, RegDate dateValidite) {
        if (pp == null || !pp.isConnuAuCivil()) {
            return null;
        }

        final Long numeroIndividu = pp.getNumeroIndividu();
        Assert.notNull(numeroIndividu);

        final Individu individu = serviceCivilService.getIndividu(numeroIndividu, dateValidite, AttributeIndividu.PARENTS);
        if (individu == null) {
            throw new IndividuNotFoundException(numeroIndividu);
        }

        PersonnePhysique mere = null;

        final List<RelationVersIndividu> relParents = individu.getParents();
        if (relParents != null) {
            for (RelationVersIndividu relation : relParents) {
                final Individu parent = serviceCivilService.getIndividu(relation.getNumeroAutreIndividu(), dateValidite);
                if (parent == null) {
                    throw new IndividuNotFoundException(relation.getNumeroAutreIndividu());
                }
                if (!parent.isSexeMasculin()) {
                    mere = tiersDAO.getPPByNumeroIndividu(parent.getNoTechnique(), true);
                    break;
                }
            }
        }

        return mere;
    }

    @Override
    public List<PersonnePhysique> getParents(PersonnePhysique pp, RegDate dateValidite) {
        if (pp == null || !pp.isConnuAuCivil()) {
            return Collections.emptyList();
        }

        final Long numeroIndividu = pp.getNumeroIndividu();
        Assert.notNull(numeroIndividu);

        final Individu individu = serviceCivilService.getIndividu(numeroIndividu, dateValidite, AttributeIndividu.PARENTS);
        if (individu == null) {
            throw new IndividuNotFoundException(numeroIndividu);
        }

        final List<PersonnePhysique> parents = new ArrayList<PersonnePhysique>(2);

        final List<RelationVersIndividu> relParents = individu.getParents();
        if (relParents != null) {
            for (RelationVersIndividu relation : relParents) {
                final Individu individuParent = serviceCivilService.getIndividu(relation.getNumeroAutreIndividu(), dateValidite);
                if (individuParent == null) {
                    throw new IndividuNotFoundException(relation.getNumeroAutreIndividu());
                }
                final PersonnePhysique parent = tiersDAO.getPPByNumeroIndividu(individuParent.getNoTechnique(), true);
                if (parent != null) {
                    parents.add(parent);
                }
            }
        }

        return parents;
    }

    @Override
    public List<PersonnePhysique> getEnfants(PersonnePhysique pp, RegDate dateValidite) {
        if (pp == null || !pp.isConnuAuCivil()) {
            return Collections.emptyList();
        }

        final Long numeroIndividu = pp.getNumeroIndividu();
        Assert.notNull(numeroIndividu);

        final Individu individu = serviceCivilService.getIndividu(numeroIndividu, dateValidite, AttributeIndividu.ENFANTS, AttributeIndividu.ADOPTIONS);
        if (individu == null) {
            throw new IndividuNotFoundException(numeroIndividu);
        }

        final List<PersonnePhysique> enfants = new ArrayList<PersonnePhysique>();
        if (individu.getEnfants() != null) {
            for (RelationVersIndividu rel : individu.getEnfants()) {
                final PersonnePhysique enfant = tiersDAO.getPPByNumeroIndividu(rel.getNumeroAutreIndividu(), true);
                if (enfant != null) {
                    enfants.add(enfant);
                }
            }
        }
        if (individu.getAdoptionsReconnaissances() != null) {
            for (AdoptionReconnaissance adopt : individu.getAdoptionsReconnaissances()) {
                final RegDate debutLien = RegDateHelper.maximum(adopt.getDateAdoption(), adopt.getDateReconnaissance(), NullDateBehavior.EARLIEST);
                final DateRange dureeLien = new DateRangeHelper.Range(debutLien, adopt.getDateDesaveu());
                if (dureeLien.isValidAt(dateValidite)) {
                    final PersonnePhysique enfant = tiersDAO.getPPByNumeroIndividu(adopt.getAdopteReconnu().getNoTechnique(), true);
                    if (enfant != null) {
                        enfants.add(enfant);
                    }
                }
            }
        }

        return enfants;
    }

    @Override
    public List<PersonnePhysique> getEnfants(MenageCommun mc, RegDate dateValidite) {
        EnsembleTiersCouple ensembleTiersCouple = getEnsembleTiersCouple(mc, dateValidite);
        PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
        Set<PersonnePhysique> setEnfantsMenage = new HashSet<PersonnePhysique>();
        PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
        setEnfantsMenage.addAll(getEnfants(principal, dateValidite));
        if (conjoint != null) {
            setEnfantsMenage.addAll(getEnfants(conjoint, dateValidite));
        }
        return new ArrayList<PersonnePhysique>(setEnfantsMenage);
    }

    @Override
    public List<PersonnePhysique> getEnfants(Contribuable ctb, RegDate dateValidite) {
        if (ctb instanceof MenageCommun) {
            return getEnfants((MenageCommun) ctb, dateValidite);
        } else if (ctb instanceof PersonnePhysique) {
            return getEnfants((PersonnePhysique) ctb, dateValidite);
        } else {
            return Collections.emptyList();
        }

    }

    @Override
    public List<PersonnePhysique> getEnfantsForDeclaration(Contribuable ctb, RegDate finPeriodeImposition) {
        final List<PersonnePhysique> listeEnfants = new ArrayList<PersonnePhysique>();
        final List<PersonnePhysique> listeRecherche = getEnfants(ctb, finPeriodeImposition);
        if (!listeRecherche.isEmpty()) {

            // warm-up du cache individu avec adresses
            final List<Long> noTiersEnfants = new ArrayList<Long>(listeRecherche.size());
            for (PersonnePhysique enfant : listeRecherche) {
                noTiersEnfants.add(enfant.getNumero());
            }
            serviceCivilCacheWarmer.warmIndividusPourTiers(noTiersEnfants, finPeriodeImposition, AttributeIndividu.ADRESSES);

            for (PersonnePhysique enfant : listeRecherche) {
                final RegDate dateDeces = getDateDeces(enfant);
                //enfant non Décédé
                if (dateDeces == null || dateDeces.isAfter(finPeriodeImposition)) {
                    //Enfant mineur à la date de fin de la periode d'imposition
                    if (isMineur(enfant, finPeriodeImposition)) {
                        //L'adresse de domicile (EGID) doit être la même entre l'enfant et le contribuable
                        if (isEgidCoherent(ctb, enfant, finPeriodeImposition)) {
                            listeEnfants.add(enfant);
                        }
                    }
                }
            }
        }

        //[SIFISC-2703] tri des enfants par date de naissance croissante
        Collections.sort(listeEnfants, new Comparator<PersonnePhysique>() {
            @Override
            public int compare(PersonnePhysique o1, PersonnePhysique o2) {
                final RegDate dateNaissance1 = getDateNaissance(o1);
                final RegDate dateNaissance2 = getDateNaissance(o2);
                if (dateNaissance1 == null && dateNaissance2 == null) {
                    return 0;
                } else if (dateNaissance1 == null) {
                    return -1;
                } else if (dateNaissance2 == null) {
                    return 1;
                } else {
                    return dateNaissance1.compareTo(dateNaissance2);
                }
            }
        });
        return listeEnfants;
    }

    /**
     * L’EGID de l’adresse domicile doit être le même entre l’enfant et le CTB à la fin de la période d’imposition. Dans le cadre d’un ménage commun, il faut que chacun des deux parents ait un EGID
     * identique à celui de l’enfant. Si deux membres d’un ménage commun habitent à des adresses vaudoises (EGID) différentes, l’enfant ne doit pas figurer sur la DI. Si les deux parents ne sont pas en
     * ménage commun alors qu’ils ont le même EGID, l’enfant ne doit figurer sur aucune des deux DI.
     *
     * @param parent               le contribuable  <i>PersonnePhysique</i> ou <i>MenageCommun</i>
     * @param enfant               qui est succeptible d'être rajouter sur la DI
     * @param finPeriodeImposition date de fin de période d'imposition
     * @return <i>TRUE</i> si les egid sont cohérents, <i>FALSE</i>  des incoherences sont trouvées entre l'egid de l'enfant et des parents
     */
    private boolean isEgidCoherent(Contribuable parent, PersonnePhysique enfant, RegDate finPeriodeImposition) {
        try {
            final AdresseGenerique adresseDomicileParent = adresseService.getAdresseFiscale(parent, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);
            final AdresseGenerique adresseDomicileEnfant = adresseService.getAdresseFiscale(enfant, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);
            //Les EGID ne peuvent être déterminées, on retourne false
            if (adresseDomicileEnfant == null || adresseDomicileParent == null) {
                return false;
            }

            if (parent instanceof PersonnePhysique) {
                //Si les deux parents ne sont pas en ménage commun alors qu’ils ont le même EGID, l’enfant ne doit figurer sur aucune des deux DI.
                final boolean hasParentsAvecEgidDifferent =
                        TiersHelper.hasParentsAvecEgidDifferent(enfant, (PersonnePhysique) parent, adresseDomicileParent, finPeriodeImposition, adresseService, this);
                if (adresseDomicileParent != null && adresseDomicileEnfant != null && hasParentsAvecEgidDifferent) {
                    //On compare l'egid du parent et de l'enfant
                    return TiersHelper.isSameEgid(adresseDomicileEnfant.getEgid(), adresseDomicileParent.getEgid());
                }

            } else if (parent instanceof MenageCommun) {
                EnsembleTiersCouple ensembleTiersCouple = getEnsembleTiersCouple((MenageCommun) parent, finPeriodeImposition);
                PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
                PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
                if (conjoint != null) {
                    final AdresseGenerique adresseDomicilePrincipal = adresseService.getAdresseFiscale(principal, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);
                    final AdresseGenerique adresseDomicileConjoint = adresseService.getAdresseFiscale(conjoint, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);
                    //Dans le cadre d’un ménage commun, il faut que chacun des deux parents ait un EGID identique à celui de l’enfant.
                    //noinspection SimplifiableIfStatement
                    if (adresseDomicilePrincipal != null && adresseDomicileConjoint != null && TiersHelper.isSameEgid(adresseDomicilePrincipal.getEgid(), adresseDomicileConjoint.getEgid())) {
                        return TiersHelper.isSameEgid(adresseDomicileEnfant.getEgid(), adresseDomicileParent.getEgid());

                    } else {
                        //les deux membres du ménage commun habitent a deux endroits différents
                        return false;
                    }
                } else {
                    //Marié(e) seul(e)
                    return TiersHelper.isSameEgid(adresseDomicileEnfant.getEgid(), adresseDomicileParent.getEgid());
                }

            }

        } catch (AdresseException e) {
            // rien à faire...
            LOGGER.warn("Test de la coherence d'egid: Impossible de déterminer l'adresse des tiers parent " + parent.getNumero() + " et enfant " + enfant.getNumero(), e);
        }
        //Dans tous les autres cas, on retourne faux, on ne prend pas de risque
        return false;
    }

    @NotNull
    @Override
    public List<RapportFiliation> getRapportsFiliation(PersonnePhysique personnePhysique) {

        final List<RapportFiliation> filiations = new ArrayList<RapportFiliation>();
        if (!personnePhysique.isConnuAuCivil()) {
            return Collections.emptyList();
        }

        final RegDate date = RegDate.get();

        final AttributeIndividu[] enumValues = new AttributeIndividu[]{AttributeIndividu.ENFANTS, AttributeIndividu.PARENTS, AttributeIndividu.ADOPTIONS};
        final Individu ind = serviceCivilService.getIndividu(personnePhysique.getNumeroIndividu(), date, enumValues);

        // enfants biologiques
        final Collection<RelationVersIndividu> enfants = ind.getEnfants();
        if (enfants != null) {
            for (RelationVersIndividu enfant : enfants) {
                RapportFiliation r = createRapportPourEnfant(personnePhysique, ind, enfant);
                if (r != null) {
                    filiations.add(r);
                }
            }
        }

        // enfants adoptés / reconnus
        final Collection<AdoptionReconnaissance> adoptions = ind.getAdoptionsReconnaissances();
        if (adoptions != null) {
            for (AdoptionReconnaissance ar : adoptions) {
                RapportFiliation r = createRapportPourAdoption(personnePhysique, ind, ar);
                if (r != null) {
                    filiations.add(r);
                }
            }
        }

        // parents
        final List<RelationVersIndividu> parents = ind.getParents();
        if (parents != null) {
            for (RelationVersIndividu parent : parents) {
                RapportFiliation r = createRapportPourParents(personnePhysique, ind, parent, date);
                if (r != null) {
                    filiations.add(r);
                }
            }
        }

		if (LOGGER.isDebugEnabled()) {
			final String message = "Personne physique n°" + personnePhysique.getNumero() + " / individu n°" + personnePhysique.getNumeroIndividu() + " :\n" +
					"  - " + size(ind.getEnfants()) + " enfant(s)\n" +
					"  - " + size(ind.getAdoptionsReconnaissances()) + " adoptés(s)\n" +
					"  - " + size(ind.getParents()) + " parent(s)\n" +
					" => total " + size(filiations) + " liens de filiation générés.";
			LOGGER.debug(message);
		}

        return filiations;
    }

	private static int size(Collection<?> coll) {
		return coll == null ? 0 : coll.size();
	}

	@Nullable
	private RapportFiliation createRapportPourEnfant(PersonnePhysique personnePhysique, Individu individu, RelationVersIndividu relationEnfant) {

        final long noIndEnfant = relationEnfant.getNumeroAutreIndividu();

        final PersonnePhysique ppEnfant = tiersDAO.getPPByNumeroIndividu(noIndEnfant);
        if (ppEnfant == null) {
	        LOGGER.warn("Impossible de trouver la personne physique qui correspond à l'individu enfant n°" + noIndEnfant);
        }

        final Individu enfant = serviceCivilService.getIndividu(noIndEnfant, null);
        if (enfant == null) {
            throw new IndividuNotFoundException(noIndEnfant);
        }

        return new RapportFiliation(individu, personnePhysique, enfant, ppEnfant, RapportFiliation.Type.ENFANT);
    }

    @Nullable
    private RapportFiliation createRapportPourAdoption(PersonnePhysique personnePhysique, Individu individu, AdoptionReconnaissance ar) {

        final Individu enfant = ar.getAdopteReconnu();

        final PersonnePhysique ppEnfant = tiersDAO.getPPByNumeroIndividu(enfant.getNoTechnique());
        if (ppEnfant == null) {
	        LOGGER.warn("Impossible de trouver la personne physique qui correspond à l'individu enfant n°" + enfant.getNoTechnique());
        }

        final RapportFiliation rapportView = new RapportFiliation(individu, personnePhysique, enfant, ppEnfant, RapportFiliation.Type.ENFANT);

        final RegDate dateDebut = RegDateHelper.maximum(ar.getDateAdoption(), ar.getDateReconnaissance(), NullDateBehavior.EARLIEST);
        if (dateDebut != null) {
            rapportView.setDateDebut(dateDebut);
        }
        if (ar.getDateDesaveu() != null) {
            rapportView.setDateFin(ar.getDateDesaveu());
        }

        return rapportView;
    }

    @Nullable
    private RapportFiliation createRapportPourParents(PersonnePhysique personnePhysique, Individu individu, RelationVersIndividu relationParent, RegDate date) {

        final long noIndParent = relationParent.getNumeroAutreIndividu();

        final PersonnePhysique ppParent = tiersDAO.getPPByNumeroIndividu(noIndParent);
        if (ppParent == null) {
	        LOGGER.warn("Impossible de trouver la personne physique qui correspond à l'individu parent n°" + noIndParent);
            return null;
        }

        final Individu parent = serviceCivilService.getIndividu(noIndParent, date, AttributeIndividu.ADOPTIONS);
        if (parent == null) {
            throw new IndividuNotFoundException(noIndParent);
        }

        final RapportFiliation rapportView = new RapportFiliation(individu, personnePhysique, parent, ppParent, RapportFiliation.Type.PARENT);

        final AdoptionReconnaissance ar = getAdoptionPourEnfant(parent.getAdoptionsReconnaissances(), personnePhysique.getNumeroIndividu());
        if (ar != null) {
            final RegDate dateDebut = RegDateHelper.maximum(ar.getDateAdoption(), ar.getDateReconnaissance(), NullDateBehavior.EARLIEST);
            if (dateDebut != null) {
                rapportView.setDateDebut(dateDebut);
            }
            if (ar.getDateDesaveu() != null && (rapportView.getDateFin() == null || ar.getDateDesaveu().isBefore(rapportView.getDateFin()))) {
                rapportView.setDateFin(ar.getDateDesaveu());
            }
        }

        return rapportView;
    }

    private static AdoptionReconnaissance getAdoptionPourEnfant(Collection<AdoptionReconnaissance> adoptions, long noIndEnfant) {
        AdoptionReconnaissance a = null;
        if (adoptions != null && !adoptions.isEmpty()) {
            for (AdoptionReconnaissance candidat : adoptions) {
                if (candidat.getAdopteReconnu().getNoTechnique() == noIndEnfant) {
                    a = candidat;
                    break;
                }
            }
        }
        return a;
    }

    @Override
    public boolean changeNHEnHabitantSiDomicilieDansLeCanton(PersonnePhysique pp, RegDate dateArrivee) {
        boolean change = false;
        if (pp != null && !pp.isHabitantVD() && !isDecede(pp) && pp.getNumeroIndividu() != null && pp.getNumeroIndividu() != 0) {
            // on doit vérifier l'adresse de domicile du contribuable,
            // et ne le passer en habitant que si cette adresse est vaudoise...
            if (isDomicileVaudois(pp, null)) {
                changeNHenHabitant(pp, pp.getNumeroIndividu(), dateArrivee);
                change = true;
            }
        }
        return change;
    }

    private void afterForFiscalPrincipalAdded(Contribuable contribuable, ForFiscalPrincipal forFiscalPrincipal) {
        final MotifFor motifOuverture = forFiscalPrincipal.getMotifOuverture();
        final TypeAutoriteFiscale typeAutoriteFiscale = forFiscalPrincipal.getTypeAutoriteFiscale();
        final RegDate dateOuverture = forFiscalPrincipal.getDateDebut();
        final ModeImposition modeImposition = forFiscalPrincipal.getModeImposition();

        if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
            if (motifOuverture == MotifFor.CHGT_MODE_IMPOSITION || motifOuverture == MotifFor.PERMIS_C_SUISSE) {
                this.evenementFiscalService.publierEvenementFiscalChangementModeImposition(contribuable, dateOuverture, modeImposition, forFiscalPrincipal.getId());
            } else if (motifOuverture == MotifFor.DEMENAGEMENT_VD ||
                    motifOuverture == MotifFor.FUSION_COMMUNES ||
                    motifOuverture == MotifFor.MAJORITE ||
                    motifOuverture == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION ||
                    motifOuverture == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT ||
                    motifOuverture == MotifFor.VEUVAGE_DECES ||
                    motifOuverture == MotifFor.ARRIVEE_HC ||
                    motifOuverture == MotifFor.ARRIVEE_HS) {
                this.evenementFiscalService.publierEvenementFiscalOuvertureFor(contribuable, dateOuverture, motifOuverture, forFiscalPrincipal.getId());
            }

            if (motifOuverture == MotifFor.MAJORITE && contribuable instanceof PersonnePhysique) {
                final PersonnePhysique enfant = (PersonnePhysique) contribuable;
                // [UNIREG-3244] Lorsqu'un premier for principal pour le revenu ou la fortune est ouvert pour un contribuable
                // pour motif de « Majorité », un événement fiscal de type « Fin d'autorité parentale » est engendré
                if (enfant.getForsFiscauxNonAnnules(false).size() == 1) {
                    final Contribuable parent = getAutoriteParentaleDe(enfant, dateOuverture.getOneDayBefore());
                    if (parent != null) {
                        evenementFiscalService.publierEvenementFiscalFinAutoriteParentale(enfant, parent, dateOuverture);
                    }
                }
            }
        }

        // [UNIREG-1373] Un départ HS ajuste la date de fin d'une eventuelle DI libre.
        if (TypeAutoriteFiscale.PAYS_HS == typeAutoriteFiscale && MotifFor.DEPART_HS == motifOuverture) {
            if (forFiscalPrincipal.getDateDebut().year() == RegDate.get().year()) {
                // Le for ouvert est dans la période courante, on verifie que le contribuable n'ait pas une DI libre
                final List<Declaration> dis = contribuable.getDeclarationsForPeriode(RegDate.get().year(), false);
                if (dis != null && !dis.isEmpty()) {
                    Collections.sort(dis, new DateRangeComparator<Declaration>());
                    final Declaration di = dis.get(dis.size() - 1);
                    // Le contribuable a une DI libre, on ajuste la periode d'imposition
                    di.setDateFin(forFiscalPrincipal.getDateDebut());
                }
            }
        }

        final ModeImposition ancienModeImposition;
        if (motifOuverture == MotifFor.CHGT_MODE_IMPOSITION) {
            final ForFiscalPrincipal ancienFfp = contribuable.getForFiscalPrincipalAt(forFiscalPrincipal.getDateDebut().getOneDayBefore());
            if (ancienFfp != null) {
                ancienModeImposition = ancienFfp.getModeImposition();
            } else {
                // bizarre qu'il n'y ait pas de for, non ?
                ancienModeImposition = ModeImposition.SOURCE;
            }
        } else {
            ancienModeImposition = null;
        }

        tacheService.genereTacheDepuisOuvertureForPrincipal(contribuable, forFiscalPrincipal, ancienModeImposition);

        // [UNIREG-2794] déblocage en cas d'ouverture de for fiscal principal vaudois
        resetFlagBlocageRemboursementAutomatiqueSelonFors(contribuable);
    }

    @Override
    public Contribuable getAutoriteParentaleDe(PersonnePhysique contribuableEnfant, RegDate dateValidite) {

        final PersonnePhysique parent = getMere(contribuableEnfant, dateValidite);
        if (parent == null) {
            return null;
        }

        final Contribuable autoriteParentale;
        final EnsembleTiersCouple ensemble = getEnsembleTiersCouple(parent, dateValidite);
        if (ensemble == null) {
            autoriteParentale = parent;
        } else {
            autoriteParentale = ensemble.getMenage();
        }
        return autoriteParentale;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalSecondaire openForFiscalSecondaire(Contribuable contribuable, final RegDate dateOuverture,
                                                       MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
                                                       MotifFor motifOuverture) {
        return addForSecondaire(contribuable, dateOuverture, null, motifRattachement, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifOuverture, null);
    }

    private void afterForFiscalSecondaireAdded(Contribuable contribuable, ForFiscalSecondaire forFiscalSecondaire) {
        // TODO (PBO) à déplacer dans EvenementFiscalService
        if (contribuable.getForFiscalPrincipalAt(forFiscalSecondaire.getDateDebut()).getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
            boolean isFirst = true;
            List<ForFiscal> fors = contribuable.getForsFiscauxValidAt(forFiscalSecondaire.getDateDebut());
            for (ForFiscal forFiscal : fors) {
                if (!forFiscal.isAnnule() && forFiscal instanceof ForFiscalSecondaire && forFiscal != forFiscalSecondaire) {
                    ForFiscalSecondaire forSec = (ForFiscalSecondaire) forFiscal;
                    MotifRattachement motifRattachement = forFiscalSecondaire.getMotifRattachement();
                    if (forSec.getMotifRattachement() == motifRattachement) {
                        isFirst = false;
                        break;
                    }
                }
            }
            // PBO (26-06-2009) ajout des motifs de rattachement pour la génération d'événements fiscaux
            if (isFirst && (forFiscalSecondaire.getMotifRattachement() == MotifRattachement.ACTIVITE_INDEPENDANTE ||
                    forFiscalSecondaire.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE)) {
                MotifFor motifOuverture = forFiscalSecondaire.getMotifOuverture();
                RegDate dateOuverture = forFiscalSecondaire.getDateDebut();
                this.evenementFiscalService.publierEvenementFiscalOuvertureFor(contribuable, dateOuverture, motifOuverture, forFiscalSecondaire.getId());
            }
        }
        tacheService.genereTacheDepuisOuvertureForSecondaire(contribuable, forFiscalSecondaire);
    }

    /**
     * Ouvre un nouveau for fiscal autre élément imposable sur un contribuable.
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param genreImpot               le genre d'impot
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale
     * @param motifOuverture           le motif d'ouverture
     * @return le nouveau for fiscal autre élément imposable
     */
    @Override
    public ForFiscalAutreElementImposable openForFiscalAutreElementImposable(Contribuable contribuable, GenreImpot genreImpot,
                                                                             final RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
                                                                             TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture) {

        // Ouvre un nouveau for à la date d'événement
        ForFiscalAutreElementImposable nouveauForFiscal = new ForFiscalAutreElementImposable();
        nouveauForFiscal.setGenreImpot(genreImpot);
        nouveauForFiscal.setDateDebut(dateOuverture);
        nouveauForFiscal.setMotifRattachement(motifRattachement);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal.setMotifOuverture(motifOuverture);
        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        Assert.notNull(nouveauForFiscal);
        return nouveauForFiscal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalAutreImpot openForFiscalAutreImpot(Contribuable contribuable, GenreImpot genreImpot, final RegDate dateImpot, int numeroOfsAutoriteFiscale,
                                                       TypeAutoriteFiscale typeAutoriteFiscale) {

        // Ouvre un nouveau for à la date d'événement
        ForFiscalAutreImpot nouveauForFiscal = new ForFiscalAutreImpot();
        nouveauForFiscal.setGenreImpot(genreImpot);
        nouveauForFiscal.setDateDebut(dateImpot);
        nouveauForFiscal.setDateFin(dateImpot);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            afterForAutreImportAdded(contribuable, nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        return nouveauForFiscal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForDebiteurPrestationImposable openForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, int numeroOfsAutoriteFiscale,
                                                                             TypeAutoriteFiscale typeAutoriteFiscale) {

        // Ouvre un nouveau for à la date d'événement
        ForDebiteurPrestationImposable nouveauForFiscal = new ForDebiteurPrestationImposable();
        nouveauForFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
        nouveauForFiscal.setDateDebut(dateOuverture);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal = tiersDAO.addAndSave(debiteur, nouveauForFiscal);

        if (validationService.validate(debiteur).errorsCount() == 0) {
            afterForDebiteurPrestationImposableAdded(debiteur, nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        return nouveauForFiscal;
    }

    /**
     * Ré-ouvre les rapports de prestation imposables du débiteur qui ont été fermés à la date de désactivation avec une nouvelle date d'ouverture à la réactivation
     *
     * @param debiteur          le débiteur sur lequel les rapports de prestation imposable doivent être ré-ouverts
     * @param dateDesactivation la date à laquelle les rapports avaient été fermés
     * @param dateReactivation  la date à laquelle les rapports doivent être ré-ouverts
     */
    @Override
    public void reopenRapportsPrestation(DebiteurPrestationImposable debiteur, RegDate dateDesactivation, RegDate dateReactivation) {
        final List<RapportEntreTiers> nouveaux = new ArrayList<RapportEntreTiers>(debiteur.getRapportsObjet().size());
        for (RapportEntreTiers rapport : debiteur.getRapportsObjet()) {
            if (rapport instanceof RapportPrestationImposable && rapport.isValidAt(dateDesactivation) && dateDesactivation.equals(rapport.getDateFin())) {
                final Tiers sourcier = getTiers(rapport.getSujetId());
                final RapportPrestationImposable rpi = new RapportPrestationImposable(dateReactivation, null, (Contribuable) sourcier, debiteur);
                nouveaux.add(rpi);
            }
        }
        debiteur.getRapportsObjet().addAll(nouveaux);
    }

    /**
     * Supprime la date de fermeture des rapports ayant comme date de fin la date passéeé en paramètre
     *
     * @param debiteur          sur lequel les rapports doivent être réouverts
     * @param dateDesactivation la date à laquelle les rapports avaient été fermés
     */
    private void reopenRapportsPrestationImposableFermesAt(DebiteurPrestationImposable debiteur, RegDate dateDesactivation) {
        if (debiteur.getRapportsObjet() != null) {
            for (RapportEntreTiers rapport : debiteur.getRapportsObjet()) {
                if (rapport instanceof RapportPrestationImposable && rapport.isValidAt(dateDesactivation) && dateDesactivation.equals(rapport.getDateFin())) {
                    rapport.setDateFin(null);
                }
            }
        }

    }

    private void afterForDebiteurPrestationImposableAdded(DebiteurPrestationImposable debiteur, ForDebiteurPrestationImposable forDebiteur) {
        RegDate dateOuverture = forDebiteur.getDateDebut();
        this.evenementFiscalService.publierEvenementFiscalOuvertureFor(debiteur, dateOuverture, null, forDebiteur.getId());

    }

    private void afterForAutreImportAdded(Tiers tiers, ForFiscalAutreImpot forFiscal) {
        RegDate dateOuverture = forFiscal.getDateDebut();
        this.evenementFiscalService.publierEvenementFiscalOuvertureFor(tiers, dateOuverture, null, forFiscal.getId());
    }

    /**
     * Réouvre, pour un tiers, tous ses fors fermés à une date donnée et avec le motif de fermeture spécifié si applicable.
     *
     * @param date           la date de fermeture
     * @param motifFermeture le motif de fermeture
     * @param tiers          le tiers pour qui les fors seront réouverts
     */
    @Override
    public void reopenForsClosedAt(RegDate date, MotifFor motifFermeture, Tiers tiers) {
        List<ForFiscal> openFors = new ArrayList<ForFiscal>();
        for (ForFiscal forFiscal : tiers.getForsFiscaux()) {
            if (!forFiscal.isAnnule() && date.equals(forFiscal.getDateFin())) {
                /*
				 *  La remise à nul de la date de fin du For ne génère pas d'événement fiscal.
				 *  La bonne méthode consiste à recréer un nouveau For en reprenant les données de l'ancien,
				 *  annuler ce dernier, mettre la date de fin du nouveau For à nul et l'assigner au tiers.
				 */
                boolean isForFiscalRevenuFortune = forFiscal instanceof ForFiscalRevenuFortune;
                boolean isForFiscalAutreImpot = forFiscal instanceof ForFiscalAutreImpot;

                if (isForFiscalAutreImpot ||
                        (isForFiscalRevenuFortune && motifFermeture == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
                    // Duplication du for
                    ForFiscal nouveauForFiscal = forFiscal.duplicate();
                    // réouvrir le nouveau For
                    nouveauForFiscal.setDateFin(null);
                    if (isForFiscalRevenuFortune) {
                        ((ForFiscalRevenuFortune) nouveauForFiscal).setMotifFermeture(null);
                    }
                    // annuler l'ancien For
                    forFiscal.setAnnule(true);
                    openFors.add(nouveauForFiscal);
                }
            }
        }

        // assigner les nouveaux Fors au tiers
        // d'abord les fors principaux, puis les fors secondaires [UNIREG-1832]
        addAndSaveReopenedFors(tiers, openFors, true);
        addAndSaveReopenedFors(tiers, openFors, false);
    }

    /**
     * @param tiers          tiers auquel les fors sont associés
     * @param reopenedFors   listes des fors à sauver
     * @param forsPrincipaux si <code>true</code>, ne s'occupe que des fors principaux, et si <code>false</code>, que des fors secondaires
     */
    private void addAndSaveReopenedFors(Tiers tiers, List<ForFiscal> reopenedFors, boolean forsPrincipaux) {
        for (ForFiscal reopenedFor : reopenedFors) {
            if ((forsPrincipaux && reopenedFor instanceof ForFiscalPrincipal) || (!forsPrincipaux && reopenedFor instanceof ForFiscalSecondaire)) {
                reopenedFor = tiersDAO.addAndSave(tiers, reopenedFor);
                // exécution des règles événements fiscaux
                if (validationService.validate(tiers).errorsCount() == 0) {
                    afterForAdded(reopenedFor);
                }
            }
        }
    }

    /**
     * Réouvre le for et l'assigne au tiers.
     *
     * @param ff
     * @param tiers
     */
    @Override
    public void reopenFor(ForFiscal ff, Tiers tiers) {
        ForFiscal nouveauForFiscal = ff.duplicate();
        nouveauForFiscal.setAnnule(false);
        nouveauForFiscal.setDateFin(null);
        nouveauForFiscal = tiersDAO.addAndSave(tiers, nouveauForFiscal);
        // exécution des règles événements fiscaux
        if (validationService.validate(tiers).errorsCount() == 0) {
            afterForAdded(nouveauForFiscal);
        }
    }

    private void afterForAdded(ForFiscal forFiscal) {
        if (forFiscal instanceof ForFiscalPrincipal) {
            afterForFiscalPrincipalAdded((Contribuable) forFiscal.getTiers(), (ForFiscalPrincipal) forFiscal);
        } else if (forFiscal instanceof ForFiscalSecondaire) {
            afterForFiscalSecondaireAdded((Contribuable) forFiscal.getTiers(), (ForFiscalSecondaire) forFiscal);
        } else if (forFiscal instanceof ForDebiteurPrestationImposable) {
            afterForDebiteurPrestationImposableAdded((DebiteurPrestationImposable) forFiscal.getTiers(), (ForDebiteurPrestationImposable) forFiscal);
        } else if (forFiscal instanceof ForFiscalAutreImpot) {
            afterForAutreImportAdded(forFiscal.getTiers(), (ForFiscalAutreImpot) forFiscal);
        }
        // ajouter d'autres si necessaire
    }

    /**
     * Ferme le for fiscal principal d'un contribuable.
     *
     * @param contribuable  le contribuable concerné
     * @param dateFermeture la date de fermeture du for
     * @return le for fiscal principal fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    @Override
    public ForFiscalPrincipal closeForFiscalPrincipal(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {
        ForFiscalPrincipal forFiscalPrincipal = contribuable.getForFiscalPrincipalAt(null);
        if (forFiscalPrincipal != null) {
            forFiscalPrincipal = closeForFiscalPrincipal(contribuable, forFiscalPrincipal, dateFermeture, motifFermeture);
        }

        return forFiscalPrincipal;
    }

    /**
     * Ferme le for fiscal principal d'un contribuable.
     *
     * @param forFiscalPrincipal le for fiscal principal concerné
     * @param dateFermeture      la date de fermeture du for
     * @return le for fiscal principal fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    @Override
    public ForFiscalPrincipal closeForFiscalPrincipal(ForFiscalPrincipal forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture) {
        if (forFiscalPrincipal != null) {
            forFiscalPrincipal = closeForFiscalPrincipal((Contribuable) forFiscalPrincipal.getTiers(), forFiscalPrincipal, dateFermeture, motifFermeture);
        }

        return forFiscalPrincipal;
    }

    protected ForFiscalPrincipal closeForFiscalPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture) {
        Assert.notNull(contribuable);
        Assert.notNull(forFiscalPrincipal);
        if (forFiscalPrincipal.getDateDebut().isAfter(dateFermeture)) {
            throw new ValidationException(forFiscalPrincipal, "La date de fermeture (" + RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début (" +
                    RegDateHelper.dateToDisplayString(forFiscalPrincipal.getDateDebut())
                    + ") du for fiscal actif");
        }

        forFiscalPrincipal.setDateFin(dateFermeture);
        forFiscalPrincipal.setMotifFermeture(motifFermeture);

        afterForFiscalPrincipalClosed(contribuable, forFiscalPrincipal, dateFermeture, motifFermeture);

        return forFiscalPrincipal;
    }

    private void afterForFiscalPrincipalClosed(Contribuable contribuable, ForFiscalPrincipal forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture) {

        if (forFiscalPrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
            if (motifFermeture == MotifFor.DEPART_HC ||
                    motifFermeture == MotifFor.DEPART_HS ||
                    motifFermeture == MotifFor.VEUVAGE_DECES ||
                    motifFermeture == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION ||
                    motifFermeture == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT) {
                evenementFiscalService.publierEvenementFiscalFermetureFor(contribuable, dateFermeture, motifFermeture, forFiscalPrincipal.getId());
            }
        }

        tacheService.genereTacheDepuisFermetureForPrincipal(contribuable, forFiscalPrincipal);
        resetFlagBlocageRemboursementAutomatiqueSelonFors(contribuable);
    }

    /**
     * Ferme le for fiscal secondaire d'un contribuable.
     *
     * @param contribuable   le contribuable concerné
     * @param dateFermeture  la date de fermeture du for
     * @param motifFermeture la motif de fermeture du for
     * @return le for fiscal secondaire fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    @Override
    public ForFiscalSecondaire closeForFiscalSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscalSecondaire,
                                                        RegDate dateFermeture, MotifFor motifFermeture) {
        if (forFiscalSecondaire != null) {
            if (forFiscalSecondaire.getDateDebut().isAfter(dateFermeture)) {
                throw new ValidationException(forFiscalSecondaire, "La date de fermeture ("
                        + RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
                        + RegDateHelper.dateToDisplayString(forFiscalSecondaire.getDateDebut()) + ") du for fiscal actif");
            }
            forFiscalSecondaire.setDateFin(dateFermeture);
            forFiscalSecondaire.setMotifFermeture(motifFermeture);
            afterForFiscalSecondaireClosed(contribuable, forFiscalSecondaire, dateFermeture, motifFermeture);
        }

        return forFiscalSecondaire;
    }

    private void afterForFiscalSecondaireClosed(Contribuable contribuable, ForFiscalSecondaire forFiscalSecondaire, RegDate dateFermeture, MotifFor motifFermeture) {
        if (contribuable.getForFiscalPrincipalAt(forFiscalSecondaire.getDateFin()).getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
            boolean isLast = true;
            List<ForFiscal> fors = contribuable.getForsFiscauxValidAt(forFiscalSecondaire.getDateFin());
            for (ForFiscal forFiscal : fors) {
                if (forFiscal instanceof ForFiscalSecondaire) {
                    ForFiscalSecondaire forSec = (ForFiscalSecondaire) forFiscal;
                    if (forSec.getMotifRattachement() == forFiscalSecondaire.getMotifRattachement()) {
                        isLast = false;
                        break;
                    }
                }
            }
            if (isLast) {
                this.evenementFiscalService.publierEvenementFiscalFermetureFor(contribuable, dateFermeture,
                        motifFermeture, forFiscalSecondaire.getId());
            }
        }
        tacheService.genereTacheDepuisFermetureForSecondaire(contribuable, forFiscalSecondaire);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalPrincipal changeModeImposition(Contribuable contribuable, RegDate dateChangementModeImposition, ModeImposition modeImposition, MotifFor motifFor) {

        final ForFiscalPrincipal forFiscalPrincipal = contribuable.getForFiscalPrincipalAt(null);
        Assert.notNull(forFiscalPrincipal);

        //Fermeture du for principal précédent
        if (forFiscalPrincipal.getDateDebut().isAfter(dateChangementModeImposition.getOneDayBefore())) {
            throw new ValidationException(forFiscalPrincipal, "La date de changement de mode d'imposition ("
                    + RegDateHelper.dateToDisplayString(dateChangementModeImposition.getOneDayBefore())
                    + ") est avant la date de début (" + RegDateHelper.dateToDisplayString(forFiscalPrincipal.getDateDebut())
                    + ") du for fiscal actif");
        }

        forFiscalPrincipal.setDateFin(dateChangementModeImposition.getOneDayBefore());
        forFiscalPrincipal.setMotifFermeture(motifFor);
        tacheService.genereTacheDepuisFermetureForPrincipal(contribuable, forFiscalPrincipal);

        //Ouverture d'un nouveau for principal
        ForFiscalPrincipal nouveauForFiscal = new ForFiscalPrincipal();
        nouveauForFiscal.setDateDebut(dateChangementModeImposition);
        nouveauForFiscal.setMotifRattachement(forFiscalPrincipal.getMotifRattachement());
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(forFiscalPrincipal.getNumeroOfsAutoriteFiscale());
        nouveauForFiscal.setTypeAutoriteFiscale(forFiscalPrincipal.getTypeAutoriteFiscale());
        nouveauForFiscal.setModeImposition(modeImposition);
        nouveauForFiscal.setMotifOuverture(motifFor);
        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            tacheService.genereTacheDepuisOuvertureForPrincipal(contribuable, nouveauForFiscal, forFiscalPrincipal.getModeImposition());
            //Envoi d'un événement fiscal
            if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == nouveauForFiscal.getTypeAutoriteFiscale()) {
                evenementFiscalService.publierEvenementFiscalChangementModeImposition(contribuable, dateChangementModeImposition, modeImposition, nouveauForFiscal.getId());
            }
        }

        // [UNIREG-2794] déblocage en cas d'ouverture de for fiscal principal vaudois
        resetFlagBlocageRemboursementAutomatiqueSelonFors(contribuable);

        // [UNIREG-2806] On schedule un réindexation pour le début du mois suivant (les changements d'assujettissement source->ordinaire sont décalés en fin de mois)
        final RegDate debutMoisProchain = RegDate.get(dateChangementModeImposition.year(), dateChangementModeImposition.month(), 1).addMonths(1);
        contribuable.scheduleReindexationOn(debutMoisProchain);

        return nouveauForFiscal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscal corrigerAutoriteFiscale(ForFiscal forFiscal, int noOfsAutoriteFiscale) {
        Assert.notNull(forFiscal);

        if (forFiscal.getNumeroOfsAutoriteFiscale().equals(noOfsAutoriteFiscale)) {
            // rien à faire
            return null;
        }

        final Tiers tiers = forFiscal.getTiers();
        Assert.notNull(tiers);

        // [UNIREG-2322] toutes les corrections doivent s'effectuer par une annulation du for suivi de la création d'un nouveau for avec la valeur corrigée.
        ForFiscal forCorrige = forFiscal.duplicate();
        forFiscal.setAnnule(true);
        forCorrige.setNumeroOfsAutoriteFiscale(noOfsAutoriteFiscale);
        forCorrige = tiersDAO.addAndSave(tiers, forCorrige);

        // notifie le reste du monde
        if (forFiscal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
            evenementFiscalService.publierEvenementFiscalAnnulationFor(forFiscal, RegDate.get());
            final MotifFor motifFor = (forFiscal instanceof ForFiscalRevenuFortune ? ((ForFiscalRevenuFortune) forFiscal).getMotifOuverture() : null);
            evenementFiscalService.publierEvenementFiscalOuvertureFor(tiers, RegDate.get(), motifFor, forCorrige.getId());
        }

        return forCorrige;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalSecondaire corrigerPeriodeValidite(ForFiscalSecondaire ffs, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture) {
        Assert.notNull(ffs);

        if (ffs.getDateDebut() == dateOuverture && ffs.getDateFin() == dateFermeture
                && ffs.getMotifOuverture() == motifOuverture && ffs.getMotifFermeture() == motifFermeture) {
            // rien à faire
            return null;
        }

        final Tiers tiers = ffs.getTiers();
        Assert.notNull(tiers);

        // [UNIREG-2322] toutes les corrections doivent s'effectuer par une annulation du for suivi de la création d'un nouveau for avec la valeur corrigée.
        ForFiscalSecondaire forCorrige = (ForFiscalSecondaire) ffs.duplicate();
        ffs.setAnnule(true);
        forCorrige.setDateDebut(dateOuverture);
        forCorrige.setMotifOuverture(motifOuverture);
        forCorrige.setDateFin(dateFermeture);
        forCorrige.setMotifFermeture(motifFermeture);
        forCorrige = tiersDAO.addAndSave(tiers, forCorrige);

        // notifie le reste du monde
        evenementFiscalService.publierEvenementFiscalAnnulationFor(ffs, RegDate.get());
        evenementFiscalService.publierEvenementFiscalOuvertureFor(tiers, RegDate.get(), null, forCorrige.getId());
        if (dateFermeture != null) {
            evenementFiscalService.publierEvenementFiscalFermetureFor(tiers, RegDate.get(), null, forCorrige.getId());
        }

        return forCorrige;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, MotifRattachement motifRattachement,
                                              int autoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, ModeImposition modeImposition) {
        final ForFiscalPrincipal dernierForPrincipal = contribuable.getDernierForFiscalPrincipal();
        if (dernierForPrincipal != null && dernierForPrincipal.getDateFin() == null) {
            if (dateFin == null || dateFin.isAfter(dernierForPrincipal.getDateDebut())) {
                closeForFiscalPrincipal(contribuable, dateDebut.getOneDayBefore(), motifOuverture);
            }
        }

        final ForFiscalPrincipal forRtr;
        if (dateFin == null) {
            forRtr = openForFiscalPrincipal(contribuable, dateDebut, motifRattachement, autoriteFiscale, typeAutoriteFiscale, modeImposition, motifOuverture, true);
        } else {
            forRtr = openAndCloseForFiscalPrincipal(contribuable, dateDebut, motifRattachement, autoriteFiscale, typeAutoriteFiscale, modeImposition, motifOuverture, dateFin, motifFermeture, true);
        }

        if (motifOuverture == MotifFor.PERMIS_C_SUISSE || motifOuverture == MotifFor.CHGT_MODE_IMPOSITION) {
            // [UNIREG-2806] On schedule un réindexation pour le début du mois suivant (les changements d'assujettissement source->ordinaire sont décalés en fin de mois)
            final RegDate debutMoisProchain = RegDate.get(dateDebut.year(), dateDebut.month(), 1).addMonths(1);
            contribuable.scheduleReindexationOn(debutMoisProchain);
        }

        return forRtr;
    }

    /**
     * Annule tous les fors ouverts à la date spécifiée (et qui ne sont pas fermés) sur le contribuable donné et dont le motif d'ouverture correspond à ce qui est indiqué
     *
     * @param contribuable   contribuable visé
     * @param dateOuverture  date d'ouverture des fors à annuler
     * @param motifOuverture motif d'ouverture des fors à annuler (<code>null</code> possible si tout motif convient)
     */
    @Override
    public void annuleForsOuvertsAu(Contribuable contribuable, RegDate dateOuverture, MotifFor motifOuverture) {
        for (ForFiscal forFiscal : contribuable.getForsFiscaux()) {
            if (!forFiscal.isAnnule() && forFiscal.getDateFin() == null && dateOuverture.equals(forFiscal.getDateDebut())) {
                boolean isForFiscalRevenuFortune = forFiscal instanceof ForFiscalRevenuFortune;
                if (!isForFiscalRevenuFortune || motifOuverture == null || motifOuverture == ((ForFiscalRevenuFortune) forFiscal).getMotifOuverture()) {
                    forFiscal.setAnnule(true);
                }
            }
        }
        resetFlagBlocageRemboursementAutomatiqueSelonFors(contribuable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Periodicite addPeriodicite(DebiteurPrestationImposable debiteur, PeriodiciteDecompte periodiciteDecompte, PeriodeDecompte periodeDecompte, RegDate dateDebut, RegDate dateFin) {
        annulerPeriodicitePosterieur(debiteur, dateDebut);
        while (true) { // cette boucle permet de fusionner - si nécessaire - la nouvelle périodicité avec celles existantes
            Periodicite courante = debiteur.getPeriodiciteAt(dateDebut);
            if (courante == null) {
                //on regarde la périodicité précédente
                courante = debiteur.getPeriodiciteAt(dateDebut.getOneDayBefore());
                if (courante == null) {
                    // pas de périodicité, on continue
                    break;
                }

            }
            if (courante.getId() == null) {
                // l'implémentation de getPeriodiciteAt() crée une périodicité transiente à la volée lorsqu'aucun périodicité n'existe => on l'ignore gaiment
                break;
            }

            if (courante.getPeriodiciteDecompte() == periodiciteDecompte && courante.getPeriodeDecompte() == periodeDecompte &&
                    RegDateHelper.isBetween(courante.getDateFin(), dateDebut.getOneDayBefore(), dateFin, NullDateBehavior.LATEST)) {
                Assert.isTrue(courante.getDateDebut().isBeforeOrEqual(dateDebut));
                // la nouvelle périodicité est identique à la périodicité courante (ou ne fait que la prolonger), on adapte donc la périodicité courante
                courante.setDateFin(RegDateHelper.maximum(courante.getDateFin(), dateFin, NullDateBehavior.LATEST));
                return courante;
            }

            //[UNIREG-2683]dans le cas d'une périodicité UNIQUE, la période de décompte ne doit pas être historisée le changemnt doit être immediat
            if (periodiciteDecompte == PeriodiciteDecompte.UNIQUE) {
                if (courante.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE && courante.getPeriodeDecompte() != periodeDecompte) {
                    // la periodicité est toujours de type UNIQUE, seule la période change, on met à jour la périodicité courante
                    courante.setPeriodeDecompte(periodeDecompte);
                    return courante;
                }
            }

            final RegDate veilleDebut = dateDebut.getOneDayBefore();
            if (courante.getDateDebut() != null && courante.getDateDebut().isAfter(veilleDebut)) {
                // la périodicité courante est masquée par le nouvelle périodicité, on l'annule (et on continue de boucler)
                courante.setAnnule(true);
            } else {
                // autrement, on adapte la date fin
                courante.setDateFin(veilleDebut);
                break;
            }
        }

        final Periodicite nouvelle = new Periodicite(periodiciteDecompte, periodeDecompte, dateDebut, dateFin);
        return tiersDAO.addAndSave(debiteur, nouvelle);
    }

    /**
     * Annule toute les périodicités du débiteur qui ont une date de début postérieur à la date passée en paramètre UNIREG-3041
     *
     * @param debiteur  sur qui on veut annuler les périodicités
     * @param dateDebut date de référence  calculée par @link #getDateDebutNouvellePeriodicite
     */
    private void annulerPeriodicitePosterieur(DebiteurPrestationImposable debiteur, RegDate dateDebut) {
        List<Periodicite> periodicites = debiteur.getPeriodicitesSorted();
        if (periodicites != null) {
            for (Periodicite periodicite : periodicites) {
                if (!periodicite.isAnnule() && periodicite.getDateDebut().isAfter(dateDebut)) {
                    periodicite.setAnnule(true);
                }
            }

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalSecondaire addForSecondaire(Contribuable contribuable, RegDate dateOuverture, @Nullable RegDate dateFermeture, MotifRattachement motifRattachement,
                                                int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture, @Nullable MotifFor motifFermeture) {

        // Ouvre un nouveau for à la date d'événement
        ForFiscalSecondaire nouveauForFiscal = new ForFiscalSecondaire();
        nouveauForFiscal.setDateDebut(dateOuverture);
        nouveauForFiscal.setDateFin(dateFermeture);
        nouveauForFiscal.setMotifRattachement(motifRattachement);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal.setMotifOuverture(motifOuverture);
        nouveauForFiscal.setMotifFermeture(motifFermeture);
        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            afterForFiscalSecondaireAdded(contribuable, nouveauForFiscal);
        }

        if (dateFermeture != null) {
            afterForFiscalSecondaireClosed(contribuable, nouveauForFiscal, dateFermeture, motifFermeture);
        }

        Assert.notNull(nouveauForFiscal);
        return nouveauForFiscal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalAutreElementImposable addForAutreElementImposable(Contribuable contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture,
                                                                      MotifRattachement motifRattachement, TypeAutoriteFiscale typeAutoriteFiscale, int autoriteFiscale) {
        final ForFiscalAutreElementImposable forRtr =
                openForFiscalAutreElementImposable(contribuable, GenreImpot.REVENU_FORTUNE, dateDebut, motifRattachement, autoriteFiscale, typeAutoriteFiscale, motifOuverture);
        if (dateFin != null) {
            closeForFiscalAutreElementImposable(contribuable, forRtr, dateFin, motifFermeture);
        }
        return forRtr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable debiteur, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, int autoriteFiscale) {
        final ForDebiteurPrestationImposable dernierForDebiteur = debiteur.getDernierForDebiteur();
        if (dernierForDebiteur != null && dernierForDebiteur.getDateFin() == null) {
            closeForDebiteurPrestationImposable(debiteur, dernierForDebiteur, dateDebut.getOneDayBefore(), false);
        }
        if (dernierForDebiteur == null) {
            //[UNIREG-2885] dans le cas de la création d'un premier for, on doit adapter si besoin la première périodicité
            adaptPremierePeriodicite(debiteur, dateDebut);
        }

        ForDebiteurPrestationImposable forRtr;
        if (dateFin == null) {
            forRtr = openForDebiteurPrestationImposable(debiteur, dateDebut, autoriteFiscale, typeAutoriteFiscale);
        } else {
            forRtr = openAndCloseForDebiteurPrestationImposable(debiteur, dateDebut, dateFin, autoriteFiscale, typeAutoriteFiscale);
        }
        return forRtr;

    }

    /**
     * Ferme le for fiscal autre élément imposable d'un contribuable.
     *
     * @param contribuable                   le contribuable concerné
     * @param forFiscalAutreElementImposable le for à fermer
     * @param dateFermeture                  la date de fermeture du for
     * @param motifFermeture                 la motif de fermeture du for
     * @return le for fiscal autre élément imposable fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    @Override
    public ForFiscalAutreElementImposable closeForFiscalAutreElementImposable(Contribuable contribuable,
                                                                              ForFiscalAutreElementImposable forFiscalAutreElementImposable, RegDate dateFermeture, MotifFor motifFermeture) {
        if (forFiscalAutreElementImposable != null) {
            if (forFiscalAutreElementImposable.getDateDebut().isAfter(dateFermeture)) {
                throw new ValidationException(forFiscalAutreElementImposable, "La date de fermeture ("
                        + RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
                        + RegDateHelper.dateToDisplayString(forFiscalAutreElementImposable.getDateDebut()) + ") du for fiscal actif");
            }
            forFiscalAutreElementImposable.setDateFin(dateFermeture);
            forFiscalAutreElementImposable.setMotifFermeture(motifFermeture);
        }

        return forFiscalAutreElementImposable;
    }

    /**
     * Ferme le for debiteur d'un contribuable.
     *
     * @param debiteur                       le debiteur concerné
     * @param forDebiteurPrestationImposable le for débiteur concerné
     * @param dateFermeture                  la date de fermeture du for
     * @param fermerRapportsPrestation       <code>true</code> s'il faut fermer les rapports "prestation" du débiteur, <code>false</code> s'il faut les laisser ouverts
     * @return le for debiteur fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    @Override
    public ForDebiteurPrestationImposable closeForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur,
                                                                              ForDebiteurPrestationImposable forDebiteurPrestationImposable,
                                                                              RegDate dateFermeture,
                                                                              boolean fermerRapportsPrestation) {
        if (forDebiteurPrestationImposable != null) {
            if (forDebiteurPrestationImposable.getDateDebut().isAfter(dateFermeture)) {
                throw new ValidationException(forDebiteurPrestationImposable, "La date de fermeture ("
                        + RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
                        + RegDateHelper.dateToDisplayString(forDebiteurPrestationImposable.getDateDebut()) + ") du for fiscal actif");
            }
            forDebiteurPrestationImposable.setDateFin(dateFermeture);

            // [UNIREG-2144] Fermeture des rapports de travail
            if (fermerRapportsPrestation) {
                for (RapportEntreTiers rapport : debiteur.getRapportsObjet()) {
                    if (rapport instanceof RapportPrestationImposable && rapport.isValidAt(dateFermeture) && rapport.getDateFin() == null) {
                        rapport.setDateFin(dateFermeture);
                    }
                }
            }

            this.evenementFiscalService.publierEvenementFiscalFermetureFor(debiteur, dateFermeture, null,
                    forDebiteurPrestationImposable.getId());
        }

        return forDebiteurPrestationImposable;
    }

    @Override
    public void closeForAutreImpot(ForFiscalAutreImpot autre, RegDate dateFermeture) {
        if (autre != null) {
            if (autre.getDateDebut().isAfter(dateFermeture)) {
                throw new ValidationException(autre, "La date de fermeture ("
                        + RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
                        + RegDateHelper.dateToDisplayString(autre.getDateDebut()) + ") du for fiscal actif");
            }
            autre.setDateFin(dateFermeture);

            this.evenementFiscalService.publierEvenementFiscalFermetureFor(autre.getTiers(), dateFermeture, null,
                    autre.getId());
        }
    }

    /**
     * Ferme tous les fors fiscaux d'un contribuable.
     *
     * @param contribuable  le contribuable concerné.
     * @param dateFermeture la date de fermeture des fors.
     */
    @Override
    public void closeAllForsFiscaux(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {
        final List<ForFiscal> fors = contribuable.getForsFiscauxValidAt(dateFermeture);
        for (ForFiscal forFiscal : fors) {
            if (forFiscal.getDateFin() == null) {
                if (forFiscal instanceof ForFiscalPrincipal) {
                    // voir commentaire plus bas
                    //closeForFiscalPrincipal(contribuable, (ForFiscalPrincipal)forFiscal, dateFermeture, motifFermeture);
                } else if (forFiscal instanceof ForFiscalSecondaire) {
                    closeForFiscalSecondaire(contribuable, (ForFiscalSecondaire) forFiscal, dateFermeture, motifFermeture);
                } else if (forFiscal instanceof ForFiscalAutreElementImposable) {
                    closeForFiscalAutreElementImposable(contribuable, (ForFiscalAutreElementImposable) forFiscal, dateFermeture, motifFermeture);
                } else if (forFiscal instanceof ForFiscalAutreImpot) {
                    closeForAutreImpot((ForFiscalAutreImpot) forFiscal, dateFermeture);
                }
                //else if (forFiscal instanceof ForDebiteurPrestationImposable) {//impossible
            }
        }
        /*
		 * La fermeture est faite en 2 étapes pour suivre la logique métier:
		 *  - aucun for secondaire ne doit exister s'il nya pas de for principal
		 * Pour cette raison les fors secondaires, autre élément imposable et
		 * autre impot doivent être fermés avant le principal.
		 */
        for (ForFiscal forFiscal : fors) {
            if (forFiscal.getDateFin() == null) {
                if (forFiscal instanceof ForFiscalPrincipal) {
                    closeForFiscalPrincipal(contribuable, (ForFiscalPrincipal) forFiscal, dateFermeture, motifFermeture);
                }
            }
        }
    }

    /**
     * Fusionne un non habitant avec un habitant
     *
     * @param habitant
     * @param nonHabitant
     */
    @Override
    public void fusionne(PersonnePhysique habitant, PersonnePhysique nonHabitant) {
        // Onglet Complements
        copieComplements(nonHabitant, habitant);
        copieRemarques(nonHabitant, habitant);

        // Onglet Fiscal
        final Set<ForFiscal> forsCible = new HashSet<ForFiscal>();
        for (ForFiscal forFiscalSource : nonHabitant.getForsFiscaux()) {
            if (forFiscalSource instanceof ForFiscalAutreImpot) {
                ForFiscalAutreImpot forFiscalCible = copieForFiscalAutreImpot((ForFiscalAutreImpot) forFiscalSource);
                forsCible.add(forFiscalCible);
            }
            if (forFiscalSource instanceof ForFiscalSecondaire) {
                ForFiscalSecondaire forFiscalCible = copieForFiscalSecondaire((ForFiscalSecondaire) forFiscalSource);
                forsCible.add(forFiscalCible);
            }
            if (forFiscalSource instanceof ForFiscalAutreElementImposable) {
                ForFiscalAutreElementImposable forFiscalCible = copieForFiscalAutreElementImposable((ForFiscalAutreElementImposable) forFiscalSource);
                forsCible.add(forFiscalCible);
            }
            if (forFiscalSource instanceof ForFiscalPrincipal) {
                ForFiscalPrincipal forFiscalCible = copieForFiscalPrincipal((ForFiscalPrincipal) forFiscalSource);
                forsCible.add(forFiscalCible);
            }

        }
        habitant.setForsFiscaux(forsCible);
        resetFlagBlocageRemboursementAutomatiqueSelonFors(habitant);

        //Annulation du nonHabitant
        annuleTiers(nonHabitant);
    }

    /**
     * [UNIREG-2794] déblocage en cas d'ouverture de for fiscal principal vaudois, blocage en cas de fermeture de for principal vaudois
     *
     * @param tiers le tiers dont on veut débloquer le reboursement automatique.
     */
    private static void resetFlagBlocageRemboursementAutomatiqueSelonFors(Tiers tiers) {
        if (tiers instanceof PersonnePhysique || tiers instanceof MenageCommun) {
            final Contribuable ctb = (Contribuable) tiers;
            final ForFiscalPrincipal forVaudois = ctb.getDernierForFiscalPrincipalVaudois();
            ctb.setBlocageRemboursementAutomatique(forVaudois == null || forVaudois.getDateFin() != null);
        }
    }

    private void copieRemarques(Tiers tiersSource, Tiers tiersCible) {

        final List<Remarque> list = remarqueDAO.getRemarques(tiersSource.getNumero());
        for (Remarque r : list) {
            Remarque c = new Remarque();
            c.setLogCreationDate(r.getLogCreationDate());
            c.setLogCreationUser(r.getLogCreationUser());
            c.setTexte(r.getTexte());
            c.setTiers(tiersCible);
            remarqueDAO.save(c);
        }
    }

    /**
     * Copie les informations Complements
     *
     * @param tiersCible
     * @param tiersSource
     */
    private void copieComplements(Tiers tiersSource, Tiers tiersCible) {
        tiersCible.setComplementNom(tiersSource.getComplementNom());
        tiersCible.setPersonneContact(tiersSource.getPersonneContact());
        tiersCible.setNumeroTelecopie(tiersSource.getNumeroTelecopie());
        tiersCible.setNumeroTelephonePortable(tiersSource.getNumeroTelephonePortable());
        tiersCible.setNumeroTelephonePrive(tiersSource.getNumeroTelephonePrive());
        tiersCible.setNumeroTelephoneProfessionnel(tiersSource.getNumeroTelephoneProfessionnel());
        tiersCible.setBlocageRemboursementAutomatique(tiersSource.getBlocageRemboursementAutomatique());
        tiersCible.setTitulaireCompteBancaire(tiersSource.getTitulaireCompteBancaire());
        tiersCible.setAdresseBicSwift(tiersSource.getAdresseBicSwift());
    }

    private ForFiscalAutreImpot copieForFiscalAutreImpot(ForFiscalAutreImpot forFiscalSource) {
        ForFiscalAutreImpot forFiscalCible = new ForFiscalAutreImpot();
        copieForFiscal(forFiscalSource, forFiscalCible);
        return forFiscalCible;
    }

    private ForFiscalSecondaire copieForFiscalSecondaire(ForFiscalSecondaire forFiscalSource) {
        ForFiscalSecondaire forFiscalCible = new ForFiscalSecondaire();
        copieForFiscal(forFiscalSource, forFiscalCible);
        copieForFiscalRevenuFortune(forFiscalSource, forFiscalCible);
        return forFiscalCible;
    }

    private ForFiscalAutreElementImposable copieForFiscalAutreElementImposable(ForFiscalAutreElementImposable forFiscalSource) {
        ForFiscalAutreElementImposable forFiscalCible = new ForFiscalAutreElementImposable();
        copieForFiscal(forFiscalSource, forFiscalCible);
        copieForFiscalRevenuFortune(forFiscalSource, forFiscalCible);
        return forFiscalCible;
    }

    private ForFiscalPrincipal copieForFiscalPrincipal(ForFiscalPrincipal forFiscalSource) {
        ForFiscalPrincipal forFiscalCible = new ForFiscalPrincipal();
        copieForFiscal(forFiscalSource, forFiscalCible);
        copieForFiscalRevenuFortune(forFiscalSource, forFiscalCible);
        forFiscalCible.setModeImposition(forFiscalSource.getModeImposition());
        return forFiscalCible;
    }

    /**
     * Copie les attributs de ForFiscal
     *
     * @param forFiscalSource
     * @param forFiscalCible
     */
    private void copieForFiscal(ForFiscal forFiscalSource, ForFiscal forFiscalCible) {
        forFiscalCible.setDateDebut(forFiscalSource.getDateDebut());
        forFiscalCible.setDateFin(forFiscalSource.getDateFin());
        forFiscalCible.setGenreImpot(forFiscalSource.getGenreImpot());
        forFiscalCible.setTypeAutoriteFiscale(forFiscalSource.getTypeAutoriteFiscale());
        forFiscalCible.setNumeroOfsAutoriteFiscale(forFiscalSource.getNumeroOfsAutoriteFiscale());
        forFiscalCible.setAnnule(forFiscalSource.isAnnule());
    }

    /**
     * Copie les attributs de ForFiscalRevenuFortune
     *
     * @param forFiscalRevenuFortuneSource
     * @param forFiscalRevenuFortuneCible
     */
    private void copieForFiscalRevenuFortune(ForFiscalRevenuFortune forFiscalRevenuFortuneSource,
                                             ForFiscalRevenuFortune forFiscalRevenuFortuneCible) {
        forFiscalRevenuFortuneCible.setMotifRattachement(forFiscalRevenuFortuneSource.getMotifRattachement());
        forFiscalRevenuFortuneCible.setMotifOuverture(forFiscalRevenuFortuneSource.getMotifOuverture());
        forFiscalRevenuFortuneCible.setMotifFermeture(forFiscalRevenuFortuneSource.getMotifFermeture());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
        this.evenementFiscalService = evenementFiscalService;
    }

    @Override
    public String getNomPrenom(PersonnePhysique personne) {
        Assert.notNull(personne);
        final NomPrenom nomPrenom = getDecompositionNomPrenom(personne);
        return nomPrenom.getNomPrenom();
    }

    private String getNom(PersonnePhysique personne) {
        final NomPrenom nomPrenom = getDecompositionNomPrenom(personne);
        return nomPrenom.getNom();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNomPrenom(Individu individu) {
        return serviceCivilService.getNomPrenom(individu);
    }

    @Override
    public NomPrenom getDecompositionNomPrenom(Individu individu) {
        return serviceCivilService.getDecompositionNomPrenom(individu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegDate getDateNaissance(PersonnePhysique pp) {
        if (pp.isHabitantVD()) {
            final Individu individu = getIndividu(pp);
            if (individu == null) {
                throw new IndividuNotFoundException(pp);
            }
            return individu.getDateNaissance();
        } else {
            return pp.getDateNaissance();
        }
    }

    @Override
    public boolean isMineur(PersonnePhysique pp, RegDate date) {
        final RegDate dateNaissance = getDateNaissance(pp);
        return dateNaissance != null && dateNaissance.addYears(18).compareTo(date) > 0;
    }

    @Override
    public RegDate getDateDebutVeuvage(PersonnePhysique pp, RegDate date) {
        final VueSituationFamille situation = situationFamilleService.getVue(pp, date, true);
        if (situation != null && EtatCivil.VEUF == situation.getEtatCivil()) {
            return situation.getDateDebut();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegDate getDateDeces(PersonnePhysique pp) {
        if (pp == null)
            return null;

        if (pp.isHabitantVD()) {
            if (pp.getDateDeces() != null) {
                return pp.getDateDeces();
            }
            final Individu individu = getIndividu(pp);
            if (individu == null) {
                throw new IndividuNotFoundException(pp);
            }
            return individu.getDateDeces();
        } else {
            return pp.getDateDeces();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDecede(PersonnePhysique pp) {
        return getDateDeces(pp) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNumeroAssureSocial(PersonnePhysique pp) {
        if (pp.isHabitantVD()) {
            final Individu individu = getIndividu(pp);
            if (individu == null) {
                throw new IndividuNotFoundException(pp);
            }
            return individu.getNouveauNoAVS();
        } else {
            return pp.getNumeroAssureSocial();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAncienNumeroAssureSocial(PersonnePhysique pp) {
        if (pp.isHabitantVD()) {
            final Individu individu = getIndividu(pp);
            if (individu == null) {
                throw new IndividuNotFoundException(pp);
            }
            return individu.getNoAVS11();
        } else {
            final Set<IdentificationPersonne> identifications = pp.getIdentificationsPersonnes();
            if (identifications != null) {
                for (IdentificationPersonne i : identifications) {
                    if (CategorieIdentifiant.CH_AHV_AVS == i.getCategorieIdentifiant()) {
                        return i.getIdentifiant();
                    }
                }
            }
            return null; // non-disponible
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getOfficeImpotId(Tiers tiers) {
        return getOfficeImpotIdAt(tiers, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getOfficeImpotIdAt(Tiers tiers, @Nullable RegDate date) {
        Integer oid = tiers.getOfficeImpotId();
        if (date == null && oid != null) {
            // l'oid courant est déjà connu, pas besoin d'en faire plus
            return oid;
        }

        // Calcul de l'oid à la date demandée
        final ForGestion forGestion = getDernierForGestionConnu(tiers, date);
        if (forGestion != null) {
            oid = getOfficeImpotId(forGestion.getNoOfsCommune());
        }

        // [UNIREG-1850] il ne faut pas modifier l'office d'impôt ici, car les éventuelles tâches associées doivent aussi être mises à jour (par l'intercepteur)
        //		if (date == null && oid != null) {
        //			// on profite de cacher l'oid courant dans le tiers
        //			tiers.setOfficeImpotId(oid);
        //		}

        return oid;
    }

    @Override
    public CollectiviteAdministrative getOfficeImpotAt(Tiers tiers, RegDate date) {

        final Integer oid = getOfficeImpotIdAt(tiers, date);
        if (oid == null) {
            return null;
        }

        final CollectiviteAdministrative ca = getCollectiviteAdministrative(oid, true);
        if (ca == null) {
            throw new IllegalArgumentException("Impossible de trouver la collectivité correspondant à l'office d'impôt n°" + oid + '.');
        }

        return ca;
    }

    @Override
    public CollectiviteAdministrative getOfficeImpotRegionAt(Tiers tiers, @Nullable RegDate date) {

        final ForGestion forGestion = getDernierForGestionConnu(tiers, date);
        //RechercherComplementInformationContribuable de la commune du for de gestion
        if (forGestion != null) {
            Commune communeGestion = serviceInfra.getCommuneByNumeroOfsEtendu(forGestion.getNoOfsCommune(), date);
            if (communeGestion != null) {
                District district = communeGestion.getDistrict();
                if (district != null) {
                    Region region = district.getRegion();
                    if (region != null) {
                        return tiersDAO.getCollectiviteAdministrativeForRegion(region.getCode());
                    }

                }
            }
        }
        return null;
    }

    @Override
    public Integer getOfficeImpotId(ForGestion forGestion) {
        return getOfficeImpotId(forGestion.getNoOfsCommune());
    }

    @Override
    public Integer getOfficeImpotId(int noOfsCommune) {
        Integer oid = null;
        try {
            ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative office = serviceInfra.getOfficeImpotDeCommune(noOfsCommune);
            if (office != null) {
                oid = office.getNoColAdm();
            }
        } catch (ServiceInfrastructureException e) {
            throw new RuntimeException("Impossible de déterminer l'office d'impôt de la commune avec le numéro Ofs = " + noOfsCommune);
        }
        return oid;
    }

    @Override
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public Integer calculateCurrentOfficeID(Tiers tiers) {
        final ForGestion forGestion = getDernierForGestionConnu(tiers, null);
        if (forGestion == null) {
            // pas de for de gestion, pas d'oid
            return null;
        }

        final int noOfs = forGestion.getNoOfsCommune();
        final Integer oid = getOfficeImpotId(noOfs);
        return oid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void annuleForFiscal(ForFiscal forFiscal, boolean changeHabitantFlag) throws ValidationException {
        Assert.notNull(forFiscal, "le for fiscal doit être renseigné");
        final Tiers tiers = forFiscal.getTiers();
        Assert.notNull(tiers, "le for fiscal doit être rattaché à un tiers");

        //
        // Annulation du for
        //

        if (forFiscal instanceof ForFiscalPrincipal) {
            final ForFiscalPrincipal forPrincipal = (ForFiscalPrincipal) forFiscal;
            final List<ForFiscalPrincipal> fors = tiers.getForsFiscauxPrincipauxActifsSorted();

            // [UNIREG-2607] Apparemment, quelqu'un a réussi à arriver dans un cas où cette collection
            // était vide... le seul scénario auquel je pense est si cette méthode est appelée deux fois
            // (double-click sur le bouton d'annulation, concurrence entre deux sessions...)
            if (fors.isEmpty()) {
                throw new ValidationException(forPrincipal, "Tous les fors principaux sont déjà annulés.");
            }

            // le for principal doit être le plus récent
            final ForFiscalPrincipal dernierFor = fors.get(fors.size() - 1);
            if (forPrincipal != dernierFor) {
                throw new ValidationException(forPrincipal, "Seul le dernier for fiscal principal peut être annulé.");
            }

            // réouvre le for précédent si nécessaire
            ForFiscalPrincipal forPrecedent = null;
            for (ForFiscalPrincipal f : fors) {
                if (f.getDateFin() == forPrincipal.getDateDebut().getOneDayBefore()) {
                    forPrecedent = f;
                    break;
                }
            }
            if (forPrecedent != null) {
                reopenForFiscalPrincipal(forPrecedent, changeHabitantFlag);
            }
        } else if (forFiscal instanceof ForDebiteurPrestationImposable) {
            final ForDebiteurPrestationImposable forDPI = (ForDebiteurPrestationImposable) forFiscal;
            final ForsParType fors = tiers.getForsParType(true);
            if (fors.dpis.isEmpty()) {
                throw new ValidationException(forDPI, "Tous les fors débiteurs sont déjà annulés.");
            }

            // trouvons le for débiteur (non-annulé) le plus récent
            ForDebiteurPrestationImposable dernierFor = null;
            final ListIterator<ForDebiteurPrestationImposable> iterator = fors.dpis.listIterator(fors.dpis.size());
            while (iterator.hasPrevious()) {
                final ForDebiteurPrestationImposable forCandidat = iterator.previous();
                if (!forCandidat.isAnnule()) {
                    dernierFor = forCandidat;
                    break;
                }
            }
            if (forDPI != dernierFor) {
                throw new ValidationException(forDPI, "Seul le dernier for débiteur peut être annulé.");
            }

            // réouvre le for précédent si nécessaire
            ForDebiteurPrestationImposable forPrecedent = null;
            while (iterator.hasPrevious()) {
                final ForDebiteurPrestationImposable forCandidat = iterator.previous();
                if (!forCandidat.isAnnule()) {
                    forPrecedent = forCandidat;
                    break;
                }
            }
            if (forPrecedent != null && forPrecedent.getDateFin() == forDPI.getDateDebut().getOneDayBefore()) {
                reopenForDebiteur(forPrecedent);
            }
        }
        forFiscal.setAnnule(true);

        //
        // Envoi d'un événement fiscal
        //

        boolean envoi = false;
        if (forFiscal instanceof ForDebiteurPrestationImposable) {
            envoi = true;
        } else if (forFiscal instanceof ForFiscalPrincipal) {
            if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forFiscal.getTypeAutoriteFiscale()) {
                envoi = true;
            }
        } else if (forFiscal instanceof ForFiscalSecondaire) {
            /*
			 * Dans le cas d'un for secondaire pour un hors-Canton ou hors-Suisse, on envoie un événement, sauf s'il subsiste un autre for
			 * secondaire avec le même motif de rattachement
			 */
            final ForFiscalPrincipal forPrincipalCourant = forFiscal.getTiers().getDernierForFiscalPrincipal();
            Assert.notNull(forPrincipalCourant);

            if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD != forPrincipalCourant.getTypeAutoriteFiscale()) {
                envoi = true;
                final List<ForFiscal> fors = forFiscal.getTiers().getForsFiscauxValidAt(null);
                final MotifRattachement motifRattachement = ((ForFiscalSecondaire) forFiscal).getMotifRattachement();
                for (ForFiscal f : fors) {
                    if (f instanceof ForFiscalSecondaire) {
                        final ForFiscalSecondaire fs = (ForFiscalSecondaire) f;
                        if (fs.getMotifRattachement() == motifRattachement) {
                            envoi = false;
                            break;
                        }
                    }
                }
            }
        }
        if (envoi) {
            evenementFiscalService.publierEvenementFiscalAnnulationFor(forFiscal, forFiscal.getDateDebut());
        }

        //
        // Création des tâches
        //
        if (tiers instanceof Contribuable) {
            Contribuable ctb = (Contribuable) tiers;
            tacheService.genereTachesDepuisAnnulationDeFor(ctb);
        }

        // [UNIREG-2794] déblocage en cas d'ouverture de for fiscal principal vaudois
        resetFlagBlocageRemboursementAutomatiqueSelonFors(tiers);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void annuleTiers(Tiers tiers) {
        Assert.notNull(tiers);
        tiers.setAnnule(true);

        if (tiers instanceof Contribuable) {
            tacheService.onAnnulationContribuable((Contribuable) tiers);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForGestion getDernierForGestionConnu(Tiers tiers, @Nullable RegDate date) {

        if (tiers instanceof DebiteurPrestationImposable) {
            //un DPI n'a pas de for de gestion, il est géré par une OID spéciale
            return null;
        }

        final List<ForFiscal> forsFiscaux = tiers.getForsFiscauxSorted();
        if (forsFiscaux == null || forsFiscaux.isEmpty()) {
            return null;
        }

        // Lucky guess
        ForGestion forGestion;
        if (date == null) {
            // on essaie tout d'abord à la date de fin du dernier for fiscal
            forGestion = getForGestionActif(tiers, forsFiscaux.get(forsFiscaux.size() - 1).getDateFin());
        } else {
            // on essaie tout d'abord à la date spécifiée
            forGestion = getForGestionActif(tiers, date);
        }
        if (forGestion != null) {
            return forGestion;
        }

        // -> pas trouvé: on sort la grosse artillerie et on passe en revue tous les fors fiscaux

        // Récupère la liste des dates (triées par ordre croissant) où les fors fiscaux ont changés
        final List<RegDate> dates = DateRangeHelper.extractBoundaries(forsFiscaux);

        // En commençant par la date la plus récente, regarde si on trouve un for de gestion
        for (int i = dates.size() - 1; i >= 0; --i) {
            final RegDate d = dates.get(i);
            if (date != null && (d == null || d.isAfter(date))) {
                // on ignore toutes les dates plus vieilles que la date spécifiée
                continue;
            }
            forGestion = getForGestionActif(tiers, d);
            if (forGestion != null) {
                break;
            }
        }

        return forGestion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForGestion getForGestionActif(Tiers tiers, RegDate date) {

        if (tiers instanceof DebiteurPrestationImposable) {
            //un DPI n'a pas de for de gestion, il est géré par une OID spéciale
            return null;
        }

        /*
		 * Cette méthode implémente l'algorithme de détermination des fors spécifié dans le document SCU-TenirLeRegistreDesTiers.doc au
		 * paragraphe intitulé "Détermination de la commune ou fraction de commune du for de gestion"
		 */

        final List<ForFiscal> forsFiscaux = tiers.getForsFiscauxValidAt(date);
        Assert.notNull(forsFiscaux);

        ForFiscalPrincipal forPrincipal = null;
        FirstForsList forsActiviteIndependante = new FirstForsList();
        FirstForsList forsImmeuble = new FirstForsList();

        // Analyse des fors
        for (ForFiscal f : forsFiscaux) {
            if (f.isPrincipal()) {
                // Les fors principaux hors canton/Suisse ou avec un mode d'imposition Source ne peuvent pas être des fors de gestion
                if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == f.getTypeAutoriteFiscale()) {
                    ForFiscalPrincipal fp = (ForFiscalPrincipal) f;
                    if (ModeImposition.SOURCE != fp.getModeImposition()) {
                        forPrincipal = fp;
                        break; // pas besoin de chercher plus loin
                    }
                }
            } else if (f instanceof ForFiscalRevenuFortune) {

                final ForFiscalRevenuFortune frf = (ForFiscalRevenuFortune) f;
                final MotifRattachement motifRattachement = frf.getMotifRattachement();

                if (MotifRattachement.ACTIVITE_INDEPENDANTE == motifRattachement) {
                    forsActiviteIndependante.checkFor(frf);
                } else if (MotifRattachement.IMMEUBLE_PRIVE == motifRattachement) {
                    forsImmeuble.checkFor(frf);
                }
            }
        }

        ForFiscalRevenuFortune forGestion = null;

        if (forPrincipal != null) {
            /*
			 * [Spec] Si le contribuable a un for principal ouvert sur une commune ou fraction de commune vaudoise avec un mode d’imposition
			 * différent de « Source », cette commune ou fraction est celle du for de gestion
			 */
            forGestion = forPrincipal;
        } else {
            /*
			 * [Spec] Autrement, si le contribuable n’a pas ou plus de tel for, deux cas sont possibles :
			 */

            if (forsActiviteIndependante.size() == 1) {
                /*
				 * [Spec] Il existe un ou plusieurs fors secondaires ouverts à raison d’une activité indépendante : dans ce cas, le for de
				 * gestion est le for ouvert le plus ancien à raison d’une activité indépendante
				 */
                forGestion = (ForFiscalRevenuFortune) forsActiviteIndependante.get(0);
            } else if (forsImmeuble.size() == 1 && forsActiviteIndependante.isEmpty()) {
                /*
				 * [Spec] Il n’existe aucun for secondaire ouvert à raison d’une activité indépendante mais un ou plusieurs fors secondaires
				 * ouverts à raison d’un immeuble privé : dans ce cas, le for de gestion est le for ouvert le plus ancien à raison d’un
				 * immeuble privé.
				 */
                forGestion = (ForFiscalRevenuFortune) forsImmeuble.get(0);
            } else if (forsActiviteIndependante.size() > 1 || forsImmeuble.size() > 1) {
                /*
				 * [Spec] En cas d’ancienneté égale, le dernier for principal sur une commune ou fraction de commune vaudoise est examiné.
				 * S’il correspond à l’un des fors secondaires, il est choisi comme for de gestion. S’il n’existe pas ou s’il ne correspond
				 * à aucun for secondaire, le 1er for secondaire par ordre alphabétique du nom de la commune ou fraction de commune vaudoise
				 * est choisi parmi les fors d’ancienneté égale.
				 */

                final FirstForsList forsSecondaires = (forsActiviteIndependante.size() > 1 ? forsActiviteIndependante : forsImmeuble);
                final ForFiscalPrincipal dernierForVaudois = tiers.getDernierForFiscalPrincipalVaudois();

                if (dernierForVaudois != null) {
                    final Integer noOfs = dernierForVaudois.getNumeroOfsAutoriteFiscale();
                    forGestion = (ForFiscalRevenuFortune) forsSecondaires.findForWithNumeroOfs(noOfs);
                }

                if (forGestion == null) {
                    // [UNIREG-1029] On trie les fors secondaires restants par ordre alphabétique du nom de la commune, et on prend le premier.
                    Collections.sort(forsSecondaires, new Comparator<ForFiscal>() {
                        @Override
                        public int compare(ForFiscal o1, ForFiscal o2) {
                            final Integer ofs1 = o1.getNumeroOfsAutoriteFiscale();
                            final Integer ofs2 = o2.getNumeroOfsAutoriteFiscale();
                            try {
                                Commune c1 = serviceInfra.getCommuneByNumeroOfsEtendu(ofs1, o1.getDateFin());
                                Commune c2 = serviceInfra.getCommuneByNumeroOfsEtendu(ofs2, o2.getDateFin());
                                return c1.getNomMinuscule().compareTo(c2.getNomMinuscule());
                            } catch (ServiceInfrastructureException e) {
                                LOGGER.warn("Impossible de trier les communes ofs=" + ofs1 + " et ofs=" + ofs2
                                        + " par nom, on trie sur le numéro Ofs à la place", e);
                                return ofs1.compareTo(ofs2);
                            }
                        }
                    });
                    forGestion = (ForFiscalRevenuFortune) forsSecondaires.get(0);
                }
            }
        }

        return forGestion == null ? null : new ForGestion(forGestion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ForGestion> getForsGestionHisto(Tiers tiers) {

        if (tiers instanceof DebiteurPrestationImposable) {
            //un DPI n'a pas de for de gestion, il est géré par une OID spéciale
            return Collections.emptyList();
        }

        List<ForGestion> results = new ArrayList<ForGestion>();

        final List<ForFiscal> forsFiscaux = tiers.getForsFiscauxSorted();
        if (forsFiscaux == null || forsFiscaux.isEmpty()) {
            return results;
        }

        // Récupère la liste des dates (triées par ordre croissant) où les fors fiscaux ont changés
        final SortedSet<RegDate> dates = new TreeSet<RegDate>();
        for (DateRange r : forsFiscaux) {
            final RegDate dateDebut = r.getDateDebut();
            if (dateDebut != null) {
                dates.add(dateDebut);
            }
            RegDate dateFin = r.getDateFin();
            if (dateFin != null) {
                dates.add(dateFin.getOneDayAfter());
            }
        }

        // En commençant par la date la plus ancienne, collecte tous les fors de gestion
        List<ForGestion> forsGestion = new ArrayList<ForGestion>();
        ForGestion precedent = null;
        for (RegDate debut : dates) {
            final ForGestion f = getForGestionActif(tiers, debut);
            if (f != null && (precedent == null || f.getSousjacent() != precedent.getSousjacent())) {
                ForGestion fg = new ForGestion(debut, null, f);
                if (precedent != null) {
                    precedent.setDateFin(debut.getOneDayBefore());
                }
                forsGestion.add(fg);
                precedent = fg;
            }
        }

        // On colle ensemble tous les fors de gestion qui se touchent et qui sont sur la même commune
        ForGestion dernier = null;
        for (ForGestion f : forsGestion) {
            if (dernier != null && dernier.isColletable(f)) {
                dernier.collate(f);
            } else {
                results.add(f);
                dernier = f;
            }
        }

        return results;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
        this.situationFamilleService = situationFamilleService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AdresseTiers> fermeAdresseTiersTemporaire(Tiers tiers, RegDate date) {
        final List<AdresseTiers> listeDesAdressesFermees = new ArrayList<AdresseTiers>();
        if (tiers.getAdressesTiers() != null) {
            for (AdresseTiers adr : tiers.getAdressesTiers()) {
                if (adr instanceof AdresseSupplementaire) {
                    final AdresseSupplementaire adrSupp = (AdresseSupplementaire) adr;
                    if (!adrSupp.isPermanente() && adr.getDateFin() == null && !adrSupp.isAnnule() && RegDateHelper.isBeforeOrEqual(adrSupp.getDateDebut(), date, NullDateBehavior.LATEST)) {
                        adr.setDateFin(date);
                        listeDesAdressesFermees.add(adr);
                    }
                }
            }
        }
        return listeDesAdressesFermees;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRoleAssujettissement(Tiers tiers, @Nullable RegDate date) {
        String str = null;
	    if (date == null) {
		    date = RegDate.get();
	    }

        // les I107 sont "inactifs" (mais attention, ce flag est un peu galvaudé ces temps... donc je
        // teste aussi l'absence de fors fiscaux non annulés)
        if (tiers instanceof PersonnePhysique
                && tiers.isDebiteurInactif()
                && tiers.getForsFiscauxNonAnnules(false).isEmpty()) {
            str = "inactif";
        }

        // seuls les contribuables peuvent avoir un assujettissement
        else if (tiers instanceof Contribuable) {
            try {
                final List<Assujettissement> assujettissements = assujettissementService.determine((Contribuable) tiers, date.year());
                if (assujettissements != null && !assujettissements.isEmpty()) {
                    final Assujettissement valide = DateRangeHelper.rangeAt(assujettissements, date);
                    if (valide != null) {
                        str = valide.getDescription();
                    }
                }

                if (str == null) {
                    str = "Non assujetti";
                }
            } catch (Exception e) {
                LOGGER.warn("Impossible de calculer l'assujettissement du tiers " + tiers.getNumero(), e);
            }
        } else if (tiers instanceof DebiteurPrestationImposable) {
            final CategorieImpotSource categorie = ((DebiteurPrestationImposable) tiers).getCategorieImpotSource();
            if (categorie != null) {
                str = categorie.texte();
            }
        }

        return str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExclureContribuablesEnvoiResults setDateLimiteExclusion(List<Long> ctbIds, RegDate dateLimite, StatusManager s) {
        ExclureContribuablesEnvoiProcessor processor = new ExclureContribuablesEnvoiProcessor(hibernateTemplate, transactionManager);
        return processor.run(ctbIds, dateLimite, s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CorrectionFlagHabitantSurPersonnesPhysiquesResults corrigeFlagHabitantSurPersonnesPhysiques(int nbThreads, StatusManager statusManager) {
        final CorrectionFlagHabitantProcessor processor = new CorrectionFlagHabitantProcessor(hibernateTemplate, this, transactionManager, statusManager);
        return processor.corrigeFlagSurPersonnesPhysiques(nbThreads);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CorrectionFlagHabitantSurMenagesResults corrigeFlagHabitantSurMenagesCommuns(int nbThreads, StatusManager statusManager) {
        final CorrectionFlagHabitantProcessor processor = new CorrectionFlagHabitantProcessor(hibernateTemplate, this, transactionManager, statusManager);
        return processor.corrigeFlagSurMenages(nbThreads);
    }

    @Override
    public boolean isSourcierGris(Contribuable ctb, RegDate date) {

        if (date == null) {
            date = RegDate.get();
        }

        if (ctb instanceof PersonnePhysique) {
            final PersonnePhysique pp = (PersonnePhysique) ctb;
            return isPersonnePhysiqueSourcierGris(pp, date);
        } else if (ctb instanceof MenageCommun) {
            final MenageCommun mc = (MenageCommun) ctb;
            return isMenageCommunSourcierGris(mc, date);
        } else {
            return false;
        }
    }

    @Override
    public Set<DebiteurPrestationImposable> getDebiteursPrestationImposable(Contribuable contribuable) {

        Set<DebiteurPrestationImposable> debiteurs = null;

        final Set<RapportEntreTiers> rapports = contribuable.getRapportsSujet();
        if (rapports != null) {
            for (RapportEntreTiers r : rapports) {
                if (r.isValidAt(null) && r instanceof ContactImpotSource) {
                    final Long debiteurId = r.getObjetId();
                    final DebiteurPrestationImposable d = (DebiteurPrestationImposable) tiersDAO.get(debiteurId);
                    if (debiteurs == null) {
                        debiteurs = new HashSet<DebiteurPrestationImposable>(); // création à la demande
                    }
                    debiteurs.add(d);
                }
            }
        }

        return debiteurs;
    }

    @Override
    public RegDate getDateDebutNouvellePeriodicite(DebiteurPrestationImposable debiteur) {
        RegDate debutValidite;
        int anneeDebut = RegDate.get().year();
        Declaration derniereDeclaration = debiteur.getDerniereDeclaration();
        if (derniereDeclaration != null) {
            anneeDebut = derniereDeclaration.getPeriode().getAnnee() + 1;
            debutValidite = RegDate.get(anneeDebut, 1, 1);
        } else {
            ForFiscal forDebiteur = debiteur.getDernierForDebiteur();
            if (forDebiteur != null) {
                debutValidite = forDebiteur.getDateDebut();
            } else {
                debutValidite = RegDate.get(anneeDebut, 1, 1);
            }
        }

        return debutValidite;
    }

    private static boolean isForVaudoisSource(ForFiscalPrincipal ffp) {
        return ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffp.getModeImposition() == ModeImposition.SOURCE;
    }

    private boolean isPersonnePhysiqueSourcierGris(PersonnePhysique pp, RegDate date) {
        final boolean gris;
        if (pp.getNumeroIndividu() == null) {
            final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipalAvant(date);
            if (ffp != null) {
                // for spécifique sur la personne physique
                gris = isForVaudoisSource(ffp);
            } else {
                // un sourcier gris peut être marié, et n'est effectivement sourcier
                // gris que si l'autre membre du couple n'a pas de numéro individu non-plus
                final EnsembleTiersCouple ensemble = getEnsembleTiersCouple(pp, date);
                if (ensemble != null) {
                    final MenageCommun mc = ensemble.getMenage();
                    final PersonnePhysique autreMembre = ensemble.getConjoint(pp);
                    if (autreMembre != null && autreMembre.getNumeroIndividu() != null) {
                        gris = false;
                    } else {
                        final ForFiscalPrincipal ffpMc = mc.getDernierForFiscalPrincipalAvant(date);
                        gris = ffpMc != null && isForVaudoisSource(ffpMc);
                    }
                } else {
                    // pas marié, sans for...
                    gris = false;
                }
            }
        } else {
            // personne physique connue dans le registre civil
            gris = false;
        }
        return gris;
    }

    private boolean isMenageCommunSourcierGris(MenageCommun mc, RegDate date) {
        final EnsembleTiersCouple ensemble = getEnsembleTiersCouple(mc, date);
        final PersonnePhysique principal = ensemble.getPrincipal();
        final PersonnePhysique conjoint = ensemble.getConjoint();
        final boolean gris;
        if (principal != null || conjoint != null) {
            final boolean tousInconnusAuCivil = (principal == null || principal.getNumeroIndividu() == null) && (conjoint == null || conjoint.getNumeroIndividu() == null);
            if (tousInconnusAuCivil) {
                final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipalAvant(date);
                gris = ffp != null && isForVaudoisSource(ffp);
            } else {
                gris = false;
            }
        } else {
            // cas du ménage commun déconnecté de toute personne physique...
            gris = false;
        }
        return gris;
    }


    @NotNull
    @Override
    public Set<PersonnePhysique> getPersonnesPhysiques(MenageCommun menage) {
        return getPersonnesPhysiques(menage, false).keySet();
    }

    @NotNull
    @Override
    public Map<PersonnePhysique, RapportEntreTiers> getToutesPersonnesPhysiquesImpliquees(MenageCommun menage) {
        return getPersonnesPhysiques(menage, true);
    }

    /**
     * @param menage               un ménage-commun
     * @param aussiRapportsAnnules <b>vrai</b> si l'on veut aussi les rapports annulés; <b>faux</b> autrement.
     * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun en ignorant (ou pas) les rapports annulés ; le dernier rapport entre tiers est également indiqué
     */
    @NotNull
    private Map<PersonnePhysique, RapportEntreTiers> getPersonnesPhysiques(MenageCommun menage, boolean aussiRapportsAnnules) {
        final Map<PersonnePhysique, RapportEntreTiers> personnes = new HashMap<PersonnePhysique, RapportEntreTiers>(aussiRapportsAnnules ? 4 : 2);
        final Set<RapportEntreTiers> rapports = menage.getRapportsObjet();
        if (rapports != null) {
            for (RapportEntreTiers r : rapports) {
                if ((aussiRapportsAnnules || !r.isAnnule()) && r.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {

                    // on ne considère que les rapport dont le ménage commun est l'objet
                    // (les autres correspondent à des rattrapages de données en prod...)
                    final Long objetId = r.getObjetId();
                    if (objetId.equals(menage.getId())) {

                        final Long ppId = r.getSujetId();
                        final PersonnePhysique sujet = (PersonnePhysique) tiersDAO.get(ppId);

                        // si le rapport est annulé, on vérifie qu'il n'existe pas un
                        // autre rapport avec la même personne physique (le non-annulé a la priorité !)
                        boolean ignore = false;
                        if (r.isAnnule()) {
                            // s'il n'y est pas déjà, ou
                            // s'il y est déjà, et que l'autre date d'annulation est antérieure,
                            // alors cette nouvelle date remplace la valeur précédente
                            final RapportEntreTiers rapportConnu = personnes.get(sujet);
                            if (rapportConnu != null) {
                                final Date annulationConnue = rapportConnu.getAnnulationDate();
                                if (annulationConnue == null || annulationConnue.after(r.getAnnulationDate())) {
                                    ignore = true;
                                }
                            }
                        }
                        if (!ignore) {
                            personnes.put(sujet, r);
                        }
                    }
                }
            }
        }
        return personnes;
    }

    /**
     * @return le contribuable associé au débiteur; ou <b>null</b> si le débiteur n'en possède pas.
     */
    @Override
    public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
        return tiersDAO.getContribuable(debiteur);
    }

    @Override
    public List<String> getRaisonSociale(DebiteurPrestationImposable debiteur) {

        // si le débiteur a un tiers référent, c'est là qu'il faut chercher
        // sinon, les champs nom1 et nom2 dans le débiteur lui-même sont utilisés
        final List<String> raisonSociale;
        final Contribuable referent = getContribuable(debiteur);
        if (referent != null) {
            if (referent instanceof PersonnePhysique) {
                raisonSociale = Arrays.asList(getNomPrenom((PersonnePhysique) referent));
            } else if (referent instanceof MenageCommun) {
                raisonSociale = new ArrayList<String>(2);
                final EnsembleTiersCouple couple = getEnsembleTiersCouple((MenageCommun) referent, null);
                final PersonnePhysique principal = couple.getPrincipal();
                final PersonnePhysique conjoint = couple.getConjoint();
                final String nomPrenomPrincipal = principal != null ? getNomPrenom(principal) : null;
                final String nomPrenomConjoint = conjoint != null ? getNomPrenom(conjoint) : null;
                if (StringUtils.isNotBlank(nomPrenomPrincipal)) {
                    raisonSociale.add(nomPrenomPrincipal);
                }
                if (StringUtils.isNotBlank(nomPrenomConjoint)) {
                    raisonSociale.add(nomPrenomConjoint);
                }
            } else if (referent instanceof AutreCommunaute) {
                raisonSociale = Arrays.asList(((AutreCommunaute) referent).getNom());
            } else if (referent instanceof Entreprise) {
                raisonSociale = getRaisonSociale((Entreprise) referent);
            } else if (referent instanceof CollectiviteAdministrative) {
                try {
                    final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca = serviceInfra.getCollectivite(((CollectiviteAdministrative) referent).getNumeroCollectiviteAdministrative());
                    raisonSociale = new ArrayList<String>(3);
                    if (ca != null) {
                        final String ligne1 = ca.getNomComplet1();
                        if (StringUtils.isNotBlank(ligne1)) {
                            raisonSociale.add(ligne1.trim());
                        }
                        final String ligne2 = ca.getNomComplet2();
                        if (StringUtils.isNotBlank(ligne2)) {
                            raisonSociale.add(ligne2.trim());
                        }
                        final String ligne3 = ca.getNomComplet3();
                        if (StringUtils.isNotBlank(ligne3)) {
                            raisonSociale.add(ligne3.trim());
                        }
                    }
                } catch (ServiceInfrastructureException e) {
                    throw new RuntimeException("Impossible d'accéder à la collectivité administrative " + ((CollectiviteAdministrative) referent).getNumeroCollectiviteAdministrative());
                }
            } else {
                throw new IllegalArgumentException("Type de contribuable référent non supporté : " + referent.getClass().getName());
            }
        } else {
            // pas de tiers référent : on se sert des données connues sur le débiteur
            raisonSociale = new ArrayList<String>(2);
            if (StringUtils.isNotBlank(debiteur.getNom1())) {
                raisonSociale.add(debiteur.getNom1().trim());
            }
            if (StringUtils.isNotBlank(debiteur.getNom2())) {
                raisonSociale.add(debiteur.getNom2().trim());
            }
        }

        return raisonSociale;
    }

    @Override
    public String getRaisonSocialeAbregee(Entreprise entreprise) {
        final Long numeroEntreprise = entreprise.getNumero();
        Assert.notNull(numeroEntreprise);
        final PersonneMorale pm = servicePM.getPersonneMorale(numeroEntreprise);
        return pm != null ? StringUtils.trimToNull(pm.getRaisonSociale()) : null;
    }

    @Override
    public List<String> getRaisonSociale(Entreprise entreprise) {

        final Long numeroEntreprise = entreprise.getNumero();
        Assert.notNull(numeroEntreprise);
        final PersonneMorale pm = servicePM.getPersonneMorale(numeroEntreprise);

        final List<String> nomsComplets = new ArrayList<String>(3);
        if (pm != null) {
            if (StringUtils.isNotBlank(pm.getRaisonSociale1())) {
                nomsComplets.add(StringUtils.trimToNull(pm.getRaisonSociale1()));
            }
            if (StringUtils.isNotBlank(pm.getRaisonSociale2())) {
                nomsComplets.add(StringUtils.trimToNull(pm.getRaisonSociale2()));
            }
            if (StringUtils.isNotBlank(pm.getRaisonSociale3())) {
                nomsComplets.add(StringUtils.trimToNull(pm.getRaisonSociale3()));
            }
        }
        return nomsComplets;
    }

    @Override
    public Set<PersonnePhysique> getComposantsMenage(MenageCommun menageCommun, RegDate date) {
        if (menageCommun == null) {
            return null;
        }

        final Set<RapportEntreTiers> rapportsEntreTiers = menageCommun.getRapportsObjet();
        if (rapportsEntreTiers == null) {
            return null;
        }

        Set<PersonnePhysique> personnes = null;
        for (RapportEntreTiers rapport : rapportsEntreTiers) {
            if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType()) {
                if (date == null || rapport.isValidAt(date)) {
                    if (personnes == null) {
                        // création à la demande
                        personnes = new HashSet<PersonnePhysique>();
                    }
                    final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
                    personnes.add(pp);
                }
            }
        }

        return personnes;
    }

    @Override
    public Set<PersonnePhysique> getComposantsMenage(MenageCommun menageCommun, int anneePeriode) {
        if (menageCommun == null) {
            return null;
        }

        final Set<RapportEntreTiers> rapportsEntreTiers = menageCommun.getRapportsObjet();
        if (rapportsEntreTiers == null) {
            return null;
        }

        final DateRangeHelper.Range periode = new DateRangeHelper.Range(RegDate.get(anneePeriode, 1, 1), RegDate.get(anneePeriode, 12, 31));

        Set<PersonnePhysique> personnes = null;
        for (RapportEntreTiers rapport : rapportsEntreTiers) {
            if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType()) {
                if (DateRangeHelper.intersect(rapport, periode)) {
                    if (personnes == null) {
                        // création à la demande
                        personnes = new HashSet<PersonnePhysique>();
                    }
                    final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
                    personnes.add(pp);
                }
            }
        }

        return personnes;
    }

	private Set<Long> getIndividusAvecEvenementsCivilsRegPPNonTraites(Set<Long> nosIndividus) {
		final Set<Long> inds;
		if (!nosIndividus.isEmpty()) {
			final List<EvenementCivilRegPP> evts = evenementCivilRegPPDAO.getEvenementsCivilsNonTraites(nosIndividus);
			if (evts != null && !evts.isEmpty()) {
				inds = new HashSet<Long>(nosIndividus.size());
				for (EvenementCivilRegPP evt : evts) {
					if (evt.getNumeroIndividuPrincipal() != null) {
						inds.add(evt.getNumeroIndividuPrincipal());
					}
					if (evt.getNumeroIndividuConjoint() != null) {
						inds.add(evt.getNumeroIndividuConjoint());
					}
				}
			}
			else {
				inds = Collections.emptySet();
			}
		}
		else {
			inds = Collections.emptySet();
		}
		return inds;
	}

	private Set<Long> getIndividusAvecEvenementsCivilsECHNonTraites(Set<Long> nosIndividus) {
		final Set<Long> inds;
		if (!nosIndividus.isEmpty()) {
			final List<EvenementCivilEch> evts = evenementCivilEchDAO.getEvenementsCivilsNonTraites(nosIndividus);
			if (evts != null && !evts.isEmpty()) {
				inds = new HashSet<Long>(nosIndividus.size());
				for (EvenementCivilEch evt : evts) {
					if (evt.getNumeroIndividu() != null) {
						inds.add(evt.getNumeroIndividu());
					}
				}
			}
			else {
				inds = Collections.emptySet();
			}
		}
		else {
			inds = Collections.emptySet();
		}
		return inds;
	}

	@Override
	public Set<Long> getIndividusAvecEvenementsCivilsNonTraites(Tiers tiers) {
		final Set<Long> set;

		final Set<Long> noTiers = new HashSet<Long>(1);
		noTiers.add(tiers.getNumero());
		final Set<Long> nosIndividus = tiersDAO.getNumerosIndividu(noTiers, true);
		if (!nosIndividus.isEmpty()) {
			set = new TreeSet<Long>();
			set.addAll(getIndividusAvecEvenementsCivilsRegPPNonTraites(nosIndividus));
			set.addAll(getIndividusAvecEvenementsCivilsECHNonTraites(nosIndividus));
		}
		else {
			set = Collections.emptySet();
		}
		return set;
	}

	@Override
    public boolean isVeuvageMarieSeul(PersonnePhysique tiers) {
        MenageCommun menageCommun = findDernierMenageCommun(tiers);
        if (menageCommun != null) {
            PersonnePhysique[] personnes = getPersonnesPhysiques(menageCommun).toArray(new PersonnePhysique[getPersonnesPhysiques(menageCommun).size()]);
            if (personnes.length == 1 && EtatCivil.VEUF == situationFamilleService.getEtatCivil(tiers, RegDate.get(), true)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void adaptPremierePeriodicite(DebiteurPrestationImposable debiteurPrestationImposable, RegDate dateDebut) {
        Periodicite periodicite = debiteurPrestationImposable.getPremierePeriodicite();
        if (periodicite != null && dateDebut.isBefore(periodicite.getDateDebut())) {
            periodicite.setDateDebut(dateDebut);
        }
    }

    @Override
    public String getNomCollectiviteAdministrative(int collId) {
        String nom = null;
        try {
            final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca = serviceInfra.getCollectivite(collId);

            if (ca != null) {
                final String ligne1 = ca.getNomComplet1();
                if (StringUtils.isNotBlank(ligne1)) {
                    nom = ligne1.trim();
                }
                final String ligne2 = ca.getNomComplet2();
                if (StringUtils.isNotBlank(ligne2)) {
                    nom += ' ' + ligne2.trim();
                }
                final String ligne3 = ca.getNomComplet3();
                if (StringUtils.isNotBlank(ligne3)) {
                    nom += ' ' + ligne3.trim();
                }
            }
        } catch (ServiceInfrastructureException e) {
            throw new RuntimeException("Impossible d'accéder à la collectivité administrative " + collId);
        }

        return nom;
    }

    @Override
    public String getNomCollectiviteAdministrative(CollectiviteAdministrative collectiviteAdministrative) {
        final Integer collId = collectiviteAdministrative.getNumeroCollectiviteAdministrative();
        Assert.notNull(collId);
        return getNomCollectiviteAdministrative(collId);
    }

    @Override
    public boolean isHorsCanton(Contribuable contribuable, RegDate date) {
        ForsParTypeAt forsParTypeAt = contribuable.getForsParTypeAt(date, true);
        ForFiscalPrincipal forPrincipal = forsParTypeAt.principal;
        if (forPrincipal != null) {
            if (TypeAutoriteFiscale.COMMUNE_HC == forPrincipal.getTypeAutoriteFiscale()) {
                return true;
            }

        }
        return false;
    }

    /**
     * Extrait Le numéro d'individu à partir d'un tiers si c'est possible
     *
     * @param tiers
     * @return le numéro d'individu de la personne physique ou de la personne principal du menage. null si le tiers ne possède pas de numéro d'individu
     */
    @Override
    public Long extractNumeroIndividuPrincipal(Tiers tiers) {
        if (tiers instanceof PersonnePhysique) {
            final PersonnePhysique personne = (PersonnePhysique) tiers;
            return personne.getNumeroIndividu();
        } else if (tiers instanceof MenageCommun) {
            final MenageCommun menage = (MenageCommun) tiers;
            final PersonnePhysique personnePrincipal = getPrincipal(menage);
            if (personnePrincipal != null) {
                return personnePrincipal.getNumeroIndividu();
            }
        }
        return null;
    }

    /**
     * Recherche la presence d'un for actif sur une période
     *
     * @param ctb     un contribuable
     * @param periode une période
     * @return <b>vrai</b> si le contribuable possède au moins un for actif pendant la période spécifiée; <b>faux</b> autrement.
     */
    public static boolean isForActifSurPeriode(final ch.vd.uniregctb.tiers.Contribuable ctb, final DateRangeHelper.Range periode) {

        for (ForFiscal f : ctb.getForsFiscaux()) {
            if (DateRangeHelper.intersect(f, periode) && !f.isAnnule()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Tiers> getLinkedTiers(LinkedEntity entity, boolean includeAnnuled) {
        final Set<Tiers> tiers = new HashSet<Tiers>();
        final Set<Object> visited = new HashSet<Object>(); // contient les entités et les clés déjà visitées
        extractLinkedTiers(entity, includeAnnuled, tiers, visited);
        return tiers;
    }

    private void extractLinkedTiers(LinkedEntity entity, boolean includeAnnuled, Set<Tiers> tiers, Set<Object> visited) {

        final List<?> list = entity.getLinkedEntities(includeAnnuled);
        if (list == null) {
            return;
        }

        for (Object o : list) {

            if (visited.contains(o)) { // test sur la clé ou l'entité elle-même
                continue; // on a déjà visité cette entité
            }
            visited.add(o);

            // on met la main sur l'entité hibernate
            final HibernateEntity e;
            if (o instanceof EntityKey) {
                final EntityKey key = (EntityKey) o;
                e = (HibernateEntity) hibernateTemplate.get(key.getClazz(), (Serializable) key.getId());

                if (visited.contains(e)) { // on reteste sur l'entité uniquement
                    continue; // on a déjà visité cette entité
                }
                visited.add(e);
            } else {
                e = (HibernateEntity) o; // selon le contrat de getLinkedEntities()
            }

            // on ajoute les tiers trouvés
            if (e instanceof Tiers) {
                tiers.add((Tiers) e);
            } else if (e instanceof LinkedEntity) {
                extractLinkedTiers((LinkedEntity) e, false /* l'annulation des sous-entités est traitée séparemment, si nécessaire */, tiers, visited); // récursif
            }
        }
    }

    @Override
    public boolean isDernierForFiscalPrincipalFermePourSeparation(Tiers tiers) {
        final ForFiscalPrincipal ffp = tiers.getDernierForFiscalPrincipal();
        return ffp != null && ffp.getDateFin() != null && ffp.getMotifFermeture() == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
    }

    @Override
    public boolean isMenageActif(MenageCommun menage, RegDate date) {

        final Set<RapportEntreTiers> rapports = menage.getRapportsObjet();
        if (rapports != null) {
            for (RapportEntreTiers rapport : rapports) {
                if (rapport instanceof AppartenanceMenage && rapport.isValidAt(date)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void traiterReOuvertureForDebiteur(ForFiscal forFiscal) {

        if (forFiscal instanceof ForDebiteurPrestationImposable) {
            ForDebiteurPrestationImposable forDebiteur = (ForDebiteurPrestationImposable) forFiscal;
            final RegDate dateFin = forDebiteur.getDateFin();
            reopenForDebiteur(forDebiteur);
            DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) forDebiteur.getTiers();
            reopenRapportsPrestationImposableFermesAt(dpi, dateFin);
        }

    }

    @Override
    public NumerosOfficesImpot getOfficesImpot(int noOfs, @Nullable RegDate date) {

        final Commune commune = serviceInfra.getCommuneByNumeroOfsEtendu(noOfs, date);
        if (commune == null || !commune.isVaudoise()) {
            return null;
        }

        final District district = commune.getDistrict();
        if (district == null) {
            return null;
        }

        final Region region = district.getRegion();
        final Integer codeDistrict = district.getCode();
        final Integer codeRegion = region == null ? null : region.getCode();

        final CollectiviteAdministrative oid = codeDistrict == null ? null : tiersDAO.getCollectiviteAdministrativeForDistrict(codeDistrict);
        final CollectiviteAdministrative oir = codeRegion == null ? null : tiersDAO.getCollectiviteAdministrativeForRegion(codeRegion);

        return new NumerosOfficesImpot(oid == null ? 0 : oid.getNumero(), oir == null ? 0 : oir.getNumero());
    }

}

package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.EntityKey;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.GentilComparator;
import ch.vd.uniregctb.common.GentilDateRangeExtendedAdapterCallback;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.LiteralStringHelper;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.common.NationaliteHelper;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.common.Rerangeable;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.ide.ServiceIDEService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessorInternal;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext;
import ch.vd.uniregctb.parentes.ParenteUpdateInfo;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.tiers.Contribuable.FirstForsList;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.tiers.etats.TransitionEtatEntrepriseService;
import ch.vd.uniregctb.tiers.etats.transition.TransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantProcessor;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantResults;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.GroupeFlagsEntreprise;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.StatutMenageCommun;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.ValidationInterceptor;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Service de recherche des tiers. Effectue conjointement la recherche en base et dans le moteur de recherche.
 */
public class TiersServiceImpl implements TiersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TiersServiceImpl.class);
	private static final int AGE_MAXIMUM_LIEN_VERS_TIERS = 5;

	private TiersDAO tiersDAO;
	private EvenementFiscalService evenementFiscalService;
	private GlobalTiersSearcher tiersSearcher;
	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;
	private EvenementCivilEchDAO evenementCivilEchDAO;
	private ServiceInfrastructureService serviceInfra;
	private ServiceCivilService serviceCivilService;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private ServiceOrganisationService serviceOrganisationService;
	private ServiceIDEService serviceIDEService;
	private ServiceRegimeFiscal serviceRegimeFiscal;
	private TacheService tacheService;
	private SituationFamilleService situationFamilleService;
	private AdresseService adresseService;
	private RemarqueDAO remarqueDAO;
	private ValidationService validationService;
	private ValidationInterceptor validationInterceptor;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private AssujettissementService assujettissementService;
	private RapportEntreTiersDAO rapportEntreTiersDAO;
	private FlagBlocageRemboursementAutomatiqueCalculationRegister flagBlocageRembAutoCalculateurDecale;
	private BouclementService bouclementService;
	private TransitionEtatEntrepriseService transitionEtatEntrepriseService;

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

    public void setTiersDAO(TiersDAO tiersDAO) {
        this.tiersDAO = tiersDAO;
    }

    public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
        this.serviceInfra = serviceInfra;
    }

    public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
        this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
    }

    public void setServiceCivilService(ServiceCivilService serviceCivil) {
        this.serviceCivilService = serviceCivil;
    }

	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceIDEService(ServiceIDEService serviceIDEService) {
		this.serviceIDEService = serviceIDEService;
	}

	public void setServiceRegimeFiscal(ServiceRegimeFiscal serviceRegimeFiscal) {
		this.serviceRegimeFiscal = serviceRegimeFiscal;
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

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
        this.validationInterceptor = validationInterceptor;
    }

	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setFlagBlocageRembAutoCalculateurDecale(FlagBlocageRemboursementAutomatiqueCalculationRegister flagBlocageRembAutoCalculateurDecale) {
		this.flagBlocageRembAutoCalculateurDecale = flagBlocageRembAutoCalculateurDecale;
	}

	public void setBouclementService(BouclementService bouclementService) {
		this.bouclementService = bouclementService;
	}

	public void setTransitionEtatEntrepriseService(TransitionEtatEntrepriseService transitionEtatEntrepriseService) {
		this.transitionEtatEntrepriseService = transitionEtatEntrepriseService;
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

	/**
     * Renvoie l'entreprise correspondant au numéro d'individu passé en paramètre.
     *
     * @param numeroOrganisation le numéro de l'organisation.
     * @return l'entreprise correspondante au numéro d'organisation passé en paramètre, ou <b>null</b> s'il n'existe pas.
     */
    @Override
    public Entreprise getEntrepriseByNumeroOrganisation(long numeroOrganisation) {
        return tiersDAO.getEntrepriseByNumeroOrganisation(numeroOrganisation);
    }

	@Override
	public Etablissement getEtablissementByNumeroSite(long numeroSite) {
		return tiersDAO.getEtablissementByNumeroSite(numeroSite);
	}

	@Override
	public List<DateRanged<Etablissement>> getEtablissementsPrincipauxEntreprise(Entreprise entreprise) {
		return getEtablissementsEntreprise(entreprise, true, ActiviteEconomique::isPrincipal);
	}

	@Override
	public List<DateRanged<Etablissement>> getEtablissementsSecondairesEntreprise(Entreprise entreprise) {
		return getEtablissementsEntreprise(entreprise, true, ae -> !ae.isPrincipal());
	}

	@Override
	public List<Etablissement> getEtablissementsSecondairesEntrepriseSansRange(Entreprise entreprise) {
		final List<DateRanged<Etablissement>> etablissementsRanges = getEtablissementsSecondairesEntreprise(entreprise);
		Set<Etablissement> etablissements = new HashSet<>(etablissementsRanges.size());
		if (!etablissementsRanges.isEmpty()) {
			for (DateRanged<Etablissement> range : etablissementsRanges) {
				etablissements.add(range.getPayload());
			}
		}
		return new ArrayList<>(etablissements);
	}


	@Override
	public List<Etablissement> getEtablissementsSecondairesEntreprise(Entreprise entreprise, final RegDate date) {
		final List<DateRanged<Etablissement>> etablissementsEntreprise = getEtablissementsEntreprise(entreprise, true, ae -> {
				final RegDate dateDeValeur = date != null ? date : RegDate.get();
				return ae.isValidAt(dateDeValeur) && !ae.isPrincipal();
		});
		List<Etablissement> etablissements = new ArrayList<>(etablissementsEntreprise.size());
		for (DateRanged<Etablissement> etablissementDateRanged : etablissementsEntreprise) {
			etablissements.add(etablissementDateRanged.getPayload());
		}
		return etablissements;
	}

	/**
	 * NOTE: un établissement peut apparaître plusieurs fois dans la liste, en fonction de son historique
	 * de rattachement à l'entreprise.
	 */
	private List<DateRanged<Etablissement>> getEtablissementsEntreprise(Entreprise entreprise, boolean avecTri, Predicate<ActiviteEconomique> filtre) {
		final Set<RapportEntreTiers> sujets = entreprise.getRapportsSujet();
		final List<DateRanged<Etablissement>> etablissements = new LinkedList<>();
		for (RapportEntreTiers ret : sujets) {
			if (!ret.isAnnule() && ret instanceof ActiviteEconomique && filtre.test((ActiviteEconomique) ret)) {
				final Etablissement etb = (Etablissement) getTiers(ret.getObjetId());
				if (etb != null) {
					etablissements.add(new DateRanged<>(ret.getDateDebut(), ret.getDateFin(), etb));
				}
				else {
					LOGGER.warn(String.format("Etablissement non trouvé (%d) sur le rapport %d.", ret.getObjetId(), ret.getId()));
				}
			}
		}

		if (avecTri) {
			final List<DateRanged<Etablissement>> tries = new ArrayList<>(etablissements);
			Collections.sort(tries, new DateRangeComparator<>());
			return tries;
		}
		else {
			return etablissements;
		}
	}

	/**
	 * Créée une nouvelle entreprise pour l'organisation de l'événement dont l'id est passé en paramètre.
	 * Rapporte cette création dans une entrée "suivi" dans les erreurs de l'événement.
	 *
	 * <p>
	 *     La méthode vérifie que l'entreprise n'existe pas déjà et lance une {@link IllegalStateException} si c'est le cas.
	 * </p>
	 * @param evt L'événement organisation
	 * @return L'entreprise créé
	 */
	@Override
	public Entreprise createEntreprisePourEvenementOrganisation(EvenementOrganisation evt) throws TiersException {
		final RegDate dateEvt = evt.getDateEvenement();
		final RegDate dateCreation;

		final long noOrganisation = evt.getNoOrganisation();
		final Organisation organisation = serviceOrganisationService.getOrganisationHistory(noOrganisation);
		final SiteOrganisation sitePrincipal = organisation.getSitePrincipal(dateEvt).getPayload();

		final InscriptionRC inscriptionRC = sitePrincipal.getDonneesRC().getInscription(dateEvt);
		final RegDate dateInscriptionVd = inscriptionRC != null ? inscriptionRC.getDateInscriptionVD() : null;
		if (dateInscriptionVd != null && dateInscriptionVd.isAfter(dateEvt.addDays(- OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC))) {
			dateCreation = dateInscriptionVd.getOneDayAfter();
		}
		else {
			dateCreation = dateEvt.getOneDayAfter();
		}

		final Entreprise entreprise = createEntreprise(noOrganisation);
		if (organisation.isInscriteAuRC(dateCreation)) {
			changeEtatEntreprise(TypeEtatEntreprise.INSCRITE_RC, entreprise, dateCreation, TypeGenerationEtatEntreprise.MANUELLE);
		} else {
			changeEtatEntreprise(TypeEtatEntreprise.FONDEE, entreprise, dateCreation, TypeGenerationEtatEntreprise.MANUELLE);
		}

		final Etablissement etablissement = createEtablissement(sitePrincipal.getNumeroSite());

		// L'activité économique
		addRapport(new ActiviteEconomique(dateCreation, null, entreprise, etablissement, true), entreprise, etablissement);

		if (dateInscriptionVd != null) {
			appliqueDonneesCivilesSurPeriode(entreprise, new DateRangeHelper.Range(dateInscriptionVd, dateEvt.getOneDayBefore()), dateEvt);
		}

		// NOTE: C'est volontaire si le domicile n'est pas créé. Ca permet de le différencier d'un établissement préexistant dans Unireg.

		final EvenementOrganisationErreur evtErreur = new EvenementOrganisationErreur();
		evtErreur.setType(TypeEvenementErreur.SUIVI);
		evtErreur.setMessage(String.format("Entreprise créée manuellement avec le numéro de contribuable %s et l'état initial %s pour l'organisation %s", entreprise.getNumero(), entreprise.getEtatActuel(), noOrganisation));
		evt.getErreurs().add(evtErreur);
		return entreprise;
	}

	/**
	 * Créer une entreprise pour le numéro d'organisation fourni. La méthode refuse de la créer si une entreprise est déjà associée à l'organisation.
	 *
	 * @param noOrganisation
	 * @return L'entreprise créée.
	 */
	@NotNull
	@Override
	public Entreprise createEntreprise(long noOrganisation) {
		Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(noOrganisation);
		if (entreprise != null) {
			throw new IllegalStateException(String.format("Il existe déjà une entreprise pour l'organisation %s", noOrganisation));
		}
		entreprise = new Entreprise();
		entreprise.setNumeroEntreprise(noOrganisation);
		return (Entreprise) tiersDAO.save(entreprise);
	}

	/**
	 * Créer un établissement pour le numéro de site fourni. La méthode refuse de le créer si un établissement est déjà associé au site.
	 *
	 * @param numeroSite
	 * @return L'établissement créé.
	 */
	@NotNull
	@Override
	public Etablissement createEtablissement(Long numeroSite) {
		Etablissement etablissement = tiersDAO.getEtablissementByNumeroSite(numeroSite);
		if (etablissement != null) {
			throw new IllegalStateException(String.format("Il existe déjà un établissement pour le site %s", numeroSite));
		}
		etablissement = new Etablissement();
		etablissement.setNumeroEtablissement(numeroSite);
		return (Etablissement) tiersDAO.save(etablissement);
	}

	@Override
	public List<DateRanged<Contribuable>> getEntitesJuridiquesEtablissement(Etablissement etablissement) {
		final Set<RapportEntreTiers> objets = etablissement.getRapportsObjet();
		final List<DateRanged<Contribuable>> entites = new LinkedList<>();
		for (RapportEntreTiers ret : objets) {
			if (!ret.isAnnule() && ret instanceof ActiviteEconomique) {
				final Contribuable ctb = (Contribuable) getTiers(ret.getSujetId());
				if (ctb != null) {
					entites.add(new DateRanged<>(ret.getDateDebut(), ret.getDateFin(), ctb));
				}
				else {
					LOGGER.warn(String.format("Contribuable non-trouvé (%d) sur le rapport %d.", ret.getSujetId(), ret.getId()));
				}
			}
		}

		final List<DateRanged<Contribuable>> tries = new ArrayList<>(entites);
		Collections.sort(tries, new DateRangeComparator<>());
		return tries;
	}

	@Override
    public Tiers getTiers(long numeroTiers) {
        return tiersDAO.get(numeroTiers);
    }

	/**
	 * change un non habitant en habitant (en cas d'arrivée HC ou HS)
	 *
	 * @param nonHabitant la PP de type nonHabitant
	 * @param numInd      le numéro d'individu de l'habitant
	 * @param date        la dte du changement (si aucune date donnée, ni les situations de famille ni les adresses ne seront modifiées = rattrapage!)
	 * @return la même PP de type habitant maintenant
	 */
	private PersonnePhysique changeNHenHabitant(PersonnePhysique nonHabitant, Long numInd, RegDate date) {
        Assert.isFalse(nonHabitant.isHabitantVD(), "changeNHenHabitant : la PP fourni est habitant");
        nonHabitant.setNumeroIndividu(numInd);
        nonHabitant.setNumeroAssureSocial(null);
        nonHabitant.setNom(null);
        nonHabitant.setPrenomUsuel(null);
        nonHabitant.setTousPrenoms(null);
        nonHabitant.setDateNaissance(null);
		nonHabitant.setNomNaissance(null);
		nonHabitant.setDateDeces(null);
        nonHabitant.setSexe(null);
        nonHabitant.setNumeroOfsNationalite(null);
        nonHabitant.setCategorieEtranger(null);
        nonHabitant.setDateDebutValiditeAutorisation(null);
        nonHabitant.setIdentificationsPersonnes(null);
		nonHabitant.setNomPere(null);
		nonHabitant.setPrenomsPere(null);
		nonHabitant.setNomMere(null);
		nonHabitant.setPrenomsMere(null);
		nonHabitant.setOrigine(null);

		// si on a donné une date de référence, on s'attaque aux situations de famille et aux adresses surchargées non-permanentes
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
	                    adr.setAnnule(true);
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
	 * @param habitant un habitant
	 * @return l'habitant transformé en non-habitant
	 */
    private PersonnePhysique changeHabitantenNH(PersonnePhysique habitant) {

        Assert.isTrue(habitant.isHabitantVD(), "changeHabitantenNH : la PP fourni n'est pas habitant");
        final Individu ind = getIndividu(habitant);
        habitant.setNumeroAssureSocial(ind.getNouveauNoAVS());
        habitant.setNom(ind.getNom());
        habitant.setPrenomUsuel(ind.getPrenomUsuel());
        habitant.setTousPrenoms(ind.getTousPrenoms());
        habitant.setDateNaissance(ind.getDateNaissance());
	    habitant.setNomNaissance(ind.getNomNaissance());
	    habitant.setDateDeces(ind.getDateDeces());
	    habitant.setSexe(ind.getSexe());

	    final Individu individu = serviceCivilService.getIndividu(habitant.getNumeroIndividu(), null, AttributeIndividu.NATIONALITES, AttributeIndividu.PERMIS, AttributeIndividu.ORIGINE);
	    if (individu == null) {
		    throw new IndividuNotFoundException(habitant.getNumeroIndividu());
	    }

	    // noms et prénoms officiels de la mère
	    final NomPrenom nomOfficielMere = individu.getNomOfficielMere();
	    if (nomOfficielMere != null) {
	        habitant.setNomMere(nomOfficielMere.getNom());
	        habitant.setPrenomsMere(nomOfficielMere.getPrenom());
        }
	    else {
		    habitant.setNomMere(null);
		    habitant.setPrenomsMere(null);
	    }

	    // nom et prénoms officiels du père
	    final NomPrenom nomOfficielPere = individu.getNomOfficielPere();
	    if (nomOfficielPere != null) {
		    habitant.setNomPere(nomOfficielPere.getNom());
		    habitant.setPrenomsPere(nomOfficielPere.getPrenom());
	    }
	    else {
		    habitant.setNomPere(null);
		    habitant.setPrenomsPere(null);
	    }

	    // nationalité
	    final Nationalite nationalite = NationaliteHelper.refAt(individu.getNationalites(), null);
	    if (nationalite != null) {
		    habitant.setNumeroOfsNationalite(nationalite.getPays().getNoOFS());
	    }
	    else {
		    habitant.setNumeroOfsNationalite(null);
	    }

	    //permis
        final Permis dernierPermis = individu.getPermis().getPermisActif(null);
        if (dernierPermis != null) {
            habitant.setCategorieEtranger(CategorieEtranger.valueOf(dernierPermis.getTypePermis()));
            habitant.setDateDebutValiditeAutorisation(dernierPermis.getDateDebut());
        } else {
            habitant.setCategorieEtranger(null);
            habitant.setDateDebutValiditeAutorisation(null);
        }

	    // Commune d'origine
	    if (individu.getOrigines() != null && !individu.getOrigines().isEmpty()) {
		    final Origine first = individu.getOrigines().iterator().next();
		    habitant.setOrigine(new OriginePersonnePhysique(first.getNomLieu(), first.getSigleCanton()));
	    }
	    else {
		    habitant.setOrigine(null);
	    }

        // identification navs11 et numRCE
        setIdentifiantsPersonne(habitant, ind.getNoAVS11(), ind.getNumeroRCE());

        habitant.setHabitant(false);
        return habitant;
    }

	@Override
	@NotNull
	public PersonnePhysique createNonHabitantFromIndividu(long numeroIndividu) {

		if (tiersDAO.getPPByNumeroIndividu(numeroIndividu) != null) {
			throw new IllegalArgumentException("Il existe déjà une personne physique avec le numéro d'individu = " + numeroIndividu);
		}

		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.save(new PersonnePhysique(numeroIndividu));
		changeHabitantenNH(pp);
		return pp;
	}

	private UpdateHabitantFlagResultat getFlagHabitantChangementNecessaire(@NotNull PersonnePhysique pp, long noInd) throws TiersException {
		final Individu individu = serviceCivilService.getIndividu(noInd, null, AttributeIndividu.ADRESSES);
		if (individu == null) {
			throw new IndividuNotFoundException(noInd);
		}

		final boolean dansLeCanton;
		if (individu.getDateDeces() == null) {
			final Boolean isVaudois = isDomicileDansLeCanton(pp, RegDate.get(), false);
			if (isVaudois == null) {
				throw new TiersException("Impossible de déterminer si le domicile du contribuable " + pp.getNumero() + " est vaudois ou pas");
			}
			dansLeCanton = isVaudois;
		}
		else {
			// les décédés civils sont toujours non-habitants
			dansLeCanton = false;
		}

		// on met-à-jour le flag si nécessaire
		if (dansLeCanton && pp.isHabitantVD() || (!dansLeCanton && !pp.isHabitantVD())) {
			// rien à faire
			return UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT;
		}

		return dansLeCanton ? UpdateHabitantFlagResultat.CHANGE_EN_HABITANT : UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT;
	}

	/**
	 * Gère un éventuel changement de la valeur de l'état habitant d'une personne physique. Le flag lui-même dépend de l'adresse de résidence courante
	 * de l'individu civil, et si une date de référence est données, on considèrera qu'il s'agit d'une réelle transition à une date donnée (et pas d'une
	 * correction de données) et cela pourra avoir une influence sur les situations de famille et les surcharges d'adresses non-permanentes valides après
	 * cette date
	 * @param pp la personne physique
	 * @param noInd le numéro d'individu correspondant
	 * @param numeroEvenement un éventuel numéro d'événement civil (pour l'audit)
	 * @param dateReferenceDonnees une éventuelle date de référence pour la manipulation des adresses
	 * @return un indicateur de ce qui a été fait
	 * @throws TiersException s'il n'est pas possible de déterminer si le domicile du contribuable est vaudois ou pas
	 */
	private UpdateHabitantFlagResultat manageHabitantStatus(@NotNull PersonnePhysique pp, long noInd, @Nullable Long numeroEvenement, @Nullable RegDate dateReferenceDonnees) throws TiersException {
		final UpdateHabitantFlagResultat changement = getFlagHabitantChangementNecessaire(pp, noInd);
		if (changement == UpdateHabitantFlagResultat.CHANGE_EN_HABITANT) {
			changeNHenHabitant(pp, noInd, dateReferenceDonnees);
			Audit.info(numeroEvenement, "La personne physique n°" + pp.getNumero() + " a été passée habitante.");
		}
		else if (changement == UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT) {
			changeHabitantenNH(pp);
			Audit.info(numeroEvenement, "La personne physique n°" + pp.getNumero() + " a été passée non-habitante.");
		}
		return changement;
	}

	@Override
	public UpdateHabitantFlagResultat updateHabitantFlag(@NotNull PersonnePhysique pp, long noInd, Long numeroEvenement) throws TiersException {
		return manageHabitantStatus(pp, noInd, numeroEvenement, null);
	}

	@Override
	public UpdateHabitantFlagResultat updateHabitantStatus(@NotNull PersonnePhysique pp, long noInd, @Nullable RegDate date, Long numeroEvenement) throws TiersException {
		return manageHabitantStatus(pp, noInd, numeroEvenement, date == null ? RegDate.get() : date);
	}

	@Override
	public Boolean isHabitantResidencePrincipale(@NotNull PersonnePhysique pp, RegDate date) {
		return isDomicileDansLeCanton(pp, date, true);
	}

	@Override
    public void setIdentifiantsPersonne(PersonnePhysique nonHabitant, String navs11, String numRce) {
        final Set<IdentificationPersonne> set = new HashSet<>(2);

        // numéro avs à 11 positions
        if (StringUtils.isNotBlank(navs11)) {
            navs11 = FormatNumeroHelper.removeSpaceAndDash(navs11);
	        if (navs11.length() == 8) {
		        navs11 = navs11.concat("000");
	        }
	        try {
		        set.add(getOrCreateIdentifiantPersonne(nonHabitant, CategorieIdentifiant.CH_AHV_AVS, navs11));
	        }
	        catch (IdentifiantInvalideException e) {
		        LOGGER.warn(String.format("Identifiant personne %s '%s'ignoré sur contribuable %s : %s.",
		                                  CategorieIdentifiant.CH_AHV_AVS,
		                                  navs11,
		                                  FormatNumeroHelper.numeroCTBToDisplay(nonHabitant.getNumero()),
		                                  e.getMessage()));
	        }
        }

        // numéro du registre des étrangers
        if (StringUtils.isNotBlank(numRce) && !"0".equals(StringUtils.trimToEmpty(numRce))) {
	        try {
		        set.add(getOrCreateIdentifiantPersonne(nonHabitant, CategorieIdentifiant.CH_ZAR_RCE, numRce.trim()));
	        }
	        catch (IdentifiantInvalideException e) {
		        LOGGER.warn(String.format("Identifiant personne %s '%s'ignoré sur contribuable %s : %s.",
		                                  CategorieIdentifiant.CH_ZAR_RCE,
		                                  numRce,
		                                  FormatNumeroHelper.numeroCTBToDisplay(nonHabitant.getNumero()),
		                                  e.getMessage()));
	        }
        }

        // assignation des identifiants
        nonHabitant.setIdentificationsPersonnes(set);
    }

	private static class IdentifiantInvalideException extends Exception {
		public IdentifiantInvalideException(String message) {
			super(message);
		}
	}

	private static IdentificationPersonne getOrCreateIdentifiantPersonne(PersonnePhysique nonHabitant, CategorieIdentifiant categorie, String nouvelleValeur) throws IdentifiantInvalideException {
		final String nouvelleValeurTrimmee = StringUtils.trimToNull(nouvelleValeur);
		if (nouvelleValeurTrimmee == null) {
			throw new IdentifiantInvalideException("identifiant vide");
		}
		if (nouvelleValeurTrimmee.length() > LengthConstants.IDENTPERSONNE_IDENTIFIANT) {
			throw new IdentifiantInvalideException(String.format("identifiant trop long (%d là où seulement %d caractères sont acceptés)",
			                                                     nouvelleValeurTrimmee.length(),
			                                                     LengthConstants.IDENTPERSONNE_IDENTIFIANT));
		}

		final Set<IdentificationPersonne> ips = nonHabitant.getIdentificationsPersonnes();
		if (ips != null) {
			for (IdentificationPersonne ip : ips) {
				if (ip.getCategorieIdentifiant() == categorie) {
					ip.setIdentifiant(nouvelleValeur);
					return ip;
				}
			}
		}

		final IdentificationPersonne newIp = new IdentificationPersonne();
		newIp.setCategorieIdentifiant(categorie);
		newIp.setIdentifiant(nouvelleValeur);
		newIp.setPersonnePhysique(nonHabitant);
		return newIp;
	}

	@Override
	public void setIdentifiantEntreprise(Contribuable contribuable, String ide) {
		checkEditionCivileAutorisee(contribuable);
		final Set<IdentificationEntreprise> set = new HashSet<>(1);
		final String normalizedIde = NumeroIDEHelper.normalize(ide);
		if (StringUtils.isNotBlank(normalizedIde)) {
			set.add(getOrCreateIdentifiantEntreprise(contribuable, normalizedIde));
		}
		contribuable.setIdentificationsEntreprise(set);
	}

	@Override
	public void checkEditionCivileAutorisee(Contribuable contribuable)  {
		final RegDate aujourdhui = RegDate.get();
		if (contribuable instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) contribuable;
			final DegreAssociationRegistreCivil degreAssociationRegistreCivil = determineDegreAssociationCivil(entreprise, aujourdhui);
			if (degreAssociationRegistreCivil == DegreAssociationRegistreCivil.CIVIL_ESCLAVE) {
				throw new AccessDeniedException(
						String.format("Impossible d'éditer l'entreprise n°%s, elle est connue au civil et Unireg n'est pas service IDE à obligations étendues.",
						              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())
						)
				);
			}
		} else if (contribuable instanceof Etablissement) {
			final Etablissement etablissement = (Etablissement) contribuable;
			final DegreAssociationRegistreCivil degreAssociationRegistreCivil = determineDegreAssociationCivil(etablissement, aujourdhui);
			if (degreAssociationRegistreCivil == DegreAssociationRegistreCivil.CIVIL_ESCLAVE) {
				throw new AccessDeniedException(
						String.format("Impossible d'éditer l'établissement n°%s, il est connu au civil et Unireg n'est pas service IDE à obligations étendues.",
						              FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())
						)
				);
			}
		}
	}

	private static IdentificationEntreprise getOrCreateIdentifiantEntreprise(Contribuable contribuable, String ide) {
		final Set<IdentificationEntreprise> ies = contribuable.getIdentificationsEntreprise();
		if (ies != null) {
			for (IdentificationEntreprise ie : ies) {
				if (ide.equals(ie.getNumeroIde())) {
					return ie;
				}
			}
		}

		final IdentificationEntreprise newIe = new IdentificationEntreprise();
		newIe.setNumeroIde(ide);
		newIe.setCtb(contribuable);
		return newIe;
	}

    /* (non-Javadoc)
	 * @see ch.vd.uniregctb.tiers.TiersService#changeNHenMenage(long)
	 */
    // ATTENTION: cette fonction ne doit être appelée que si c'est STRICTEMENT nécessaire!!!
    @Override
    public void changeNHenMenage(final long numeroTiers) {

        final DiscriminatorColumn typeAnnotation = AnnotationUtils.findAnnotation(Tiers.class, DiscriminatorColumn.class);
        final DiscriminatorValue discrimatorAnnotationMenage = AnnotationUtils.findAnnotation(MenageCommun.class, DiscriminatorValue.class);
	    final DiscriminatorValue discrimatorAnnotationPersonnePhysique = AnnotationUtils.findAnnotation(PersonnePhysique.class, DiscriminatorValue.class);

        if (typeAnnotation == null || discrimatorAnnotationMenage == null || discrimatorAnnotationPersonnePhysique == null) {
            throw new RuntimeException("Impossible de changer le type du tiers n° " + numeroTiers + " à ménageCommun");
        }

        final List<VueSituationFamille> histoSF = situationFamilleService.getVueHisto((Contribuable) tiersDAO.get(numeroTiers));

        // effacement des liens d'identification (qui ne concernent qu'une personne physique, pas un ménage commun)
        // [UNIREG-2893] effacement des droits d'accès (qui ne concernent que les personnes physiques)
	    // [SIFISC-13187] effacement des situations de famille dont la future ex-personne physique est le contribuable principal
        hibernateTemplate.execute(new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                {
                    final String deleteQuery = "DELETE FROM IDENTIFICATION_PERSONNE WHERE NON_HABITANT_ID=:tiersId";
                    final SQLQuery query = session.createSQLQuery(deleteQuery);
                    query.setLong("tiersId", numeroTiers);
                    query.executeUpdate();
                }
                {
                    final String deleteQuery = "DELETE FROM DROIT_ACCES WHERE TIERS_ID=:tiersId";
                    final SQLQuery query = session.createSQLQuery(deleteQuery);
                    query.setLong("tiersId", numeroTiers);
                    query.executeUpdate();
                }
                {
                    final String deleteQuery = "DELETE FROM SITUATION_FAMILLE WHERE TIERS_PRINCIPAL_ID=:tiersId";
                    final SQLQuery query = session.createSQLQuery(deleteQuery);
                    query.setLong("tiersId", numeroTiers);
                    query.executeUpdate();
                }
	            {
		            final String deleteQuery = "DELETE FROM RAPPORT_ENTRE_TIERS WHERE TIERS_SUJET_ID=:tiersId AND RAPPORT_ENTRE_TIERS_TYPE='AppartenanceMenage'";
		            final SQLQuery query = session.createSQLQuery(deleteQuery);
		            query.setLong("tiersId", numeroTiers);
		            query.executeUpdate();
	            }
	            {
		            final String deleteQuery = "DELETE FROM RAPPORT_ENTRE_TIERS WHERE (TIERS_SUJET_ID=:tiersId OR TIERS_OBJET_ID=:tiersId) AND RAPPORT_ENTRE_TIERS_TYPE='Parente'";
		            final SQLQuery query = session.createSQLQuery(deleteQuery);
		            query.setLong("tiersId", numeroTiers);
		            query.executeUpdate();
	            }

                return null;
            }
        });

        // changement du type de tiers
        hibernateTemplate.execute(new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {

	            final SQLQuery query = session.createSQLQuery("UPDATE TIERS SET " + typeAnnotation.name() + "=:newType, LOG_MDATE=CURRENT_DATE, LOG_MUSER=:muser, " +
			                                   "PP_HABITANT=NULL, " +
			                                   "NUMERO_INDIVIDU=NULL, " +
			                                   "ANCIEN_NUMERO_SOURCIER = null," +
			                                   "NH_NUMERO_ASSURE_SOCIAL = null," +
			                                   "NH_NOM_NAISSANCE = null," +
			                                   "NH_NOM = null," +
			                                   "NH_PRENOM = null," +
			                                   "NH_DATE_NAISSANCE = null," +
			                                   "NH_SEXE = null," +
			                                   "NH_NO_OFS_NATIONALITE = null," +
			                                   "NH_LIBELLE_ORIGINE = null," +
			                                   "NH_CANTON_ORIGINE = null," +
			                                   "NH_CAT_ETRANGER = null," +
			                                   "NH_DATE_DEBUT_VALID_AUTORIS = null," +
			                                   "DATE_DECES = null," +
			                                   "MAJORITE_TRAITEE = null " +
			                                   "WHERE NUMERO=:id AND TIERS_TYPE=:oldType");

	            query.setParameter("newType", discrimatorAnnotationMenage.value());
	            query.setParameter("muser", AuthenticationHelper.getCurrentPrincipal());
	            query.setParameter("id", numeroTiers);
	            query.setParameter("oldType", discrimatorAnnotationPersonnePhysique.value());
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

                    final String updateSFQuery = String.format("update SITUATION_FAMILLE set LOG_MDATE=CURRENT_DATE, LOG_MUSER=:user, %1$s=:newClass where ID=:id", columnTypeSituationFamille.name());

                    final SQLQuery query = session.createSQLQuery(updateSFQuery);
	                query.setParameter("user", AuthenticationHelper.getCurrentPrincipal());
	                query.setParameter("newClass", valueTypeSituationFamille.value());
	                query.setParameter("id", idSF);
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

    private interface IndividuProvider {
        Individu getIndividu(PersonnePhysique pp);
    }

    private final IndividuProvider individuProviderWithoutDate = TiersServiceImpl.this::getIndividu;

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
            sexe = individu.getSexe();
        }
        else {
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
                AttributeIndividu.NATIONALITES, AttributeIndividu.PERMIS, AttributeIndividu.ORIGINE);

        // A-t-il une nationalité suisse en cours et/ou des nationalites étrangères ?
	    final Nationalite nationalite = NationaliteHelper.refAt(individu.getNationalites(), date);
	    boolean nationaliteSuisse = false;
	    boolean nationalitesEtrangeres = false;
	    if (nationalite != null) {
	        final int noOFS = nationalite.getPays().getNoOFS();
	        nationaliteSuisse = noOFS == ServiceInfrastructureService.noOfsSuisse;
	        nationalitesEtrangeres = noOFS != ServiceInfrastructureService.noPaysInconnu;
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

	@Override
	public boolean isSuisse(PersonnePhysique pp, RegDate date) throws TiersException {

		if (pp.isHabitantVD()) {
			final Individu individu = getIndividu(pp, date, AttributeIndividu.NATIONALITES);
			if (individu == null) {
				throw new IndividuNotFoundException(pp.getNumeroIndividu());
			}
			final Nationalite nationalite = NationaliteHelper.refAt(individu.getNationalites(), date);
			if (nationalite == null) {
				throw new TiersException("Impossible de déterminer la nationalité de l'individu n°" + pp.getNumeroIndividu());
			}
			return nationalite.getPays().isSuisse();
		}
		else {
			final Integer numeroOfsNationalite = pp.getNumeroOfsNationalite();
			if (numeroOfsNationalite == null) {
				throw new TiersException("La nationalité du contribuable " + FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()) + " est inconnue");
			}
			return (numeroOfsNationalite == ServiceInfrastructureService.noOfsSuisse);
		}
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
    public boolean isSuisse(Individu individu, @Nullable RegDate date) throws TiersException {
	    final Nationalite nationalite = NationaliteHelper.refAt(individu.getNationalites(), date);
        if (nationalite == null) {
            throw new TiersException("Impossible de déterminer la nationalité de l'individu n°" + individu.getNoTechnique());
        }
        return nationalite.getPays().isSuisse();
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
                throw new TiersException("Impossible de déterminer la nationalité du contribuable " + FormatNumeroHelper.numeroCTBToDisplay(nonHabitant.getNumero()));
            }
        }
    }

    /**
     * Détermination de l'individidu secondaire <ul> <li>2 personnes de meme sexe : le deuxieme dans l'ordre alphabétique est le secondaire</li> <li>2 personnes de sexe different : la femme est le
     * secondaire</li> </ul>
     */
    @Override
    public PersonnePhysique getPrincipal(PersonnePhysique tiers1, PersonnePhysique tiers2) {

	    // Pour le cas spécial où le sexe d'un des deux tiers est inconnu: on part du principe que c'est un couple "normal" (non-pacsé)
	    final Comparator<Sexe> sexeComparator = new GentilComparator<>(Arrays.asList(Sexe.MASCULIN, null, Sexe.FEMININ));
	    final Comparator<String> nomPrenomComparator = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    	return Stream.of(tiers1, tiers2)
			    .filter(Objects::nonNull)
			    .min(Comparator.<PersonnePhysique, Sexe>comparing(this::getSexe, sexeComparator)        // Monsieur avant Madame
					         .thenComparing(this::getNom, nomPrenomComparator)                          // à sexes identiques, par ordre alphabétique du nom de famille
					         .thenComparing(this::getPrenoms, nomPrenomComparator)                      // à sexes et noms de famille identiques, par ordre alphabétique des prénoms
					         .thenComparing(this::getDateNaissance, NullDateBehavior.LATEST::compare)   // à sexes, noms et prénoms identiques, par ordre chronologique des dates de naissance
					         .thenComparing(PersonnePhysique::getNumero))                               // à sexes, noms, prénoms et dates de naissance identiques, par ordre croissant du numéro de contribuable
			    .orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersonnePhysique getPrincipal(MenageCommun menageCommun) {
        final Set<PersonnePhysique> personnes = getPersonnesPhysiques(menageCommun);
        if (!personnes.isEmpty()) {
            final List<PersonnePhysique> liste = new ArrayList<>(personnes);
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

	@Override
	public List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique personne, int anneePeriode) {
		if (personne == null) {
			return null;
		}

		final Set<RapportEntreTiers> rapportsEntreTiers = personne.getRapportsSujet();
		if (rapportsEntreTiers == null) {
			return null;
		}

		final DateRangeHelper.Range periode = new DateRangeHelper.Range(RegDate.get(anneePeriode, 1, 1), RegDate.get(anneePeriode, 12, 31));

		List<EnsembleTiersCouple> listeEnsemble =null;
		for (RapportEntreTiers rapport : rapportsEntreTiers) {
			if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType()) {
				if (DateRangeHelper.intersect(rapport, periode)) {
					if (listeEnsemble == null) {
						// création à la demande
						listeEnsemble =  new ArrayList<>();
					}
					final MenageCommun menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
					listeEnsemble.add(getEnsembleTiersCouple(menageCommun,anneePeriode));
				}
			}
		}

		return listeEnsemble;
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
     * @return le rapport
     */
    @Override
    public RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable) {

        return addContactImpotSource(debiteur, contribuable, RegDate.get());
    }

    /**
     * Ajout d'un rapport de type contact impôt source entre le débiteur et le contribuable avec une date de début
     *
     * @return le rapport
     */
    @Override
    public RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable, RegDate dateDebut) {

        RapportEntreTiers rapport = new ContactImpotSource();
        rapport.setDateDebut(dateDebut);

        return addRapport(rapport, contribuable, debiteur);
    }

	/**
	 * Ajout d'un rapport de type activité économique entre l'établissement et le contribuable avec une date de début
	 *
	 * @return le rapport
	 */
	@Override
	public RapportEntreTiers addActiviteEconomique(Etablissement etablissement, Contribuable contribuable, RegDate dateDebut, boolean principal) {

		RapportEntreTiers rapport = new ActiviteEconomique(dateDebut, null, contribuable, etablissement, principal);

		return addRapport(rapport, contribuable, etablissement);
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
     *
     * @param sourcier     le sourcier sur lequel le debiteur doit être ajouté
     * @param debiteur     le debiteur à ajouter au sourcier
     * @param dateDebut    la date de début de validité de la relation entre tiers
     * @param dateFin      la date de fin de validité de la relation entre tiers (peut être nulle)
     * @return le rapport-prestation-imposable avec les références mises-à-jour des objets sauvés
     */
    @Override
    public RapportPrestationImposable addRapportPrestationImposable(PersonnePhysique sourcier, DebiteurPrestationImposable debiteur,
                                                                    RegDate dateDebut, RegDate dateFin) {
        RapportPrestationImposable rapport = new RapportPrestationImposable();
        rapport.setDateDebut(dateDebut);
        rapport.setDateFin(dateFin);
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
	 * @param tousPrenoms <code>true</code> si tous les prénoms du tiers doivent être utilisés, <code>false</code> si seul le prénom usuel doit être pris
	 * @return une pair composée du (ou des) prénom(s) (premier élément) et du nom (deuxième élément) de la personne physique ( ou {@link NomPrenom#VIDE} si la donnée est inconnue)
	 */
	@Override
	public NomPrenom getDecompositionNomPrenom(PersonnePhysique pp, boolean tousPrenoms) {
		if (pp.isHabitantVD()) {
			final Individu individu = getIndividu(pp);
			if (individu == null) {
				throw new IndividuNotFoundException(pp.getNumeroIndividu());
			}
			return serviceCivilService.getDecompositionNomPrenom(individu, tousPrenoms);
		}
		else {
			final String prenoms = tousPrenoms && StringUtils.isNotBlank(pp.getTousPrenoms()) ? pp.getTousPrenoms() : pp.getPrenomUsuel();
			return new NomPrenom(pp.getNom(), prenoms);
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
                individu = serviceCivilService.getIndividu(noIndividu, date, attributes);
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
     * @return le nouveau for fiscal principal
     */
    @Override
    public ForFiscalPrincipalPP openForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture,
                                                     MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
                                                     ModeImposition modeImposition, MotifFor motifOuverture) {

        Assert.isNull(contribuable.getForFiscalPrincipalAt(null), "Le contribuable possède déjà un for principal ouvert");

        // Ouvre un nouveau for à la date d'événement

	    ForFiscalPrincipalPP nouveauForFiscal = new ForFiscalPrincipalPP();
        nouveauForFiscal.setDateDebut(dateOuverture);
        nouveauForFiscal.setMotifRattachement(motifRattachement);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal.setModeImposition(modeImposition);
        nouveauForFiscal.setMotifOuverture(motifOuverture);
        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            afterForFiscalPrincipalAdded(nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);

	    return nouveauForFiscal;

    }

	/**
	 * Ouvre un nouveau for fiscal principal sur un contribuable soumis au régime des personnes morales
	 * <b>Note:</b> pour ajouter un for fiscal fermé voir la méthode {@link #addForPrincipal(ContribuableImpositionPersonnesMorales, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor,
	 * ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor, ch.vd.uniregctb.type.MotifRattachement, int, ch.vd.uniregctb.type.TypeAutoriteFiscale, GenreImpot)}
	 *
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param motifRattachement        le motif de rattachement du nouveau for
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	@Override
	public ForFiscalPrincipalPM openForFiscalPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
	                                                   TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture, GenreImpot genreImpot) {


		Assert.isNull(contribuable.getForFiscalPrincipalAt(null), "Le contribuable possède déjà un for principal ouvert");

		// Ouvre un nouveau for à la date d'événement

		ForFiscalPrincipalPM nouveauForFiscal = new ForFiscalPrincipalPM();
		nouveauForFiscal.setDateDebut(dateOuverture);
		nouveauForFiscal.setMotifRattachement(motifRattachement);
		nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
		nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
		nouveauForFiscal.setMotifOuverture(motifOuverture);
		nouveauForFiscal.setGenreImpot(genreImpot);
		nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

		if (validationService.validate(contribuable).errorsCount() == 0) {
			afterForFiscalPrincipalAdded(nouveauForFiscal);
		}

		Assert.notNull(nouveauForFiscal);

		return nouveauForFiscal;
	}

    /**
     * Ouvre et ferme un nouveau for fiscal principal sur un contribuable soumis au régime des personnes physiques.
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau for.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @param modeImposition           le mode d'imposition du for fiscal principal
     * @param motifOuverture           le motif d'ouverture
     * @param dateFermeture            la date de fermeture du for
     * @param motifFermeture           le motif de fermeture
     * @return le nouveau for fiscal principal
     */
    @Override
    public ForFiscalPrincipalPP openAndCloseForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture,
                                                               MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
                                                               ModeImposition modeImposition, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture) {

        // Ouvre un nouveau for à la date d'événement

	    ForFiscalPrincipalPP nouveauForFiscal = new ForFiscalPrincipalPP();
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
            afterForFiscalPrincipalAdded(nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        nouveauForFiscal = closeForFiscalPrincipal(contribuable, nouveauForFiscal, dateFermeture, motifFermeture);
        return nouveauForFiscal;
    }

    /**
     * Ouvre et ferme un nouveau for fiscal principal sur un contribuable soumis au régime des personnes morales.
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau for.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @param motifOuverture           le motif d'ouverture
     * @param dateFermeture            la date de fermeture du for
     * @param motifFermeture           le motif de fermeture
     * @param genreImpot               le genre d'impôt
     * @return le nouveau for fiscal principal
     */
    @Override
    public ForFiscalPrincipalPM openAndCloseForFiscalPrincipal(ContribuableImpositionPersonnesMorales contribuable, final RegDate dateOuverture,
                                                               MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
                                                               MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, GenreImpot genreImpot) {

        // Ouvre un nouveau for à la date d'événement

	    ForFiscalPrincipalPM nouveauForFiscal = new ForFiscalPrincipalPM();
        nouveauForFiscal.setDateDebut(dateOuverture);
        nouveauForFiscal.setMotifRattachement(motifRattachement);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal.setMotifOuverture(motifOuverture);
        nouveauForFiscal.setDateFin(dateFermeture);
        nouveauForFiscal.setMotifFermeture(motifFermeture);
	    nouveauForFiscal.setGenreImpot(genreImpot);
        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            afterForFiscalPrincipalAdded(nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        nouveauForFiscal = closeForFiscalPrincipal(contribuable, nouveauForFiscal, dateFermeture, motifFermeture);
        return nouveauForFiscal;
    }

	@Override
    public ForDebiteurPrestationImposable openAndCloseForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, MotifFor motifOuverture,
                                                                                     RegDate dateFermeture, MotifFor motifFermeture, int numeroOfsAutoriteFiscale,
                                                                                     TypeAutoriteFiscale typeAutoriteFiscale) {
        // Ouvre un nouveau for à la date d'événement
        ForDebiteurPrestationImposable nouveauForFiscal = new ForDebiteurPrestationImposable();
        nouveauForFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
        nouveauForFiscal.setDateDebut(dateOuverture);
		nouveauForFiscal.setMotifOuverture(motifOuverture);
        nouveauForFiscal.setDateFin(dateFermeture);
		nouveauForFiscal.setMotifFermeture(motifFermeture);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal = tiersDAO.addAndSave(debiteur, nouveauForFiscal);

        if (validationService.validate(debiteur).errorsCount() == 0) {
            afterForDebiteurPrestationImposableAdded(nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        nouveauForFiscal = closeForDebiteurPrestationImposable(debiteur, nouveauForFiscal, dateFermeture, motifFermeture, true);
        return nouveauForFiscal;
    }

	/**
	 * Détermine si une personne physique est domiciliée dans le canton de vaud, ou non (SIFISC-5970) (SIFISC-6841) maintenant.
	 *
	 * @param pp   une personne physique
	 * @param date date de référence (si <code>null</code>, on prend la date du jour)
	 * @param residencePrincipaleSeulement <code>true</code> si les seuls résidences qui comptes sont les résidences principales, <code>false</code> si les résidences secondaires comptent aussi
	 * @return <b>true</b> si l'adresse de domicile de la personne donnée à la date donnée est dans le canton; <b>false</b> si elle est hors-canton où hors-Suisse. Retourne <code>null</code> si on ne
	 *         sait pas répondre de manière définitive (pas d'adresse de domicile connue, erreurs...)
	 */
	protected Boolean isDomicileDansLeCanton(PersonnePhysique pp, RegDate date, boolean residencePrincipaleSeulement) {

		final Long numeroIndividu = pp.getNumeroIndividu();
		if (numeroIndividu == null) {
			// non-habitant => pas de domicile dans le canton par définition
			return Boolean.FALSE;
		}

		try {
			// [SIFISC-6841] On tient aussi compte des résidences secondaires
			// [SIFISC-13741] Mais on ne tient plus compte que des adresses actives (finies, les recherches de destinations !)
			// [SIFISC-11521] Les adresses secondaires ne sont pas toujours bonnes à prendre...
			final RegDate dateReference = date == null ? RegDate.get() : date;
			final AdressesCivilesActives adresses = serviceCivilService.getAdresses(numeroIndividu, dateReference, false);
			return isAdresseVaudoise(adresses.principale) || (!residencePrincipaleSeulement && hasAdresseVaudoise(adresses.secondaires));
		}
		catch (ServiceInfrastructureException e) {
			// rien à faire...
			LOGGER.warn("Impossible de déterminer la commune de l'adresse de domicile du tiers " + pp.getNumero(), e);
		}
		catch (DonneesCivilesException e) {
			// rien à faire...
			LOGGER.warn("Impossible de déterminer les adresses principales du tiers " + pp.getNumero(), e);
		}

		return null;
	}

	@NotNull
	public List<DateRange> getPeriodesDeResidence(PersonnePhysique pp, boolean residencePrincipaleSeulement) throws DonneesCivilesException {
		final Long numeroIndividu = pp.getNumeroIndividu();
		if (numeroIndividu == null) {
			// inconnu au civil -> pas de période de résidence possible
			return Collections.emptyList();
		}

		// je demande déjà ici la part ADRESSES car un nouvel appel au service civil est fait juste après avec cette part aussi...
		// si on peut garantir d'utiliser le cache local, tant mieux, non ?
		final Individu individu = serviceCivilService.getIndividu(numeroIndividu, null, AttributeIndividu.ADRESSES);
		if (individu == null) {
			throw new IndividuNotFoundException(pp);
		}

		final RegDate dateDeces = individu.getDateDeces();      // les décédés civils ne sont plus habitants...
		final DateRange limitationRangeDeces = new DateRangeHelper.Range(null, dateDeces);

		final AdressesCivilesHistoriques adresses = serviceCivilService.getAdressesHisto(numeroIndividu, false);
		final List<DateRange> presencesVaudoises = Stream.of(adresses.principales,
		                                                     residencePrincipaleSeulement ? null : adresses.secondaires)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.filter(this::isAdresseVaudoise)
				.map(adresse -> DateRangeHelper.intersection(adresse, limitationRangeDeces))
				.filter(Objects::nonNull)
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());

		final List<DateRange> periodes = DateRangeHelper.merge(presencesVaudoises);
		return periodes != null ? periodes : Collections.emptyList();
	}

	private Commune getCommuneForAdresse(Adresse adresse, boolean faitiereOnly) {
		final RegDate refDate = RegDateHelper.minimum(adresse.getDateDebut(), adresse.getDateFin(), NullDateBehavior.LATEST);
		final Commune directe = serviceInfra.getCommuneByAdresse(adresse, refDate);
		if (faitiereOnly) {
			return serviceInfra.getCommuneFaitiere(directe, refDate);
		}
		else {
			return directe;
		}
	}

	/**
	 * @param adresse adresse à tester
	 * @return <code>true</code> si l'adresse est non-nulle et est associée à une commune vaudoise, <code>false</code> dans tous les autres cas
	 */
	private boolean isAdresseVaudoise(Adresse adresse) {
		if (adresse == null) {
			return false;
		}

		final Commune commune = getCommuneForAdresse(adresse, false);
		return commune != null && commune.isVaudoise();
	}

	/**
	 * @param adresses collection d'adresses dans laquelle on cherche au moins une adresse vaudoise
	 * @return <code>true</code> si au moins l'une des adresses de la collection est vaudoise au sens de {@link #isAdresseVaudoise(Adresse)}, <code>false</code> sinon
	 */
	private boolean hasAdresseVaudoise(Collection<Adresse> adresses) {
		if (adresses != null && !adresses.isEmpty()) {
			for (Adresse adresse : adresses) {
				if (isAdresseVaudoise(adresse)) {
					return true;
				}
			}
		}
		return false;
	}

	private List<PersonnePhysique> getEnfants(MenageCommun mc, RegDate dateValidite) {
        final EnsembleTiersCouple ensembleTiersCouple = getEnsembleTiersCouple(mc, dateValidite);
        final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
        final Set<PersonnePhysique> setEnfantsMenage = new HashSet<>();
        final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
        setEnfantsMenage.addAll(getEnfants(principal, dateValidite));
        if (conjoint != null) {
            setEnfantsMenage.addAll(getEnfants(conjoint, dateValidite));
        }
        return new ArrayList<>(setEnfantsMenage);
    }

    private List<PersonnePhysique> getEnfants(Contribuable ctb, RegDate dateValidite) {
        if (ctb instanceof MenageCommun) {
            return getEnfants((MenageCommun) ctb, dateValidite);
        }
        else if (ctb instanceof PersonnePhysique) {
            return getEnfants((PersonnePhysique) ctb, dateValidite);
        }
        else {
            return Collections.emptyList();
        }
    }

	private List<PersonnePhysique> getEnfants(PersonnePhysique pp, RegDate dateValidite) {
		final List<Parente> parentes = extractParentes(pp.getRapportsObjet(), false);
		final List<PersonnePhysique> enfants = new ArrayList<>(parentes.size());
		for (Parente parente : parentes) {
			if (parente.isValidAt(dateValidite)) {
				final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(parente.getSujetId());
				enfants.add(enfant);
			}
		}
		return enfants;
	}

    @Override
    public List<PersonnePhysique> getEnfantsForDeclaration(Contribuable ctb, RegDate finPeriodeImposition) {
        final List<PersonnePhysique> listeEnfants = new ArrayList<>();
        final List<PersonnePhysique> listeRecherche = getEnfants(ctb, finPeriodeImposition);
        if (!listeRecherche.isEmpty()) {

            // warm-up du cache individu avec adresses
            final List<Long> noTiersEnfants = new ArrayList<>(listeRecherche.size());
            for (PersonnePhysique enfant : listeRecherche) {
                noTiersEnfants.add(enfant.getNumero());
            }
            serviceCivilCacheWarmer.warmIndividusPourTiers(noTiersEnfants, finPeriodeImposition, true, AttributeIndividu.ADRESSES);

            for (PersonnePhysique enfant : listeRecherche) {
                final RegDate dateDeces = getDateDeces(enfant);
                // enfant non Décédé
                if (dateDeces == null || dateDeces.isAfter(finPeriodeImposition)) {
                    // Enfant mineur à la date de fin de la periode d'imposition
                    if (isMineur(enfant, finPeriodeImposition)) {
                        // L'adresse de domicile (EGID/EWID) doit être la même entre l'enfant et le contribuable
                        if (areEgidEwidCoherents(ctb, enfant, finPeriodeImposition)) {
                            listeEnfants.add(enfant);
                        }
                    }
                }
            }
        }

        //[SIFISC-2703] tri des enfants par date de naissance croissante
        Collections.sort(listeEnfants, (pp1, pp2) -> NullDateBehavior.EARLIEST.compare(getDateNaissance(pp1), getDateNaissance(pp2)));
        return listeEnfants;
    }

    /**
     * Le couple EGID/EWID de l’adresse domicile doit être le même entre l’enfant et le CTB à la fin de la période d’imposition. Dans le cadre d’un ménage commun, il faut que chacun des deux parents ait un EGID/EWID
     * identique à celui de l’enfant. Si deux membres d’un ménage commun habitent à des adresses vaudoises (EGID/EWID) différentes, l’enfant ne doit pas figurer sur la DI. Si les deux parents ne sont pas en
     * ménage commun alors qu’ils ont le même EGID/EWID, l’enfant ne doit figurer sur aucune des deux DI.
     *
     * @param parent               le contribuable  <i>PersonnePhysique</i> ou <i>MenageCommun</i>
     * @param enfant               qui est succeptible d'être rajouter sur la DI
     * @param finPeriodeImposition date de fin de période d'imposition
     * @return <i>TRUE</i> si les egid sont cohérents, <i>FALSE</i>  des incoherences sont trouvées entre l'egid de l'enfant et des parents
     */
    private boolean areEgidEwidCoherents(Contribuable parent, PersonnePhysique enfant, RegDate finPeriodeImposition) {
        try {
            final AdresseGenerique adresseDomicileParent = adresseService.getAdresseFiscale(parent, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);
            final AdresseGenerique adresseDomicileEnfant = adresseService.getAdresseFiscale(enfant, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);
            //Les EGID/EWID ne peuvent être déterminées, on retourne false
            if (adresseDomicileEnfant == null || adresseDomicileParent == null) {
                return false;
            }

            if (parent instanceof PersonnePhysique) {
                //Si les deux parents ne sont pas en ménage commun alors qu’ils ont le même EGID, l’enfant ne doit figurer sur aucune des deux DI.
                final boolean hasParentsAvecEgidDifferents = TiersHelper.hasParentsAvecEgidEwidDifferents(enfant, (PersonnePhysique) parent, adresseDomicileParent, finPeriodeImposition, adresseService, this);
                if (hasParentsAvecEgidDifferents) {
                    //On compare l'egid/ewid du parent et de l'enfant
                    return TiersHelper.isSameEgidEwid(adresseDomicileEnfant, adresseDomicileParent);
                }
            }
            else if (parent instanceof MenageCommun) {
                final EnsembleTiersCouple ensembleTiersCouple = getEnsembleTiersCouple((MenageCommun) parent, finPeriodeImposition);
                final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
                final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
                if (conjoint != null) {
                    final AdresseGenerique adresseDomicilePrincipal = adresseService.getAdresseFiscale(principal, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);
                    final AdresseGenerique adresseDomicileConjoint = adresseService.getAdresseFiscale(conjoint, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);

                    //Dans le cadre d’un ménage commun, il faut que chacun des deux parents ait un EGID/EWID identique à celui de l’enfant.
	                return TiersHelper.isSameEgidEwid(adresseDomicilePrincipal, adresseDomicileConjoint) && TiersHelper.isSameEgidEwid(adresseDomicileEnfant, adresseDomicileParent);
                }
                else {
                    //Marié(e) seul(e)
                    return TiersHelper.isSameEgidEwid(adresseDomicileEnfant, adresseDomicileParent);
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
	public List<Parente> getParents(PersonnePhysique enfant, boolean yComprisRelationsAnnulees) {
		return extractParentes(enfant.getRapportsSujet(), yComprisRelationsAnnulees);
	}

	@NotNull
	@Override
	public List<PersonnePhysique> getParents(PersonnePhysique enfant, RegDate dateValidite) {
		final List<Parente> parentes = getParents(enfant, false);
		final List<PersonnePhysique> parents = new ArrayList<>(parentes.size());
		for (Parente parente : parentes) {
			if (parente.isValidAt(dateValidite)) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(parente.getObjetId());
				parents.add(pp);
			}
		}
		return parents;
	}

	@NotNull
	@Override
	public List<Parente> getEnfants(PersonnePhysique parent, boolean yComprisRelationsAnnulees) {
		return extractParentes(parent.getRapportsObjet(), yComprisRelationsAnnulees);
	}

	private static List<Parente> extractParentes(Collection<RapportEntreTiers> rapports, boolean yComprisRelationsAnnulees) {
		final List<Parente> parentes = new LinkedList<>();
		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (rapport instanceof Parente) {
					if (yComprisRelationsAnnulees || !rapport.isAnnule()) {
						parentes.add((Parente) rapport);
					}
				}
			}
		}
		return parentes;
	}

	@Override
	public ParenteUpdateResult refreshParentesDepuisNumeroIndividu(long noIndividu) {
		final PersonnePhysique pp = getPersonnePhysiqueByNumeroIndividu(noIndividu);
		final ParenteUpdateResult result;
		if (pp != null) {
			result = refreshParentesSurPersonnePhysique(pp, true);
		}
		else {
			result = ParenteUpdateResult.EMPTY;
		}
		return result;
	}

	@Override
	public void markParentesDirtyDepuisNumeroIndividu(long noIndividu) {
		final PersonnePhysique pp = getPersonnePhysiqueByNumeroIndividu(noIndividu);
		if (pp != null) {
			markParentesDirtySurPersonnePhysique(pp, true);
		}
	}

	private void markParentesDirtySurPersonnePhysique(PersonnePhysique pp, boolean enfantsAussi) {
		setParenteDirtyFlag(pp, true);
		if (enfantsAussi) {
			final List<Parente> enfants = getEnfants(pp, false);
			for (Parente enfant : enfants) {
				setParenteDirtyFlag(enfant.getSujetId(), true);
			}
		}
	}

	@Override
	public ParenteUpdateResult initParentesDepuisFiliationsCiviles(PersonnePhysique pp) {
		if (pp.isHabitantVD()) {
			final ParenteUpdateResult result = new ParenteUpdateResult();
			final long noIndividu = pp.getNumeroIndividu();
			final Individu individu = serviceCivilService.getIndividu(noIndividu, null, AttributeIndividu.PARENTS);
			boolean parenteDirty = false;
			if (individu == null) {
				final String msg = String.format("Individu %d lié à l'habitant %d non-récupérable depuis le registre civil", noIndividu, pp.getNumero());
				LOGGER.error(msg);
				result.addError(pp.getNumero(), msg);
				parenteDirty = true;
			}
			else {
				final List<RelationVersIndividu> parentRel = individu.getParents();
				if (parentRel != null && !parentRel.isEmpty()) {
					for (RelationVersIndividu rel : parentRel) {
						try {
							final Parente parente = createParente(pp, rel);
							if (parente != null) {
								final Parente merged = hibernateTemplate.merge(parente);
								pp.getRapportsSujet().add(merged);
								result.addUpdate(ParenteUpdateInfo.getCreation(parente));
							}
						}
						catch (CreationParenteImpossibleCarTiersParentInconnuAuFiscal | PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
							LOGGER.warn(e.getMessage(), e);
							result.addError(pp.getNumero(), e.getMessage());
							parenteDirty = true;
						}
					}
				}
			}
			if (parenteDirty != pp.isParenteDirty()) {
				setParenteDirtyFlag(pp, parenteDirty);
			}
			return result.isEmpty() ? ParenteUpdateResult.EMPTY : result;
		}
		else {
			return ParenteUpdateResult.EMPTY;
		}
	}

	private void setParenteDirtyFlag(final long ppId, final boolean flag) {
		final String sql = "UPDATE TIERS SET PP_PARENTE_DIRTY=:flag WHERE NUMERO=:id";
		final int nbChanged = hibernateTemplate.execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(sql);
				query.setBoolean("flag", flag);
				query.setLong("id", ppId);
				return query.executeUpdate();
			}
		});
		if (LOGGER.isDebugEnabled() && nbChanged > 0) {
			LOGGER.debug(String.format("Flag 'parenté dirty' passé à %s sur la personne physique %d", flag, ppId));
		}
	}

	private void setParenteDirtyFlag(PersonnePhysique pp, boolean flag) {
		setParenteDirtyFlag(pp.getNumero(), flag);
	}

	private static class CreationParenteImpossibleCarTiersParentInconnuAuFiscal extends Exception {
		public final PersonnePhysique enfant;
		public final long noIndividuParent;

		public CreationParenteImpossibleCarTiersParentInconnuAuFiscal(PersonnePhysique enfant, long noIndividuParent) {
			this.enfant = enfant;
			this.noIndividuParent = noIndividuParent;
		}

		@Override
		public String getMessage() {
			return String.format("Impossible de créer une parenté depuis l'enfant %s vers son parent car aucun tiers n'existe dans le registre avec le numéro d'individu %d.",
			                     enfant.getNumero(), noIndividuParent);
		}
	}

	/**
	 * Création d'un objet "parenté" ascendante depuis l'enfant donné vers le contribuable derrière l'individu donné par la filiation
	 * @param enfant enfant à qui on rajoute une parenté ascendante
	 * @param filiation la filiation civile dont on doit s'inspirer
	 * @return la parenté qui va bien (nouvel objet non inscrit dans aucune session hibernate)
	 * @throws CreationParenteImpossibleCarTiersParentInconnuAuFiscal si la filiation nous donne un numéro d'individu pour lequel il n'existe aucun tiers dans le registre
	 * @throws PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException si la filiation nous donne un numéro d'individu pour lequel il y a plusieurs tiers acitfs dans le registre
	 */
	private Parente createParente(PersonnePhysique enfant, RelationVersIndividu filiation) throws CreationParenteImpossibleCarTiersParentInconnuAuFiscal {
		final RegDate dateNaissanceEnfant = findCompleteDate(getDateNaissance(enfant));
		final RegDate dateDecesEnfant = getDateDeces(enfant);

		final long noIndividuParent = filiation.getNumeroAutreIndividu();
		final PersonnePhysique parent = tiersDAO.getPPByNumeroIndividu(noIndividuParent, true);
		if (parent != null) {
			final RegDate dateDecesParent = getDateDeces(parent);
			final RegDate dateNaissanceParent = findCompleteDate(getDateNaissance(parent));

			final RegDate dateDebut = maximum(NullDateBehavior.EARLIEST, findCompleteDate(filiation.getDateDebut()), dateNaissanceEnfant, dateNaissanceParent);
			final RegDate dateFin = minimum(NullDateBehavior.LATEST, filiation.getDateFin(), dateDecesEnfant, dateDecesParent);

			if (dateDebut == null || dateFin == null || dateDebut.isBeforeOrEqual(dateFin)) {
				return new Parente(dateDebut, dateFin, parent, enfant);
			}
			return null;
		}
		else {
			throw new CreationParenteImpossibleCarTiersParentInconnuAuFiscal(enfant, noIndividuParent);
		}
	}

	@Nullable
	private static RegDate findCompleteDate(@Nullable RegDate date) {
		return date != null ? FiscalDateHelper.getDateComplete(date) : null;
	}

	@Nullable
	private static RegDate minimum(NullDateBehavior nullDateBehavior, RegDate date1, RegDate date2, RegDate date3) {
		final RegDate min = RegDateHelper.minimum(date1, date2, nullDateBehavior);
		return RegDateHelper.minimum(min, date3, nullDateBehavior);
	}

	@Nullable
	private static RegDate maximum(NullDateBehavior nullDateBehavior, RegDate date1, RegDate date2, RegDate date3) {
		final RegDate max = RegDateHelper.maximum(date1, date2, nullDateBehavior);
		return RegDateHelper.maximum(max, date3, nullDateBehavior);
	}

	/**
	 * @return <code>true</code> si les deux parentés ont des données métier équivalentes (dates + parent + enfant)
	 */
	private static boolean areEqualBusinesswise(Parente p1, Parente p2) {
		if (p1 == p2) {
			return true;
		}
		else if (p1 == null || p2 == null) {
			return false;
		}
		else {
			return DateRangeHelper.equals(p1, p2) && Objects.equals(p1.getObjetId(), p2.getObjetId()) && Objects.equals(p1.getSujetId(), p2.getSujetId());
		}
	}

	@Override
	public Set<Long> getNumerosIndividusLiesParParente(long noIndividuSource) {
		return tiersDAO.getNumerosIndividusLiesParParente(noIndividuSource);
	}

	@Override
	public ParenteUpdateResult refreshParentesSurPersonnePhysique(PersonnePhysique pp, boolean enfantsAussi) {
		if (pp != null && pp.isHabitantVD()) {

			// 1. on retrouve les parents depuis les données civiles
			final long noIndividu = pp.getNumeroIndividu();
			final ParenteUpdateResult result = new ParenteUpdateResult();
			boolean parenteDirty = false;

			final Individu individu = serviceCivilService.getIndividu(noIndividu, null, AttributeIndividu.PARENTS);
			if (individu == null) {
				final String msg = String.format("Individu %d lié à l'habitant %d non-récupérable depuis le registre civil", noIndividu, pp.getNumero());
				LOGGER.error(msg);
				result.addError(pp.getNumero(), msg);
				parenteDirty = true;
			}
			else {
				final List<RelationVersIndividu> parents = individu.getParents();

				// 1.1. on récupère les données "en tiers"
				final Set<RapportEntreTiers> sujetsConnus = pp.getRapportsSujet();
				final Map<Long, Parente> filiationsCiviles;         // parenté indexée par le numéro de tiers du parent
				if (parents != null && !parents.isEmpty()) {
					filiationsCiviles = new HashMap<>(parents.size());
					for (RelationVersIndividu filiation : parents) {
						try {
							final Parente parente = createParente(pp, filiation);
							if (parente != null) {
								filiationsCiviles.put(parente.getObjetId(), parente);
							}
						}
						catch (CreationParenteImpossibleCarTiersParentInconnuAuFiscal | PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
							LOGGER.warn(e.getMessage(), e);
							result.addError(pp.getNumero(), e.getMessage());
							parenteDirty = true;
						}
					}
				}
				else {
					filiationsCiviles = Collections.emptyMap();
				}

				// 1.2. on passe d'abord en revue les parentés (-> parents) connues pour voir celles qui doivent disparaître
				final Set<RapportEntreTiers> sujets;
				if (sujetsConnus != null) {
					sujets = sujetsConnus;
					for (RapportEntreTiers sujet : sujetsConnus) {
						if (!sujet.isAnnule() && sujet.getType() == TypeRapportEntreTiers.PARENTE) {
							// y a-t-il une relation civile avec le même parent ?
							final Parente filiation = filiationsCiviles.get(sujet.getObjetId());
							if (filiation != null && areEqualBusinesswise(filiation, (Parente) sujet)) {
								// tout correspond, on l'enlève de la liste de ceux qu'il faudra ensuite ajouter...
								filiationsCiviles.remove(sujet.getObjetId());
							}
							else {
								// non, pas de relation avec cet individu ou la relation ne correspond pas (dates ?)
								// -> il faut annuler l'ancienne
								sujet.setAnnule(true);
								result.addUpdate(ParenteUpdateInfo.getAnnulation((Parente) sujet));
							}
						}
					}
				}
				else {
					sujets = new HashSet<>();
					pp.setRapportsSujet(sujets);
				}

				// 1.3. puis on ajoute les filiations civiles qui n'ont pas été reconnues (et que l'on peut maintenant persister)
				for (Parente civile : filiationsCiviles.values()) {
					final Parente persistent = hibernateTemplate.merge(civile);
					sujets.add(persistent);
					result.addUpdate(ParenteUpdateInfo.getCreation(persistent));
				}
			}

			if (parenteDirty != pp.isParenteDirty()) {
				setParenteDirtyFlag(pp, parenteDirty);
			}

			// 2. si nécessaire, on fait pareil sur les enfants connus
			if (enfantsAussi) {
				final List<Parente> enfants = getEnfants(pp, false);
				for (Parente versEnfant : enfants) {
					final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(versEnfant.getSujetId());
					result.addAll(refreshParentesSurPersonnePhysique(enfant, false));
				}
			}

			return result.isEmpty() ? ParenteUpdateResult.EMPTY : result;
		}
		else {
			return ParenteUpdateResult.EMPTY;
		}
	}

	private void afterForFiscalPrincipalAdded(ForFiscalPrincipalPM forFiscalPrincipal) {
	    this.evenementFiscalService.publierEvenementFiscalOuvertureFor(forFiscalPrincipal);
	    resetFlagBlocageRemboursementAutomatiqueSelonFors(forFiscalPrincipal.getTiers());
	}

	private void afterForFiscalPrincipalAdded(ForFiscalPrincipalPP forFiscalPrincipal) {
		final ContribuableImpositionPersonnesPhysiques contribuable = forFiscalPrincipal.getTiers();
		final MotifFor motifOuverture = forFiscalPrincipal.getMotifOuverture();
        final TypeAutoriteFiscale typeAutoriteFiscale = forFiscalPrincipal.getTypeAutoriteFiscale();
        final RegDate dateOuverture = forFiscalPrincipal.getDateDebut();

        if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
            if (motifOuverture == MotifFor.CHGT_MODE_IMPOSITION || motifOuverture == MotifFor.PERMIS_C_SUISSE) {
                this.evenementFiscalService.publierEvenementFiscalChangementModeImposition(forFiscalPrincipal);
            }
            else  {
                this.evenementFiscalService.publierEvenementFiscalOuvertureFor(forFiscalPrincipal);
            }

            if (motifOuverture == MotifFor.MAJORITE && contribuable instanceof PersonnePhysique) {
                final PersonnePhysique enfant = (PersonnePhysique) contribuable;
                // [UNIREG-3244] Lorsqu'un premier for principal pour le revenu ou la fortune est ouvert pour un contribuable
                // pour motif de « Majorité », un événement fiscal de type « Fin d'autorité parentale » est engendré
                if (enfant.getForsFiscauxNonAnnules(false).size() == 1) {
                    final ContribuableImpositionPersonnesPhysiques parent = getAutoriteParentaleDe(enfant, dateOuverture.getOneDayBefore());
                    if (parent != null) {
                        evenementFiscalService.publierEvenementFiscalFinAutoriteParentale(enfant, parent, dateOuverture);
                    }
                }
            }
        }
		else {
	        this.evenementFiscalService.publierEvenementFiscalOuvertureFor(forFiscalPrincipal);
        }

        // [UNIREG-1373] Un départ HS ajuste la date de fin d'une eventuelle DI libre.
        if (TypeAutoriteFiscale.PAYS_HS == typeAutoriteFiscale && MotifFor.DEPART_HS == motifOuverture) {
            if (forFiscalPrincipal.getDateDebut().year() == RegDate.get().year()) {
                // Le for ouvert est dans la période courante, on verifie que le contribuable n'ait pas une DI libre
                final List<DeclarationImpotOrdinairePP> dis = contribuable.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, RegDate.get().year(), false);
                if (dis != null && !dis.isEmpty()) {
                    Collections.sort(dis, new DateRangeComparator<>());
                    final DeclarationImpotOrdinairePP di = dis.get(dis.size() - 1);
                    // Le contribuable a une DI libre, on ajuste la periode d'imposition
                    di.setDateFin(forFiscalPrincipal.getDateDebut());
                }
            }
        }

        final ModeImposition ancienModeImposition;
        if (motifOuverture == MotifFor.CHGT_MODE_IMPOSITION) {
            final ForFiscalPrincipalPP ancienFfp = contribuable.getForFiscalPrincipalAt(forFiscalPrincipal.getDateDebut().getOneDayBefore());
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

	private PersonnePhysique getMere(PersonnePhysique pp, RegDate dateValidite) {
		final List<PersonnePhysique> parents = getParents(pp, dateValidite);
		for (PersonnePhysique parent : parents) {
			if (getSexe(parent) == Sexe.FEMININ) {
				return parent;
			}
		}
		return null;
	}

	@Override
    public ContribuableImpositionPersonnesPhysiques getAutoriteParentaleDe(PersonnePhysique contribuableEnfant, RegDate dateValidite) {

		final PersonnePhysique parent = getMere(contribuableEnfant, dateValidite);
        if (parent == null) {
            return null;
        }

        final ContribuableImpositionPersonnesPhysiques autoriteParentale;
        final EnsembleTiersCouple ensemble = getEnsembleTiersCouple(parent, dateValidite);
        if (ensemble == null) {
            autoriteParentale = parent;
        }
        else {
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
                                                       MotifFor motifOuverture, GenreImpot genreImpot) {
        return addForSecondaire(contribuable, dateOuverture, null, motifRattachement, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifOuverture, null, genreImpot);
    }

	private void afterForFiscalSecondaireAdded(Contribuable contribuable, ForFiscalSecondaire forFiscalSecondaire) {
	    evenementFiscalService.publierEvenementFiscalOuvertureFor(forFiscalSecondaire);
        tacheService.genereTacheDepuisOuvertureForSecondaire(contribuable, forFiscalSecondaire);
		resetFlagBlocageRemboursementAutomatiqueSelonFors(contribuable);
    }

	@Override
	public ForFiscalAutreElementImposable openForFiscalAutreElementImposable(Contribuable contribuable, GenreImpot genreImpot, final RegDate dateOuverture, MotifRattachement motifRattachement,
	                                                                         int numeroOfsAutoriteFiscale, MotifFor motifOuverture) {
		return openForFiscalAutreElementImposable(contribuable, dateOuverture, motifOuverture, null, null, motifRattachement, numeroOfsAutoriteFiscale);
	}

	@Override
	public ForFiscalAutreElementImposable openForFiscalAutreElementImposable(Contribuable contribuable, RegDate dateOuverture, MotifFor motifOuverture, @Nullable RegDate dateFermeture,
	                                                                         @Nullable MotifFor motifFermeture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale) {
		// Ouvre un nouveau for à la date d'événement
		ForFiscalAutreElementImposable nouveauForFiscal = new ForFiscalAutreElementImposable();
		nouveauForFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		nouveauForFiscal.setDateDebut(dateOuverture);
		nouveauForFiscal.setDateFin(dateFermeture);
		nouveauForFiscal.setMotifOuverture(motifOuverture);
		nouveauForFiscal.setMotifFermeture(motifFermeture);
		nouveauForFiscal.setMotifRattachement(motifRattachement);
		nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
		nouveauForFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

		Assert.notNull(nouveauForFiscal);
		return nouveauForFiscal;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalAutreImpot openForFiscalAutreImpot(Contribuable contribuable, GenreImpot genreImpot, final RegDate dateImpot, int numeroOfsAutoriteFiscale) {

        // Ouvre un nouveau for à la date d'événement
        ForFiscalAutreImpot nouveauForFiscal = new ForFiscalAutreImpot();
	    nouveauForFiscal.setGenreImpot(genreImpot);
	    nouveauForFiscal.setDateDebut(dateImpot);
	    nouveauForFiscal.setDateFin(dateImpot);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
        nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            afterForAutreImportAdded(nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        return nouveauForFiscal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForDebiteurPrestationImposable openForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, MotifFor motifOuverture, int numeroOfsAutoriteFiscale,
                                                                             TypeAutoriteFiscale typeAutoriteFiscale) {

        // Ouvre un nouveau for à la date d'événement
        ForDebiteurPrestationImposable nouveauForFiscal = new ForDebiteurPrestationImposable();
        nouveauForFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
        nouveauForFiscal.setDateDebut(dateOuverture);
	    nouveauForFiscal.setMotifOuverture(motifOuverture);
        nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
        nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
        nouveauForFiscal = tiersDAO.addAndSave(debiteur, nouveauForFiscal);

        if (validationService.validate(debiteur).errorsCount() == 0) {
	        afterForDebiteurPrestationImposableAdded(nouveauForFiscal);
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
        final List<RapportEntreTiers> nouveaux = new ArrayList<>(debiteur.getRapportsObjet().size());
        for (RapportEntreTiers rapport : debiteur.getRapportsObjet()) {
            if (rapport instanceof RapportPrestationImposable && rapport.isValidAt(dateDesactivation) && dateDesactivation.equals(rapport.getDateFin())) {
                final Tiers sourcier = getTiers(rapport.getSujetId());
                final RapportPrestationImposable rpi = new RapportPrestationImposable(dateReactivation, null, (Contribuable) sourcier, debiteur);
                nouveaux.add(hibernateTemplate.merge(rpi));
            }
        }
	    debiteur.getRapportsObjet().addAll(nouveaux);
    }

	@Override
	public void reopenRapportsEntreTiers(Tiers tiers, RegDate dateFermeture, Set<TypeRapportEntreTiers> typesRapportsSujet, Set<TypeRapportEntreTiers> typesRapportsObjet) {
		reopenRapportsEntreTiers(dateFermeture, typesRapportsSujet, tiers.getRapportsSujet());
		reopenRapportsEntreTiers(dateFermeture, typesRapportsObjet, tiers.getRapportsObjet());
	}

	private void reopenRapportsEntreTiers(RegDate dateFermeture, Set<TypeRapportEntreTiers> typesRapports, Set<RapportEntreTiers> rapports) {

		// constitution des données
		final List<RapportEntreTiers> aOuvrir = new ArrayList<>(rapports.size());
		final List<RapportEntreTiers> aAnnuler = new ArrayList<>(rapports.size());
		for (RapportEntreTiers rapport : rapports) {
			if (!rapport.isAnnule() && rapport.getDateFin() == dateFermeture && typesRapports.contains(rapport.getType())) {
				final RapportEntreTiers copie = rapport.duplicate();
				copie.setDateFin(null);

				aOuvrir.add(copie);
				aAnnuler.add(rapport);
			}
		}

		// ajout des nouveaux rapports
		for (RapportEntreTiers rapport : aOuvrir) {
			final Tiers sujet = tiersDAO.get(rapport.getSujetId());
			final Tiers objet = tiersDAO.get(rapport.getObjetId());
			addRapport(rapport, sujet, objet);
		}

		// annulation des rapports maintenant remplacés par les autres
		for (RapportEntreTiers rapport : aAnnuler) {
			rapport.setAnnule(true);
		}
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

    private void afterForDebiteurPrestationImposableAdded(ForDebiteurPrestationImposable forDebiteur) {
        this.evenementFiscalService.publierEvenementFiscalOuvertureFor(forDebiteur);
    }

    private void afterForAutreImportAdded(ForFiscalAutreImpot forFiscal) {
        this.evenementFiscalService.publierEvenementFiscalOuvertureFor(forFiscal);
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
	    final Set<ForFiscal> allFors = tiers.getForsFiscaux();
	    final List<ForFiscal> openFors = new ArrayList<>(allFors.size());
	    for (ForFiscal forFiscal : allFors) {
            if (!forFiscal.isAnnule() && date.equals(forFiscal.getDateFin())) {
                /*
				 *  La bonne méthode consiste à recréer un nouveau For en reprenant les données de l'ancien,
				 *  annuler ce dernier, mettre la date de fin du nouveau For à nul et l'assigner au tiers.
				 */
                final boolean isForFiscalAvecMotif = forFiscal instanceof ForFiscalAvecMotifs;
                final boolean isForFiscalAutreImpot = forFiscal instanceof ForFiscalAutreImpot;

                if (isForFiscalAutreImpot || (isForFiscalAvecMotif && motifFermeture == ((ForFiscalAvecMotifs) forFiscal).getMotifFermeture())) {
                    // duplication du for à annuler
                    final ForFiscal nouveauForFiscal = forFiscal.duplicate();
	                forFiscal.setAnnule(true);
	                evenementFiscalService.publierEvenementFiscalAnnulationFor(forFiscal);

	                // effacement des information de fermeture sur la copie
                    nouveauForFiscal.setDateFin(null);
                    if (isForFiscalAvecMotif) {
                        ((ForFiscalAvecMotifs) nouveauForFiscal).setMotifFermeture(null);
                    }
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
     * @param forsPrincipaux si <code>true</code>, ne s'occupe que des fors principaux, et si <code>false</code>, que des autres
     */
    private void addAndSaveReopenedFors(Tiers tiers, List<ForFiscal> reopenedFors, boolean forsPrincipaux) {
        for (ForFiscal reopenedFor : reopenedFors) {
	        if (forsPrincipaux == reopenedFor.isPrincipal()) {
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
     * Le for fiscal doit avoir été annulé précédemment.
     *
     * @param ff    un for fiscal déjà annulé
     * @param tiers un tiers à qui doit être assigné le nouveau for.
     */
    @Override
    public void reopenFor(ForFiscal ff, Tiers tiers) {
        ForFiscal nouveauForFiscal = ff.duplicate();
        nouveauForFiscal.setAnnule(false);
        nouveauForFiscal.setDateFin(null);
	    if (nouveauForFiscal instanceof ForFiscalAvecMotifs) {
		    ((ForFiscalAvecMotifs) nouveauForFiscal).setMotifFermeture(null);
	    }
        nouveauForFiscal = tiersDAO.addAndSave(tiers, nouveauForFiscal);
        // exécution des règles événements fiscaux
        if (validationService.validate(tiers).errorsCount() == 0) {
            afterForAdded(nouveauForFiscal);
        }
    }

	private <T extends ForFiscal> T reopenForFiscal(T ff) {
		//noinspection unchecked
		final T reouvert = (T) ff.duplicate();
		ff.setAnnule(true);
		reouvert.setAnnule(false);
		reouvert.setDateFin(null);
		if (reouvert instanceof ForFiscalAvecMotifs) {
			((ForFiscalAvecMotifs) reouvert).setMotifFermeture(null);
		}
		final T saved = tiersDAO.addAndSave(ff.getTiers(), reouvert);
		if (validationService.validate(ff.getTiers()).errorsCount() == 0) {
			afterForAdded(saved);
		}
		return saved;
	}

    private void afterForAdded(ForFiscal forFiscal) {
        if (forFiscal instanceof ForFiscalPrincipalPP) {
	        afterForFiscalPrincipalAdded((ForFiscalPrincipalPP) forFiscal);
        }
        else if (forFiscal instanceof ForFiscalPrincipalPM) {
	        afterForFiscalPrincipalAdded((ForFiscalPrincipalPM) forFiscal);
        }
        else if (forFiscal instanceof ForFiscalSecondaire) {
            afterForFiscalSecondaireAdded((Contribuable) forFiscal.getTiers(), (ForFiscalSecondaire) forFiscal);
        }
        else if (forFiscal instanceof ForDebiteurPrestationImposable) {
            afterForDebiteurPrestationImposableAdded((ForDebiteurPrestationImposable) forFiscal);
        }
        else if (forFiscal instanceof ForFiscalAutreImpot) {
            afterForAutreImportAdded((ForFiscalAutreImpot) forFiscal);
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
    public <F extends ForFiscalPrincipal> F closeForFiscalPrincipal(F forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture) {
        if (forFiscalPrincipal != null) {
            forFiscalPrincipal = closeForFiscalPrincipal(forFiscalPrincipal.getTiers(), forFiscalPrincipal, dateFermeture, motifFermeture);
        }

        return forFiscalPrincipal;
    }

    protected <F extends ForFiscalPrincipal> F closeForFiscalPrincipal(Contribuable contribuable, F forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture) {
        Assert.notNull(contribuable);
        Assert.notNull(forFiscalPrincipal);
        if (forFiscalPrincipal.getDateDebut().isAfter(dateFermeture)) {
            throw new ValidationException(forFiscalPrincipal, "La date de fermeture (" + RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début (" +
                    RegDateHelper.dateToDisplayString(forFiscalPrincipal.getDateDebut())
                    + ") du for fiscal actif");
        }

        forFiscalPrincipal.setDateFin(dateFermeture);
        forFiscalPrincipal.setMotifFermeture(motifFermeture);

        afterForFiscalPrincipalClosed(contribuable, forFiscalPrincipal);

        return forFiscalPrincipal;
    }

	@Override
	public DecisionAci closeDecisionAci(DecisionAci decision, RegDate dateFin) {
		Assert.notNull(decision);
		if (decision.getDateDebut().isAfter(dateFin)) {
			throw new ValidationException(decision, "La date de fermeture (" + RegDateHelper.dateToDisplayString(dateFin) + ") est avant la date de début (" +
					RegDateHelper.dateToDisplayString(decision.getDateDebut())
					+ ") de la décision");
		}

		decision.setDateFin(dateFin);
		return decision;
	}

    private void afterForFiscalPrincipalClosed(Contribuable contribuable, ForFiscalPrincipal forFiscalPrincipal) {
	    evenementFiscalService.publierEvenementFiscalFermetureFor(forFiscalPrincipal);
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
            afterForFiscalSecondaireClosed(contribuable, forFiscalSecondaire);
        }

        return forFiscalSecondaire;
    }

    private void afterForFiscalSecondaireClosed(Contribuable contribuable, ForFiscalSecondaire forFiscalSecondaire) {
	    evenementFiscalService.publierEvenementFiscalFermetureFor(forFiscalSecondaire);
        tacheService.genereTacheDepuisFermetureForSecondaire(contribuable, forFiscalSecondaire);
	    resetFlagBlocageRemboursementAutomatiqueSelonFors(contribuable);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalPrincipalPP changeModeImposition(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateChangementModeImposition, ModeImposition modeImposition, MotifFor motifFor) {

        final ForFiscalPrincipalPP forFiscalPrincipal = contribuable.getForFiscalPrincipalAt(null);
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
	    ForFiscalPrincipalPP nouveauForFiscal = new ForFiscalPrincipalPP();
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
                evenementFiscalService.publierEvenementFiscalChangementModeImposition(nouveauForFiscal);
            }
        }

        // [UNIREG-2794] déblocage en cas d'ouverture de for fiscal principal vaudois
        resetFlagBlocageRemboursementAutomatiqueSelonFors(contribuable);

        // [UNIREG-2806] On schedule un réindexation pour le début du mois suivant (les changements d'assujettissement source->ordinaire sont décalés en fin de mois)
        final RegDate debutMoisProchain = RegDate.get(dateChangementModeImposition.year(), dateChangementModeImposition.month(), 1).addMonths(1);
        contribuable.scheduleReindexationOn(debutMoisProchain);

        return nouveauForFiscal;
    }

	private <F extends ForFiscalPrincipal> F corrigerForFiscalPrincipal(@NotNull F forFiscal, MotifFor motifFermeture, int noOfsAutoriteFiscale) {
		if (forFiscal.getMotifFermeture() == motifFermeture && forFiscal.getNumeroOfsAutoriteFiscale() == noOfsAutoriteFiscale) {
			// rien à faire
			return null;
		}

		final Tiers tiers = forFiscal.getTiers();
		Assert.notNull(tiers);

		// [UNIREG-2322] toutes les corrections doivent s'effectuer par une annulation du for suivi de la création d'un nouveau for avec la valeur corrigée.
		//noinspection unchecked
		F forCorrige = (F) forFiscal.duplicate();
		forFiscal.setAnnule(true);
		forCorrige.setMotifFermeture(motifFermeture);
		forCorrige.setNumeroOfsAutoriteFiscale(noOfsAutoriteFiscale);
		forCorrige = tiersDAO.addAndSave(tiers, forCorrige);

		// notifie le reste du monde
		evenementFiscalService.publierEvenementFiscalAnnulationFor(forFiscal);
		evenementFiscalService.publierEvenementFiscalOuvertureFor(forCorrige);
		if (forCorrige.getDateFin() != null) {
			evenementFiscalService.publierEvenementFiscalFermetureFor(forCorrige);
		}

		// [SIFISC-23472] ne pas oublier de recalculer le flag de blocage de remboursement automatique
		resetFlagBlocageRemboursementAutomatiqueSelonFors(tiers);

		return forCorrige;
	}

	private DecisionAci corrigerDecisionAci(@NotNull DecisionAci decisionAci, RegDate dateFin, String remarque, int noOfsAutoriteFiscale) {

		final boolean remarquesIdentiques = decisionAci.getRemarque()!=null && remarque !=null && decisionAci.getRemarque().equals(remarque);
		final boolean remarquesNulles = decisionAci.getRemarque()==null && remarque ==null;
		if (remarquesIdentiques || remarquesNulles)
			if ((decisionAci.getNumeroOfsAutoriteFiscale() == noOfsAutoriteFiscale)) {
				// rien à faire
				return null;
			}
		final Contribuable ctb = decisionAci.getContribuable();
		Assert.notNull(ctb);

		// [SIFISC-12624] toutes les corrections doivent s'effectuer par une annulation de la décision suivi de la création d'une nouvelle décision avec la valeur corrigée.
		DecisionAci decisionCorrigee = decisionAci.duplicate();
		decisionAci.setAnnule(true);
		decisionCorrigee.setRemarque(remarque);
		decisionCorrigee.setDateFin(dateFin);
		decisionCorrigee.setNumeroOfsAutoriteFiscale(noOfsAutoriteFiscale);
		decisionCorrigee = tiersDAO.addAndSave(ctb, decisionCorrigee);

		return decisionCorrigee;
	}

	private ForFiscalAutreElementImposable corrigerForAutreElementImposable(@NotNull ForFiscalAutreElementImposable forFiscal, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale) {
		if (forFiscal.getDateFin() == dateFermeture && forFiscal.getMotifFermeture() == motifFermeture && forFiscal.getNumeroOfsAutoriteFiscale() == noOfsAutoriteFiscale) {
			// rien à faire
			return null;
		}

		final Tiers tiers = forFiscal.getTiers();
		Assert.notNull(tiers);

		// [UNIREG-2322] toutes les corrections doivent s'effectuer par une annulation du for suivi de la création d'un nouveau for avec la valeur corrigée.
		ForFiscalAutreElementImposable forCorrige = (ForFiscalAutreElementImposable) forFiscal.duplicate();
		forFiscal.setAnnule(true);
		forCorrige.setDateFin(dateFermeture);
		forCorrige.setMotifFermeture(motifFermeture);
		forCorrige.setNumeroOfsAutoriteFiscale(noOfsAutoriteFiscale);
		forCorrige = tiersDAO.addAndSave(tiers, forCorrige);

		// notifie le reste du monde
		evenementFiscalService.publierEvenementFiscalAnnulationFor(forFiscal);
		evenementFiscalService.publierEvenementFiscalOuvertureFor(forCorrige);
		if (dateFermeture != null) {
			evenementFiscalService.publierEvenementFiscalFermetureFor(forCorrige);
		}

		return forCorrige;
	}

    public ForFiscalRevenuFortune corrigerForFiscalSecondaire(ForFiscalRevenuFortune forFiscal, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
                                                              int noOfsAutoriteFiscale) {
        Assert.notNull(forFiscal);

	    if (forFiscal.getDateDebut() == dateOuverture && forFiscal.getDateFin() == dateFermeture
			    && forFiscal.getMotifOuverture() == motifOuverture && forFiscal.getMotifFermeture() == motifFermeture
			    && forFiscal.getNumeroOfsAutoriteFiscale() == noOfsAutoriteFiscale) {
		    // rien à faire
		    return null;
	    }

	    final Tiers tiers = forFiscal.getTiers();
        Assert.notNull(tiers);

        // [UNIREG-2322] toutes les corrections doivent s'effectuer par une annulation du for suivi de la création d'un nouveau for avec la valeur corrigée.
	    ForFiscalRevenuFortune forCorrige = (ForFiscalRevenuFortune) forFiscal.duplicate();
        forFiscal.setAnnule(true);
        forCorrige.setDateDebut(dateOuverture);
	    forCorrige.setMotifOuverture(motifOuverture);
        forCorrige.setDateFin(dateFermeture);
        forCorrige.setMotifFermeture(motifFermeture);
	    forCorrige.setNumeroOfsAutoriteFiscale(noOfsAutoriteFiscale);
        forCorrige = tiersDAO.addAndSave(tiers, forCorrige);

        // notifie le reste du monde
        evenementFiscalService.publierEvenementFiscalAnnulationFor(forFiscal);
        evenementFiscalService.publierEvenementFiscalOuvertureFor(forCorrige);
        if (dateFermeture != null) {
            evenementFiscalService.publierEvenementFiscalFermetureFor(forCorrige);
        }

	    // [SIFISC-23472] ne pas oublier de recalculer le flag de blocage de remboursement automatique
	    resetFlagBlocageRemboursementAutomatiqueSelonFors(tiers);

	    return forCorrige;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, MotifRattachement motifRattachement,
                                                int autoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, ModeImposition modeImposition) {
        final ForFiscalPrincipalPP dernierForPrincipal = contribuable.getDernierForFiscalPrincipal();
        if (dernierForPrincipal != null && dernierForPrincipal.getDateFin() == null) {
            if (dateFin == null || dateFin.isAfter(dernierForPrincipal.getDateDebut())) {
                closeForFiscalPrincipal(contribuable, dateDebut.getOneDayBefore(), motifOuverture);
            }
        }

        final ForFiscalPrincipalPP forRtr;
        if (dateFin == null) {
            forRtr = openForFiscalPrincipal(contribuable, dateDebut, motifRattachement, autoriteFiscale, typeAutoriteFiscale, modeImposition, motifOuverture);
        }
        else {
            forRtr = openAndCloseForFiscalPrincipal(contribuable, dateDebut, motifRattachement, autoriteFiscale, typeAutoriteFiscale, modeImposition, motifOuverture, dateFin, motifFermeture);
        }

        if (motifOuverture == MotifFor.PERMIS_C_SUISSE || motifOuverture == MotifFor.CHGT_MODE_IMPOSITION) {
            // [UNIREG-2806] On schedule un réindexation pour le début du mois suivant (les changements d'assujettissement source->ordinaire sont décalés en fin de mois)
            final RegDate debutMoisProchain = RegDate.get(dateDebut.year(), dateDebut.month(), 1).addMonths(1);
            contribuable.scheduleReindexationOn(debutMoisProchain);
        }

        return forRtr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, MotifRattachement motifRattachement,
                                                int autoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, GenreImpot genreImpot) {
        final ForFiscalPrincipalPM dernierForPrincipal = contribuable.getDernierForFiscalPrincipal();
        if (dernierForPrincipal != null && dernierForPrincipal.getDateFin() == null) {
            if (dateFin == null || dateFin.isAfter(dernierForPrincipal.getDateDebut())) {
                closeForFiscalPrincipal(contribuable, dateDebut.getOneDayBefore(), motifOuverture);
            }
        }

        final ForFiscalPrincipalPM forRtr;
        if (dateFin == null) {
            forRtr = openForFiscalPrincipal(contribuable, dateDebut, motifRattachement, autoriteFiscale, typeAutoriteFiscale, motifOuverture, genreImpot);
        }
        else {
            forRtr = openAndCloseForFiscalPrincipal(contribuable, dateDebut, motifRattachement, autoriteFiscale, typeAutoriteFiscale, motifOuverture, dateFin, motifFermeture, genreImpot);
        }
        return forRtr;
    }

	@Nullable
	@Override
	public ForFiscalAutreElementImposable updateForAutreElementImposable(ForFiscalAutreElementImposable ffaei, RegDate dateFermeture, MotifFor motifFermeture, Integer noOfsAutoriteFiscale) {

		ForFiscalAutreElementImposable updated = ffaei;

		if (ffaei.getDateFin() == null && dateFermeture != null) {
			// le for a été fermé
			updated = closeForFiscalAutreElementImposable((Contribuable) ffaei.getTiers(), ffaei, dateFermeture, motifFermeture);
		}

		if (updated.getDateFin() != dateFermeture || updated.getMotifFermeture() != motifFermeture || !updated.getNumeroOfsAutoriteFiscale().equals(noOfsAutoriteFiscale)) {
			// quelque chose d'autre a changé
			updated = corrigerForAutreElementImposable(updated, dateFermeture, motifFermeture, noOfsAutoriteFiscale);
		}

		return updated == ffaei ? null : updated;
	}

	@Nullable
	@Override
	public ForFiscalSecondaire updateForSecondaire(ForFiscalSecondaire ffs, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
	                                               int noOfsAutoriteFiscale) {

		ForFiscalSecondaire updated = ffs;

		if (ffs.getDateDebut() == dateOuverture && ffs.getDateFin() == null && dateFermeture != null) {
			// le for a été fermé
			updated = closeForFiscalSecondaire((Contribuable) ffs.getTiers(), ffs, dateFermeture, motifFermeture);
		}

		if (updated.getDateDebut() != dateOuverture || updated.getDateFin() != dateFermeture ||
				updated.getMotifOuverture() != motifOuverture || updated.getMotifFermeture() != motifFermeture ||
				!updated.getNumeroOfsAutoriteFiscale().equals(noOfsAutoriteFiscale)) {
			// quelque chose d'autre a changé
			updated = (ForFiscalSecondaire) corrigerForFiscalSecondaire(updated, dateOuverture, motifOuverture, dateFermeture, motifFermeture, noOfsAutoriteFiscale);
		}

		return updated == ffs ? null : updated;
	}

	@Nullable
	@Override
	public ForFiscalPrincipal updateForPrincipal(ForFiscalPrincipal ffp, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale) {

		ForFiscalPrincipal updated = ffp;

		if (ffp.getDateFin() == null && dateFermeture != null) {
			// le for a été fermé
			updated = closeForFiscalPrincipal(ffp, dateFermeture, motifFermeture);
		}

		if (updated.getMotifFermeture() != motifFermeture || updated.getNumeroOfsAutoriteFiscale() != noOfsAutoriteFiscale) {
			// quelque chose d'autre a changé
			updated = corrigerForFiscalPrincipal(updated, motifFermeture, noOfsAutoriteFiscale);
		}

		return updated == ffp ? null : updated;
	}

	@Nullable
	@Override
	public ForDebiteurPrestationImposable updateForDebiteur(ForDebiteurPrestationImposable fdpi, RegDate dateFermeture, MotifFor motifFermeture) {

		ForDebiteurPrestationImposable updated = null;

		if (fdpi.getDateFin() == null && dateFermeture != null) {
			// le for a été fermé
			updated = closeForDebiteurPrestationImposable((DebiteurPrestationImposable) fdpi.getTiers(), fdpi, dateFermeture, motifFermeture, true);
		}

		return updated;
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

        annulerPeriodicitePosterieure(debiteur, dateDebut);

	    if (dateDebut == null) {
		    final Periodicite active = debiteur.getPeriodiciteAt(null);
		    if (periodeDecompte != active.getPeriodeDecompte() || periodiciteDecompte != active.getPeriodiciteDecompte()) {
			    throw new IllegalArgumentException("Une date doit toujours être donnée si on change la périodicité");
		    }
		    final Periodicite derniere = debiteur.getDernierePeriodicite();
		    if (active != derniere) {
			    throw new IllegalArgumentException("Une date doit toujours être donnée quand il existe des périodicités avec une date de début future.");
		    }
		    return active;
	    }

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

            final RegDate veilleDebut = dateDebut.getOneDayBefore();
            if (courante.getDateDebut() != null && courante.getDateDebut().isAfter(veilleDebut)) {
                // la périodicité courante est masquée par le nouvelle périodicité, on l'annule (et on continue de boucler)
                courante.setAnnule(true);
            }
            else {
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
    private void annulerPeriodicitePosterieure(DebiteurPrestationImposable debiteur, RegDate dateDebut) {
        final List<Periodicite> periodicites = debiteur.getPeriodicitesSorted();
        if (periodicites != null && !periodicites.isEmpty()) {
            for (Periodicite periodicite : periodicites) {
                if (RegDateHelper.isAfter(periodicite.getDateDebut(), dateDebut, NullDateBehavior.LATEST)) {
                    periodicite.setAnnule(true);
                }
            }
        }
    }

	@Override
	public ForFiscalSecondaire addForSecondaire(Contribuable contribuable, RegDate dateOuverture, @Nullable RegDate dateFermeture, MotifRattachement motifRattachement,
	                                            int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture, @Nullable MotifFor motifFermeture,
	                                            GenreImpot genreImpot) {

		// Ouvre un nouveau for à la date d'événement
		ForFiscalSecondaire nouveauForFiscal = new ForFiscalSecondaire();
		nouveauForFiscal.setDateDebut(dateOuverture);
		nouveauForFiscal.setDateFin(dateFermeture);
		nouveauForFiscal.setMotifRattachement(motifRattachement);
		nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
		nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
		nouveauForFiscal.setMotifOuverture(motifOuverture);
		nouveauForFiscal.setMotifFermeture(motifFermeture);
		nouveauForFiscal.setGenreImpot(genreImpot);
		nouveauForFiscal = tiersDAO.addAndSave(contribuable, nouveauForFiscal);

        if (validationService.validate(contribuable).errorsCount() == 0) {
            afterForFiscalSecondaireAdded(contribuable, nouveauForFiscal);
        }

        if (dateFermeture != null) {
            afterForFiscalSecondaireClosed(contribuable, nouveauForFiscal);
        }

        Assert.notNull(nouveauForFiscal);
        return nouveauForFiscal;
    }

	@Override
	public ForFiscalAutreElementImposable addForAutreElementImposable(Contribuable contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture,
	                                                                  MotifRattachement motifRattachement, int autoriteFiscale) {
		return openForFiscalAutreElementImposable(contribuable, dateDebut, motifOuverture, dateFin, motifFermeture, motifRattachement, autoriteFiscale);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable debiteur, RegDate dateDebut, MotifFor motifOuverture,
                                                         RegDate dateFin, MotifFor motifFermeture, TypeAutoriteFiscale typeAutoriteFiscale, int autoriteFiscale) {
        final ForDebiteurPrestationImposable dernierForDebiteur = debiteur.getDernierForDebiteur();
        if (dernierForDebiteur != null && dernierForDebiteur.getDateFin() == null) {
	        if (dateFin == null || dateFin.isAfter(dernierForDebiteur.getDateDebut())) {
		        closeForDebiteurPrestationImposable(debiteur, dernierForDebiteur, dateDebut.getOneDayBefore(), motifOuverture, false);
	        }

        }
        if (dernierForDebiteur == null) {
            //[UNIREG-2885] dans le cas de la création d'un premier for, on doit adapter si besoin la première périodicité
            adaptPremierePeriodicite(debiteur, dateDebut);
        }

        ForDebiteurPrestationImposable forRtr;
        if (dateFin == null) {
            forRtr = openForDebiteurPrestationImposable(debiteur, dateDebut, motifOuverture, autoriteFiscale, typeAutoriteFiscale);
        }
        else {
            forRtr = openAndCloseForDebiteurPrestationImposable(debiteur, dateDebut, motifOuverture, dateFin, motifFermeture, autoriteFiscale, typeAutoriteFiscale);
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
     * @param motifFermeture                 la motif de fermeture du for
     * @param fermerRapportsPrestation       <code>true</code> s'il faut fermer les rapports "prestation" du débiteur, <code>false</code> s'il faut les laisser ouverts
     * @return le for debiteur fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    @Override
    public ForDebiteurPrestationImposable closeForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur,
                                                                              ForDebiteurPrestationImposable forDebiteurPrestationImposable,
                                                                              RegDate dateFermeture, MotifFor motifFermeture,
                                                                              boolean fermerRapportsPrestation) {
        if (forDebiteurPrestationImposable != null) {
            if (forDebiteurPrestationImposable.getDateDebut().isAfter(dateFermeture)) {
                throw new ValidationException(forDebiteurPrestationImposable, "La date de fermeture ("
                        + RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
                        + RegDateHelper.dateToDisplayString(forDebiteurPrestationImposable.getDateDebut()) + ") du for fiscal actif");
            }
            forDebiteurPrestationImposable.setDateFin(dateFermeture);
	        forDebiteurPrestationImposable.setMotifFermeture(motifFermeture);

            // [UNIREG-2144] Fermeture des rapports de travail
            if (fermerRapportsPrestation) {
                for (RapportEntreTiers rapport : debiteur.getRapportsObjet()) {
	                if (!rapport.isAnnule() && rapport instanceof RapportPrestationImposable) {
	                    if (rapport.isValidAt(dateFermeture) && rapport.getDateFin() == null) {
                            rapport.setDateFin(dateFermeture);
	                    }
	                    else if (rapport.getDateDebut().isAfter(dateFermeture)) {
		                    rapport.setAnnule(true);
	                    }
	                }
                }
            }

            this.evenementFiscalService.publierEvenementFiscalFermetureFor(forDebiteurPrestationImposable);
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

            this.evenementFiscalService.publierEvenementFiscalFermetureFor(autre);
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
     */
    @Override
    public void fusionne(PersonnePhysique habitant, PersonnePhysique nonHabitant) {
        // Onglet Complements
        copieComplements(nonHabitant, habitant);
        copieRemarques(nonHabitant, habitant);

        // Onglet Fiscal
        final Set<ForFiscal> forsCible = new HashSet<>();
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
            if (forFiscalSource instanceof ForFiscalPrincipalPP) {
                ForFiscalPrincipalPP forFiscalCible = copieForFiscalPrincipal((ForFiscalPrincipalPP) forFiscalSource);
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
     * [SIFISC-12290] Calcul décalé en fin de transaction
     * [SIFISC-21857] Les entreprises sont maintenant également concernées
     * @param tiers le tiers dont on veut débloquer le reboursement automatique.
     */
    private void resetFlagBlocageRemboursementAutomatiqueSelonFors(Tiers tiers) {
        if (tiers instanceof ContribuableImpositionPersonnesPhysiques || tiers instanceof Entreprise) {
	        flagBlocageRembAutoCalculateurDecale.enregistrerDemandeRecalcul(tiers.getNumero());
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
     */
    private void copieComplements(Tiers tiersSource, Tiers tiersCible) {
        tiersCible.setComplementNom(tiersSource.getComplementNom());
        tiersCible.setPersonneContact(tiersSource.getPersonneContact());
        tiersCible.setNumeroTelecopie(tiersSource.getNumeroTelecopie());
        tiersCible.setNumeroTelephonePortable(tiersSource.getNumeroTelephonePortable());
        tiersCible.setNumeroTelephonePrive(tiersSource.getNumeroTelephonePrive());
        tiersCible.setNumeroTelephoneProfessionnel(tiersSource.getNumeroTelephoneProfessionnel());
        tiersCible.setBlocageRemboursementAutomatique(tiersSource.getBlocageRemboursementAutomatique());
	    if (tiersSource.getCoordonneesFinancieres() != null) {
		    tiersCible.setCoordonneesFinancieres(new CoordonneesFinancieres(tiersSource.getCoordonneesFinancieres()));
	    }
	    else {
		    tiersCible.setCoordonneesFinancieres(null);
	    }
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

    private ForFiscalPrincipalPP copieForFiscalPrincipal(ForFiscalPrincipalPP forFiscalSource) {
        final ForFiscalPrincipalPP forFiscalCible = new ForFiscalPrincipalPP();
        copieForFiscal(forFiscalSource, forFiscalCible);
        copieForFiscalRevenuFortune(forFiscalSource, forFiscalCible);
        forFiscalCible.setModeImposition(forFiscalSource.getModeImposition());
        return forFiscalCible;
    }

    /**
     * Copie les attributs de ForFiscal
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

	/**
	 * @param tiers un tiers
	 * @return sur une ligne, le nom ou la raison sociale du tiers
	 */
	@Override
	public String getNomRaisonSociale(Tiers tiers) {
		if (tiers instanceof PersonnePhysique) {
			return getNomPrenom((PersonnePhysique) tiers);
		}
		if (tiers instanceof MenageCommun) {
			final EnsembleTiersCouple couple = getEnsembleTiersCouple((MenageCommun) tiers, null);
			if (couple != null) {
				final PersonnePhysique principal = couple.getPrincipal();
				final PersonnePhysique conjoint = couple.getConjoint();
				final String nomPrincipal = principal != null ? getNomRaisonSociale(principal) : null;
				final String nomConjoint = conjoint != null ? getNomRaisonSociale(conjoint) : null;
				if (nomPrincipal == null && nomConjoint == null) {
					return null;
				}
				else if (nomConjoint == null) {
					return nomPrincipal;
				}
				else if (nomPrincipal == null) {
					return nomConjoint;
				}
				else {
					return CollectionsUtils.concat(Arrays.asList(nomPrincipal, nomConjoint), " / ");
				}
			}
			else {
				return null;
			}
		}
		if (tiers instanceof Entreprise) {
			return getDerniereRaisonSociale((Entreprise) tiers);
		}
		if (tiers instanceof Etablissement) {
			return getDerniereRaisonSociale((Etablissement) tiers);
		}
		if (tiers instanceof CollectiviteAdministrative) {
			return getNomCollectiviteAdministrative((CollectiviteAdministrative) tiers);
		}
		if (tiers instanceof AutreCommunaute) {
			return ((AutreCommunaute) tiers).getNom();
		}
		if (tiers instanceof DebiteurPrestationImposable) {
			final List<String> lignes = getRaisonSociale((DebiteurPrestationImposable) tiers);
			if (lignes != null && !lignes.isEmpty()) {
				return CollectionsUtils.concat(lignes, " / ");
			}
			else {
				return null;
			}
		}
		return null;
	}

	@Override
    public String getNomPrenom(PersonnePhysique personne) {
        Assert.notNull(personne);
        final NomPrenom nomPrenom = getDecompositionNomPrenom(personne, false);
        return nomPrenom.getNomPrenom();
    }

    private String getNom(PersonnePhysique personne) {
        final NomPrenom nomPrenom = getDecompositionNomPrenom(personne, false);
        return nomPrenom.getNom();
    }

    private String getPrenoms(PersonnePhysique personne) {
		final NomPrenom nomPrenom = getDecompositionNomPrenom(personne, true);
		return nomPrenom.getPrenom();
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
        return serviceCivilService.getDecompositionNomPrenom(individu, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegDate getDateNaissance(PersonnePhysique pp) {
	    if (pp == null) {
		    return null;
	    }

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
	    final List<ForFiscal> forsFiscauxValides = pp.getForsFiscauxValidAt(date);
	    for (ForFiscal forFiscal : forsFiscauxValides) {
		    if (forFiscal.isPrincipal() && MotifFor.VEUVAGE_DECES == ((ForFiscalPrincipal)forFiscal).getMotifOuverture()) {
			    return forFiscal.getDateDebut();
		    }
	    }
	    return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegDate getDateDeces(@Nullable PersonnePhysique pp) {
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

	@Override
	public RegDate getDateDecesDepuisDernierForPrincipal(PersonnePhysique pp) {

		// 1. On détermine si on doit se baser sur la personne physique ou sur un eventuel ménage
		Contribuable ctb = pp;
		final ForFiscalPrincipal dernierForPrincipalPP = pp.getDernierForFiscalPrincipal();
		final MenageCommun menage = findDernierMenageCommun(pp);
		if (menage != null) {
			final ForFiscalPrincipal dernierForPrincipalMenage = menage.getDernierForFiscalPrincipal();
			if ( dernierForPrincipalMenage != null) {
				if (dernierForPrincipalPP  == null || dernierForPrincipalMenage.getDateDebut().isAfter(dernierForPrincipalPP.getDateDebut())) {
					ctb = menage;
				}
			}
		}

		// 2. On verifie que le dernier for principal du contribuable concerné est bien fermé avec un motif veuvage/décès
		final ForFiscalPrincipal dernierForPrincipal = ctb.getDernierForFiscalPrincipal();
		if (dernierForPrincipal == null || dernierForPrincipal.getDateFin() == null || dernierForPrincipal.getMotifFermeture() != MotifFor.VEUVAGE_DECES) {
			return null; // D'apres son dernier for, cette personne est toujours vivante
		} else {
			return dernierForPrincipal.getDateFin(); // Le jour du décès est le jour de la date de fermeture du for
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

	    // TODO [SIPM] pour les PM, l'office de gestion est l'OIPM, non ?
	    if (tiers instanceof Entreprise || tiers instanceof Etablissement) {
		    return ServiceInfrastructureService.noOIPM;
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
            Commune communeGestion = serviceInfra.getCommuneByNumeroOfs(forGestion.getNoOfsCommune(), date);
            if (communeGestion != null) {
                final Integer codeRegion = communeGestion.getCodeRegion();
	            if (codeRegion != null) {
                    return tiersDAO.getCollectiviteAdministrativeForRegion(codeRegion);
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
        }
        catch (ServiceInfrastructureException e) {
            throw new RuntimeException("Impossible de déterminer l'office d'impôt de la commune avec le numéro Ofs = " + noOfsCommune, e);
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
    public ForFiscal annuleForFiscal(ForFiscal forFiscal) throws ValidationException {

	    ForFiscal forReouvert = null;

        Assert.notNull(forFiscal, "le for fiscal doit être renseigné");
        final Tiers tiers = forFiscal.getTiers();
        Assert.notNull(tiers, "le for fiscal doit être rattaché à un tiers");

	    // Récupération des données valides avant annulation
	    final ForsParType fpt = forFiscal.getTiers().getForsParType(true);

        //
        // Annulation du for
        //

	    forFiscal.setAnnule(true);

	    //
	    // Envoi d'un événement fiscal
	    //
	    evenementFiscalService.publierEvenementFiscalAnnulationFor(forFiscal);

	    //
	    // Ré-ouverture éventuelle du for précédent
	    //

	    final List<? extends ForFiscal> fors;
	    final Pair<String, String> singPlur;
	    if (forFiscal instanceof ForFiscalPrincipalPM) {
		    fors = fpt.principauxPM;
		    singPlur = Pair.of("for fiscal principal", "fors principaux");
	    }
	    else if (forFiscal instanceof ForFiscalPrincipalPP) {
		    fors = fpt.principauxPP;
		    singPlur = Pair.of("for fiscal principal", "fors principaux");
	    }
	    else if (forFiscal instanceof ForDebiteurPrestationImposable) {
		    fors = fpt.dpis;
		    singPlur = Pair.of("for débiteur", "fors débiteurs");
	    }
	    else {
		    fors = null;
		    singPlur = null;
	    }

	    if (fors != null) {
		    // [UNIREG-2607] Apparemment, quelqu'un a réussi à arriver dans un cas où cette collection
		    // était vide... le seul scénario auquel je pense est si cette méthode est appelée deux fois
		    // (double-click sur le bouton d'annulation, concurrence entre deux sessions...)
		    if (fors.isEmpty()) {
			    throw new ValidationException(forFiscal, String.format("Tous les %s sont déjà annulés.", singPlur.getRight()));
		    }

		    // seul le dernier for principal/débiteur peut être annulé
		    final ForFiscal dernierFor = CollectionsUtils.getLastElement(fors);
		    if (dernierFor != forFiscal) {
			    throw new ValidationException(forFiscal, String.format("Seul le dernier %s peut être annulé.", singPlur.getLeft()));
		    }

		    // ré-ouvre le for précédent si nécessaire
		    ForFiscal forPrecedent = null;
		    for (ForFiscal f : fors) {
			    if (f.getDateFin() == forFiscal.getDateDebut().getOneDayBefore()) {
				    forPrecedent = f;
				    break;
			    }
		    }
		    if (forPrecedent != null) {
			    forReouvert = reopenForFiscal(forPrecedent);
		    }
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

	    return forReouvert;
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

        final List<ForFiscal> forsFiscaux = AnnulableHelper.sansElementsAnnules(tiers.getForsFiscaux());
        if (forsFiscaux.isEmpty()) {
            return null;
        }

        // Lucky guess
        ForGestion forGestion;
        if (date == null) {
            // on essaie tout d'abord à la date de fin du dernier for fiscal
	        final RegDate dateDernierForFical = forsFiscaux.stream()
			        .max(Comparator.comparing(ForFiscal::getDateFin, NullDateBehavior.LATEST::compare))
			        .map(ForFiscal::getDateFin)
			        .orElse(null);
            forGestion = getForGestionActif(tiers, dateDernierForFical);
        }
        else {
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

	    if (!(tiers instanceof ContribuableImpositionPersonnesPhysiques)) {
		    // seuls les "assimilés-PP" ont un for de gestion
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
	                final ForFiscalPrincipalPP fp = (ForFiscalPrincipalPP) f;
	                if (isConformePourForGestion(tiers, fp, date)) {
		                forPrincipal = fp;
                        break; // pas besoin de chercher plus loin
                    }
                }
            }
            else if (f instanceof ForFiscalRevenuFortune) {
                final ForFiscalRevenuFortune frf = (ForFiscalRevenuFortune) f;
                final MotifRattachement motifRattachement = frf.getMotifRattachement();

                if (MotifRattachement.ACTIVITE_INDEPENDANTE == motifRattachement) {
                    forsActiviteIndependante.checkFor(frf);
                }
                else if (MotifRattachement.IMMEUBLE_PRIVE == motifRattachement) {
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
                                Commune c1 = serviceInfra.getCommuneByNumeroOfs(ofs1, o1.getDateFin());
                                Commune c2 = serviceInfra.getCommuneByNumeroOfs(ofs2, o2.getDateFin());
                                return c1.getNomOfficiel().compareTo(c2.getNomOfficiel());
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

	/**SIFISC-14122
	 * Permet de savoir si un for fiscal est compatible  avec les régles de determination d'un for de gestion à savoir:
	 * - Le mode d'imposition est Différent de source.
	 * - Si le for est fermé, le motif de fermeture est différent d'un mariage, d'une séparation ou d'un départ HC,  sauf pour les mixte_2 dans le cas d'un départ HC ou si la date de fin est au 31.12 de la période
	 * @param f for fiscal à analyser
	 * @param date de référence pour analyser le for de gestion, peut être nulle
	 * @return vrai si le for est éligible pour etre for de gestion, false sinon
	 */
	private boolean isConformePourForGestion(Tiers tiers, ForFiscalPrincipalPP f, @Nullable RegDate date) {

		//Découpage précis afin de faciliter la compréhension des différents cas
		final Integer periodeReference = date!=null ?RegDateHelper.getYear(date):RegDateHelper.getYear(RegDate.get());
		final RegDate dernierJourAnnee =  RegDateHelper.get(periodeReference, 12, 31);
		final boolean isDateFinForDansPeriodeReference=periodeReference.equals(RegDateHelper.getYear(f.getDateFin()));
		final boolean isFermetureDernierJourAnnee =RegDateHelper.equals(f.getDateFin(), dernierJourAnnee);
		final boolean isSourcier = f.getModeImposition() == ModeImposition.SOURCE;
		final boolean isDeparHorsCantonPourMixte2 = f.getModeImposition() == ModeImposition.MIXTE_137_2 && f.getMotifFermeture() == MotifFor.DEPART_HC;

		final boolean isFermeturePourMotifNotableDansPeriode = isDateFinForDansPeriodeReference && hasMotifFermetureSupprimantAssujettisssement(f);


		//Le ssourciers n'ont pas de for de gestion
		if (isSourcier) {
			return false;
		}

		//Un départ hors canton non mixte 2, un mariage ou une séparation  dans la période avant le dernier jour de l'année
		//Dans ce cas, pas la peine d'aller plus loin, ce n'est pas un candidat pour être for de gestion
		if (isFermeturePourMotifNotableDansPeriode && !isFermetureDernierJourAnnee && !isDeparHorsCantonPourMixte2) {
			return false;
		}

		//On récupère pour le for en cours d'analyse la liste des fors vaudois suivants accolés et ouverts dans la même période
		//Si on en trouve un avec un départ hors canton, le for en cours d'analyse ne peut pas être for de gestion
		final List<ForFiscalPrincipal> principaux = tiers.getForsFiscauxPrincipauxOuvertsApres(f.getDateDebut());
		final MovingWindow<ForFiscalPrincipal> iter = new MovingWindow<>(principaux);
		//Devrait être toujours vrai car on doit retrouver en début le for que l'on analyse
		if (iter.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipal> snapshot = iter.next();
			final ForFiscalPrincipalContext<ForFiscalPrincipal> forPrincipalContext = new ForFiscalPrincipalContext<>(snapshot);
			//On retrouve le for principal dans la movingWindow, on peut l'analyser
			final ForFiscalPrincipal current = forPrincipalContext.getCurrent();
			if (forPrincipalContext.hasNext()) {
				final List<ForFiscalPrincipal> nexts = forPrincipalContext.getAllNext();
				for (ForFiscalPrincipal next : nexts) {
					//SIFISC-16670 il faut que les fors suivants soient vaudois si on a un seul for non vaudois
					// dans les suivants on peut arréter car on a plus de continuité
					if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD != next.getTypeAutoriteFiscale()) {
						break;
					}
					final boolean isFermeDansPeriode = periodeReference.equals(RegDateHelper.getYear(next.getDateFin()));

					if (hasMotifFermetureSupprimantAssujettisssement(next) && isFermeDansPeriode) {
						//ON trouve un for avec un départ hors canton collé au for analysé
						// avec une date de fin dans la période,
						return false;
					}
				}
			}

		}
		// on a passé tous les tests, on est conforme à la définition d'un for de gestion
		return true;

	}

	/**
	 * Permet de savoir si un for a un motif de fermeture suceptible de supprimer un sassujettissement
	 * @param f for a analyser
	 * @return true si le for est fermé pour un  départ, un mariage ou une séparation, false pour tout autre motif de fermeture
	 */
	private boolean hasMotifFermetureSupprimantAssujettisssement(ForFiscalPrincipal f){
		final boolean isDepartHorsCantonDansPeriode =  f.getMotifFermeture() == MotifFor.DEPART_HC;
		final boolean isMariageDansPeriode =   f.getMotifFermeture() == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;
		final boolean isSeparationDansPeriode = f.getMotifFermeture() == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		return isDepartHorsCantonDansPeriode || isMariageDansPeriode || isSeparationDansPeriode;
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

        List<ForGestion> results = new ArrayList<>();

        final List<ForFiscal> forsFiscaux = tiers.getForsFiscauxSorted();
        if (forsFiscaux == null || forsFiscaux.isEmpty()) {
            return results;
        }

        // Récupère la liste des dates (triées par ordre croissant) où les fors fiscaux ont changés
        final SortedSet<RegDate> dates = new TreeSet<>();
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
        List<ForGestion> forsGestion = new ArrayList<>();
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
        final List<AdresseTiers> listeDesAdressesFermees = new ArrayList<>();
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

	    // [SIFISC-11465] La mention "Inactif" doit apparaître dès que le flag "débiteur inactif" est levé
        if (tiers instanceof PersonnePhysique && tiers.isDebiteurInactif()) {
            str = "Inactif";
        }

        // seuls les contribuables peuvent avoir un assujettissement
        else if (tiers instanceof Contribuable) {
            try {
                final List<Assujettissement> assujettissements = assujettissementService.determine((Contribuable) tiers, date.year());
                if (assujettissements != null && !assujettissements.isEmpty()) {
                    final Assujettissement valide = DateRangeHelper.rangeAt(assujettissements, date);
                    if (valide != null && valide.getType() != null) {
                        str = valide.getType().getDescription();
                    }
                }

                if (str == null) {
                    str = "Non assujetti";
                }
            }
            catch (Exception e) {
                LOGGER.warn("Impossible de calculer l'assujettissement du tiers " + tiers.getNumero(), e);
            }
        }
        else if (tiers instanceof DebiteurPrestationImposable) {
            final CategorieImpotSource categorie = ((DebiteurPrestationImposable) tiers).getCategorieImpotSource();
            if (categorie != null) {
                str = categorie.texte();
            }
        }

        return str;
    }

	@Override
	public Assujettissement getAssujettissement(Contribuable contribuable, @Nullable RegDate date) {
		if (date == null) {
			date = RegDate.get();
		}

		try {
			final List<Assujettissement> assujettissements = assujettissementService.determine(contribuable, date.year());
			if (assujettissements != null && !assujettissements.isEmpty()) {
				final Assujettissement valide = DateRangeHelper.rangeAt(assujettissements, date);
				if (valide != null) {
					return valide;
				}
			}
		}
		catch (AssujettissementException e) {
			LOGGER.warn("Impossible de calculer l'assujettissement du tiers " + contribuable.getNumero(), e);
		}

		// non-assujetti...
		return null;
	}

	/**
	 * @param entreprise entreprise dont on veut les exercices commerciaux
	 * @param dateLimite date de référence (a priori aujourd'hui ou dans le futur) pour la détermination du dernier exercice à renvoyer (si l'entreprise est encore active)
	 * @return les exercices commerciaux de cette entreprise (jusqu'à au plus tard l'exercice de la date de référence, ou, s'il n'y en a plus, le dernier exercice connu)
	 */
	private List<ExerciceCommercial> getExercicesCommerciauxJusqua(Entreprise entreprise, @NotNull RegDate dateLimite) {
		final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();
		final List<Bouclement> bouclements = AnnulableHelper.sansElementsAnnules(entreprise.getBouclements());
		final boolean noFors = forsPrincipaux.isEmpty();
		final boolean noBouclements = bouclements.isEmpty();

		if (noFors && noBouclements) {
			// rien de rien...
			return Collections.emptyList();
		}

		final RegDate dateDebutPremierExercice;
		if (entreprise.getDateDebutPremierExerciceCommercial() != null) {
			dateDebutPremierExercice = entreprise.getDateDebutPremierExerciceCommercial();
		}
		else if (noFors) {
			// on va supposer une date de début au lendemain du premier bouclement connu
			dateDebutPremierExercice = bouclementService.getDateProchainBouclement(bouclements, RegDateHelper.getEarlyDate(), false).getOneDayAfter();
		}
		else {
			// création à l'ouverture du premier for principal ? -> c'est la date de début du premier exercice
			final ForFiscalPrincipalPM premierForPrincipal = forsPrincipaux.get(0);
			final MotifFor premierMotif = premierForPrincipal.getMotifOuverture();
			if (premierMotif == MotifFor.DEBUT_EXPLOITATION || noBouclements) {
				// il s'agit donc de la création de la société, ou sinon, on n'a pas vraiment d'autre donnée de toute façon
				dateDebutPremierExercice = premierForPrincipal.getDateDebut();
			}
			else {
				// il s'agit donc d'un déménagement (ou de la création d'un établissement ou l'achat d'un immeuble...) par exemple, la PM existait
				// déjà avant avec des données connues de bouclements
				final RegDate dateBouclementConnueAvantDebutFor = bouclementService.getDateDernierBouclement(bouclements, premierForPrincipal.getDateDebut(), false);
				if (dateBouclementConnueAvantDebutFor == null) {
					// pas de bouclement connu avant le démarrage du for, on prend la date du for
					// TODO [SIPM] date de début du for ou une année avant le premier bouclement connu après le début du for ?
					dateDebutPremierExercice = premierForPrincipal.getDateDebut();
				}
				else {
					dateDebutPremierExercice = dateBouclementConnueAvantDebutFor.getOneDayAfter();
				}
			}
		}

		final RegDate dateFinDernierExercice;
		if (noFors) {
			// la seule limite de fin sera celle de l'exercice courant
			dateFinDernierExercice = bouclementService.getDateProchainBouclement(bouclements, dateLimite, true);
		}
		else {
			// ici, nous avons des fors principaux

			final ForFiscalPrincipalPM dernierForPrincipal = forsPrincipaux.get(forsPrincipaux.size() - 1);
			if (dernierForPrincipal.getDateFin() != null) {
				// [SIFISC-17850] si le dernier for principal est fermé, on s'arrête là, sauf si le motif de fermeture est "FAILLITE"
				// (s'il n'y a pas de cycle de bouclements connu, on s'arrête quand-même à la fin du for)
				if (dernierForPrincipal.getMotifFermeture() == MotifFor.FAILLITE && !noBouclements) {
					// en cas de faillite, on continue jusqu'à la fin du cycle en cours
					dateFinDernierExercice = bouclementService.getDateProchainBouclement(bouclements, dernierForPrincipal.getDateFin(), true);
				}
				else {
					dateFinDernierExercice = dernierForPrincipal.getDateFin();
				}
			}
			else if (noBouclements) {
				// arbitrairement, fin de l'exercice à la fin de cette année
				dateFinDernierExercice = RegDate.get(dateLimite.year(), 12, 31);
			}
			else {
				// for encore ouvert -> la seule limite de fin sera celle de l'exercice courant
				dateFinDernierExercice = bouclementService.getDateProchainBouclement(bouclements, dateLimite, true);
			}
		}

		return bouclementService.getExercicesCommerciaux(bouclements, new DateRangeHelper.Range(dateDebutPremierExercice, dateFinDernierExercice), false);
	}

	@Override
	public List<ExerciceCommercial> getExercicesCommerciaux(Entreprise entreprise) {
		return getExercicesCommerciauxJusqua(entreprise, RegDate.get());
	}

	@Override
	public ExerciceCommercial getExerciceCommercialAt(Entreprise entreprise, RegDate date) {
		final RegDate dateLimite = RegDateHelper.maximum(date, RegDate.get(), NullDateBehavior.EARLIEST);
		return DateRangeHelper.rangeAt(getExercicesCommerciauxJusqua(entreprise, dateLimite), date);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public ExclureContribuablesEnvoiResults setDateLimiteExclusion(List<Long> ctbIds, RegDate dateLimite, StatusManager s) {
        final ExclureContribuablesEnvoiProcessor processor = new ExclureContribuablesEnvoiProcessor(hibernateTemplate, transactionManager, this, adresseService);
        return processor.run(ctbIds, dateLimite, s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CorrectionFlagHabitantResults corrigeFlagHabitantSurPersonnesPhysiques(int nbThreads, StatusManager statusManager) {
        final CorrectionFlagHabitantProcessor processor = new CorrectionFlagHabitantProcessor(hibernateTemplate, this, transactionManager, statusManager, adresseService);
        return processor.corrigeFlagSurPersonnesPhysiques(nbThreads);
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
                        debiteurs = new HashSet<>(); // création à la demande
                    }
                    debiteurs.add(d);
                }
            }
        }

        return debiteurs;
    }

    @Override
    public RegDate getDateDebutNouvellePeriodicite(DebiteurPrestationImposable debiteur, PeriodiciteDecompte souhait) {
	    final RegDate debutValidite;
        final Declaration derniereDeclaration = debiteur.getDerniereDeclaration(DeclarationImpotSource.class);
        if (derniereDeclaration != null) {

	        final DeclarationImpotSource lr = (DeclarationImpotSource) derniereDeclaration;
	        final PeriodiciteDecompte dernierePeriodicite = lr.getPeriodicite();
	        if (dernierePeriodicite == PeriodiciteDecompte.UNIQUE || souhait == PeriodiciteDecompte.UNIQUE) {
		        // l'année d'après !
		        final int anneeDebut = derniereDeclaration.getPeriode().getAnnee() + 1;
		        debutValidite = RegDate.get(anneeDebut, 1, 1);
	        }
	        else {
		        // dès que c'est compatible avec l'ancienne et la nouvelle périodicité
		        final RegDate reference = derniereDeclaration.getDateFin().getOneDayAfter();
		        if (souhait.getDebutPeriode(reference) == reference) {
			        debutValidite = reference;
		        }
		        else {
			        debutValidite = souhait.getDebutPeriodeSuivante(reference);
			        Assert.isSame(debutValidite, dernierePeriodicite.getDebutPeriode(debutValidite));       // valide que cela joue aussi pour l'ancienne périodicité
		        }
	        }
        }
        else {
            final ForFiscal forDebiteur = debiteur.getDernierForDebiteur();
            if (forDebiteur != null) {
	            if (souhait == PeriodiciteDecompte.UNIQUE) {
		            debutValidite = RegDate.get(forDebiteur.getDateDebut().year(), 1, 1);
	            }
	            else {
                    debutValidite = souhait.getDebutPeriode(forDebiteur.getDateDebut());
	            }
            }
            else {
	            final int anneeDebut = RegDate.get().year();
                debutValidite = RegDate.get(anneeDebut, 1, 1);
            }
        }
        return debutValidite;
    }

    private static boolean isForVaudoisSource(ForFiscalPrincipalPP ffp) {
        return ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffp.getModeImposition() == ModeImposition.SOURCE;
    }

    private boolean isPersonnePhysiqueSourcierGris(PersonnePhysique pp, RegDate date) {
        final boolean gris;
        if (pp.getNumeroIndividu() == null) {
            final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipalAvant(date);
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
                        final ForFiscalPrincipalPP ffpMc = mc.getDernierForFiscalPrincipalAvant(date);
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
                final ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipalAvant(date);
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
        return getPersonnesPhysiques(menage, false,null).keySet();
    }
	@NotNull
	private Set<PersonnePhysique> getPersonnesPhysiques(MenageCommun menage,Integer ageMaximum) {
		return getPersonnesPhysiques(menage, false,ageMaximum).keySet();
	}

    @NotNull
    @Override
    public Map<PersonnePhysique, RapportEntreTiers> getToutesPersonnesPhysiquesImpliquees(MenageCommun menage) {
        return getPersonnesPhysiques(menage, true,null);
    }

    /**
     * @param menage               un ménage-commun
     * @param aussiRapportsAnnules <b>vrai</b> si l'on veut aussi les rapports annulés; <b>faux</b> autrement.
     * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun en ignorant (ou pas) les rapports annulés ; le dernier rapport entre tiers est également indiqué
     */
    @NotNull
    private Map<PersonnePhysique, RapportEntreTiers> getPersonnesPhysiques(MenageCommun menage, boolean aussiRapportsAnnules,Integer ageMaximum) {
        final Map<PersonnePhysique, RapportEntreTiers> personnes = new HashMap<>(aussiRapportsAnnules ? 4 : 2);
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
	                    final boolean ageMaximumAtteint = isRapportPlusVieuxQueAgeMaximum(r, ageMaximum);
                        if (!ignore && !ageMaximumAtteint) {
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
                raisonSociale = Collections.singletonList(getNomPrenom((PersonnePhysique) referent));
            } else if (referent instanceof MenageCommun) {
                raisonSociale = new ArrayList<>(2);
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
                raisonSociale = Collections.singletonList(((AutreCommunaute) referent).getNom());
            } else if (referent instanceof Entreprise) {
	            raisonSociale = Collections.singletonList(getDerniereRaisonSociale((Entreprise) referent));
            } else if (referent instanceof Etablissement) {
	            raisonSociale = Collections.singletonList(getDerniereRaisonSociale((Etablissement) referent));
            } else if (referent instanceof CollectiviteAdministrative) {
                try {
                    final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca = serviceInfra.getCollectivite(((CollectiviteAdministrative) referent).getNumeroCollectiviteAdministrative());
                    raisonSociale = new ArrayList<>(3);
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
            raisonSociale = new ArrayList<>(2);
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
    public String getDerniereRaisonSociale(Entreprise entreprise) {
	    final List<RaisonSocialeHisto> rss = getRaisonsSociales(entreprise, false);
	    if (! rss.isEmpty()) {
		    return CollectionsUtils.getLastElement(rss).getRaisonSociale();
	    }
	    return null;
    }

    @Override
    public String getDerniereRaisonSociale(Etablissement etablissement) {
	    if (etablissement.isConnuAuCivil()) {
		    SiteOrganisation siteOrganisation = getSiteOrganisationPourEtablissement(etablissement);
		    final List<DateRanged<String>> nom = siteOrganisation.getNom();
		    return nom.get(nom.size() - 1).getPayload();
	    }
	    else {
		    return etablissement.getRaisonSociale();
	    }
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
                        personnes = new HashSet<>();
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
                        personnes = new HashSet<>();
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
				inds = new HashSet<>(nosIndividus.size());
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
				inds = new HashSet<>(nosIndividus.size());
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
	public EvenementsCivilsNonTraites getIndividusAvecEvenementsCivilsNonTraites(Tiers tiers) {
		final Set<Long> noTiers = new HashSet<>(1);
		noTiers.add(tiers.getNumero());
		final Set<Long> nosIndividus = tiersDAO.getNumerosIndividu(noTiers, true);
		final EvenementsCivilsNonTraites res = new EvenementsCivilsNonTraites();
		if (!nosIndividus.isEmpty()) {
			res.addAll(EvenementsCivilsNonTraites.Source.REGPP, getIndividusAvecEvenementsCivilsRegPPNonTraites(nosIndividus));
			res.addAll(EvenementsCivilsNonTraites.Source.RCPERS, getIndividusAvecEvenementsCivilsECHNonTraites(nosIndividus));
		}
		return res;
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
     * @param tiers un tiers
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

	@NotNull
	@Override
	public <T extends HibernateEntity> Set<T> getLinkedEntities(@NotNull LinkedEntity entity, @NotNull Class<T> clazz, @NotNull LinkedEntity.Context context, boolean includeAnnuled) {
		final Set<T> linked = new HashSet<>();
		final Set<Object> visited = new HashSet<>(); // contient les entités et les clés déjà visitées
		extractLinkedEntities(entity, clazz, context, includeAnnuled, linked, visited);
		return linked;
	}

	private <T extends HibernateEntity> void extractLinkedEntities(LinkedEntity entity, @NotNull Class<T> clazz, @NotNull LinkedEntity.Context context, boolean includeAnnuled, Set<T> linked, Set<Object> visited) {

		final List<?> list = entity.getLinkedEntities(context, includeAnnuled);
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
				e = hibernateTemplate.get(key.getClazz(), (Serializable) key.getId());

				if (visited.contains(e)) { // on reteste sur l'entité uniquement
					continue; // on a déjà visité cette entité
				}
				visited.add(e);
			}
			else {
				e = (HibernateEntity) o; // selon le contrat de getLinkedEntities()
			}

			// on ajoute les tiers trouvés
			if (e != null && clazz.isAssignableFrom(e.getClass())) {
				//noinspection unchecked
				linked.add((T) e);
			}
			else if (e instanceof LinkedEntity) {
				extractLinkedEntities((LinkedEntity) e, clazz, context, false /* l'annulation des sous-entités est traitée séparemment, si nécessaire */, linked, visited); // récursif
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
    public void reouvrirForDebiteur(@NotNull ForDebiteurPrestationImposable forDebiteur) {
	    final RegDate dateFin = forDebiteur.getDateFin();
	    reopenForFiscal(forDebiteur);
	    final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) forDebiteur.getTiers();
	    reopenRapportsPrestationImposableFermesAt(dpi, dateFin);
    }

    @Override
    public NumerosOfficesImpot getOfficesImpot(int noOfs, @Nullable RegDate date) {

        final Commune commune = serviceInfra.getCommuneByNumeroOfs(noOfs, date);
        if (commune == null || !commune.isVaudoise()) {
            return null;
        }

        final Integer codeDistrict = commune.getCodeDistrict();
	    final Integer codeRegion = commune.getCodeRegion();
	    if (codeDistrict == null && codeRegion == null) {
		    return null;
	    }

        final CollectiviteAdministrative oid = codeDistrict == null ? null : tiersDAO.getCollectiviteAdministrativeForDistrict(codeDistrict, false);
        final CollectiviteAdministrative oir = codeRegion == null ? null : tiersDAO.getCollectiviteAdministrativeForRegion(codeRegion);

        return new NumerosOfficesImpot(oid == null ? 0 : oid.getNumero(), oir == null ? 0 : oir.getNumero());
    }

	@Override
	public List<RapportPrestationImposable> getAllRapportPrestationImposable(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, boolean nonAnnuleOnly, boolean doNotAutoFlush) {

		final List<RapportPrestationImposable> allRapports  = rapportEntreTiersDAO.getRapportsPrestationImposable(dpi.getNumero(),sourcier.getNumero(),false, doNotAutoFlush);
		if (nonAnnuleOnly) {
			List<RapportPrestationImposable> rapportsNonAnnule = new ArrayList<>();
			for (RapportPrestationImposable rapport : allRapports) {
				if (!rapport.isAnnule()) {
					rapportsNonAnnule.add(rapport);
				}
			}
			return rapportsNonAnnule;
		}
		return allRapports;
	}

	@Override
	public DecisionAci addDecisionAci(Contribuable ctb, TypeAutoriteFiscale typeAutoriteFiscale, int numeroAutoritéFiscale, RegDate dateDebut, RegDate dateFin, String Remarque) {
		DecisionAci d = new DecisionAci(ctb,dateDebut,dateFin,numeroAutoritéFiscale,typeAutoriteFiscale,Remarque);
		d = tiersDAO.addAndSave(ctb,d);
		Assert.notNull(d);
		return d;
	}

	@Override
	public DecisionAci updateDecisionAci(DecisionAci decisionAci, RegDate dateFin, String remarque, Integer numeroAutoriteFiscale) {

		DecisionAci updated = null;
		if (needCreationNouvelleDecision(decisionAci, dateFin, remarque, numeroAutoriteFiscale)) {
			updated = corrigerDecisionAci(decisionAci,dateFin, remarque, numeroAutoriteFiscale);
		}
		else{
			if (decisionAci.getDateFin() == null && dateFin != null) {
				decisionAci = closeDecisionAci(decisionAci,dateFin);
			}

			if (decisionAci.getRemarque() == null && StringUtils.trimToNull(remarque) != null) {
				decisionAci.setRemarque(remarque);
			}
			updated = decisionAci;
		}

		return updated;

	}

	@Override
	public boolean hasDecisionAciEnCours(long idTiers) {
		Tiers  tiers = tiersDAO.get(idTiers);
		if ( tiers == null){
			return false;
		}
		if (tiers instanceof Contribuable) {
			Contribuable ctb = (Contribuable)tiers;
			return ctb.hasDecisionEnCours();
		}

		return false;
	}

	@Override
	public boolean hasDecisionAciValidAt(long idTiers, RegDate date) {
		Tiers  tiers = tiersDAO.get(idTiers);
		if ( tiers == null){
			return false;
		}
		if (tiers instanceof Contribuable) {
			Contribuable ctb = (Contribuable)tiers;
			return ctb.hasDecisionAciValidAt(date);
		}

		return false;
	}

	@Override
	public List<MenageCommun> getAllMenagesCommuns(PersonnePhysique pp) {

		return getAllMenagesCommuns(pp,null);
	}

	private List<MenageCommun> getAllMenagesCommuns(PersonnePhysique pp,Integer ageMaximum) {
		if (pp == null) {
			return null;
		}

		final Set<RapportEntreTiers> rapportsEntreTiers = pp.getRapportsSujet();
		if (rapportsEntreTiers == null) {
			return null;
		}


		List<MenageCommun> menageCommuns =null;
		for (RapportEntreTiers rapport : rapportsEntreTiers) {
			if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType()) {
				if (menageCommuns == null) {
					menageCommuns =  new ArrayList<>();
				}
				final MenageCommun mc = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				final boolean ageMaximumAtteint = isRapportPlusVieuxQueAgeMaximum(rapport, ageMaximum);
				if (!ageMaximumAtteint ) {
					menageCommuns.add(mc);
				}

			}
		}

		return menageCommuns;
	}

	private boolean isRapportPlusVieuxQueAgeMaximum(RapportEntreTiers rapport, Integer ageMaximum) {
		//Si la date de début du ménage commun ajouté à l'age maximum est plus petit que lannée courante, on est plus vieux que l
		//l'age maximum
		return ageMaximum != null && rapport.getDateFin()!=null && rapport.getDateFin().year() +ageMaximum < RegDate.get().year();
	}

	@Override
	public boolean isSousInfluenceDecisions(Contribuable ctb) {

		// règle différenciée pour les contribuables PP et PM
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {

			final RegDate dateMinimalEffet = FiscalDateHelper.getDateMinimalePourEffetDecisionAci();
			final Set<Contribuable> contribuablesToCheck = new HashSet<>();

			final int ageMaximumLienVersTiers = AGE_MAXIMUM_LIEN_VERS_TIERS;
			construireListeTiersLies(ctb, contribuablesToCheck, ageMaximumLienVersTiers);
			//Verification sur tous les ctb trouvés
			for (Contribuable ctbToCheck : contribuablesToCheck) {
				if (ctbToCheck.hasDecisionRecenteFor(dateMinimalEffet)) {
					return true;
				}
			}
		}
		else if (ctb instanceof ContribuableImpositionPersonnesMorales) {
			return ctb.hasDecisionEnCours();
		}

		return false;
	}

	@Override
	public Set<Contribuable> getContribuablesLies(Contribuable ctb, Integer ageLiaison) {
		if (ageLiaison == null) {
			ageLiaison = AGE_MAXIMUM_LIEN_VERS_TIERS;
		}
		final Set<Contribuable> listeCtbs = new HashSet<>();
		construireListeTiersLies(ctb,listeCtbs,ageLiaison);
		return listeCtbs;
	}


	private void construireListeTiersLies(Contribuable ctb, Set<Contribuable> tiersLies, int ageMaximum) {
		if (tiersLies == null) {
			tiersLies = new HashSet<>();
		}

		tiersLies.add(ctb);
		if (ctb instanceof PersonnePhysique) {
			List<MenageCommun> menages = getAllMenagesCommuns((PersonnePhysique) ctb,ageMaximum);
			if (menages != null) {
				for (MenageCommun menage : menages) {
					if (!tiersLies.contains(menage)) {
						construireListeTiersLies(menage, tiersLies,ageMaximum);
					}

				}
			}
		}
		else if (ctb instanceof MenageCommun) {
			final Set<PersonnePhysique> membres = getPersonnesPhysiques((MenageCommun) ctb,ageMaximum);
			for (PersonnePhysique membre : membres) {
				if (!tiersLies.contains(membre)) {
					construireListeTiersLies(membre, tiersLies, ageMaximum);
				}

			}
		}
	}

	private boolean needCreationNouvelleDecision(DecisionAci d,RegDate dateFin, String remarque, Integer numeroAutoriteFiscale){
		final boolean datefinModifiee = d.getDateFin()!=null && dateFin!=null && !d.getDateFin().equals(dateFin);
		final boolean datefinSupprimee = d.getDateFin()!=null && dateFin==null;
		final boolean autoriteModifiee = d.getNumeroOfsAutoriteFiscale()!=null && numeroAutoriteFiscale !=null && !d.getNumeroOfsAutoriteFiscale().equals(numeroAutoriteFiscale);
		final boolean remarqueModifie = d.getRemarque()!=null && remarque!=null && !d.getRemarque().equals(remarque);
		final boolean remarqueSupprimee = d.getRemarque()!=null && remarque ==null;
		return datefinModifiee||datefinSupprimee|| autoriteModifiee||remarqueModifie||remarqueSupprimee;
	}

	@Override
	public StatutMenageCommun getStatutMenageCommun(MenageCommun menageCommun) {

		final EnsembleTiersCouple ensemble = getEnsembleTiersCouple(menageCommun, null);

		final Set<RapportEntreTiers> rapports = menageCommun.getRapportsObjet();
		if (rapports == null || rapports.isEmpty()) {
			// il n'y a pas de relation du tout, le ménage est nul
			return null;
		}

		AppartenanceMenage derniereAppartenance = null;
		for (RapportEntreTiers rapport : rapports) {
			if (!rapport.isAnnule() && rapport instanceof AppartenanceMenage) {
				if (derniereAppartenance == null || RegDateHelper.isAfter(rapport.getDateDebut(), derniereAppartenance.getDateDebut(), NullDateBehavior.EARLIEST)) {
					derniereAppartenance = (AppartenanceMenage) rapport;
				}
			}
		}

		if (derniereAppartenance == null) {
			// il n'y a pas d'appartenance non-annulée, le ménage est nul
			return null;
		}

		final RegDate dateFermeture = derniereAppartenance.getDateFin();
		if (dateFermeture == null) {
			// la dernière appartenance est toujours en cours, le ménage est actif
			return StatutMenageCommun.EN_VIGUEUR;
		}

		final RegDate dateDecesPrincipal = getDateDeces(ensemble.getPrincipal());
		if (dateDecesPrincipal == dateFermeture) {
			// le ménage est terminé, mais en raison du décès du principal.
			return StatutMenageCommun.TERMINE_SUITE_DECES;
		}

		final RegDate dateDecesConjoint = getDateDeces(ensemble.getConjoint());
		if (dateDecesConjoint == dateFermeture) {
			// le ménage est terminé, mais en raison du décès du conjoint
			return StatutMenageCommun.TERMINE_SUITE_DECES;
		}
		//SIFISC-15258
		final ForFiscalPrincipal dernierFor = menageCommun.getDernierForFiscalPrincipal();
		if ((dernierFor != null) && (dernierFor.getMotifFermeture() != null) && (dernierFor.getMotifFermeture() == MotifFor.VEUVAGE_DECES)) {
			return StatutMenageCommun.TERMINE_SUITE_DECES;
		}

		// dans tous les autres cas, il s'agit d'une séparation/divorce normal
		return StatutMenageCommun.TERMINE_SUITE_SEPARATION;
	}

	@Override
	public DomicileEtablissement addDomicileEtablissement(Etablissement etb, TypeAutoriteFiscale typeAutoriteFiscale, int numeroAutoriteFiscale, RegDate dateDebut, RegDate dateFin) {
		return tiersDAO.addAndSave(etb, new DomicileEtablissement(dateDebut, dateFin, typeAutoriteFiscale, numeroAutoriteFiscale, etb));
	}

	@Override
	public void closeDomicileEtablissement(DomicileEtablissement domicile, RegDate dateFin) {
		Assert.notNull(domicile);
		if (domicile.getDateDebut().isAfter(dateFin)) {
			throw new ValidationException(domicile, "La date de fermeture (" + RegDateHelper.dateToDisplayString(dateFin) + ") est avant la date de début (" +
					RegDateHelper.dateToDisplayString(domicile.getDateDebut())
					+ ") du domicile de l'établissement");
		}

		domicile.setDateFin(dateFin);
	}

	@Override
	public void closeRapportEntreTiers(RapportEntreTiers rapport, RegDate dateFin) {
		Assert.notNull(rapport);
		if (rapport.getDateDebut().isAfter(dateFin)) {
			throw new ValidationException(rapport, "La date de fermeture (" + RegDateHelper.dateToDisplayString(dateFin) + ") est avant la date de début (" +
					RegDateHelper.dateToDisplayString(rapport.getDateDebut())
					+ ") du rapport");
		}

		rapport.setDateFin(dateFin);
	}

	@Nullable
	private static AllegementFiscal getDernierAllegementFiscal(Entreprise e, AllegementFiscalHelper.OverlappingKey key) {
		final List<AllegementFiscal> tous = e.getAllegementsFiscauxNonAnnulesTries();
		for (AllegementFiscal af : CollectionsUtils.revertedOrder(tous)) {
			final AllegementFiscalHelper.OverlappingKey otherKey = AllegementFiscalHelper.OverlappingKey.valueOf(af);
			if (key.equals(otherKey)) {
				return af;
			}
		}
		return null;
	}

	@Override
	public AllegementFiscalCanton addAllegementFiscalCantonal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin, AllegementFiscalCantonCommune.Type type) {
		final AllegementFiscal existing = getDernierAllegementFiscal(e, AllegementFiscalHelper.OverlappingKey.cantonal(typeImpot));
		if (existing != null && existing.getDateFin() == null) {
			if (dateFin == null || dateFin.isAfter(existing.getDateDebut())) {
				closeAllegementFiscal(existing, dateDebut.getOneDayBefore());
			}
		}

		if (dateFin == null) {
			return openAllegementFiscalCantonal(e, pourcentageAllegement, typeImpot, dateDebut, type);
		}
		else {
			return openAndCloseAllegementFiscalCantonal(e, pourcentageAllegement, typeImpot, dateDebut, dateFin, type);
		}
	}

	@Override
	public AllegementFiscalCommune addAllegementFiscalCommunal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin, AllegementFiscalCantonCommune.Type type,
	                                                           @Nullable Integer noOfsCommune) {
		final AllegementFiscal existing = getDernierAllegementFiscal(e, AllegementFiscalHelper.OverlappingKey.communal(typeImpot, noOfsCommune));
		if (existing != null && existing.getDateFin() == null) {
			if (dateFin == null || dateFin.isAfter(existing.getDateDebut())) {
				closeAllegementFiscal(existing, dateDebut.getOneDayBefore());
			}
		}

		if (dateFin == null) {
			return openAllegementFiscalCommunal(e, pourcentageAllegement, typeImpot, noOfsCommune, dateDebut, type);
		}
		else {
			return openAndCloseAllegementFiscalCommunal(e, pourcentageAllegement, typeImpot, noOfsCommune, dateDebut, dateFin, type);
		}
	}

	@Override
	public AllegementFiscalConfederation addAllegementFiscalFederal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin, AllegementFiscalConfederation.Type type) {
		final AllegementFiscal existing = getDernierAllegementFiscal(e, AllegementFiscalHelper.OverlappingKey.federal(typeImpot));
		if (existing != null && existing.getDateFin() == null) {
			if (dateFin == null || dateFin.isAfter(existing.getDateDebut())) {
				closeAllegementFiscal(existing, dateDebut.getOneDayBefore());
			}
		}

		if (dateFin == null) {
			return openAllegementFiscalFederal(e, pourcentageAllegement, typeImpot, dateDebut, type);
		}
		else {
			return openAndCloseAllegementFiscalFederal(e, pourcentageAllegement, typeImpot, dateDebut, dateFin, type);
		}
	}

	@Override
	public AllegementFiscalCanton openAllegementFiscalCantonal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, AllegementFiscalCantonCommune.Type type) {
		return openAllegementFiscal(e, new AllegementFiscalCanton(dateDebut, null, pourcentageAllegement, typeImpot, type));
	}

	@Override
	public AllegementFiscalCanton openAndCloseAllegementFiscalCantonal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin, AllegementFiscalCantonCommune.Type type) {
		return openAndCloseAllegementFiscal(e, new AllegementFiscalCanton(dateDebut, dateFin, pourcentageAllegement, typeImpot, type));
	}

	@Override
	public AllegementFiscalCommune openAllegementFiscalCommunal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, Integer noOfsCommune, RegDate dateDebut, AllegementFiscalCantonCommune.Type type) {
		return openAllegementFiscal(e, new AllegementFiscalCommune(dateDebut, null, pourcentageAllegement, typeImpot, type, noOfsCommune));
	}

	@Override
	public AllegementFiscalCommune openAndCloseAllegementFiscalCommunal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, Integer noOfsCommune, RegDate dateDebut, RegDate dateFin,
	                                                                    AllegementFiscalCantonCommune.Type type) {
		return openAndCloseAllegementFiscal(e, new AllegementFiscalCommune(dateDebut, dateFin, pourcentageAllegement, typeImpot, type, noOfsCommune));
	}

	@Override
	public AllegementFiscalConfederation openAllegementFiscalFederal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, AllegementFiscalConfederation.Type type) {
		return openAllegementFiscal(e, new AllegementFiscalConfederation(dateDebut, null, pourcentageAllegement, typeImpot, type));
	}

	@Override
	public AllegementFiscalConfederation openAndCloseAllegementFiscalFederal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin,
	                                                                         AllegementFiscalConfederation.Type type) {
		return openAndCloseAllegementFiscal(e, new AllegementFiscalConfederation(dateDebut, dateFin, pourcentageAllegement, typeImpot, type));
	}

	private <T extends AllegementFiscal> T openAllegementFiscal(Entreprise e, T allegement) {
		final T af = tiersDAO.addAndSave(e, allegement);
		evenementFiscalService.publierEvenementFiscalOuvertureAllegementFiscal(af);
		return af;
	}

	private <T extends AllegementFiscal> T openAndCloseAllegementFiscal(Entreprise e, T allegement) {
		if (allegement.getDateFin() == null) {
			throw new IllegalArgumentException("Date de fin doit être assignée pour la clôture...");
		}
		final T saved = openAllegementFiscal(e, allegement);
		closeAllegementFiscal(saved, saved.getDateFin());
		return saved;
	}

	@Override
	public void closeAllegementFiscal(AllegementFiscal af, RegDate dateFin) {
		Assert.notNull(af);
		if (af.getDateDebut().isAfter(dateFin)) {
			throw new ValidationException(af, String.format("La date de fermeture (%s) est avant la date de début (%s) de l'allègement fiscal.",
			                                                RegDateHelper.dateToDisplayString(dateFin), RegDateHelper.dateToDisplayString(af.getDateDebut())));
		}

		af.setDateFin(dateFin);
		evenementFiscalService.publierEvenementFiscalFermetureAllegementFiscal(af);
	}

	@Override
	public void annuleAllegementFiscal(AllegementFiscal af) {
		if (!af.isAnnule()) {
			// annulation + envoi d'un événement fiscal
			af.setAnnule(true);
			evenementFiscalService.publierEvenementFiscalAnnulationAllegementFiscal(af);
		}
	}

	@Nullable
	private static RaisonSocialeFiscaleEntreprise getDerniereRaisonSocialeFiscale(Entreprise e) {
		final List<RaisonSocialeFiscaleEntreprise> toutes = e.getRaisonsSocialesNonAnnuleesTriees();
		if (toutes.isEmpty()) {
			return null;
		}
		return CollectionsUtils.getLastElement(toutes);
	}

	@Override
	public RaisonSocialeFiscaleEntreprise addRaisonSocialeFiscale(Entreprise e, String raisonSociale, RegDate dateDebut, RegDate dateFin) {
		checkEditionCivileAutorisee(e);

		final String raisonSocialeNettoyee = LiteralStringHelper.stripExtraSpacesAndBlanks(raisonSociale);
		if (StringUtils.isBlank(raisonSocialeNettoyee)) {
			return null;
		}

		RaisonSocialeFiscaleEntreprise existing = getDerniereRaisonSocialeFiscale(e);

		if (existing != null) {
			if (existing.getDateFin() == null) {
				if (dateFin == null || dateFin.isAfter(existing.getDateDebut())) {
					closeRaisonSocialeFiscale(existing, dateDebut.getOneDayBefore());
				}
			}
		}

		return (RaisonSocialeFiscaleEntreprise) tiersDAO.addAndSave(e, new RaisonSocialeFiscaleEntreprise(dateDebut, dateFin, raisonSocialeNettoyee));
	}

	@Override
	public void updateRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise rs, String raisonSociale) {
		checkEditionCivileAutorisee(rs.getEntreprise());

		rs.setAnnule(true);
		addRaisonSocialeFiscale(rs.getEntreprise(), raisonSociale, rs.getDateDebut(), rs.getDateFin());
	}

	@Override
	public void closeRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise raisonSociale, RegDate dateFin) {
		Assert.notNull(raisonSociale);
		raisonSociale.setDateFin(dateFin);
	}

	@Override
	public void annuleRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise raisonSociale) {
		checkEditionCivileAutorisee(raisonSociale.getEntreprise());
		final RaisonSocialeFiscaleEntreprise dernier = getDerniereRaisonSocialeFiscale(raisonSociale.getEntreprise());
		if (dernier != raisonSociale) {
			throw new ValidationException(raisonSociale, "Seule la dernière raison sociale peut être annulée.");
		}
		raisonSociale.setAnnule(true);

		// On ré-ouvre la précédente
		final RaisonSocialeFiscaleEntreprise precedente = getDerniereRaisonSocialeFiscale(raisonSociale.getEntreprise());
		if (precedente != null) {
			if (precedente.getDateFin() == raisonSociale.getDateDebut().getOneDayBefore()) {
				final RaisonSocialeFiscaleEntreprise reouverte = new RaisonSocialeFiscaleEntreprise(precedente.getDateDebut(), null, precedente.getRaisonSociale());
				precedente.setAnnule(true);
				tiersDAO.addAndSave(raisonSociale.getEntreprise(), reouverte);
			}
		}
		else {
			throw new ValidationException(raisonSociale, "Impossible d'annuler l'unique raison sociale.");
		}
	}

	@Nullable
	private static FormeJuridiqueFiscaleEntreprise getDerniereFormeJuridiqueFiscale(Entreprise e) {
		final List<FormeJuridiqueFiscaleEntreprise> toutes = e.getFormesJuridiquesNonAnnuleesTriees();
		if (toutes.isEmpty()) {
			return null;
		}
		return CollectionsUtils.getLastElement(toutes);
	}

	@Override
	public FormeJuridiqueFiscaleEntreprise addFormeJuridiqueFiscale(Entreprise e, FormeJuridiqueEntreprise formeJuridique, RegDate dateDebut, RegDate dateFin) {
		checkEditionCivileAutorisee(e);

		FormeJuridiqueFiscaleEntreprise existing = getDerniereFormeJuridiqueFiscale(e);

		if (existing != null) {
			if (existing.getDateFin() == null) {
				if (dateFin == null || dateFin.isAfter(existing.getDateDebut())) {
					closeFormeJuridiqueFiscale(existing, dateDebut.getOneDayBefore());
				}
			}
		}

		return (FormeJuridiqueFiscaleEntreprise) tiersDAO.addAndSave(e, new FormeJuridiqueFiscaleEntreprise(dateDebut, dateFin, formeJuridique));
	}

	@Override
	public void updateFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise fj, FormeJuridiqueEntreprise formeJuridique) {
		checkEditionCivileAutorisee(fj.getEntreprise());
		fj.setAnnule(true);
		addFormeJuridiqueFiscale(fj.getEntreprise(), formeJuridique, fj.getDateDebut(), fj.getDateFin());
	}

	@Override
	public void closeFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise formeJuridique, RegDate dateFin) {
		Assert.notNull(formeJuridique);
		formeJuridique.setDateFin(dateFin);
	}

	@Override
	public void annuleFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise formeJuridique) {
		checkEditionCivileAutorisee(formeJuridique.getEntreprise());
		final FormeJuridiqueFiscaleEntreprise dernier = getDerniereFormeJuridiqueFiscale(formeJuridique.getEntreprise());
		if (dernier != formeJuridique) {
			throw new ValidationException(formeJuridique, "Seule la dernière forme juridique peut être annulée.");
		}
		formeJuridique.setAnnule(true);

		// On ré-ouvre la précédente
		final FormeJuridiqueFiscaleEntreprise precedente = getDerniereFormeJuridiqueFiscale(formeJuridique.getEntreprise());
		if (precedente != null) {
			if (precedente.getDateFin() == formeJuridique.getDateDebut().getOneDayBefore()) {
				final FormeJuridiqueFiscaleEntreprise reouverte = new FormeJuridiqueFiscaleEntreprise(precedente.getDateDebut(), null, precedente.getFormeJuridique());
				precedente.setAnnule(true);
				tiersDAO.addAndSave(formeJuridique.getEntreprise(), reouverte);
			}
		}
		else {
			throw new ValidationException(formeJuridique, "Impossible d'annuler l'unique forme juridique.");
		}
	}

	@Nullable
	private static DomicileEtablissement getDernierDomicileFiscal(Etablissement eta) {
		final List<DomicileEtablissement> tous = eta.getSortedDomiciles(false);
		if (tous.isEmpty()) {
			return null;
		}
		return CollectionsUtils.getLastElement(tous);
	}

	@Override
	public DomicileEtablissement addDomicileFiscal(Etablissement etablissement, TypeAutoriteFiscale typeAutorite, Integer noOfs, RegDate dateDebut, RegDate dateFin) {
		checkEditionCivileAutorisee(etablissement);

		DomicileEtablissement existing = getDernierDomicileFiscal(etablissement);

		if (existing != null) {
			if (existing.getDateFin() == null) {
				if (dateFin == null || dateFin.isAfter(existing.getDateDebut())) {
					closeDomicileFiscal(existing, dateDebut.getOneDayBefore());
				}
			}
		}

		return addDomicileEtablissement(etablissement, typeAutorite, noOfs, dateDebut, dateFin);
	}

	@Override
	public DomicileEtablissement updateDomicileFiscal(DomicileEtablissement domicile, TypeAutoriteFiscale typeAutorite, Integer noOfs) {
		checkEditionCivileAutorisee(domicile.getEtablissement());
		domicile.setAnnule(true);
		return addDomicileFiscal(domicile.getEtablissement(), typeAutorite, noOfs, domicile.getDateDebut(), domicile.getDateFin());
	}

	@Override
	public DomicileEtablissement updateDomicileFiscal(DomicileEtablissement domicile, TypeAutoriteFiscale typeAutorite, Integer noOfs, RegDate dateFin) {
		Assert.notNull(domicile);
		checkEditionCivileAutorisee(domicile.getEtablissement());
		final DomicileEtablissement dernier = getDernierDomicileFiscal(domicile.getEtablissement());
		if (dernier != domicile && domicile.getDateFin() != dateFin) {
			throw new ValidationException(domicile, "Seul le dernier domicile peut avoir sa date de fin changée.");
		}
		domicile.setAnnule(true);
		return addDomicileFiscal(domicile.getEtablissement(), typeAutorite, noOfs, domicile.getDateDebut(), dateFin);
	}

	@Override
	public void closeDomicileFiscal(DomicileEtablissement domicile, RegDate dateFin) {
		checkEditionCivileAutorisee(domicile.getEtablissement());
		final DomicileEtablissement dernier = getDernierDomicileFiscal(domicile.getEtablissement());
		if (dernier != domicile) {
			throw new ValidationException(domicile, "Seul le dernier domicile peut être fermé.");
		}
		closeDomicileEtablissement(domicile, dateFin);
	}

	@Override
	public void annuleDomicileFiscal(DomicileEtablissement domicile) {
		checkEditionCivileAutorisee(domicile.getEtablissement());
		final DomicileEtablissement dernier = getDernierDomicileFiscal(domicile.getEtablissement());
		if (dernier != domicile) {
			throw new ValidationException(domicile, "Seul le dernier domicile peut être annulée.");
		}
		domicile.setAnnule(true);

		// On ré-ouvre la précédente si les domiciles se touchaient
		final DomicileEtablissement precedent = getDernierDomicileFiscal(domicile.getEtablissement());
		if (precedent != null) {
			if (precedent.getDateFin() == domicile.getDateDebut().getOneDayBefore()) {
				final DomicileEtablissement reouvert = new DomicileEtablissement(precedent.getDateDebut(), null, precedent.getTypeAutoriteFiscale(), precedent.getNumeroOfsAutoriteFiscale(), domicile.getEtablissement());
				precedent.setAnnule(true);
				tiersDAO.addAndSave(domicile.getEtablissement(), reouvert);
			}
		}
		else {
			throw new ValidationException(domicile, "Impossible d'annuler l'unique domicile.");
		}
	}

	@Nullable
	private static CapitalFiscalEntreprise getDernierCapitalFiscal(Entreprise e) {
		final List<CapitalFiscalEntreprise> tous = e.getCapitauxNonAnnulesTries();
		if (tous.isEmpty()) {
			return null;
		}
		return CollectionsUtils.getLastElement(tous);
	}

	@Override
	public CapitalFiscalEntreprise addCapitalFiscal(Entreprise e, Long montant, String monnaie, RegDate dateDebut, RegDate dateFin) {
		Assert.notNull(montant);
		Assert.notNull(monnaie);
		checkEditionCivileAutorisee(e);

		CapitalFiscalEntreprise existing = getDernierCapitalFiscal(e);

		if (existing != null) {
			if (existing.getDateFin() == null) {
				if (dateFin == null || dateFin.isAfter(existing.getDateDebut())) {
					closeCapitalFiscal(existing, dateDebut.getOneDayBefore());
				}
			}
		}

		return (CapitalFiscalEntreprise) tiersDAO.addAndSave(e, new CapitalFiscalEntreprise(dateDebut, dateFin, new MontantMonetaire(montant, monnaie)));
	}

	@Override
	public void updateCapitalFiscal(CapitalFiscalEntreprise cf, Long montant, String monnaie, RegDate dateFin) {
		checkEditionCivileAutorisee(cf.getEntreprise());
		cf.setAnnule(true);
		addCapitalFiscal(cf.getEntreprise(), montant, monnaie, cf.getDateDebut(), cf.getDateFin());
	}

	@Override
	public void closeCapitalFiscal(CapitalFiscalEntreprise capital, RegDate dateFin) {
		Assert.notNull(capital);
		capital.setDateFin(dateFin);
	}

	@Override
	public void annuleCapitalFiscal(CapitalFiscalEntreprise capital) {
		checkEditionCivileAutorisee(capital.getEntreprise());

		final CapitalFiscalEntreprise dernier = getDernierCapitalFiscal(capital.getEntreprise());
		if (dernier != capital) {
			throw new ValidationException(capital, "Seul le dernier capital peut être annulé.");
		}
		capital.setAnnule(true);

		// On ré-ouvre le précédent
		final CapitalFiscalEntreprise precedent = getDernierCapitalFiscal(capital.getEntreprise());
		if (precedent != null && precedent.getDateFin() == capital.getDateDebut().getOneDayBefore()) {
			final CapitalFiscalEntreprise reouvert = new CapitalFiscalEntreprise(precedent.getDateDebut(), null, precedent.getMontant());
			precedent.setAnnule(true);
			tiersDAO.addAndSave(capital.getEntreprise(), reouvert);
		}
	}

	@Nullable
	private static RegimeFiscal getDernierRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee) {
		final List<RegimeFiscal> tous = e.getRegimesFiscauxNonAnnulesTries();
		for (RegimeFiscal rf : CollectionsUtils.revertedOrder(tous)) {
			if (rf.getPortee() == portee) {
				return rf;
			}
		}
		return null;
	}

	@Override
	public RegimeFiscal addRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut, RegDate dateFin) {
		// s'il existe un régime fiscal déjà valide à la date de début demandée, il faut
		// le fermer (simple fermeture ou annulation et remplacement, selon la valeur actuelle de la date de fin)
		final List<RegimeFiscal> all = e.getRegimesFiscauxNonAnnulesTries(portee);
		final RegimeFiscal toClose = DateRangeHelper.rangeAt(all, dateDebut);
		if (toClose != null) {
			// deux cas : date de fin nulle ou pas
			if (toClose.getDateFin() == null) {
				closeRegimeFiscal(toClose, dateDebut.getOneDayBefore());
			}
			else {
				final RegimeFiscal copy = toClose.duplicate();
				toClose.setAnnule(true);
				copy.setDateFin(dateDebut.getOneDayBefore());
				final RegimeFiscal savedCopy = tiersDAO.addAndSave(e, copy);
				evenementFiscalService.publierEvenementFiscalFermetureRegimeFiscal(savedCopy);
			}
		}

		if (dateFin == null) {
			return openRegimeFiscal(e, portee, type, dateDebut);
		}
		else {
			return openAndCloseRegimeFiscal(e, portee, type, dateDebut, dateFin);
		}
	}

	@Override
	public RegimeFiscal replaceRegimeFiscal(RegimeFiscal oldValue, TypeRegimeFiscal type) {
		oldValue.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationRegimeFiscal(oldValue);
		return addRegimeFiscal(oldValue.getEntreprise(), oldValue.getPortee(), type, oldValue.getDateDebut(), oldValue.getDateFin());
	}

	@Override
	public RegimeFiscal openAndCloseRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut, RegDate dateFin) {
		final RegimeFiscal rf = tiersDAO.addAndSave(e, new RegimeFiscal(dateDebut, dateFin, portee, type.getCode()));
		evenementFiscalService.publierEvenementFiscalOuvertureRegimeFiscal(rf);
		closeRegimeFiscal(rf, rf.getDateFin());
		return rf;

		// TODO si le type est indeterminé, il faudra envoyer une nouvelle tâche au DPerm
	}

	@Override
	public RegimeFiscal openRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut) {
		final RegimeFiscal rf = tiersDAO.addAndSave(e, new RegimeFiscal(dateDebut, null, portee, type.getCode()));
		evenementFiscalService.publierEvenementFiscalOuvertureRegimeFiscal(rf);
		return rf;

		// TODO si le type est indeterminé, il faudra envoyer une nouvelle tâche au DPerm
	}

	@Override
	public void closeRegimeFiscal(RegimeFiscal rf, RegDate dateFin) {
		if (rf.getDateDebut().isAfter(dateFin)) {
			throw new ValidationException(rf, String.format("La date de fermeture (%s) est avant la date de début (%s) du régime fiscal actif.",
			                                                RegDateHelper.dateToDisplayString(dateFin), RegDateHelper.dateToDisplayString(rf.getDateDebut())));
		}

		rf.setDateFin(dateFin);
		evenementFiscalService.publierEvenementFiscalFermetureRegimeFiscal(rf);
	}

	@Override
	public void annuleRegimeFiscal(RegimeFiscal rf) {
		final RegimeFiscal dernier = getDernierRegimeFiscal(rf.getEntreprise(), rf.getPortee());
		if (dernier != rf) {
			throw new ValidationException(rf, "Seul le dernier régime fiscal peut être annulé.");
		}
		rf.setAnnule(true);

		// éventuellement, ou ré-ouvre le précédent
		final RegimeFiscal precedent = getDernierRegimeFiscal(rf.getEntreprise(), rf.getPortee());
		if (precedent != null && precedent.getDateFin() == rf.getDateDebut().getOneDayBefore()) {
			final RegimeFiscal reouvert = precedent.duplicate();
			precedent.setAnnule(true);
			reouvert.setDateFin(null);
			tiersDAO.addAndSave(rf.getEntreprise(), reouvert);
		}

		// et on envoie un événement fiscal
		evenementFiscalService.publierEvenementFiscalAnnulationRegimeFiscal(rf);
	}

	@Nullable
	private static FlagEntreprise getDernierFlagEntreprise(Entreprise e, GroupeFlagsEntreprise groupe) {
		final List<FlagEntreprise> all = e.getFlagsNonAnnulesTries(groupe);
		return all.isEmpty() ? null : all.get(all.size() - 1);
	}

	@Override
	public FlagEntreprise addFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut, @Nullable RegDate dateFin) {
		final FlagEntreprise existing = getDernierFlagEntreprise(e, type.getGroupe());
		if (existing != null && existing.getDateFin() == null && type.getGroupe().isFlagsMutuellementExclusifs()) {
			if (dateFin == null || dateFin.isAfter(existing.getDateDebut())) {
				closeFlagEntreprise(existing, dateDebut.getOneDayBefore());
			}
		}

		if (dateFin == null) {
			return openFlagEntreprise(e, type, dateDebut);
		}
		else {
			return openAndCloseFlagEntreprise(e, type, dateDebut, dateFin);
		}
	}

	@Override
	public FlagEntreprise openFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut) {
		final FlagEntreprise flag = tiersDAO.addAndSave(e, new FlagEntreprise(type, dateDebut, null));
		evenementFiscalService.publierEvenementFiscalOuvertureFlagEntreprise(flag);
		return flag;
	}

	@Override
	public FlagEntreprise openAndCloseFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut, RegDate dateFin) {
		final FlagEntreprise flag = tiersDAO.addAndSave(e, new FlagEntreprise(type, dateDebut, dateFin));
		evenementFiscalService.publierEvenementFiscalOuvertureFlagEntreprise(flag);
		closeFlagEntreprise(flag, flag.getDateFin());
		return flag;
	}

	@Override
	public void closeFlagEntreprise(FlagEntreprise flag, RegDate dateFin) {
		if (flag.getDateDebut().compareTo(dateFin) > 0) {
			throw new ValidationException(flag, String.format("La date de fin de validité (%s) est avant la date de début de validité (%s) du flag entreprise.",
			                                                  RegDateHelper.dateToDisplayString(dateFin),
			                                                  RegDateHelper.dateToDisplayString(flag.getDateDebut())));
		}
		flag.setDateFin(dateFin);
		evenementFiscalService.publierEvenementFiscalFermetureFlagEntreprise(flag);
	}

	@Override
	public void annuleFlagEntreprise(FlagEntreprise flag) {
		flag.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationFlagEntreprise(flag);
	}

	private static final class MandatKey {

		private final TypeMandat typeMandat;
		private final String codeGenreImpot;

		public MandatKey(Mandat mandat) {
			this.typeMandat = mandat.getTypeMandat();
			this.codeGenreImpot = this.typeMandat == TypeMandat.SPECIAL ? mandat.getCodeGenreImpot() : null;
		}

		public MandatKey(AdresseMandataire adresse) {
			this.typeMandat = adresse.getTypeMandat();
			this.codeGenreImpot = this.typeMandat == TypeMandat.SPECIAL ? adresse.getCodeGenreImpot() : null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final MandatKey mandatKey = (MandatKey) o;
			return typeMandat == mandatKey.typeMandat && Objects.equals(codeGenreImpot, mandatKey.codeGenreImpot);
		}

		@Override
		public int hashCode() {
			int result = typeMandat != null ? typeMandat.hashCode() : 0;
			result = 31 * result + (codeGenreImpot != null ? codeGenreImpot.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Annule les entites existantes qui intersectent avec le nouveau mandat ciblé et renvoie une liste d'entités non-persistés
	 * qui prennent en compte la place que va occuper le nouveau mandat
	 * @param mandat le nouveau mandat (enfin, ses dates...)
	 * @param sortedExistingMandats la liste triée des mandats existants de même type
	 * @param <T> le type d'entité de mandat
	 * @return la liste des nouvelles entités à créer pour que, en tenant compte des entités annulés, on ait bien la place pour le nouveau mandat
	 */
	@NotNull
	private static <T extends HibernateDateRangeEntity & Duplicable<T>> List<T> computeNewEntitieForRoomMaking(DateRange mandat, List<T> sortedExistingMandats) {
		// intersections du nouveau mandat avec les mandats de même type existants ?
		final List<DateRange> intersections = DateRangeHelper.intersections(mandat, sortedExistingMandats);
		if (intersections != null && !intersections.isEmpty()) {
			final List<T> aAjouter = new LinkedList<>();
			for (DateRange intersection : intersections) {
				// pour toute intersection, on recopie la partie avant et la partie après du mandat existant
				final RegDate dateReference = intersection.getDateDebut() != null ? intersection.getDateDebut() : intersection.getDateFin();
				final T intersectant = DateRangeHelper.rangeAt(sortedExistingMandats, dateReference);
				RegDate datePourFermerIntersectant = null;
				if (RegDateHelper.isBefore(intersectant.getDateDebut(), intersection.getDateDebut(), NullDateBehavior.EARLIEST)) {
					// si l'existant n'est pas fermé, on peut juste faire ça...
					if (intersectant.getDateFin() == null) {
						datePourFermerIntersectant = intersection.getDateDebut().getOneDayBefore();
					}
					else {
						// sinon, il faut passer par une annulation / recréation
						final T copie = intersectant.duplicate();
						copie.setDateFin(intersection.getDateDebut().getOneDayBefore());
						aAjouter.add(copie);
					}
				}
				if (RegDateHelper.isAfter(intersectant.getDateFin(), intersection.getDateFin(), NullDateBehavior.LATEST)) {
					final T copie = intersectant.duplicate();
					copie.setDateDebut(intersection.getDateFin().getOneDayAfter());
					aAjouter.add(copie);
				}
				if (datePourFermerIntersectant == null) {
					intersectant.setAnnule(true);
				}
				else {
					intersectant.setDateFin(datePourFermerIntersectant);
				}
			}
			return aAjouter;
		}
		else {
			return Collections.emptyList();
		}
	}

	private void makeRoomInMandats(Contribuable mandant, DateRange mandat, MandatKey key) {
		// on regarde dans les mandats de même type (= dont la MandatKey est la même)
		final Set<RapportEntreTiers> rets = mandant.getRapportsSujet();
		final List<RapportEntreTiers> mandatsConnusMemeType = new ArrayList<>(rets.size());
		for (RapportEntreTiers ret : rets) {
			if (ret instanceof Mandat && !ret.isAnnule()) {
				final Mandat candidate = (Mandat) ret;
				final MandatKey retKey = new MandatKey(candidate);
				if (key.equals(retKey)) {
					mandatsConnusMemeType.add(candidate);
				}
			}
		}
		Collections.sort(mandatsConnusMemeType, new DateRangeComparator<>());

		// maintenant, on fait de la place
		final List<RapportEntreTiers> aAjouter = computeNewEntitieForRoomMaking(mandat, mandatsConnusMemeType);
		if (!aAjouter.isEmpty()) {
			// ajout des nouveautés
			for (RapportEntreTiers ret : aAjouter) {
				final RapportEntreTiers saved = hibernateTemplate.merge(ret);
				mandant.getRapportsSujet().add(saved);

				final Tiers mandataire = tiersDAO.get(saved.getObjetId());
				mandataire.getRapportsObjet().add(saved);
			}
		}
	}

	private static void makeRoomInAdressesMandataires(Contribuable mandant, DateRange mandat, MandatKey key) {
		// on regarde les adresses mandataires de même type
		final Set<AdresseMandataire> toutes = mandant.getAdressesMandataires();
		final List<AdresseMandataire> connuesMemeType = new ArrayList<>(toutes.size());
		for (AdresseMandataire am : toutes) {
			if (!am.isAnnule()) {
				final MandatKey candidateKey = new MandatKey(am);
				if (key.equals(candidateKey)) {
					connuesMemeType.add(am);
				}
			}
		}
		Collections.sort(connuesMemeType, new DateRangeComparator<>());

		// maintenant, on fait de la place
		final List<AdresseMandataire> aAjouter = computeNewEntitieForRoomMaking(mandat, connuesMemeType);
		if (!aAjouter.isEmpty()) {
			// ajout des nouveautés
			for (AdresseMandataire am : aAjouter) {
				mandant.addAdresseMandataire(am);
			}
		}
	}

	@Override
	public void addMandat(Contribuable mandant, Mandat mandat) {
		if (mandat.getId() != null) {
			throw new IllegalArgumentException("Mandat déjà persisté interdit ici...");
		}
		if (mandat.getSujetId() != null && !Objects.equals(mandat.getSujetId(), mandant.getNumero())) {
			throw new IllegalArgumentException("Mandat assigné au mandant " + mandat.getSujetId() + " alors qu'il est demandé pour le mandant " + mandant.getNumero());
		}

		final MandatKey key = new MandatKey(mandat);
		makeRoomInMandats(mandant, mandat, key);
		makeRoomInAdressesMandataires(mandant, mandat, key);

		final Tiers mandataire = tiersDAO.get(mandat.getObjetId());
		addRapport(mandat, mandant, mandataire);
	}

	@Override
	public void addMandat(Contribuable mandant, AdresseMandataire mandat) {
		if (mandat.getId() != null) {
			throw new IllegalArgumentException("Adresse mandataire déjà persistée interdite ici...");
		}
		if (mandat.getMandant() != null && mandat.getMandant() != mandant) {
			throw new IllegalArgumentException("Adresse mandantaire assignée au mandant " + mandat.getMandant().getNumero() + " alors qu'elle est demandée pour le mandant " + mandant.getNumero());
		}

		final MandatKey key = new MandatKey(mandat);
		makeRoomInMandats(mandant, mandat, key);
		makeRoomInAdressesMandataires(mandant, mandat, key);
		mandant.addAdresseMandataire(mandat);
	}

	@Override
	public List<DateRange> getPeriodesNonSocieteDePersonnesNiIndividuelle(Entreprise entreprise) {
		final Organisation organisation = getOrganisation(entreprise);
		final List<DateRange> brutto = new LinkedList<>();
		if (organisation != null) {
			final Set<FormeLegale> sp = EnumSet.of(FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF, FormeLegale.N_0104_SOCIETE_EN_COMMANDITE, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE);

			// connue au civil -> les données civiles reignent en maître
			final List<DateRanged<FormeLegale>> all = organisation.getFormeLegale();
			for (DateRanged<FormeLegale> data : all) {
				if (!sp.contains(data.getPayload())) {
					brutto.add(data);
				}
			}
		}
		else {
			final Set<FormeJuridiqueEntreprise> sp = EnumSet.of(FormeJuridiqueEntreprise.SC, FormeJuridiqueEntreprise.SNC);

			// inconnue au civil, ce sont donc nos données fiscales qui font foi
			final List<FormeJuridiqueFiscaleEntreprise> all = entreprise.getFormesJuridiquesNonAnnuleesTriees();
			for (FormeJuridiqueFiscaleEntreprise data : all) {
				if (!sp.contains(data.getFormeJuridique())) {
					brutto.add(data);
				}
			}
		}
		final List<DateRange> res = DateRangeHelper.collateRange(brutto);
		return res.isEmpty() ? Collections.emptyList() : brutto;
	}

	@Override
	public Organisation getOrganisation(@NotNull Entreprise entreprise) {
		// inconnue au registre civil, pas difficile...
		if (!entreprise.isConnueAuCivil()) {
			return null;
		}

		final long numeroOrganisation = entreprise.getNumeroEntreprise();
		final Organisation organisationHistory = serviceOrganisationService.getOrganisationHistory(numeroOrganisation);
		if (organisationHistory == null) {
			// Sérieux problème d'appariement avec le registre civil.
			throw new OrganisationNotFoundException(entreprise);
		}
		return organisationHistory;
	}

	@Override
	public Organisation getOrganisationPourEtablissement(@NotNull Etablissement etablissement) {

		// inconnue au registre civil, pas difficile...
		if (!etablissement.isConnuAuCivil()) {
			return null;
		}

		final long numeroSIteOrganisation = etablissement.getNumeroEtablissement();
		return serviceOrganisationService.getOrganisationHistory(serviceOrganisationService.getOrganisationPourSite(numeroSIteOrganisation));
	}

	@Override
	public SiteOrganisation getSiteOrganisationPourEtablissement(@NotNull Etablissement etablissement) {
		Organisation organisation = getOrganisationPourEtablissement(etablissement);
		if (organisation != null) {
			final SiteOrganisation siteForNo = organisation.getSiteForNo(etablissement.getNumeroEtablissement());
			if (siteForNo == null) {
				// Sérieux problème d'appariement avec le registre civil.
				throw new SiteOrganisationNotFoundException(etablissement);
			}
			Assert.isEqual(etablissement.getNumeroEtablissement(), siteForNo.getNumeroSite());

			return siteForNo;
		}
		return null;
	}

	@Override
	public RegDate getDateCreation(@NotNull Entreprise entreprise) {
		// la date d'inscription RC (en provenance du civil)
		final Organisation organisation = getOrganisation(entreprise);
		if (organisation != null) {
			final RegDate inscriptionRC = organisation.getSitePrincipal(null).getPayload().getDateInscriptionRC(null);
			if (inscriptionRC != null) {
				return inscriptionRC;
			}
		}

		// pas de date d'inscription RC en provenance des données civiles... voyons si on trouve quelque chose dans les états de l'entreprise
		final List<EtatEntreprise> etats = entreprise.getEtatsNonAnnulesTries();
		if (etats != null && !etats.isEmpty()) {
			final Set<TypeEtatEntreprise> etatsInteressants = EnumSet.of(TypeEtatEntreprise.FONDEE, TypeEtatEntreprise.INSCRITE_RC);
			for (EtatEntreprise etat : etats) {
				if (etatsInteressants.contains(etat.getType())) {
					return etat.getDateObtention();
				}
			}
		}

		// aucune date connue -> null
		return null;
	}

	@Nullable
	@Override
	public String getNumeroIDE(@NotNull Entreprise entreprise) {
		if (entreprise.isConnueAuCivil()) {
			final Organisation org = getOrganisation(entreprise);
			final List<DateRanged<String>> liste = org.getNumeroIDE();
			if (liste != null && ! liste.isEmpty()) {
				Collections.sort(liste, new DateRangeComparator<>());
				DateRanged<String> last = CollectionsUtils.getLastElement(liste);
				if (last != null) {
					return last.getPayload();
				}
			}
		} else {
			Set<IdentificationEntreprise> identificationEntreprises = entreprise.getIdentificationsEntreprise();
			if (identificationEntreprises != null && !identificationEntreprises.isEmpty()) {
				List<IdentificationEntreprise> ident = AnnulableHelper.sansElementsAnnules(identificationEntreprises);

				Collections.sort(ident, Comparator.comparing(IdentificationEntreprise::getLogCreationDate));
				return CollectionsUtils.getLastElement(ident).getNumeroIde();
			}
		}
		return null;
	}

	@Nullable
	@Override
	public String getNumeroIDE(@NotNull Etablissement etablissement) {
		final SiteOrganisation site = getSiteOrganisationPourEtablissement(etablissement);
		if (site != null) {
			final List<DateRanged<String>> liste = site.getNumeroIDE();
			if (liste != null && ! liste.isEmpty()) {
				Collections.sort(liste, new DateRangeComparator<>());
				DateRanged<String> last = CollectionsUtils.getLastElement(liste);
				if (last != null) {
					return last.getPayload();
				}
			}
		}
		Set<IdentificationEntreprise> identificationEntreprises = etablissement.getIdentificationsEntreprise();
		if (identificationEntreprises != null && ! identificationEntreprises.isEmpty()) {
			List<IdentificationEntreprise> ident = new ArrayList<>(identificationEntreprises);
			Collections.sort(ident, Comparator.comparing(IdentificationEntreprise::getLogCreationDate));
			return CollectionsUtils.getLastElement(ident).getNumeroIde();
		}
		return null;
	}

	@Override
	public List<CapitalHisto> getCapitaux(@NotNull Entreprise entreprise, boolean aussiAnnules) {
		final List<CapitalHisto> donneesCiviles = extractCapitauxCivils(entreprise);
		final List<CapitalHisto> donneesFiscales = extractCapitauxFiscaux(entreprise, aussiAnnules);

		return surchargeDonneesHisto(donneesCiviles, donneesFiscales);
	}

	@NotNull
	private List<CapitalHisto> extractCapitauxCivils(@NotNull Entreprise entreprise) {
		final Organisation org = getOrganisation(entreprise);
		if (org != null) {
			final List<Capital> capitaux = org.getCapitaux();
			if (capitaux != null && !capitaux.isEmpty()) {
				final List<CapitalHisto> liste = new ArrayList<>(capitaux.size());
				for (Capital capital : capitaux) {
					liste.add(new CapitalHisto(capital));
				}
				return DateRangeHelper.collate(liste);
			}
		}
		return Collections.emptyList();
	}

	@NotNull
	private List<CapitalHisto> extractCapitauxFiscaux(@NotNull Entreprise entreprise, boolean aussiAnnules) {
		final List<CapitalFiscalEntreprise> capitaux;
		if (aussiAnnules) {
			capitaux = entreprise.getCapitauxTries();
		} else {
			capitaux = entreprise.getCapitauxNonAnnulesTries();
		}
		if (capitaux.isEmpty()) {
			return Collections.emptyList();
		}
		final List<CapitalHisto> liste = new ArrayList<>(capitaux.size());
		for (CapitalFiscalEntreprise capital : capitaux) {
			liste.add(new CapitalHisto(capital));
		}
		return liste;
	}

	@NotNull
	public CategorieEntreprise getCategorieEntreprise(@NotNull Entreprise entreprise, @Nullable RegDate date) {
		final RegDate dateEffective = date == null ? RegDate.get() : date;
		final TypeRegimeFiscal currentTypeRegimeFiscal = serviceRegimeFiscal.getTypeRegimeFiscalVD(entreprise, dateEffective);

		/*
			La catégorie est portée par le type de régime fiscal en vigueur.
		 */
		if (currentTypeRegimeFiscal != null) {
			return currentTypeRegimeFiscal.getCategorie();
		}
		/*
			On considère l'absence de régime fiscal de manière analogue à un régime fiscal "A déterminer/Indéterminé", et on retourne la catégorie correspondante.
		 */
		else {
			return CategorieEntreprise.INDET;
		}
	}

	@Override
	public List<FormeLegaleHisto> getFormesLegales(@NotNull Entreprise entreprise, boolean aussiAnnulees) {
		final List<FormeLegaleHisto> donneesCiviles = extractFormesLegalesCiviles(entreprise);
		final List<FormeLegaleHisto> donneesFiscales = extractFormesLegalesFiscales(entreprise, aussiAnnulees);

		return surchargeDonneesHisto(donneesCiviles, donneesFiscales);
	}

	private List<FormeLegaleHisto> extractFormesLegalesCiviles(Entreprise entreprise) {
		final Long numeroEntreprise = entreprise.getNumeroEntreprise();

		if (numeroEntreprise != null) {
			final Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroEntreprise);
			final List<FormeLegaleHisto> formes = new ArrayList<>();
			final List<DateRanged<FormeLegale>> formeLegalesCiviles = organisation.getFormeLegale();
			if (formeLegalesCiviles == null) {
				return Collections.emptyList();
			}
			for (DateRanged<FormeLegale> formeLegale: formeLegalesCiviles) {
				formes.add(new FormeLegaleHisto(formeLegale));
			}
			Collections.sort(formes, new DateRangeComparator<>());
			return formes;
		}
		return Collections.emptyList();
	}


	private List<FormeLegaleHisto> extractFormesLegalesFiscales(Entreprise entreprise, boolean aussiAnnulees) {
		final List<FormeJuridiqueFiscaleEntreprise> fjs;
		if (aussiAnnulees) {
			fjs = entreprise.getFormesJuridiquesTriees();
		} else {
			fjs = entreprise.getFormesJuridiquesNonAnnuleesTriees();
		}
		if (fjs.isEmpty()) {
			return Collections.emptyList();
		}
		final List<FormeLegaleHisto> histo = new ArrayList<>(fjs.size());
		for (FormeJuridiqueFiscaleEntreprise fj : fjs) {
			histo.add(new FormeLegaleHisto(fj));
		}
		return histo;
	}

	@Override
	public List<RaisonSocialeHisto> getRaisonsSociales(@NotNull Entreprise entreprise, boolean aussiAnnulees) {
		final List<RaisonSocialeHisto> donneesCiviles = extractRaisonsSocialesCiviles(entreprise);
		final List<RaisonSocialeHisto> donneesFiscales = extractRaisonsSocialesFiscales(entreprise, aussiAnnulees);

		return surchargeDonneesHisto(donneesCiviles, donneesFiscales);
	}

	/**
	 * Surcharge les données civiles avec les données fiscales, en prenant soin de mettre les éventuelles données annulées de côté
	 * et de appondre à la suite des données non-annulées.
	 */
	private static <T extends DateRange & Annulable & Duplicable<T> & Rerangeable<T>> List<T> surchargeDonneesHisto(List<T> donneesCiviles, List<T> donneesFiscales) {
		List<T> nonAnnulees = new ArrayList<>();
		List<T> annulees = new ArrayList<>();
		for (T raison : donneesFiscales) {
			if (raison.isAnnule()) {
				annulees.add(raison);
			} else {
				nonAnnulees.add(raison);
			}
		}

		final List<T> result = new ArrayList<>(); // Nécessaire car la collection retournée sera en lecture seule si elle est vide.
		result.addAll(DateRangeHelper.override(donneesCiviles, nonAnnulees, new GentilDateRangeExtendedAdapterCallback<>()));
		result.addAll(annulees);
		return result;
	}

	private List<RaisonSocialeHisto> extractRaisonsSocialesCiviles(Entreprise entreprise) {
		Long numeroEntreprise = entreprise.getNumeroEntreprise();

		if (numeroEntreprise != null) {
			final Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroEntreprise);
			final List<RaisonSocialeHisto> raisonsSociales = new ArrayList<>();
			final List<DateRanged<String>> nomsCivils = organisation.getNom();
			if (nomsCivils == null) {
				return Collections.emptyList();
			}
			for (DateRanged<String> raisonSociale : nomsCivils) {
				raisonsSociales.add(new RaisonSocialeHisto(raisonSociale));
			}
			Collections.sort(raisonsSociales, new DateRangeComparator<>());
			return raisonsSociales;
		}
		return Collections.emptyList();
	}


	private List<RaisonSocialeHisto> extractRaisonsSocialesFiscales(Entreprise entreprise, boolean aussiAnnulees) {
		final List<RaisonSocialeFiscaleEntreprise> rss;
		if (aussiAnnulees) {
			rss = entreprise.getRaisonsSocialesTriees();
		} else {
			rss = entreprise.getRaisonsSocialesNonAnnuleesTriees();
		}
		if (rss.isEmpty()) {
			return Collections.emptyList();
		}
		final List<RaisonSocialeHisto> histo = new ArrayList<>(rss.size());
		for (RaisonSocialeFiscaleEntreprise rs : rss) {
			histo.add(new RaisonSocialeHisto(rs));
		}
		return histo;
	}

	@Override
	public List<DomicileHisto> getSieges(@NotNull Entreprise entreprise, boolean aussiAnnules) {
		final List<DomicileHisto> donneesCiviles = extractSiegesCiviles(entreprise);
		final List<DomicileHisto> donneesFiscales = extractSiegesFiscaux(entreprise, aussiAnnules);

		return surchargeDonneesHisto(donneesCiviles, donneesFiscales);
	}

	private List<DomicileHisto> extractSiegesCiviles(Entreprise entreprise) {
		final Long numeroEntreprise = entreprise.getNumeroEntreprise();

		if (numeroEntreprise != null) {
			final Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroEntreprise);
			final List<DomicileHisto> sieges = new ArrayList<>();
			for (Domicile siege: organisation.getSiegesPrincipaux()) {
				sieges.add(new DomicileHisto(siege));
			}
			Collections.sort(sieges, new DateRangeComparator<>());
			return sieges;
		}
		return Collections.emptyList();
	}

	private List<DomicileHisto> extractSiegesFiscaux(Entreprise entreprise, boolean aussiAnnules) {
		final List<DateRanged<Etablissement>> principaux = getEtablissementsPrincipauxEntreprise(entreprise);
		final List<DomicileEtablissement> domicileEtablissements = extractDomicileFromEtablissements(principaux, aussiAnnules);
		final List<DomicileHisto> liste = new ArrayList<>(domicileEtablissements.size());
		for (DomicileEtablissement domicile: domicileEtablissements) {
			liste.add(new DomicileHisto(domicile));
		}
		Collections.sort(liste, new DateRangeComparator<>());
		return liste;
	}

	private List<DomicileEtablissement> extractDomicileFromEtablissements(List<DateRanged<Etablissement>> principaux, boolean aussiAnnules) {
		if (principaux != null) {
			final List<DomicileEtablissement> domiciles = new ArrayList<>(principaux.size());
			for (DateRanged<Etablissement> principal : principaux) {

				final List<DomicileEtablissement> sortedDomiciles = principal.getPayload().getSortedDomiciles(aussiAnnules);

				List<DomicileEtablissement> nonAnnulees = new ArrayList<>();
				List<DomicileEtablissement> annulees = new ArrayList<>();
				for (DomicileEtablissement domicile : sortedDomiciles) {
					if (domicile.isAnnule()) {
						annulees.add(domicile);
					} else {
						nonAnnulees.add(domicile);
					}
				}

				List<DomicileEtablissement> extractedDomiciles = DateRangeHelper.extract(nonAnnulees,
				                                                                         principal.getDateDebut(),
				                                                                         principal.getDateFin(),
				                                                                         new DateRangeHelper.AdapterCallback<DomicileEtablissement>() {
					                                                                         @Override
					                                                                         public DomicileEtablissement adapt(DomicileEtablissement domicile, RegDate debut, RegDate fin) {
						                                                                         final DomicileEtablissement domicileEtablissement =
								                                                                         new DomicileEtablissement(debut != null ? debut : domicile.getDateDebut(),
								                                                                                                   fin != null ? fin : domicile.getDateFin(),
								                                                                                                   domicile.getTypeAutoriteFiscale(),
								                                                                                                   domicile.getNumeroOfsAutoriteFiscale(),
								                                                                                                   domicile.getEtablissement()
								                                                                         );
						                                                                         domicileEtablissement.setId(domicile.getId());
						                                                                         return domicileEtablissement;
					                                                                         }
				                                                                         });

				domiciles.addAll(extractedDomiciles);
				domiciles.addAll(annulees);
			}
			return domiciles;
		}
		return null;
	}

	@Override
	public List<DomicileHisto> getDomiciles(@NotNull Etablissement etablissement, boolean aussiAnnules) {
		final List<DomicileHisto> donneesCiviles = extractDomicilesCivilsEtablissement(etablissement, false);
		final List<DomicileHisto> donneesFiscales = extractDomicilesFiscauxEtablissement(etablissement, aussiAnnules);

		return surchargeDonneesHisto(donneesCiviles, donneesFiscales);
	}

	@Override
	public List<DomicileHisto> getDomicilesEnActiviteSourceReelle(@NotNull Etablissement etablissement, boolean aussiAnnules) {
		final List<DomicileHisto> donneesCiviles = extractDomicilesCivilsEtablissement(etablissement, true);
		final List<DomicileHisto> donneesFiscales = extractDomicilesIssusDuFiscal(etablissement, aussiAnnules);

		return surchargeDonneesHisto(donneesCiviles, donneesFiscales);
	}

	private List<DomicileHisto> extractDomicilesCivilsEtablissement(Etablissement etablissement, boolean enActivite) {
		SiteOrganisation siteOrganisation = getSiteOrganisationPourEtablissement(etablissement);
		if (siteOrganisation == null) {
			return Collections.emptyList();
		}
		final List<DomicileHisto> domiciles = new ArrayList<>();
		List<Domicile> domicilesCivils;
		if (enActivite) {
			domicilesCivils = siteOrganisation.getDomicilesEnActivite();
		} else {
			domicilesCivils = siteOrganisation.getDomiciles();
		}
		if (domicilesCivils == null) {
			return Collections.emptyList();
		}
		for (Domicile domicile : domicilesCivils) {
			domiciles.add(new DomicileHisto(domicile));
		}
		Collections.sort(domiciles, new DateRangeComparator<>());
		return domiciles;
	}

	private List<DomicileHisto> extractDomicilesFiscauxEtablissement(Etablissement etablissement, boolean aussiAnnules) {
		final Set<DomicileEtablissement> domicileEtablissement = etablissement.getDomiciles();
		if (domicileEtablissement == null) {
			return Collections.emptyList();
		}
		final List<DomicileHisto> domiciles = new ArrayList<>(domicileEtablissement.size());
		for (DomicileEtablissement domicile: domicileEtablissement) {
			if (!domicile.isAnnule() || aussiAnnules) {
				domiciles.add(new DomicileHisto(domicile));
			}
		}
		Collections.sort(domiciles, new DateRangeComparator<>());
		return domiciles;
	}

	private List<DomicileHisto> extractDomicilesIssusDuFiscal(Etablissement etablissement, boolean aussiAnnules) {
		final Set<DomicileEtablissement> domicileEtablissement = etablissement.getDomiciles();
		if (domicileEtablissement == null) {
			return Collections.emptyList();
		}
		final List<DomicileHisto> domiciles = new ArrayList<>(domicileEtablissement.size());
		for (DomicileEtablissement domicile: domicileEtablissement) {
			if (!domicile.isAnnule() || aussiAnnules) {
				/*
					Le seul moyen, actuellement, de savoir si on est en présence d'une surcharge corrective des données civiles, c'est
					de regarder quel utilisateur l'a créé. Une telle surcharge est nécessairement créée pendant le traitement automatique
					et ce dernier n'a pas d'autre occasion de créer des surcharges de domicile.
				 */
				if (domicile.getLogCreationUser() != null && domicile.getLogCreationUser().startsWith(EvenementOrganisationProcessorInternal.EVT_ORGANISATION_PRINCIPAL)) {
					final Commune commune = serviceInfra.getCommuneByNumeroOfs(domicile.getNumeroOfsAutoriteFiscale(), domicile.getDateDebut());
					final Domicile pseudoCivil =
							new Domicile(domicile.getDateDebut(), domicile.getDateFin(), commune);
					domiciles.add(new DomicileHisto(pseudoCivil));
				} else {
					domiciles.add(new DomicileHisto(domicile));

				}
			}
		}
		Collections.sort(domiciles, new DateRangeComparator<>());
		return domiciles;
	}

	@Override
	public boolean isInscriteRC(@NotNull Entreprise entreprise, RegDate dateReference) {
		final Organisation organisation = getOrganisation(entreprise);
		if (organisation != null) {
			return organisation.isInscriteAuRC(dateReference);
		}
		else {
			final List<EtatEntreprise> etats = entreprise.getEtatsNonAnnulesTries();
			for (EtatEntreprise etat : etats) {
				if (etat.getType() == TypeEtatEntreprise.INSCRITE_RC && RegDateHelper.isBeforeOrEqual(etat.getDateObtention(), dateReference, NullDateBehavior.LATEST)) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public boolean hasInscriptionActiveRC(@NotNull Entreprise entreprise, RegDate dateReference) {
		final Organisation organisation = getOrganisation(entreprise);
		if (organisation != null) {
			return organisation.isInscriteAuRC(dateReference) && !organisation.isRadieeDuRC(dateReference);
		}
		else {
			final List<EtatEntreprise> etats = entreprise.getEtatsNonAnnulesTries();
			for (EtatEntreprise etat : CollectionsUtils.revertedOrder(etats)) {
				if (RegDateHelper.isBeforeOrEqual(etat.getDateObtention(), dateReference, NullDateBehavior.LATEST)) {
					if (etat.getType() == TypeEtatEntreprise.INSCRITE_RC) {
						return true;
					}
					else if (etat.getType() == TypeEtatEntreprise.RADIEE_RC) {
						return false;
					}
				}
			}
			return false;
		}
	}

	@Override
	public List<TypeEtatEntreprise> getTransitionsEtatEntrepriseDisponibles(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		List<TypeEtatEntreprise> transitions = new ArrayList<>(transitionEtatEntrepriseService.getTransitionsDisponibles(entreprise, date, generation).keySet());
		Collections.sort(transitions);
		return transitions;
	}

	@Override
	public EtatEntreprise changeEtatEntreprise(TypeEtatEntreprise type, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise etatActuel = entreprise.getEtatActuel();
		if (etatActuel != null && etatActuel.getType() == type) {
			return etatActuel;
		}
		final TransitionEtatEntreprise transition = transitionEtatEntrepriseService.getTransitionVersEtat(type, entreprise, date, generation);
		if (transition == null) {
			throw new IllegalArgumentException(String.format("La transition vers l'état demandé (%s) n'est pas possible. Etat actuel: %s.", type, etatActuel != null ? etatActuel.getType() : null));
		}
		return transition.apply();
	}

	@Override
	public void annuleEtatEntreprise(EtatEntreprise etat) {
		final Entreprise entreprise = etat.getEntreprise();
		final EtatEntreprise actuel = entreprise.getEtatActuel();
		if (actuel != etat) {
				throw new ValidationException(etat, "Seul le dernier état peut être annulé.");
		}
		etat.setAnnule(true);
	}

	@Override
	public void appliqueDonneesCivilesSurPeriode(Entreprise entreprise, DateRange range, RegDate dateValeur) throws TiersException {
		Assert.isTrue(entreprise.isConnueAuCivil());
		Assert.isTrue(range.getDateDebut() != null && range.getDateFin() != null);
		Assert.isTrue(dateValeur != null);

		/* Récupération des données civiles pour la date. */
		final Organisation organisation = serviceOrganisationService.getOrganisationHistory(entreprise.getNumeroEntreprise());
		final DateRanged<SiteOrganisation> sitePrincipal = organisation.getSitePrincipal(dateValeur);
		if (sitePrincipal == null) {
			throw new TiersException(String.format("L'organisation %d n'a pas de sitePrincipal principal à la date demandée %s.", organisation.getNumeroOrganisation(), RegDateHelper
					.dateToDisplayString(dateValeur)));
		}

		final Etablissement etablissementPrincipal = tiersDAO.getEtablissementByNumeroSite(sitePrincipal.getPayload().getNumeroSite());

		final Domicile domicileCivil = organisation.getSiegePrincipal(dateValeur);
		final List<DomicileEtablissement> domicilesFiscaux = etablissementPrincipal.getSortedDomiciles(false);
		Set<DomicileEtablissement> domicilesFiscauxASauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, domicilesFiscaux, "domicile");
		for (DomicileEtablissement domicile : domicilesFiscauxASauver) {
			tiersDAO.addAndSave(etablissementPrincipal, domicile);
		}
		if (domicileCivil != null) {
			tiersDAO.addAndSave(etablissementPrincipal, new DomicileEtablissement(range.getDateDebut(), range.getDateFin(), domicileCivil.getTypeAutoriteFiscale(), domicileCivil.getNumeroOfsAutoriteFiscale(), etablissementPrincipal));
		}

		final String raisonSocialeCivile = organisation.getNom(dateValeur);
		final List<RaisonSocialeFiscaleEntreprise> raisonsSocialesFiscales = entreprise.getRaisonsSocialesNonAnnuleesTriees();
		Set<RaisonSocialeFiscaleEntreprise> raisonsSocialesASauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, raisonsSocialesFiscales, "raison sociale");
		for (RaisonSocialeFiscaleEntreprise raisonSociale : raisonsSocialesASauver) {
			tiersDAO.addAndSave(entreprise, raisonSociale);
		}
		if (raisonSocialeCivile != null) {
			tiersDAO.addAndSave(entreprise, new RaisonSocialeFiscaleEntreprise(range.getDateDebut(), range.getDateFin(), raisonSocialeCivile));
		}

		final FormeLegale formeLegaleCivile = organisation.getFormeLegale(dateValeur);
		final List<FormeJuridiqueFiscaleEntreprise> formesJuridiquesFiscales = entreprise.getFormesJuridiquesNonAnnuleesTriees();
		Set<FormeJuridiqueFiscaleEntreprise> formesJuridiquesASauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, formesJuridiquesFiscales, "forme juridique");
		for (FormeJuridiqueFiscaleEntreprise formeJuridique : formesJuridiquesASauver) {
			tiersDAO.addAndSave(entreprise, formeJuridique);
		}
		if (formeLegaleCivile != null) {
			tiersDAO.addAndSave(entreprise, new FormeJuridiqueFiscaleEntreprise(range.getDateDebut(), range.getDateFin(), FormeJuridiqueEntreprise.fromCode(formeLegaleCivile.getCode())));
		}

		final Capital capitalCivil = organisation.getCapital(dateValeur);
		final List<CapitalFiscalEntreprise> capitaux = entreprise.getCapitauxNonAnnulesTries();
		Set<CapitalFiscalEntreprise> capitauxASauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, capitaux, "capital");
		for (CapitalFiscalEntreprise capitalFiscal : capitauxASauver) {
			tiersDAO.addAndSave(entreprise, capitalFiscal);
		}
		if (capitalCivil != null) {
			tiersDAO.addAndSave(entreprise,
			                    new CapitalFiscalEntreprise(range.getDateDebut(), range.getDateFin(), new MontantMonetaire(capitalCivil.getCapitalLibere().longValue(), capitalCivil.getDevise())));
		}
	}

	@Override
	public void appliqueDonneesCivilesSurPeriode(Etablissement etablissement, DateRange range, RegDate dateValeur) throws TiersException {
		Assert.isTrue(etablissement.isConnuAuCivil());
		Assert.isTrue(range.getDateDebut() != null && range.getDateFin() != null);
		Assert.isTrue(dateValeur != null);

		/* Récupération des données civiles pour la date. */
		final Long noOrganisationPourSite = serviceOrganisationService.getOrganisationPourSite(etablissement.getNumeroEtablissement());
		final Organisation organisation = serviceOrganisationService.getOrganisationHistory(noOrganisationPourSite);
		final SiteOrganisation site = organisation.getSiteForNo(etablissement.getNumeroEtablissement());
		if (site == null) {
			throw new TiersException(String.format("Le site %d n'a pas été trouvé!", etablissement.getNumeroEtablissement()));
		}

		final Domicile domicileCivil = site.getDomicile(dateValeur);
		final List<DomicileEtablissement> domicilesFiscaux = etablissement.getSortedDomiciles(false);
		Set<DomicileEtablissement> domicilesFiscauxASauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, domicilesFiscaux, "domicile");
		for (DomicileEtablissement domicile : domicilesFiscauxASauver) {
			tiersDAO.addAndSave(etablissement, domicile);
		}
		tiersDAO.addAndSave(etablissement, new DomicileEtablissement(range.getDateDebut(), range.getDateFin(), domicileCivil.getTypeAutoriteFiscale(), domicileCivil.getNumeroOfsAutoriteFiscale(), etablissement));
	}

	@Override
	public Entreprise getEntreprise(Etablissement etablissement, RegDate date) {
		final RegDate notreDate = date == null ? RegDate.get() : date;

		final RapportEntreTiers rapportEntreTiers = etablissement.getRapportObjetValidAt(notreDate, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);

		return rapportEntreTiers == null ? null : (Entreprise) getTiers(rapportEntreTiers.getSujetId());
	}

	@Override
	public Etablissement getEtablissementPrincipal(Entreprise entreprise, RegDate date) {
		final RegDate notreDate = date == null ? RegDate.get() : date;

		final RapportEntreTiers rapportEntreTiers = entreprise.getActiviteEconomiquePrincipaleValidAt(notreDate);
		if (rapportEntreTiers == null) {
			return null;
		}
		return (Etablissement) getTiers(rapportEntreTiers.getObjetId());
	}

	@Override
	public void apparier(Entreprise entreprise, Organisation organisation, boolean fermerSurcharges) {
		final List<DateRanged<FormeLegale>> formesLegales = organisation.getFormeLegale();
		final RegDate debutCivil = formesLegales.isEmpty() ? RegDate.get() : formesLegales.get(0).getDateDebut();
		final RegDate finFiscale = debutCivil.getOneDayBefore();

		// mise à jour du numéro cantonal
		entreprise.setNumeroEntreprise(organisation.getNumeroOrganisation());
		entreprise.setIdentificationsEntreprise(null); // L'identifiant IDE est dès lors fourni par RCEnt.

		if (fermerSurcharges) {
			fermeSurchargesCiviles(entreprise, finFiscale);
		}
	}

	@Override
	public void fermeSurchargesCiviles(Entreprise entreprise, RegDate finFiscale) {
		// on stoppe les raisons sociales fiscales à la veille de la date de début, si celle-ci a un sens...
		final List<RaisonSocialeFiscaleEntreprise> raisonSocialeFiscaleEntreprises = entreprise.getRaisonsSocialesNonAnnuleesTriees();
		for (RaisonSocialeFiscaleEntreprise raisonSocialeFiscaleEntreprise : raisonSocialeFiscaleEntreprises) {
			if (RegDateHelper.isBefore(finFiscale, raisonSocialeFiscaleEntreprise.getDateDebut(), NullDateBehavior.EARLIEST)) {
				raisonSocialeFiscaleEntreprise.setAnnule(true);
			}
			else if (raisonSocialeFiscaleEntreprise.getDateFin() == null) {
				raisonSocialeFiscaleEntreprise.setDateFin(finFiscale);
			}
			else if (RegDateHelper.isBefore(finFiscale, raisonSocialeFiscaleEntreprise.getDateFin(), NullDateBehavior.LATEST)) {
				final RaisonSocialeFiscaleEntreprise nouvelleRaisonSociale = raisonSocialeFiscaleEntreprise.duplicate();
				raisonSocialeFiscaleEntreprise.setAnnule(true);
				nouvelleRaisonSociale.setDateFin(finFiscale);
				entreprise.addDonneeCivile(nouvelleRaisonSociale);
			}
		}
		// on stoppe les formesLegales fiscales à la veille de la date de début, si celle-ci a un sens...
		final List<FormeJuridiqueFiscaleEntreprise> formeJuridiqueFiscaleEntreprises = entreprise.getFormesJuridiquesNonAnnuleesTriees();
		for (FormeJuridiqueFiscaleEntreprise formeJuridiqueFiscaleEntreprise : formeJuridiqueFiscaleEntreprises) {
			if (RegDateHelper.isBefore(finFiscale, formeJuridiqueFiscaleEntreprise.getDateDebut(), NullDateBehavior.EARLIEST)) {
				formeJuridiqueFiscaleEntreprise.setAnnule(true);
			}
			else if (formeJuridiqueFiscaleEntreprise.getDateFin() == null) {
				formeJuridiqueFiscaleEntreprise.setDateFin(finFiscale);
			}
			else if (RegDateHelper.isBefore(finFiscale, formeJuridiqueFiscaleEntreprise.getDateFin(), NullDateBehavior.LATEST)) {
				final FormeJuridiqueFiscaleEntreprise nouvelleFormeJuridique = formeJuridiqueFiscaleEntreprise.duplicate();
				formeJuridiqueFiscaleEntreprise.setAnnule(true);
				nouvelleFormeJuridique.setDateFin(finFiscale);
				entreprise.addDonneeCivile(nouvelleFormeJuridique);
			}
		}
		// on stoppe les formesLegales fiscaux à la veille de la date de début, si celle-ci a un sens...
		final List<CapitalFiscalEntreprise> capitalFiscalEntreprises = entreprise.getCapitauxNonAnnulesTries();
		for (CapitalFiscalEntreprise capitalFiscalEntreprise : capitalFiscalEntreprises) {
			if (RegDateHelper.isBefore(finFiscale, capitalFiscalEntreprise.getDateDebut(), NullDateBehavior.EARLIEST)) {
				capitalFiscalEntreprise.setAnnule(true);
			}
			else if (capitalFiscalEntreprise.getDateFin() == null) {
				capitalFiscalEntreprise.setDateFin(finFiscale);
			}
			else if (RegDateHelper.isBefore(finFiscale, capitalFiscalEntreprise.getDateFin(), NullDateBehavior.LATEST)) {
				final CapitalFiscalEntreprise nouvelleRaisonSociale = capitalFiscalEntreprise.duplicate();
				capitalFiscalEntreprise.setAnnule(true);
				nouvelleRaisonSociale.setDateFin(finFiscale);
				entreprise.addDonneeCivile(nouvelleRaisonSociale);
			}
		}
	}

	@Override
	public void apparier(Etablissement etablissement, SiteOrganisation site) {
		final List<Domicile> domiciles = site.getDomicilesEnActivite();
		final RegDate debutCivil = domiciles.isEmpty() ? RegDate.get() : domiciles.get(0).getDateDebut();
		final RegDate finFiscale = debutCivil.getOneDayBefore();

		// mise à jour du numéro cantonal
		etablissement.setNumeroEtablissement(site.getNumeroSite());
		etablissement.setIdentificationsEntreprise(null); // L'identifiant IDE est dès lors fourni par RCEnt.
		etablissement.setRaisonSociale(null);       // [SIFISC-22336] la raison sociale est maintenant fournie par RCEnt

		fermeSurchargesCiviles(etablissement, finFiscale);
	}

	@Override
	public void fermeSurchargesCiviles(Etablissement etablissement, RegDate finFiscale) {
		// on stoppe les domiciles fiscaux à la veille de la date de début, si celle-ci a un sens...
		final List<DomicileEtablissement> domicilesFiscaux = etablissement.getSortedDomiciles(false);
		for (DomicileEtablissement domicileFiscal : domicilesFiscaux) {
			if (RegDateHelper.isBefore(finFiscale, domicileFiscal.getDateDebut(), NullDateBehavior.EARLIEST)) {
				domicileFiscal.setAnnule(true);
			}
			else if (domicileFiscal.getDateFin() == null) {
				domicileFiscal.setDateFin(finFiscale);
			}
			else if (RegDateHelper.isBefore(finFiscale, domicileFiscal.getDateFin(), NullDateBehavior.LATEST)) {
				final DomicileEtablissement nouveauDomicile = domicileFiscal.duplicate();
				domicileFiscal.setAnnule(true);
				nouveauDomicile.setDateFin(finFiscale);
				etablissement.addDomicile(nouveauDomicile);
			}
		}
	}

	@Override
	@NotNull
	public DegreAssociationRegistreCivil determineDegreAssociationCivil(Entreprise entreprise, RegDate date) {
		if (!entreprise.isConnueAuCivil()) {
			return DegreAssociationRegistreCivil.FISCAL;
		}
		if (serviceIDEService.isServiceIDEObligEtendues(entreprise, date)) {
			return DegreAssociationRegistreCivil.CIVIL_MAITRE;
		} else {
			return DegreAssociationRegistreCivil.CIVIL_ESCLAVE;
		}
	}

	@Override
	@NotNull
	public DegreAssociationRegistreCivil determineDegreAssociationCivil(Etablissement etablissement, RegDate date) {
		/*
			Si on n'est pas connu au civil, notre quête s'arrête là
		 */
		if (!etablissement.isConnuAuCivil()) {
			return DegreAssociationRegistreCivil.FISCAL;
		}

		/*
			A partir d'ici, on est connu au civil
		 */

		final ActiviteEconomique rapport = (ActiviteEconomique) etablissement.getRapportObjetValidAt(date, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
		/*
			S'il n'y a pas d'entreprise, on n'est pas principal et la règle de base s'applique
		 */
		if (rapport == null) {
			return DegreAssociationRegistreCivil.CIVIL_ESCLAVE;
		}

		/*
			Les établissements secondaires, non gérés, ne sont pas pris en considération
		 */
		if (!rapport.isPrincipal()) {
			return DegreAssociationRegistreCivil.CIVIL_ESCLAVE;
		}
		Entreprise entreprise = (Entreprise) getTiers(rapport.getSujetId());
		if (entreprise == null) {
			throw new TiersNotFoundException();
		}
		/*
			Déterminer si Unireg est un service IDE à obligations étendues.
		 */
		if (serviceIDEService.isServiceIDEObligEtendues(entreprise, date)) {
			return DegreAssociationRegistreCivil.CIVIL_MAITRE;
		} else {
			return DegreAssociationRegistreCivil.CIVIL_ESCLAVE;
		}
	}
}

package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.common.TicketTimeoutException;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationGenerationOperation;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscalePP;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

public class EnvoiDIsPPEnMasseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiDIsPPEnMasseProcessor.class);

	private final TiersService tiersService;

	private final HibernateTemplate hibernateTemplate;

	private final PeriodeFiscaleDAO periodeDAO;

	private final ModeleDocumentDAO modeleDAO;

	private final DelaisService delaisService;

	private final DeclarationImpotService diService;

	private final PlatformTransactionManager transactionManager;

	private final ParametreAppService parametreService;

	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;

	private final AdresseService adresseService;

	private final TicketService ticketService;

	private final int tailleLot;
	private RegDate dateExclusionDecedes;

	protected static class Cache {
		public final CollectiviteAdministrative cedi;
		public final CollectiviteAdministrative aci;
		public final PeriodeFiscale periode;
		public final ModeleDocument modele;

		public Cache(CollectiviteAdministrative cedi, CollectiviteAdministrative aci, ModeleDocument modele, PeriodeFiscale periode) {
			this.cedi = cedi;
			this.modele = modele;
			this.periode = periode;
			this.aci = aci;
		}
	}

	public EnvoiDIsPPEnMasseProcessor(TiersService tiersService, HibernateTemplate hibernateTemplate, ModeleDocumentDAO modeleDAO,
	                                  PeriodeFiscaleDAO periodeDAO, DelaisService delaisService, DeclarationImpotService diService, int tailleLot,
	                                  PlatformTransactionManager transactionManager, ParametreAppService parametreService,
	                                  ServiceCivilCacheWarmer serviceCivilCacheWarmer, AdresseService adresseService, TicketService ticketService) {
		this.tiersService = tiersService;
		this.hibernateTemplate = hibernateTemplate;
		this.modeleDAO = modeleDAO;
		this.periodeDAO = periodeDAO;
		this.tailleLot = tailleLot;
		this.delaisService = delaisService;
		this.diService = diService;
		this.transactionManager = transactionManager;
		this.parametreService = parametreService;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.adresseService = adresseService;
		this.ticketService = ticketService;
		this.dateExclusionDecedes = null;
		Assert.isTrue(tailleLot > 0);
	}

	public EnvoiDIsPPResults run(final int anneePeriode, final CategorieEnvoiDI categorie, @Nullable final Long noCtbMin, @Nullable final Long noCtbMax, final int nbMax,
	                           final RegDate dateTraitement, final boolean exclureDecedes, final int nbThreads, @Nullable StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		if (exclureDecedes) {
			dateExclusionDecedes = getDateDebutExclusion(anneePeriode);
		}
		final EnvoiDIsPPResults rapportFinal = new EnvoiDIsPPResults(anneePeriode, categorie, dateTraitement, nbMax, noCtbMin, noCtbMax, dateExclusionDecedes, nbThreads, tiersService, adresseService);

		// certains contribuables ne recoivent pas de DI du canton (par exemple les diplomates suisses)
		if (categorie.getTypeDocument() != null) {

			status.setMessage("Récupération des contribuables à traiter...");
			final List<Long> ids = createListOnContribuableIds(anneePeriode, categorie.getTypeContribuable(), categorie.getTypeDocument(), noCtbMin, noCtbMax);

			// Traite les contribuables par lots
			final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
			final ParallelBatchTransactionTemplateWithResults<Long, EnvoiDIsPPResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids, tailleLot, nbThreads, Behavior.REPRISE_AUTOMATIQUE,
			                                                                                                                                      transactionManager, status, AuthenticationInterface.INSTANCE);
			template.execute(rapportFinal, new BatchWithResultsCallback<Long, EnvoiDIsPPResults>() {

				@Override
				public EnvoiDIsPPResults createSubRapport() {
					return new EnvoiDIsPPResults(anneePeriode, categorie, dateTraitement, nbMax, noCtbMin, noCtbMax, dateExclusionDecedes, nbThreads, tiersService, adresseService);
				}

				@Override
				public boolean doInTransaction(List<Long> batch, EnvoiDIsPPResults r) throws Exception {
					status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());

					if (nbMax > 0 && rapportFinal.nbCtbsTotal + batch.size() >= nbMax) {
						// limite le nombre de contribuable pour ne pas dépasser le nombre max
						int reducedSize = nbMax - rapportFinal.nbCtbsTotal;
						batch = batch.subList(0, reducedSize);
					}

					if (!batch.isEmpty()) {
						traiterBatch(batch, r, anneePeriode, categorie, dateTraitement);
					}

					return !rapportFinal.interrompu && (nbMax <= 0 || rapportFinal.nbCtbsTotal + batch.size() < nbMax);
				}
			}, progressMonitor);
		}

		if (status.interrupted()) {
			status.setMessage("L'envoi en masse des déclarations d'impôt a été interrompue."
					+ " Nombre de déclarations envoyées au moment de l'interruption = " + rapportFinal.ctbsAvecDiGeneree.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("L'envoi en masse des déclarations d'impôt est terminée. Nombre de déclarations envoyées = "
					+ rapportFinal.ctbsAvecDiGeneree.size() + ". Nombre d'erreurs = " + rapportFinal.ctbsEnErrors.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite tout le batch des contribuables, un par un.
	 *
	 * @param ids            les ids des contribuables à traiter
	 * @param rapport        le rapport en cours de construction
	 * @param anneePeriode   l'année fiscale considérée
	 * @param categorie      la catégorie de contribuable considérée
	 * @param dateTraitement la date de traitement
	 * @throws DeclarationException en cas d'erreur dans le traitement d'un contribuable.
	 */
	protected void traiterBatch(List<Long> ids, EnvoiDIsPPResults rapport, int anneePeriode, CategorieEnvoiDI categorie, RegDate dateTraitement) throws DeclarationException {

		rapport.nbCtbsTotal += ids.size();

		final Cache cache = initCache(anneePeriode, categorie);
		final DeclarationsCache dcache = new DeclarationsCache(anneePeriode, ids);

		// pré-chauffage du cache des individus du civil
		if (serviceCivilCacheWarmer != null) {
			serviceCivilCacheWarmer.warmIndividusPourTiers(ids, null, true, AttributeIndividu.ADRESSES);
		}

		final Iterator<TacheEnvoiDeclarationImpotPP> iter = createIteratorOnTaches(anneePeriode, categorie.getTypeContribuable(), categorie.getTypeDocument(), ids);
		while (iter.hasNext()) {
			final TacheEnvoiDeclarationImpotPP tache = iter.next();
			traiterTache(tache, dateTraitement, rapport, cache, dcache);
		}

		dcache.clear();
	}

	/**
	 * Initialise les données pré-cachées pour éviter de les recharger plusieurs fois de la base de données.
	 *
	 * @param anneePeriode   l'année fiscale considérée
	 * @param categorie      la catégorie de contribuable considérée
	 * @throws DeclarationException en cas d'erreur dans le traitement d'un contribuable.
	 */
	protected Cache initCache(int anneePeriode, CategorieEnvoiDI categorie) throws DeclarationException {

		// Récupère le CEDI
		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		if (cedi == null) {
			throw new DeclarationException("Impossible de charger le centre d'enregistrement des déclarations d'impôt (CEDI).");
		}

		final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noACI);
		if (aci == null) {
			throw new DeclarationException("Impossible de charger la collectivité administrative de l'administration cantonale des impôts (ACI).");
		}

		// Récupère la période fiscale
		final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriode);
		if (periode == null) {
			throw new DeclarationException("La période fiscale [" + anneePeriode + "] n'existe pas dans la base de données.");
		}

		// Récupère le modèle de document
		final ModeleDocument modele = modeleDAO.getModelePourDeclarationImpotOrdinaire(periode, categorie.getTypeDocument());
		if (modele == null) {
			throw new DeclarationException("Impossible de trouver le modèle de document pour une déclaration d'impôt pour la période ["
					+ periode.getAnnee() + "] et le type de document [" + categorie.getTypeDocument().name() + "].");
		}

		return new Cache(cedi, aci, modele, periode);
	}

	private static final String queryTacheEnvoiEnInstance = // ------------------------------
	"SELECT                                                                                  "
			+ "    tache                                                                     "
			+ "FROM                                                                          "
			+ "    TacheEnvoiDeclarationImpotPP AS tache                                     "
			+ "WHERE                                                                         "
			+ "    tache.etat = 'EN_INSTANCE' AND                                            "
			+ "    tache.annulationDate IS null AND                                          "
			+ "    tache.typeContribuable = :typeContribuable AND                            "
			+ "    tache.typeDocument = :typeDocument AND                                    "
			+ "    tache.contribuable.id in (:ids) AND                                       "
			+ "    tache.dateDebut >= :debutPeriode AND                                      "
			+ "    tache.dateFin <= :finPeriode                                              "
			+ "ORDER BY tache.id DESC                                                        ";

	/**
	 * Crée un iterateur sur les tâches d'envoi des DIs en instance.
	 * <p/>
	 * [UNIREG-1791] on traite les tâches les plus récentes en premier (tache.id DESC)
	 *
	 * @return itérateur sur les tiers
	 */
	protected Iterator<TacheEnvoiDeclarationImpotPP> createIteratorOnTaches(final int annee, final TypeContribuable typeContribuable,
	                                                                      final TypeDocument typeDocument, final List<Long> ids) {

		final RegDate debutAnnee = RegDate.get(annee, 1, 1);
		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		return hibernateTemplate.execute(new HibernateCallback<Iterator<TacheEnvoiDeclarationImpotPP>>() {
			@Override
			public Iterator<TacheEnvoiDeclarationImpotPP> doInHibernate(Session session) throws HibernateException {
				FlushMode mode = session.getFlushMode();
				try {
					// On traite toutes les tâches d'un lot de contribuables à la fois : il ne peut pas y avoir de tâches déjà modifiées
					// concernant les contribuables spécifiés et on peut donc sans risque ne pas flusher la session.
					session.setFlushMode(FlushMode.MANUAL);
					final Query queryObject = session.createQuery(queryTacheEnvoiEnInstance);
					queryObject.setParameter("typeContribuable", typeContribuable);
					queryObject.setParameter("typeDocument", typeDocument);
					queryObject.setParameterList("ids", ids);
					queryObject.setParameter("debutPeriode", debutAnnee);
					queryObject.setParameter("finPeriode", finAnnee);
					//noinspection unchecked
					return queryObject.iterate();
				}
				finally {
					session.setFlushMode(mode);
				}
			}
		});
	}

	/**
	 * Crée un iterateur sur les ids des contribuables possédant des tâches d'envoi des DIs en instance.
	 *
	 * @return itérateur sur les ids des contribuables
	 */
	@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
	protected List<Long> createListOnContribuableIds(final int annee, final TypeContribuable typeContribuable,
	                                              final TypeDocument typeDocument, final Long noCtbMin, final Long noCtbMax) {

		final RegDate debutAnnee = RegDate.get(annee, 1, 1);
		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		
		final List<Long> i = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {

						final StringBuilder builder = new StringBuilder();
						builder.append("SELECT DISTINCT tache.contribuable.id");
						builder.append(" FROM TacheEnvoiDeclarationImpotPP AS tache");
						builder.append(" WHERE");
						builder.append(" tache.etat = 'EN_INSTANCE'");
						builder.append(" AND tache.annulationDate IS NULL");
						builder.append(" AND tache.typeContribuable = :typeContribuable");
						builder.append(" AND tache.typeDocument = :typeDocument");
						builder.append(" AND tache.dateDebut >= :debutPeriode");
						builder.append(" AND tache.dateFin <= :finPeriode");
						if (noCtbMin != null && noCtbMax != null) {
							builder.append(" AND tache.contribuable.id BETWEEN :noCtbMin AND :noCtbMax");
						}
						else if (noCtbMin != null) {
							builder.append(" AND tache.contribuable.id >= :noCtbMin");
						}
						else if (noCtbMax != null) {
							builder.append(" AND tache.contribuable.id <= :noCtbMax");
						}
						builder.append(" ORDER BY tache.contribuable.id ASC");

						final Query queryObject = session.createQuery(builder.toString());
						queryObject.setParameter("typeContribuable", typeContribuable);
						queryObject.setParameter("typeDocument", typeDocument);
						queryObject.setParameter("debutPeriode", debutAnnee);
						queryObject.setParameter("finPeriode", finAnnee);
						if (noCtbMin != null) {
							queryObject.setParameter("noCtbMin", noCtbMin);
						}
						if (noCtbMax != null) {
							queryObject.setParameter("noCtbMax", noCtbMax);
						}
						return queryObject.list();
					}
				});
			}
		});

		return i;
	}

	/**
	 * Traite la tâche d'envoi d'une déclaration spécifiée.
	 *
	 * @return <b>vrai</b> si la tâche a été traitée, <b>faux</b> autrement
	 */
	protected boolean traiterTache(TacheEnvoiDeclarationImpotPP tache, RegDate dateTraitement, AbstractEnvoiDIsPPResults rapport, Cache cache, DeclarationsCache dcache) throws DeclarationException {
		return traiterTache(tache, dateTraitement, rapport, cache, dcache, false);
	}

	protected boolean traiterTache(TacheEnvoiDeclarationImpotPP tache, RegDate dateTraitement, AbstractEnvoiDIsPPResults rapport, Cache cache, DeclarationsCache dcache, boolean simul) throws DeclarationException {

		final Contribuable contribuable = tache.getContribuable();
		if (!(contribuable instanceof ContribuableImpositionPersonnesPhysiques)) {
			return false;
		}

		// Voir le use-case "SCU-ExclureContribuablesEnvoiDI"
		final RegDate dateLimiteExclusion = contribuable.getDateLimiteExclusionEnvoiDeclarationImpot();
		if (dateLimiteExclusion != null && dateTraitement.isBeforeOrEqual(dateLimiteExclusion)) {
			if (rapport != null) {
				rapport.addIgnoreCtbExclu(contribuable, dateLimiteExclusion);
			}
			return false;
		}

		final DeclarationGenerationOperation tickettingKey = new DeclarationGenerationOperation(contribuable.getNumero());
		try {
			final TicketService.Ticket ticket = ticketService.getTicket(tickettingKey, 500);
			try {
				return traiterTache(tache, (ContribuableImpositionPersonnesPhysiques) contribuable, dateTraitement, rapport, cache, dcache, simul);
			}
			finally {
				ticketService.releaseTicket(ticket);
			}
		}
		catch (TicketTimeoutException e) {
			throw new DeclarationException(String.format("Une DI est actuellement déjà en cours d'émission pour le contribuable %d.", contribuable.getNumero()), e);
		}
		catch (InterruptedException e) {
			throw new DeclarationException(e);
		}
	}

	private boolean traiterTache(TacheEnvoiDeclarationImpotPP tache, ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateTraitement, AbstractEnvoiDIsPPResults rapport, Cache cache,
	                             DeclarationsCache dcache, boolean simul) throws DeclarationException {

		final Long numeroCtb = contribuable.getNumero();
		final List<DeclarationImpotOrdinairePP> list = dcache.getDeclarationsInRange(contribuable, tache, false);
		if (!list.isEmpty() && !simul) {

			// Il existe déjà une (ou plusieurs) déclarations
			if (list.size() == 1 && correspondent(list.get(0), tache)) {
				Audit.warn("Il existe déjà une déclaration d'impôt pour la période [" + tache.getDateDebut() + " - " + tache.getDateFin()
						           + "] et le contribuable [" + numeroCtb + "]. Aucune nouvelle déclaration n'est créée" + " et la tâche ["
						           + tache.getId() + "] est considérée comme traitée.");
				tache.setEtat(TypeEtatTache.TRAITE);
				rapport.addIgnoreDIDejaExistante(contribuable, tache.getDateDebut(), tache.getDateFin());
			}
			else {
				String message = "La tâche [id=" + tache.getId() + ", période=" + tache.getDateDebut() + '-' + tache.getDateFin() + "] est en conflit avec " + list.size() +
						" déclaration(s) d'impôt pré-existante(s) sur le contribuable [" + numeroCtb + "]. Aucune nouvelle déclaration n'est créée et la tâche reste en instance.";
				Audit.error(message);
				rapport.addErrorDICollision(contribuable, tache.getDateDebut(), tache.getDateFin(), message);
				return false;
			}
		}
		//UNIREG-1952
		else if (dateExclusionDecedes != null && estDecedeEnFinDannee(contribuable, tache.getDateFin())) {
			//rapport
			rapport.addIgnoreCtbExcluDecede(contribuable);
			return false;
		}
		else {
			// [UNIREG-1852] les contribuables indigents décédés dans l'année doivent être traités comme les autres contribuables.
			final boolean estIndigentNonDecede = (estIndigent(contribuable, tache.getDateFin()) && !estDecede(contribuable, tache.getDateFin()));
			if (estIndigentNonDecede) {
				if (envoyerDIIndigent(tache, dateTraitement, rapport, cache, dcache, simul)) {
					tache.setEtat(TypeEtatTache.TRAITE);
				}
			}
			else {
				if (envoyerDINormal(tache, dateTraitement, rapport, cache, dcache, simul)) {
					tache.setEtat(TypeEtatTache.TRAITE);
				}
			}
		}

		return true;
	}

	/**
	 * Permet de savoir si le contribuable est décédé au 31 décembre de l'année spécifiée par la date
	 *
	 * @param contribuable un contribuable
	 * @param date         la date à partir de laquelle est extraite l'année à tester
	 * @return <b>vrai</b> si le contribuable est décédée; <b>faux</b> autrement.
	 */
	private boolean estDecedeEnFinDannee(Contribuable contribuable, final RegDate date) {
		final ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(date);

		final RegDate dateFinExclusion = RegDate.get(date.year(), 12, 31);
		if (forPrincipal != null) {
			if (RegDateHelper.isBetween(forPrincipal.getDateFin(), dateExclusionDecedes, dateFinExclusion, NullDateBehavior.LATEST)) {
				return (forPrincipal.getMotifFermeture() == MotifFor.VEUVAGE_DECES);
			}
		}
		return false;
	}

	private RegDate getDateDebutExclusion(int anneePeriode) {
		Integer[] momentExclusion = parametreService.getDateExclusionDecedeEnvoiDI();
		return RegDate.get(anneePeriode, momentExclusion[1], momentExclusion[0]);

	}

	/**
	 * Cas spécial indigent : une déclaration d'impôt retournée le jour même est enregistrée dans l'application sans impression.
	 *
	 * @param tache          la tâche à traiter
	 * @param dateTraitement la date d'émission et de retour de la déclaration d'impôt
	 * @param rapport        le rapport en cours de construction
	 * @param cache          le cache local des données de type structurel (cedi, aci,...)
	 * @param dcache         la cache des données qui vont bien
	 * @param simul          <b>vrai</b> s'il s'agit d'une simulation; <b>faux</b> dans le cas normal.
	 * @return <b>vrai</b> si la tâche a été traitée, c'est-à-dire que la déclaration a été envoyée; <b>faux</b> en cas de problème.
	 * @throws ch.vd.uniregctb.declaration.DeclarationException
	 *          en cas d'exception
	 */
	private boolean envoyerDIIndigent(TacheEnvoiDeclarationImpotPP tache, RegDate dateTraitement, AbstractEnvoiDIsPPResults rapport, Cache cache, DeclarationsCache dcache, boolean simul) throws DeclarationException {

		final DeclarationImpotOrdinairePP di = creeDI(tache, rapport, cache, dcache, simul);
		if (di == null) {
			return false;
		}
		di.setRetourCollectiviteAdministrativeId(cache.cedi.getId());

		// [UNIREG-1980] l'état 'émis' doit aussi être présent sur les DIs des indigents
		final RegDate dateExpedition = ajouterEtatInitial(di, dateTraitement, false);

		// [SIFISC-1720] il faut également enregistrer un délai de retour initial (pour l'éventuel duplicata qui serait imprimé plus tard)
		ajouterDelaisDeRetourInitial(di, dateTraitement, dateExpedition);

		// Les déclarations d'indigent sont marquées comme déjà retournées
		diService.quittancementDI(tache.getContribuable(), di, dateExpedition, EtatDeclarationRetournee.SOURCE_INDIGENT, false);

		rapport.addCtbIndigent(tache.getContribuable().getNumero());
		return true;
	}

	/**
	 * Crée une nouvelle déclaration d'impôt sur le tiers
	 */
	protected DeclarationImpotOrdinairePP creeDI(TacheEnvoiDeclarationImpotPP tache, AbstractEnvoiDIsPPResults rapport, Cache cache, DeclarationsCache dcache, boolean simul) throws DeclarationException {

		final ContribuableImpositionPersonnesPhysiques contribuable = (ContribuableImpositionPersonnesPhysiques) tache.getContribuable();
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(contribuable, tache.getDateFin());
		if (forGestion == null) {
			String message = "Impossible de trouver un for de gestion pour le contribuable no [" + contribuable.getNumero()
					+ "] à la date [" + tache.getDateFin() + "].";
			rapport.addErrorForGestionNul(contribuable, tache.getDateDebut(), tache.getDateFin(), message);
			if (!simul) {
				Audit.error(message);
			}
			return null;
		}

		// calcul du nombre de déclarations déjà existantes pour l'année considérée
		final List<DeclarationImpotOrdinairePP> decls = dcache.getDeclarationsInRange(contribuable, dcache.baseRange, true);
		final int nbDecls = decls != null ? decls.size() : 0;

		// [SIFISC-1368] récupération ou génération du code de contrôle
		String codeControle = null;
		if (cache.periode.getAnnee() >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
			if (decls == null) {
				// pas de déclaration : on génère un nouveau code de contrôle
				codeControle = DeclarationImpotOrdinairePP.generateCodeControle();
			}
			else {
				// on recherche un code de contrôle déjà généré sur les déclarations préexistantes de la période
				for (DeclarationImpotOrdinairePP d : decls) {
					if (d.getCodeControle() != null) {
						codeControle = d.getCodeControle();
						break;
					}
				}
				if (codeControle == null) {
					// pas de code déjà généré : on en génère un nouveau
					codeControle = DeclarationImpotOrdinairePP.generateCodeControle();
					// on profite pour assigner le code de contrôle généré à toutes les déclarations préexistantes de la période (= rattrapage de données)
					for (DeclarationImpotOrdinairePP d : decls) {
						d.setCodeControle(codeControle);
					}
				}
			}
		}

		DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.setDateDebut(tache.getDateDebut());
		di.setDateFin(tache.getDateFin());
		di.setPeriode(cache.periode);
		di.setTypeContribuable(tache.getTypeContribuable());
		di.setQualification(tache.getQualification());
		if (cache.periode.getAnnee() >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE && tache.getCodeSegment() == null ) {
			di.setCodeSegment(DeclarationImpotService.VALEUR_DEFAUT_CODE_SEGMENT);
		} else {
			di.setCodeSegment(tache.getCodeSegment());
		}
		di.setModeleDocument(cache.modele);
		di.setNumeroOfsForGestion(forGestion.getNoOfsCommune());
		di.setTiers(contribuable);
		di.setNumero(nbDecls + 1);
		di.setCodeControle(codeControle);

		di = hibernateTemplate.merge(di); // force le save de la DI pour s'assurer qu'elle reçoit un id

		// [UNIREG-1791] On met-à-jour le cache de manière à détecter deux déclarations qui se chevauchent.
		dcache.addDeclaration(di);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Création de la DI #" + di.getId() + " contribuable=[" + di.getTiers().getNumero() + "] periode=["
					+ di.getDateDebut() + ';' + di.getDateFin() + "] type=[" + di.getTypeContribuable() + "] forGestion=["
					+ di.getNumeroOfsForGestion() + ']');
		}

		return di;
	}

	/**
	 * Crée une déclaration d'impôt ordinaire et envoie la à éditique pour impression, selon la tâche en instance spécifiée.
	 *
	 * @param tache          la tâche à traiter
	 * @param dateTraitement la date de traitement officielle de ce job
	 * @param rapport        le rapport en cours de construction
	 * @param cache          le cache local des données de type structurel (cedi, aci,...)
	 * @param dcache         le cache des déclarations
	 * @param simul          true si le batch est appelé en mode simulation ( {@link ProduireListeDIsNonEmisesProcessor} ). Dans ce cas on imprime pas
	 *
	 */
	private boolean envoyerDINormal(TacheEnvoiDeclarationImpotPP tache, RegDate dateTraitement, AbstractEnvoiDIsPPResults rapport, Cache cache, DeclarationsCache dcache, boolean simul) throws DeclarationException {

		final Contribuable ctb = tache.getContribuable();
		if (!simul && ctb.getOfficeImpotId() == null) {
			rapport.addErrorForGestionNul(ctb, null, null, "L'office d'impôt du contribuable n'est pas renseigné");
			return false;
		}
		// Création de la déclaration d'impôt, du délai de retour et de son état
		final DeclarationImpotOrdinairePP di = creeDI(tache, rapport, cache, dcache, simul);
		if (di == null) {
			return false;
		}
		final RegDate dateExpedition = ajouterEtatInitial(di, dateTraitement, true);
		ajouterDelaisDeRetourInitial(di, dateTraitement, dateExpedition);
		ajouterAdresseRetour(di, tache, cache);

		// Impression de la déclaration proprement dites
		if (!simul) {
			imprimerDI(di, dateTraitement);
		}

		rapport.addDeclarationTraitee(ctb.getNumero());
		return true;
	}

	/**
	 * Détermine l'adresse d'envoi et envoie la déclaration spécifiée à éditique pour impression.
	 */
	private void imprimerDI(DeclarationImpotOrdinairePP di, RegDate dateTraitement) throws DeclarationException {

		final Contribuable contribuable = di.getTiers();
		Assert.notNull(contribuable);

		diService.envoiDIForBatch(di, dateTraitement);
	}

	/**
	 * Ajoute les délais accordé et imprimé sur une déclaration d'impôt.
	 *
	 * @param di             une déclaration
	 * @param dateTraitement la date de traitement
	 * @param dateExpedition la date d'expédition calculée (généralement dans le futur)
	 */
	protected void ajouterDelaisDeRetourInitial(DeclarationImpotOrdinairePP di, RegDate dateTraitement, RegDate dateExpedition) {

		final PeriodeFiscale periode = di.getPeriode();
		final Contribuable contribuable = (Contribuable) di.getTiers();
		Assert.notNull(periode);
		Assert.notNull(contribuable);

		final RegDate dateRetourAccorde;
		final RegDate dateRetourImprime;

		final RegDate finAnnee = RegDate.get(periode.getAnnee(), 12, 31);
		if (di.getDateFin().isBefore(finAnnee)) {
			// [UNIREG-1740] spéc: pour un assujettissement qui ne s’étend qu’à une partie de l’année, ou exceptionnellement à toute l’année mais pas au-delà (départ hors Suisse ou
			// décès au 31 décembre), le délai de retour est fixé au 60e jour (paramètre) qui suit la date d’expédition. Si cette date tombe un samedi, un dimanche ou un jour légalement férié,
			// elle est reportée au 1er jour ouvrable qui suit.
			// [UNIREG-1861] c'est bien la date d'expédition qu'il faut prendre, pas la date de traitement...
			dateRetourAccorde = delaisService.getDateFinDelaiRetourDeclarationImpotEmiseManuellement(dateExpedition);
			dateRetourImprime = dateRetourAccorde;
		}
		else {
			// Traitement normal
			final ParametrePeriodeFiscalePP ppf = periode.getParametrePeriodeFiscalePP(di.getTypeContribuable());
			Assert.notNull(ppf, "Impossible de retrouver les parametres pour la periode fiscale [" + periode.getAnnee() + "] pour le type de contribuable [" + di.getTypeContribuable() + ']');

			dateRetourAccorde = ppf.getTermeGeneralSommationEffectif(); // [UNIREG-1976] le délai de retour accordé est toujours la date effective
			dateRetourImprime = ppf.getTermeGeneralSommationReglementaire(); // [UNIREG-1740] la date de retour imprimée est toujours la date réglementaire
		}

		// Mise-à-jour de la date de retour
		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateDemande(dateTraitement);
		delai.setDateTraitement(dateTraitement);
		delai.setDelaiAccordeAu(dateRetourAccorde);

		di.setDelaiRetourImprime(dateRetourImprime);
		di.addDelai(delai);
	}

	/**
	 * Ajoute l'état initial d'une déclaration, à savoir EMISE.
	 * @return la date d'expédition (= date d'obtention de l'état "EMISE")
	 */
	private RegDate ajouterEtatInitial(DeclarationImpotOrdinaire di, RegDate dateTraitement, boolean avecCalculExpedition) {

		final RegDate dateExpedition = avecCalculExpedition ? calculerDateExpedition(dateTraitement) : dateTraitement;

		final EtatDeclaration etat = new EtatDeclarationEmise();
		etat.setDateObtention(dateExpedition);
		di.addEtat(etat);

		return dateExpedition;
	}

	/**
	 * Ajoute l'adresse de retour de la déclaration d'impôt spécifiée, à savoir le CEDI.
	 *
	 * @param di    une déclaration
	 * @param tache la tâche à l'origine de la création de la déclaration
	 * @param cache le cache local des données de type structurel (cedi, aci,...)
	 */
	private void ajouterAdresseRetour(DeclarationImpotOrdinaire di, TacheEnvoiDeclarationImpotPP tache, Cache cache) {

		final TypeAdresseRetour adresseRetour = tache.getAdresseRetour();
		if (adresseRetour == null || adresseRetour == TypeAdresseRetour.CEDI) {
			// par défaut, les DIs envoyées par batch doivent retournée au CEDI
			di.setRetourCollectiviteAdministrativeId(cache.cedi.getId());
		}
		else if (adresseRetour == TypeAdresseRetour.ACI) {
			di.setRetourCollectiviteAdministrativeId(cache.aci.getId());
		}
		else {
			Assert.isEqual(TypeAdresseRetour.OID, adresseRetour);

			final Contribuable ctb = tache.getContribuable();
			final CollectiviteAdministrative coll = tiersService.getOfficeImpotAt(ctb, tache.getDateFin());
			if (coll == null) {
				throw new IllegalArgumentException("Impossible de trouver l'office d'impôt à la date de fin de la déclaration.");
			}

			di.setRetourCollectiviteAdministrativeId(coll.getId());
		}
	}

	/**
	 * Calcul de la date d'expédition.
	 * <p>
	 * La spéc dit: la date d'expédition est égale à la date du jour augmenté de 3 jours (paramètre). Si cette date tombe un samedi, un
	 * dimanche ou un jour légalement férié, elle est reporté au 1er jour ouvrable qui suit.
	 * <p>
	 */
	protected RegDate calculerDateExpedition(RegDate dateTraitement) {
		return delaisService.getDateFinDelaiCadevImpressionDeclarationImpot(dateTraitement);
	}

	/**
	 * Retourne vrai si la déclaration d'impôt et la tâches spécifiées correspondent parfaitement
	 */
	protected boolean correspondent(DeclarationImpotOrdinairePP di, TacheEnvoiDeclarationImpotPP tache) {
		return (di.getTypeContribuable() == tache.getTypeContribuable() && di.getTypeDeclaration() == tache.getTypeDocument() && DateRangeHelper.equals(di, tache));
	}

	/**
	 * @param contribuable un contribuable
	 * @param date         une date
	 * @return <b>vrai</b> si le contribuable est considéré comme indigent à la date spécifiée; <b>faux</b> autrement.
	 */
	protected static boolean estIndigent(Contribuable contribuable, final RegDate date) {
		if (contribuable instanceof ContribuableImpositionPersonnesPhysiques) {
			final ForFiscalPrincipalPP forPrincipal = ((ContribuableImpositionPersonnesPhysiques) contribuable).getForFiscalPrincipalAt(date);
			return (forPrincipal != null && forPrincipal.getModeImposition() == ModeImposition.INDIGENT);
		}
		return false;
	}

	/**
	 * @param contribuable un contribuable
	 * @param date         une date
	 * @return <b>vrai</b> si le contribuable est décédé à la date spécifiée; <b>faux</b> autrement.
	 */
	protected static boolean estDecede(Contribuable contribuable, final RegDate date) {
		final ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(date);
		return (forPrincipal != null && forPrincipal.getMotifFermeture() == MotifFor.VEUVAGE_DECES);
	}

	protected static boolean estAssujettiDansLeCanton(Contribuable contribuable, RegDate date) {
		final ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(date);
		return (forPrincipal != null && TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forPrincipal.getTypeAutoriteFiscale());
	}

	/**
	 * Cache qui contient toutes les déclarations existantes pendant une période donnée, pour un interval de contribuables donné.
	 */
	protected class DeclarationsCache {

		private final DateRange baseRange;
		private final Map<Long, List<DeclarationImpotOrdinairePP>> map = new HashMap<>();

		public DeclarationsCache(int annee, List<Long> ids) {
			this.baseRange = new Range(RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
			initMap(ids);
		}

		/**
		 * Retourne les déclarations qui existent (= intersectent) dans la période spécifiée.
		 *
		 * @param ids les ids des contribuable dont on recherche les déclarations
		 */
		@SuppressWarnings("unchecked")
		protected void initMap(final List<Long> ids) {

			// [SIFISC-1227] on ne peut pas exclure les DI annulées ici car le contenu de la map est utilisée
			// pour générer le numéro de séquence de la DI

			final String declQuery = // --------------------------------------------------------------------------
			"FROM                                                                                             "
					+ "    DeclarationImpotOrdinairePP AS di                                                  "
					+ "WHERE                                                                                  "
					+ "    di.tiers.id in (:ids) AND                                                          "
					+ "    di.periode.id = :periodeId                                                         "
					+ "ORDER BY                                                                               "
					+ "    di.dateDebut ASC                                                                   ";

			// On récupère toutes les DIs correspondant au critères du cache
			final List<DeclarationImpotOrdinairePP> list = hibernateTemplate.execute(new HibernateCallback<List<DeclarationImpotOrdinairePP>>() {
				@Override
				public List<DeclarationImpotOrdinairePP> doInHibernate(Session session) throws HibernateException {
					final FlushMode mode = session.getFlushMode();
					try {
						/*
						 * On traite toutes les tâches d'un lot de contribuables à la fois : il ne peut pas y avoir de déclarations
						 * déjà créées concernant les contribuables spécifiés et on peut donc sans risque ne pas flusher la session.
						 */
						session.setFlushMode(FlushMode.MANUAL);

						// on récupère le numéro de la période
						final Query queryPeriode = session.createQuery("SELECT p.id FROM PeriodeFiscale AS p WHERE p.annee = :annee");
						queryPeriode.setParameter("annee", baseRange.getDateDebut().year());
						final Long periodeId = (Long) queryPeriode.uniqueResult();

						// on précharge en session les tiers
						final Query queryCtbs = session.createQuery("FROM Tiers AS t WHERE t.id in (:ids)");
						queryCtbs.setParameterList("ids", ids);
						final List<?> ctbs = queryCtbs.list();
						Assert.notEmpty(ctbs);

						// et finalement on charge les déclarations
						final Query queryDecls = session.createQuery(declQuery);
						queryDecls.setParameterList("ids", ids);
						queryDecls.setParameter("periodeId", periodeId);
						return queryDecls.list();
					}
					finally {
						session.setFlushMode(mode);
					}
				}
			});

			// Initialisation de la map
			for (DeclarationImpotOrdinairePP di : list) {
				addDeclaration(di);
			}
		}

		/**
		 * Ajoute une déclaration dans le cache.
		 * @param di une déclaration d'impôt à ajouter.
		 */
		public void addDeclaration(DeclarationImpotOrdinairePP di) {
			final Long numero = di.getTiers().getNumero();

			List<DeclarationImpotOrdinairePP> l = map.get(numero);
			if (l == null) {
				l = new ArrayList<>();
				map.put(numero, l);
			}

			l.add(di);
		}

		/**
		 * Efface le cache
		 */
		public void clear() {
			map.clear();
		}

		/**
		 * Retourne les déclarations qui existent (= intersectent) dans la période spécifiée.
		 *
		 * @param contribuable le contribuable dont on recherche des déclarations
		 * @param range la période de recherche des déclaration
		 * @param annuleesIncluses si oui ou non les déclarations annulées doivent être renvoyées
		 * @return une liste des déclarations trouvées
		 */
		public List<DeclarationImpotOrdinairePP> getDeclarationsInRange(final ContribuableImpositionPersonnesPhysiques contribuable, final DateRange range, boolean annuleesIncluses) {

			if (!DateRangeHelper.within(range, baseRange)) {
				Assert.fail("Le range [" + range.getDateDebut() + ';' + range.getDateFin() + "] n'est pas compris dans le range de base ["
						+ baseRange.getDateDebut() + ';' + baseRange.getDateFin() + ']');
			}
			List<DeclarationImpotOrdinairePP> list = map.get(contribuable.getNumero());

			if (list == null) {
				list = Collections.emptyList();
			}

			// si le range spécifié ne corresponds pas à celui utilisé pour initialiser le cache, on retrie la liste en conséquence
			final boolean sameRange = DateRangeHelper.equals(range, baseRange);
			if (!list.isEmpty() && (!annuleesIncluses || !sameRange)) {
				final List<DeclarationImpotOrdinairePP> listeFiltree = new ArrayList<>(list.size());
				for (DeclarationImpotOrdinairePP di : list) {
					if ((annuleesIncluses || !di.isAnnule()) && (sameRange || DateRangeHelper.intersect(di, range))) {
						listeFiltree.add(di);
					}
				}
				list = listeFiltree;
			}

			return list;
		}
	}
}

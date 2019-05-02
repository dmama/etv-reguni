package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.pp.DeterminationDIsPPAEmettreProcessor.ExistenceResults.TacheStatus;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

public class DeterminationDIsPPAEmettreProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeterminationDIsPPAEmettreProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final PeriodeFiscaleDAO periodeDAO;
	private final TacheDAO tacheDAO;
	private final ParametreAppService parametres;
	private final TiersService tiersService;
	private final PlatformTransactionManager transactionManager;
	private final ValidationService validationService;
	private final PeriodeImpositionService periodeImpositionService;
	private final AdresseService adresseService;

	private int batchSize = BATCH_SIZE;

	public DeterminationDIsPPAEmettreProcessor(HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO, TacheDAO tacheDAO,
	                                           ParametreAppService parametres, TiersService tiersService, PlatformTransactionManager transactionManager,
	                                           ValidationService validationService, PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.periodeDAO = periodeDAO;
		this.tacheDAO = tacheDAO;
		this.parametres = parametres;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.validationService = validationService;
		this.periodeImpositionService = periodeImpositionService;
		this.adresseService = adresseService;
	}

	public DeterminationDIsPPResults run(final int anneePeriode, final RegDate dateTraitement, final int nbThreads, @Nullable StatusManager s) throws DeclarationException {

		checkParams(anneePeriode, dateTraitement);

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final DeterminationDIsPPResults rapportFinal = new DeterminationDIsPPResults(anneePeriode, dateTraitement, nbThreads, tiersService, adresseService);

		status.setMessage("Récupération des contribuables à traiter...");

		// Récupère la liste des ids des contribuables à traiter
		final List<Long> ids = createListeIdsContribuables(anneePeriode);

		// Traite les contribuables par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, DeterminationDIsPPResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids, batchSize, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, DeterminationDIsPPResults>() {

			@Override
			public DeterminationDIsPPResults createSubRapport() {
				return new DeterminationDIsPPResults(anneePeriode, dateTraitement, nbThreads, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, DeterminationDIsPPResults r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, anneePeriode, r);
				return true;
			}
		}, progressMonitor);

		final int count = rapportFinal.traites.size();

		if (status.isInterrupted()) {
			status.setMessage("La création des tâches d'envoi des déclarations d'impôt PP a été interrompue."
					+ " Nombre de nouvelles tâches en instance créées au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La création des tâches d'envoi des déclarations d'impôt PP est terminée."
					+ " Nombre de nouvelles tâches en instance créées = " + count + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void checkParams(final int anneePeriode, final RegDate dateTraitement) throws DeclarationException {

		// La période fiscale ne doit pas être antérieure à la première période fiscale paramétrée
		final int premierePeriodeFiscale = parametres.getPremierePeriodeFiscalePersonnesPhysiques();
		if (anneePeriode < premierePeriodeFiscale) {
			throw new DeclarationException("La période fiscale " + anneePeriode
					+ " est antérieure à la première période fiscale paramétrée [" + premierePeriodeFiscale + ']');
		}

		// Vérification de la date de traitement. La spec dit : la période fiscale doit être échue et la période d'envoi de masse ne doit
		// pas être dépassée.
		final RegDate dateDebutEnvoi = RegDate.get(anneePeriode + 1, 1, 1);
		if (dateTraitement.isBefore(dateDebutEnvoi)) {
			throw new DeclarationException("La période fiscale considérée [" + anneePeriode + "] n'est pas échue à la date de traitement ["
					+ dateTraitement + "].");
		}

		try {
			TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.execute(status -> {
				// Récupère la période fiscale
				final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriode);
				if (periode == null) {
					throw new RuntimeException("La période fiscale " + anneePeriode + " n'existe pas dans la base de données.");
				}

				final RegDate dateFinEnvoi = periode.getLatestDateFinEnvoiMasseDIPP();
				if (dateTraitement.isAfter(dateFinEnvoi)) {
					throw new RuntimeException("La date de fin d'envoi en masse [" + dateFinEnvoi
							                           + "] est dépassée à la date de traitement [" + dateTraitement + "].");
				}
				;
				return null;
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}

	/**
	 * Traite tout le batch des contribuabées, un par un.
	 *
	 *
	 * @param batch        le batch des contribuables à traiter
	 * @param anneePeriode la période fiscale considérée
	 * @param r
	 * @throws DeclarationException      en cas d'erreur dans le traitement d'un contribuable.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement d'un contribuable
	 */
	private void traiterBatch(final List<Long> batch, int anneePeriode, DeterminationDIsPPResults r) throws DeclarationException, AssujettissementException {

		// Récupère la période fiscale
		final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriode);
		if (periode == null) {
			throw new DeclarationException("La période fiscale " + anneePeriode + " n'existe pas dans la base de données.");
		}

		// On charge tous les contribuables en vrac (avec préchargement des déclarations)
        final List<ContribuableImpositionPersonnesPhysiques> list = hibernateTemplate.execute(session -> {
	        final Criteria crit = session.createCriteria(ContribuableImpositionPersonnesPhysiques.class);
	        crit.add(Restrictions.in("numero", batch));
	        crit.setFetchMode("declarations", FetchMode.JOIN); // force le préchargement
	        crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	        //noinspection unchecked
	        return (List<ContribuableImpositionPersonnesPhysiques>) crit.list();
        });

		// Traite tous les contribuables
		for (ContribuableImpositionPersonnesPhysiques ctb : list) {
			traiterContribuable(ctb, periode, r);
		}
	}

	/**
	 * Traite un contribuable. C'est-à-dire, détermine s'il doit recevoir une déclaration d'impôt, et si c'est le cas, crée une tâche d'envoi correspondante.
	 *
	 *
	 * @param contribuable le contribubale
	 * @param periodeFiscale la période fiscale considérée
	 * @param r
	 * @throws AssujettissementException en cas d'impossibilité de déterminer l'assujettissement
	 */
	protected void traiterContribuable(ContribuableImpositionPersonnesPhysiques contribuable, PeriodeFiscale periodeFiscale, DeterminationDIsPPResults r) throws AssujettissementException {

		r.nbCtbsTotal++;

		if (validationService.validate(contribuable).hasErrors()) {
			r.addErrorCtbInvalide(contribuable);
			return;
		}

		// on calcule les périodes d'imposition et on les traite
		final List<PeriodeImpositionPersonnesPhysiques> periodes = determineDetailsEnvoi(contribuable, periodeFiscale.getAnnee(), r);
		traiterPeriodesImposition(contribuable, periodeFiscale, periodes, r);

		// [UNIREG-1742] rattrapage des déclarations et des tâches non-valides
		verifierValiditeDeclarations(contribuable, periodeFiscale, periodes, r);
		verifierValiditeTachesEnvoi(contribuable, periodeFiscale, periodes, r);
	}

	/**
	 * Génère les tâches d'envoi de déclaration d'impôt à partir des périodes d'imposition spécifiées.
	 *
	 * @param contribuable   le contribuable concerné
	 * @param periodeFiscale la période fiscale concernée
	 * @param periodes       les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param r
	 */
	private void traiterPeriodesImposition(ContribuableImpositionPersonnesPhysiques contribuable, PeriodeFiscale periodeFiscale, List<PeriodeImpositionPersonnesPhysiques> periodes, DeterminationDIsPPResults r) {
		if (periodes == null || periodes.isEmpty()) {
			r.addIgnorePasAssujetti(contribuable); // pas assujetti au rôle ordinaire (départ HC, mariage, ...)
		}
		else {
			for (PeriodeImpositionPersonnesPhysiques p : periodes) {
				if (needsDeclaration(p, r)) {
					traiterPeriodeImposition(contribuable, periodeFiscale, p, r);
				}
			}
		}
	}

	/**
	 * Vérifie les déclarations préexistantes, et dans le cas où elles ne correspondent pas exactement à une période calculée, génère une tâche d'annulation.
	 *
	 * @param contribuable   le contribuable concerné
	 * @param periodeFiscale la période fiscale concernée
	 * @param periodes       les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param r
	 */
	private void verifierValiditeDeclarations(ContribuableImpositionPersonnesPhysiques contribuable,
	                                          PeriodeFiscale periodeFiscale,
	                                          List<PeriodeImpositionPersonnesPhysiques> periodes,
	                                          DeterminationDIsPPResults r) {
		final List<DeclarationImpotOrdinairePP> declarations = contribuable.getDeclarationsTriees(DeclarationImpotOrdinairePP.class, false);
		for (DeclarationImpotOrdinairePP di : declarations) {
			if (di.getPeriode().getAnnee().equals(periodeFiscale.getAnnee())) {
				verifierValiditeDeclaration(contribuable, periodes, di, r);
			}
		}
	}

	/**
	 * Vérifie la déclaration spécifiée, et dans le cas où elle ne correspond pas exactement à une période calculée, génère une tâche d'annulation.
	 *
	 * @param contribuable le contribuable concerné
	 * @param periodes     les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param di           la déclaration d'impôt ordinaire à vérifier
	 * @param r
	 */
	private void verifierValiditeDeclaration(ContribuableImpositionPersonnesPhysiques contribuable,
	                                         List<PeriodeImpositionPersonnesPhysiques> periodes,
	                                         DeclarationImpotOrdinaire di,
	                                         DeterminationDIsPPResults r) {

		final ExistenceResults<PeriodeImpositionPersonnesPhysiques> results = checkExistencePeriode(periodes, di);
		if (results == null) {

			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			criterion.setDeclarationAnnulee(di);
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);

			if (tacheDAO.count(criterion, true /* don't flush */) == 0) {

				CollectiviteAdministrative oid = tiersService.getOfficeImpotAt(contribuable, null);
				if (oid == null) {
					// le contribuable n'a jamais possédé de for fiscal vaudois -> on l'envoie sur l'ACI
					oid = tiersService.getOfficeImpot(ServiceInfrastructureService.noACI);
					if (oid == null) {
						throw new IllegalArgumentException();
					}
				}

				// [UNIREG-1742] pas de période pour la déclaration => on crée une tâche d'annulation
				TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, null, contribuable, di, oid);
				r.addTacheAnnulationCreee(contribuable, tache);
				tacheDAO.save(tache);
			}
			else {
				// [UNIREG-1981] on ne crée pas de tâche d'annulation s'il en existe déjà une
				r.addIgnoreTacheAnnulationDejaExistante(contribuable);
			}
		}
		else {
			switch (results.status) {
			case EXISTE_DEJA:
				// tout est ok, il y a bien une période qui correspond à la déclaration
				break;
			case INTERSECTE:
				// problème, mais il doit avoir été détecté lors de la création de la tâche
				break;
			default:
				throw new IllegalArgumentException("Type de résultat inconnu = [" + results.status + ']');
			}
		}
	}

	/**
	 * Vérifie les tâches d'envoi de déclaration préexistantes, et dans le cas où elles ne correspondent pas exactement à une période calculée, elles sont annulées.
	 *
	 * @param contribuable   le contribuable concerné
	 * @param periodeFiscale la période fiscale concernée
	 * @param periodes       les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param r
	 */
	private void verifierValiditeTachesEnvoi(ContribuableImpositionPersonnesPhysiques contribuable,
	                                         PeriodeFiscale periodeFiscale,
	                                         List<PeriodeImpositionPersonnesPhysiques> periodes,
	                                         DeterminationDIsPPResults r) {
		
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
		criterion.setAnnee(periodeFiscale.getAnnee());
		criterion.setContribuable(contribuable);
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE); // [UNIREG-1981] on ignore les tâches déjà traitées

		final List<Tache> taches = tacheDAO.find(criterion, true /* don't flush */);
		if (taches != null && !taches.isEmpty()) {
			for (Tache t : taches) {
				final TacheEnvoiDeclarationImpotPP te = (TacheEnvoiDeclarationImpotPP) t;
				if (!te.isAnnule()) {
					verifierValiditeTacheEnvoi(contribuable, periodes, te, r);
				}
			}
		}
	}

	/**
	 * Vérifie la tâche d'envoi de déclaration spécifiée, et dans le cas où elle ne correspond pas exactement à une période calculée, elle est annulée.
	 *
	 * @param contribuable le contribuable concerné
	 * @param periodes     les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param tache        la tache d'envoi de déclaration à vérifier
	 * @param r
	 */
	private void verifierValiditeTacheEnvoi(ContribuableImpositionPersonnesPhysiques contribuable,
	                                        List<PeriodeImpositionPersonnesPhysiques> periodes,
	                                        TacheEnvoiDeclarationImpotPP tache,
	                                        DeterminationDIsPPResults r) {

		final ExistenceResults<PeriodeImpositionPersonnesPhysiques> results = checkExistencePeriode(periodes, tache);
		if (results == null) {
			// [UNIREG-1742] pas de période pour la tâche => on l'annule
			tache.setAnnule(true);
			r.addTacheEnvoiAnnulee(contribuable, tache);
		}
		else {
			switch (results.status) {
			case EXISTE_DEJA:
				// tout est ok, il y a bien une période qui correspond à la déclaration
				break;
			case INTERSECTE:
				// [UNIREG-1984] la tâche ne corresponds pas à la période d'imposition calculée, on l'annule
				tache.setAnnule(true);
				r.addTacheEnvoiAnnulee(contribuable, tache);
				break;
			default:
				throw new IllegalArgumentException("Type de résultat inconnu = [" + results.status + ']');
			}
		}
	}

	/**
	 *
	 * @param contribuable un contribuable
	 * @param annee        l'année de la période fiscale considérée
	 * @param r
	 * @return les périodes d'imposition déterminées; ou <b>null</b> s'il n'y en a pas ou s'il n'a pas été possible de les déterminer.
	 */
	protected List<PeriodeImpositionPersonnesPhysiques> determineDetailsEnvoi(ContribuableImpositionPersonnesPhysiques contribuable, int annee, DeterminationDIsPPResults r) {

		// Détermination des périodes d'imposition du contribuable dans l'année
		final List<PeriodeImpositionPersonnesPhysiques> periodes;
		try {
			final List<PeriodeImposition> generic = periodeImpositionService.determine(contribuable, annee);
			if (generic == null || generic.isEmpty()) {
				periodes = null;
			}
			else {
				periodes = new ArrayList<>(generic.size());
				for (PeriodeImposition pi : generic) {
					periodes.add((PeriodeImpositionPersonnesPhysiques) pi);
				}
			}
		}
		catch (AssujettissementException e) {
			if (r != null) {
				r.addCtbErrorDonneesIncoherentes(contribuable, e.getMessage() + ". Aucune déclaration envoyée.");
			}
			return null;
		}

		return periodes;
	}

	/**
	 * Analyse la période d'imposition spécifiée et dit s'il faut envoyer une déclaration d'impôt ou pas.
	 *
	 *
	 * @param periode la période à analyser
	 * @param r
	 * @return <b>vrai</b> si la période nécessite l'envoi d'une déclaration d'impôt; <b>faux</i> autrement.
	 */
	protected boolean needsDeclaration(PeriodeImpositionPersonnesPhysiques periode, DeterminationDIsPPResults r) {

		final Contribuable contribuable = periode.getContribuable();

		if (periode.isDiplomateSuisseSansImmeuble()) {
			r.addIgnoreDiplomate(contribuable);
			return false; // pas d'envoi de DI
		}

		if (periode.isDeclarationRemplaceeParNote()) {
			r.addIgnoreDeclarationRemplaceeParNote(contribuable);
			return false; // pas d'envoi de DI
		}

		if (periode.isDeclarationOptionnelle()) {
			r.addIgnoreDeclarationOptionnelle(contribuable);
			return false; // pas d'envoi de DI
		}

		return true;
	}

	/**
	 * Traite la période d'imposition spécifiée et crée une tâche en instance si possible.
	 *
	 *
	 * @param contribuable le tiers dont on envoie la déclaration
	 * @param periode      la période fiscale considérée
	 * @param details      les détails précalculés du contribuable
	 * @param r
	 * @return la tâche créée; ou <b>null</i> si une erreur a été détectée
	 */
	protected TacheEnvoiDeclarationImpotPP traiterPeriodeImposition(ContribuableImpositionPersonnesPhysiques contribuable,
	                                                                PeriodeFiscale periode,
	                                                                PeriodeImpositionPersonnesPhysiques details,
	                                                                DeterminationDIsPPResults r) {

		// Vérifie qu'une déclaration d'impôt n'existe pas déjà
		final ExistenceResults<DeclarationImpotOrdinaire> checkDI = checkExistenceDeclaration(contribuable, details);
		if (checkDI != null) {
			switch (checkDI.status) {
			case EXISTE_DEJA:
				// une déclaration existe déjà, rien à faire
				if (r != null) {
					r.addIgnoreDeclarationDejaExistante(contribuable);
				}
				break;
			case INTERSECTE:
				// une autre déclaration existe qui occupe partiellement le range, il y a un problème
				if (r != null) {
					final String message = "Déclaration d'impôt [id=" + checkDI.object.getId() + ", début=" + checkDI.object.getDateDebut() + ", fin=" +
							checkDI.object.getDateFin() + "]. Période d'imposition calculée [début=" + details.getDateDebut() + ", fin=" + details.getDateFin() + ']';
					r.addErrorDeclarationCollision(contribuable, message);
				}
				break;
			default:
				throw new NotImplementedException("");
			}
			return null;
		}

		// Vérifie qu'une tâche n'existe pas déjà
		final ExistenceResults<TacheEnvoiDeclarationImpotPP> checkTache = checkExistenceTache(contribuable, details);
		if (checkTache != null) {
			switch (checkTache.status) {
			case EXISTE_DEJA:
				// la tâche existe déjà, rien à faire
				if (r != null) {
					r.addIgnoreTacheEnvoiDejaExistante(contribuable);
				}
				return null;
			case INTERSECTE:
				// [UNIREG-1984] une autre tâche occupe partiellement le range -> elle sera annulée par le post-processing de vérification des tâches, on continue donc.
				break;
			case DEJA_TRAITE:
				//Rien a faire dans ce cas la tache sera de nouveau générée
				break;
			default:
				throw new IllegalArgumentException("Le status de tâche [" + checkTache.status + "] est inconnu.");
			}
		}

		final CollectiviteAdministrative oid = tiersService.getOfficeImpotAt(contribuable, details.getDateFin());
		if (oid == null) {
			throw new IllegalArgumentException();
		}

		// Création et sauvegarde de la tâche en base
		final RegDate dateEcheance = periode.getParametrePeriodeFiscalePP(details.getTypeContribuable()).getDateFinEnvoiMasseDI();
		if (dateEcheance == null) {
			throw new IllegalArgumentException();
		}

		final TacheEnvoiDeclarationImpotPP tache = new TacheEnvoiDeclarationImpotPP(TypeEtatTache.EN_INSTANCE, dateEcheance, contribuable, details.getDateDebut(), details.getDateFin(),
		                                                                            details.getTypeContribuable(), details.getTypeDocumentDeclaration(), null,
		                                                                            details.getCodeSegment(), details.getAdresseRetour(), oid);
		if (r != null) {
			r.addTacheEnvoiCreee(contribuable, tache);
		}

		return (TacheEnvoiDeclarationImpotPP) tacheDAO.save(tache);
	}

	/**
	 * Résultat de la recherche de l'existence d'un objet dans une période spécifiée.
	 */
	protected static class ExistenceResults<T extends DateRange> {

		protected enum TacheStatus {
			EXISTE_DEJA, INTERSECTE, DEJA_TRAITE
		}

		public final TacheStatus status;
		public final T object;

		public ExistenceResults(TacheStatus status, T object) {
			this.status = status;
			this.object = object;
		}

		public T getObject() {
			return object;
		}
	}

	protected ExistenceResults<PeriodeImpositionPersonnesPhysiques> checkExistencePeriode(final List<PeriodeImpositionPersonnesPhysiques> periodes, final DateRange range) {

		ExistenceResults<PeriodeImpositionPersonnesPhysiques> status = null;

		if (periodes != null && !periodes.isEmpty()) {
			for (PeriodeImpositionPersonnesPhysiques p : periodes) {

				if (DateRangeHelper.equals(p, range)) {
					status = new ExistenceResults<>(TacheStatus.EXISTE_DEJA, p);
				}
				else if (DateRangeHelper.intersect(p, range)) {
					status = new ExistenceResults<>(TacheStatus.INTERSECTE, p);
					break; // inutile de continuer
				}
			}
		}

		return status;
	}

	/**
	 * Vérifie la présence d'une déclaration pré-existante pour le contribuable et la période spécifiée.
	 *
	 * @param contribuable un contribuable
	 * @param periode      la période
	 * @return <b>null</b> si aucune déclaraiton n'existe, ou un résultat détaillé si une déclaration préexistente a été détectée.
	 */
	protected ExistenceResults<DeclarationImpotOrdinaire> checkExistenceDeclaration(final ContribuableImpositionPersonnesPhysiques contribuable,
	                                                                                final PeriodeImpositionPersonnesPhysiques periode) {

		ExistenceResults<DeclarationImpotOrdinaire> status = null;

		// [UNIREG-1417] : ne pas tenir compte des DI annulées...
		final List<DeclarationImpotOrdinairePP> declarations = contribuable.getDeclarationsTriees(DeclarationImpotOrdinairePP.class, false);
		for (DeclarationImpotOrdinaire di : declarations) {
			if (DateRangeHelper.equals(di, periode)) {
				status = new ExistenceResults<>(TacheStatus.EXISTE_DEJA, di);
			}
			else if (DateRangeHelper.intersect(di, periode)) {
				status = new ExistenceResults<>(TacheStatus.INTERSECTE, di);
				break; // inutile de continuer
			}
		}

		return status;
	}

	/**
	 * Vérifie la présence d'une tâche pré-existante pour le contribuable et la période spécifiée.
	 *
	 * @param contribuable un contribuable
	 * @param range        la période
	 * @return <b>null</b> si aucune tâche n'existe, ou un résultat détaillé si un tâche préexistente a été détectée.
	 */
	protected ExistenceResults<TacheEnvoiDeclarationImpotPP> checkExistenceTache(ContribuableImpositionPersonnesPhysiques contribuable, DateRange range) {

		ExistenceResults<TacheEnvoiDeclarationImpotPP> status = null;

		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
		criterion.setAnnee(range.getDateDebut().year());
		criterion.setContribuable(contribuable);
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE); // [UNIREG-1984] on ignore les tâches déjà traitées

		final List<Tache> list = tacheDAO.find(criterion, true /* don't flush */);
		for (Tache t : list) {
			final TacheEnvoiDeclarationImpotPP tache = (TacheEnvoiDeclarationImpotPP) t;

			if (DateRangeHelper.equals(tache, range)) {
				if (TypeEtatTache.TRAITE == tache.getEtat()) {
					//[UNIREG-1417] une tache trouvée à l'etat TRAITEE indique qu'une di a été emise
					//mais qu'elle a été annulée par la suite. Il faut de nouveau l'emettre vu  que le contribuable
					//concerné à été assujetti sur la période fiscale
					status = new ExistenceResults<>(TacheStatus.DEJA_TRAITE, tache);
				}
				else {
					status = new ExistenceResults<>(TacheStatus.EXISTE_DEJA, tache);
				}
			}
			else if (DateRangeHelper.intersect(tache, range)) {
				status = new ExistenceResults<>(TacheStatus.INTERSECTE, tache);
				break; // inutile de continuer
			}
		}

		return status;
	}

	private static final String queryIdsCtbWithFors = // --------------------------------
	"SELECT DISTINCT                                                                         "
			+ "    cont.id                                                                   "
			+ "FROM                                                                          "
			+ "    ContribuableImpositionPersonnesPhysiques AS cont                          "
			+ "INNER JOIN                                                                    "
			+ "    cont.forsFiscaux AS fors                                                  "
			+ "WHERE                                                                         "
			+ "    cont.annulationDate IS null                                               "
			+ "    AND fors.annulationDate IS null                                           "
			+ "    AND fors.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'                   "
			+ "    AND (fors.class = ForFiscalPrincipalPP OR fors.class = ForFiscalSecondaire) "
			+ "    AND (fors.modeImposition IS null OR fors.modeImposition != 'SOURCE')      "
			+ "    AND (fors.dateDebut IS null OR fors.dateDebut <= :finAnnee)               "
			+ "    AND (fors.dateFin IS null OR fors.dateFin >= :debutAnnee)                 " // [UNIREG-1742] for actif n'importe quand dans l'année
			+ "ORDER BY cont.id ASC                                                          ";

	private static final String queryIdsCtbWithTasks = // ------------------
	"SELECT DISTINCT                                                             "
			+ "    tache.contribuable.id                                         "
			+ "FROM                                                              "
			+ "    TacheEnvoiDeclarationImpotPP AS tache                           "
			+ "WHERE                                                             "
			+ "    tache.annulationDate IS null                                  "
			+ "    AND tache.etat = 'EN_INSTANCE'                                "
			+ "    AND (tache.dateDebut IS null OR tache.dateDebut <= :finAnnee) "
			+ "    AND (tache.dateFin IS null OR tache.dateFin >= :debutAnnee)   "
			+ "ORDER BY tache.contribuable.id ASC                                ";

	private static final String queryIdsCtbWithDeclarations = // -------------
	"SELECT DISTINCT                                                           "
			+ "    decl.tiers.id                                               "
			+ "FROM                                                            "
			+ "    DeclarationImpotOrdinairePP AS decl                         "
			+ "WHERE                                                           "
			+ "    decl.annulationDate IS null                                 "
			+ "    AND decl.dateDebut <= :finAnnee                             "
			+ "    AND decl.dateFin >= :debutAnnee                             "
			+ "ORDER BY decl.tiers.id ASC                                      ";

	/**
	 * Crée la liste des ids des contribuables devant être traités.
	 * <p/>
	 * Il s'agit du regroupement de trois populations :
	 * <ul>
	 * <li>les contribuables avec des fors actifs pendant l'année considérée (= potentiellement assujettis)</li>
	 * <li>les contribuables avec des tâches d'envoi de déclarations en instance pour l'année considérée (= pour vérification, éventuellement annulation)</li>
	 * <li>les contribuables avec des déclaration d'impôt ordinaires valides pendant l'année considérée (= pour vérification, éventuellement création de tâches d'annulation)</li>
	 * </ul>
	 *
	 * @param annee la période fiscale considérée
	 * @return itérateur sur les ids des contribuables
	 */
	protected List<Long> createListeIdsContribuables(final int annee) {

		final RegDate debutAnnee = RegDate.get(annee, 1, 1);
		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		//noinspection UnnecessaryLocalVariable
		final List<Long> ids = template.execute(status -> {

			final List<Long> idsFors = hibernateTemplate.executeWithNewSession(session -> {
				final Query queryObject = session.createQuery(queryIdsCtbWithFors);
				queryObject.setParameter("debutAnnee", debutAnnee);
				queryObject.setParameter("finAnnee", finAnnee);
				//noinspection unchecked
				return (List<Long>) queryObject.list();
			});

			final List<Long> idsTasks = hibernateTemplate.executeWithNewSession(session -> {
				final Query queryObject = session.createQuery(queryIdsCtbWithTasks);
				queryObject.setParameter("debutAnnee", debutAnnee);
				queryObject.setParameter("finAnnee", finAnnee);
				//noinspection unchecked
				return (List<Long>) queryObject.list();
			});

			final List<Long> idsDIs = hibernateTemplate.executeWithNewSession(session -> {
				final Query queryObject = session.createQuery(queryIdsCtbWithDeclarations);
				queryObject.setParameter("debutAnnee", debutAnnee);
				queryObject.setParameter("finAnnee", finAnnee);
				//noinspection unchecked
				return (List<Long>) queryObject.list();
			});

			final Set<Long> set = new HashSet<>(idsFors.size() + idsTasks.size() + idsDIs.size());
			set.addAll(idsFors);
			set.addAll(idsTasks);
			set.addAll(idsDIs);

			final List<Long> ids1 = new ArrayList<>(set);
			Collections.sort(ids1);

			return ids1;
		});

		return ids;
	}

	/**
	 * Pour le testing uniquement !
	 *
	 * @param batchSize la taille du batch
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
}

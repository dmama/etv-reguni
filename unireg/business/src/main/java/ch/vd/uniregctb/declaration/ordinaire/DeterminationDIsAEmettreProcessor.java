package ch.vd.uniregctb.declaration.ordinaire;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.*;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsAEmettreProcessor.ExistenceResults.TacheStatus;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

public class DeterminationDIsAEmettreProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(DeterminationDIsAEmettreProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final PeriodeFiscaleDAO periodeDAO;
	private final TacheDAO tacheDAO;
	private final ParametreAppService parametres;
	private final PlatformTransactionManager transactionManager;

	private DeterminationDIsResults rapport;

	private int batchSize = BATCH_SIZE;

	public DeterminationDIsAEmettreProcessor(HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO, TacheDAO tacheDAO,
			ParametreAppService parametres, PlatformTransactionManager transactionManager) {
		this.hibernateTemplate = hibernateTemplate;
		this.periodeDAO = periodeDAO;
		this.tacheDAO = tacheDAO;
		this.parametres = parametres;
		this.transactionManager = transactionManager;
	}

	public DeterminationDIsResults run(final int anneePeriode, final RegDate dateTraitement, final StatusManager s)
			throws DeclarationException {

		checkParams(anneePeriode, dateTraitement);

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final DeterminationDIsResults rapportFinal = new DeterminationDIsResults(anneePeriode, dateTraitement);

		status.setMessage("Récupération des contribuables à traiter...");

		// Récupère la liste des ids des contribuables à traiter
		final List<Long> ids = createListeIdsContribuables(anneePeriode);

		// Traite les contribuables par lots
		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(ids, batchSize, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, status, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			private Long idCtb = null;

			@Override
			public void beforeTransaction() {
				rapport = new DeterminationDIsResults(anneePeriode, dateTraitement);
				idCtb = null;
			}

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {

				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				if (batch.size() == 1) {
					idCtb = batch.get(0);
				}
				traiterBatch(batch, anneePeriode);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (willRetry) {
					// le batch va être rejoué -> on peut ignorer le rapport
					rapport = null;
				}
				else {
					// on ajoute l'exception directement dans le rapport final
					rapportFinal.addErrorException(idCtb, e);
					rapport = null;
				}
			}

			@Override
			public void afterTransactionCommit() {
				rapportFinal.add(rapport);
			}
		});

		final int count = rapportFinal.traites.size();

		if (status.interrupted()) {
			status.setMessage("La création des tâches d'envoi des déclarations d'impôt a été interrompue."
					+ " Nombre de nouvelles tâches en instance créées au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La création des tâches d'envoi des déclarations d'impôt est terminée."
					+ " Nombre de nouvelles tâches en instance créées = " + count + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void checkParams(final int anneePeriode, final RegDate dateTraitement) throws DeclarationException {

		// La période fiscale ne doit pas être antérieure à la première période fiscale paramétrée
		final int premierePeriodeFiscale = parametres.getPremierePeriodeFiscale();
		if (anneePeriode < premierePeriodeFiscale) {
			throw new DeclarationException("La période fiscale " + anneePeriode
					+ " est antérieure à la première période fiscale paramétrée [" + premierePeriodeFiscale + "]");
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
			template.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {

					// Récupère la période fiscale
					final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriode);
					if (periode == null) {
						throw new RuntimeException("La période fiscale " + anneePeriode + " n'existe pas dans la base de données.");
					}

					final RegDate dateFinEnvoi = periode.getLatestDateFinEnvoiMasseDI();
					if (dateTraitement.isAfter(dateFinEnvoi)) {
						throw new RuntimeException("La date de fin d'envoi en masse [" + dateFinEnvoi
								+ "] est dépassée à la date de traitement [" + dateTraitement + "].");
					}
					return null;
				}
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}

	/**
	 * Traite tout le batch des contribuabées, un par un.
	 *
	 * @param batch        le batch des contribuables à traiter
	 * @param anneePeriode la période fiscale considérée
	 * @throws DeclarationException      en cas d'erreur dans le traitement d'un contribuable.
	 * @throws AssujettissementException en cas d'impossibilité de déterminer l'assujettissement
	 */
	private void traiterBatch(List<Long> batch, int anneePeriode) throws DeclarationException, AssujettissementException {

		// Récupère la période fiscale
		final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriode);
		if (periode == null) {
			throw new DeclarationException("La période fiscale " + anneePeriode + " n'existe pas dans la base de données.");
		}

		// Traite tous les contribuables
		for (Long id : batch) {
			traiterContribuable(id, periode);
		}
	}

	/**
	 * Traite un contribuable. C'est-à-dire, détermine s'il doit recevoir une déclaration d'impôt, et si c'est le cas, crée une tâche d'envoi correspondante.
	 *
	 * @param ctbId   le numéro du contribubale
	 * @param periodeFiscale la période fiscale considérée
	 * @throws AssujettissementException en cas d'impossibilité de déterminer l'assujettissement
	 */
	protected void traiterContribuable(Long ctbId, PeriodeFiscale periodeFiscale) throws AssujettissementException {

		rapport.nbCtbsTotal++;

		final Contribuable contribuable = (Contribuable) hibernateTemplate.get(Contribuable.class, ctbId);
		if (contribuable.validate().hasErrors()) {
			rapport.addErrorCtbInvalide(contribuable);
			return;
		}

		// on calcule les périodes d'imposition et on les traite
		final List<PeriodeImposition> periodes = determineDetailsEnvoi(contribuable, periodeFiscale.getAnnee());
		traiterPeriodesImposition(contribuable, periodeFiscale, periodes);

		// [UNIREG-1742] rattrapage des déclarations et des tâches non-valides
		verifierValiditeDeclarations(contribuable, periodeFiscale, periodes);
		verifierValiditeTachesEnvoi(contribuable, periodeFiscale, periodes);
	}

	/**
	 * Génère les tâches d'envoi de déclaration d'impôt à partir des périodes d'imposition spécifiées.
	 *
	 * @param contribuable   le contribuable concerné
	 * @param periodeFiscale la période fiscale concernée
	 * @param periodes       les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 */
	private void traiterPeriodesImposition(Contribuable contribuable, PeriodeFiscale periodeFiscale, List<PeriodeImposition> periodes) {
		if (periodes == null || periodes.isEmpty()) {
			if (rapport != null) {
				rapport.addIgnorePasAssujetti(contribuable); // pas assujetti au rôle ordinaire (départ HC, mariage, ...)
			}
		}
		else {
			for (PeriodeImposition p : periodes) {
				if (needsDeclaration(p)) {
					traiterPeriodeImposition(contribuable, periodeFiscale, p);
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
	 */
	private void verifierValiditeDeclarations(Contribuable contribuable, PeriodeFiscale periodeFiscale, List<PeriodeImposition> periodes) {
		final Set<Declaration> declarations = contribuable.getDeclarations();
		if (declarations != null && !declarations.isEmpty()) {
			for (Declaration d : declarations) {
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (!di.isAnnule() && di.getPeriode().getAnnee().equals(periodeFiscale.getAnnee())) {
					verifierValiditeDeclaration(contribuable, periodes, di);
				}
			}
		}
	}

	/**
	 * Vérifie la déclaration spécifiée, et dans le cas où elle ne correspond pas exactement à une période calculée, génère une tâche d'annulation.
	 *
	 * @param contribuable le contribuable concerné
	 * @param periodes     les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param di           la déclaration d'impôt ordinaire à vérifier
	 */
	private void verifierValiditeDeclaration(Contribuable contribuable, List<PeriodeImposition> periodes, DeclarationImpotOrdinaire di) {

		final ExistenceResults<PeriodeImposition> results = checkExistencePeriode(periodes, di);
		if (results == null) {

			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			criterion.setDeclarationAnnulee(di);
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);

			if (tacheDAO.count(criterion, true /* don't flush */) == 0) {
				// [UNIREG-1742] pas de période pour la déclaration => on crée une tâche d'annulation
				TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, null, contribuable, di);
				if (rapport != null) {
					rapport.addTacheAnnulationCreee(contribuable, tache);
				}
				tacheDAO.save(tache);
			}
			else {
				// [UNIREG-1981] on ne crée pas de tâche d'annulation s'il en existe déjà une
				rapport.addIgnoreTacheAnnulationDejaExistante(contribuable);
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
				throw new IllegalArgumentException("Type de résultat inconnu = [" + results.status + "]");
			}
		}
	}

	/**
	 * Vérifie les tâches d'envoi de déclaration préexistantes, et dans le cas où elles ne correspondent pas exactement à une période calculée, elles sont annulées.
	 *
	 * @param contribuable   le contribuable concerné
	 * @param periodeFiscale la période fiscale concernée
	 * @param periodes       les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 */
	private void verifierValiditeTachesEnvoi(Contribuable contribuable, PeriodeFiscale periodeFiscale, List<PeriodeImposition> periodes) {
		
		TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		criterion.setAnnee(periodeFiscale.getAnnee());
		criterion.setContribuable(contribuable);
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE); // [UNIREG-1981] on ignore les tâches déjà traitées

		final List<Tache> taches = tacheDAO.find(criterion, true /* don't flush */);
		if (taches != null && !taches.isEmpty()) {
			for (Tache t : taches) {
				final TacheEnvoiDeclarationImpot te = (TacheEnvoiDeclarationImpot) t;
				if (!te.isAnnule()) {
					verifierValiditeTacheEnvoi(contribuable, periodes, te);
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
	 */
	private void verifierValiditeTacheEnvoi(Contribuable contribuable, List<PeriodeImposition> periodes, TacheEnvoiDeclarationImpot tache) {

		final ExistenceResults<PeriodeImposition> results = checkExistencePeriode(periodes, tache);
		if (results == null) {
			// [UNIREG-1742] pas de période pour la tâche => on l'annule
			tache.setAnnule(true);
			if (rapport != null) {
				rapport.addTacheEnvoiAnnulee(contribuable, tache);
			}
		}
		else {
			switch (results.status) {
			case EXISTE_DEJA:
				// tout est ok, il y a bien une période qui correspond à la déclaration
				break;
			case INTERSECTE:
				// [UNIREG-1984] la tâche ne corresponds pas à la période d'imposition calculée, on l'annule
				tache.setAnnule(true);
				if (rapport != null) {
					rapport.addTacheEnvoiAnnulee(contribuable, tache);
				}
				break;
			default:
				throw new IllegalArgumentException("Type de résultat inconnu = [" + results.status + "]");
			}
		}
	}

	/**
	 * @param contribuable un contribuable
	 * @param annee        l'année de la période fiscale considérée
	 * @return les périodes d'imposition déterminées; ou <b>null</b> s'il n'y en a pas ou s'il n'a pas été possible de les déterminer.
	 */
	protected List<PeriodeImposition> determineDetailsEnvoi(Contribuable contribuable, int annee) {

		// Détermination des périodes d'imposition du contribuable dans l'année
		final List<PeriodeImposition> periodes;
		try {
			periodes = PeriodeImposition.determine(contribuable, annee);
		}
		catch (AssujettissementException e) {
			if (rapport != null) {
				rapport.addCtbErrorDonneesIncoherentes(contribuable, e.getMessage() + ". Aucune déclaration envoyée.");
			}
			return null;
		}

		return periodes;
	}

	/**
	 * Analyse la période d'imposition spécifiée et dit s'il faut envoyer une déclaration d'impôt ou pas.
	 *
	 * @param periode la période à analyser
	 * @return <b>vrai</b> si la période nécessite l'envoi d'une déclaration d'impôt; <b>faux</i> autrement.
	 */
	protected boolean needsDeclaration(PeriodeImposition periode) {

		final Contribuable contribuable = periode.getContribuable();

		if (periode.isDiplomateSuisse()) {
			if (rapport != null) {
				rapport.addIgnoreDiplomate(contribuable);
			}
			return false; // pas d'envoi de DI
		}

		if (periode.isRemplaceeParNote()) {
			if (rapport != null) {
				rapport.addIgnoreDeclarationRemplaceeParNote(contribuable);
			}
			return false; // pas d'envoi de DI
		}

		if (periode.isOptionnelle()) {
			if (rapport != null) {
				rapport.addIgnoreDeclarationOptionnelle(contribuable);
			}
			return false; // pas d'envoi de DI
		}

		return true;
	}

	/**
	 * Traite la période d'imposition spécifiée et crée une tâche en instance si possible.
	 *
	 * @param contribuable le tiers dont on envoie la déclaration
	 * @param periode      la période fiscale considérée
	 * @param details      les détails précalculés du contribuable
	 * @return la tâche créée; ou <b>null</i> si une erreur a été détectée
	 */
	protected TacheEnvoiDeclarationImpot traiterPeriodeImposition(Contribuable contribuable, PeriodeFiscale periode, PeriodeImposition details) {

		// Vérifie qu'une déclaration d'impôt n'existe pas déjà
		final ExistenceResults<DeclarationImpotOrdinaire> checkDI = checkExistenceDeclaration(contribuable, details);
		if (checkDI != null) {
			switch (checkDI.status) {
			case EXISTE_DEJA:
				// une déclaration existe déjà, rien à faire
				if (rapport != null) {
					rapport.addIgnoreDeclarationDejaExistante(contribuable);
				}
				break;
			case INTERSECTE:
				// une autre déclaration existe qui occupe partiellement le range, il y a un problème
				final String message = "Déclaration d'impôt [id=" + checkDI.object.getId() + ", début=" + checkDI.object.getDateDebut() + ", fin=" +
						checkDI.object.getDateFin() + "]. Période d'imposition calculée [début=" + details.getDateDebut() + ", fin=" + details.getDateFin() + "]";
				if (rapport != null) {
					rapport.addErrorDeclarationCollision(contribuable, message);
				}
				break;
			default:
				throw new NotImplementedException();
			}
			return null;
		}

		// Vérifie qu'une tâche n'existe pas déjà
		final ExistenceResults<TacheEnvoiDeclarationImpot> checkTache = checkExistenceTache(contribuable, details);
		if (checkTache != null) {
			switch (checkTache.status) {
			case EXISTE_DEJA:
				// la tâche existe déjà, rien à faire
				if (rapport != null) {
					rapport.addIgnoreTacheEnvoiDejaExistante(contribuable);
				}
				return null;
			case INTERSECTE:
				// [UNIREG-1984] une autre tâche occupe partiellement le range -> elle sera annulée par le post-processing de vérification des tâches, on continue donc.
				break;
			default:
				throw new IllegalArgumentException("Le status de tâche [" + checkTache.status + "] est inconnu.");
			}
		}

		// Création et sauvegarde de la tâche en base
		final TypeContribuableDI type = details.getTypeContribuableDI();
		final RegDate dateEcheance = periode.getParametrePeriodeFiscale(type.getTypeContribuable()).getDateFinEnvoiMasseDI();
		Assert.notNull(dateEcheance);

		final TacheEnvoiDeclarationImpot tache = new TacheEnvoiDeclarationImpot(TypeEtatTache.EN_INSTANCE, dateEcheance, contribuable, details.getDateDebut(), details.getDateFin(),
				type.getTypeContribuable(), type.getTypeDocument(), details.getQualification(), details.getAdresseRetour());
		if (rapport != null) {
			rapport.addTacheEnvoiCreee(contribuable, tache);
		}

		return (TacheEnvoiDeclarationImpot) tacheDAO.save(tache);
	}

	/**
	 * Résultat de la recherche de l'existence d'un objet dans une période spécifiée.
	 */
	protected static class ExistenceResults<T extends DateRange> {

		protected enum TacheStatus {
			EXISTE_DEJA, INTERSECTE
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

	@SuppressWarnings({"unchecked"})
	protected ExistenceResults<PeriodeImposition> checkExistencePeriode(final List<PeriodeImposition> periodes, final DateRange range) {

		ExistenceResults<PeriodeImposition> status = null;

		if (periodes != null && !periodes.isEmpty()) {
			for (PeriodeImposition p : periodes) {

				if (DateRangeHelper.equals(p, range)) {
					status = new ExistenceResults<PeriodeImposition>(TacheStatus.EXISTE_DEJA, p);
				}
				else if (DateRangeHelper.intersect(p, range)) {
					status = new ExistenceResults<PeriodeImposition>(TacheStatus.INTERSECTE, p);
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
	protected ExistenceResults<DeclarationImpotOrdinaire> checkExistenceDeclaration(final Contribuable contribuable, final PeriodeImposition periode) {

		ExistenceResults<DeclarationImpotOrdinaire> status = null;

		final Set<Declaration> declarations = contribuable.getDeclarations();
		if (declarations != null) {
			for (Declaration d : declarations) {
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (DateRangeHelper.equals(d, periode)) {
					status = new ExistenceResults<DeclarationImpotOrdinaire>(TacheStatus.EXISTE_DEJA, di);
				}
				else if (DateRangeHelper.intersect(d, periode)) {
					status = new ExistenceResults<DeclarationImpotOrdinaire>(TacheStatus.INTERSECTE, di);
					break; // inutile de continuer
				}
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
	protected ExistenceResults<TacheEnvoiDeclarationImpot> checkExistenceTache(Contribuable contribuable, DateRange range) {

		ExistenceResults<TacheEnvoiDeclarationImpot> status = null;

		TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
		criterion.setAnnee(range.getDateDebut().year());
		criterion.setContribuable(contribuable);
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE); // [UNIREG-1984] on ignore les tâches déjà traitées

		final List<Tache> list = tacheDAO.find(criterion, true /* don't flush */);
		for (Tache t : list) {
			final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) t;

			if (DateRangeHelper.equals(tache, range)) {
				status = new ExistenceResults<TacheEnvoiDeclarationImpot>(TacheStatus.EXISTE_DEJA, tache);
			}
			else if (DateRangeHelper.intersect(tache, range)) {
				status = new ExistenceResults<TacheEnvoiDeclarationImpot>(TacheStatus.INTERSECTE, tache);
				break; // inutile de continuer
			}
		}

		return status;
	}

	final private static String queryIdsCtbWithFors = // --------------------------------
	"SELECT DISTINCT                                                                         "
			+ "    cont.id                                                                   "
			+ "FROM                                                                          "
			+ "    Contribuable AS cont                                                      "
			+ "INNER JOIN                                                                    "
			+ "    cont.forsFiscaux AS fors                                                  "
			+ "WHERE                                                                         "
			+ "    cont.annulationDate IS null                                               "
			+ "    AND fors.annulationDate IS null                                           "
			+ "    AND fors.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'                   "
			+ "    AND (fors.class = ForFiscalPrincipal OR fors.class = ForFiscalSecondaire) "
			+ "    AND (fors.modeImposition IS null OR fors.modeImposition != 'SOURCE')      "
			+ "    AND (fors.dateDebut IS null OR fors.dateDebut <= :finAnnee)               "
			+ "    AND (fors.dateFin IS null OR fors.dateFin >= :debutAnnee)                 " // [UNIREG-1742] for actif n'importe quand dans l'année
			+ "ORDER BY cont.id ASC                                                          ";

	final private static String queryIdsCtbWithTasks = // ------------------
	"SELECT DISTINCT                                                             "
			+ "    tache.contribuable.id                                         "
			+ "FROM                                                              "
			+ "    TacheEnvoiDeclarationImpot AS tache                           "
			+ "WHERE                                                             "
			+ "    tache.annulationDate IS null                                  "
			+ "    AND tache.etat = 'EN_INSTANCE'                                "
			+ "    AND (tache.dateDebut IS null OR tache.dateDebut <= :finAnnee) "
			+ "    AND (tache.dateFin IS null OR tache.dateFin >= :debutAnnee)   "
			+ "ORDER BY tache.contribuable.id ASC                                ";

	final private static String queryIdsCtbWithDeclarations = // -------------
	"SELECT DISTINCT                                                           "
			+ "    decl.tiers.id                                               "
			+ "FROM                                                            "
			+ "    DeclarationImpotOrdinaire AS decl                           "
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
	@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
	protected List<Long> createListeIdsContribuables(final int annee) {

		final RegDate debutAnnee = RegDate.get(annee, 1, 1);
		final RegDate finAnnee = RegDate.get(annee, 12, 31);


		final List<Long> idsFors = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryIdsCtbWithFors);
				queryObject.setParameter("debutAnnee", debutAnnee.index());
				queryObject.setParameter("finAnnee", finAnnee.index());
				return queryObject.list();
			}
		});

		final List<Long> idsTasks = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryIdsCtbWithTasks);
				queryObject.setParameter("debutAnnee", debutAnnee.index());
				queryObject.setParameter("finAnnee", finAnnee.index());
				return queryObject.list();
			}
		});

		final List<Long> idsDIs = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryIdsCtbWithDeclarations);
				queryObject.setParameter("debutAnnee", debutAnnee.index());
				queryObject.setParameter("finAnnee", finAnnee.index());
				return queryObject.list();
			}
		});

		final Set<Long> set = new HashSet<Long>(idsFors.size() + idsTasks.size() + idsDIs.size());
		set.addAll(idsFors);
		set.addAll(idsTasks);
		set.addAll(idsDIs);

		final List<Long> ids = new ArrayList<Long>(set);
		Collections.sort(ids);

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


	/**
	 * Uniquement pour le testing.
	 *
	 * @param rapport le rapport à utiliser
	 */
	protected void setRapport(DeterminationDIsResults rapport) {
		this.rapport = rapport;
	}
}

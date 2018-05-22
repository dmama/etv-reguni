package ch.vd.unireg.declaration.ordinaire.pm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TacheHelper;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;
import ch.vd.unireg.validation.ValidationService;

public class DeterminationDIsPMAEmettreProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeterminationDIsPMAEmettreProcessor.class);

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

	public DeterminationDIsPMAEmettreProcessor(HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO, TacheDAO tacheDAO, ParametreAppService parametres,
	                                           TiersService tiersService, PlatformTransactionManager transactionManager,
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

	public DeterminationDIsPMResults run(final int periodeFiscale, final RegDate dateTraitement, final int nbThreads, StatusManager s) throws DeclarationException {
		final StatusManager status = s != null ? s : new LoggingStatusManager(LOGGER);

		// une petite vérification préalable
		checkParams(periodeFiscale, dateTraitement);

		// récupération des identifiants des contribuables concernés
		status.setMessage("Récupération des contribuables à traiter...");
		final List<Long> ids = createListeIdsContribuables();

		// traitements par lots
		final DeterminationDIsPMResults rapportFinal = new DeterminationDIsPMResults(periodeFiscale, dateTraitement, nbThreads, tiersService, adresseService);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, DeterminationDIsPMResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, DeterminationDIsPMResults>() {
			@Override
			public DeterminationDIsPMResults createSubRapport() {
				return new DeterminationDIsPMResults(periodeFiscale, dateTraitement, nbThreads, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, DeterminationDIsPMResults rapport) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, periodeFiscale, dateTraitement, rapport);
				return true;
			}
		}, progressMonitor);

		// message de fin
		final int count = rapportFinal.traites.size();
		if (status.isInterrupted()) {
			status.setMessage("La création des tâches d'envoi des déclarations d'impôt PM a été interrompue."
					                  + " Nombre de nouvelles tâches en instance créées au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La création des tâches d'envoi des déclarations d'impôt PM est terminée."
					                  + " Nombre de nouvelles tâches en instance créées = " + count + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		// récapitulation et fin
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(final List<Long> batch, int periodeFiscale, RegDate dateTraitement, DeterminationDIsPMResults rapport) throws DeclarationException {
		// Récupère la période fiscale
		final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(periodeFiscale);
		if (periode == null) {
			throw new DeclarationException("La période fiscale " + periodeFiscale + " n'existe pas dans la base de données.");
		}

		// On charge tous les contribuables en vrac (avec préchargement des déclarations)
		final List<Entreprise> list = hibernateTemplate.execute(new HibernateCallback<List<Entreprise>>() {
			@Override
			public List<Entreprise> doInHibernate(Session session) throws HibernateException {
				final Criteria crit = session.createCriteria(Entreprise.class);
				crit.add(Restrictions.in("numero", batch));
				crit.setFetchMode("declarations", FetchMode.JOIN); // force le préchargement
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				//noinspection unchecked
				return crit.list();
			}
		});

		// Traitement de tous les contribuables
		for (Entreprise ctb : list) {
			traiterEntreprise(ctb, periode, dateTraitement, rapport);
		}
	}

	protected void traiterEntreprise(Entreprise entreprise, PeriodeFiscale periodeFiscale, RegDate dateTraitement, DeterminationDIsPMResults rapport) {
		++rapport.nbCtbsTotal;

		// une entreprise ne va pas devenir valide au prétexte qu'une nouvelle tâche lui est associée
		if (validationService.validate(entreprise).hasErrors()) {
			rapport.addErrorCtbInvalide(entreprise);
			return;
		}

		try {
			// extraction des périodes d'imposition à considérer pour la génération des tâches d'envoi
			final TraitementPeriodesImpositionData periodesATraiter = extrairePeriodesImpositionATraiter(entreprise, periodeFiscale.getAnnee(), dateTraitement, rapport);

			// ... et finalement il faut traiter les périodes élues
			for (PeriodeImpositionPersonnesMorales pi : periodesATraiter.aTraiter) {
				traiterPeriodeImposition(entreprise, pi, dateTraitement, rapport);
			}

			// [UNIREG-1742] rattrapage des déclarations et des tâches non-valides
			verifierValiditeDeclarations(entreprise, periodeFiscale, periodesATraiter, rapport);
			verifierValiditeTachesEnvoi(entreprise, periodeFiscale, periodesATraiter, rapport);
		}
		catch (AssujettissementException e) {
			rapport.addCtbErrorDonneesIncoherentes(entreprise, e.getMessage() + ". Aucune tâche générée.");
		}
	}

	/**
	 * Container des périodes d'imposition à traiter et des périodes d'imposition calculées mais
	 * qui ne sont pas à traiter car la date de traitement est avant la date de fin de la période,
	 * sur la bonne PF cependant
	 */
	static final class TraitementPeriodesImpositionData {

		static final TraitementPeriodesImpositionData EMPTY = new TraitementPeriodesImpositionData(null, null);

		/**
		 * Périodes d'imposition à traiter (bonne PF, bonne date de fin)
		 */
		@NotNull
		final List<PeriodeImpositionPersonnesMorales> aTraiter;

		/**
		 * Périodes d'imposition sur la bonne PF mais dont la date de fin est postérieure (ou égale) à la date de traitement (= à ignorer)
		 */
		@NotNull
		final List<PeriodeImpositionPersonnesMorales> aIgnorer;

		/**
		 * Constructeur
		 * @param aTraiter Périodes d'imposition à traiter (bonne PF, bonne date de fin)
		 * @param aIgnorer Périodes d'imposition sur la bonne PF mais dont la date de fin est postérieure (ou égale) à la date de traitement (= à ignorer)
		 */
		public TraitementPeriodesImpositionData(List<PeriodeImpositionPersonnesMorales> aTraiter, List<PeriodeImpositionPersonnesMorales> aIgnorer) {
			this.aTraiter = aTraiter == null ? Collections.emptyList() : aTraiter;
			this.aIgnorer = aIgnorer == null ? Collections.emptyList() : aIgnorer;
		}
	}

	@NotNull
	protected TraitementPeriodesImpositionData extrairePeriodesImpositionATraiter(Entreprise entreprise, int pf, RegDate dateTraitement, DeterminationDIsPMResults rapport) throws AssujettissementException {

		// calculons toutes les périodes d'imposition de l'entreprise
		final List<PeriodeImposition> pis = periodeImpositionService.determine(entreprise);
		if (pis == null || pis.isEmpty()) {
			rapport.addIgnorePasAssujetti(entreprise);
			return TraitementPeriodesImpositionData.EMPTY;
		}

		// on se ramène à la période cherchée
		final DateRange anneeCivile = new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31));
		boolean anneeCivileVue = false;
		final List<PeriodeImpositionPersonnesMorales> periodesATraiter = new ArrayList<>(pis.size());
		final List<PeriodeImpositionPersonnesMorales> periodesBonnePfAIgnorer = new ArrayList<>(pis.size());
		for (PeriodeImposition pi : pis) {
			// pour être candidate au traitement, une période doit correspondre à période fiscale choisie et
			// être terminée avant la date de traitement (nous ne générons pas de tâche pour les DI libres...)
			if (pi.getPeriodeFiscale() == pf) {
				if (dateTraitement.isAfter(pi.getDateFin())) {
					periodesATraiter.add((PeriodeImpositionPersonnesMorales) pi);
				}
				else {
					anneeCivileVue = true;
					periodesBonnePfAIgnorer.add((PeriodeImpositionPersonnesMorales) pi);
				}
			}
			else if (!anneeCivileVue) {
				// pour distinguer entre le cas où l'assujettissement de l'entreprise n'intersecte pas du tout
				// l'année civile de la période fiscale choisie du cas où l'assujettissement existe bien cette
				// année là mais, en raison de l'absence de bouclement, l'entreprise n'a pas de période d'imposition
				// relative à la période fiscale correspondante.
				anneeCivileVue = DateRangeHelper.intersect(anneeCivile, pi);
			}
		}

		// rien dans la période fiscale ?
		if (periodesATraiter.isEmpty()) {
			if (anneeCivileVue) {
				rapport.addIgnoreSansBouclement(entreprise);
			}
			else {
				rapport.addIgnorePasAssujetti(entreprise);
			}
		}

		return new TraitementPeriodesImpositionData(periodesATraiter, periodesBonnePfAIgnorer);
	}

	/**
	 * Vérifie les déclarations préexistantes, et dans le cas où elles ne correspondent pas exactement à une période calculée, génère une tâche d'annulation.
	 *
	 * @param entreprise     le contribuable concerné
	 * @param periodeFiscale la période fiscale concernée
	 * @param data           les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param rapport        données du rapport d'exécution du job
	 */
	private void verifierValiditeDeclarations(Entreprise entreprise,
	                                          PeriodeFiscale periodeFiscale,
	                                          TraitementPeriodesImpositionData data,
	                                          DeterminationDIsPMResults rapport) {

		// TODO attention : pour les PM, la PF n'est pas nécessairement terminée quand on lance ce job...
		// (il peut y avoir, en particulier si on jour avec la date de traitement, des DI existantes pour la PF choisie mais
		// qui n'apparaissent pas dans la liste des périodes d'imposition car celles-ci ont été éliminées en raison de leur position
		// relative à la date de traitement)

		final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, false);
		if (!declarations.isEmpty()) {
			for (DeclarationImpotOrdinairePM di : declarations) {
				if (di.getPeriode().getAnnee().equals(periodeFiscale.getAnnee())) {
					verifierValiditeDeclaration(entreprise, data, di, rapport);
				}
			}
		}
	}

	/**
	 * Vérifie la déclaration spécifiée, et dans le cas où elle ne correspond pas exactement à une période calculée, génère une tâche d'annulation.
	 *
	 * @param entreprise le contribuable concerné
	 * @param data       les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param di         la déclaration d'impôt ordinaire à vérifier
	 * @param rapport    données du rapport d'exécution du job
	 */
	private void verifierValiditeDeclaration(Entreprise entreprise,
	                                         TraitementPeriodesImpositionData data,
	                                         DeclarationImpotOrdinaire di,
	                                         DeterminationDIsPMResults rapport) {

		final ExistenceResults<PeriodeImpositionPersonnesMorales> results = checkExistencePeriode(data, di);
		if (results == null) {

			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			criterion.setDeclarationAnnulee(di);
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);

			if (tacheDAO.count(criterion, true /* don't flush */) == 0) {

				final CollectiviteAdministrative oid = tiersService.getOfficeImpot(ServiceInfrastructureService.noOIPM);

				// [UNIREG-1742] pas de période pour la déclaration => on crée une tâche d'annulation
				final TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, null, entreprise, di, oid);
				rapport.addTacheAnnulationCreee(entreprise, tache);
				tacheDAO.save(tache);
			}
			else {
				// [UNIREG-1981] on ne crée pas de tâche d'annulation s'il en existe déjà une
				rapport.addIgnoreTacheAnnulationDejaExistante(entreprise);
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
			case A_IGNORER:
				// surtout ne rien faire !!
				break;
			default:
				throw new IllegalArgumentException("Type de résultat inconnu = [" + results.status + ']');
			}
		}
	}

	/**
	 * Vérifie les tâches d'envoi de déclaration préexistantes, et dans le cas où elles ne correspondent pas exactement à une période calculée, elles sont annulées.
	 *
	 * @param entreprise     le contribuable concerné
	 * @param periodeFiscale la période fiscale concernée
	 * @param data           les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param rapport        données du rapport d'exécution du job
	 */
	private void verifierValiditeTachesEnvoi(Entreprise entreprise,
	                                         PeriodeFiscale periodeFiscale,
	                                         TraitementPeriodesImpositionData data,
	                                         DeterminationDIsPMResults rapport) {

		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPM);
		criterion.setAnnee(periodeFiscale.getAnnee());
		criterion.setContribuable(entreprise);
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE); // [UNIREG-1981] on ignore les tâches déjà traitées

		final List<Tache> taches = tacheDAO.find(criterion, true /* don't flush */);
		if (taches != null && !taches.isEmpty()) {
			for (Tache t : taches) {
				final TacheEnvoiDeclarationImpotPM te = (TacheEnvoiDeclarationImpotPM) t;
				if (!te.isAnnule()) {
					verifierValiditeTacheEnvoi(entreprise, data, te, rapport);
				}
			}
		}
	}

	/**
	 * Vérifie la tâche d'envoi de déclaration spécifiée, et dans le cas où elle ne correspond pas exactement à une période calculée, elle est annulée.
	 *
	 * @param entreprise le contribuable concerné
	 * @param data       les périodes d'imposition calculées pour le contribuable et la période fiscale concernée
	 * @param tache      la tache d'envoi de déclaration à vérifier
	 * @param rapport    données du rapport d'exécution du job
	 */
	private void verifierValiditeTacheEnvoi(Entreprise entreprise,
	                                        TraitementPeriodesImpositionData data,
	                                        TacheEnvoiDeclarationImpotPM tache,
	                                        DeterminationDIsPMResults rapport) {

		final ExistenceResults<PeriodeImpositionPersonnesMorales> results = checkExistencePeriode(data, tache);
		if (results == null) {
			// [UNIREG-1742] pas de période pour la tâche => on l'annule
			tache.setAnnule(true);
			rapport.addTacheEnvoiAnnulee(entreprise, tache);
		}
		else {
			switch (results.status) {
			case EXISTE_DEJA:
				// tout est ok, il y a bien une période qui correspond à la déclaration
				break;
			case INTERSECTE:
				// [UNIREG-1984] la tâche ne corresponds pas à la période d'imposition calculée, on l'annule
				tache.setAnnule(true);
				rapport.addTacheEnvoiAnnulee(entreprise, tache);
				break;
			case A_IGNORER:
				// surtout, ne rien faire !!
				break;
			default:
				throw new IllegalArgumentException("Type de résultat inconnu = [" + results.status + ']');
			}
		}
	}

	/**
	 * @param pi une des périodes d'imposition de l'entreprise
	 * @param rapport le rapport d'exécution du job
	 * @return si oui ou non la période d'imposition devrait correspondre à une déclaration d'impôt
	 */
	protected boolean needsDeclaration(PeriodeImpositionPersonnesMorales pi, DeterminationDIsPMResults rapport) {

		// certaines de ces périodes sont optionnelles...
		if (pi.isDeclarationOptionnelle()) {
			rapport.addIgnoreDeclarationOptionnelle(pi.getContribuable());
			return false;
		}

		// ou remplacées par des notes (ça existe, ça, pour les PM ?)
		if (pi.isDeclarationRemplaceeParNote()) {
			rapport.addIgnoreDeclarationRemplaceeParNote(pi.getContribuable());
			return false;
		}

		return true;
	}

	/**
	 * Traite la période d'imposition spécifiée et crée une tâche en instance si possible.
	 *
	 * @param entreprise le tiers dont on envoie la déclaration
	 * @param pi         les détails précalculés du contribuable
	 * @param dateTraitement date de traitement
	 * @param rapport    données du rapport d'exécution du job
	 * @return la tâche créée; ou <b>null</i> si une erreur a été détectée
	 */
	@Nullable
	protected TacheEnvoiDeclarationImpotPM traiterPeriodeImposition(Entreprise entreprise,
	                                                                PeriodeImpositionPersonnesMorales pi,
	                                                                RegDate dateTraitement,
	                                                                DeterminationDIsPMResults rapport) {

		// pas la peine d'aller plus loin si la période d'imposition ne génère pas de DI obligatoire
		if (!needsDeclaration(pi, rapport)) {
			return null;
		}

		// Vérifie qu'une déclaration d'impôt n'existe pas déjà
		final ExistenceResults<DeclarationImpotOrdinaire> checkDI = checkExistenceDeclaration(entreprise, pi);
		if (checkDI != null) {
			switch (checkDI.status) {
			case EXISTE_DEJA:
				// une déclaration existe déjà, rien à faire
				if (rapport != null) {
					rapport.addIgnoreDeclarationDejaExistante(entreprise);
				}
				break;
			case INTERSECTE:
				// une autre déclaration existe qui occupe partiellement le range, il y a un problème
				if (rapport != null) {
					final String message = "Déclaration d'impôt [id=" + checkDI.object.getId() + ", début=" + checkDI.object.getDateDebut() + ", fin=" +
							checkDI.object.getDateFin() + "]. Période d'imposition calculée [début=" + pi.getDateDebut() + ", fin=" + pi.getDateFin() + ']';
					rapport.addErrorDeclarationCollision(entreprise, message);
				}
				break;
			case A_IGNORER:
			default:
				throw new NotImplementedException();
			}
			return null;
		}

		// Vérifie qu'une tâche n'existe pas déjà
		final ExistenceResults<TacheEnvoiDeclarationImpotPM> checkTache = checkExistenceTache(entreprise, pi);
		if (checkTache != null) {
			switch (checkTache.status) {
			case EXISTE_DEJA:
				// [SIFISC-17232] une tâche avec les mêmes dates existe déjà... reste à voir si les autres données sont les mêmes
				if (checkTache.getObject().getTypeDocument() == pi.getTypeDocumentDeclaration()) {
					// la bonne tâche existe déjà, rien à faire
					if (rapport != null) {
						rapport.addIgnoreTacheEnvoiDejaExistante(entreprise);
					}

					// éventuelle correction de la catégorie d'entreprise
					if (checkTache.getObject().getCategorieEntreprise() != pi.getCategorieEntreprise() && pi.getCategorieEntreprise() != null) {
						checkTache.getObject().setCategorieEntreprise(pi.getCategorieEntreprise());
					}
					return null;
				}
				else {
					// le type de document n'est pas le bon : on annule la tâche existante et on continue
					checkTache.getObject().setAnnule(true);
					break;
				}
			case INTERSECTE:
				// [UNIREG-1984] une autre tâche occupe partiellement le range -> elle sera annulée par le post-processing de vérification des tâches, on continue donc.
				break;
			case DEJA_TRAITE:
				//Rien a faire dans ce cas la tâche sera de nouveau générée
				break;
			case A_IGNORER:
				// surtout, ne rien faire..,
				break;
			default:
				throw new IllegalArgumentException("Le statut de tâche [" + checkTache.status + "] est inconnu.");
			}
		}

		final CollectiviteAdministrative oid = tiersService.getOfficeImpot(ServiceInfrastructureService.noOIPM);
		if (oid == null) {
			throw new IllegalArgumentException();
		}

		// Création et sauvegarde de la tâche en base
		final ExerciceCommercial exercice = pi.getExerciceCommercial();
		final RegDate dateEcheance = TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, pi.getTypeContribuable(), dateTraitement, pi.getDateFin());
		final TacheEnvoiDeclarationImpotPM tache = new TacheEnvoiDeclarationImpotPM(TypeEtatTache.EN_INSTANCE, dateEcheance,
		                                                                            entreprise, pi.getDateDebut(), pi.getDateFin(),
		                                                                            exercice.getDateDebut(), exercice.getDateFin(),
		                                                                            pi.getTypeContribuable(), pi.getTypeDocumentDeclaration(),
		                                                                            tiersService.getCategorieEntreprise(entreprise, pi.getDateFin()), oid);
		if (rapport != null) {
			rapport.addTacheEnvoiCreee(entreprise, tache);
		}

		return (TacheEnvoiDeclarationImpotPM) tacheDAO.save(tache);
	}

	/**
	 * Résultat de la recherche de l'existence d'un objet dans une période spécifiée.
	 */
	protected static class ExistenceResults<T extends DateRange> {

		protected enum TacheStatus {
			EXISTE_DEJA, INTERSECTE, DEJA_TRAITE, A_IGNORER
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

	protected ExistenceResults<PeriodeImpositionPersonnesMorales> checkExistencePeriode(TraitementPeriodesImpositionData data, DateRange range) {

		ExistenceResults<PeriodeImpositionPersonnesMorales> status = null;

		for (PeriodeImpositionPersonnesMorales p : data.aTraiter) {
			if (DateRangeHelper.equals(p, range)) {
				status = new ExistenceResults<>(ExistenceResults.TacheStatus.EXISTE_DEJA, p);
			}
			else if (DateRangeHelper.intersect(p, range)) {
				status = new ExistenceResults<>(ExistenceResults.TacheStatus.INTERSECTE, p);
				break; // inutile de continuer
			}
		}

		// potentiellement la déclaration colle avec une PF existante mais ignorée (pour cause de date de traitement antérieure à la date de fin de la période)
		if (status == null) {
			for (PeriodeImpositionPersonnesMorales p : data.aIgnorer) {
				if (DateRangeHelper.intersect(p, range)) {
					status = new ExistenceResults<>(ExistenceResults.TacheStatus.A_IGNORER, p);
					break;
				}
			}
		}

		return status;
	}

	/**
	 * Vérifie la présence d'une déclaration pré-existante pour le contribuable et la période spécifiée.
	 *
	 * @param entreprise un contribuable
	 * @param periode    la période
	 * @return <b>null</b> si aucune déclaraiton n'existe, ou un résultat détaillé si une déclaration préexistente a été détectée.
	 */
	protected ExistenceResults<DeclarationImpotOrdinaire> checkExistenceDeclaration(final Entreprise entreprise,
	                                                                                final PeriodeImpositionPersonnesMorales periode) {

		ExistenceResults<DeclarationImpotOrdinaire> status = null;

		// [UNIREG-1417] : ne pas tenir compte des DI annulées...
		final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, false);
		for (DeclarationImpotOrdinaire di : declarations) {
			if (DateRangeHelper.equals(di, periode)) {
				status = new ExistenceResults<>(ExistenceResults.TacheStatus.EXISTE_DEJA, di);
			}
			else if (DateRangeHelper.intersect(di, periode)) {
				status = new ExistenceResults<>(ExistenceResults.TacheStatus.INTERSECTE, di);
				break; // inutile de continuer
			}
		}

		return status;
	}

	/**
	 * Vérifie la présence d'une tâche pré-existante pour le contribuable et la période spécifiée.
	 *
	 * @param entreprise un contribuable
	 * @param range      la période
	 * @return <b>null</b> si aucune tâche n'existe, ou un résultat détaillé si un tâche préexistente a été détectée.
	 */
	protected ExistenceResults<TacheEnvoiDeclarationImpotPM> checkExistenceTache(Entreprise entreprise, DateRange range) {

		ExistenceResults<TacheEnvoiDeclarationImpotPM> status = null;

		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPM);
		criterion.setAnnee(range.getDateFin().year());
		criterion.setContribuable(entreprise);
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE); // [UNIREG-1984] on ignore les tâches déjà traitées

		final List<Tache> list = tacheDAO.find(criterion, true /* don't flush */);
		for (Tache t : list) {
			final TacheEnvoiDeclarationImpotPM tache = (TacheEnvoiDeclarationImpotPM) t;

			if (DateRangeHelper.equals(tache, range)) {
				if (TypeEtatTache.TRAITE == tache.getEtat()) {
					//[UNIREG-1417] une tache trouvée à l'etat TRAITEE indique qu'une di a été emise
					//mais qu'elle a été annulée par la suite. Il faut de nouveau l'emettre vu  que le contribuable
					//concerné à été assujetti sur la période fiscale
					status = new ExistenceResults<>(ExistenceResults.TacheStatus.DEJA_TRAITE, tache);
				}
				else {
					status = new ExistenceResults<>(ExistenceResults.TacheStatus.EXISTE_DEJA, tache);
				}
			}
			else if (DateRangeHelper.intersect(tache, range)) {
				status = new ExistenceResults<>(ExistenceResults.TacheStatus.INTERSECTE, tache);
				break; // inutile de continuer
			}
		}

		return status;
	}


	/**
	 * Récupération des identifiants des contribuables concernés par une éventuelle DI PM
	 *
	 * @return une liste d'identifiants de contribuables PM
	 */
	protected List<Long> createListeIdsContribuables() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						final String hql = "select distinct ff.tiers.id from ForFiscalRevenuFortune as ff where ff.tiers.class in (Entreprise) and ff.annulationDate is null and ff.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD' and ff.genreImpot = 'BENEFICE_CAPITAL' order by ff.tiers.id";
						final Query query = session.createQuery(hql);
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});
	}

	/**
	 * Vérification des paramètres du job
	 *
	 * @param anneePeriodeFiscale l'année de la période fiscale choisie
	 * @param dateTraitement      la date de traitement
	 * @throws DeclarationException en cas d'incohérence ou de valeur invalide
	 */
	private void checkParams(final int anneePeriodeFiscale, RegDate dateTraitement) throws DeclarationException {

		// laissons l'histoire aux vieilles applications
		final int premierePFautorisee = parametres.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		if (anneePeriodeFiscale < premierePFautorisee) {
			throw new DeclarationException(String.format("La période fiscale %d est antérieure à la première période fiscale d'envoi de DI PM par Unireg [%d].",
			                                             anneePeriodeFiscale, premierePFautorisee));
		}

		// il faudrait tout de même que la période fiscale ait une chance, i.e. soit commencée au moins
		if (anneePeriodeFiscale > dateTraitement.year()) {
			throw new DeclarationException(String.format("La période fiscale %d n'est même pas commencée à la date de traitement [%s].",
			                                             anneePeriodeFiscale,
			                                             RegDateHelper.dateToDisplayString(dateTraitement)));
		}

		// et on vérifie quand-même que la période fiscale existe en base
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);
			template.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// Récupère la période fiscale
					final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriodeFiscale);
					if (periode == null) {
						throw new RuntimeException(String.format("La période fiscale %d n'existe pas dans la base de données.", anneePeriodeFiscale));
					}
				}
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}

	}
}

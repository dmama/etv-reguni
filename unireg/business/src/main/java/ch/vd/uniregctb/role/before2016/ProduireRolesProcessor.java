package ch.vd.uniregctb.role.before2016;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.DecompositionFors;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsPeriode;
import ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement;
import ch.vd.uniregctb.metier.assujettissement.SourcierMixte;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.role.before2016.InfoContribuable.TypeAssujettissement;
import ch.vd.uniregctb.role.before2016.InfoContribuable.TypeContribuable;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Processeur spécialisé dans la production des rôles pour les communes vaudoises. Ce processeur doit être appelé par le service 'rôle'
 * uniquement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ProduireRolesProcessor {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ProduireRolesProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final ServiceInfrastructureService infraService;
	private final TiersDAO tiersDAO;
	private final PlatformTransactionManager transactionManager;
	private final AdresseService adresseService;
	private final TiersService tiersService;
	private final ServiceCivilService serviceCivilService;
	private final ValidationService validationService;
	private final AssujettissementService assujettissementService;

	public ProduireRolesProcessor(HibernateTemplate hibernateTemplate, ServiceInfrastructureService infraService, TiersDAO tiersDAO, PlatformTransactionManager transactionManager,
	                              AdresseService adresseService, TiersService tiersService, ServiceCivilService serviceCivilService, ValidationService validationService,
	                              AssujettissementService assujettissementService) {
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.tiersDAO = tiersDAO;
		this.transactionManager = transactionManager;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.serviceCivilService = serviceCivilService;
		this.validationService = validationService;
		this.assujettissementService = assujettissementService;
	}

	private interface GroupementCommunes {
		boolean isDansGroupementDeCommunes(Integer ofsCandidat, Integer ofsReference);
		Set<Integer> getCommunes(Integer ofsReference);
	}

	private static class CommuneParCommune implements GroupementCommunes {
		@Override
		public boolean isDansGroupementDeCommunes(Integer ofsCandidat, Integer ofsReference) {
			return ofsCandidat != null && ofsCandidat.equals(ofsReference);
		}

		@Override
		public Set<Integer> getCommunes(Integer ofsReference) {
			return Collections.singleton(ofsReference);
		}
	}

	private static class GroupementCommunesPourOfficeImpot implements GroupementCommunes {
		private final Set<Integer> ofsCommunes;

		private GroupementCommunesPourOfficeImpot(Set<Integer> ofsCommunes) {
			this.ofsCommunes = ofsCommunes;
		}

		@Override
		public boolean isDansGroupementDeCommunes(Integer ofsCandidat, Integer ofsReference) {
			return ofsCommunes.contains(ofsCandidat) && ofsCommunes.contains(ofsReference);
		}

		@Override
		public Set<Integer> getCommunes(Integer ofsReference) {
			return ofsCommunes.contains(ofsReference) ? Collections.unmodifiableSet(ofsCommunes) : Collections.emptySet();
		}
	}

	/**
	 * Interface implémentée par les variantes de la production des rôles (tous, pour un OID, pour une commune)
	 */
	private interface VarianteProductionRole<T extends ProduireRolesResults> {
		/**
		 * Renvoie la liste des ID techniques des contribuables listés dans la variante concernée
		 */
		List<Long> getIdsContribuablesConcernes(int anneePeriode);

		/**
		 * Instancie un nouveau rapport (intermédiaire et final)
		 */
		T creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal);

		/**
		 * Retourne le groupement de communes à utiliser pour la recherche de fors historiques
		 */
		GroupementCommunes getGroupementCommunes();

		/**
		 * Détermine si un contribuable sera pris en compte dans le traitement (peut être utile si des calculs
		 * non-négligeables sont nécessaires - et carrément impossibles en base - pour déterminer si un contribuable
		 * doit apparaître dans les rôles)
		 */
		boolean isContribuableInteressant(Contribuable ctb, int anneePeriode) throws ServiceException;
	}

	/**
	 * Factorisation du code de processing indépendant de la variante
	 * @param anneePeriode période fiscale pour les rôles
	 * @param statusMessagePrefix [optionnel] préfixe utilisable pour le message d'avancement
	 * @param status status manager
	 * @param progressCalculator object qui pourra calculer le taux de progression à afficher dans les messages d'avancement
	 * @param variante variante de construction des résultats
	 * @return un rapport (technique) sur les rôles demandés
	 */
	private <T extends ProduireRolesResults<T>> T doRun(final int anneePeriode, final int nbThreads, @Nullable String statusMessagePrefix,
	                                                 final StatusManager status, final ProgressCalculator progressCalculator, final VarianteProductionRole<T> variante) {

		final RegDate today = RegDate.get();
		final T rapportFinal = variante.creerRapport(anneePeriode, nbThreads, today, true);

		// parties à aller chercher en bloc par groupe de tiers
		final Set<TiersDAO.Parts> parts = EnumSet.of(TiersDAO.Parts.FORS_FISCAUX,
		                                             TiersDAO.Parts.RAPPORTS_ENTRE_TIERS,
		                                             TiersDAO.Parts.ADRESSES,
		                                             TiersDAO.Parts.DECLARATIONS);

		// groupement de communes (utilisé pour calculer la date du premier for, et la date du dernier)
		final GroupementCommunes groupement = variante.getGroupementCommunes();

		// préfixe pour les différentes phases du batch (s'il y en a)
		final String prefixe = StringUtils.isBlank(statusMessagePrefix) ? StringUtils.EMPTY : String.format("%s. ", statusMessagePrefix);

		final String msgRechercheContribuables = String.format("%sRecherche des contribuables.", prefixe);
		status.setMessage(msgRechercheContribuables, progressCalculator.getProgressPercentage(0, 0));

		final List<Long> list = variante.getIdsContribuablesConcernes(anneePeriode);
		final ParallelBatchTransactionTemplateWithResults<Long, T>
				template = new ParallelBatchTransactionTemplateWithResults<>(list, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.setReadonly(true);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, T>() {

			@Override
			public boolean doInTransaction(List<Long> batch, T rapport) throws Exception {

				final List<Tiers> tierz = tiersDAO.getBatch(batch, parts);

				final int size = list.size();
				final int ctbsTraites = rapportFinal.ctbsTraites;
				final String msg;
				if (progressCalculator instanceof DifferenciatingProgressCalculator) {
					final int localPercent = ((DifferenciatingProgressCalculator) progressCalculator).getLocalProgressPercentage(ctbsTraites, size);
					msg = String.format("%sTraitement des contribuables (%d traité(s), %d%%).", prefixe, ctbsTraites, localPercent);
				}
				else {
					msg = String.format("%sTraitement des contribuables (%d traité(s)).", prefixe, ctbsTraites);
				}
				status.setMessage(msg, progressCalculator.getProgressPercentage(ctbsTraites, size));

				// première boucle sur les tiers pour aller chercher en un bloc les individus du civil
				// pour les habitants (nom, adresse, no-avs...)
				preloadIndividus(tierz, anneePeriode);

				// deuxième boucle pour le traitement proprement dit
				for (Tiers tiers : tierz) {

					final long ctbId = tiers.getNumero();
					final Contribuable ctb = (Contribuable) tiers;

					try {
						if (variante.isContribuableInteressant(ctb, anneePeriode)) {
							processContribuable(anneePeriode, rapport, groupement, ctb);
						}
					}
					catch (Exception e) {
						final String msgException = String.format("Exception levée lors du traitement du contribuable %s", FormatNumeroHelper.numeroCTBToDisplay(ctbId));
						LOGGER.error(msgException, e);
						rapport.addErrorException(ctb, e);
					}

					if (status.interrupted()) {
						break;
					}
				}
				return !status.interrupted();
			}

			@Override
			public T createSubRapport() {
				return variante.creerRapport(anneePeriode, nbThreads, today, false);
			}
		}, null);

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();

		return rapportFinal;
	}

	private void preloadIndividus(List<Tiers> tierz, int anneePeriode) {
		final Map<Long, PersonnePhysique> ppByNoIndividu = new HashMap<>(tierz.size() * 2);
		final RegDate date = RegDate.get(anneePeriode, 12, 31);
		for (Tiers tiers : tierz) {
			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				if (pp.isConnuAuCivil()) {
					final Long noIndividu = pp.getNumeroIndividu();
					ppByNoIndividu.put(noIndividu, pp);
				}
			}
			else if (tiers instanceof MenageCommun) {
				final Set<PersonnePhysique> membres = tiersService.getComposantsMenage((MenageCommun) tiers, date);
				if (membres != null) {
					for (PersonnePhysique pp : membres) {
						if (pp.isConnuAuCivil()) {
							ppByNoIndividu.put(pp.getNumeroIndividu(), pp);
						}
					}
				}
			}
		}

		if (!ppByNoIndividu.isEmpty()) {
			// remplit le cache des individus...
			try {
				final List<Individu> individus = serviceCivilService.getIndividus(ppByNoIndividu.keySet(), null, AttributeIndividu.ADRESSES);

				// et on remplit aussi le cache individu sur les personnes physiques... (utilisé pour l'accès à la date de décès et au sexe)
				for (Individu individu : individus) {
					final PersonnePhysique pp = ppByNoIndividu.get(individu.getNoTechnique());
					pp.setIndividuCache(individu);
				}
			}
			catch (ServiceCivilException e) {
				LOGGER.error("Impossible de précharger le lot d'individus [" + ppByNoIndividu.keySet() + "]. L'erreur est : " + e.getMessage());
			}
		}
	}

	/**
	 * Produit la liste de contribuables PP de toutes les communes (et fractions de commune) vaudoise pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode l'année de la période fiscale considérée.
	 * @return un rapport (technique) sur les rôles par commune et contribuables.
	 */
	public ProduireRolesPPCommunesResults runPPPourToutesCommunes(final int anneePeriode, final int nbThreads, @Nullable final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final GroupementCommunes communeParCommune = new CommuneParCommune();

		return doRun(anneePeriode, nbThreads, null, status, DEFAULT_PROGRESS_CALCULATOR, new VarianteProductionRole<ProduireRolesPPCommunesResults>() {
			@Override
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuablesPP(anneePeriode);
			}

			@Override
			public ProduireRolesPPCommunesResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesPPCommunesResults(anneePeriode, nbThreads, today, tiersService, adresseService);
			}

			@Override
			public GroupementCommunes getGroupementCommunes() {
				return communeParCommune;
			}

			@Override
			public boolean isContribuableInteressant(Contribuable ctb, int anneePeriode) {
				// la requête en base est suffisante...
				return true;
			}
		});
	}

	/**
	 * Produit la liste de contribuables d'une commune (ou fraction de commune) vaudoise pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que la commune spécifiée, en fonction des déménagement des
	 * contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param noOfsCommune
	 *            le numéro Ofs de la commune à traiter
	 * @return un rapport (technique) sur les rôles des contribuables de la commune spécifiée.
	 */
	public ProduireRolesPPCommunesResults runPPPourUneCommune(final int anneePeriode, final int noOfsCommune, final int nbThreads, final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final GroupementCommunes communeParCommune = new CommuneParCommune();

		return doRun(anneePeriode, nbThreads, null, status, DEFAULT_PROGRESS_CALCULATOR, new VarianteProductionRole<ProduireRolesPPCommunesResults>() {
			@Override
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuablesPPSurCommunes(anneePeriode, Collections.singletonList(noOfsCommune));
			}

			@Override
			public ProduireRolesPPCommunesResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesPPCommunesResults(anneePeriode, noOfsCommune, nbThreads, today, tiersService, adresseService);
			}

			@Override
			public GroupementCommunes getGroupementCommunes() {
				return communeParCommune;
			}

			@Override
			public boolean isContribuableInteressant(Contribuable ctb, int anneePeriode) {
				// la requête en base est suffisante...
				return true;
			}
		});
	}

	/**
	 * Produit la liste de contribuables PM de toutes les communes (et fractions de commune) vaudoise pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode l'année de la période fiscale considérée.
	 * @return un rapport (technique) sur les rôles par commune et contribuables.
	 */
	public ProduireRolesPMCommunesResults runPMPourToutesCommunes(final int anneePeriode, final int nbThreads, @Nullable final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final GroupementCommunes communeParCommune = new CommuneParCommune();

		return doRun(anneePeriode, nbThreads, null, status, DEFAULT_PROGRESS_CALCULATOR, new VarianteProductionRole<ProduireRolesPMCommunesResults>() {
			@Override
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuablesPM();
			}

			@Override
			public ProduireRolesPMCommunesResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesPMCommunesResults(anneePeriode, nbThreads, today, tiersService, adresseService);
			}

			@Override
			public GroupementCommunes getGroupementCommunes() {
				return communeParCommune;
			}

			@Override
			public boolean isContribuableInteressant(Contribuable ctb, int anneePeriode) throws ServiceException {
				// la requête en base n'est pas suffisante... il faut voir avec les PFs
				try {
					final List<Assujettissement> assujettissements = assujettissementService.determine(ctb);
					final List<DateRange> exercicesInteressants = getRangesExercicesCommerciauxInteressants((Entreprise) ctb, anneePeriode);

					// si on a une intersection entre les deux collections (assujettissements & exercices récents),
					// alors on prend l'entreprise
					return DateRangeHelper.intersections(exercicesInteressants, assujettissements) != null;
				}
				catch (AssujettissementException e) {
					throw new ServiceException("Assujettissement incalculable sur l'entreprise " + FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()), e);
				}
			}
		});
	}

	/**
	 * Produit la liste de contribuables PM d'une commune (ou fraction de commune) vaudoise pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que la commune spécifiée, en fonction des déménagement des
	 * contribuables.
	 *
	 * @param anneePeriode l'année de la période fiscale considérée.
	 * @param noOfsCommune le numéro Ofs de la commune à traiter
	 * @return un rapport (technique) sur les rôles par commune et contribuables.
	 */
	public ProduireRolesPMCommunesResults runPMPourUneCommune(final int anneePeriode, final int noOfsCommune, final int nbThreads, @Nullable final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final GroupementCommunes communeParCommune = new CommuneParCommune();
		final Set<Integer> communesInteressantes = Collections.singleton(noOfsCommune);

		return doRun(anneePeriode, nbThreads, null, status, DEFAULT_PROGRESS_CALCULATOR, new VarianteProductionRole<ProduireRolesPMCommunesResults>() {
			@Override
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuablesPMSurCommunes(communesInteressantes);
			}

			@Override
			public ProduireRolesPMCommunesResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesPMCommunesResults(anneePeriode, noOfsCommune, nbThreads, today, tiersService, adresseService);
			}

			@Override
			public GroupementCommunes getGroupementCommunes() {
				return communeParCommune;
			}

			@Override
			public boolean isContribuableInteressant(Contribuable ctb, int anneePeriode) throws ServiceException {
				// la requête en base n'est pas suffisante... il faut voir avec les PFs
				try {
					final List<Assujettissement> assujettissements = assujettissementService.determinePourCommunes(ctb, communesInteressantes);
					final List<DateRange> exercicesInteressants = getRangesExercicesCommerciauxInteressants((Entreprise) ctb, anneePeriode);

					// si on a une intersection entre les deux collections (assujettissements & exercices récents),
					// alors on prend l'entreprise
					return DateRangeHelper.intersections(exercicesInteressants, assujettissements) != null;
				}
				catch (AssujettissementException e) {
					throw new ServiceException("Assujettissement incalculable sur l'entreprise " + FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()), e);
				}
			}
		});
	}

	/**
	 * Ne nous intéressent que les exercices commerciaux de l'année donnée et, éventuellement, le dernier exercice
	 * avant le premier de l'année donnée
	 * @param entreprise entreprise ciblée
	 * @param anneePeriode année ciblée
	 * @return la liste de ces ranges d'exercices commerciaux
	 */
	private List<DateRange> getRangesExercicesCommerciauxInteressants(Entreprise entreprise, int anneePeriode) {
		final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);

		// ne nous intéressent que les exercices commerciaux de l'année donnée et, éventuellement, le dernier
		// avant le premier de l'année donnée
		final List<DateRange> exercicesInteressants = new ArrayList<>(exercices.size());
		final MovingWindow<ExerciceCommercial> wnd = new MovingWindow<>(exercices);
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<ExerciceCommercial> snap = wnd.next();
			final ExerciceCommercial current = snap.getCurrent();
			if (current.getDateFin().year() == anneePeriode) {
				final ExerciceCommercial previous = snap.getPrevious();
				if (previous != null && previous.getDateFin().year() == anneePeriode - 1) {
					exercicesInteressants.add(previous);
				}
				exercicesInteressants.add(current);
			}
			else if (current.getDateFin().year() == anneePeriode - 1 && snap.getNext() == null) {
				exercicesInteressants.add(current);
			}
		}

		return exercicesInteressants;
	}

	/**
	 * Produit la liste de contribuables d'un office d'impôt pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que celles gérées par l'office d'impôt, en fonction des
	 * déménagement des contribuables.
	 *
	 * @param anneePeriode l'année de la période fiscale considérée.
	 * @param oid l'id de l'office d'impôt concerné
	 * @param nbThreads le nombre de threads de processing souhaité
	 * @param s status manager
	 * @return un rapport (technique) sur les rôles des contribuables de l'OID spécifié.
	 */
	public ProduireRolesOIDsResults runPourUnOfficeImpot(int anneePeriode, int oid, int nbThreads, StatusManager s) throws ServiceException {
		return runPourUnOfficeImpot(anneePeriode, oid, nbThreads, null, s, false, DEFAULT_PROGRESS_CALCULATOR);
	}

	/**
	 * Produit la liste des contribuables PM du canton
	 * @param anneePeriode la période fiscale concernée
	 * @param nbThreads le nombre de threads de processing souhaité
	 * @param s status manager
	 * @return un rapport (technique) sur les rôles des contribuables PM du canton
	 * @throws ServiceException en cas de souci
	 */
	public ProduireRolesOIPMResults runPourOfficePersonnesMorales(int anneePeriode, int nbThreads, StatusManager s) throws ServiceException {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final List<Commune> communesVaudoises = infraService.getCommunesDeVaud();
		final Set<Integer> ofsCommunesVaudoises = new HashSet<>(communesVaudoises.size());
		for (Commune commune : communesVaudoises) {
			ofsCommunesVaudoises.add(commune.getNoOFS());
		}
		final GroupementCommunes groupementCommunes = new GroupementCommunesPourOfficeImpot(ofsCommunesVaudoises);

		return doRun(anneePeriode, nbThreads, null, status, DEFAULT_PROGRESS_CALCULATOR, new VarianteProductionRole<ProduireRolesOIPMResults>() {
			@Override
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuablesPM();
			}

			@Override
			public ProduireRolesOIPMResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesOIPMResults(anneePeriode, nbThreads, today, tiersService, adresseService);
			}

			@Override
			public GroupementCommunes getGroupementCommunes() {
				return groupementCommunes;
			}

			@Override
			public boolean isContribuableInteressant(Contribuable ctb, int anneePeriode) throws ServiceException {
				// la requête en base n'est pas suffisante... il faut voir avec les PFs
				try {
					final List<Assujettissement> assujettissements = assujettissementService.determine(ctb);
					final List<DateRange> exercicesInteressants = getRangesExercicesCommerciauxInteressants((Entreprise) ctb, anneePeriode);

					// si on a une intersection entre les deux collections (assujettissements & exercices récents),
					// alors on prend l'entreprise
					return DateRangeHelper.intersections(exercicesInteressants, assujettissements) != null;
				}
				catch (AssujettissementException e) {
					throw new ServiceException("Assujettissement incalculable sur l'entreprise " + FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()), e);
				}
			}
		});
	}

	/**
	 * Interface utilisable pour calculer, à partir d'un nombre de cas traités et d'un nombre total de cas,
	 * l'avancement de l'opération
	 */
	private interface ProgressCalculator {
		int getProgressPercentage(int nbTreated, int size);
	}

	/**
	 * Interface utilisable pour calculer, en plus d'un avancement global, une partie locale
	 * (cas d'un job global décomposé en plusieurs phases)
	 */
	private interface DifferenciatingProgressCalculator extends ProgressCalculator {
		int getLocalProgressPercentage(int nbTreated, int size);
	}

	/**
	 * Implémentation par défault du {@link ProgressCalculator} qui ne considère que
	 * les éléments fournis (nombre de cas traités et nombre total de cas), en d'autres termes
	 * qui ne fait pas de différences entre la progression locale et la progression globale
	 */
	private static final ProgressCalculator DEFAULT_PROGRESS_CALCULATOR = new ProgressCalculator() {
		@Override
		public int getProgressPercentage(int nbTreated, int size) {
			return size == 0 ? 0 : nbTreated * 100 / size;
		}
	};

	/**
	 * Méthode interne qui produit le résultat des rôles pour un office d'impôt spécifique
	 * @param anneePeriode période fiscale considérée
	 * @param oid l'id de l'office d'impôt à traiter
	 * @param nbThreads le nombre de threads de processing souhaité
	 * @param statusMessagePrefixe [optionnel] préfixe à utiliser pour les messages d'avancement
	 * @param s status manager
	 * @param nullSiAucuneCommune si <code>true</code>, on ne renvoie aucun résultat si l'OID n'a pas de commune, alors qu'on renverra un résultat vide dans ce cas si <code>false</code>
	 * @param progressCalculator object qui pourra calculer le taux de progression à afficher dans les messages d'avancement
	 * @return un rapport (technique) sur les rôles des contribuables de l'OID spécifié
	 * @throws ServiceException en cas de problème
	 */
	private ProduireRolesOIDsResults runPourUnOfficeImpot(int anneePeriode, final int oid, int nbThreads, @Nullable String statusMessagePrefixe, StatusManager s, boolean nullSiAucuneCommune,
	                                                      ProgressCalculator progressCalculator) throws ServiceException {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final String prefixe = StringUtils.isBlank(statusMessagePrefixe) ? StringUtils.EMPTY : String.format("%s. ", statusMessagePrefixe);
		status.setMessage(String.format("%sRécupération des correspondances entre communes et OID.", prefixe), progressCalculator.getProgressPercentage(0, 0));

		// récupère les numéros Ofs des communes gérées par l'office d'impôt spécifié
		final Set<Integer> nosOfsCommunes;
		try {
			// [SIFISC-13373] On a maintenant besoin d'une transaction ici car la liaison OID <-> Commune nécessite un accès en base
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			template.setReadOnly(true);
			nosOfsCommunes = template.execute(new TransactionCallback<Set<Integer>>() {
				@Override
				public Set<Integer> doInTransaction(TransactionStatus status) {
					final Set<Integer> nosOfsCommunes = new HashSet<>();
					final List<Commune> communes = infraService.getListeCommunesByOID(oid);
					for (Commune c : communes) {
						nosOfsCommunes.add(c.getNoOFS());
					}
					return nosOfsCommunes;
				}
			});
		}
		catch (ServiceInfrastructureException e) {
			throw new ServiceException(e);
		}

		if (!nosOfsCommunes.isEmpty() || !nullSiAucuneCommune) {
			final GroupementCommunes groupement = new GroupementCommunesPourOfficeImpot(nosOfsCommunes);
			return doRun(anneePeriode, nbThreads, statusMessagePrefixe, status, progressCalculator, new VarianteProductionRole<ProduireRolesOIDsResults>() {

				@Override
				public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
					return getIdsOfAllContribuablesPPSurCommunes(anneePeriode, nosOfsCommunes);
				}

				@Override
				public ProduireRolesOIDsResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
					return new ProduireRolesOIDsResults(anneePeriode, oid, nbThreads, today, tiersService, adresseService);
				}

				@Override
				public GroupementCommunes getGroupementCommunes() {
					return groupement;
				}

				@Override
				public boolean isContribuableInteressant(Contribuable ctb, int anneePeriode) {
					// la requête en base est suffisante...
					return true;
				}
			});
		}
		else {
			return null;
		}
	}

	/**
	 * Produit la liste de contribuables de tous les offices d'impôt pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que celles gérées par l'office d'impôt, en fonction des
	 * déménagement des contribuables.
	 *
	 * @param anneePeriode l'année de la période fiscale considérée.
	 * @param nbThreads nombre de threads de traitement
	 * @return les rapports (techniques) sur les rôles des contribuables pour chaque OID
	 */
	public ProduireRolesOIDsResults[] runPourTousOfficesImpot(final int anneePeriode, final int nbThreads, final StatusManager s) throws ServiceException {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		try {
			// on va boucler sur tous les OIDs connus dans l'ordre des numéro de collectivité administrative
			final List<OfficeImpot> oids = infraService.getOfficesImpot();
			final List<OfficeImpot> oidsTries = new ArrayList<>(oids);
			Collections.sort(oidsTries, Comparator.comparingInt(OfficeImpot::getNoColAdm));

			// on commence enfin la boucle
			int index = 0;
			final int nbOids = oidsTries.size();
			final List<ProduireRolesOIDsResults> liste = new ArrayList<>(nbOids);
			for (OfficeImpot oid : oidsTries) {
				final int zeroBasedIndex = index;
				final ProgressCalculator progressCalculator = new DifferenciatingProgressCalculator() {
					@Override
					public int getProgressPercentage(int nbTreated, int size) {
						final int inPhaseProgress = getLocalProgressPercentage(nbTreated, size);
						return (zeroBasedIndex * 100 + inPhaseProgress) / nbOids;
					}

					@Override
					public int getLocalProgressPercentage(int nbTreated, int size) {
						return size == 0 ? 0 : nbTreated * 100 / size;
					}
				};
				final String prefixe = String.format("%s (%d/%d)", oid.getNomCourt(), ++ index, nbOids);
				final ProduireRolesOIDsResults results = runPourUnOfficeImpot(anneePeriode, oid.getNoColAdm(), nbThreads, prefixe, status, true, progressCalculator);
				if (results != null) {
					liste.add(results);
				}
				if (status.interrupted()) {
					break;
				}
			}
			return liste.toArray(new ProduireRolesOIDsResults[liste.size()]);
		}
		catch (ServiceInfrastructureException e) {
			throw new ServiceException(e);
		}
	}

	/**
	 * Traite le contribuable spécifié et ajoute toutes les informations nécessaires au rapport
	 */
	private void processContribuable(int anneePeriode, ProduireRolesResults rapport, GroupementCommunes groupement, Contribuable ctb) {

		++ rapport.ctbsTraites;

		if (tiersService.isSourcierGris(ctb, RegDate.get(anneePeriode, 12, 31))) {
			rapport.addCtbIgnoreSourcierGris(ctb);
		}
		else if (validationService.validate(ctb).hasErrors()) {
			rapport.addErrorCtbInvalide(ctb);
		}
		else {

			// ajoute les assujettissements du contribuable de l'année spécifiée dans le rapport
			try {
				processAssujettissements(anneePeriode, ctb, rapport, groupement);
			}
			catch (TraitementException e) {
				// ok, rien à faire, l'erreur a déjà été renseignée dans le rapport
			}
		}
	}

	protected static class TraitementException extends Exception {

		private static final long serialVersionUID = -7831475785605819136L;

		public TraitementException() {
		}
	}

	/**
	 * Ajoute les détails du ou des assujettissements du contribuable spécifié durant la période fiscale spécifiée au rapport.
	 *
	 * @param anneePeriode la période fiscale dont on veut déterminer l'assujettissement
	 * @param contribuable le contribuable en question
	 * @param rapport le rapport à compléter
	 * @param groupement
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	private void processAssujettissements(int anneePeriode, Contribuable contribuable, ProduireRolesResults rapport, GroupementCommunes groupement) throws TraitementException {

		final AssujettissementContainer assujettissementContainer = new AssujettissementContainer(contribuable, anneePeriode, rapport, assujettissementService, tiersService);

		// traite les assujettissements
		final List<Assujettissement> assujettissements = assujettissementContainer.getAssujettissementAnneePeriode();
		if (assujettissements != null) {
			for (Assujettissement a : assujettissements) {
				processAssujettissement(a, assujettissementContainer, groupement);
			}
		}
		else {
			final DecompositionFors fors = assujettissementContainer.getForsAnneePeriode();
			if (!fors.isFullyEmpty()) {
				// pas d'assujettissement, mais des fors quand-même... A priori ils se terminent dans la période...
				processNonAssujettissement(fors, assujettissementContainer, groupement, RegDate.get(anneePeriode, 1, 1));
			}
		}
	}

	/**
	 * Ajoute les détails de l'assujettissements du contribuable spécifié au rapport.
	 *
	 * @param assujet l'assujettissement à traiter
	 * @param assujettissementContainer cache pour les assujettissements calculés sur la période courante et la précédente
	 * @param groupement
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	protected void processAssujettissement(Assujettissement assujet, AssujettissementContainer assujettissementContainer, GroupementCommunes groupement) throws TraitementException {

		final Contribuable contribuable = assujettissementContainer.ctb;
		final ProduireRolesResults rapport = assujettissementContainer.rapport;
		final TypeContribuable typeCtb = getTypeContribuable(assujet);
		if (typeCtb == null) {
			rapport.addCtbIgnoreDiplomateSuisse(contribuable);
			return;
		}

		final DecompositionFors fors = assujet.getFors();

		/*
		 * On traite tous les fors actifs durant la période d'assujettissement (à l'exception des fors principaux hors canton, évidemment)
		 * et pas seulement les fors déterminants, car toute les communes ayant un for ouvert doivent recevoir l'information
		 */
		for (ForFiscalRevenuFortune f : fors.principauxDansLaPeriode) {
			if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == f.getTypeAutoriteFiscale()) {
				processForFiscal(assujet, f, typeCtb, assujettissementContainer, groupement);
			}
		}
		for (ForFiscalRevenuFortune f : fors.secondairesDansLaPeriode) {
			processForFiscal(assujet, f, typeCtb, assujettissementContainer, groupement);
		}
	}

	/**
	 * Ajoute les détails du non-assujettissement au rapport (= cas du contribuable ayant au moins un for dans le canton mais pas du tout
	 * assujetti dans l'année)
	 *
	 * @param fors la décomposition des fors du contribuable pour l'année de la période courante
	 * @param assujettissementContainer cache pour les assujettissements calculés sur la période courante et la précédente
	 * @param groupement
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	private void processNonAssujettissement(DecompositionFors fors, AssujettissementContainer assujettissementContainer, GroupementCommunes groupement, RegDate dateDebutPeriodeCourante) throws TraitementException {

		// calcul de l'assujettissement dans la période précédente
		final List<Assujettissement> assujettissementsAnneePrecedente = assujettissementContainer.getAssujettissementPeriodePrecedente(dateDebutPeriodeCourante);

		// prenons ensuite les communes des fors de la décomposition
		final Set<Integer> ofsCommunes = new HashSet<>(fors.principauxDansLaPeriode.size() + fors.secondairesDansLaPeriode.size());
		for (ForFiscalRevenuFortune ff : fors.principauxDansLaPeriode) {
			if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				ofsCommunes.add(ff.getNumeroOfsAutoriteFiscale());
			}
		}
		for (ForFiscalRevenuFortune ff : fors.secondairesDansLaPeriode) {
			ofsCommunes.add(ff.getNumeroOfsAutoriteFiscale());
		}

		// sur chacune de ces communes, il faut ensuite indiquer que le contribuable n'est pas assujetti
		for (Integer noOfsCommune : ofsCommunes) {
			traiteNonAssujettiAvecPrecedentAssujettissement(assujettissementContainer, noOfsCommune, groupement, dateDebutPeriodeCourante);
		}
	}

	private static final class AssujettissementContainer {

		private final int anneePeriode;
		private final Contribuable ctb;
		private final ProduireRolesResults rapport;
		private final AssujettissementService assujettissementService;
		private final List<DateRange> periodesFiscales;

		private final DecompositionFors forsAnneePeriode;
		private final List<Assujettissement> assujettissementAnneePeriode;
		private final Map<RegDate, List<Assujettissement>> assujettissementPeriodePrecedente;

		private AssujettissementContainer(Contribuable ctb, int anneePeriode, ProduireRolesResults<?> rapport, AssujettissementService assujettissementService, TiersService tiersService) throws TraitementException {
			this.anneePeriode = anneePeriode;
			this.ctb = ctb;
			this.rapport = rapport;
			this.assujettissementService = assujettissementService;
			this.periodesFiscales = rapport.getPeriodesFiscales(ctb, tiersService);

			final List<DateRange> periodesFiscalesPourAnneeRoles = new ArrayList<>(periodesFiscales.size());
			for (DateRange pf : this.periodesFiscales) {
				if (pf.getDateFin().year() == anneePeriode) {
					periodesFiscalesPourAnneeRoles.add(pf);
				}
			}
			if (periodesFiscalesPourAnneeRoles.isEmpty()) {
				// par dépit plutôt qu'autre chose
				this.forsAnneePeriode = new DecompositionForsAnneeComplete(ctb, anneePeriode);
			}
			else {
				this.forsAnneePeriode = new DecompositionForsPeriode(ctb, periodesFiscalesPourAnneeRoles.get(0).getDateDebut(),
				                                                     CollectionsUtils.getLastElement(periodesFiscalesPourAnneeRoles).getDateFin());
			}
			this.assujettissementAnneePeriode = determineAssujettissement(forsAnneePeriode, ctb, rapport);
			this.assujettissementPeriodePrecedente = new HashMap<>();
		}

		@Nullable
		public List<Assujettissement> getAssujettissementPeriodePrecedente(RegDate dateDebutPeriodeCourante) throws TraitementException {
			final List<Assujettissement> cached = assujettissementPeriodePrecedente.get(dateDebutPeriodeCourante);
			if (cached != null) {
				return cached.isEmpty() ? null : cached;
			}

			final RegDate dateFinPeriodePrecedente = dateDebutPeriodeCourante.getOneDayBefore();
			final DateRange rangePrecedent = DateRangeHelper.rangeAt(periodesFiscales, dateFinPeriodePrecedente);
			final DecompositionFors fors;
			if (rangePrecedent == null) {
				// par dépit plutôt qu'autre chose
				fors = new DecompositionForsAnneeComplete(ctb, dateDebutPeriodeCourante.year() - 1);
			}
			else {
				fors = new DecompositionForsPeriode(ctb, rangePrecedent.getDateDebut(), rangePrecedent.getDateFin());
			}
			final List<Assujettissement> computed = determineAssujettissement(fors, ctb, rapport);
			final List<Assujettissement> newlyStored = computed != null ? computed : Collections.emptyList();
			assujettissementPeriodePrecedente.put(dateDebutPeriodeCourante, newlyStored);
			return computed;
		}

		public List<Assujettissement> getAssujettissementAnneePeriode() {
			return assujettissementAnneePeriode;
		}

		public DecompositionFors getForsAnneePeriode() {
			return forsAnneePeriode;
		}

		private List<Assujettissement> determineAssujettissement(DecompositionFors fors, Contribuable ctb, ProduireRolesResults rapport) throws TraitementException {
			try {
				final List<DateRange> pfDansFors = DateRangeHelper.intersections(fors, periodesFiscales);
				return assujettissementService.determine(fors.contribuable, pfDansFors);
			}
			catch (AssujettissementException e) {
				rapport.addErrorErreurAssujettissement(ctb, e.getMessage());
				throw new TraitementException();
			}
		}
	}

	/**
	 * Ajoute au rapport les détails de l'assujettissement pour le for fiscal spécifié.
	 *
	 * @param assujettissement
	 * @param forFiscal le for fiscal déterminant de l'assujettissement
	 * @param typeCtb le type de contribuable, ou <b>null</b> si le contribuable n'est pas assujetti
	 * @param groupement
     * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiquement renseigné dans le rapport)
	 */
	protected void processForFiscal(Assujettissement assujettissement, ForFiscalRevenuFortune forFiscal, TypeContribuable typeCtb, AssujettissementContainer assujettissementContainer,
	                                GroupementCommunes groupement) throws TraitementException {

		final ProduireRolesResults rapport = assujettissementContainer.rapport;
		final Contribuable contribuable = assujettissementContainer.ctb;
		final int anneePeriode = assujettissementContainer.anneePeriode;

		if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD != forFiscal.getTypeAutoriteFiscale()) {
			final String details = String.format("L'autorité fiscale du for [%s] n'est pas dans le canton.", forFiscal);
			rapport.addCtbIgnoreDonneesIncoherentes(contribuable, details);
			throw new TraitementException();
		}

		try {
			final Integer ofsCommune = forFiscal.getNumeroOfsAutoriteFiscale();
			final TypeAssujettissement type = getTypeAssujettissementPourCommunes(groupement.getCommunes(ofsCommune), assujettissement);
			if (type == TypeAssujettissement.NON_ASSUJETTI) {

				// pas d'assujettissement sur cette commune dans l'annee de période
				// -> s'il y en avait un avant, il faut l'indiquer dans le rapport
				// (sauf si le contribuable était alors sourcier gris, auquel cas il ne doit pas apparaître)
				if (!tiersService.isSourcierGris(contribuable, RegDate.get(anneePeriode - 1, 12, 31))) {
					traiteNonAssujettiAvecPrecedentAssujettissement(assujettissementContainer, ofsCommune, groupement, assujettissement.getDateDebut());
				}
			}
			else {
				final DebutFinFor debutFin = getInformationDebutFin(forFiscal, groupement, anneePeriode, type, assujettissement);
				if (type == TypeAssujettissement.TERMINE_DANS_PF && tiersService.isSourcierGris(contribuable, debutFin.dateFermeture)) {
					// [SIFISC-1797] Il ne faut pas tout d'un coup afficher à la commune le départ d'un contribuable qu'elle ne connait pas
					rapport.addCtbIgnoreSourcierGris(contribuable);
					throw new TraitementException();
				}
				else {
					final InfoFor infoFor = new InfoFor(typeCtb, debutFin.dateOuverture, debutFin.motifOuverture, debutFin.dateFermeture, debutFin.motifFermeture, type, forFiscal);
					rapport.digestInfoFor(infoFor, contribuable, assujettissement, assujettissement.getDateDebut().getOneDayBefore(), anneePeriode, ofsCommune, adresseService, tiersService);
				}
			}
		}
		catch (AssujettissementCommunalException e) {
			// --> on met le calcul sur ce groupe de communes en erreur, mais cela ne doit pas péjorer les autres communes éventuelles où ce contribuable pourrait intervenir
			rapport.addErrorErreurAssujettissement(contribuable, e.getMessage());
		}
		catch (AssujettissementException e) {
			rapport.addErrorErreurAssujettissement(contribuable, e.getMessage());
			throw new TraitementException();
		}
	}

	private void traiteNonAssujettiAvecPrecedentAssujettissement(AssujettissementContainer assujettissementContainer, Integer ofsCommune, GroupementCommunes groupement, RegDate dateDebutPeriodeCourante) throws TraitementException {

		final List<Assujettissement> assujettissementsAnneePrecedente = assujettissementContainer.getAssujettissementPeriodePrecedente(dateDebutPeriodeCourante);
		if (assujettissementsAnneePrecedente != null) {

			final ProduireRolesResults rapport = assujettissementContainer.rapport;
			final Contribuable contribuable = assujettissementContainer.ctb;
			final int anneePeriode = assujettissementContainer.anneePeriode;

			for (Assujettissement assAnneePrecedente : assujettissementsAnneePrecedente) {
				final TypeContribuable typeCtbAnneePrecedente = getTypeContribuable(assAnneePrecedente);
				final DecompositionFors forsAnneePrecedente = assAnneePrecedente.getFors();
				for (ForFiscalRevenuFortune ff : forsAnneePrecedente.principauxDansLaPeriode) {
					if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ff.getNumeroOfsAutoriteFiscale() == ofsCommune.intValue()) {
						traiteNonAssujettiAvecPrecedentAssujettissement(rapport, contribuable, assAnneePrecedente, ofsCommune, groupement, typeCtbAnneePrecedente, ff);
					}
				}
				for (ForFiscalRevenuFortune ff : forsAnneePrecedente.secondairesDansLaPeriode) {
					if (ff.getNumeroOfsAutoriteFiscale() == ofsCommune.intValue()) {
						traiteNonAssujettiAvecPrecedentAssujettissement(rapport, contribuable, assAnneePrecedente, ofsCommune, groupement, typeCtbAnneePrecedente, ff);
					}
				}
			}
		}
	}

	/**
	 * Traitement, pour une commune particulière, d'un contribuable qui n'est pas assujetti sur la période courante mais qui l'était
	 * sur la période précédente
	 * @param rapport rapport à compléter
	 * @param contribuable contribuable en cours de traitement
	 * @param assujettissementPrecedent assujettissement précédent la période actuelle de non-assujettissement
	 * @param ofsCommune numéro OFS de la commune considérée
	 * @param groupement groupement des communes considérées
	 * @param typeCtbAnneePrecedente type de contribuable sur la commune à l'époque de la période précédente
	 * @param ff for fiscal sur la commune qui était ouvert dans la période précédente
	 */
	private void traiteNonAssujettiAvecPrecedentAssujettissement(ProduireRolesResults rapport, Contribuable contribuable, Assujettissement assujettissementPrecedent, Integer ofsCommune,
	                                                             GroupementCommunes groupement, TypeContribuable typeCtbAnneePrecedente, ForFiscalRevenuFortune ff) {
		final int anneePeriode = assujettissementPrecedent.getDateFin().getOneDayAfter().year();
		final DebutFinFor debutFin = getInformationDebutFin(ff, groupement, anneePeriode, null, null);
		final InfoFor infoFor = new InfoFor(debutFin.dateOuverture, debutFin.motifOuverture, debutFin.dateFermeture, debutFin.motifFermeture, typeCtbAnneePrecedente, ff.isPrincipal(), ff.getMotifRattachement(), ofsCommune, ff.getDateDebut(), ff.getDateFin());
		rapport.digestInfoFor(infoFor, contribuable, null, assujettissementPrecedent.getDateFin(), anneePeriode, ofsCommune, adresseService, tiersService);
	}

	private static class DebutFinFor {
		public final RegDate dateOuverture;
		public final MotifAssujettissement motifOuverture;
		public final RegDate dateFermeture;
		public final MotifAssujettissement motifFermeture;

		private DebutFinFor(RegDate dateOuverture, MotifAssujettissement motifOuverture, RegDate dateFermeture, MotifAssujettissement motifFermeture) {
			this.dateOuverture = dateOuverture;
			this.motifOuverture = motifOuverture;
			this.dateFermeture = dateFermeture;
			this.motifFermeture = motifFermeture;
		}
	}

	private static ForFiscalRevenuFortune getForDebut(ForFiscalRevenuFortune forFiscal, GroupementCommunes groupement) {

		final Tiers tiers = forFiscal.getTiers();

		// on pousse vers le passé autant qu'on peut tant qu'on reste sur la même commune
		ForFiscalRevenuFortune candidatRetenu = forFiscal;
		while (true) {
			final List<ForFiscal> fors = tiers.getForsFiscauxValidAt(candidatRetenu.getDateDebut().getOneDayBefore());
			ForFiscalRevenuFortune nouveauCandidat = null;
			for (ForFiscal candidat : fors) {
				if (candidat instanceof ForFiscalRevenuFortune &&
						candidat.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
						groupement.isDansGroupementDeCommunes(candidat.getNumeroOfsAutoriteFiscale(), forFiscal.getNumeroOfsAutoriteFiscale()) &&
						((ForFiscalRevenuFortune) candidat).getMotifRattachement() == forFiscal.getMotifRattachement()) {
					if (nouveauCandidat == null || nouveauCandidat.getDateDebut().isAfter(candidat.getDateDebut())) {
						nouveauCandidat = (ForFiscalRevenuFortune) candidat;
					}
				}
			}
			if (nouveauCandidat == null) {
				break;
			}
			else {
				candidatRetenu = nouveauCandidat;
			}
		}

	    return candidatRetenu;
	}

	private static ForFiscalRevenuFortune getForFin(ForFiscalRevenuFortune forFiscal, GroupementCommunes groupement) {

		final Tiers tiers = forFiscal.getTiers();

		// on pousse vers le futur tant qu'on peut et qu'on reste sur la même commune
		ForFiscalRevenuFortune candidatRetenu = forFiscal;
		while (candidatRetenu.getDateFin() != null) {
			final List<ForFiscal> fors = tiers.getForsFiscauxValidAt(candidatRetenu.getDateFin().getOneDayAfter());
			Collections.sort(fors, new DateRangeComparator<>());       // [SIFISC-13803] l'ordre ne change rien pour le traitement, mais permet de stabiliser les choses
			ForFiscalRevenuFortune nouveauCandidat = null;
			for (ForFiscal candidat : fors) {
				if (candidat instanceof ForFiscalRevenuFortune &&
						candidat.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
						groupement.isDansGroupementDeCommunes(candidat.getNumeroOfsAutoriteFiscale(), forFiscal.getNumeroOfsAutoriteFiscale()) &&
						((ForFiscalRevenuFortune) candidat).getMotifRattachement() == forFiscal.getMotifRattachement()) {
					if (nouveauCandidat == null || NullDateBehavior.LATEST.compare(nouveauCandidat.getDateFin(), candidat.getDateFin()) < 0) {
						nouveauCandidat = (ForFiscalRevenuFortune) candidat;
					}
				}
			}
			if (nouveauCandidat == null) {
				break;
			}
			else {
				candidatRetenu = nouveauCandidat;
			}
		}

		return candidatRetenu;
	}

	private static boolean isFinAnnee(RegDate date) {
		return date != null && date.month() == 12 && date.day() == 31;
	}

	private static DebutFinFor getInformationDebutFin(ForFiscalRevenuFortune forFiscal, GroupementCommunes groupement, int anneePeriode, TypeAssujettissement typeAssujettissement, Assujettissement assujettissement) {

		final ForFiscalRevenuFortune forFiscalPourOuverture = getForDebut(forFiscal, groupement);
		final ForFiscalRevenuFortune forFiscalPourFermeture = getForFin(forFiscal, groupement);

		final Set<MotifAssujettissement> fractionnementsIgnores = EnumSet.of(MotifAssujettissement.CHGT_MODE_IMPOSITION,
		                                                                     MotifAssujettissement.PERMIS_C_SUISSE);

		final RegDate dateFermeture;
		final MotifAssujettissement motifFermeture;
		if (forFiscalPourFermeture.getDateFin() != null && forFiscalPourFermeture.getDateFin().year() <= anneePeriode) {
			dateFermeture = forFiscalPourFermeture.getDateFin();
			motifFermeture = MotifAssujettissement.of(forFiscalPourFermeture.getMotifFermeture());
		}
		else if (forFiscalPourFermeture.getDateFin() != null && forFiscalPourFermeture.getDateFin().year() <= anneePeriode + 1 && typeAssujettissement == TypeAssujettissement.TERMINE_DANS_PF) {
			// SIFISC-1717 : pour obtenir l'assujettissement TERMINE_DANS_PF, on a regardé l'assujettissement au premier janvier de la période fiscale suivante, et il n'y en
			// avait pas sur les communes du groupement : il faut indiquer une fin pour le groupement avec une date au 31.12 de l'année
			dateFermeture = forFiscalPourFermeture.getDateFin();
			motifFermeture = MotifAssujettissement.of(forFiscalPourFermeture.getMotifFermeture());
		}
		else if (assujettissement != null && assujettissement.getMotifFractFin() != null && !fractionnementsIgnores.contains(assujettissement.getMotifFractFin()) && !isFinAnnee(assujettissement.getDateFin())) {
			dateFermeture = assujettissement.getDateFin();
			// FIXME jde
			motifFermeture = assujettissement.getMotifFractFin();
		}
		else {
			dateFermeture = null;
			motifFermeture = null;
		}

		final MotifAssujettissement motifOuverture = MotifAssujettissement.of(forFiscalPourOuverture.getMotifOuverture());
		final RegDate dateOuverture = forFiscalPourOuverture.getDateDebut();
		return new DebutFinFor(dateOuverture, motifOuverture, dateFermeture, motifFermeture);
	}

	private static class AssujettissementCommunalException extends AssujettissementException {

		private static final long serialVersionUID = 2637719390064882251L;

		public AssujettissementCommunalException(Set<Integer> ofsCommunes) {
			super(buildMessage(ofsCommunes));
		}

		private static String buildMessage(Set<Integer> ofsCommunes) {
			final String communesExploded;
			if (ofsCommunes.size() == 1) {
				communesExploded = String.format("de la commune %d", ofsCommunes.iterator().next());
			}
			else {
				final Integer[] ofsArray = ofsCommunes.toArray(new Integer[ofsCommunes.size()]);
				communesExploded = String.format("des communes %s", Arrays.toString(ofsArray));
			}

			return String.format("Assujettissement non calculable pour les rôles %s (incohérence de fors ?)", communesExploded);
		}
	}

	private TypeAssujettissement getTypeAssujettissementPourCommunes(Set<Integer> communes, Assujettissement assujettissement) throws AssujettissementException {

		// l'assujettissement est-il actif sur au moins une commune du groupement ?
		boolean communeActive = false;
		if (assujettissement != null) {
			for (Integer ofs : communes) {
				if (assujettissement.isActifSurCommune(ofs)) {
					communeActive = true;
					break;
				}
			}
		}

		final TypeAssujettissement typeAssujettissement;
		if (communeActive) {
			// [SIFISC-7798] c'est l'assujettissement au lendemain de l'assujettisement en cours d'analyse qui détermine si on doit indiquer un assujettissement poursuivi ou terminé
			// (en fait, on prend le premier jour du mois suivant à cause des assujettissements source dont la fin est arrondie à la fin d'un mois)
			// -> il faut le re-calculer !
			final List<Assujettissement> assujettissementsCommune = assujettissementService.determinePourCommunes(assujettissement.getContribuable(), communes);
			if (assujettissementsCommune == null) {
				// [SIFISC-11991] si l'assujettissement est de type SOURCE_PURE, cela peut arriver (cas par exemple d'un mixte avec immeuble qui se marie
				// pour devenir ordinaire... : l'année du mariage, l'ex-célibataire a un assujettissement "source pure" jusqu'à son mariage (= mixte - ordinaire)
				// et l'assujettissement est déclaré "actif" sur la commune du for immeuble, alors qu'aucun assujettissement ne correspond en fait...)
				if (assujettissement instanceof SourcierPur) {
					typeAssujettissement = TypeAssujettissement.NON_ASSUJETTI;
				}
				else {
					// à quoi correspond ce cas, maintenant... ?
					throw new AssujettissementCommunalException(communes);
				}
			}
			else {
				final Assujettissement assujettissementApres = DateRangeHelper.rangeAt(assujettissementsCommune, RegDateHelper.getFirstDayOfNextMonth(assujettissement.getDateFin()));
				if (assujettissementApres == null) {
					typeAssujettissement = TypeAssujettissement.TERMINE_DANS_PF;
				}
				else {
					typeAssujettissement = TypeAssujettissement.POURSUIVI_APRES_PF;
				}
			}
		}
		else {
			typeAssujettissement = TypeAssujettissement.NON_ASSUJETTI;
		}
		return typeAssujettissement;
	}

	/**
	 * @return calcul le type de contribuable (au sens 'rôle pour les communes') à partir de l'assujettissement spécifié.
	 */
	protected static TypeContribuable getTypeContribuable(Assujettissement a) {
		final TypeContribuable typeCtb;

		if (a instanceof VaudoisOrdinaire || a instanceof Indigent) {
			typeCtb = TypeContribuable.ORDINAIRE;
		}
		else if (a instanceof VaudoisDepense) {
			typeCtb = TypeContribuable.DEPENSE;
		}
		else if (a instanceof SourcierMixte) {
			typeCtb = TypeContribuable.MIXTE;
		}
		else if (a instanceof HorsCanton) {
			typeCtb = TypeContribuable.HORS_CANTON;
		}
		else if (a instanceof HorsSuisse) {
			typeCtb = TypeContribuable.HORS_SUISSE;
		}
		else if (a instanceof SourcierPur) {
			typeCtb = TypeContribuable.SOURCE;
		}
		else if (a instanceof DiplomateSuisse && a.getFors().secondairesDansLaPeriode.contains(MotifRattachement.IMMEUBLE_PRIVE)) {
			typeCtb = TypeContribuable.HORS_SUISSE;
		}
		else {
			Assert.isTrue(a instanceof DiplomateSuisse);
			typeCtb = null; // les diplomates suisses non soumis à l'ICC sont ignorés
		}
		return typeCtb;
	}

	/**
	 * @return la liste des ID de tous les contribuables PP ayant au moins un for fiscal actif dans une commune vaudoise durant l'année spécifiée
	 *         <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement).
	 */
	protected List<Long> getIdsOfAllContribuablesPP(final int annee) {

		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT cont.id FROM ContribuableImpositionPersonnesPhysiques AS cont INNER JOIN cont.forsFiscaux AS for");
		b.append(" WHERE cont.annulationDate IS NULL");
		b.append(" AND for.annulationDate IS NULL AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
		b.append(" AND (for.dateDebut IS NULL OR for.dateDebut <= :finPeriode)");
		b.append(" AND (for.dateFin IS NULL OR for.dateFin >= :debutPeriode)");
		b.append(" ORDER BY cont.id ASC");
		final String hql = b.toString();

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {

						final RegDate debutPeriode = RegDate.get(annee, 1, 1);
						final RegDate finPeriode = RegDate.get(annee, 12, 31);

						final Query query = session.createQuery(hql);
						query.setParameter("debutPeriode", debutPeriode);
						query.setParameter("finPeriode", finPeriode);
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});
	}

	/**
	 * @return la liste des ID de tous les contribuables PM ayant au moins un for fiscal actif dans une commune vaudoise durant l'année spécifiée
	 *         <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement).
	 */
	protected List<Long> getIdsOfAllContribuablesPM() {

		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT cont.id FROM Entreprise AS cont INNER JOIN cont.forsFiscaux AS for");
		b.append(" WHERE cont.annulationDate IS NULL");
		b.append(" AND for.annulationDate IS NULL AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
		b.append(" AND for.genreImpot = 'BENEFICE_CAPITAL'");
		b.append(" ORDER BY cont.id ASC");
		final String hql = b.toString();

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery(hql);
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});
	}

	/**
	 * @return la liste des ID de tous les contribuables ayant au moins un for fiscal actif dans la commune vaudoise spécifiée durant l'année
	 *         spécifiée <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement). Si aucune commune n'est donnée, aucun contribuable ne sera renvoyé.
	 */
	protected List<Long> getIdsOfAllContribuablesPPSurCommunes(final int annee, final Collection<Integer> noOfsCommunes) {

		if (noOfsCommunes == null || noOfsCommunes.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			final StringBuilder b = new StringBuilder();
			b.append("SELECT DISTINCT cont.id FROM ContribuableImpositionPersonnesPhysiques AS cont INNER JOIN cont.forsFiscaux AS for");
			b.append(" WHERE cont.annulationDate IS NULL");
			b.append(" AND for.annulationDate IS NULL AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
			b.append(" AND for.numeroOfsAutoriteFiscale IN (:noOfsCommune)");
			b.append(" AND (for.dateDebut IS NULL OR for.dateDebut <= :finPeriode)");
			b.append(" AND (for.dateFin IS NULL OR for.dateFin >= :debutPeriode)");
			b.append(" ORDER BY cont.id ASC");
			final String hql = b.toString();

			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);

			return template.execute(new TransactionCallback<List<Long>>() {
				@Override
				public List<Long> doInTransaction(TransactionStatus status) {
					return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
						@Override
						public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {

							final RegDate debutPeriode = RegDate.get(annee, 1, 1);
							final RegDate finPeriode = RegDate.get(annee, 12, 31);

							final Query query = session.createQuery(hql);
							query.setParameter("debutPeriode", debutPeriode);
							query.setParameter("finPeriode", finPeriode);
							query.setParameterList("noOfsCommune", noOfsCommunes);
							//noinspection unchecked
							return query.list();
						}
					});
				}
			});
		}
	}

	/**
	 * @return la liste des ID de tous les contribuables ayant au moins un for fiscal actif dans la commune vaudoise spécifiée durant l'année
	 *         spécifiée <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement). Si aucune commune n'est donnée, aucun contribuable ne sera renvoyé.
	 */
	protected List<Long> getIdsOfAllContribuablesPMSurCommunes(final Collection<Integer> noOfsCommunes) {

		if (noOfsCommunes == null || noOfsCommunes.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			final StringBuilder b = new StringBuilder();
			b.append("SELECT DISTINCT cont.id FROM Entreprise AS cont INNER JOIN cont.forsFiscaux AS for");
			b.append(" WHERE cont.annulationDate IS NULL");
			b.append(" AND for.annulationDate IS NULL AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
			b.append(" AND for.numeroOfsAutoriteFiscale IN (:noOfsCommune)");
			b.append(" AND for.genreImpot = 'BENEFICE_CAPITAL'");
			b.append(" ORDER BY cont.id ASC");
			final String hql = b.toString();

			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);

			return template.execute(new TransactionCallback<List<Long>>() {
				@Override
				public List<Long> doInTransaction(TransactionStatus status) {
					return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
						@Override
						public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
							final Query query = session.createQuery(hql);
							query.setParameterList("noOfsCommune", noOfsCommunes);
							//noinspection unchecked
							return query.list();
						}
					});
				}
			});
		}
	}
}

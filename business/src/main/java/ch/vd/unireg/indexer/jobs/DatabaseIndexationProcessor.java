package ch.vd.unireg.indexer.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.simpleindexer.LuceneException;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplate;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.indexer.IndexerBatchException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.lucene.LuceneHelper;
import ch.vd.unireg.indexer.tiers.AutreCommunauteIndexable;
import ch.vd.unireg.indexer.tiers.CollectiviteAdministrativeIndexable;
import ch.vd.unireg.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.unireg.indexer.tiers.EntrepriseIndexable;
import ch.vd.unireg.indexer.tiers.EtablissementIndexable;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.HabitantIndexable;
import ch.vd.unireg.indexer.tiers.MenageCommunIndexable;
import ch.vd.unireg.indexer.tiers.NonHabitantIndexable;
import ch.vd.unireg.indexer.tiers.TiersIndexableData;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersDAOImpl;
import ch.vd.unireg.tiers.TypeTiers;

import static ch.vd.unireg.indexer.lucene.LuceneHelper.extractTiersId;

/**
 * Processeur spécialisé dans l'indexation en masse des tiers Unireg.
 */
public class DatabaseIndexationProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseIndexationProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final TiersDAO tiersDAO;
	@Nullable
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final StatsService statsService;
	private final GlobalTiersSearcher tiersSearcher;
	private final GlobalTiersIndexer tiersIndexer;
	private final GlobalIndexInterface globalIndex;
	private final SessionFactory sessionFactory;
	private final PlatformTransactionManager transactionManager;

	public DatabaseIndexationProcessor(TiersDAO tiersDAO, @Nullable ServiceCivilCacheWarmer serviceCivilCacheWarmer, StatsService statsService, GlobalTiersSearcher tiersSearcher, GlobalTiersIndexer tiersIndexer,
	                                   GlobalIndexInterface globalIndex, SessionFactory sessionFactory, PlatformTransactionManager transactionManager) {
		this.tiersDAO = tiersDAO;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.statsService = statsService;
		this.tiersSearcher = tiersSearcher;
		this.tiersIndexer = tiersIndexer;
		this.globalIndex = globalIndex;
		this.sessionFactory = sessionFactory;
		this.transactionManager = transactionManager;
	}

	@NotNull
	public DatabaseIndexationResults run(@NotNull GlobalTiersIndexer.Mode mode, @NotNull Set<TypeTiers> typesTiers, int nbThreads, @Nullable StatusManager s) {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		if (mode == GlobalTiersIndexer.Mode.FULL) {
			// on écrase l'indexe lucene sur le disque local
			status.setMessage("Effacement du répertoire d'indexation...");
			tiersIndexer.overwriteIndex();
		}

		status.setMessage("Détermination des tiers à indexer...");
		final DeltaIds deltaIds = getIdsToIndex(mode, typesTiers);
		final DatabaseIndexationResults rapportFinal = new DatabaseIndexationResults(mode, typesTiers, nbThreads, statsService);

		// statistiques
		final Date indexationStart = DateHelper.getCurrentDate();
		final int toProcessCount = deltaIds.toAdd.size();
		final AtomicInteger processedCount = new AtomicInteger(0);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();

		// indexation sur plusieurs threads
		final ParallelBatchTransactionTemplate<Long> template = new ParallelBatchTransactionTemplate<>(deltaIds.toAdd, BATCH_SIZE, nbThreads, Behavior.SANS_REPRISE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(new BatchCallback<Long>() {
			@Override
			public boolean doInTransaction(List<Long> batch) {
				status.setMessage("Indexation du tiers n°" + batch.get(0) + " (" + processedCount.intValue() + "/" + toProcessCount + ")", (100 * processedCount.intValue()) / toProcessCount);
				indexBatch(batch, mode, rapportFinal);
				processedCount.addAndGet(batch.size());
				return !status.isInterrupted();
			}
		}, progressMonitor);

		// suppression des tiers en trop
		if (!status.isInterrupted()) {
			status.setMessage("Nettoyage des tiers surnuméraires...");
			remove(deltaIds.toRemove, rapportFinal, status);
		}

		if (mode == GlobalTiersIndexer.Mode.FULL_INCREMENTAL && !status.isInterrupted()) {
			// on supprime les tiers non-indexés dans la phase incrémentale (car cela veut dire qu'ils n'existent plus)
			deleteTiersIndexedBefore(indexationStart, rapportFinal, typesTiers);
		}

		// [SIFISC-1184] on détecte et supprime les doublons une fois l'indexation effectuée
		if (!status.isInterrupted()) {
			status.setMessage("Suppression des doublons...");
			globalIndex.deleteDuplicate();
		}

		rapportFinal.setInterrompu(status.isInterrupted());
		rapportFinal.end(template.getThreadStats());
		rapportFinal.getTimeLog().logStats(LOGGER);
		Audit.info("Indexation terminée.");

		return rapportFinal;

	}

	private void indexBatch(@NotNull List<Long> batch, @NotNull GlobalTiersIndexer.Mode mode, @NotNull DatabaseIndexationResults results) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Indexation des tiers n° " + Arrays.toString(batch.toArray()));
		}

		// On désactive la validation des tiers, ce qui permet de flagger comme 'dirty' même les tiers qui ne valident pas.
		// Autrement, le premier tiers qui ne valide pas fait péter une exception.
		doWithDisabledInterceptor(session -> {
			final List<Tiers> list = loadTiers(batch, session);
			indexTiers(list, mode, results, session);
		});
	}

	/**
	 * Méthode pour exécuter des requêtes Hibernate dans une nouvelle session et avec les intercepteurs désactivés.
	 */
	private void doWithDisabledInterceptor(@NotNull Consumer<Session> callback) {

		// On désactive l'intercepteur global, ce qui désactive la validation des tiers
		final Switchable interceptorSwitch = (Switchable) sessionFactory.getSessionFactoryOptions().getInterceptor();

		final boolean enabled = interceptorSwitch.isEnabled();
		interceptorSwitch.setEnabled(false);
		try {
			final Session session = sessionFactory.openSession();
			try {
				session.setFlushMode(FlushMode.MANUAL);
				callback.accept(session);
			}
			finally {
				session.flush();
				session.close();
			}
		}
		finally {
			interceptorSwitch.setEnabled(enabled);
		}
	}

	private List<Tiers> loadTiers(@NotNull List<Long> ids, Session session) {
		// Si le service est chauffable, on précharge les individus en vrac pour améliorer les performances.
		if (ids.size() > 1) {
			warmIndividuCache(session, ids);
		}

		final Query query = session.createQuery("from Tiers t where t.id in (:ids)");
		query.setParameterList("ids", ids);
		//noinspection unchecked
		return (List<Tiers>) query.list();
	}

	private void indexTiers(@NotNull List<Tiers> tiers, @NotNull GlobalTiersIndexer.Mode mode, @NotNull DatabaseIndexationResults results, @NotNull Session session) {

		if (tiers.isEmpty()) {
			return;
		}

		// on index les tiers
		try {
			// on n'enlève pas préalablement les données indexées en mode FULL et MISSING_ONLY, parce que - par définition - ces données n'existent pas dans ces modes-là.
			final boolean removeBefore = (mode == GlobalTiersIndexer.Mode.DIRTY_ONLY || mode == GlobalTiersIndexer.Mode.FULL_INCREMENTAL);
			// on indexe une liste précise de tiers, par besoin d'étendre l'indexation à d'autres tiers.
			final boolean followDependents = false;

			tiersIndexer.indexTiers(tiers, removeBefore, followDependents);
			tiersDAO.setDirtyFlag(extractIds(tiers), false, session);
			tiers.forEach(t -> results.addTiersIndexe(t.getNumero()));
		}
		catch (IndexerBatchException e) {
			// 1 ou plusieurs tiers n'ont pas pu être indexés (selon la liste fournie par l'exception)
			LOGGER.error(e.getMessage());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}

			// la plupart des tiers ont pu être indexés...
			final Set<Long> indexedIds = new HashSet<>(extractIds(tiers));

			// ...sauf ceux-ci
			final Set<Long> inErrorIds = new HashSet<>();
			final List<Pair<Long, Exception>> list = e.getExceptions();
			for (Pair<Long, Exception> p : list) {
				final Long tiersId = p.getFirst();
				if (tiersId != null) {
					inErrorIds.add(tiersId);
					results.addErrorException(tiersId, p.getSecond());
				}
			}
			indexedIds.removeAll(inErrorIds);
			indexedIds.forEach(results::addTiersIndexe);

			// reset le flag dirty de tous les tiers qui ont été indexés
			tiersDAO.setDirtyFlag(indexedIds, false, session);
			// flag tous les tiers qui n'ont pas pu être indexés comme dirty, notamment ceux qui ne l'étaient pas avant
			tiersDAO.setDirtyFlag(inErrorIds, true, session);
		}
		catch (Exception e) {
			// potentiellement aucun des tiers n'a pu être indexés
			LOGGER.error("Impossible d'indexer les tiers n°" + buildTiersNumeros(tiers), e);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}

			tiers.forEach(id -> results.addErrorException(id.getNumero(), e));
			tiersDAO.setDirtyFlag(extractIds(tiers), true, session);
		}
	}

	/**
	 * [UNIREG-1988] Supprime les tiers spécifiés de l'indexeur.
	 *
	 * @param ids           les ids des tiers à supprimer
	 * @param results       les résultats à compléter
	 * @param statusManager un status manager
	 */
	private void remove(List<Long> ids, DatabaseIndexationResults results, StatusManager statusManager) {

		LOGGER.info("Suppression de l'indexeur de " + ids.size() + " tiers");

		final int size = ids.size();
		int i = 0;

		for (Long id : ids) {
			statusManager.setMessage("Suppression du tiers " + id, (100 * i) / size);
			globalIndex.removeEntity(id);
			i++;
			results.addTiersSupprime(id);
		}
	}

	/**
	 * Supprime de l'indexe les éléments dont la date d'indexation est antérieure à la date spécifiée.
	 *
	 * @param date       une date
	 * @param results    les résultats à compléter
	 * @param typesTiers les types de tiers à prendre en compte
	 */
	void deleteTiersIndexedBefore(@NotNull Date date, @Nullable DatabaseIndexationResults results, @NotNull Set<TypeTiers> typesTiers) {

		final Set<String> subTypes = typesTiers.stream()
				.map(DatabaseIndexationProcessor::getIndexSubTypes)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		try {
			// critère de recherche sur la date d'indexation (qui doit tomber dans le range [0..date[ )
			final org.apache.lucene.search.Query dateQuery = NumericRangeQuery.newLongRange(TiersIndexableData.INDEXATION_DATE,
			                                                                                null,
			                                                                                date.getTime(),
			                                                                                false,
			                                                                                false);

			// critère de recherche sur le ou les types de contribuables
			final BooleanQuery typeQuery = new BooleanQuery();
			for (String s : subTypes) {
				typeQuery.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, s)), BooleanClause.Occur.SHOULD);
			}

			// les deux critères doivent être respectés
			final BooleanQuery fullQuery = new BooleanQuery();
			fullQuery.add(dateQuery, BooleanClause.Occur.MUST);
			fullQuery.add(typeQuery, BooleanClause.Occur.MUST);

			if (results != null) {
				globalIndex.searchAll(fullQuery, (docId, docGetter) -> {
					final Document doc = docGetter.get(docId);
					final long id = extractTiersId(doc);
					results.addTiersSupprime(id);
				});
			}

			// on efface les tiers qui correspondent au critère complet
			globalIndex.deleteEntitiesMatching(fullQuery);
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}
	}

	private static List<Long> extractIds(@NotNull List<Tiers> tiers) {
		return  tiers.stream()
				.map(Tiers::getNumero)
				.collect(Collectors.toList());
	}

	private static String buildTiersNumeros(List<Tiers> list) {
		StringBuilder builder = new StringBuilder("{");
		for (int i = 0, listSize = list.size(); i < listSize; i++) {
			final Tiers t = list.get(i);
			builder.append(t.getNumero());
			if (i < listSize - 1) {
				builder.append(", ");
			}
		}
		builder.append('}');
		return builder.toString();
	}

	private void warmIndividuCache(Session session, List<Long> batch) {
		if (serviceCivilCacheWarmer != null && serviceCivilCacheWarmer.isServiceWarmable()) {
			final Set<Long> numerosIndividus = TiersDAOImpl.getNumerosIndividu(batch, true, session);
			final AttributeIndividu[] parties = AttributeIndividu.values(); // toutes les parties (pour charger le cache persistent)
			serviceCivilCacheWarmer.warmIndividus(numerosIndividus, null, parties);
		}
	}

	private static class DeltaIds {
		public final List<Long> toAdd;
		public final List<Long> toRemove;

		private DeltaIds() {
			this.toAdd = new ArrayList<>();
			this.toRemove = new ArrayList<>();
		}

		private DeltaIds(List<Long> toAdd) {
			this.toAdd = toAdd;
			this.toRemove = Collections.emptyList();
		}
	}

	private DeltaIds getIdsToIndex(final GlobalTiersIndexer.Mode mode, @NotNull Set<TypeTiers> typesTiers) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> {

			final DeltaIds deltaIds;
			switch (mode) {
			case FULL:
			case FULL_INCREMENTAL: {
				// JDE : on traite les identifiants dans l'ordre décroissant pour traiter les PP d'abord...
				final List<Long> allIds = new ArrayList<>(tiersDAO.getAllIdsFor(true, typesTiers));
				allIds.sort(Collections.reverseOrder());
				deltaIds = new DeltaIds(allIds);
				break;
			}

			case DIRTY_ONLY:
				deltaIds = new DeltaIds(tiersDAO.getDirtyIds());
				break;

			case MISSING_ONLY:
				deltaIds = getIncrementalIds();
				break;

			default:
				throw new ProgrammingException("Mode d'indexation inconnu = " + mode);
			}
			return deltaIds;
		});
	}

	/**
	 * @return la liste des ids non-indexés, ainsi que ceux indexés à tord
	 */
	private DeltaIds getIncrementalIds() {

		final Set<Long> idsDb = new HashSet<>(tiersDAO.getAllIds());
		final Set<Long> idsIndex = tiersSearcher.getAllIds();

		DeltaIds ids = new DeltaIds();

		for (Long id : idsDb) {
			if (!idsIndex.contains(id)) {
				ids.toAdd.add(id);
			}
		}
		for (Long id : idsIndex) {
			if (!idsDb.contains(id)) {
				ids.toRemove.add(id);
			}
		}

		return ids;
	}

	/**
	 * @param type un type de tiers
	 * @return le ou less sous-type d'indexation qui correspondent au type de tiers spécifié.
	 */
	@NotNull
	private static List<String> getIndexSubTypes(@NotNull TypeTiers type) {
		final List<String> sub = new ArrayList<>(2);
		switch (type) {
		case AUTRE_COMMUNAUTE:
			sub.add(AutreCommunauteIndexable.SUB_TYPE);
			break;
		case COLLECTIVITE_ADMINISTRATIVE:
			sub.add(CollectiviteAdministrativeIndexable.SUB_TYPE);
			break;
		case DEBITEUR_PRESTATION_IMPOSABLE:
			sub.add(DebiteurPrestationImposableIndexable.SUB_TYPE);
			break;
		case ENTREPRISE:
			sub.add(EntrepriseIndexable.SUB_TYPE);
			break;
		case ETABLISSEMENT:
			sub.add(EtablissementIndexable.SUB_TYPE);
			break;
		case MENAGE_COMMUN:
			sub.add(MenageCommunIndexable.SUB_TYPE);
			break;
		case PERSONNE_PHYSIQUE:
			sub.add(HabitantIndexable.SUB_TYPE);
			sub.add(NonHabitantIndexable.SUB_TYPE);
			break;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + "]");
		}
		return sub;
	}
}

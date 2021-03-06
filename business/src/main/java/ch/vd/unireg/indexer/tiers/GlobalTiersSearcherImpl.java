package ch.vd.unireg.indexer.tiers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.simpleindexer.DocGetter;
import ch.vd.unireg.common.Fuse;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.indexer.EmptySearchCriteriaException;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.SearchCallback;
import ch.vd.unireg.indexer.TooManyClausesIndexerException;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.indexer.lucene.LuceneHelper;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersFilter;

/**
 * Classe principale de recherche de tiers suivant certains criteres
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 *
 */
public class GlobalTiersSearcherImpl implements GlobalTiersSearcher, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTiersSearcherImpl.class);

	private ParametreAppService parametreAppService;
	private int maxHits = 100;

	/**
	 * Le mysterieux global index.
	 */
	private GlobalIndexInterface globalIndex;

	/**
	 * Methode principale de recherche des tiers
	 *
	 * @param criteria les critères de recherche
	 * @return la liste des tiers repondant aux criteres de recherche
	 */
	@Override
	public List<TiersIndexedData> search(TiersCriteria criteria) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche des tiers correspondant aux criteres=" + criteria);
		}

		if (criteria.isEmpty()) {
			throw new EmptySearchCriteriaException("Les critères de recherche sont vides");
		}

		final List<TiersIndexedData> list;
		try {
			list = adaptativeSearch(criteria);
		}
		catch (TooManyResultsIndexerException e) {
			LOGGER.warn("Trop de résultats retournés avec la requête = [" + criteria + "] : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Problème avec la requête = [" + criteria + ']', e);
			throw e;
		}

		return list;
	}

	@Override
	public TopList<TiersIndexedData> searchTop(TiersCriteria criteria, int max) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche des tiers correspondant aux mots-clés =" + criteria);
		}

		if (criteria.isEmpty()) {
			return new TopList<>();
		}

		final QueryConstructor contructor = new QueryConstructor(criteria);
		final Query query = contructor.constructQuery();

		// on effectue la recherche
		final TopList<TiersIndexedData> list = new TopList<>();
		final TopCallback callback = new TopCallback(list);
		globalIndex.search(query, max, callback);

		return list;
	}

	@Override
	public TopList<TiersIndexedData> searchTop(String keywords, @Nullable TiersFilter filter, int max) throws IndexerException {

		final int tokenMinLength = 3;

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche des tiers correspondant aux mots-clés =" + keywords);
		}

		if (StringUtils.isBlank(keywords)) {
			return new TopList<>();
		}

		keywords = keywords.toLowerCase().replaceAll("\\.", ""); // [SIFISC-6093] on supprime tous les points ('.') dans les critères de recherche.

		// critère sur le numéro de contribuable
		final BooleanQuery query = new BooleanQuery();

		if (filter != null) {
			QueryConstructor.addTypeTiers(query, filter.getTypesTiers());
			QueryConstructor.addContrainteVisualisationLimitee(query, filter);
			QueryConstructor.addAnnule(query, filter);
			QueryConstructor.addActif(query, filter);
			QueryConstructor.addDebiteurInactif(query, filter);
			QueryConstructor.addTiersActif(query, filter);
		}

		final Query queryNumeros = LuceneHelper.getTermsCommence(TiersIndexableData.TOUT, keywords, tokenMinLength);
		if (queryNumeros != null) {
			query.add(queryNumeros, BooleanClause.Occur.SHOULD);
		}

		// on effectue la recherche
		final TopList<TiersIndexedData> list = new TopList<>();
		final TopCallback callback = new TopCallback(list);
		globalIndex.search(query, max, callback);

		return list;
	}


	/**
	 * [UNIREG-1386] exécute la requête, et si une exception BooleanQuery.TooManyClause est levée par lucene, adapte la requête en supprimant les termes les plus courts.
	 *
	 * @param criteria les critères de recherche
	 * @return la liste des données trouvées
	 */
	private List<TiersIndexedData> adaptativeSearch(TiersCriteria criteria) {
		
		final List<TiersIndexedData> list = new ArrayList<>();

		QueryConstructor contructor = new QueryConstructor(criteria);
		TooManyClausesIndexerException toomany = null;

		// [UNIREG-1386] exécute la requête, et si une exception BooleanQuery.TooManyClause est levée par lucene, adapte la requête en
		// supprimant les termes les plus courts.
		int essais = 0;
		while (true) {

			final Query query = contructor.constructQuery();
			if (query == null) {
				if (toomany == null) {
					return Collections.emptyList();
				}
				else {
					// la nouvelle requête ne contient plus de critère, on abandonne et on relance l'exception initiale.
					throw new TooManyResultsIndexerException(toomany);
				}
			}

			try {
				++essais;
				globalIndex.search(query, maxHits, new Callback(list));
				break;
			}
			catch (TooManyClausesIndexerException e) {
				if (essais >= 10) {
					// on a déjà essayé 10 fois
					throw new TooManyResultsIndexerException(e);
				}

				// le nombre maximal de termes lucene a été dépassé, et on peut essayer de corriger ça automatiquement
				contructor = contructor.constructBroaderQuery();
				toomany = e;
			}
		}

		return list;
	}

	private final class Callback implements SearchCallback {
		private final List<TiersIndexedData> list;

		private Callback(List<TiersIndexedData> list) {
			this.list = list;
		}

		@Override
		public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
			if (hits.totalHits > maxHits) {
				throw new TooManyResultsIndexerException("Le nombre max de résultats ne peut pas excéder "
						+ maxHits + ". Hits: " + hits.totalHits, hits.totalHits);
			}

			try {
				for (ScoreDoc h : hits.scoreDocs) {
					Document doc = docGetter.get(h.doc);
					TiersIndexedData data = new TiersIndexedData(doc);
					list.add(data);
				}
			}
			catch (IOException e) {
				throw new IndexerException(e);
			}
		}
	}

	private static final class TopCallback implements SearchCallback {
		
		private final TopList<TiersIndexedData> list;

		private TopCallback(TopList<TiersIndexedData> list) {
			this.list = list;
		}

		@Override
		public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
			list.setTotalHits(hits.totalHits);
			try {
				for (ScoreDoc h : hits.scoreDocs) {
					Document doc = docGetter.get(h.doc);
					TiersIndexedData data = new TiersIndexedData(doc);
					list.add(data);
				}
			}
			catch (IOException e) {
				throw new IndexerException(e);
			}
		}
	}

	@Override
	public boolean exists(Long numero) throws IndexerException {
		if (numero == null) {
			return false;
		}

		// Critère de recherche sur le numéro de contribuable
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(numero);

		final class Results {
			boolean exists = false;
		}
		final Results results = new Results();

		// Lancement de la recherche
		Query query = new QueryConstructor(criteria).constructQuery();
		if (query != null) {
			globalIndex.search(query, maxHits, (hits, docGetter) -> results.exists = (hits.totalHits > 0));
		}

		return results.exists;
	}

	@Override
	public TiersIndexedData get(final Long numero) throws IndexerException {
		if (numero == null) {
			return null;
		}

		// Critère de recherche sur le numéro de contribuable
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(numero);

		final class Results {
			TiersIndexedData data = null;
		}
		final Results results = new Results();

		// Lancement de la recherche
		Query query = new QueryConstructor(criteria).constructQuery();
		if (query != null) {
			globalIndex.search(query, maxHits, (hits, docGetter) -> {
				try {
					/*
					 * On peut réellement recevoir plusieurs résultats en recherchant sur un seul numéro de contribuable (= cas du ménage
					 * commun), mais ici on s'intéresse uniquement au contribuable spécifié.
					 */
					for (ScoreDoc h : hits.scoreDocs) {
						Document doc = docGetter.get(h.doc);
						TiersIndexedData data = new TiersIndexedData(doc);
						if (data.getNumero().equals(numero)) {
							results.data = data;
							break;
						}
					}
				}
				catch (IOException e) {
					throw new IndexerException(e);
				}
			});
		}

		return results.data;
	}

	@Override
	public Set<Long> getAllIds() {

		final Set<Long> ids = new HashSet<>(globalIndex.getApproxDocCount());

		// [UNIREG-2597] on veut explicitement tous les ids, sans limite de recherche
		globalIndex.searchAll(new MatchAllDocsQuery(), (docId, docGetter) -> {
			final Document doc = docGetter.get(docId);
			final long id = LuceneHelper.extractTiersId(doc);
			ids.add(id);
		});

		return ids;
	}

	@Override
	public void checkCoherenceIndex(final Set<Long> existingIds, final StatusManager statusManager, final CheckCallback callback) {

		final Set<Long> indexedIds = new HashSet<>();

		statusManager.setMessage("Vérification que les données de l'indexeur existent dans la base...", 50);

		// Vérifie la cohérence des tiers indexés
		globalIndex.search(new MatchAllDocsQuery(), maxHits, (hits, docGetter) -> {
			for (ScoreDoc h : hits.scoreDocs) {

				if (statusManager.isInterrupted()) {
					break;
				}

				final Document doc = docGetter.get(h.doc);
				final long id = LuceneHelper.extractTiersId(doc);

				if (!existingIds.contains(id)) {
					final String message = "Le tiers n° " + id + " existe dans l'indexeur mais pas dans la base.";
					callback.onError(id, message);
				}

				final boolean added = indexedIds.add(id);
				if (!added) {
					final String message = "Le tiers n° " + id + " existe plusieurs fois dans l'indexeur.";
					callback.onError(id, message);
				}
			}
		});

		if (statusManager.isInterrupted()) {
			return;
		}

		statusManager.setMessage("Vérification que les données de la base existent dans l'indexeur...", 75);

		// Vérifie que tous les tiers existants sont indexés.
		for (Long id : existingIds) {
			if (!indexedIds.contains(id)) {
				final String message = "Le tiers n° " + id + " n'est pas indexé.";
				callback.onWarning(id, message);
			}
		}
	}

	@Override
	public int getApproxDocCount() {
		return globalIndex.getApproxDocCount();
	}

	@Override
	public int getExactDocCount() {
		return globalIndex.getExactDocCount();
	}

	public void setGlobalIndex(GlobalIndexInterface globalIndex) {
		this.globalIndex = globalIndex;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		maxHits = parametreAppService.getNbMaxParListe();
	}

	public void flowSearch(TiersCriteria criteria, final BlockingQueue<TiersIndexedData> queue, final Fuse fusible) throws IndexerException {
		if (fusible.isNotBlown()) {
			final QueryConstructor queryConstructor = new QueryConstructor(criteria);
			final Query query = queryConstructor.constructQuery();
			globalIndex.search(query, Integer.MAX_VALUE, (hits, docGetter) -> {
				if (fusible.isNotBlown()) {
					for (ScoreDoc h : hits.scoreDocs) {
						final Document doc = docGetter.get(h.doc);
						final TiersIndexedData data = new TiersIndexedData(doc);
						while (fusible.isNotBlown() && !queue.offer(data, 100, TimeUnit.MILLISECONDS)) {
							// on ré-essaie tant que le fusible n'est pas grillé (= demande externe d'arrêt) et que la queue bloque
						}
						if (fusible.isBlown()) {
							break;
						}
					}
				}
			});
		}
	}

}

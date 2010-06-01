package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.*;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.TiersCriteria;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.*;

/**
 * Classe principale de recherche de tiers suivant certains criteres
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 *
 */
public class GlobalTiersSearcherImpl implements GlobalTiersSearcher, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(GlobalTiersSearcherImpl.class);

	private ParametreAppService parametreAppService;
	private int maxHits;

	/**
	 * Le mysterieux global index.
	 */
	private GlobalIndexInterface globalIndex;

	/**
	 * Methode principale de recherche des tiers
	 *
	 * @param criteria les critères de recherche
	 * @return la liste des tiers repondant aux criteres de recherche
	 * @throws IndexerException
	 */
	public List<TiersIndexedData> search(TiersCriteria criteria) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche des tiers correspondant aux criteres=" + criteria);
		}

		if (criteria.isEmpty()) {
			throw new IndexerException("Les critères de recherche sont vides");
		}

		final List<TiersIndexedData> list = new ArrayList<TiersIndexedData>();

		QueryConstructor contructor = new QueryConstructor(criteria);
		TooManyResultsIndexerException toomany = null;

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
					throw toomany;
				}
			}

			try {
				++essais;
				globalIndex.search(query, maxHits, new Callback(list));
				break;
			}
			catch (TooManyResultsIndexerException e) {
				if (e.getNbResults() > 0 || essais >= 10) {
					// il y a bien trop de résultats (ou on a essayé 10 fois)
					throw e;
				}

				// autrement, c'est que le nombre maximal de termes lucene a été dépassé, et on peut essayer de corriger ça automatiquement
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


	/**
	 * {@inheritDoc}
	 */
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
			globalIndex.search(query, maxHits, new SearchCallback() {
				public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
					results.exists = (hits.totalHits > 0);
				}
			});
		}

		return results.exists;
	}

	/**
	 * {@inheritDoc}
	 */
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
			globalIndex.search(query, maxHits, new SearchCallback() {
				public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
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
				}
			});
		}

		return results.data;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<Long> getAllIds() {

		final Set<Long> ids = new HashSet<Long>();

		globalIndex.search(new MatchAllDocsQuery(), maxHits, new SearchCallback() {

			public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
				for (ScoreDoc h : hits.scoreDocs) {
					final Document doc = docGetter.get(h.doc);
					final long id = extractTiersId(doc);
					ids.add(id);
				}
			}
		});

		return ids;
	}

	/**
	 * {@inheritDoc}
	 */
	public void checkCoherenceIndex(final Set<Long> existingIds, final StatusManager statusManager, final CheckCallback callback) {

		final Set<Long> indexedIds = new HashSet<Long>();

		statusManager.setMessage("Vérification que les données de l'indexeur existent dans la base...", 50);

		// Vérifie la cohérence des tiers indexés
		globalIndex.search(new MatchAllDocsQuery(), maxHits, new SearchCallback() {
			public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
				for (ScoreDoc h : hits.scoreDocs) {

					if (statusManager.interrupted()) {
						break;
					}

					final Document doc = docGetter.get(h.doc);
					final long id = extractTiersId(doc);

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
			}
		});

		if (statusManager.interrupted()) {
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

	private static long extractTiersId(final Document doc) {
		final String idAsString = doc.get(LuceneEngine.F_ENTITYID);
		return Long.parseLong(idAsString);
	}

	public int getApproxDocCount() {
		return globalIndex.getApproxDocCount();
	}

	public int getExactDocCount() {
		return globalIndex.getExactDocCount();
	}

	public void setGlobalIndex(GlobalIndexInterface globalIndex) {
		this.globalIndex = globalIndex;
	}

	public ParametreAppService getParametreAppService() {
		return parametreAppService;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public void afterPropertiesSet() throws Exception {
		maxHits = parametreAppService.getNbMaxParListe();
	}
}

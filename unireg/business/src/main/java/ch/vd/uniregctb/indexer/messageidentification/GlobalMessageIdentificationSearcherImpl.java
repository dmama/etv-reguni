package ch.vd.uniregctb.indexer.messageidentification;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.simpleindexer.DocGetter;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.SearchAllCallback;
import ch.vd.uniregctb.indexer.SearchCallback;

public class GlobalMessageIdentificationSearcherImpl implements GlobalMessageIdentificationSearcher {

	private static final Map<String, Pair<String, SortBuilder>> PAGINATION_SORTING_FIELD_MAPPING = buildPaginationSortingFieldMapping();

	private interface SortBuilder {
		Sort build(String fieldName, boolean reverse);
	}

	private static SortBuilder buildSortBuilder(final SortField.Type type, boolean addIdSorting) {
		if (addIdSorting) {
			return (fieldName, reverse) -> new Sort(new SortField(fieldName, type, reverse), new SortField(MessageIdentificationIndexableData.TRI_ID, SortField.Type.LONG, reverse));
		}
		else {
			return (fieldName, reverse) -> new Sort(new SortField(fieldName, type, reverse));
		}
	}

	private static Map<String, Pair<String, SortBuilder>> buildPaginationSortingFieldMapping() {
		final Map<String, Pair<String, SortBuilder>> map = new HashMap<>();

		final SortBuilder intSortBuilder = buildSortBuilder(SortField.Type.INT, true);
		final SortBuilder longSortBuilder = buildSortBuilder(SortField.Type.LONG, true);
		final SortBuilder stringSortBuilder = buildSortBuilder(SortField.Type.STRING, true);

		map.put("id", Pair.of(MessageIdentificationIndexableData.TRI_ID, buildSortBuilder(SortField.Type.LONG, false)));
		map.put("demande.typeMessage", Pair.of(MessageIdentificationIndexableData.TYPE_MESSAGE, stringSortBuilder));
		map.put("demande.periodeFiscale", Pair.of(MessageIdentificationIndexableData.PERIODE_FISCALE, intSortBuilder));
		map.put("demande.emetteurId", Pair.of(MessageIdentificationIndexableData.EMETTEUR, stringSortBuilder));
		map.put("demande.date", Pair.of(MessageIdentificationIndexableData.DATE_MESSAGE, intSortBuilder));
		map.put("etat", Pair.of(MessageIdentificationIndexableData.ETAT, stringSortBuilder));
		map.put("demande.montant", Pair.of(MessageIdentificationIndexableData.TRI_MONTANT, longSortBuilder));
		map.put("demande.personne.dateNaissance", Pair.of(MessageIdentificationIndexableData.TRI_DATE_NAISSANCE, intSortBuilder));
		map.put("demande.personne.nom", Pair.of(MessageIdentificationIndexableData.TRI_NOM, stringSortBuilder));
		map.put("demande.personne.prenoms", Pair.of(MessageIdentificationIndexableData.TRI_PRENOMS, stringSortBuilder));
		map.put("demande.personne.NAVS11", Pair.of(MessageIdentificationIndexableData.NAVS11, stringSortBuilder));
		map.put("demande.personne.NAVS13", Pair.of(MessageIdentificationIndexableData.NAVS13, stringSortBuilder));
		map.put("reponse.noContribuable", Pair.of(MessageIdentificationIndexableData.TRI_CTB_TROUVE, longSortBuilder));

		return map;
	}

	private GlobalIndexInterface globalIndex;

	public void setGlobalIndex(GlobalIndexInterface globalIndex) {
		this.globalIndex = globalIndex;
	}

	@Override
	public List<MessageIdentificationIndexedData> search(IdentificationContribuableCriteria criteria, @Nullable TypeDemande[] typesDemande, IdentificationContribuableEtatFilter etatFilter, final ParamPagination pagination) {
		final List<MessageIdentificationIndexedData> result = new LinkedList<>();
		final SearchCallback callback = new SearchCallback() {
			@Override
			public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
				final int firstIndex;
				final int maxResults;
				if (pagination == null) {
					firstIndex = 0;
					maxResults = MAX_RESULTS;
				}
				else {
					firstIndex = pagination.getSqlFirstResult();
					maxResults = pagination.getSqlMaxResults();
				}
				final ScoreDoc[] docs = hits.scoreDocs;
				if (docs != null && firstIndex < docs.length) {
					final int size = Math.min(maxResults, docs.length - firstIndex);
					for (int idx = 0 ; idx < size ; ++ idx) {
						result.add(new MessageIdentificationIndexedData(docGetter.get(docs[firstIndex + idx].doc)));
					}
				}
			}
		};

		final Query query = QueryConstructor.buildQuery(criteria, typesDemande, etatFilter);
		final Pair<String, SortBuilder> sortingInfo = pagination != null ? PAGINATION_SORTING_FIELD_MAPPING.get(pagination.getChamp()) : null;
		if (sortingInfo != null) {
			final SortBuilder sortBuilder = sortingInfo.getRight();
			final String fieldName = sortingInfo.getLeft();
			final Sort sort = sortBuilder.build(fieldName, !pagination.isSensAscending());
			globalIndex.search(query, MAX_RESULTS, sort, callback);
		}
		else {
			globalIndex.search(query, MAX_RESULTS, callback);
		}

		return result;
	}

	@Override
	public int count(IdentificationContribuableCriteria criteria, @Nullable TypeDemande[] typesDemande, IdentificationContribuableEtatFilter etatFilter) {
		final MutableInt counter = new MutableInt(0);
		final Query query = QueryConstructor.buildQuery(criteria, typesDemande, etatFilter);
		globalIndex.searchAll(query, new SearchAllCallback() {
			@Override
			public void handle(int doc, DocGetter docGetter) throws Exception {
				counter.increment();
			}
		});
		return counter.intValue();
	}

	@Override
	public int getApproxDocCount() {
		return globalIndex.getApproxDocCount();
	}

	@Override
	public int getExactDocCount() {
		return globalIndex.getExactDocCount();
	}
}

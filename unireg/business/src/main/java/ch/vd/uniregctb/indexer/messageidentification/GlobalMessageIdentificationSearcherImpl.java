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
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.SearchAllCallback;
import ch.vd.uniregctb.indexer.SearchCallback;

public class GlobalMessageIdentificationSearcherImpl implements GlobalMessageIdentificationSearcher {

	private static final Map<String, Pair<String, SortBuilder>> PAGINATION_SORTING_FIELD_MAPPING = buildPaginationSortingFieldMapping();

	private static interface SortBuilder {
		Sort build(String fieldName, boolean reverse);
	}

	private static Map<String, Pair<String, SortBuilder>> buildPaginationSortingFieldMapping() {
		final Map<String, Pair<String, SortBuilder>> map = new HashMap<>();

		final SortBuilder intSortBuilder = new SortBuilder() {
			@Override
			public Sort build(String fieldName, boolean reverse) {
				return new Sort(new SortField(fieldName, SortField.Type.INT, reverse));
			}
		};
		final SortBuilder longSortBuilder = new SortBuilder() {
			@Override
			public Sort build(String fieldName, boolean reverse) {
				return new Sort(new SortField(fieldName, SortField.Type.LONG, reverse));
			}
		};

		map.put("demande.periodeFiscale", Pair.of(MessageIdentificationIndexableData.PERIODE_FISCALE, intSortBuilder));
		map.put("demande.date", Pair.of(MessageIdentificationIndexableData.DATE_MESSAGE, intSortBuilder));

		// TODO pose des problèmes (ArrayIndexOutOfBoundsException) dans Lucene -> est-ce parce que certaines valeurs sont stockées en strings (= les valeurs vides) ?
		map.put("demande.personne.dateNaissance", Pair.of(MessageIdentificationIndexableData.DATE_NAISSANCE, intSortBuilder));

		// TODO le montant est stocké en String et non-indexé... est-ce un problème ?
		map.put("demande.montant", Pair.of(MessageIdentificationIndexableData.MONTANT, longSortBuilder));
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
				final int firstIndex = pagination.getSqlFirstResult();
				final ScoreDoc[] docs = hits.scoreDocs;
				if (docs != null && firstIndex < docs.length) {
					final int size = Math.min(pagination.getSqlMaxResults(), docs.length - firstIndex);
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
			globalIndex.search(query, Integer.MAX_VALUE, sort, callback);
		}
		else {
			globalIndex.search(query, Integer.MAX_VALUE, callback);
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
}

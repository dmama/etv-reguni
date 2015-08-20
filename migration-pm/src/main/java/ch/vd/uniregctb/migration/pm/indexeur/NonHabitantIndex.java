package ch.vd.uniregctb.migration.pm.indexeur;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.indexer.lucene.DocumentExtractorHelper;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

public class NonHabitantIndex {

	public static class NonHabitantSearchParameters implements SearchParameter<PersonnePhysique> {

		final String nom;
		final String prenom;
		final Sexe sexe;
		final RegDate dateNaissance;

		public NonHabitantSearchParameters(String nom, String prenom, Sexe sexe, RegDate dateNaissance) {
			this.nom = nom;
			this.prenom = prenom;
			this.sexe = sexe;
			this.dateNaissance = dateNaissance;
		}

		@Override
		public boolean isEmpty() {
			return StringUtils.isBlank(nom) && StringUtils.isBlank(prenom) && sexe == null && dateNaissance == null;
		}
	}

	private static class NonHabitantData extends IndexableData {

		public static final String NOM = "S_NOM";
		public static final String DATE_NAISSANCE = "S_DATE_NAISSANCE";
		public static final String SEXE = "S_SEXE";

		private final String noms;
		private final RegDate dateNaissance;       // valeurs utilisées pour la recherche (calculées à partir de la date connue)
		private final String sexe;

		NonHabitantData(PersonnePhysique pp) {
			super(pp.getNumero(), "PersonnePhysique", "NonHabitant");
			this.noms = buildNoms(pp);
			this.dateNaissance = pp.getDateNaissance();
			this.sexe = IndexerFormatHelper.enumToString(pp.getSexe());
		}

		private static String buildNoms(PersonnePhysique pp) {
			final String BLANKS = "\\s+";
			final List<String> noms = new LinkedList<>(Arrays.asList(StringUtils.trimToEmpty(pp.getNom()).split(BLANKS)));
			noms.addAll(Arrays.asList(StringUtils.trimToEmpty(pp.getNomNaissance()).split(BLANKS)));
			noms.addAll(Arrays.asList(StringUtils.trimToEmpty(pp.getPrenomUsuel()).split(BLANKS)));
			noms.addAll(Arrays.asList(StringUtils.trimToEmpty(pp.getTousPrenoms()).split(BLANKS)));
			return new LinkedHashSet<>(noms).stream().collect(Collectors.joining(" "));
		}

		@Override
		public Document asDoc() {
			final Document d = super.asDoc();
			addAnalyzedValue(d, NOM, noms);
			addAnalyzedValue(d, DATE_NAISSANCE, IndexerFormatHelper.dateToString(dateNaissance, IndexerFormatHelper.DateStringMode.INDEXATION));
			addAnalyzedValue(d, SEXE, sexe);
			return d;
		}
	}

	private Index index;

	public void setIndex(Index index) {
		this.index = index;
	}

	public void overwriteIndex() {
		this.index.overwriteIndex();
	}

	public void index(PersonnePhysique entity) {
		this.index.indexEntity(new NonHabitantData(entity));
	}

	public List<Long> search(NonHabitantSearchParameters params, int maxHits) {
		if (params.isEmpty()) {
			throw new IndexerException("Les paramètres de recherche sont vide...");
		}

		final Query query = buildQuery(params);

		final LinkedList<Long> result = new LinkedList<>();
		this.index.search(query, maxHits, (hits, docGetter) -> {
			for (ScoreDoc doc : hits.scoreDocs) {
				final Document document = docGetter.get(doc.doc);
				final long id = Long.parseLong(DocumentExtractorHelper.getDocValue(LuceneHelper.F_ENTITYID, document));
				result.add(id);
			}
		});
		return result;
	}

	private static Query buildQuery(NonHabitantSearchParameters params) {
		final BooleanQuery fullQuery = new BooleanQuery();
		if (StringUtils.isNotBlank(params.nom)) {
			final Query query = LuceneHelper.getTermsExact(NonHabitantData.NOM, params.nom);
			if (query != null) {
				fullQuery.add(query, BooleanClause.Occur.MUST);
			}
		}
		if (StringUtils.isNotBlank(params.prenom)) {
			final Query query = LuceneHelper.getTermsExact(NonHabitantData.NOM, params.prenom);
			if (query != null) {
				fullQuery.add(query, BooleanClause.Occur.MUST);
			}
		}
		if (params.sexe != null) {
			final Query query = LuceneHelper.getTermsExact(NonHabitantData.SEXE, IndexerFormatHelper.enumToString(params.sexe));
			if (query != null) {
				fullQuery.add(query, BooleanClause.Occur.MUST);
			}
		}
		if (params.dateNaissance != null) {
			final Query query = LuceneHelper.getTermsCommence(NonHabitantData.DATE_NAISSANCE, RegDateHelper.toIndexString(params.dateNaissance), 0);
			if (query != null) {
				fullQuery.add(query, BooleanClause.Occur.MUST);
			}
		}
		return fullQuery;
	}
}

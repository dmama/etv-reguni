package ch.vd.uniregctb.indexer.messageidentification;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;

public abstract class QueryConstructor {

	public static Query buildQuery(IdentificationContribuableCriteria criteria, @Nullable TypeDemande[] typesDemande, IdentificationContribuableEtatFilter etatFilter) throws IndexerException {
		final BooleanQuery fullQuery = new BooleanQuery();
		addTypesDemande(fullQuery, typesDemande);
		addTypeMessage(fullQuery, criteria.getTypeMessage());
		addPeriodeFiscale(fullQuery, criteria.getPeriodeFiscale());
		addEmetteur(fullQuery, criteria.getEmetteurId());
		addPriorite(fullQuery, criteria.getPrioriteEmetteur());
		addDateMessage(fullQuery, criteria.getDateMessageDebut(), criteria.getDateMessageFin());
		addEtat(fullQuery, criteria.getEtatMessage(), etatFilter);
		addNom(fullQuery, criteria.getNom());
		addPrenoms(fullQuery, criteria.getPrenoms());
		addNavs13(fullQuery, criteria.getNAVS13());
		addNavs11(fullQuery, criteria.getNAVS11());
		addDateNaissance(fullQuery, criteria.getDateNaissance());
		addVisaTraitement(fullQuery, criteria.getTraitementUser());
		addDateTraitement(fullQuery, criteria.getDateTraitementDebut(), criteria.getDateTraitementFin());
		return fullQuery;
	}

	private static void addTypesDemande(BooleanQuery fullQuery, @Nullable TypeDemande[] typesDemande) throws IndexerException {
		if (typesDemande != null) {
			final BooleanQuery sub = new BooleanQuery();
			for (TypeDemande type : typesDemande) {
				if (type != null) {
					// Les DocSubtypes sont tous en minuscules!
					sub.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, type.name().toLowerCase())), BooleanClause.Occur.SHOULD);
				}
			}
			if (!sub.clauses().isEmpty()) {
				fullQuery.add(sub, BooleanClause.Occur.MUST);
			}
		}
	}

	private static void addTypeMessage(BooleanQuery fullQuery, String typeMessage) throws IndexerException {
		if (StringUtils.isNotBlank(typeMessage)) {
			fullQuery.add(new TermQuery(new Term(MessageIdentificationIndexableData.TYPE_MESSAGE, typeMessage)), BooleanClause.Occur.MUST);
		}
	}

	private static void addPeriodeFiscale(BooleanQuery fullQuery, Integer periodeFiscale) throws IndexerException {
		if (periodeFiscale != null) {
			fullQuery.add(NumericRangeQuery.newIntRange(MessageIdentificationIndexableData.PERIODE_FISCALE, periodeFiscale, periodeFiscale, true, true), BooleanClause.Occur.MUST);
		}
	}

	private static void addEmetteur(BooleanQuery fullQuery, String emetteurId) throws IndexerException {
		if (StringUtils.isNotBlank(emetteurId)) {
			fullQuery.add(new TermQuery(new Term(MessageIdentificationIndexableData.EMETTEUR, emetteurId)), BooleanClause.Occur.MUST);
		}
	}

	private static void addPriorite(BooleanQuery fullQuery, Demande.PrioriteEmetteur priorite) throws IndexerException {
		if (priorite != null) {
			fullQuery.add(new TermQuery(new Term(MessageIdentificationIndexableData.PRIORITE, priorite.name())), BooleanClause.Occur.MUST);
		}
	}

	private static void addDateMessage(BooleanQuery fullQuery, Date dateMessageMin, Date dateMessageMax) throws IndexerException {
		if (dateMessageMin != null || dateMessageMax != null) {
			final RegDate min = RegDateHelper.get(dateMessageMin);
			final RegDate max = RegDateHelper.get(dateMessageMax);
			final Integer indexMin = min != null ? min.index() : null;
			final Integer indexMax = max != null ? max.index() : null;
			if (indexMin != null || indexMax != null) {
				fullQuery.add(NumericRangeQuery.newIntRange(MessageIdentificationIndexableData.DATE_MESSAGE, indexMin, indexMax, true, true), BooleanClause.Occur.MUST);
			}
		}
	}

	private static void addEtat(BooleanQuery fullQuery, IdentificationContribuable.Etat etat, IdentificationContribuableEtatFilter filter) throws IndexerException {
		if (etat != null || (filter != null && filter != IdentificationContribuableEtatFilter.TOUS)) {
			if (etat != null && filter != null && !filter.isIncluded(etat)) {
				// recherche tous les éléments dont le champ "ETAT" est "NULL" -> aucun !
				fullQuery.add(new TermQuery(new Term(MessageIdentificationIndexableData.ETAT, IndexerFormatHelper.nullValue())), BooleanClause.Occur.MUST);
			}
			else if (etat != null) {
				// si on est ici, c'est que l'état demandé est accepté par le filtre... ok
				fullQuery.add(new TermQuery(new Term(MessageIdentificationIndexableData.ETAT, etat.name())), BooleanClause.Occur.MUST);
			}
			else {
				// pas d'état demandé -> juste le filtre qui doit jouer son rôle
				final BooleanQuery sub = new BooleanQuery();
				for (IdentificationContribuable.Etat candidate : IdentificationContribuable.Etat.values()) {
					if (filter.isIncluded(candidate)) {
						sub.add(new TermQuery(new Term(MessageIdentificationIndexableData.ETAT, candidate.name())), BooleanClause.Occur.SHOULD);
					}
				}
				fullQuery.add(sub, BooleanClause.Occur.MUST);
			}
		}
	}

	private static void addNom(BooleanQuery fullQuery, String nom) throws IndexerException {
		if (StringUtils.isNotBlank(nom)) {
			final Query sub = LuceneHelper.getAnyTermsExact(MessageIdentificationIndexableData.NOM, nom);
			if (sub != null) {
				fullQuery.add(sub, BooleanClause.Occur.MUST);
			}
		}
	}

	private static void addPrenoms(BooleanQuery fullQuery, String prenoms) throws IndexerException {
		if (StringUtils.isNotBlank(prenoms)) {
			final Query sub = LuceneHelper.getAnyTermsExact(MessageIdentificationIndexableData.PRENOMS, prenoms);
			if (sub != null) {
				fullQuery.add(sub, BooleanClause.Occur.MUST);
			}
		}
	}

	private static void addNavs13(BooleanQuery fullQuery, String avs13) throws IndexerException {
		if (StringUtils.isNotBlank(avs13)) {
			final Query sub = LuceneHelper.getAnyTermsExact(MessageIdentificationIndexableData.NAVS13, IndexerFormatHelper.noAvsToString(avs13));
			if (sub != null) {
				fullQuery.add(sub, BooleanClause.Occur.MUST);
			}
		}
	}

	private static void addNavs11(BooleanQuery fullQuery, String avs11) throws IndexerException {
		if (StringUtils.isNotBlank(avs11)) {
			final Query sub = LuceneHelper.getAnyTermsExact(MessageIdentificationIndexableData.NAVS11, IndexerFormatHelper.noAvsToString(avs11));
			if (sub != null) {
				fullQuery.add(sub, BooleanClause.Occur.MUST);
			}
		}
	}

	private static void addDateNaissance(BooleanQuery fullQuery, RegDate dateNaissance) throws IndexerException {
		if (dateNaissance != null) {
			final int index = dateNaissance.index();
			final BooleanQuery sub = new BooleanQuery();
			sub.add(NumericRangeQuery.newIntRange(MessageIdentificationIndexableData.DATE_NAISSANCE, index, toPartialMax(index), true, true), BooleanClause.Occur.SHOULD);
			sub.add(NumericRangeQuery.newIntRange(MessageIdentificationIndexableData.DATE_NAISSANCE, (index / 100) * 100, (index / 100) * 100, true, true), BooleanClause.Occur.SHOULD);
			sub.add(NumericRangeQuery.newIntRange(MessageIdentificationIndexableData.DATE_NAISSANCE, (index / 10000) * 10000, (index / 10000) * 10000, true, true), BooleanClause.Occur.SHOULD);
			fullQuery.add(sub, BooleanClause.Occur.MUST);
		}
	}

	private static int toPartialMax(int partialDateIndex) {
		if (partialDateIndex % 10000 == 0) {
			return partialDateIndex + 9999;
		}
		else if (partialDateIndex % 100 == 0) {
			return partialDateIndex + 99;
		}
		else {
			return partialDateIndex;
		}
	}

	private static void addVisaTraitement(BooleanQuery fullQuery, String visaTraitement) throws IndexerException {
		if (StringUtils.isNotBlank(visaTraitement)) {
			final Query sub = new TermQuery(new Term(MessageIdentificationIndexableData.VISA_TRAITEMENT, visaTraitement));
			fullQuery.add(sub, BooleanClause.Occur.MUST);
		}
	}

	private static void addDateTraitement(BooleanQuery fullQuery, Date dateTraitementMin, Date dateTraitementMax) throws IndexerException {
		final Long tsMin = dateTraitementMin == null ? null : dateTraitementMin.getTime();
		final Long tsMax = dateTraitementMax == null ? null : dateTraitementMax.getTime() + TimeUnit.DAYS.toMillis(1);
		if (tsMin != null || tsMax != null) {
			fullQuery.add(NumericRangeQuery.newLongRange(MessageIdentificationIndexableData.DATE_TRAITEMENT, tsMin, tsMax, true, false), BooleanClause.Occur.MUST);
		}
	}
}


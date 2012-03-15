package ch.vd.uniregctb.indexer.tiers;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeVisualisation;
import ch.vd.uniregctb.tiers.TiersFilter;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class QueryConstructor {

	private static final int INTIAL_NAME_LEN = 2;

	private static final BooleanClause.Occur should = BooleanClause.Occur.SHOULD;
	private static final BooleanClause.Occur must = BooleanClause.Occur.MUST;
	private static final BooleanClause.Occur mustNot = BooleanClause.Occur.MUST_NOT;

	private final TiersCriteria criteria;

	/**
	 * Longueur minimales des tokens sur le critère wildcard (nom/raison, localité, ...).
	 */
	private final int tokenMinLength;

	public QueryConstructor(TiersCriteria criteria) {
		this.criteria = criteria;
		this.tokenMinLength = INTIAL_NAME_LEN;
	}

	private QueryConstructor(TiersCriteria criteria, int tokenMinLength) {
		this.criteria = criteria;
		this.tokenMinLength = tokenMinLength;
	}

	public static void addTypeTiers(BooleanQuery fullQuery, TiersFilter filter) throws IndexerException {

		// Type de tiers
		final Set<TypeTiers> set = filter.getTypesTiers();
		if (set != null && !set.isEmpty()) {

			BooleanQuery query = new BooleanQuery();
			for (TypeTiers typeTiers : set) {
				switch (typeTiers) {
				case DEBITEUR_PRESTATION_IMPOSABLE:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, DebiteurPrestationImposableIndexable.SUB_TYPE)), should);
					break;
				case HABITANT:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
					break;
				case NON_HABITANT:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
					break;
				case MENAGE_COMMUN:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, MenageCommunIndexable.SUB_TYPE)), should);
					break;
				case ENTREPRISE:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, EntrepriseIndexable.SUB_TYPE)), should);
					break;
				case AUTRE_COMMUNAUTE:
					break;
				case ETABLISSEMENT:
					// TODO(JEC) Indexer pour etablissement et collectiviteadministrative
					// query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, EtablissementIndexable.SUB_TYPE)), should);
					break;
				case COLLECTIVITE_ADMINISTRATIVE:
					// query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, CollectiviteAdministrative.SUB_TYPE)), should);
					break;
				case CONTRIBUABLE:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, EntrepriseIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, MenageCommunIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, AutreCommunauteIndexable.SUB_TYPE)), should);
					// TODO(JEC) Indexer pour etablissement et collectiviteadministrative
					// query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, EtablissementIndexable.SUB_TYPE)), should);
					// query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, CollectiviteAdministrative.SUB_TYPE)), should);
					break;
				case PERSONNE_PHYSIQUE:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
					break;
				case CONTRIBUABLE_PP:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, MenageCommunIndexable.SUB_TYPE)), should);
					break;
				default:
					throw new IndexerException("Type de tiers inconnu = [" + typeTiers + ']');
				}
			}
			if (query.getClauses().length > 0) {
				fullQuery.add(query, must);
			}
		}
	}

	private void addNumero(BooleanQuery fullQuery) throws IndexerException {

		// Numero CTB ou individu ou AncienNumeroSourcier
		if (criteria.getNumero() != null) {
			BooleanQuery query = new BooleanQuery();
			query.add(new TermQuery(new Term(LuceneHelper.F_ENTITYID, criteria.getNumero().toString())), should);
			query.add(new TermQuery(new Term(TiersIndexableData.NUMEROS, criteria.getNumero().toString())), should);
			fullQuery.add(query, must);
		}
	}

	private void addNomRaison(BooleanQuery fullQuery) throws IndexerException {

		// Nom courrier
		if (StringUtils.isNotBlank(criteria.getNomRaison())) { // [UNIREG-2592]

			BooleanQuery query = new BooleanQuery();
			TiersCriteria.TypeRecherche typeRecherche = criteria.getTypeRechercheDuNom();
			String nomCourrier = criteria.getNomRaison();
			switch (typeRecherche) {
			case PHONETIQUE:
				// Fuzzy
				{
					final Query subQuery = LuceneHelper.getTermsFuzzy(TiersIndexableData.NOM_RAISON, nomCourrier);
					if (subQuery != null) {
						query.add(subQuery, should);
					}
					final BooleanQuery subQuery2 = LuceneHelper.getTermsFuzzy(TiersIndexableData.AUTRES_NOM, nomCourrier);
					if (subQuery2 != null) {
						query.add(subQuery2, should);
					}
				}
				break;

			case EST_EXACTEMENT:
				{
					final Query subQuery = LuceneHelper.getTermsExact(TiersIndexableData.NOM_RAISON, nomCourrier);
					if (subQuery != null) {
						query.add(subQuery, should);
					}
					final Query subQuery2 = LuceneHelper.getTermsExact(TiersIndexableData.AUTRES_NOM, nomCourrier);
					if (subQuery2 != null) {
						query.add(subQuery2, should);
					}
				}
				break;

			case CONTIENT:
			default:
				{
					final Query subQuery = LuceneHelper.getTermsContient(TiersIndexableData.NOM_RAISON, nomCourrier, tokenMinLength);
					if (subQuery != null) {
						query.add(subQuery, should);
					}
					final Query subQuery2 = LuceneHelper.getTermsContient(TiersIndexableData.AUTRES_NOM, nomCourrier, tokenMinLength);
					if (subQuery2 != null) {
						query.add(subQuery2, should);
					}
				}
				break;
			}

			if (query.clauses() != null && query.getClauses().length > 0) {
				fullQuery.add(query, BooleanClause.Occur.MUST);
			}
		}
	}

	private void addFors(BooleanQuery fullQuery) throws IndexerException {

		// Fors
		final String numeroOfsFor = criteria.getNoOfsFor();
		if (numeroOfsFor != null && !"".equals(numeroOfsFor.trim())) {
			/*
			 * [UNIREG-258] il faut retourner le ou les fors correspondant exactement à la commune spécifiée: on utilise le numéro Ofs de la
			 * commune/pays pour cela.
			 */

			final BooleanQuery query = new BooleanQuery();
			final Query queryForPrincipal = LuceneHelper.getTermsExact(TiersIndexableData.NO_OFS_FOR_PRINCIPAL, numeroOfsFor);
			if (queryForPrincipal != null) {
				query.add(queryForPrincipal, should);
			}
			if (criteria.isForPrincipalActif()) {
				// Recherche sur les fors principaux actifs uniquement -> rien d'autre à faire
			}
			else {
				// Recherche sur tous les fors (principaux, secondaires, inactifs, ...)
				final Query queryAutresFors = LuceneHelper.getTermsContient(TiersIndexableData.NOS_OFS_AUTRES_FORS, numeroOfsFor, 0);
				if (queryAutresFors != null) {
					query.add(queryAutresFors, should);
				}
			}
			if (!query.clauses().isEmpty()) {
				fullQuery.add(query, BooleanClause.Occur.MUST);
			}
		}
	}

	private void addLocalitePays(BooleanQuery fullQuery) throws IndexerException {

		// Localite ou Pays
		if (StringUtils.isNotBlank(criteria.getLocaliteOuPays())) { // [UNIREG-2592]
			final String nomLocaliteOuPays = criteria.getLocaliteOuPays().toLowerCase();
			final Query q = LuceneHelper.getTermsCommence(TiersIndexableData.LOCALITE_PAYS, nomLocaliteOuPays, tokenMinLength);
			if (q != null) {
				fullQuery.add(q, must);
			}
		}
	}

	private void addNpa(BooleanQuery fullQuery) throws IndexerException {

		// Localite ou Pays
		if (StringUtils.isNotBlank(criteria.getNpa())) { // [UNIREG-2592]
			final String npa = criteria.getNpa();
			final Query q = LuceneHelper.getTermsCommence(TiersIndexableData.NPA, npa, 0);
			if (q != null) {
				fullQuery.add(q, must);
			}
		}
	}

	private void addNumeroAVS(BooleanQuery fullQuery) throws IndexerException {

		// Numero AVS
		if (StringUtils.isNotBlank(criteria.getNumeroAVS())) { // [UNIREG-2592]
			final String noAVS = IndexerFormatHelper.formatNumeroAVS(criteria.getNumeroAVS());
			final Query q = LuceneHelper.getTermsCommence(TiersIndexableData.NUMERO_ASSURE_SOCIAL, noAVS, 0);
			if (q != null) {
				fullQuery.add(q, must);
			}
		}
	}

	private void addDateNaissance(BooleanQuery fullQuery) throws IndexerException {

		// Date de naissance
		if (criteria.getDateNaissance() != null) {
			final Query q = LuceneHelper.getTermsCommence(TiersIndexableData.DATE_NAISSANCE, RegDateHelper.toIndexString(criteria.getDateNaissance()), 0);
			fullQuery.add(q, must);
		}
	}

	private void addNatureJuridique(BooleanQuery fullQuery) throws IndexerException {

		if (StringUtils.isNotBlank(criteria.getNatureJuridique())) { // [UNIREG-2592]
			final Query q = new TermQuery(new Term(TiersIndexableData.NATURE_JURIDIQUE, criteria.getNatureJuridique()));
			fullQuery.add(q, must);
		}
	}

	public static void addLimitation(BooleanQuery fullQuery, TiersFilter filter) {
		if (filter.getTypeVisualisation() == TypeVisualisation.LIMITEE) {
			BooleanQuery query = new BooleanQuery();
			// restriction des DPI
			query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, DebiteurPrestationImposableIndexable.SUB_TYPE)), should);
			fullQuery.add(query, mustNot);
			// restriction des gris
			query = new BooleanQuery();
			query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), must);
			query.add(LuceneHelper.getTermsExact(TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name()), must);
			fullQuery.add(query, mustNot);
			// restriction des I107
			query = new BooleanQuery();
			query.add(new TermQuery(new Term(TiersIndexableData.DEBITEUR_INACTIF, Constants.NON)), must);
			fullQuery.add(query, must);
		}
	}

	public static void addAnnule(BooleanQuery fullQuery, TiersFilter filter) throws IndexerException {

		if (!filter.isInclureTiersAnnules()) {
			final Query q = new TermQuery(new Term(TiersIndexableData.ANNULE, Constants.NON));
			fullQuery.add(q, must);
		}
	}

	public static void addActif(BooleanQuery fullQuery, TiersFilter filter) throws IndexerException {

		if (filter.isTiersAnnulesSeulement()) {
			final Query q = new TermQuery(new Term(TiersIndexableData.ANNULE, Constants.OUI));
			fullQuery.add(q, must);
		}
	}

	public static void addDebiteurInactif(BooleanQuery fullQuery, TiersFilter filter) throws IndexerException {

		if (!filter.isInclureI107()) {
			final Query q = new TermQuery(new Term(TiersIndexableData.DEBITEUR_INACTIF, Constants.NON));
			fullQuery.add(q, must);
		}
	}

	private void addModeImposition(BooleanQuery fullQuery) throws IndexerException {

		if (criteria.getModeImposition() != null) {
			final Query q = new TermQuery(new Term(TiersIndexableData.MODE_IMPOSITION, criteria.getModeImposition().name()));
			fullQuery.add(q, must);
		}
	}

	private void addNumeroSymic(BooleanQuery fullQuery) throws IndexerException {

		if (StringUtils.isNotBlank(criteria.getNoSymic())) { // [UNIREG-2592]
			final Query q = new TermQuery(new Term(TiersIndexableData.NO_SYMIC, criteria.getNoSymic().toLowerCase()));
			fullQuery.add(q, must);
		}
	}

	private void addCategorieDebiteurIs(BooleanQuery fullQuery) throws IndexerException {

		if (criteria.getCategorieDebiteurIs() != null) {
			final Query q = new TermQuery(new Term(TiersIndexableData.CATEGORIE_DEBITEUR_IS, criteria.getCategorieDebiteurIs().name()));
			fullQuery.add(q, must);
		}
	}

	public static void addTiersActif(BooleanQuery fullQuery, TiersFilter filter) throws IndexerException {

		if (filter.isTiersActif() != null) {
			final String value = (filter.isTiersActif() ? Constants.OUI : Constants.NON);
			final Query q = new TermQuery(new Term(TiersIndexableData.TIERS_ACTIF, value));
			fullQuery.add(q, must);
		}
	}

	public Query constructQuery() throws IndexerException {

		BooleanQuery fullQuery = new BooleanQuery();

		addTypeTiers(fullQuery, criteria);

		if (criteria.getNumero() != null) {
			// Si on a un NUMERO CTB, on ne recherche que sur celui-ci
			addNumero(fullQuery);
		}
		else {
			// Sinon, on recherche sur les autres critères
			addNomRaison(fullQuery);
			addFors(fullQuery);
			addLocalitePays(fullQuery);
			addNpa(fullQuery);
			addNumeroAVS(fullQuery);
			addDateNaissance(fullQuery);
			addNatureJuridique(fullQuery);
			addAnnule(fullQuery, criteria);
			addActif(fullQuery, criteria);
			addDebiteurInactif(fullQuery, criteria);
			addModeImposition(fullQuery);
			addNumeroSymic(fullQuery);
			addCategorieDebiteurIs(fullQuery);
			addTiersActif(fullQuery, criteria);
		}

		addLimitation(fullQuery, criteria);

		BooleanClause[] clauses = fullQuery.getClauses();
		if (clauses != null && clauses.length > 0) {
			return fullQuery;
		}
		else {
			return null;
		}
	}

	/**
	 * @return une nouvelle requête avec des critères de recherches sur les critères wildcard plus large (c'est-à-dire en ayant supprimé les critères
	 *         les plus courts, de manière à éviter une exception de type BooleanQuery.TooManyClause)
	 */
	public QueryConstructor constructBroaderQuery() {
		return new QueryConstructor(this.criteria, this.tokenMinLength + 1);
	}
}

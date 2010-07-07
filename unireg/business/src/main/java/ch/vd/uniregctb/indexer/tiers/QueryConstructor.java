package ch.vd.uniregctb.indexer.tiers;

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
import ch.vd.uniregctb.indexer.LuceneEngine;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeVisualisation;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class QueryConstructor {

	private static int INTIAL_NAME_LEN = 2;

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

	private void addTypeTiers(BooleanQuery fullQuery) throws IndexerException {

		// Type de tiers
		if ((criteria.getTypeTiers() != null) && (!criteria.getTypeTiers().equals(TypeTiers.TIERS))) {
			BooleanQuery query = new BooleanQuery();
			TiersCriteria.TypeTiers typeTiers = criteria.getTypeTiers();
			switch (typeTiers) {
			case DEBITEUR_PRESTATION_IMPOSABLE:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, DebiteurPrestationImposableIndexable.SUB_TYPE)), should);
				break;
			case HABITANT:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
				break;
			case NON_HABITANT:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
				break;
			case MENAGE_COMMUN:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, MenageCommunIndexable.SUB_TYPE)), should);
				break;
			case ENTREPRISE:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, EntrepriseIndexable.SUB_TYPE)), should);
				break;
			case AUTRE_COMMUNAUTE:
				break;
			// TODO(JEC) Indexer pour etablissement et collectiviteadministrative
			// case ETABLISSEMENT:
			// query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, EtablissementIndexable.SUB_TYPE)), should);
			// break;
			// case COLLECTIVITE_ADMINISTRATIVE:
			// query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, CollectiviteAdministrative.SUB_TYPE)), should);
			// break;
			case CONTRIBUABLE:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, EntrepriseIndexable.SUB_TYPE)), should);
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, MenageCommunIndexable.SUB_TYPE)), should);
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, AutreCommunauteIndexable.SUB_TYPE)), should);
				// TODO(JEC) Indexer pour etablissement et collectiviteadministrative
				// query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, EtablissementIndexable.SUB_TYPE)), should);
				// query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, CollectiviteAdministrative.SUB_TYPE)), should);
				break;
			case PERSONNE_PHYSIQUE:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
				break;
			case NON_HABITANT_OU_MENAGE_COMMUN:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, MenageCommunIndexable.SUB_TYPE)), should);
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
				break;
			case CONTRIBUABLE_PP:
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
				query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, MenageCommunIndexable.SUB_TYPE)), should);
				break;
			}
			fullQuery.add(query, must);
		}
	}

	private void addNumero(BooleanQuery fullQuery) throws IndexerException {

		// Numero CTB ou individu ou AncienNumeroSourcier
		if (criteria.getNumero() != null) {
			BooleanQuery query = new BooleanQuery();
			query.add(new TermQuery(new Term(LuceneEngine.F_ENTITYID, criteria.getNumero().toString())), should);
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
				query.add(LuceneEngine.getTermsFuzzy(TiersIndexableData.NOM_RAISON, nomCourrier), should);
				query.add(LuceneEngine.getTermsFuzzy(TiersIndexableData.AUTRES_NOM, nomCourrier), should);
				break;
			case EST_EXACTEMENT:
				query.add(LuceneEngine.getTermsExact(TiersIndexableData.NOM_RAISON, nomCourrier), should);
				query.add(LuceneEngine.getTermsExact(TiersIndexableData.AUTRES_NOM, nomCourrier), should);
				break;
			case CONTIENT:
			default:
				final Query subQuery = LuceneEngine.getTermsContient(TiersIndexableData.NOM_RAISON, nomCourrier, tokenMinLength);
				if (subQuery != null) {
					query.add(subQuery, should);
				}
				final Query subQuery2 = LuceneEngine.getTermsContient(TiersIndexableData.AUTRES_NOM, nomCourrier, tokenMinLength);
				if (subQuery2 != null) {
					query.add(subQuery2, should);
				}
				break;
			}
			fullQuery.add(query, BooleanClause.Occur.MUST);
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

			BooleanQuery query = new BooleanQuery();
			if (criteria.isForPrincipalActif()) {
				// Recherche sur les fors principaux actifs uniquement
				query.add(LuceneEngine.getTermsExact(TiersIndexableData.NO_OFS_FOR_PRINCIPAL, numeroOfsFor), should);
			}
			else {
				// Recherche sur tous les fors (principaux, secondaires, inactifs, ...)
				query.add(LuceneEngine.getTermsExact(TiersIndexableData.NO_OFS_FOR_PRINCIPAL, numeroOfsFor), should);
				query.add(LuceneEngine.getTermsContient(TiersIndexableData.NOS_OFS_AUTRES_FORS, numeroOfsFor, 0), should);
			}

			fullQuery.add(query, BooleanClause.Occur.MUST);
		}
	}

	private void addLocalitePays(BooleanQuery fullQuery) throws IndexerException {

		// Localite ou Pays
		if (StringUtils.isNotBlank(criteria.getLocaliteOuPays())) { // [UNIREG-2592]
			final String nomLocaliteOuPays = criteria.getLocaliteOuPays().toLowerCase();
			final Query q = LuceneEngine.getTermsCommence(TiersIndexableData.LOCALITE_PAYS, nomLocaliteOuPays, tokenMinLength);
			fullQuery.add(q, must);
		}
	}

	private void addNpa(BooleanQuery fullQuery) throws IndexerException {

		// Localite ou Pays
		if (StringUtils.isNotBlank(criteria.getNpa())) { // [UNIREG-2592]
			final String npa = criteria.getNpa();
			final Query q = LuceneEngine.getTermsCommence(TiersIndexableData.NPA, npa, 0);
			fullQuery.add(q, must);
		}
	}

	private void addNumeroAVS(BooleanQuery fullQuery) throws IndexerException {

		// Numero AVS
		if (StringUtils.isNotBlank(criteria.getNumeroAVS())) { // [UNIREG-2592]
			final String noAVS = IndexerFormatHelper.formatNumeroAVS(criteria.getNumeroAVS());
			final Query q = LuceneEngine.getTermsCommence(TiersIndexableData.NUMERO_ASSURE_SOCIAL, noAVS, 0);
			fullQuery.add(q, must);
		}
	}

	private void addDateNaissance(BooleanQuery fullQuery) throws IndexerException {

		// Date de naissance
		if (criteria.getDateNaissance() != null) {
			final Query q = LuceneEngine.getTermsCommence(TiersIndexableData.DATE_NAISSANCE, RegDateHelper.toIndexString(criteria.getDateNaissance()), 0);
			fullQuery.add(q, must);
		}
	}

	private void addNatureJuridique(BooleanQuery fullQuery) throws IndexerException {

		if (StringUtils.isNotBlank(criteria.getNatureJuridique())) { // [UNIREG-2592]
			final Query q = new TermQuery(new Term(TiersIndexableData.NATURE_JURIDIQUE, criteria.getNatureJuridique().toLowerCase()));
			fullQuery.add(q, must);
		}
	}

	private void addLimitation(BooleanQuery fullQuery) {
		if (criteria.getTypeVisualisation().equals(TypeVisualisation.LIMITEE)) {
			BooleanQuery query = new BooleanQuery();
			// restriction des DPI
			query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, DebiteurPrestationImposableIndexable.SUB_TYPE)), should);
			fullQuery.add(query, mustNot);
			// restriction des gris
			query = new BooleanQuery();
			query.add(new TermQuery(new Term(LuceneEngine.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), must);
			query.add(LuceneEngine.getTermsExact(TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					.toString()), must);
			fullQuery.add(query, mustNot);
			// restriction des I107
			query = new BooleanQuery();
			query.add(LuceneEngine.getTermsExact(TiersIndexableData.DEBITEUR_INACTIF, Constants.NON), must);
			fullQuery.add(query, must);
		}
	}

	private void addAnnule(BooleanQuery fullQuery) throws IndexerException {

		if (!criteria.isInclureTiersAnnules()) {
			final Query q = new TermQuery(new Term(TiersIndexableData.ANNULE, Constants.NON.toLowerCase()));
			fullQuery.add(q, must);
		}
	}

	private void addActif(BooleanQuery fullQuery) throws IndexerException {

		if (criteria.isTiersAnnulesSeulement()) {
			final Query q = new TermQuery(new Term(TiersIndexableData.ANNULE, Constants.OUI.toLowerCase()));
			fullQuery.add(q, must);
		}
	}

	private void addDebiteurInactif(BooleanQuery fullQuery) throws IndexerException {

		if (!criteria.isInclureI107()) {
			final Query q = new TermQuery(new Term(TiersIndexableData.DEBITEUR_INACTIF, Constants.NON.toLowerCase()));
			fullQuery.add(q, must);
		}
	}

	private void addModeImposition(BooleanQuery fullQuery) throws IndexerException {

		if (criteria.getModeImposition() != null) {
			final Query q = new TermQuery(new Term(TiersIndexableData.MODE_IMPOSITION, criteria.getModeImposition().toString().toLowerCase()));
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
			final Query q = new TermQuery(new Term(TiersIndexableData.CATEGORIE_DEBITEUR_IS, criteria.getCategorieDebiteurIs().toString().toLowerCase()));
			fullQuery.add(q, must);
		}
	}

	private void addTiersActif(BooleanQuery fullQuery) throws IndexerException {

		if (criteria.isTiersActif() != null) {
			final String value = (criteria.isTiersActif() ? Constants.OUI.toLowerCase() : Constants.NON.toLowerCase());
			final Query q = new TermQuery(new Term(TiersIndexableData.TIERS_ACTIF, value));
			fullQuery.add(q, must);
		}
	}

	public Query constructQuery() throws IndexerException {

		BooleanQuery fullQuery = new BooleanQuery();

		addTypeTiers(fullQuery);

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
			addAnnule(fullQuery);
			addActif(fullQuery);
			addDebiteurInactif(fullQuery);
			addModeImposition(fullQuery);
			addNumeroSymic(fullQuery);
			addCategorieDebiteurIs(fullQuery);
			addTiersActif(fullQuery);
		}

		addLimitation(fullQuery);

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

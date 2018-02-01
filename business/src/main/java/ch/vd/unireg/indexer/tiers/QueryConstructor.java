package ch.vd.unireg.indexer.tiers;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.common.Constants;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.IndexerFormatHelper;
import ch.vd.unireg.indexer.lucene.LuceneHelper;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersCriteria.TypeTiers;
import ch.vd.unireg.tiers.TiersCriteria.TypeVisualisation;
import ch.vd.unireg.tiers.TiersFilter;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;

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

	public static void addTypeTiers(BooleanQuery fullQuery, Set<TypeTiers> set) throws IndexerException {

		// Type de tiers
		if (set != null && !set.isEmpty()) {

			BooleanQuery query = new BooleanQuery();
			for (TypeTiers typeTiers : set) {
				if (typeTiers == null) { // SIFISC-4752
					continue;
				}
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
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, AutreCommunauteIndexable.SUB_TYPE)), should);
					break;
				case ETABLISSEMENT:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, EtablissementIndexable.SUB_TYPE)), should);
					break;
				case ETABLISSEMENT_PRINCIPAL:
				case ETABLISSEMENT_SECONDAIRE: {
					final BooleanQuery subQuery = new BooleanQuery();
					subQuery.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, EtablissementIndexable.SUB_TYPE)), must);
					subQuery.add(new TermQuery(new Term(TiersIndexableData.TYPE_ETABLISSEMENT, typeTiers == TypeTiers.ETABLISSEMENT_PRINCIPAL ? TypeEtablissement.PRINCIPAL.name() : TypeEtablissement.SECONDAIRE.name())), must);
					query.add(subQuery, should);
					break;
				}
				case COLLECTIVITE_ADMINISTRATIVE:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, CollectiviteAdministrativeIndexable.SUB_TYPE)), should);
					break;
				case CONTRIBUABLE:
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, HabitantIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, NonHabitantIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, EntrepriseIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, MenageCommunIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, AutreCommunauteIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, CollectiviteAdministrativeIndexable.SUB_TYPE)), should);
					query.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, EtablissementIndexable.SUB_TYPE)), should);
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
		if (StringUtils.isNotBlank(criteria.getNpaCourrier())) { // [UNIREG-2592]
			final String npa = criteria.getNpaCourrier();
			final Query q = LuceneHelper.getTermsCommence(TiersIndexableData.NPA_COURRIER, npa, 0);
			if (q != null) {
				fullQuery.add(q, must);
			}
		}

		addCriterionCommence(fullQuery, TiersIndexableData.NPA_TOUS, 0, criteria.getNpaTous(), IndexerFormatHelper.DEFAULT_RENDERER);
	}

	private void addNumeroAVS(BooleanQuery fullQuery) throws IndexerException {
		// Numero AVS (11 ou 13, indifféremment)
		if (StringUtils.isNotBlank(criteria.getNumeroAVS())) {
			final BooleanQuery boolQuery = new BooleanQuery();
			boolQuery.add(LuceneHelper.getTermsExact(TiersIndexableData.NAVS11, IndexerFormatHelper.noAvsToString(criteria.getNumeroAVS())), should);
			boolQuery.add(LuceneHelper.getTermsExact(TiersIndexableData.NAVS13, IndexerFormatHelper.noAvsToString(criteria.getNumeroAVS())), should);
			fullQuery.add(boolQuery, must);
		}

		// Numéro AVS 11 précisément
		addCriterionCommence(fullQuery, TiersIndexableData.NAVS11, 0, criteria.getNavs11(), IndexerFormatHelper.AVS_RENDERER);

		// Numéro AVS 13 précisément
		addCriterionExact(fullQuery, TiersIndexableData.NAVS13, criteria.getNavs13(), IndexerFormatHelper.AVS_RENDERER);
	}

	private void addDateNaissanceInscriptionRC(BooleanQuery fullQuery) throws IndexerException {
		// Date de naissance
		addCriterionCommence(fullQuery, TiersIndexableData.S_DATE_NAISSANCE_INSCRIPTION_RC, 0, criteria.getDateNaissanceInscriptionRC(), IndexerFormatHelper.STORAGE_REGDATE_RENDERER);
	}

	private void addSexe(BooleanQuery fullQuery) throws IndexerException {
		// Sexe
		addCriterionExact(fullQuery, TiersIndexableData.SEXE, criteria.getSexe(), IndexerFormatHelper.DEFAULT_RENDERER);
	}

	private static <T extends Serializable> void addCriterionExact(BooleanQuery fullQuery, String field, TiersCriteria.ValueOrNull<T> criterionValue, StringRenderer<? super T> renderer) {
		if (criterionValue != null) {
			if (criterionValue.orNull) {
				final Query bNull = LuceneHelper.getTermsExact(field, IndexerFormatHelper.nullValue());
				if (criterionValue.value == null) {
					// value MUST be null
					fullQuery.add(bNull, must);
				}
				else {
					final Query bValue = LuceneHelper.getTermsExact(field, renderer.toString(criterionValue.value));
					if (bValue != null) {
						final BooleanQuery b = new BooleanQuery();
						b.add(bValue, should);
						b.add(bNull, should);
						fullQuery.add(b, must);
					}
					else {
						fullQuery.add(bNull, must);
					}
				}
			}
			else {
				if (criterionValue.value == null) {
					throw new IllegalArgumentException("On ne devrait pas pouvoir avoir orNull à false et une valeur cible nulle... : " + field);
				}
				final Query q = LuceneHelper.getTermsExact(field, renderer.toString(criterionValue.value));
				if (q != null) {
					fullQuery.add(q, must);
				}
			}
		}
	}

	private static <T extends Serializable> void addCriterionCommence(BooleanQuery fullQuery, String field, int minLength, TiersCriteria.ValueOrNull<T> criterionValue, StringRenderer<? super T> renderer) {
		if (criterionValue != null) {
			if (criterionValue.orNull) {
				final Query bNull = LuceneHelper.getTermsExact(field, IndexerFormatHelper.nullValue());
				if (criterionValue.value == null) {
					// value MUST be null
					fullQuery.add(bNull, must);
				}
				else {
					final Query bValue = LuceneHelper.getTermsCommence(field, renderer.toString(criterionValue.value), minLength);
					if (bValue != null) {
						final BooleanQuery b = new BooleanQuery();
						b.add(bValue, should);
						b.add(bNull, should);
						fullQuery.add(b, must);
					}
					else {
						fullQuery.add(bNull, must);
					}
				}
			}
			else {
				if (criterionValue.value == null) {
					throw new IllegalArgumentException("On ne devrait pas pouvoir avoir orNull à false et une valeur cible nulle... : " + field);
				}
				final Query q = LuceneHelper.getTermsCommence(field, renderer.toString(criterionValue.value), minLength);
				if (q != null) {
					fullQuery.add(q, must);
				}
			}
		}
	}

	private void addNatureJuridique(BooleanQuery fullQuery) throws IndexerException {

		if (StringUtils.isNotBlank(criteria.getNatureJuridique())) { // [UNIREG-2592]
			final Query q = new TermQuery(new Term(TiersIndexableData.NATURE_JURIDIQUE, criteria.getNatureJuridique()));
			fullQuery.add(q, must);
		}
	}

	private void addFormeJuridique(BooleanQuery fullQuery) throws IndexerException {

		if (criteria.getFormeJuridique() != null) {
			final Query q = new TermQuery(new Term(TiersIndexableData.FORME_JURIDIQUE, criteria.getFormeJuridique().getCodeECH()));
			fullQuery.add(q, must);
		}
	}

	private void addCategorieEntreprise(BooleanQuery fullQuery) throws IndexerException {

		if (criteria.getCategorieEntreprise() != null) {
			final Query q = new TermQuery(new Term(TiersIndexableData.CATEGORIE_ENTREPRISE, criteria.getCategorieEntreprise().name()));
			fullQuery.add(q, must);
		}
	}

	public static void addContrainteVisualisationLimitee(BooleanQuery fullQuery, TiersFilter filter) {
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

	public void addContrainteEtatsEntrepriseInterdits(BooleanQuery fullQuery) {
		if (criteria.getEtatsEntrepriseInterdits() != null && !criteria.getEtatsEntrepriseInterdits().isEmpty()) {
			final BooleanQuery query = new BooleanQuery();
			for (TypeEtatEntreprise etat : criteria.getEtatsEntrepriseInterdits()) {
				query.add(new TermQuery(new Term(TiersIndexableData.ETATS_ENTREPRISE, etat.name())), should);
			}
			fullQuery.add(query, mustNot);
		}
	}

	public void addContrainteEtatCourantEntrepriseInterdit(BooleanQuery fullQuery) {
		if (criteria.getEtatsEntrepriseCourantsInterdits() != null && !criteria.getEtatsEntrepriseCourantsInterdits().isEmpty()) {
			final BooleanQuery query = new BooleanQuery();
			for (TypeEtatEntreprise etat : criteria.getEtatsEntrepriseCourantsInterdits()) {
				query.add(new TermQuery(new Term(TiersIndexableData.ETAT_ENTREPRISE_COURANT, etat.name())), should);
			}
			fullQuery.add(query, mustNot);
		}
	}

	public void addEtatEntrepriseCourant(BooleanQuery fullQuery) {
		if (criteria.getEtatEntrepriseCourant() != null) {
			final TermQuery q = new TermQuery(new Term(TiersIndexableData.ETAT_ENTREPRISE_COURANT, criteria.getEtatEntrepriseCourant().name()));
			fullQuery.add(q, must);
		}
	}

	public void addContrainteAbsorptionEntreprisePassee(BooleanQuery fullQuery) {
		if (criteria.getCorporationMergeResult() != null) {
			final TermQuery q = new TermQuery(new Term(TiersIndexableData.CORPORATION_IS_MERGE_RESULT, criteria.getCorporationMergeResult() ? Constants.OUI : Constants.NON));
			fullQuery.add(q, must);
		}
	}

	public void addContrainteScissionEntreprisePassee(BooleanQuery fullQuery) {
		if (criteria.getCorporationSplit() != null) {
			final TermQuery q = new TermQuery(new Term(TiersIndexableData.CORPORATION_WAS_SPLIT, criteria.getCorporationSplit() ? Constants.OUI : Constants.NON));
			fullQuery.add(q, must);
		}
	}

	public void addContrainteEmissionPatrimoinePassee(BooleanQuery fullQuery) {
		if (criteria.hasCorporationTransferedPatrimony() != null) {
			final TermQuery q = new TermQuery(new Term(TiersIndexableData.CORPORATION_TRANSFERED_PATRIMONY, criteria.hasCorporationTransferedPatrimony() ? Constants.OUI : Constants.NON));
			fullQuery.add(q, must);
		}
	}

	public void addContrainteConnuAuCivil(BooleanQuery fullQuery) {
		if (criteria.getConnuAuCivil() != null) {
			final TermQuery q = new TermQuery(new Term(TiersIndexableData.CONNU_CIVIL, criteria.getConnuAuCivil() ? Constants.OUI : Constants.NON));
			fullQuery.add(q, must);
		}
	}

	public void addContrainteMotifFermetureDernierForPrincipal(BooleanQuery fullQuery) {
		final Set<MotifFor> motifsAttendus = criteria.getMotifsFermetureDernierForPrincipal();
		if (motifsAttendus != null) {
			final BooleanQuery q = new BooleanQuery();
			if (motifsAttendus.isEmpty()) {
				// moyen de chercher les fors non-fermés (est-ce vraiment nécessaire ? on ne sait pas distinguer les cas "sans for" des cas "for actif" avec ce critère seul, de toute façon)
				q.add(new TermQuery(new Term(TiersIndexableData.MOTIF_FERMETURE_DERNIER_FOR_PRINCIPAL, IndexerFormatHelper.nullValue())), should);
			}
			else {
				// on cherche l'un des motifs donnés
				for (MotifFor motif : motifsAttendus) {
					q.add(new TermQuery(new Term(TiersIndexableData.MOTIF_FERMETURE_DERNIER_FOR_PRINCIPAL, motif.name())), should);
				}
			}
			fullQuery.add(q, must);
		}
	}

	public void addEtatInscriptionRC(BooleanQuery fullQuery) {
		final TiersCriteria.TypeInscriptionRC etatInscriptionRC = criteria.getEtatInscriptionRC();
		if (etatInscriptionRC != null) {
			if (etatInscriptionRC == TiersCriteria.TypeInscriptionRC.INSCRIT_ACTIF) {
				final Query q = new TermQuery(new Term(TiersIndexableData.INSCRIPTION_RC, TypeEtatInscriptionRC.ACTIVE.name()));
				fullQuery.add(q, must);
			}
			else if (etatInscriptionRC == TiersCriteria.TypeInscriptionRC.INSCRIT_RADIE) {
				final Query q = new TermQuery(new Term(TiersIndexableData.INSCRIPTION_RC, TypeEtatInscriptionRC.RADIEE.name()));
				fullQuery.add(q, must);
			}
			else {
				final BooleanQuery q = new BooleanQuery();
				q.add(new TermQuery(new Term(TiersIndexableData.INSCRIPTION_RC, TypeEtatInscriptionRC.ACTIVE.name())), should);
				q.add(new TermQuery(new Term(TiersIndexableData.INSCRIPTION_RC, TypeEtatInscriptionRC.RADIEE.name())), should);
				fullQuery.add(q, etatInscriptionRC == TiersCriteria.TypeInscriptionRC.AVEC_INSCRIPTION ? must : mustNot);
			}
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

	private static void addAncienNumeroSourcier(BooleanQuery fullQuery, TiersCriteria criteria) throws IndexerException {

		if (criteria.getAncienNumeroSourcier() != null) { // [SIFISC-5846]
			final Query q = new TermQuery(new Term(TiersIndexableData.ANCIEN_NUMERO_SOURCIER, criteria.getAncienNumeroSourcier().toString()));
			fullQuery.add(q, must);
		}
	}

	private static void addNumeroIDE(BooleanQuery fullQuery, TiersCriteria criteria) throws IndexerException {

		if (StringUtils.isNotBlank(criteria.getNumeroIDE())) {
			final Query q = new TermQuery(new Term(TiersIndexableData.NUM_IDE, IndexerFormatHelper.noIdeToString(criteria.getNumeroIDE()).toLowerCase(Locale.getDefault())));
			fullQuery.add(q, must);
		}
	}

	private static void addNumeroRC(BooleanQuery fullQuery, TiersCriteria criteria) throws IndexerException {

		if (StringUtils.isNotBlank(criteria.getNumeroRC())) {
			final Query q = new TermQuery(new Term(TiersIndexableData.NUM_RC, IndexerFormatHelper.numRCToString(criteria.getNumeroRC()).toLowerCase(Locale.getDefault())));
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

		final BooleanQuery fullQuery = new BooleanQuery();
		final Set<TypeTiers> typesTiers = EnumSet.noneOf(TypeTiers.class);
		if (criteria.getTypesTiersImperatifs() != null) {
			typesTiers.addAll(criteria.getTypesTiersImperatifs());
		}

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
			addDateNaissanceInscriptionRC(fullQuery);
			addSexe(fullQuery);
			addNatureJuridique(fullQuery);
			addFormeJuridique(fullQuery);
			addCategorieEntreprise(fullQuery);
			addAnnule(fullQuery, criteria);
			addActif(fullQuery, criteria);
			addDebiteurInactif(fullQuery, criteria);
			addModeImposition(fullQuery);
			addNumeroSymic(fullQuery);
			addAncienNumeroSourcier(fullQuery, criteria);
			addCategorieDebiteurIs(fullQuery);
			addTiersActif(fullQuery, criteria);
			addNumeroIDE(fullQuery, criteria);
			addNumeroRC(fullQuery, criteria);

			final Set<TypeTiers> typesTiersUtilisateur = criteria.getTypesTiers();
			if (typesTiersUtilisateur != null && !typesTiersUtilisateur.isEmpty()) {
				if (typesTiers.isEmpty()) {
					// pas de contraintes métiers (= recherche globale) -> on prend en compte les demandes utilisateurs telles qu'elles
					typesTiers.addAll(typesTiersUtilisateur);
				}
				else {
					// en raison des recouvrements qui existent entre les différents types de tiers tels qu'exprimables
					// par l'énuméré TypeTiers, c'est un peu compliqué de faire l'intersection entre ces deux ensembles
					// (par exemple si l'un des ensemble contient PersonnePhysique et l'autre Habitant, ils ne sont pas
					// du tout incompatibles, car l'un est un sous-ensemble de l'autre, mais cela ne se voit pas sans une
					// analyse plus approfondie, analyse que nous ne ferons pas ici pour le moment parce que le cas où ces deux
					// critères existent en même temps n'est pas (encore ?) utilisé dans Unireg)
					throw new NotImplementedException("Utilisation simultanée des critères de type de tiers métier et utilisateur");
				}
			}
		}

		addTypeTiers(fullQuery, typesTiers);
		addContrainteVisualisationLimitee(fullQuery, criteria);
		addContrainteEtatsEntrepriseInterdits(fullQuery);
		addContrainteEtatCourantEntrepriseInterdit(fullQuery);
		addEtatInscriptionRC(fullQuery);
		addEtatEntrepriseCourant(fullQuery);
		addContrainteAbsorptionEntreprisePassee(fullQuery);
		addContrainteScissionEntreprisePassee(fullQuery);
		addContrainteEmissionPatrimoinePassee(fullQuery);
		addContrainteMotifFermetureDernierForPrincipal(fullQuery);
		addContrainteConnuAuCivil(fullQuery);

		final BooleanClause[] clauses = fullQuery.getClauses();
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

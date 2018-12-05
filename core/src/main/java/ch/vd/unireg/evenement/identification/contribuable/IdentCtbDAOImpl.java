package ch.vd.unireg.evenement.identification.contribuable;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.dbutils.QueryFragment;
import ch.vd.unireg.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable.Etat;

public class IdentCtbDAOImpl extends BaseDAOImpl<IdentificationContribuable, Long> implements IdentCtbDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentCtbDAOImpl.class);

	private static final String TOUS = "TOUS";

	public IdentCtbDAOImpl() {
		super(IdentificationContribuable.class);
	}

	@Override
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                             boolean suspenduOnly, TypeDemande... typeDemande) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationContribuableDAO:find");
		}

		final Map<String, Object> criteria = new HashMap<>();
		final String queryWhere = buildCriterion(criteria, identificationContribuableCriteria, nonTraiteOnly, archiveOnly, suspenduOnly);
		return executeSearch(paramPagination, criteria, queryWhere, typeDemande);
	}

	@Override
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination,IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationContribuableDAO:find");
		}

		final Map<String, Object> criteria = new HashMap<>();
		final String queryWhere = buildCriterion(criteria, identificationContribuableCriteria,filter);
		return executeSearch(paramPagination, criteria, queryWhere, typeDemande);
	}

	private static String buildWhereAvecTypeDemande(String tableName, TypeDemande... typeDemande) {
		if (typeDemande == null || typeDemande.length < 1) {
			return " 1=1 ";
		}
		else if (typeDemande.length == 1) {
			if (typeDemande[0] == null) {
				return String.format(" %s.demande.typeDemande is null ", tableName);
			}
			else {
				return String.format(" %s.demande.typeDemande = '%s' ", tableName, typeDemande[0]);
			}
		}
		else {
			final StringBuilder b = new StringBuilder();
			for (int i = 0 ; i < typeDemande.length ; ++i) {
				if (i > 0) {
					b.append(",");
				}
				b.append("'").append(typeDemande[i]).append("'");
			}
			return String.format(" %s.demande.typeDemande in (%s) ", tableName, b.toString());
		}
	}

	@SuppressWarnings("unchecked")
	private List<IdentificationContribuable> executeSearch(final ParamPagination paramPagination, final Map<String, ?> criteriaWhere, String queryWhere, TypeDemande... typeDemande) {
		final QueryFragment fragment = new QueryFragment("select identificationContribuable from IdentificationContribuable identificationContribuable where");
		fragment.add(buildWhereAvecTypeDemande("identificationContribuable", typeDemande));
		fragment.add(queryWhere, criteriaWhere);
		fragment.add(paramPagination.buildOrderClause("identificationContribuable", null, true, null));

		final int firstResult = paramPagination.getSqlFirstResult();
		final int maxResult = paramPagination.getSqlMaxResults();

		final Session session = getCurrentSession();
		final Query queryObject = fragment.createQuery(session);

		queryObject.setFirstResult(firstResult);
		queryObject.setMaxResults(maxResult);
		return queryObject.list();
	}

	/**
	 * @param nonTraiteOnly
	 * @param typeDemande
	 */
	@Override
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly,
	                 boolean suspenduOnly, TypeDemande... typeDemande) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationContribuableDAO:count");
		}
		if (identificationContribuableCriteria == null) {
			throw new IllegalArgumentException("Les critères de recherche peuvent pas être nuls");
		}
		final Map<String, Object> criteria = new HashMap<>();
		String queryWhere = buildCriterion(criteria, identificationContribuableCriteria, nonTraiteOnly, archiveOnly, suspenduOnly);

		final String selectBase = "select count(*) from IdentificationContribuable identificationContribuable where";
		final String whereTypeDemande = buildWhereAvecTypeDemande("identificationContribuable", typeDemande);
		final String query = selectBase + whereTypeDemande + queryWhere;
		return DataAccessUtils.intResult(find(query, criteria, null));
	}

	@Override
	public List<IdentificationContribuable> find(TypeDemande typeDemande, String emetteur, String businessIdStart) {
		final String hql = "FROM IdentificationContribuable ic WHERE ic.demande.typeDemande=:typeDemande AND ic.demande.emetteurId=:emetteur AND ic.header.businessId LIKE :businessIdStart";
		final Query query = getCurrentSession().createQuery(hql);
		query.setParameter("typeDemande", typeDemande);
		query.setParameter("emetteur", emetteur);
		query.setParameter("businessIdStart", businessIdStart + '%');
		//noinspection unchecked
		return query.list();
	}

	/**
	 * @param typeDemande
	 */
	@Override
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria,IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationContribuableDAO:count");
		}
		if (identificationContribuableCriteria == null) {
			throw new IllegalArgumentException("Les critères de recherche peuvent pas être nuls");
		}
		final Map<String, Object> criteria = new HashMap<>();
		String queryWhere = buildCriterion(criteria, identificationContribuableCriteria, filter);

		final String selectBase = "select count(*) from IdentificationContribuable identificationContribuable where";
		final String whereTypeDemande = buildWhereAvecTypeDemande("identificationContribuable", typeDemande);
		final String query = selectBase + whereTypeDemande + queryWhere;
		return DataAccessUtils.intResult(find(query, criteria, null));
	}

	@SuppressWarnings("unchecked")
	public Map<TypeDemande, Map<Etat, List<String>>> getTypesMessages() {
		final String hql = "select distinct i.demande.typeMessage, i.demande.typeDemande, i.etat from IdentificationContribuable i";
		final Session session = getCurrentSession();

		final Query query = session.createQuery(hql);
		final Map<TypeDemande, Map<Etat, List<String>>> globalMap = new EnumMap<>(TypeDemande.class);
		final Iterator<Object[]> iter = query.iterate();
		while (iter.hasNext()) {
			final Object[] row = iter.next();
			final String typeMessage = (String) row[0];
			final TypeDemande typeDemande = (TypeDemande) row[1];
			final Etat etat = (Etat) row[2];
			final Map<Etat, List<String>> mapPourTypeDonne = globalMap.computeIfAbsent(typeDemande, k -> new EnumMap<>(Etat.class));
			final List<String> typesMessages = mapPourTypeDonne.computeIfAbsent(etat, k -> new LinkedList<>());
			typesMessages.add(typeMessage);
		}
		return globalMap;
	}

	private static final Function<Object, Integer> INTEGER_UNMARSHALLER = o -> Optional.ofNullable((Number) o).map(Number::intValue).orElse(null);

	private static final Function<Object, String> STRING_UNMARSHALLER = String.class::cast;

	private static final Function<Object, Etat> ETAT_UNMARSHALLER = Etat.class::cast;

	private static final Function<Object, PrioriteEmetteur> PRIORITE_UNMARSHALLER = PrioriteEmetteur.class::cast;

	/**
	 * Récupère la liste des valeurs d'un champ particulier par état de la demande d'identification
	 * @param champHql le nom du champ à récupérer (ex : i.demande.typeMessage)
	 * @param unmarshaller transformateur d'objet en valeur typée
	 * @param <T> le type du champ spécifique
	 * @return la map de répartition trouvée
	 */
	@SuppressWarnings("unchecked")
	private <T> Map<Etat, List<T>> getDonneesParEtat(String champHql, final Function<Object, ? extends T> unmarshaller) {
		final String hql = "select distinct " + champHql + ", i.etat from IdentificationContribuable i";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		final Map<Etat, List<T>> map = new EnumMap<>(Etat.class);
		final Iterator<Object[]> iterator = query.iterate();
		while (iterator.hasNext()) {
			final Object[] row = iterator.next();
			final T value = unmarshaller.apply(row[0]);
			final Etat etat = (Etat) row[1];
			final List<T> ids = map.computeIfAbsent(etat, k -> new LinkedList<>());
			ids.add(value);
		}
		return map;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Etat, List<String>> getEmetteursIds() {
		return getDonneesParEtat("i.demande.emetteurId", STRING_UNMARSHALLER);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Etat, List<Integer>> getPeriodesFiscales() {
		return getDonneesParEtat("i.demande.periodeFiscale", INTEGER_UNMARSHALLER);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getTraitementUser() {
		final String query = " select distinct identificationContribuable.traitementUser " +
				"from IdentificationContribuable identificationContribuable where identificationContribuable.traitementUser is not null " +
				"and identificationContribuable.traitementUser not like '%JMS-EvtIdentCtb%' ";
		return find(query, null);
	}

	@Override
	public Map<Etat, List<Etat>> getEtats() {
		return getDonneesParEtat("i.etat", ETAT_UNMARSHALLER);
	}

	@Override
	public Map<IdentificationContribuable.Etat, List<Demande.PrioriteEmetteur>> getPriorites() {
		return getDonneesParEtat("i.demande.prioriteEmetteur", PRIORITE_UNMARSHALLER);
	}

	/**
	 * Construit la clause Where
	 */
	private String buildCriterion(Map<String, Object> criteria, IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly, boolean suspenduOnly) {

		final IdentificationContribuableEtatFilter filter;
		if (nonTraiteOnly) {
			filter = IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES;
		}
		else if (archiveOnly) {
			filter = IdentificationContribuableEtatFilter.SEULEMENT_TRAITES;
		}
		else if (suspenduOnly) {
			filter = IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS;
		}
		else {
			filter = null;
		}
		return buildCriterion(criteria, identificationContribuableCriteria, filter);
	}

	/**
	 * Construit la clause Where
	 */
	private String buildCriterion(Map<String, Object> criteria, IdentificationContribuableCriteria identificationContribuableCriteria, IdentificationContribuableEtatFilter filter) {
		String queryWhere = "";

		final String typeMessage = identificationContribuableCriteria.getTypeMessage();
		if ((typeMessage != null) && (!TOUS.equals(typeMessage))) {
			queryWhere += " and identificationContribuable.demande.typeMessage = :typeMessage";
			criteria.put("typeMessage", typeMessage);
		}

		final Integer periodeFiscale = identificationContribuableCriteria.getPeriodeFiscale();
		if ((periodeFiscale != null) && (periodeFiscale != -1)) {
			queryWhere += " and identificationContribuable.demande.periodeFiscale = :pf";
			criteria.put("pf", periodeFiscale);
		}

		String visaUser = identificationContribuableCriteria.getTraitementUser();
		if ((visaUser != null) && (!TOUS.equals(visaUser))) {
			if ("Traitement automatique".equals(visaUser)) {
				visaUser = "%JMS-EvtIdentCtb%";
				queryWhere += " and identificationContribuable.traitementUser like :visaUser";

			}
			else {
				queryWhere += " and identificationContribuable.traitementUser = :visaUser";
			}

			criteria.put("visaUser", visaUser);
		}

		final Date dateTraitementDebut = identificationContribuableCriteria.getDateTraitementDebut();
		if (dateTraitementDebut != null) {
			queryWhere += " and identificationContribuable.dateTraitement >= :dateTraitementMin";
			// On prends la date a Zero Hour
			criteria.put("dateTraitementMin", dateTraitementDebut);
		}
		final Date dateTraitementFin = identificationContribuableCriteria.getDateTraitementFin();
		if (dateTraitementFin != null) {
			queryWhere += " and identificationContribuable.dateTraitement <= :dateTraitementMax";
			// On prends la date a 24 Hour
			criteria.put("dateTraitementMax", dateTraitementFin);
		}

		final String emetteurId = identificationContribuableCriteria.getEmetteurId();
		if ((emetteurId != null) && (!TOUS.equals(emetteurId))) {
			queryWhere += " and identificationContribuable.demande.emetteurId = :emetteurId";
			criteria.put("emetteurId", emetteurId);
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final PrioriteEmetteur prioriteEmetteur = identificationContribuableCriteria.getPrioriteEmetteur();
		if (prioriteEmetteur != null) {
			queryWhere += " and identificationContribuable.demande.prioriteEmetteur = :prioEmetteur";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Priorité émetteur : " + prioriteEmetteur);
			}
			criteria.put("prioEmetteur", prioriteEmetteur);
		}

		Date dateMessageDebut = identificationContribuableCriteria.getDateMessageDebut();
		if (dateMessageDebut != null) {
			queryWhere += " and identificationContribuable.demande.date >= :dateDemandeMin";
			// On prends la date a Zero Hour
			criteria.put("dateDemandeMin", dateMessageDebut);
		}
		Date dateMessageFin = identificationContribuableCriteria.getDateMessageFin();
		if (dateMessageFin != null) {
			queryWhere += " and identificationContribuable.demande.date <= :dateDemandeMax";
			// On prends la date a 24 Hour
			Calendar calendar = Calendar.getInstance();
			// Initialisé à la date de fin.
			calendar.setTime(dateMessageFin);
			calendar.add(Calendar.HOUR, 23);
			calendar.add(Calendar.MINUTE, 59);
			dateMessageFin = calendar.getTime();

			criteria.put("dateDemandeMax", dateMessageFin);
		}

		if (filter == IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES) {
			queryWhere = buildCriterionEtatNonTraite(criteria, queryWhere, identificationContribuableCriteria);
		}
		else if (filter == IdentificationContribuableEtatFilter.SEULEMENT_TRAITES) {
			queryWhere = buildCriterionEtatArchive(criteria, queryWhere, identificationContribuableCriteria);
		}
		else if (filter == IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS) {
			queryWhere = buildCriterionEtatSuspendu(criteria, queryWhere, identificationContribuableCriteria);
		}
		else if (filter == IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION) {
			queryWhere = buildCriterionEtatNonTraiteEtException(criteria, queryWhere, identificationContribuableCriteria);
		}
		else {
			queryWhere = buildCriterionEtat(criteria, queryWhere, identificationContribuableCriteria);
		}

		String nom = identificationContribuableCriteria.getNom();
		if (StringUtils.isNotEmpty(nom)) {
			queryWhere += " and upper(identificationContribuable.demande.personne.nom) = upper(:nom) ";
			criteria.put("nom", nom);
		}

		String prenoms = identificationContribuableCriteria.getPrenoms();
		if (StringUtils.isNotEmpty(prenoms)) {
			queryWhere += " and upper(identificationContribuable.demande.personne.prenoms) = upper(:prenoms) ";
			criteria.put("prenoms", prenoms);
		}

		String navs13 = identificationContribuableCriteria.getNAVS13();
		String navs13WithoutDot = StringUtils.replace(navs13, ".", "");
		if (StringUtils.isNotEmpty(navs13)) {
			queryWhere += " and identificationContribuable.demande.personne.NAVS13 in (:navs13)";
			criteria.put("navs13", Arrays.asList(navs13, navs13WithoutDot));
		}
		String navs11 = identificationContribuableCriteria.getNAVS11();
		String navs11WithoutDot = StringUtils.replace(navs11, ".", "");
		if (StringUtils.isNotEmpty(navs11)) {
			queryWhere += " and identificationContribuable.demande.personne.NAVS11 in (:navs11)";
			criteria.put("navs11", Arrays.asList(navs11, navs11WithoutDot));
		}

		RegDate dateNaissance = identificationContribuableCriteria.getDateNaissance();
		if (dateNaissance != null) {
			if (dateNaissance.isPartial()) {
				RegDate[] partialDateRange = dateNaissance.getPartialDateRange();
				queryWhere += " and (identificationContribuable.demande.personne.dateNaissance = :dateNaissance or identificationContribuable.demande.personne.dateNaissance between :dateNaissanceMin and :dateNaissanceMax)";
				criteria.put("dateNaissance", dateNaissance);
				criteria.put("dateNaissanceMin", partialDateRange[0]);
				criteria.put("dateNaissanceMax", partialDateRange[1]);
			}
			else {
				queryWhere += " and identificationContribuable.demande.personne.dateNaissance = :dateNaissance";
				criteria.put("dateNaissance", dateNaissance);
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("IdentificationContribuableCriteria Query: " + queryWhere);
			LOGGER.trace("IdentificationContribuableCriteria Table size: " + criteria.size());
		}
		return queryWhere;
	}

	/**
	 * Construit le critere avec les état initialisés à non Traité
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtatNonTraite(Map<String, Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final Etat etat = identificationContribuableCriteria.getEtatMessage();
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			final List<Etat> etats = Arrays.asList(Etat.A_TRAITER_MANUELLEMENT, Etat.A_EXPERTISER);
			queryWhere += " and identificationContribuable.etat in (:etats)";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + Arrays.toString(etats.toArray(new Etat[0])));
			}
			criteria.put("etats", etats);
		}
		else {
			queryWhere += " and identificationContribuable.etat = :etat";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.put("etat", etat);
		}

		return queryWhere;
	}

	private String buildCriterionEtatNonTraiteEtException(Map<String, Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final Etat etat = identificationContribuableCriteria.getEtatMessage();
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			final List<Etat> etats = Arrays.asList(Etat.A_TRAITER_MANUELLEMENT, Etat.A_EXPERTISER, Etat.EXCEPTION);
			queryWhere += " and identificationContribuable.etat in (:etats)";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + Arrays.asList(etats.toArray(new Etat[0])));
			}
			criteria.put("etats", etats);
		}
		else {
			queryWhere += " and identificationContribuable.etat = :etat";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.put("etat", etat);
		}

		return queryWhere;
	}

	/**
	 * Construit le critere avec les états archivés
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtatArchive(Map<String, Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final Etat etat = identificationContribuableCriteria.getEtatMessage();
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			final List<Etat> etats = Arrays.asList(Etat.TRAITE_AUTOMATIQUEMENT, Etat.NON_IDENTIFIE, Etat.TRAITE_MANUELLEMENT, Etat.TRAITE_MAN_EXPERT);
			queryWhere += " and identificationContribuable.etat in (:etats)";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + Arrays.toString(etats.toArray(new Etat[0])));
			}
			criteria.put("etats", etats);
		}
		else {
			queryWhere += " and identificationContribuable.etat = :etat";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.put("etat", etat);
		}

		return queryWhere;
	}


	/**
	 * Construit le critere avec les états en cours, a expertiser
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtatNonTraiteOrSuspendu(Map<String, Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final Etat etat = identificationContribuableCriteria.getEtatMessage();
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			final List<Etat> etats = Arrays.asList(Etat.A_EXPERTISER, Etat.A_EXPERTISER_SUSPENDU, Etat.A_TRAITER_MANUELLEMENT, Etat.A_TRAITER_MAN_SUSPENDU);
			queryWhere += " and identificationContribuable.etat in (:etats)";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + Arrays.toString(etats.toArray(new Etat[0])));
			}
			criteria.put("etats", etats);
		}
		else {
			queryWhere += " and identificationContribuable.etat = :etat";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.put("etat", etat);
		}

		return queryWhere;
	}

	/**
	 * Construit le critere avec les états en cours, a expertiser
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtatSuspendu(Map<String, Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final Etat etat = identificationContribuableCriteria.getEtatMessage();
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			final List<Etat> etats = Arrays.asList(Etat.A_EXPERTISER_SUSPENDU, Etat.A_TRAITER_MAN_SUSPENDU);
			queryWhere += " and identificationContribuable.etat in (:etats) ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + Arrays.toString(etats.toArray(new Etat[0])));
			}
			criteria.put("etats", etats);
		}
		else {
			queryWhere += " and identificationContribuable.etat = :etat";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.put("etat", etat);
		}

		return queryWhere;
	}


	/**
	 * Construit le critere avec l'état passé en paramètre
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtat(Map<String, Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final Etat etat = identificationContribuableCriteria.getEtatMessage();
		if (etat != null) {
			if (Etat.SUSPENDU == etat) {
				final List<Etat> etats = Arrays.asList(Etat.A_TRAITER_MAN_SUSPENDU, Etat.A_EXPERTISER_SUSPENDU, Etat.SUSPENDU);
				queryWhere += " and identificationContribuable.etat in (:etats)";
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Etat identification CTB: " + Arrays.toString(etats.toArray(new Etat[0])));
				}
				criteria.put("etats", etats);
			}
			else {
				queryWhere += " and identificationContribuable.etat = :etat";
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Etat identification CTB: " + etat);
				}
				criteria.put("etat", etat);
			}
		}

		return queryWhere;
	}
}

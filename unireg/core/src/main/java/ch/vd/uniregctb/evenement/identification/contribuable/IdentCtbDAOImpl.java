package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;

public class IdentCtbDAOImpl extends GenericDAOImpl<IdentificationContribuable, Long> implements IdentCtbDAO {

	private static final Logger LOGGER = Logger.getLogger(IdentCtbDAOImpl.class);

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

		final List<Object> criteria = new ArrayList<>();
		final String queryWhere = buildCriterion(criteria, identificationContribuableCriteria, nonTraiteOnly, archiveOnly, suspenduOnly);
		return executeSearch(paramPagination, criteria, queryWhere, typeDemande);
	}

	@Override
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination,IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationContribuableDAO:find");
		}

		final List<Object> criteria = new ArrayList<>();
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
	private List<IdentificationContribuable> executeSearch(final ParamPagination paramPagination, final List<Object> criteriaWhere, String queryWhere, TypeDemande... typeDemande) {
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
		Assert.notNull(identificationContribuableCriteria, "Les critères de recherche peuvent pas être nuls");
		final List<Object> criteria = new ArrayList<>();
		String queryWhere = buildCriterion(criteria, identificationContribuableCriteria, nonTraiteOnly, archiveOnly, suspenduOnly);

		final String selectBase = "select count(*) from IdentificationContribuable identificationContribuable where";
		final String whereTypeDemande = buildWhereAvecTypeDemande("identificationContribuable", typeDemande);
		final String query = selectBase + whereTypeDemande + queryWhere;
		return DataAccessUtils.intResult(find(query, criteria.toArray(), null));
	}

	/**
	 * @param nonTraiteOnly
	 * @param typeDemande
	 */
	@Override
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria,IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationContribuableDAO:count");
		}
		Assert.notNull(identificationContribuableCriteria, "Les critères de recherche peuvent pas être nuls");
		final List<Object> criteria = new ArrayList<>();
		String queryWhere = buildCriterion(criteria, identificationContribuableCriteria,filter);

		final String selectBase = "select count(*) from IdentificationContribuable identificationContribuable where";
		final String whereTypeDemande = buildWhereAvecTypeDemande("identificationContribuable", typeDemande);
		final String query = selectBase + whereTypeDemande + queryWhere;
		return DataAccessUtils.intResult(find(query, criteria.toArray(), null));
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
			Map<Etat, List<String>> mapPourTypeDonne = globalMap.get(typeDemande);
			if (mapPourTypeDonne == null) {
				mapPourTypeDonne = new EnumMap<>(Etat.class);
				globalMap.put(typeDemande, mapPourTypeDonne);
			}
			List<String> typesMessages = mapPourTypeDonne.get(etat);
			if (typesMessages == null) {
				typesMessages = new LinkedList<>();
				mapPourTypeDonne.put(etat, typesMessages);
			}
			typesMessages.add(typeMessage);
		}
		return globalMap;
	}

	private static interface Unmarshaller<T> {
		T buildValue(Object o);
	}

	private static final Unmarshaller<Integer> INTEGER_UNMARSHALLER = new Unmarshaller<Integer>() {
		@Override
		public Integer buildValue(Object o) {
			return ((Number) o).intValue();
		}
	};

	private static final Unmarshaller<String> STRING_UNMARSHALLER = new Unmarshaller<String>() {
		@Override
		public String buildValue(Object o) {
			return (String) o;
		}
	};

	private static final Unmarshaller<Etat> ETAT_UNMARSHALLER = new Unmarshaller<Etat>() {
		@Override
		public Etat buildValue(Object o) {
			return (Etat) o;
		}
	};

	private static final Unmarshaller<PrioriteEmetteur> PRIORITE_UNMARSHALLER = new Unmarshaller<PrioriteEmetteur>() {
		@Override
		public PrioriteEmetteur buildValue(Object o) {
			return (PrioriteEmetteur) o;
		}
	};

	/**
	 * Récupère la liste des valeurs d'un champ particulier par état de la demande d'identification
	 * @param champHql le nom du champ à récupérer (ex : i.demande.typeMessage)
	 * @param unmarshaller transformateur d'objet en valeur typée
	 * @param <T> le type du champ spécifique
	 * @return la map de répartition trouvée
	 */
	@SuppressWarnings("unchecked")
	private <T> Map<Etat, List<T>> getDonneesParEtat(String champHql, final Unmarshaller<T> unmarshaller) {
		final String hql = "select distinct " + champHql + ", i.etat from IdentificationContribuable i";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		final Map<Etat, List<T>> map = new EnumMap<>(Etat.class);
		final Iterator<Object[]> iterator = query.iterate();
		while (iterator.hasNext()) {
			final Object[] row = iterator.next();
			final T value = unmarshaller.buildValue(row[0]);
			final Etat etat = (Etat) row[1];
			List<T> ids = map.get(etat);
			if (ids == null) {
				ids = new LinkedList<>();
				map.put(etat, ids);
			}
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
		String query = " select distinct identificationContribuable.traitementUser " +
				"from IdentificationContribuable identificationContribuable where identificationContribuable.traitementUser is not null " +
				"and identificationContribuable.traitementUser not like '%JMS-EvtIdentCtb%' ";
		return (List<String>) find(query, null, null);
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
	private String buildCriterion(List<Object> criteria, IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly,
	                              boolean suspenduOnly) {
		String queryWhere = "";

		String typeMessage = identificationContribuableCriteria.getTypeMessage();
		if ((typeMessage != null) && (!TOUS.equals(typeMessage))) {
			queryWhere += " and identificationContribuable.demande.typeMessage = ? ";
			criteria.add(typeMessage);
		}

		Integer periodeFiscale = identificationContribuableCriteria.getPeriodeFiscale();
		if ((periodeFiscale != null) && (periodeFiscale != -1)) {
			queryWhere += " and identificationContribuable.demande.periodeFiscale = ? ";
			criteria.add(periodeFiscale);
		}

		String visaUser = identificationContribuableCriteria.getTraitementUser();
		if ((visaUser != null) && (!TOUS.equals(visaUser))) {
			if ("Traitement automatique".equals(visaUser)) {
				visaUser = "%JMS-EvtIdentCtb%";
				queryWhere += " and identificationContribuable.traitementUser like ? ";

			}
			else {
				queryWhere += " and identificationContribuable.traitementUser = ? ";
			}

			criteria.add(visaUser);
		}

		Date dateTraitementDebut = identificationContribuableCriteria.getDateTraitementDebut();
		if (dateTraitementDebut != null) {
			queryWhere += " and identificationContribuable.dateTraitement >= ? ";
			// On prends la date a Zero Hour
			criteria.add(dateTraitementDebut);
		}
		Date dateTraitementFin = identificationContribuableCriteria.getDateTraitementFin();
		if (dateTraitementFin != null) {
			queryWhere += " and identificationContribuable.dateTraitement <= ? ";
			// On prends la date a 24 Hour
			criteria.add(dateTraitementFin);
		}


		String emetteurId = identificationContribuableCriteria.getEmetteurId();
		if ((emetteurId != null) && (!TOUS.equals(emetteurId))) {
			queryWhere += " and identificationContribuable.demande.emetteurId = ? ";
			criteria.add(emetteurId);
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		PrioriteEmetteur prioriteEmetteur;
		try {
			prioriteEmetteur = PrioriteEmetteur.valueOf(identificationContribuableCriteria.getPrioriteEmetteur());
		}
		catch (Exception e) {
			prioriteEmetteur = null; // pas de priorité => TOUS
		}

		if (prioriteEmetteur != null) {
			queryWhere += " and identificationContribuable.demande.prioriteEmetteur = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Priorité émetteur : " + prioriteEmetteur);
			}
			criteria.add(prioriteEmetteur.name());
		}

		Date dateMessageDebut = identificationContribuableCriteria.getDateMessageDebut();
		if (dateMessageDebut != null) {
			queryWhere += " and identificationContribuable.demande.date >= ? ";
			// On prends la date a Zero Hour
			criteria.add(dateMessageDebut);
		}
		Date dateMessageFin = identificationContribuableCriteria.getDateMessageFin();
		if (dateMessageFin != null) {
			queryWhere += " and identificationContribuable.demande.date <= ? ";
			// On prends la date a 24 Hour
			Calendar calendar = Calendar.getInstance();
			// Initialisé à la date de fin.
			calendar.setTime(dateMessageFin);
			calendar.add(Calendar.HOUR, 23);
			calendar.add(Calendar.MINUTE, 59);
			dateMessageFin = calendar.getTime();

			criteria.add(dateMessageFin);
		}

		if (nonTraiteOnly) {
			queryWhere = buildCriterionEtatNonTraite(criteria, queryWhere, identificationContribuableCriteria);
		}
		else if (archiveOnly) {
			queryWhere = buildCriterionEtatArchive(criteria, queryWhere, identificationContribuableCriteria);
		}
		else if (suspenduOnly) {
			queryWhere = buildCriterionEtatSuspendu(criteria, queryWhere, identificationContribuableCriteria);
		}
		else {
			queryWhere = buildCriterionEtat(criteria, queryWhere, identificationContribuableCriteria);
		}


		String nom = identificationContribuableCriteria.getNom();
		if (StringUtils.isNotEmpty(nom)) {
			queryWhere += " and upper(identificationContribuable.demande.personne.nom) = upper(?) ";
			criteria.add(nom);
		}

		String prenoms = identificationContribuableCriteria.getPrenoms();
		if (StringUtils.isNotEmpty(prenoms)) {
			queryWhere += " and upper(identificationContribuable.demande.personne.prenoms) = upper(?) ";
			criteria.add(prenoms);
		}

		String navs13 = identificationContribuableCriteria.getNAVS13();
		String navs13WithoutDot = StringUtils.replace(navs13, ".", "");
		if (StringUtils.isNotEmpty(navs13)) {
			queryWhere += " and (identificationContribuable.demande.personne.NAVS13 = ? or identificationContribuable.demande.personne.NAVS13 = ?) ";
			criteria.add(navs13);
			criteria.add(navs13WithoutDot);
		}
		String navs11 = identificationContribuableCriteria.getNAVS11();
		String navs11WithoutDot = StringUtils.replace(navs11, ".", "");
		if (StringUtils.isNotEmpty(navs11)) {
			queryWhere += " and (identificationContribuable.demande.personne.NAVS11 = ? or identificationContribuable.demande.personne.NAVS11 = ?) ";
			criteria.add(navs11);
			criteria.add(navs11WithoutDot);
		}

		RegDate dateNaissance = identificationContribuableCriteria.getDateNaissance();
		if (dateNaissance != null) {
			if (dateNaissance.isPartial()) {
				RegDate[] partialDateRange = dateNaissance.getPartialDateRange();
				queryWhere += " and ((identificationContribuable.demande.personne.dateNaissance = ?) " +
						"or ((identificationContribuable.demande.personne.dateNaissance >= ?) and (identificationContribuable.demande.personne.dateNaissance <= ?)))";
				criteria.add(dateNaissance.index());
				criteria.add(partialDateRange[0].index());
				criteria.add(partialDateRange[1].index());
			}
			else {
				queryWhere += " and (identificationContribuable.demande.personne.dateNaissance = ?) ";
				criteria.add(dateNaissance.index());
			}

		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("IdentificationContribuableCriteria Query: " + queryWhere);
			LOGGER.trace("IdentificationContribuableCriteria Table size: " + criteria.toArray().length);
		}
		return queryWhere;
	}


	/**
	 * Construit la clause Where
	 */
	private String buildCriterion(List<Object> criteria, IdentificationContribuableCriteria identificationContribuableCriteria,
	                              IdentificationContribuableEtatFilter filter) {
		String queryWhere = "";

		String typeMessage = identificationContribuableCriteria.getTypeMessage();
		if ((typeMessage != null) && (!TOUS.equals(typeMessage))) {
			queryWhere += " and identificationContribuable.demande.typeMessage = ? ";
			criteria.add(typeMessage);
		}

		Integer periodeFiscale = identificationContribuableCriteria.getPeriodeFiscale();
		if ((periodeFiscale != null) && (periodeFiscale != -1)) {
			queryWhere += " and identificationContribuable.demande.periodeFiscale = ? ";
			criteria.add(periodeFiscale);
		}

		String visaUser = identificationContribuableCriteria.getTraitementUser();
		if ((visaUser != null) && (!TOUS.equals(visaUser))) {
			if ("Traitement automatique".equals(visaUser)) {
				visaUser = "%JMS-EvtIdentCtb%";
				queryWhere += " and identificationContribuable.traitementUser like ? ";

			}
			else {
				queryWhere += " and identificationContribuable.traitementUser = ? ";
			}

			criteria.add(visaUser);
		}

		Date dateTraitementDebut = identificationContribuableCriteria.getDateTraitementDebut();
		if (dateTraitementDebut != null) {
			queryWhere += " and identificationContribuable.dateTraitement >= ? ";
			// On prends la date a Zero Hour
			criteria.add(dateTraitementDebut);
		}
		Date dateTraitementFin = identificationContribuableCriteria.getDateTraitementFin();
		if (dateTraitementFin != null) {
			queryWhere += " and identificationContribuable.dateTraitement <= ? ";
			// On prends la date a 24 Hour
			criteria.add(dateTraitementFin);
		}


		String emetteurId = identificationContribuableCriteria.getEmetteurId();
		if ((emetteurId != null) && (!TOUS.equals(emetteurId))) {
			queryWhere += " and identificationContribuable.demande.emetteurId = ? ";
			criteria.add(emetteurId);
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		PrioriteEmetteur prioriteEmetteur;
		try {
			prioriteEmetteur = PrioriteEmetteur.valueOf(identificationContribuableCriteria.getPrioriteEmetteur());
		}
		catch (Exception e) {
			prioriteEmetteur = null; // pas de priorité => TOUS
		}

		if (prioriteEmetteur != null) {
			queryWhere += " and identificationContribuable.demande.prioriteEmetteur = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Priorité émetteur : " + prioriteEmetteur);
			}
			criteria.add(prioriteEmetteur.name());
		}

		Date dateMessageDebut = identificationContribuableCriteria.getDateMessageDebut();
		if (dateMessageDebut != null) {
			queryWhere += " and identificationContribuable.demande.date >= ? ";
			// On prends la date a Zero Hour
			criteria.add(dateMessageDebut);
		}
		Date dateMessageFin = identificationContribuableCriteria.getDateMessageFin();
		if (dateMessageFin != null) {
			queryWhere += " and identificationContribuable.demande.date <= ? ";
			// On prends la date a 24 Hour
			Calendar calendar = Calendar.getInstance();
			// Initialisé à la date de fin.
			calendar.setTime(dateMessageFin);
			calendar.add(Calendar.HOUR, 23);
			calendar.add(Calendar.MINUTE, 59);
			dateMessageFin = calendar.getTime();

			criteria.add(dateMessageFin);
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
			queryWhere += " and upper(identificationContribuable.demande.personne.nom) = upper(?) ";
			criteria.add(nom);
		}

		String prenoms = identificationContribuableCriteria.getPrenoms();
		if (StringUtils.isNotEmpty(prenoms)) {
			queryWhere += " and upper(identificationContribuable.demande.personne.prenoms) = upper(?) ";
			criteria.add(prenoms);
		}

		String navs13 = identificationContribuableCriteria.getNAVS13();
		String navs13WithoutDot = StringUtils.replace(navs13, ".", "");
		if (StringUtils.isNotEmpty(navs13)) {
			queryWhere += " and (identificationContribuable.demande.personne.NAVS13 = ? or identificationContribuable.demande.personne.NAVS13 = ?) ";
			criteria.add(navs13);
			criteria.add(navs13WithoutDot);
		}
		String navs11 = identificationContribuableCriteria.getNAVS11();
		String navs11WithoutDot = StringUtils.replace(navs11, ".", "");
		if (StringUtils.isNotEmpty(navs11)) {
			queryWhere += " and (identificationContribuable.demande.personne.NAVS11 = ? or identificationContribuable.demande.personne.NAVS11 = ?) ";
			criteria.add(navs11);
			criteria.add(navs11WithoutDot);
		}

		RegDate dateNaissance = identificationContribuableCriteria.getDateNaissance();
		if (dateNaissance != null) {
			if (dateNaissance.isPartial()) {
				RegDate[] partialDateRange = dateNaissance.getPartialDateRange();
				queryWhere += " and ((identificationContribuable.demande.personne.dateNaissance = ?) " +
						"or ((identificationContribuable.demande.personne.dateNaissance >= ?) and (identificationContribuable.demande.personne.dateNaissance <= ?)))";
				criteria.add(dateNaissance.index());
				criteria.add(partialDateRange[0].index());
				criteria.add(partialDateRange[1].index());
			}
			else {
				queryWhere += " and (identificationContribuable.demande.personne.dateNaissance = ?) ";
				criteria.add(dateNaissance.index());
			}

		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("IdentificationContribuableCriteria Query: " + queryWhere);
			LOGGER.trace("IdentificationContribuableCriteria Table size: " + criteria.toArray().length);
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
	private String buildCriterionEtatNonTraite(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			Etat aTraiterManuellement = Etat.A_TRAITER_MANUELLEMENT;
			Etat aExpertiser = Etat.A_EXPERTISER;
			queryWhere += " and identificationContribuable.etat in(?,?) ";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + aTraiterManuellement + " - " + aExpertiser);
			}
			criteria.add(aTraiterManuellement.name());
			criteria.add(aExpertiser.name());
		}
		else {
			queryWhere += " and identificationContribuable.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.add(etat.name());
		}

		return queryWhere;
	}

	private String buildCriterionEtatNonTraiteEtException(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			Etat aTraiterManuellement = Etat.A_TRAITER_MANUELLEMENT;
			Etat aExpertiser = Etat.A_EXPERTISER;
			Etat enException = Etat.EXCEPTION;
			queryWhere += " and identificationContribuable.etat in(?,?,?) ";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + aTraiterManuellement + " - " + aExpertiser+" - "+enException);
			}
			criteria.add(aTraiterManuellement.name());
			criteria.add(aExpertiser.name());
			criteria.add(enException.name());
		}
		else {
			queryWhere += " and identificationContribuable.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.add(etat.name());
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
	private String buildCriterionEtatArchive(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			Etat traiterAutomatique = Etat.TRAITE_AUTOMATIQUEMENT;
			Etat nonIdentifie = Etat.NON_IDENTIFIE;
			Etat traiteManuellementCBO = Etat.TRAITE_MANUELLEMENT;
			Etat traiteManuellementGBO = Etat.TRAITE_MAN_EXPERT;
			queryWhere += " and identificationContribuable.etat in(?,?,?,?) ";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + traiterAutomatique + " - " + nonIdentifie + " - " + traiteManuellementCBO + " - " + traiteManuellementGBO);
			}
			criteria.add(traiterAutomatique.name());
			criteria.add(nonIdentifie.name());
			criteria.add(traiteManuellementCBO.name());
			criteria.add(traiteManuellementGBO.name());
		}
		else {
			queryWhere += " and identificationContribuable.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.add(etat.name());
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
	private String buildCriterionEtatNonTraiteOrSuspendu(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			Etat aExpertiser = Etat.A_EXPERTISER;
			Etat aExpertiserSuspendu = Etat.A_EXPERTISER_SUSPENDU;
			Etat aTraiterManuellement = Etat.A_TRAITER_MANUELLEMENT;
			Etat aTraiterManSuspendu = Etat.A_TRAITER_MAN_SUSPENDU;

			queryWhere += " and identificationContribuable.etat in(?,?,?,?) ";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + aExpertiser + " - " + aExpertiserSuspendu + " - " + aTraiterManuellement + " - " + aTraiterManSuspendu);
			}
			criteria.add(aExpertiser.name());
			criteria.add(aExpertiserSuspendu.name());
			criteria.add(aTraiterManuellement.name());
			criteria.add(aTraiterManSuspendu.name());
		}
		else {
			queryWhere += " and identificationContribuable.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.add(etat.name());
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
	private String buildCriterionEtatSuspendu(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			Etat aExpertiserSuspendu = Etat.A_EXPERTISER_SUSPENDU;
			Etat aTraiterManSuspendu = Etat.A_TRAITER_MAN_SUSPENDU;

			queryWhere += " and identificationContribuable.etat in(?,?) ";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + aExpertiserSuspendu + " - " + aTraiterManSuspendu);
			}
			criteria.add(aExpertiserSuspendu.name());
			criteria.add(aTraiterManSuspendu.name());
		}
		else {
			queryWhere += " and identificationContribuable.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.add(etat.name());
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
	private String buildCriterionEtat(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}

		if (etat != null) {
			if (Etat.SUSPENDU == etat) {
				Etat aTraiterManuellementSuspendu = Etat.A_TRAITER_MAN_SUSPENDU;
				Etat aExpertiserSuspendu = Etat.A_EXPERTISER_SUSPENDU;
				queryWhere += " and identificationContribuable.etat in(?,?,?) ";
				if (LOGGER.isTraceEnabled()) {

					LOGGER.trace("Etat identification CTB: " + aTraiterManuellementSuspendu + " - " + aExpertiserSuspendu + " - " + etat);
				}
				criteria.add(aTraiterManuellementSuspendu.name());
				criteria.add(aExpertiserSuspendu.name());
				criteria.add(etat.name());
			}
			else {
				queryWhere += " and identificationContribuable.etat = ? ";
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Etat identification CTB: " + etat);
				}
				criteria.add(etat.name());

			}

		}

		return queryWhere;
	}
}

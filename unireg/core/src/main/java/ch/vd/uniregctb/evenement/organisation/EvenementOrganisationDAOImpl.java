package ch.vd.uniregctb.evenement.organisation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationDAOImpl extends BaseDAOImpl<EvenementOrganisation, Long> implements EvenementOrganisationDAO {

	private static final Set<EtatEvenementOrganisation> ETATS_NON_TRAITES;
	private static final Set<EtatEvenementOrganisation> ETATS_TRAITES;

	static {
		final Set<EtatEvenementOrganisation> etatsNonTraites = EnumSet.noneOf(EtatEvenementOrganisation.class);
		final Set<EtatEvenementOrganisation> etatsTraitesSucces = EnumSet.noneOf(EtatEvenementOrganisation.class);
		for (EtatEvenementOrganisation etat : EtatEvenementOrganisation.values()) {
			if (etat.isTraite()) {
				if (etat != EtatEvenementOrganisation.FORCE) {
					etatsTraitesSucces.add(etat);
				}
			} else {
				etatsNonTraites.add(etat);
			}
		}
		ETATS_NON_TRAITES = Collections.unmodifiableSet(etatsNonTraites);
		ETATS_TRAITES = Collections.unmodifiableSet(etatsTraitesSucces);
	}

	public EvenementOrganisationDAOImpl() {
		super(EvenementOrganisation.class);
	}

	@Override
	public List<EvenementOrganisation> getEvenementsOrganisationNonTraites(long noOrganisation) {
		return getEvenementsOrganisationNonTraites(Collections.singletonList(noOrganisation), true);

	}

	@Override
	public List<EvenementOrganisation> getEvenementsOrganisation(long noOrganisation) {
		return getEvenementsOrganisationNonTraites(Collections.singletonList(noOrganisation), false);
	}

	@SuppressWarnings("unchecked")
	private List<EvenementOrganisation> getEvenementsOrganisationNonTraites(Collection<Long> nosOrganisation, boolean nonTraitesSeulement) {
		//final String hql = "from EvenementOrganisation as ec where ec.annulationDate is null and ec.noOrganisation in (:nosOrganisation)" + (nonTraitesSeulement ? " and ec.etat in (:etats)" : StringUtils.EMPTY);
		Criteria query = getCurrentSession().createCriteria(EvenementOrganisation.class, "eo");
		query.add(Restrictions.isNull("annulationDate"));
		query.add(Restrictions.in("noOrganisation", nosOrganisation));
		if (nonTraitesSeulement) {
			query.add(Restrictions.in("etat", ETATS_NON_TRAITES));
		}
		query.addOrder(Order.asc("dateEvenement"));
		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<EvenementOrganisation> getEvenementsOrganisationARelancer() {
		// final String hql = "from EvenementOrganisation as ec where ec.annulationDate is null and ec.etat = :etat";
		Criteria query = getCurrentSession().createCriteria(EvenementOrganisation.class, "eo");
		query.add(Restrictions.isNull("annulationDate"));
		query.add(Restrictions.eq("etat", EtatEvenementOrganisation.A_TRAITER));
		return query.list();
	}

	@Override
	@NotNull
	public List<EvenementOrganisation> getEvenementsOrganisationApresDateNonAnnules(Long noOrganisation, RegDate date) {
		final EvenementOrganisationCriteria<TypeEvenementOrganisation> criteria = new EvenementOrganisationCriteria<>();
		criteria.setNumeroOrganisation(noOrganisation);
		criteria.setRegDateEvenementDebut(date.getOneDayAfter());
		final List<EvenementOrganisation> trouves = find(criteria, null);
		final List<EvenementOrganisation> filtres = new ArrayList<>(trouves.size());
		for (EvenementOrganisation trouve : trouves) {
			if (!trouve.isAnnule()) {
				filtres.add(trouve);
			}
		}
		return filtres;
	}

	@Override
	public boolean isEvenementDateValeurDansLePasse(EvenementOrganisation event) {
		final EvenementOrganisationCriteria<TypeEvenementOrganisation> criteria = new EvenementOrganisationCriteria<>();
		criteria.setNumeroOrganisation(event.getNoOrganisation());
		criteria.setRegDateEvenementDebut(event.getDateEvenement().getOneDayAfter());
		final List<EvenementOrganisation> trouves = find(criteria, null);
		final List<EvenementOrganisation> filtres = new ArrayList<>(trouves.size());
		for (EvenementOrganisation trouve : trouves) {
			if (!trouve.isAnnule()) {
				filtres.add(trouve);
			}
		}
		final List<EvenementOrganisation> evtRecusAvant = new ArrayList<>();
		for (EvenementOrganisation evt : filtres) {
			if (evt.getId() < event.getId()) {
				evtRecusAvant.add(evt);
			}
		}
		return !evtRecusAvant.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<Long> getOrganisationsConcerneesParEvenementsPourRetry() {
		//final String hql = "select distinct ec.noOrganisation from EvenementOrganisation ec where ec.annulationDate is null and ec.noOrganisation is not null and ec.etat in (:etats)";
		Criteria query = getCurrentSession().createCriteria(EvenementOrganisation.class, "eo");
		query.add(Restrictions.isNull("annulationDate"));
		query.add(Restrictions.isNotNull("noOrganisation"));
		query.add(Restrictions.in("etat", ETATS_NON_TRAITES));
		query.setProjection(Projections.property("noOrganisation"));
		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return new HashSet<>(query.list());
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementOrganisation> find(final EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion, @Nullable final ParamPagination paramPagination) {
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");

		final Map<String, Object> paramsWhere = new HashMap<>();
		final String queryWhere = buildCriterion(paramsWhere, criterion);
		if (queryWhere == null) {
			return Collections.emptyList();
		}

		final String fromComplement = criterion.isJoinOnEntreprise() ? ", Entreprise en" : "";
		final String select = String.format("select evenement from %s evenement %s where 1=1 %s", EvenementOrganisation.class.getSimpleName(), fromComplement, queryWhere);
		final QueryFragment fragment = new QueryFragment(select, paramsWhere);

		// tri par défaut
		if (paramPagination != null) {
			fragment.add(paramPagination.buildOrderClause("evenement", "dateEvenement", true, null));
		}

		final Session session = getCurrentSession();
		final Query queryObject = fragment.createQuery(session);
		if (paramPagination != null) {
			final int firstResult = paramPagination.getSqlFirstResult();
			final int maxResult = paramPagination.getSqlMaxResults();

			queryObject.setFirstResult(firstResult);
			queryObject.setMaxResults(maxResult);
		}
		return queryObject.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementOrganisation> getEvenementsForNoEvenement(long noEvenement) {
		Criteria query = getCurrentSession().createCriteria(EvenementOrganisation.class, "eo");
		query.add(Restrictions.eq("noEvenement", noEvenement));
		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return query.list();
	}

	@Override
	public int count(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion) {
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");
		final Map<String, Object> criteria = new HashMap<>();
		String queryWhere = buildCriterion(criteria, criterion);
		String query = String.format(
				"select count(*) from %s evenement %s where 1=1 %s",
				EvenementOrganisation.class.getSimpleName(),
				criterion.isJoinOnEntreprise() ? ", Entreprise en": "",
				queryWhere);
		return DataAccessUtils.intResult(find(query, criteria, null));
	}

	/**
	 * @param criteria target
	 * @param criterion source
	 * @return la clause where correspondante à l'objet criterion
	 */
	protected String buildCriterion(Map<String, Object> criteria, EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion) {
		String queryWhere = "";

		// Si la valeur n'existe pas (TOUS par exemple), type = null
		final TypeEvenementOrganisation type = criterion.getType();
		if (type != null) {
			queryWhere += " and evenement.type = :type";
			criteria.put("type", type);
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final EtatEvenementOrganisation etat = criterion.getEtat();
		if (etat != null) {
			queryWhere += " and evenement.etat = :etat";
			criteria.put("etat", etat);
		}

		Date dateTraitementDebut = criterion.getDateTraitementDebut();
		if (dateTraitementDebut != null) {
			queryWhere += " and evenement.dateTraitement >= :dateTraitementMin";
			// On prends la date a Zero Hour
			criteria.put("dateTraitementMin", dateTraitementDebut);
		}

		Date dateTraitementFin = criterion.getDateTraitementFin();
		if (dateTraitementFin != null) {
			queryWhere += " and evenement.dateTraitement <= :dateTraitementMax";
			// On prends la date a 24 Hour
			criteria.put("dateTraitementMax", dateTraitementFin);
		}

		RegDate dateEvenementDebut = criterion.getRegDateEvenementDebut();
		if (dateEvenementDebut != null) {
			queryWhere += " and evenement.dateEvenement >= :dateEvtMin";
			criteria.put("dateEvtMin", dateEvenementDebut);
		}

		RegDate dateEvenementFin = criterion.getRegDateEvenementFin();
		if (dateEvenementFin != null) {
			queryWhere += " and evenement.dateEvenement <= :dateEvtMax";
			criteria.put("dateEvtMax", dateEvenementFin);
		}

		Long numero = criterion.getNumeroOrganisation();
		if (numero != null) {
			queryWhere += " and evenement.noOrganisation = :noOrganisation";
			criteria.put("noOrganisation", numero);
		}

		Long numeroCTB = criterion.getNumeroCTB();
		if (numeroCTB != null) {
			queryWhere += " and (evenement.noOrganisation = en.numeroEntreprise) and en.numero = :noCtb";
			criteria.put("noCtb", numeroCTB);
		}


		return queryWhere;
	}

}

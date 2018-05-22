package ch.vd.unireg.evenement.civil;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.dbutils.QueryFragment;
import ch.vd.unireg.type.EtatEvenementCivil;

public abstract class AbstractEvenementCivilDAOImpl<EVT, TYP_EVT extends Enum<TYP_EVT>> extends BaseDAOImpl<EVT, Long> {

	public AbstractEvenementCivilDAOImpl(Class<EVT> persistentClass) {
		super(persistentClass);
	}

	/**
	 * @param criteria target
	 * @param criterion source
	 * @return la clause where correspondante à l'objet criterion
	 */
	protected String buildCriterion(Map<String, Object> criteria, EvenementCivilCriteria<TYP_EVT> criterion) {
		String queryWhere = "";

		// Si la valeur n'existe pas (TOUS par exemple), type = null
		final TYP_EVT type = criterion.getType();
		if (type != null) {
			queryWhere += " and evenement.type = :type";
			criteria.put("type", type);
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final EtatEvenementCivil etat = criterion.getEtat();
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

		return queryWhere;
	}

	@SuppressWarnings("unchecked")
	protected List<EVT> genericFind(final EvenementCivilCriteria<TYP_EVT> criterion, @Nullable final ParamPagination paramPagination) {

		if (criterion == null) {
			throw new IllegalArgumentException("Les critères de recherche peuvent pas être nuls");
		}

		final Map<String, Object> paramsWhere = new HashMap<>();
		final String queryWhere = buildCriterion(paramsWhere, criterion);
		if (queryWhere == null) {
			return Collections.emptyList();
		}

		final String fromComplement = criterion.isJoinOnPersonnePhysique() ? ", PersonnePhysique pp" : "";
		final String select = String.format("select evenement from %s evenement %s where 1=1 %s", getEvenementCivilClass().getSimpleName(), fromComplement, queryWhere);
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

	protected int genericCount(EvenementCivilCriteria<TYP_EVT> criterion) {
		if (criterion == null) {
			throw new IllegalArgumentException("Les critères de recherche peuvent pas être nuls");
		}
		final Map<String, Object> criteria = new HashMap<>();
		String queryWhere = buildCriterion(criteria, criterion);
		String query = String.format(
				"select count(*) from %s evenement %s where 1=1 %s",
				getEvenementCivilClass().getSimpleName(),
				criterion.isJoinOnPersonnePhysique() ? ", PersonnePhysique pp" : "",
				queryWhere);
		return DataAccessUtils.intResult(find(query, criteria, null));
	}

	protected abstract Class getEvenementCivilClass();

}

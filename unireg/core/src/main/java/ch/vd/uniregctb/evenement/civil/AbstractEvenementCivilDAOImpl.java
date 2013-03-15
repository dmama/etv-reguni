package ch.vd.uniregctb.evenement.civil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;
import ch.vd.uniregctb.type.EtatEvenementCivil;

public abstract class AbstractEvenementCivilDAOImpl<EVT, TYP_EVT extends Enum<TYP_EVT>> extends GenericDAOImpl<EVT, Long> {

	public AbstractEvenementCivilDAOImpl(Class<EVT> persistentClass) {
		super(persistentClass);
	}

	/**
	 * @param criteria target
	 * @param criterion source
	 * @return la clause where correspondante à l'objet criterion
	 */
	protected String buildCriterion(List<Object> criteria, EvenementCivilCriteria<TYP_EVT> criterion) {
		String queryWhere = "";

		// Si la valeur n'existe pas (TOUS par exemple), type = null
		final TYP_EVT type = criterion.getType();
		if (type != null) {
			queryWhere += " and evenement.type = ? ";
			criteria.add(type.name());
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final EtatEvenementCivil etat = criterion.getEtat();
		if (etat != null) {
			queryWhere += " and evenement.etat = ? ";
			criteria.add(etat.name());
		}

		Date dateTraitementDebut = criterion.getDateTraitementDebut();
		if (dateTraitementDebut != null) {
			queryWhere += " and evenement.dateTraitement >= ? ";
			// On prends la date a Zero Hour
			criteria.add(dateTraitementDebut);
		}
		
		Date dateTraitementFin = criterion.getDateTraitementFin();
		if (dateTraitementFin != null) {
			queryWhere += " and evenement.dateTraitement <= ? ";
			// On prends la date a 24 Hour
			criteria.add(dateTraitementFin);
		}

		RegDate dateEvenementDebut = criterion.getRegDateEvenementDebut();
		if (dateEvenementDebut != null) {
			queryWhere += " and evenement.dateEvenement >= ? ";
			criteria.add(dateEvenementDebut.index());
		}
		
		RegDate dateEvenementFin = criterion.getRegDateEvenementFin();
		if (dateEvenementFin != null) {
			queryWhere += " and evenement.dateEvenement <= ? ";
			criteria.add(dateEvenementFin.index());
		}

		return queryWhere;
	}

	@SuppressWarnings("unchecked")
	protected List<EVT> genericFind(final EvenementCivilCriteria<TYP_EVT> criterion, @Nullable final ParamPagination paramPagination) {

		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");

		final List<Object> paramsWhere = new ArrayList<>();
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

	protected int genericCount(EvenementCivilCriteria<TYP_EVT> criterion){
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");
		List<Object> criteria = new ArrayList<>();
		String queryWhere =buildCriterion(criteria, criterion);
		String query = String.format(
				" select count(*) from %s evenement %s where 1=1 %s",
				getEvenementCivilClass().getSimpleName(),
				criterion.isJoinOnPersonnePhysique() ? ", PersonnePhysique pp": "",
				queryWhere);
		return DataAccessUtils.intResult(find(query, criteria.toArray(), null));
	}

	protected abstract Class getEvenementCivilClass();

}

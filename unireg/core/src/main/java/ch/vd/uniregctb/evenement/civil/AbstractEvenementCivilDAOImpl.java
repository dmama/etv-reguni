package ch.vd.uniregctb.evenement.civil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ParamPagination;
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

		Long numero = criterion.getNumeroIndividu();
		if (numero != null) {
			queryWhere += " and (evenement.numeroIndividuPrincipal = ? or evenement.numeroIndividuConjoint = ?) ";
			criteria.add(numero);
			criteria.add(numero);
		}

		Long numeroCTB = criterion.getNumeroCTB();
		if (numeroCTB != null) {
			queryWhere += "and (evenement.numeroIndividuPrincipal = pp.numeroIndividu or evenement.numeroIndividuConjoint = pp.numeroIndividu) and pp.numero = ?";
			criteria.add(numeroCTB);
		}

		return queryWhere;
	}

	@SuppressWarnings("unchecked")
	protected List<EVT> genericFind(final EvenementCivilCriteria<TYP_EVT> criterion, final ParamPagination paramPagination) {

		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");

		final List<Object> criteria = new ArrayList<Object>();
		final String queryWhere = buildCriterion(criteria, criterion);
		if (queryWhere == null) {
			return Collections.emptyList();
		}
		String queryOrder;
		if (paramPagination != null && paramPagination.getChamp() != null) {
			queryOrder = String.format(" order by evenement.%s", paramPagination.getChamp());
		} else {
			queryOrder = " order by evenement.dateEvenement";
		}
		if (paramPagination != null && paramPagination.isSensAscending()) {
			queryOrder = String.format("%s asc", queryOrder);
		} else {
			queryOrder = String.format("%s desc", queryOrder);
		}

		final String query = String.format(
				"select evenement from %s evenement %s where 1=1 %s%s",
				getEvenementCivilClass().getSimpleName(),
				criterion.isJoinOnPersonnePhysique() ? ", PersonnePhysique pp": "",
				queryWhere, queryOrder);

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<EVT>>() {
			@Override
			public List<EVT> doInHibernate(Session session) throws HibernateException, SQLException {

				final Query queryObject = session.createQuery(query);
				final Object[] values = criteria.toArray();
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				if (paramPagination != null) {
					final int firstResult = paramPagination.getSqlFirstResult();
					final int maxResult = paramPagination.getSqlMaxResults();

					queryObject.setFirstResult(firstResult);
					queryObject.setMaxResults(maxResult);
				}

				return queryObject.list();
			}
		});
	}

	protected int genericCount(EvenementCivilCriteria<TYP_EVT> criterion){
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");
		List<Object> criteria = new ArrayList<Object>();
		String queryWhere =buildCriterion(criteria, criterion);
		String query = String.format(
				" select count(*) from %s evenement %s where 1=1 %s",
				getEvenementCivilClass().getSimpleName(),
				criterion.isJoinOnPersonnePhysique() ? ", PersonnePhysique pp": "",
				queryWhere);
		return DataAccessUtils.intResult(getHibernateTemplate().find(query, criteria.toArray()));
	}

	protected abstract Class getEvenementCivilClass();

}

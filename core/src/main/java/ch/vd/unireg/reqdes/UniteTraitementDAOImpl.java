package ch.vd.uniregctb.reqdes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.common.pagination.ParamSorting;

public class UniteTraitementDAOImpl extends BaseDAOImpl<UniteTraitement, Long> implements UniteTraitementDAO {

	public UniteTraitementDAOImpl() {
		super(UniteTraitement.class);
	}

	private static Date toDate(@Nullable RegDate date) {
		return date != null ? date.asJavaDate() : null;
	}

	private static void fillCriteriaDates(Criteria dest, String field, @Nullable RegDate min, @Nullable RegDate max, boolean fieldIsRealDate) {
		final Object effectiveMin = fieldIsRealDate ? toDate(min) : min;
		final Object effectiveMax = fieldIsRealDate ? toDate(max) : max;
		if (effectiveMax != null || effectiveMin != null) {
			if (effectiveMax == null) {
				dest.add(Restrictions.ge(field, effectiveMin));
			}
			else if (effectiveMin == null) {
				dest.add(Restrictions.le(field, effectiveMax));
			}
			else {
				dest.add(Restrictions.between(field, effectiveMin, effectiveMax));
			}
		}
	}

	private static void fillCriteria(Criteria dest, UniteTraitementCriteria source) {
		final Criteria aliasEvenement = dest.createAlias("evenement", "evenement");
		dest.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		if (source.getEtatTraitement() != null) {
			dest.add(Restrictions.eq("etat", source.getEtatTraitement()));
		}
		if (source.getNumeroMinute() != null) {
			aliasEvenement.add(Restrictions.eq("evenement.numeroMinute", source.getNumeroMinute()));
		}
		if (source.getVisaNotaire() != null) {
			aliasEvenement.add(Restrictions.eq("evenement.notaire.visa", source.getVisaNotaire()));
		}
		fillCriteriaDates(aliasEvenement, "evenement.dateActe", source.getDateActeMin(), source.getDateActeMax(), false);
		fillCriteriaDates(dest, "dateTraitement", source.getDateTraitementMin(), source.getDateTraitementMax(), true);
		fillCriteriaDates(dest, "logCreationDate", source.getDateReceptionMin(), source.getDateReceptionMax(), true);
	}

	private static Order buildOrder(boolean ascending, String field) {
		return ascending ? Order.asc(field) : Order.desc(field);
	}

	private static void sortCriteria(Criteria dest, ParamSorting paramSorting) {
		if (paramSorting != null) {
			dest.addOrder(buildOrder(paramSorting.isAscending(), paramSorting.getField()));
		}
		if (paramSorting == null || !"id".equals(paramSorting.getField())) {
			dest.addOrder(buildOrder(paramSorting != null && paramSorting.isAscending(), "id"));     // pour assurer l'unicité du tri
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<UniteTraitement> find(UniteTraitementCriteria utCriteria, ParamPagination pagination) {
		final Criteria criteria = getCurrentSession().createCriteria(UniteTraitement.class);
		fillCriteria(criteria, utCriteria);
		sortCriteria(criteria, pagination != null ? pagination.getSorting() : null);

		/**
		 * [SIFISC-13200] La pagination est gérée à la main car dans le cas où le nombre de lignes
		 * retournées par la requête avant projection par "root entity" est plus grand que le nombre
		 * de lignes maximal par page, certains éléments ne sont pas visibles...
		 */
		final List<UniteTraitement> sansPagination = criteria.list();
		final List<UniteTraitement> avecPagination;
		if (pagination != null) {
			final int skip = pagination.getSqlFirstResult();
			final int size = pagination.getSqlMaxResults();
			if (skip > 0 || size < sansPagination.size()) {
				avecPagination = new ArrayList<>(sansPagination.subList(skip, Math.min(skip + size, sansPagination.size())));
			}
			else {
				avecPagination = sansPagination;
			}
		}
		else {
			avecPagination = sansPagination;
		}
		return avecPagination;
	}

	@Override
	public int getCount(UniteTraitementCriteria utCriteria) {
		final Criteria criteria = getCurrentSession().createCriteria(UniteTraitement.class);
		fillCriteria(criteria, utCriteria);
		criteria.setProjection(Projections.countDistinct("id"));
		return ((Number) criteria.uniqueResult()).intValue();
	}
}

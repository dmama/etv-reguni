package ch.vd.unireg.reqdes;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.common.pagination.ParamSorting;

public class UniteTraitementDAOImpl extends BaseDAOImpl<UniteTraitement, Long> implements UniteTraitementDAO {

	public UniteTraitementDAOImpl() {
		super(UniteTraitement.class);
	}

	private static Date toDate(@Nullable RegDate date) {
		return date != null ? date.asJavaDate() : null;
	}

	private static void fillCriteriaDates(List<Predicate> predicates, Path<Date> field, @Nullable RegDate min, @Nullable RegDate max, CriteriaBuilder builder) {
		final Date minDate = toDate(min);
		final Date maxDate = toDate(max);
		if (maxDate != null || minDate != null) {
			if (maxDate == null) {
				predicates.add(builder.greaterThanOrEqualTo(field, minDate));
			}
			else if (minDate == null) {
				predicates.add(builder.lessThanOrEqualTo(field, maxDate));
			}
			else {
				predicates.add(builder.between(field, minDate, maxDate));
			}
		}
	}

	private static void fillCriteriaRegDates(List<Predicate> predicates, Path<RegDate> field, @Nullable RegDate min, @Nullable RegDate max, CriteriaBuilder builder) {
		if (max != null || min != null) {
			if (max == null) {
				predicates.add(builder.greaterThanOrEqualTo(field, min));
			}
			else if (min == null) {
				predicates.add(builder.lessThanOrEqualTo(field, max));
			}
			else {
				predicates.add(builder.between(field, min, max));
			}
		}
	}

	private static void fillCriteria(CriteriaQuery<?> query, Root<UniteTraitement> root, UniteTraitementCriteria criteria, CriteriaBuilder builder) {

		query.distinct(true);

		final List<Predicate> predicates = new ArrayList<>(10);
		if (criteria.getEtatTraitement() != null) {
			predicates.add(builder.equal(root.get("etat"), criteria.getEtatTraitement()));
		}
		if (criteria.getNumeroMinute() != null) {
			predicates.add(builder.equal(root.get("evenement").get("numeroMinute"), criteria.getNumeroMinute()));
		}
		if (criteria.getVisaNotaire() != null) {
			predicates.add(builder.equal(root.get("evenement").get("notaire").get("visa"), criteria.getVisaNotaire()));
		}

		fillCriteriaRegDates(predicates, root.get("evenement").get("dateActe"), criteria.getDateActeMin(), criteria.getDateActeMax(), builder);
		fillCriteriaDates(predicates, root.get("dateTraitement"), criteria.getDateTraitementMin(), criteria.getDateTraitementMax(), builder);
		fillCriteriaDates(predicates, root.get("logCreationDate"), criteria.getDateReceptionMin(), criteria.getDateReceptionMax(), builder);

		query.where(predicates.toArray(new Predicate[0]));
	}

	private static Order buildOrder(boolean ascending, Path<Object> field, CriteriaBuilder builder) {
		final Order order;
		if (ascending) {
			order = builder.asc(field);
		}
		else {
			order = builder.desc(field);
		}
		return order;
	}

	private static void sortCriteria(CriteriaQuery<UniteTraitement> query, Root<UniteTraitement> root, ParamSorting paramSorting, CriteriaBuilder builder) {

		final List<Order> orders = new ArrayList<>(3);

		if (paramSorting != null) {
			orders.add(buildOrder(paramSorting.isAscending(), root.get(paramSorting.getField()), builder));
		}
		if (paramSorting == null || !"id".equals(paramSorting.getField())) {
			// pour assurer l'unicité du tri
			orders.add(buildOrder(paramSorting != null && paramSorting.isAscending(), root.get("id"), builder));
		}

		query.orderBy(orders);
	}

	@Override
	public List<UniteTraitement> find(UniteTraitementCriteria utCriteria, ParamPagination pagination) {
		final CriteriaBuilder builder = getCurrentSession().getCriteriaBuilder();
		final CriteriaQuery<UniteTraitement> query = builder.createQuery(UniteTraitement.class);
		final Root<UniteTraitement> root = query.from(UniteTraitement.class);
		fillCriteria(query, root, utCriteria, builder);
		sortCriteria(query, root, pagination != null ? pagination.getSorting() : null, builder);

		/*
		 * [SIFISC-13200] La pagination est gérée à la main car dans le cas où le nombre de lignes
		 * retournées par la requête avant projection par "root entity" est plus grand que le nombre
		 * de lignes maximal par page, certains éléments ne sont pas visibles...
		 */
		final List<UniteTraitement> sansPagination = getCurrentSession().createQuery(query).list();
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
		final CriteriaBuilder builder = getCurrentSession().getCriteriaBuilder();
		final CriteriaQuery<Number> query = builder.createQuery(Number.class);
		final Root<UniteTraitement> root = query.from(UniteTraitement.class);
		query.select(builder.count(root));
		fillCriteria(query, root, utCriteria, builder);
		return getCurrentSession().createQuery(query).getSingleResult().intValue();
	}
}

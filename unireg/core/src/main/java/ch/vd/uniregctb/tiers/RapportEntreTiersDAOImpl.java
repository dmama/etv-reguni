package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.common.ReflexionUtils;
import ch.vd.uniregctb.dbutils.QueryFragment;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class RapportEntreTiersDAOImpl extends BaseDAOImpl<RapportEntreTiers, Long> implements RapportEntreTiersDAO {

	public RapportEntreTiersDAOImpl() {
		super(RapportEntreTiers.class);
	}

	@Override
	public List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille) {
		return getRepresentationLegaleAvecTuteurEtPupille(noTiersTuteur, noTiersPupille, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille, boolean doNotAutoFlush) {

		String query = "from RapportEntreTiers ret where ret.objetId = :tuteur and ret.sujetId = :pupille";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		return find(query,
		            buildNamedParameters(Pair.of("tuteur", noTiersTuteur),
		                                 Pair.of("pupille", noTiersPupille)),
		            mode);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<RapportPrestationImposable> getRapportsPrestationImposable(final Long numeroDebiteur, ParamPagination paramPagination, boolean activesOnly) {

		final QueryFragment fragment = new QueryFragment("SELECT rapport FROM RapportPrestationImposable rapport WHERE rapport.objetId = :debiteurId", "debiteurId", numeroDebiteur);
		if (activesOnly) {
			fragment.add("and rapport.dateFin is null and rapport.annulationDate is null");
		}
		fragment.add(paramPagination.buildOrderClause("rapport", "logCreationDate", true, null));

		final int firstResult = paramPagination.getSqlFirstResult();
		final int maxResult = paramPagination.getSqlMaxResults();

		final Session session = getCurrentSession();
		final Query queryObject = fragment.createQuery(session);

		queryObject.setFirstResult(firstResult);
		queryObject.setMaxResults(maxResult);

		return queryObject.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<RapportPrestationImposable> getRapportsPrestationImposable(final Long numeroDebiteur, final Long numeroSourcier, boolean activesOnly, boolean doNotAutoFlush) {
		final StringBuilder b = new StringBuilder();
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		b.append("SELECT rapport FROM RapportPrestationImposable rapport WHERE rapport.objetId = :debiteur and rapport.sujetId = :sourcier");
		if (activesOnly) {
			b.append(" and rapport.dateFin is null and rapport.annulationDate is null");
		}
		final String query = b.toString();

		final Session session = getCurrentSession();
		final Query queryObject = session.createQuery(query);
		queryObject.setParameter("debiteur", numeroDebiteur);
		queryObject.setParameter("sourcier", numeroSourcier);
		queryObject.setFlushMode(mode);
		return queryObject.list();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean activesOnly) {

		String query = "select count(*) from RapportPrestationImposable rapport where rapport.objetId = " + numeroDebiteur;
		if (activesOnly) {
			query += " and rapport.dateFin is null and rapport.annulationDate is null";
		}
		return DataAccessUtils.intResult(find(query, null));
	}

	private static String buildWhereClassFragment(Set<TypeRapportEntreTiers> types, String alias) {
		final StringBuilder b = new StringBuilder(String.format("and %s.class in (", alias));
		boolean first = true;
		for (TypeRapportEntreTiers type : types) {
			if (!first) {
				b.append(", ");
			}
			b.append(type.getRapportClass().getSimpleName());
			first = false;
		}
		b.append(")");
		return b.toString();
	}

	@Override
	public List<RapportEntreTiers> findBySujetAndObjet(final long tiersId, final boolean showHisto, Set<RapportEntreTiersKey> types, final ParamPagination pagination, boolean fullList) {

		// aucun type demandé -> aucun rapport trouvé!
		if (types == null || types.isEmpty()) {
			return Collections.emptyList();
		}

		// d'abord les sujets ...
		final Set<TypeRapportEntreTiers> sujets = EnumSet.noneOf(TypeRapportEntreTiers.class);
		for (RapportEntreTiersKey key : types) {
			if (key.getSource() == RapportEntreTiersKey.Source.SUJET) {
				sujets.add(key.getType());
			}
		}
		final List<RapportEntreTiers> rapportsSujets;
		if (!sujets.isEmpty()) {
			final QueryFragment fragment = new QueryFragment("from RapportEntreTiers r where r.sujetId = " + tiersId);
			fragment.add(buildWhereClassFragment(sujets, "r"));
			if (!showHisto) {
				fragment.add("and r.dateFin is null and r.annulationDate is null");
			}
			final Query query = fragment.createQuery(getCurrentSession());
			//noinspection unchecked
			rapportsSujets = query.list();
		}
		else {
			rapportsSujets = Collections.emptyList();
		}

		// ... puis les objets
		final Set<TypeRapportEntreTiers> objets = EnumSet.noneOf(TypeRapportEntreTiers.class);
		for (RapportEntreTiersKey key : types) {
			if (key.getSource() == RapportEntreTiersKey.Source.OBJET) {
				objets.add(key.getType());
			}
		}
		final List<RapportEntreTiers> rapportsObjets;
		if (!objets.isEmpty()) {
			final QueryFragment fragment = new QueryFragment("from RapportEntreTiers r where r.objetId = " + tiersId);
			fragment.add(buildWhereClassFragment(objets, "r"));
			if (!showHisto) {
				fragment.add("and r.dateFin is null and r.annulationDate is null");
			}
			final Query query = fragment.createQuery(getCurrentSession());
			//noinspection unchecked
			rapportsObjets = query.list();
		}
		else {
			rapportsObjets = Collections.emptyList();
		}

		final List<RapportEntreTiers> tous = new ArrayList<>(rapportsSujets.size() + rapportsObjets.size());
		tous.addAll(rapportsSujets);
		tous.addAll(rapportsObjets);

		final Comparator<RapportEntreTiers> comparateurAsc;
		final String sortingField = pagination.getSorting().getField();
		if (sortingField != null && "tiersId".equals(sortingField)) {
			comparateurAsc = new Comparator<RapportEntreTiers>() {
				@Override
				public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
					// [SIFISC-9965] on va trier par le résultat de l'expression "sujetId + objetId - $tiersId", i.e. par l'AUTRE numéro de tiers
					final long key1 = o1.getObjetId() + o1.getSujetId() - tiersId;
					final long key2 = o2.getObjetId() + o2.getSujetId() - tiersId;
					int comparison = Long.compare(key1, key2);
					if (comparison == 0) {
						comparison = Long.compare(o1.getId(), o2.getId());
					}
					return comparison;
				}
			};
		}
		else {
			try {
				final Map<String, PropertyDescriptor> descriptors = ReflexionUtils.getPropertyDescriptors(RapportEntreTiers.class);
				final PropertyDescriptor descriptor = descriptors.get(sortingField == null ? "id" : sortingField);
				comparateurAsc = new Comparator<RapportEntreTiers>() {
					@Override
					public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
						try {
							final Method readMethod = descriptor.getReadMethod();
							final Object value1 = readMethod.invoke(o1);
							final Object value2 = readMethod.invoke(o2);
							if (value1 == null || value2 == null) {
								if (value1 == null && value2 == null) {
									return 0;
								}
								return value1 == null ? -1 : 1;
							}
							else if (value1 instanceof Comparable && value2 instanceof Comparable) {
								//noinspection unchecked
								return ((Comparable) value1).compareTo(value2);
							}
							else if (Class.class.equals(descriptor.getPropertyType())) {
								// [SIFISC-25994] on veut comparer par classe (= type de rapport)...
								//noinspection ConstantConditions
								return ((Class<?>) value1).getSimpleName().compareTo(((Class<?>) value2).getSimpleName());
							}
							else {
								throw new IllegalArgumentException("Propriété " + descriptor.getDisplayName() + " de type " + descriptor.getPropertyType().getName() + " non comparable...");
							}
						}
						catch (IllegalAccessException | InvocationTargetException e) {
							throw new IllegalArgumentException("Impossible d'accéder à la propriété " + descriptor.getDisplayName() + " de la classe " + RapportEntreTiers.class.getSimpleName());
						}
					}
				};
			}
			catch (IntrospectionException e) {
				throw new IllegalArgumentException("Impossible d'accéder aux propriétés de la classe " + RapportEntreTiers.class.getSimpleName());
			}
		}
		tous.sort(comparateurAsc);
		if (!pagination.getSorting().isAscending()) {
			Collections.reverse(tous);
		}

		if (fullList) {
			return tous;
		}
		else {
			return tous.subList(Math.min(pagination.getSqlFirstResult(), tous.size()),
			                    Math.min(pagination.getSqlFirstResult() + pagination.getSqlMaxResults(), tous.size()));
		}
	}

	@Override
	public int countBySujetAndObjet(long tiersId, boolean showHisto, Set<RapportEntreTiersKey> types) {
		// d'abord les sujets
		final Set<TypeRapportEntreTiers> sujets = EnumSet.noneOf(TypeRapportEntreTiers.class);
		for (RapportEntreTiersKey key : types) {
			if (key.getSource() == RapportEntreTiersKey.Source.SUJET) {
				sujets.add(key.getType());
			}
		}
		final int nbSujets;
		if (!sujets.isEmpty()) {
			final QueryFragment fragment = new QueryFragment("select count(*) from RapportEntreTiers r where r.sujetId = " + tiersId);
			fragment.add(buildWhereClassFragment(sujets, "r"));
			if (!showHisto) {
				fragment.add("and r.dateFin is null and r.annulationDate is null");
			}
			nbSujets = DataAccessUtils.intResult(find(fragment.getQuery(), null));
		}
		else {
			nbSujets = 0;
		}

		// puis les objets
		final Set<TypeRapportEntreTiers> objets = EnumSet.noneOf(TypeRapportEntreTiers.class);
		for (RapportEntreTiersKey key : types) {
			if (key.getSource() == RapportEntreTiersKey.Source.OBJET) {
				objets.add(key.getType());
			}
		}
		final int nbObjets;
		if (!objets.isEmpty()) {
			final QueryFragment fragment = new QueryFragment("select count(*) from RapportEntreTiers r where r.objetId = " + tiersId);
			fragment.add(buildWhereClassFragment(objets, "r"));
			if (!showHisto) {
				fragment.add("and r.dateFin is null and r.annulationDate is null");
			}
			nbObjets = DataAccessUtils.intResult(find(fragment.getQuery(), null));
		}
		else {
			nbObjets = 0;
		}
		return nbSujets + nbObjets;
	}

	@Override
	public int removeAllOfKind(TypeRapportEntreTiers kind) {
		final Class<?> rapportClass = kind.getRapportClass();
		final DiscriminatorValue discriminator = rapportClass.getAnnotation(DiscriminatorValue.class);
		final String discriminatorValue = discriminator.value();

		final String sql = "DELETE FROM RAPPORT_ENTRE_TIERS WHERE RAPPORT_ENTRE_TIERS_TYPE=:discriminator";
		final Session session = getCurrentSession();
		final Query query = session.createSQLQuery(sql);
		query.setParameter("discriminator", discriminatorValue);
		return query.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Pair<Long, Long>> getDoublonsCandidats(TypeRapportEntreTiers kind) {
		final Class<?> rapportClass = kind.getRapportClass();
		final String hql = String.format("SELECT r.sujetId, r.objetId, COUNT(*) FROM %s AS r WHERE r.annulationDate IS NULL GROUP BY r.sujetId, r.objetId HAVING COUNT(*) > 1", rapportClass.getSimpleName());
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);

		final List<Pair<Long, Long>> liste = new LinkedList<>();
		final Iterator<Object[]> iterator = query.iterate();
		while (iterator.hasNext()) {
			final Object[] row = iterator.next();
			final Long sujetId = ((Number) row[0]).longValue();
			final Long objetId = ((Number) row[1]).longValue();
			liste.add(Pair.of(sujetId, objetId));
		}
		return liste;
	}
}
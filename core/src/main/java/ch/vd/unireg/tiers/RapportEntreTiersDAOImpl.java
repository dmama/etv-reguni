package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.ReflexionUtils;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.dbutils.QueryFragment;
import ch.vd.unireg.type.TypeRapportEntreTiers;

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
	public List<RapportEntreTiers> findBySujetAndObjet(final long tiersId, final boolean showHisto, Set<RapportEntreTiersKey> types, final ParamPagination pagination) {

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

		comparateurAsc = getRapportEntreTiersComparator(tiersId, sortingField);

		tous.sort(comparateurAsc);

		if (!pagination.getSorting().isAscending()) {
			Collections.reverse(tous);
		}

		return tous;
	}

	@NotNull
	static Comparator<RapportEntreTiers> getRapportEntreTiersComparator(long tiersId, String sortingField) {
		final Comparator<RapportEntreTiers> comparateurAsc;
		if ("tiersId".equals(sortingField)) {
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
		} else if("autoriteTutelaire".equals(sortingField)) {
			// [SIFISC-26747] pour le tri sur la colonne "autoriteTutelaire" le tri est fait au niveau service
			comparateurAsc = new Comparator<RapportEntreTiers>() {
				@Override
				public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
					return 1;
				}
			};
		}
		else {
			comparateurAsc = new Comparator<RapportEntreTiers>() {
				@Override
				public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
					try {
						final Map<String, PropertyDescriptor> descriptorsO1 = ReflexionUtils.getPropertyDescriptors(o1.getClass());
						final Map<String, PropertyDescriptor> descriptorsO2 = ReflexionUtils.getPropertyDescriptors(o2.getClass());
						final PropertyDescriptor descriptorO1 = descriptorsO1.get(sortingField == null ? "id" : sortingField);
						final PropertyDescriptor descriptorO2 = descriptorsO2.get(sortingField == null ? "id" : sortingField);
						if (descriptorO1 == null) {
							return 1;
						}
						else if (descriptorO2 == null) {
							return -1;
						}
						else {
							final Object value1 = descriptorO1.getReadMethod().invoke(o1);
							final Object value2 = descriptorO2.getReadMethod().invoke(o2);
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
							else if (Class.class.equals(descriptorO1.getPropertyType())) {
								// [SIFISC-25994] on veut comparer par classe (= type de rapport)...
								//noinspection ConstantConditions
								return ((Class<?>) value1).getSimpleName().compareTo(((Class<?>) value2).getSimpleName());
							}
							else {
								throw new IllegalArgumentException("Propriété " + descriptorO1.getDisplayName() + " de type " + descriptorO1.getPropertyType().getName() + " non comparable...");
							}
						}
					}
					catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
						throw new IllegalArgumentException("Impossible d'accéder à la propriété " + sortingField + " de la classe " + RapportEntreTiers.class.getSimpleName());
					}
				}
			};
		}
		return comparateurAsc;
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
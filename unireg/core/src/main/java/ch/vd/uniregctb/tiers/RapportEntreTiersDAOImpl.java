package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class RapportEntreTiersDAOImpl extends GenericDAOImpl<RapportEntreTiers, Long> implements RapportEntreTiersDAO {

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

		Object[] criteria = {noTiersTuteur, noTiersPupille};
		String query = "from RapportEntreTiers ret where ret.objetId = ? and ret.sujetId = ?";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);

		return (List<RapportEntreTiers>) find(query, criteria, mode);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<RapportPrestationImposable> getRapportsPrestationImposable(final Long numeroDebiteur, ParamPagination paramPagination, boolean activesOnly) {

		final QueryFragment fragment = new QueryFragment("SELECT rapport FROM RapportPrestationImposable rapport WHERE rapport.objetId = ?", Arrays.<Object>asList(numeroDebiteur));
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
		return DataAccessUtils.intResult(find(query, null, null));
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
	public List<RapportEntreTiers> findBySujetAndObjet(final long tiersId, final boolean showHisto, Set<TypeRapportEntreTiers> types, final ParamPagination pagination) {

		// aucun type demandé -> aucun rapport trouvé!
		if (types == null || types.isEmpty()) {
			return Collections.emptyList();
		}

		final QueryFragment fragment = new QueryFragment("from RapportEntreTiers r where ((r.sujetId = " + tiersId + ") or (r.objetId = " + tiersId + "))");
		fragment.add(buildWhereClassFragment(types, "r"));
		if (!showHisto) {
			fragment.add("and r.dateFin is null and r.annulationDate is null");
		}
		fragment.add(buildOrderClause(pagination));

		final Session session = getCurrentSession();
		final Query queryObject = fragment.createQuery(session);
		final int firstResult = pagination.getSqlFirstResult();
		final int maxResult = pagination.getSqlMaxResults();
		queryObject.setFirstResult(firstResult);
		queryObject.setMaxResults(maxResult);

		//noinspection unchecked
		return queryObject.list();
	}

	private static QueryFragment buildOrderClause(ParamPagination pagination) {
		return pagination.buildOrderClause("r", null, true, new ParamPagination.CustomOrderByGenerator() {
			@Override
			public boolean supports(String fieldName) {
				return "tiersId".equals(fieldName);
			}

			@Override
			public QueryFragment generate(String fieldName, ParamPagination pagination) {
				// pour le champs tiersId, on s'arrange pour trier selon l'ordre sujet-objet
				if (pagination.isSensAscending()) {
					return new QueryFragment("r.sujetId asc, r.objetId");
				}
				else {
					return new QueryFragment("r.sujetId desc, r.objetId");
				}
			}
		});
	}

	@Override
	public int countBySujetAndObjet(long tiersId, boolean showHisto, Set<TypeRapportEntreTiers> types) {

		final QueryFragment fragment = new QueryFragment("select count(*) from RapportEntreTiers r where ((r.sujetId = " + tiersId + ") or (r.objetId = " + tiersId + "))");
		fragment.add(buildWhereClassFragment(types, "r"));
		if (!showHisto) {
			fragment.add("and r.dateFin is null and r.annulationDate is null");
		}
		return DataAccessUtils.intResult(find(fragment.getQuery(), null, null));
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
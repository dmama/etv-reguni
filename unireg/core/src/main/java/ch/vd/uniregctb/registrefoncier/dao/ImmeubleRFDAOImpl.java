package ch.vd.uniregctb.registrefoncier.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public class ImmeubleRFDAOImpl extends BaseDAOImpl<ImmeubleRF, Long> implements ImmeubleRFDAO {
	protected ImmeubleRFDAOImpl() {
		super(ImmeubleRF.class);
	}

	@Nullable
	@Override
	public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushMode flushModeOverride) {
		return findUnique("from ImmeubleRF where idRF = :idRF", buildNamedParameters(Pair.of("idRF", key.getIdRF())), flushModeOverride);
	}

	@Override
	public @Nullable ImmeubleRF findByEgrid(@NotNull String egrid) {
		final Query query = getCurrentSession().createQuery("from ImmeubleRF where egrid = :egrid");
		query.setParameter("egrid", egrid);
		//noinspection unchecked
		return (ImmeubleRF) query.uniqueResult();
	}

	@NotNull
	@Override
	public Set<String> findWithActiveSurfacesAuSol() {
		final Query query = getCurrentSession().createQuery("select i.idRF from ImmeubleRF i left join i.surfacesAuSol s where s.dateFin is null and i.surfacesAuSol is not empty");
		//noinspection unchecked
		return new HashSet<>(query.list());
	}

	@NotNull
	@Override
	public Set<String> findImmeublesActifs() {
		final Query query = getCurrentSession().createQuery("select idRF from ImmeubleRF where dateRadiation is null");
		//noinspection unchecked
		return new HashSet<>(query.list());
	}

	@Nullable
	@Override
	public ImmeubleRF findImmeubleActif(int noOfsCommune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3, @Nullable FlushMode flushMode) throws NonUniqueResultException {

		String queryString = "select s.immeuble from SituationRF s, CommuneRF c " +
				"where c.noOfs = :noOfsCommune " +
				"and s.commune.id = c.id " +
				"and s.noParcelle = :noParcelle ";
		if (index1 == null) {
			queryString += "and s.index1 is null ";
		}
		else {
			queryString += "and s.index1 = :index1 ";
		}
		if (index2 == null) {
			queryString += "and s.index2 is null ";
		}
		else {
			queryString += "and s.index2 = :index2 ";
		}
		if (index3 == null) {
			queryString += "and s.index3 is null ";
		}
		else {
			queryString += "and s.index3 = :index3 ";
		}

		final Session session = getCurrentSession();
		final Query query = session.createQuery(queryString);
		query.setParameter("noOfsCommune", noOfsCommune);
		query.setParameter("noParcelle", noParcelle);
		if (index1 != null) {
			query.setParameter("index1", index1);
		}
		if (index2 != null) {
			query.setParameter("index2", index2);
		}
		if (index3 != null) {
			query.setParameter("index3", index3);
		}

		final FlushMode oldMode = session.getFlushMode();
		if (flushMode != null) {
			session.setFlushMode(flushMode);
		}
		try {
			return (ImmeubleRF) query.uniqueResult();
		}
		finally {
			if (flushMode != null) {
				session.setFlushMode(oldMode);
			}
		}
	}

	@NotNull
	@Override
	public List<Long> findImmeubleIdsAvecDatesDeFinDroitsACalculer() {
		// [SIFISC-24558] On ignore les servitudes car leurs dates de fin ne peuvent pas être calculée
		// (= une date limite est fixées à la conclusion de l'acte notariée ou la servitude s'éteint lors du décès du bénéficiaire).
		final Query query = getCurrentSession().createQuery("select distinct immeuble.id from DroitProprieteRF where dateFinMetier is null and dateFin is not null order by immeuble.id");
		final List<?> list = query.list();
		return list.stream()
				.map(n -> ((Number) n).longValue())
				.collect(Collectors.toList());
	}

	@Override
	public @NotNull List<Long> getAllIds() {
		final Query query = getCurrentSession().createQuery("select id from ImmeubleRF where annulationDate is null order by id");
		final List<?> list = query.list();
		return list.stream()
				.map(n -> ((Number) n).longValue())
				.collect(Collectors.toList());
	}
}

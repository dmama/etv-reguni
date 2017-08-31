package ch.vd.uniregctb.registrefoncier.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.TypeDroit;
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
				.map(Number.class::cast)
				.map(Number::longValue)
				.collect(Collectors.toList());
	}

	@Override
	public @NotNull Set<String> findAvecDroitsActifs(TypeDroit typeDroit) {
		final Query query;
		if (typeDroit == TypeDroit.DROIT_PROPRIETE) {
			final String queryString = "select i.idRF from ImmeubleRF i left join i.droitsPropriete d where d.dateFin is null and i.droitsPropriete is not empty";
			query = getCurrentSession().createQuery(queryString);
		}
		else if (typeDroit == TypeDroit.SERVITUDE) {
			final String queryString = "select i.idRF from ImmeubleRF i left join i.servitudes d where (d.dateFin is null or :today < d.dateFin) and i.servitudes is not empty";
			query = getCurrentSession().createQuery(queryString);
			query.setParameter("today", RegDate.get());
		}
		else {
			throw new IllegalArgumentException("Type de droit inconnu = [" + typeDroit + "]");
		}

		//noinspection unchecked
		return new HashSet<>(query.list());

	}
}

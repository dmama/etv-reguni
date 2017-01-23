package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public class AyantDroitRFDAOImpl extends BaseDAOImpl<AyantDroitRF, Long> implements AyantDroitRFDAO {
	protected AyantDroitRFDAOImpl() {
		super(AyantDroitRF.class);
	}

	@Nullable
	@Override
	public AyantDroitRF find(@NotNull AyantDroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from AyantDroitRF where idRF = :idRF");
		query.setParameter("idRF", key.getIdRF());
		return (AyantDroitRF) query.uniqueResult();
	}

	@Override
	public Set<String> findAvecDroitsActifs() {
		final Query query = getCurrentSession().createQuery("select a.idRF from AyantDroitRF a left join a.droits d where d.dateFin is null and a.droits is not empty");
		//noinspection unchecked
		return new HashSet<>(query.list());
	}

	@Nullable
	@Override
	public CommunauteRFMembreInfo getCommunauteMembreInfo(long communauteId) {

		final Set<Number> ids = new HashSet<>();

		// on récupère les ids des tiers RF PMs
		final Query query1 = getCurrentSession().createQuery("select d.ayantDroit.id from DroitProprietePersonneRF d where d.communaute.id = :communauteId");
		query1.setParameter("communauteId", communauteId);
		//noinspection unchecked
		ids.addAll((List<Number>) query1.list());

		if (ids.isEmpty()) {
			return null;
		}

		// conversion ids tiers RF -> ids de tiers Unireg
		final Query query2 = getCurrentSession().createQuery("select r.tiersRF.id, r.contribuable.id  from RapprochementRF r where r.tiersRF.id in (:ids)");
		query2.setParameterList("ids", ids);

		final Map<Long, Long> rf2ctb = new HashMap<>(ids.size());
		//noinspection unchecked
		query2.list().forEach(o -> {
			final Object row[] = (Object[]) o;
			rf2ctb.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
		});
		final Collection<Long> ctbIds = rf2ctb.values();

		// on détermine les ids des tiers RF non-rapprochés avec des tiers Unireg
		final Set<Long> idsRf = ids.stream()
				.map(Number::longValue)
				.collect(Collectors.toSet());
		idsRf.removeAll(rf2ctb.keySet());

		// on charge les tiers RF non-rapprochés correspondants
		final Collection<TiersRF> tiersRF;
		if (idsRf.isEmpty()) {
			tiersRF = Collections.emptyList();
		}
		else {
			final Query query3 = getCurrentSession().createQuery("from TiersRF where id in (:ids)");
			query3.setParameterList("ids", idsRf);
			//noinspection unchecked
			tiersRF = (Collection<TiersRF>) query3.list();
		}

		return new CommunauteRFMembreInfo(ids.size(), ctbIds, tiersRF);
	}
}

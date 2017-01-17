package ch.vd.uniregctb.registrefoncier.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFInfo;
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
	public CommunauteRFInfo getCommunauteInfo(long communauteId) {

		final Set<Number> ids = new HashSet<>();

		// on récupère les ids des tiers RF PMs
		final Query query1 = getCurrentSession().createQuery("select d.ayantDroit.id from DroitProprietePersonneMoraleRF d where d.communaute.id = :communauteId");
		query1.setParameter("communauteId", communauteId);
		//noinspection unchecked
		ids.addAll((List<Number>) query1.list());

		// on récupère les ids des tiers RF PPs
		final Query query2 = getCurrentSession().createQuery("select d.ayantDroit.id from DroitProprietePersonnePhysiqueRF d where d.communaute.id = :communauteId");
		query2.setParameter("communauteId", communauteId);
		//noinspection unchecked
		ids.addAll((List<Number>) query2.list());

		if (ids.isEmpty()) {
			return null;
		}

		// conversion ids tiers RF -> ids de tiers Unireg
		final Query query3 = getCurrentSession().createQuery("select r.contribuable.id from RapprochementRF r where r.tiersRF.id in (:ids)");
		query3.setParameterList("ids", ids);

		//noinspection unchecked
		final Set<Integer> ctbIds = ((List<Number>) query3.list()).stream()
				.map(Number::intValue)
				.collect(Collectors.toSet());

		return new CommunauteRFInfo(ids.size(), ctbIds);
	}
}

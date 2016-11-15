package ch.vd.uniregctb.registrefoncier.dao;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
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
}

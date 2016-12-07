package ch.vd.uniregctb.registrefoncier.dao;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Query;
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
	public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
		final Query query = getCurrentSession().createQuery("from ImmeubleRF where idRF = :idRF");
		query.setParameter("idRF", key.getIdRF());
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
}

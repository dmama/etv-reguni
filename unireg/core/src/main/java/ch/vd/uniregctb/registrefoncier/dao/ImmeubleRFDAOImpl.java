package ch.vd.uniregctb.registrefoncier.dao;

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
}

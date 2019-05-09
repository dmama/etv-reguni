package ch.vd.unireg.registrefoncier.dao;

import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.key.DroitRFKey;

public class ServitudeRFDAOImpl extends BaseDAOImpl<ServitudeRF, Long> implements ServitudeRFDAO {
	protected ServitudeRFDAOImpl() {
		super(ServitudeRF.class);
	}

	@Override
	public @Nullable ServitudeRF find(@NotNull DroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from ServitudeRF where masterIdRF = :masterIdRF and versionIdRF = :versionIdRF");
		query.setParameter("masterIdRF", key.getMasterIdRF());
		query.setParameter("versionIdRF", key.getVersionIdRF());
		return (ServitudeRF) query.uniqueResult();
	}
}

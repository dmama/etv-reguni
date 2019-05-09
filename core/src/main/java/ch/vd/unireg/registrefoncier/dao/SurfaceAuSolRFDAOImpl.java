package ch.vd.unireg.registrefoncier.dao;

import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.SurfaceAuSolRF;
import ch.vd.unireg.registrefoncier.key.SurfaceAuSolRFKey;

public class SurfaceAuSolRFDAOImpl extends BaseDAOImpl<SurfaceAuSolRF, Long> implements SurfaceAuSolRFDAO {
	protected SurfaceAuSolRFDAOImpl() {
		super(SurfaceAuSolRF.class);
	}

	@Nullable
	@Override
	public SurfaceAuSolRF findActive(@NotNull SurfaceAuSolRFKey key) {
		final Query query = getCurrentSession().createQuery("from SurfaceAuSolRF where immeuble.idRF = :idRF and type = :type and surface = :surface and dateFin is null");
		query.setParameter("idRF", key.getIdRF());
		query.setParameter("type", key.getType());
		query.setParameter("surface", key.getSurface());
		return (SurfaceAuSolRF) query.uniqueResult();
	}
}

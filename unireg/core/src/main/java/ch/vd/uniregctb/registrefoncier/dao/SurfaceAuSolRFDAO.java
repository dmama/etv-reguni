package ch.vd.uniregctb.registrefoncier.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.key.SurfaceAuSolRFKey;

public interface SurfaceAuSolRFDAO extends GenericDAO<SurfaceAuSolRF, Long> {

	@Nullable
	SurfaceAuSolRF find(@NotNull SurfaceAuSolRFKey key);
}

package ch.vd.unireg.registrefoncier.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.registrefoncier.SurfaceAuSolRF;
import ch.vd.unireg.registrefoncier.key.SurfaceAuSolRFKey;

public interface SurfaceAuSolRFDAO extends GenericDAO<SurfaceAuSolRF, Long> {

	/**
	 * @param key une clé d'identification d'une surface au sol
	 * @return la surface au sol <b>active</b> (c'est-à-dire avec une date de fin nulle) qui correspond à la clé spécifiée.
	 */
	@Nullable
	SurfaceAuSolRF findActive(@NotNull SurfaceAuSolRFKey key);
}

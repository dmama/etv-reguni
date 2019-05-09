package ch.vd.unireg.registrefoncier.dao;

import javax.persistence.FlushModeType;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.key.BatimentRFKey;

public interface BatimentRFDAO extends GenericDAO<BatimentRF, Long> {
	@Nullable
	BatimentRF find(@NotNull BatimentRFKey key, @Nullable FlushModeType flushModeOverride);

	/**
	 * @return les masterIdsRF des bâtiments actifs, c'est-à-dire les bâtiments avec au moins une implantation active.
	 */
	@NotNull
	Set<String> findActifs();
}

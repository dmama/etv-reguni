package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Set;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.key.BatimentRFKey;

public interface BatimentRFDAO extends GenericDAO<BatimentRF, Long> {
	@Nullable
	BatimentRF find(@NotNull BatimentRFKey key, @Nullable FlushMode flushModeOverride);

	/**
	 * @return les masterIdsRF des bâtiments actifs, c'est-à-dire les bâtiments avec au moins une implantation active.
	 */
	@NotNull
	Set<String> findActifs();
}

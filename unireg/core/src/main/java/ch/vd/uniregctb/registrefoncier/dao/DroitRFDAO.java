package ch.vd.uniregctb.registrefoncier.dao;


import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;

public interface DroitRFDAO extends GenericDAO<DroitRF, Long> {

	@Nullable
	DroitRF find(@NotNull DroitRFKey key);

	@Nullable
	DroitRF findActive(@NotNull DroitRFKey key);

	@NotNull
	List<DroitRF> findForAyantDroit(long tiersRFId, boolean fetchSituationsImmeuble);

	/**
	 * @return les ids RF des servitudes actives
	 */
	@NotNull
	Set<String> findIdsServitudesActives();
}

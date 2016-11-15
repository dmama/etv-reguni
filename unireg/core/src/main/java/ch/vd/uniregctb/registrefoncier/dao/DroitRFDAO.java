package ch.vd.uniregctb.registrefoncier.dao;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;

public interface DroitRFDAO extends GenericDAO<DroitRF, Long> {

	@Nullable
	DroitRF findActive(@NotNull DroitRFKey key);
}

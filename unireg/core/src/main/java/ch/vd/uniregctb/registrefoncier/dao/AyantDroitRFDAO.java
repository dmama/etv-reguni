package ch.vd.uniregctb.registrefoncier.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public interface AyantDroitRFDAO extends GenericDAO<AyantDroitRF, Long> {

	@Nullable
	AyantDroitRF find(@NotNull AyantDroitRFKey key);
}

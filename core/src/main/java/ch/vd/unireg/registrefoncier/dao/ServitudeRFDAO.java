package ch.vd.unireg.registrefoncier.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.key.DroitRFKey;

public interface ServitudeRFDAO extends GenericDAO<ServitudeRF, Long> {

	@Nullable
	ServitudeRF find(@NotNull DroitRFKey key);

}

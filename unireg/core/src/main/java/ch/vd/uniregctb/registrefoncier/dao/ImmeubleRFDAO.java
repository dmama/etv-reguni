package ch.vd.uniregctb.registrefoncier.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public interface ImmeubleRFDAO extends GenericDAO<ImmeubleRF, Long> {

	@Nullable
	ImmeubleRF find(@NotNull ImmeubleRFKey key);
}

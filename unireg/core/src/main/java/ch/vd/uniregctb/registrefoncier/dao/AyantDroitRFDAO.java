package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public interface AyantDroitRFDAO extends GenericDAO<AyantDroitRF, Long> {

	@Nullable
	AyantDroitRF find(@NotNull AyantDroitRFKey key);

	/**
	 * @return la liste des ids RF des ayants-droits ayant 1 ou plusieurs droits actifs sur des immeubles.
	 */
	Set<String> findAvecDroitsActifs();
}

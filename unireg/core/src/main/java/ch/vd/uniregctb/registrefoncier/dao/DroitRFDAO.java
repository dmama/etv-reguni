package ch.vd.uniregctb.registrefoncier.dao;


import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
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
	 * @return les clés RF des servitudes actives
	 */
	@NotNull
	Set<DroitRFKey> findIdsServitudesActives();

	/**
	 * @param key une clé d'identification d'un droit.
	 * @return le droit de propriété avec le même masterId et la version immédiatement précédente aux valeurs fournies dans la clé.
	 */
	@Nullable
	DroitProprieteRF findDroitPrecedent(@NotNull DroitRFKey key);
}

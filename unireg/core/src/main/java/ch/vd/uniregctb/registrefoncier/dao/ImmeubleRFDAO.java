package ch.vd.uniregctb.registrefoncier.dao;

import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public interface ImmeubleRFDAO extends GenericDAO<ImmeubleRF, Long> {

	@Nullable
	ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushMode flushModeOverride);

	@Nullable
	ImmeubleRF findByEgrid(@NotNull String egrid);

	/**
	 * @return les ids RF des immeubles qui possèdent des surfaces au sol actives.
	 */
	@NotNull
	Set<String> findWithActiveSurfacesAuSol();

	/**
	 * @return les ids RF des immeubles non-radiés.
	 */
	@NotNull
	Set<String> findImmeublesActifs();

	/**
	 * @return la liste des ids des immeubles avec au moins un droit dont la date de fin <i>métier</i> doit être calculée.
	 */
	@NotNull
	List<Long> findImmeubleIdsAvecDatesDeFinDroitsACalculer();

	/**
	 * @return les ids de tous les immeubles existants et non-annulés (mais y compris les radiés).
	 */
	@NotNull
	List<Long> getAllIds();
}

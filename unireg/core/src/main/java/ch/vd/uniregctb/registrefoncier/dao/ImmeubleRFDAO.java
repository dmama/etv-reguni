package ch.vd.uniregctb.registrefoncier.dao;

import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.NonUniqueResultException;
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
	 * Recherche et retourne l'immeuble actif à partir de critères d'identification spécifiques.
	 *
	 * @param noOfsCommune le numéro Ofs de la commune où est sis l'immeuble
	 * @param noParcelle   le numéro de parcelle
	 * @param index1       l'index 1 en cas de lot PPE
	 * @param index2       l'index 2 en cas de lot PPE
	 * @param index3       l'index 3 en cas de lot PPE
	 * @return un immeuble ou null si aucun immeuble n'est trouvé.
	 * @throws NonUniqueResultException si plusieurs immeubles actifs correspondent aux critères.
	 */
	@Nullable
	ImmeubleRF findImmeubleActif(int noOfsCommune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3, @Nullable FlushMode flushMode) throws NonUniqueResultException;

	/**
	 * @return la liste des ids des immeubles avec au moins un droit dont la date de fin <i>métier</i> doit être calculée.
	 */
	@NotNull
	List<Long> findImmeubleIdsAvecDatesDeFinDroitsACalculer();
}

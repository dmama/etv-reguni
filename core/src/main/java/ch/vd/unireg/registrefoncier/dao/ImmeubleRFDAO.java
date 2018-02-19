package ch.vd.unireg.registrefoncier.dao;

import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.TypeDroit;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;

public interface ImmeubleRFDAO extends GenericDAO<ImmeubleRF, Long> {

	@Nullable
	ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushMode flushModeOverride);

	@Nullable
	ImmeubleRF findByEgrid(@NotNull String egrid);

	/**
	 * Retourne un immeuble à partir de sa situation précise.
	 *
	 * @param noOfsCommune le numéro OFS de la commune de l'immeuble (obligatoire)
	 * @param noParcelle   le numéro de parcelle de l'immeuble (obligatoire)
	 * @param index1       l'index n°1 (optionnel, si pas renseigné retourne l'immeuble avec un index1 nul)
	 * @param index2       l'index n°2 (optionnel, si pas renseigné retourne l'immeuble avec un index2 nul)
	 * @param index3       l'index n°3 (optionnel, si pas renseigné retourne l'immeuble avec un index3 nul)
	 * @param user         l'utilisateur physique ayant fait la demande.
	 * @return l'immeuble correspondant ou null si aucun immeuble ne correspond.
	 */
	@Nullable
	ImmeubleRF getBySituation(int noOfsCommune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3);

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

	/**
	 * @param typeDroit le type de droit à considérer
	 * @return la liste des ids RF des immeubles vers lesquels pointent 1 ou plusieurs droits actifs.
	 */
	@NotNull
	Set<String> findAvecDroitsActifs(TypeDroit typeDroit);
}

package ch.vd.unireg.registrefoncier.dao;

import javax.persistence.FlushModeType;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.registrefoncier.TypeDroit;
import ch.vd.unireg.registrefoncier.key.AyantDroitRFKey;

public interface AyantDroitRFDAO extends GenericDAO<AyantDroitRF, Long> {

	@Nullable
	AyantDroitRF find(@NotNull AyantDroitRFKey key, @Nullable FlushModeType flushModeOverride);

	/**
	 * @param typeDroit le type de droit à considérer
	 * @return la liste des ids RF des ayants-droits ayant 1 ou plusieurs droits actifs sur des immeubles.
	 */
	Set<String> findAvecDroitsActifs(@NotNull TypeDroit typeDroit);

	/**
	 * @param tiersRF un tiers RF
	 * @return le numéro du contribuable rapproché avec le tiers RF spécifié.
	 */
	@Nullable
	Long getContribuableIdFor(@NotNull TiersRF tiersRF);

	/**
	 * @return les ids des communautés existantes.
	 */
	@NotNull
	List<Long> findCommunautesIds();
}

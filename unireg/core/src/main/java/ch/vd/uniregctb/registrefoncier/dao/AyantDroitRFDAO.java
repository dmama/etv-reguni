package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public interface AyantDroitRFDAO extends GenericDAO<AyantDroitRF, Long> {

	@Nullable
	AyantDroitRF find(@NotNull AyantDroitRFKey key);

	/**
	 * @return la liste des ids RF des ayants-droits ayant 1 ou plusieurs droits actifs sur des immeubles.
	 */
	Set<String> findAvecDroitsActifs();

	/**
	 * Construit et retourne les informations du point-de-vue Unireg sur les membres d'une communauté RF.
	 *
	 * @param communauteId l'id technique Unireg d'une communauté
	 * @return les infos trouvée; ou <b>null</b> si la communauté est inconnue.
	 */
	@Nullable
	CommunauteRFMembreInfo getCommunauteMembreInfo(long communauteId);

	/**
	 * @param tiersRF un tiers RF
	 * @return le numéro du contribuable rapproché avec le tiers RF spécifié.
	 */
	@Nullable
	Long getContribuableIdFor(@NotNull TiersRF tiersRF);
}

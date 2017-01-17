package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Service qui expose des méthodes d'accès aux données du registre foncier (Capistastra).
 */
public interface RegistreFoncierService {

	/**
	 * Détermine les droits sur des immeubles d'un contribuable Unireg.
	 *
	 * @param ctb un contribuable Unireg
	 * @return une liste de droits.
	 */
	@NotNull
	List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb);

	/**
	 * @param immeubleId l'id technique d'un immeuble
	 * @return l'immeuble qui correspond à l'id; ou <b>null</b> si aucun immeuble ne correspond.
	 */
	@Nullable
	ImmeubleRF getImmeuble(long immeubleId);

	/**
	 * @param batimentId l'id technique d'un bâtiment
	 * @return le bâtiment qui correspond à l'id; ou <b>null</b> si aucun bâtiment ne correspond.
	 */
	@Nullable
	BatimentRF getBatiment(long batimentId);

	/**
	 * Construit et retourne les informations du point-de-vue Unireg sur les membres d'une communauté RF.
	 *
	 * @param communauteId l'id technique Unireg d'une communauté
	 * @return les infos trouvée; ou <b>null</b> si la communauté est inconnue.
	 */
	@Nullable
	CommunauteRFInfo getCommunauteInfo(long communauteId);
}

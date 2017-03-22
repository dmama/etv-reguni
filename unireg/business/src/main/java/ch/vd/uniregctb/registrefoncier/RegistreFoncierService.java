package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.ObjectNotFoundException;
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
	 * Détermine les droits sur des immeubles d'un contribuable Unireg.
	 *
	 * @param ctb un contribuable Unireg
	 * @return une liste de droits.
	 */
	@NotNull
	List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean prefetchSituationsImmeuble);

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
	 * @param communauteId l'id technique Unireg d'une communauté
	 * @return la communauté trouvée; ou <b>null</b> si la communauté est inconnue.
	 */
	@Nullable
	CommunauteRF getCommunaute(long communauteId);

	/**
	 * Construit et retourne les informations du point-de-vue Unireg sur les membres d'une communauté RF.
	 *
	 * @param communauteId l'id technique Unireg d'une communauté
	 * @return les infos trouvée; ou <b>null</b> si la communauté est inconnue.
	 */
	@Nullable
	CommunauteRFMembreInfo getCommunauteMembreInfo(long communauteId);

	/**
	 * Construit l'URL de visualisation de l'immeuble spécifié dans l'interface Web de Capitastra.
	 *
	 * @param immeubleId l'id technique d'un immeuble
	 * @return l'URL demandée
	 * @throws ObjectNotFoundException si l'immeuble est inconnu.
	 */
	@NotNull
	String getCapitastraURL(long immeubleId) throws ObjectNotFoundException;

	/**
	 * @param tiersRF un tiers RF
	 * @return le numéro du contribuable rapproché avec le tiers RF spécifié.
	 */
	@Nullable
	Long getContribuableIdFor(@NotNull TiersRF tiersRF);

	/**
	 * @param immeuble immeuble du RF
	 * @param dateReference date de référence
	 * @return la commune de localisation de l'immeuble à la date de référence (peut être différent en fonction de la date en raison des fusions de communes)
	 */
	@Nullable
	Commune getCommune(ImmeubleRF immeuble, RegDate dateReference);

	/**
	 * @param immeuble immeuble du RF
	 * @param dateReference date de référence
	 * @return l'estimation fiscale valide à la date de référence
	 */
	@Nullable
	Long getEstimationFiscale(ImmeubleRF immeuble, RegDate dateReference);

	/**
	 * @param immeuble immeuble du RF
	 * @param dateReference date de référence
	 * @return le numéro de parcelle (avec indexes, séparés par des tirets)
	 */
	@Nullable
	String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference);

	/**
	 * @param immeuble immeuble du RF
	 * @param dateReference date de référence
	 * @return la situation valide à la date de référence ou, en l'absence d'une telle situation, la première situation valide après la date de référence
	 */
	@Nullable
	SituationRF getSituation(ImmeubleRF immeuble, RegDate dateReference);
}

package ch.vd.uniregctb.registrefoncier;

import java.util.List;
import java.util.Set;

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
	 * @param ctb            un contribuable Unireg
	 * @param includeVirtual vrai s'il faut inclure les droits virtuels du contribuable
	 * @return une liste de droits.
	 */
	List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean includeVirtual);

	/**
	 * Détermine les droits sur des immeubles d'un contribuable Unireg.
	 *
	 * @param ctb                        un contribuable Unireg
	 * @param prefetchSituationsImmeuble vrai si les situations et les immeubles doivent être chargés dans le cache de session Hibernate
	 * @param includeVirtual             vrai s'il faut inclure les droits virtuels du contribuable
	 * @return une liste de droits.
	 */
	List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean prefetchSituationsImmeuble, boolean includeVirtual);

	/**
	 * Détermine les droits sur des immeubles d'un tiers RF.
	 *
	 * @param ayantDroitRF               un ayant-droit RF
	 * @param prefetchSituationsImmeuble vrai si les situations et les immeubles doivent être chargés dans le cache de session Hibernate
	 * @param includeVirtual             vrai s'il faut inclure les droits virtuels du tiers RF
	 * @return la liste des droits du tiers RF
	 */
	List<DroitRF> getDroitsForTiersRF(AyantDroitRF ayantDroitRF, boolean prefetchSituationsImmeuble, boolean includeVirtual);

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
	 * @param communaute une communauté
	 * @return les infos trouvée; ou <b>null</b> si la communauté est inconnue.
	 */
	@NotNull
	CommunauteRFMembreInfo getCommunauteMembreInfo(@NotNull CommunauteRF communaute);

	/**
	 * @param communaute une communauté
	 * @return le numéro de ctb du principal courant de la communauté; ou <b>null</b> si la communauté est vide.
	 */
	@Nullable
	Long getCommunauteCurrentPrincipalId(@NotNull CommunauteRF communaute);

	/**
	 * Recherche ou crée un modèle de communauté qui correspond aux membres de communauté spécifiés.
	 * </p>
	 * <b>Attention !</b> Dans le cas où un nouveau modèle est créé, sa création est effectuée dans une transaction séparée et immédiatement committée.
	 *
	 * @param membres les membres de la communauté
	 * @return le modèle de communauté correspondant
	 */
	@NotNull
	ModeleCommunauteRF findOrCreateModeleCommunaute(@NotNull Set<? extends AyantDroitRF> membres);

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
	 * @param immeuble      immeuble du RF
	 * @param dateReference date de référence
	 * @return la commune de localisation de l'immeuble à la date de référence (peut être différent en fonction de la date en raison des fusions de communes)
	 */
	@Nullable
	Commune getCommune(ImmeubleRF immeuble, RegDate dateReference);

	/**
	 * @param immeuble      immeuble du RF
	 * @param dateReference date de référence
	 * @return l'estimation fiscale valide à la date de référence
	 */
	@Nullable
	EstimationRF getEstimationFiscale(ImmeubleRF immeuble, RegDate dateReference);

	/**
	 * @param immeuble      immeuble du RF
	 * @param dateReference date de référence
	 * @return le numéro de parcelle (avec indexes, séparés par des tirets)
	 */
	@Nullable
	String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference);

	/**
	 * @param immeuble      immeuble du RF
	 * @param dateReference date de référence
	 * @return la situation valide à la date de référence ou, en l'absence d'une telle situation, la première situation valide après la date de référence
	 */
	@Nullable
	SituationRF getSituation(ImmeubleRF immeuble, RegDate dateReference);

	/**
	 * Met-à-jour la commune fiscale surchargée sur une situation d'immeuble RF existante.
	 *
	 * @param situationId  l'id d'une situation existante
	 * @param noOfsCommune le numéro Ofs de la commune de surcharge
	 */
	void surchargerCommuneFiscaleSituation(long situationId, @Nullable Integer noOfsCommune);
}

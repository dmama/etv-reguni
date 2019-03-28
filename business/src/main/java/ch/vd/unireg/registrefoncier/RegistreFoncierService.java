package ch.vd.unireg.registrefoncier;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenomDates;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.foncier.AllegementFoncierVirtuel;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Service qui expose des méthodes d'accès aux données du registre foncier (Capistastra).
 */
public interface RegistreFoncierService {

	/**
	 * Détermine les droits sur des immeubles d'un contribuable Unireg.
	 *
	 * @param ctb                       un contribuable Unireg
	 * @param includeVirtualTransitive  vrai s'il faut inclure les droits virtuels transitifs du contribuable
	 * @param includeVirtualInheritance vrai s'il faut inclure les droits virtuels hérités du contribuable
	 * @return une liste de droits.
	 */
	List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean includeVirtualTransitive, boolean includeVirtualInheritance);

	/**
	 * Détermine les droits sur des immeubles d'un contribuable Unireg.
	 *
	 * @param ctb                        un contribuable Unireg
	 * @param prefetchSituationsImmeuble vrai si les situations et les immeubles doivent être chargés dans le cache de session Hibernate
	 * @param includeVirtualTransitive   vrai s'il faut inclure les droits virtuels transitifs du contribuable
	 * @param includeVirtualInheritance  vrai s'il faut inclure les droits virtuels hérités du contribuable
	 * @return une liste de droits.
	 */
	List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean prefetchSituationsImmeuble, boolean includeVirtualTransitive, boolean includeVirtualInheritance);

	/**
	 * Détermine les droits virtuels qui découlent d'un héritage ou d'une fusion d'entreprise (et seulement les droits virtuels).
	 *
	 * @param droit         un droit réel de référence
	 * @param contribuable  le contribuable rapproché au droit et qui possède potentiellement des héritiers ou des entreprises absorbantes
	 * @param dateReference une date de référence pour déterminer la validité des héritages/fusions
	 * @return une liste des droits virtuels; ou une liste vide si aucun héritage/fusion n'existe à la date de référence.
	 */
	@NotNull
	List<DroitVirtuelHeriteRF> determineDroitsVirtuelsHerites(@NotNull DroitProprieteRF droit, @Nullable Contribuable contribuable, @Nullable RegDate dateReference);

	/**
	 * Détermine les allégements fonciers virtuels qui découlent d'une fusion d'entreprise (et seulement les allégements virtuels).
	 *
	 * @param entreprise une entreprise
	 * @return une liste des allégements fonciers virtuels; ou une liste vide si aucune fusion n'existe pour cette entreprise.
	 */
	@NotNull
	List<AllegementFoncierVirtuel> determineAllegementsFonciersVirtuels(@NotNull Entreprise entreprise);

	/**
	 * @param immeubleId l'id technique d'un immeuble
	 * @return l'immeuble qui correspond à l'id; ou <b>null</b> si aucun immeuble ne correspond.
	 */
	@Nullable
	ImmeubleRF getImmeuble(long immeubleId);

	/**
	 * @param egrid l'egrid d'un immeuble
	 * @return l'immeuble qui correspond à l'egrid; ou <b>null</b> si aucun immeuble ne correspond.
	 */
	@Nullable
	ImmeubleRF getImmeuble(@NotNull String egrid);

	/**
	 * Retourne un immeuble à partir de sa situation précise.
	 *
	 * @param noOfsCommune le numéro OFS de la commune de l'immeuble (obligatoire)
	 * @param noParcelle   le numéro de parcelle de l'immeuble (obligatoire)
	 * @param index1       l'index n°1 (optionnel, si pas renseigné retourne l'immeuble avec un index1 nul)
	 * @param index2       l'index n°2 (optionnel, si pas renseigné retourne l'immeuble avec un index2 nul)
	 * @param index3       l'index n°3 (optionnel, si pas renseigné retourne l'immeuble avec un index3 nul)
	 * @return l'immeuble correspondant ou null si aucun immeuble ne correspond.
	 */
	@Nullable
	ImmeubleRF getImmeuble(int noOfsCommune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3);

	/**
	 * Recherche les immeubles qui correspondent aux critères fournis.
	 *
	 * @param noOfsCommune le numéro OFS de la commune de l'immeuble (optionnel)
	 * @param noParcelle      le numéro de parcelle de l'immeuble (optionnel)
	 * @param index1            l'index n°1 (optionnel)
	 * @param index2            l'index n°2 (optionnel)
	 * @param index3            l'index n°3 (optionnel)
	 * @return la liste des situations trouvées (et les immeubles associés).
	 */
	@NotNull
	List<SituationRF> findImmeublesParSituation(int noOfsCommune, int noParcelle, Integer index1, Integer index2, Integer index3);

	@Nullable
	DroitRF getDroit(long droitId);

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
	 * <p/>
	 * <b>Note:</b> cette méthode tient compte des rapports-entre-tiers d'héritage et complète la communauté RF en y ajoutant les héritiers des membres décédés.
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
	 * @param contribuable un contribuable
	 * @return la liste des regroupements de communautés dans lesquels apparaît le contribuable spécifié.
	 */
	@NotNull
	List<RegroupementCommunauteRF> getRegroupementsCommunautes(@NotNull Contribuable contribuable);

	/**
	 * Construit la vue historique des principaux (par défaut + explicites) pour un modèle de communauté.
	 *
	 * @param modeleCommunaute un modèle de communauté
	 * @param includeAnnules   <i>vrai</i> s'il faut inclure les principaux annulés; <i>faux</i> autrement.
	 * @param collate          <i>vrai</i> s'il faut fusionner les principaux identiques qui se suivent; <i>faux</i> autrement.
	 * @return l'historique des principaux
	 */
	@NotNull
	List<CommunauteRFPrincipalInfo> buildPrincipalHisto(@NotNull ModeleCommunauteRF modeleCommunaute, boolean includeAnnules, boolean collate);

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
	 * @param ayantDroit    un ayant-droit RF
	 * @param dateReference une date de référence
	 * @return le contribuable rapproché avec l'ayant-droit spécifié à la date spécifiée; <i>null</i> s'il n'y a pas de contribuable rapproché à la date spécifiée;
	 */
	@Nullable
	Contribuable getContribuableRapproche(@NotNull AyantDroitRF ayantDroit, @Nullable RegDate dateReference);

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

	/**
	 * Ajoute le membre spécifiée comme principal du modèle de communauté à partir d'une certaine date. L'historique des principaux est ordonné selon les dates de début et les dates de fin sont automatiquement recalculées.
	 *
	 * @param membre    un membre de la communauté
	 * @param modele    le modèle de communauté à mettre-à-jour
	 * @param dateDebut la date de début de valditié du membre comme principal
	 */
	void addPrincipalToModeleCommunaute(@NotNull TiersRF membre, @NotNull ModeleCommunauteRF modele, @NotNull RegDate dateDebut);

	/**
	 * Annule le principal de communauté spécifié.
	 *
	 * @param principal le principal à annuler
	 */
	void cancelPrincipalCommunaute(@NotNull PrincipalCommunauteRF principal);

	@Nullable
	AyantDroitRF getAyantDroit(long ayantDroitId);

	/**
	 * Retourne la décomposition nom/prénom/raison sociale du tiers RF spécifié. <b>Attention !</b> les valeurs retournées sont celles du Registre Foncier (et non celles des registres civiles des contribuables rapprochés).
	 *
	 * @param tiers un tiers RF
	 * @return la décomposition nom/prénom/raison sociale du tiers RF spécifié (données du RF !)
	 */
	@NotNull
	NomPrenomDates getDecompositionNomPrenomDateNaissanceRF(@NotNull TiersRF tiers);
}

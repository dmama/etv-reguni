package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Droit de propriété virtuel généré à la volée pour un tiers RF donné. Ce droit possède les caractéristiques suivantes :
 * <ul>
 *     <li>il n'est pas persisté</li>
 *     <li>le régime de propriété est toujours nul</li>
 *     <li>la par de propriété est toujours nulle</li>
 *     <li>la raison d'acquisition est toujours nulle</li>
 *     <li>il possède en plus le chemin vers l'immeuble concernée</li>
 * </ul>
 */
public class DroitProprieteRFVirtuel extends DroitProprieteRF {

	/**
	 * Si renseigné, la communauté à travers laquelle l'ayant-droit possède le droit de propriété.
	 */
	@Nullable
	private CommunauteRF communaute;

	/**
	 * La liste des droits qui mène de l'ayant-droit à l'immeuble.
	 */
	private List<DroitProprieteRF> chemin;

	@Nullable
	public CommunauteRF getCommunaute() {
		return communaute;
	}

	public void setCommunaute(@Nullable CommunauteRF communaute) {
		this.communaute = communaute;
	}

	public List<DroitProprieteRF> getChemin() {
		return chemin;
	}

	public void setChemin(List<DroitProprieteRF> chemin) {
		this.chemin = chemin;
	}
}

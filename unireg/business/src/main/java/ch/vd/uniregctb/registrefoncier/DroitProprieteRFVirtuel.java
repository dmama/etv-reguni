package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public class DroitProprieteRFVirtuel extends DroitProprieteRF {

	/**
	 * Si renseigné, la communauté à travers laquelle l'ayant-droit possède le droit de propriété.
	 */
	@Nullable
	private CommunauteRF communaute;

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

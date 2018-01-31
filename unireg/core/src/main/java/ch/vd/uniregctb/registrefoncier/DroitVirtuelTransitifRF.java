package ch.vd.uniregctb.registrefoncier;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public abstract class DroitVirtuelTransitifRF extends DroitVirtuelRF {

	/**
	 * L'ayant-droit RF concerné par le droit. Il peut être nul dans le cas où un tiers hérite fiscalement de droits mais ne possède (encore) pas d'immeuble lui-même.
	 */
	private AyantDroitRF ayantDroit;

	/**
	 * L'immeuble concerné par le droit.
	 */
	private ImmeubleRF immeuble;

	/**
	 * La liste des droits qui mène de l'ayant-droit à l'immeuble.
	 */
	private List<DroitRF> chemin;

	public AyantDroitRF getAyantDroit() {
		return ayantDroit;
	}

	public void setAyantDroit(AyantDroitRF ayantDroit) {
		this.ayantDroit = ayantDroit;
	}

	@Override
	@NotNull
	public List<AyantDroitRF> getAyantDroitList() {
		return Collections.singletonList(ayantDroit);
	}

	@Override
	public @NotNull List<ImmeubleRF> getImmeubleList() {
		return Collections.singletonList(immeuble);
	}

	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	public List<DroitRF> getChemin() {
		return chemin;
	}

	public void setChemin(List<DroitRF> chemin) {
		this.chemin = chemin;
	}

}

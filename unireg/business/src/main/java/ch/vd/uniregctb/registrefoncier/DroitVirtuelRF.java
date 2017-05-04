package ch.vd.uniregctb.registrefoncier;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.tiers.LinkedEntity;

/**
 * Classe abstraite qui représentent un droit de virtuel généré à la volée pour un tiers RF donné.
 */
public abstract class DroitVirtuelRF extends DroitRF {

	/**
	 * L'ayant-droit concerné par le droit.
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

	@Override
	public @NotNull List<AyantDroitRF> getAyantDroitList() {
		return Collections.singletonList(ayantDroit);
	}

	@Override
	public @NotNull List<ImmeubleRF> getImmeubleList() {
		return Collections.singletonList(immeuble);
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntity.@NotNull Context context, boolean includeAnnuled) {
		throw new ProgrammingException("On ne devrait jamais tomber ici car les droits virtuels ne sont pas persistés.");
	}
}

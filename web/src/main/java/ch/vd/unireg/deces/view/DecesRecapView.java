package ch.vd.unireg.deces.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

public class DecesRecapView {

	private Long tiersId;
	private RegDate dateDeces;
	private boolean marieSeul;
	private boolean veuf;
	private String remarque;

	public DecesRecapView() {
	}

	public DecesRecapView(long tiersId, @NotNull TiersService tiersService) {
		this.tiersId = tiersId;

		final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(tiersId);
		if (pp == null) {
			throw new TiersNotFoundException(tiersId);
		}
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, null);
		this.marieSeul = (couple != null && couple.getConjoint(pp) == null);
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

	public boolean isMarieSeul() {
		return marieSeul;
	}

	public void setMarieSeul(boolean marieSeul) {
		this.marieSeul = marieSeul;
	}

	public boolean isVeuf() {
		return veuf;
	}

	public void setVeuf(boolean veuf) {
		this.veuf = veuf;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}

}

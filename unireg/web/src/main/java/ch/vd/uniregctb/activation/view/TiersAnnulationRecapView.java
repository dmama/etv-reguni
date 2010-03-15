package ch.vd.uniregctb.activation.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.view.TiersGeneralView;

public class TiersAnnulationRecapView {

	private TiersGeneralView tiers;

	private TiersGeneralView tiersRemplacant;

	private RegDate dateAnnulation;

	private boolean nouveauTiers = true;

	public TiersGeneralView getTiers() {
		return tiers;
	}

	public void setTiers(TiersGeneralView tiers) {
		this.tiers = tiers;
	}

	public TiersGeneralView getTiersRemplacant() {
		return tiersRemplacant;
	}

	public void setTiersRemplacant(TiersGeneralView tiersRemplacant) {
		this.tiersRemplacant = tiersRemplacant;
	}

	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}

	public boolean isNouveauTiers() {
		return nouveauTiers;
	}

	public void setNouveauTiers(boolean nouveauTiers) {
		this.nouveauTiers = nouveauTiers;
	}

}

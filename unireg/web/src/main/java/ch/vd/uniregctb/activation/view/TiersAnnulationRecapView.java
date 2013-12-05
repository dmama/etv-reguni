package ch.vd.uniregctb.activation.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.TypeTiers;

public class TiersAnnulationRecapView {

	private Long numeroTiers;
	private TypeTiers typeTiers;
	private Long numeroTiersRemplacant;
	private RegDate dateAnnulation;

	public TypeTiers getTypeTiers() {
		return typeTiers;
	}

	public void setTypeTiers(TypeTiers typeTiers) {
		this.typeTiers = typeTiers;
	}

	public Long getNumeroTiers() {
		return numeroTiers;
	}

	public void setNumeroTiers(Long numeroTiers) {
		this.numeroTiers = numeroTiers;
	}

	public Long getNumeroTiersRemplacant() {
		return numeroTiersRemplacant;
	}

	public void setNumeroTiersRemplacant(Long numeroTiersRemplacant) {
		this.numeroTiersRemplacant = numeroTiersRemplacant;
	}

	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}
}

package ch.vd.uniregctb.registrefoncier.allegement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;

public abstract class AbstractEditDemandeDegrevementView {

	private Integer periodeFiscale;
	private RegDate delaiRetour;
	private RegDate dateRetour;

	public AbstractEditDemandeDegrevementView() {
	}

	public AbstractEditDemandeDegrevementView(DemandeDegrevementICI demande) {
		this.periodeFiscale = demande.getPeriodeFiscale();
		this.delaiRetour = demande.getDelaiRetour();
		this.dateRetour = demande.getDateRetour();
	}

	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public RegDate getDelaiRetour() {
		return delaiRetour;
	}

	public void setDelaiRetour(RegDate delaiRetour) {
		this.delaiRetour = delaiRetour;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}
}

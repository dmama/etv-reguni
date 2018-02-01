package ch.vd.unireg.entreprise.complexe;

public class RequisitionRadiationRCView extends FinActiviteView {

	private Integer periodeFiscale;
	private boolean imprimerDemandeBilanFinal;

	public RequisitionRadiationRCView() {
	}

	public RequisitionRadiationRCView(long idEntreprise) {
		super(idEntreprise);
	}

	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public boolean isImprimerDemandeBilanFinal() {
		return imprimerDemandeBilanFinal;
	}

	public void setImprimerDemandeBilanFinal(boolean imprimerDemandeBilanFinal) {
		this.imprimerDemandeBilanFinal = imprimerDemandeBilanFinal;
	}
}

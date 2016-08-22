package ch.vd.uniregctb.param.view;

import ch.vd.uniregctb.declaration.ParametrePeriodeFiscalePM;

public class ParametrePeriodeFiscalePMEditView {

	private Long idPeriodeFiscale;
	private Integer anneePeriodeFiscale;
	private boolean codeControleSurSommationDI;

	private Integer delaiImprimeMoisVaud;
	private ParametrePeriodeFiscalePM.ReferencePourDelai refDelaiVaud;
	private Boolean delaiImprimeRepousseFinDeMoisVaud;
	private Integer toleranceJoursVaud;
	private Boolean toleranceRepousseeFinDeMoisVaud;

	private Integer delaiImprimeMoisHorsCanton;
	private ParametrePeriodeFiscalePM.ReferencePourDelai refDelaiHorsCanton;
	private Boolean delaiImprimeRepousseFinDeMoisHorsCanton;
	private Integer toleranceJoursHorsCanton;
	private Boolean toleranceRepousseeFinDeMoisHorsCanton;

	private Integer delaiImprimeMoisHorsSuisse;
	private ParametrePeriodeFiscalePM.ReferencePourDelai refDelaiHorsSuisse;
	private Boolean delaiImprimeRepousseFinDeMoisHorsSuisse;
	private Integer toleranceJoursHorsSuisse;
	private Boolean toleranceRepousseeFinDeMoisHorsSuisse;

	private Integer delaiImprimeMoisUtilitePublique;
	private ParametrePeriodeFiscalePM.ReferencePourDelai refDelaiUtilitePublique;
	private Boolean delaiImprimeRepousseFinDeMoisUtilitePublique;
	private Integer toleranceJoursUtilitePublique;
	private Boolean toleranceRepousseeFinDeMoisUtilitePublique;

	public Long getIdPeriodeFiscale() {
		return idPeriodeFiscale;
	}

	public void setIdPeriodeFiscale(Long idPeriodeFiscale) {
		this.idPeriodeFiscale = idPeriodeFiscale;
	}

	public Integer getAnneePeriodeFiscale() {
		return anneePeriodeFiscale;
	}

	public void setAnneePeriodeFiscale(Integer anneePeriodeFiscale) {
		this.anneePeriodeFiscale = anneePeriodeFiscale;
	}

	public Integer getDelaiImprimeMoisVaud() {
		return delaiImprimeMoisVaud;
	}

	public void setDelaiImprimeMoisVaud(Integer delaiImprimeMoisVaud) {
		this.delaiImprimeMoisVaud = delaiImprimeMoisVaud;
	}

	public Boolean getDelaiImprimeRepousseFinDeMoisVaud() {
		return delaiImprimeRepousseFinDeMoisVaud;
	}

	public void setDelaiImprimeRepousseFinDeMoisVaud(Boolean delaiImprimeRepousseFinDeMoisVaud) {
		this.delaiImprimeRepousseFinDeMoisVaud = delaiImprimeRepousseFinDeMoisVaud;
	}

	public Integer getToleranceJoursVaud() {
		return toleranceJoursVaud;
	}

	public void setToleranceJoursVaud(Integer toleranceJoursVaud) {
		this.toleranceJoursVaud = toleranceJoursVaud;
	}

	public Boolean getToleranceRepousseeFinDeMoisVaud() {
		return toleranceRepousseeFinDeMoisVaud;
	}

	public void setToleranceRepousseeFinDeMoisVaud(Boolean toleranceRepousseeFinDeMoisVaud) {
		this.toleranceRepousseeFinDeMoisVaud = toleranceRepousseeFinDeMoisVaud;
	}

	public Integer getDelaiImprimeMoisHorsCanton() {
		return delaiImprimeMoisHorsCanton;
	}

	public void setDelaiImprimeMoisHorsCanton(Integer delaiImprimeMoisHorsCanton) {
		this.delaiImprimeMoisHorsCanton = delaiImprimeMoisHorsCanton;
	}

	public Boolean getDelaiImprimeRepousseFinDeMoisHorsCanton() {
		return delaiImprimeRepousseFinDeMoisHorsCanton;
	}

	public void setDelaiImprimeRepousseFinDeMoisHorsCanton(Boolean delaiImprimeRepousseFinDeMoisHorsCanton) {
		this.delaiImprimeRepousseFinDeMoisHorsCanton = delaiImprimeRepousseFinDeMoisHorsCanton;
	}

	public Integer getToleranceJoursHorsCanton() {
		return toleranceJoursHorsCanton;
	}

	public void setToleranceJoursHorsCanton(Integer toleranceJoursHorsCanton) {
		this.toleranceJoursHorsCanton = toleranceJoursHorsCanton;
	}

	public Boolean getToleranceRepousseeFinDeMoisHorsCanton() {
		return toleranceRepousseeFinDeMoisHorsCanton;
	}

	public void setToleranceRepousseeFinDeMoisHorsCanton(Boolean toleranceRepousseeFinDeMoisHorsCanton) {
		this.toleranceRepousseeFinDeMoisHorsCanton = toleranceRepousseeFinDeMoisHorsCanton;
	}

	public Integer getDelaiImprimeMoisHorsSuisse() {
		return delaiImprimeMoisHorsSuisse;
	}

	public void setDelaiImprimeMoisHorsSuisse(Integer delaiImprimeMoisHorsSuisse) {
		this.delaiImprimeMoisHorsSuisse = delaiImprimeMoisHorsSuisse;
	}

	public Boolean getDelaiImprimeRepousseFinDeMoisHorsSuisse() {
		return delaiImprimeRepousseFinDeMoisHorsSuisse;
	}

	public void setDelaiImprimeRepousseFinDeMoisHorsSuisse(Boolean delaiImprimeRepousseFinDeMoisHorsSuisse) {
		this.delaiImprimeRepousseFinDeMoisHorsSuisse = delaiImprimeRepousseFinDeMoisHorsSuisse;
	}

	public Integer getToleranceJoursHorsSuisse() {
		return toleranceJoursHorsSuisse;
	}

	public void setToleranceJoursHorsSuisse(Integer toleranceJoursHorsSuisse) {
		this.toleranceJoursHorsSuisse = toleranceJoursHorsSuisse;
	}

	public Boolean getToleranceRepousseeFinDeMoisHorsSuisse() {
		return toleranceRepousseeFinDeMoisHorsSuisse;
	}

	public void setToleranceRepousseeFinDeMoisHorsSuisse(Boolean toleranceRepousseeFinDeMoisHorsSuisse) {
		this.toleranceRepousseeFinDeMoisHorsSuisse = toleranceRepousseeFinDeMoisHorsSuisse;
	}

	public Integer getDelaiImprimeMoisUtilitePublique() {
		return delaiImprimeMoisUtilitePublique;
	}

	public void setDelaiImprimeMoisUtilitePublique(Integer delaiImprimeMoisUtilitePublique) {
		this.delaiImprimeMoisUtilitePublique = delaiImprimeMoisUtilitePublique;
	}

	public Boolean getDelaiImprimeRepousseFinDeMoisUtilitePublique() {
		return delaiImprimeRepousseFinDeMoisUtilitePublique;
	}

	public void setDelaiImprimeRepousseFinDeMoisUtilitePublique(Boolean delaiImprimeRepousseFinDeMoisUtilitePublique) {
		this.delaiImprimeRepousseFinDeMoisUtilitePublique = delaiImprimeRepousseFinDeMoisUtilitePublique;
	}

	public Integer getToleranceJoursUtilitePublique() {
		return toleranceJoursUtilitePublique;
	}

	public void setToleranceJoursUtilitePublique(Integer toleranceJoursUtilitePublique) {
		this.toleranceJoursUtilitePublique = toleranceJoursUtilitePublique;
	}

	public Boolean getToleranceRepousseeFinDeMoisUtilitePublique() {
		return toleranceRepousseeFinDeMoisUtilitePublique;
	}

	public void setToleranceRepousseeFinDeMoisUtilitePublique(Boolean toleranceRepousseeFinDeMoisUtilitePublique) {
		this.toleranceRepousseeFinDeMoisUtilitePublique = toleranceRepousseeFinDeMoisUtilitePublique;
	}

	public ParametrePeriodeFiscalePM.ReferencePourDelai getRefDelaiVaud() {
		return refDelaiVaud;
	}

	public void setRefDelaiVaud(ParametrePeriodeFiscalePM.ReferencePourDelai refDelaiVaud) {
		this.refDelaiVaud = refDelaiVaud;
	}

	public ParametrePeriodeFiscalePM.ReferencePourDelai getRefDelaiHorsCanton() {
		return refDelaiHorsCanton;
	}

	public void setRefDelaiHorsCanton(ParametrePeriodeFiscalePM.ReferencePourDelai refDelaiHorsCanton) {
		this.refDelaiHorsCanton = refDelaiHorsCanton;
	}

	public ParametrePeriodeFiscalePM.ReferencePourDelai getRefDelaiHorsSuisse() {
		return refDelaiHorsSuisse;
	}

	public void setRefDelaiHorsSuisse(ParametrePeriodeFiscalePM.ReferencePourDelai refDelaiHorsSuisse) {
		this.refDelaiHorsSuisse = refDelaiHorsSuisse;
	}

	public ParametrePeriodeFiscalePM.ReferencePourDelai getRefDelaiUtilitePublique() {
		return refDelaiUtilitePublique;
	}

	public void setRefDelaiUtilitePublique(ParametrePeriodeFiscalePM.ReferencePourDelai refDelaiUtilitePublique) {
		this.refDelaiUtilitePublique = refDelaiUtilitePublique;
	}

	public boolean isCodeControleSurSommationDI() {
		return codeControleSurSommationDI;
	}

	public void setCodeControleSurSommationDI(boolean codeControleSurSommationDI) {
		this.codeControleSurSommationDI = codeControleSurSommationDI;
	}
}

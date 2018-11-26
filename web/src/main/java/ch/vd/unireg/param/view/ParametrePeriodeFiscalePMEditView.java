package ch.vd.unireg.param.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM;
import ch.vd.unireg.type.TypeContribuable;

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

	public ParametrePeriodeFiscalePMEditView() {
	}

	public ParametrePeriodeFiscalePMEditView(@NotNull PeriodeFiscale pf) {

		this.idPeriodeFiscale = pf.getId();
		this.anneePeriodeFiscale = pf.getAnnee();
		this.codeControleSurSommationDI = pf.isShowCodeControleSommationDeclarationPM();

		final ParametrePeriodeFiscalePM hc = pf.getParametrePeriodeFiscalePM(TypeContribuable.HORS_CANTON);
		if (hc != null) {
			this.delaiImprimeMoisHorsCanton = hc.getDelaiImprimeMois();
			this.delaiImprimeRepousseFinDeMoisHorsCanton = hc.isDelaiImprimeRepousseFinDeMois();
			this.toleranceJoursHorsCanton = hc.getDelaiToleranceJoursEffective();
			this.toleranceRepousseeFinDeMoisHorsCanton = hc.isDelaiTolereRepousseFinDeMois();
			this.refDelaiHorsCanton = hc.getReferenceDelaiInitial();
		}

		final ParametrePeriodeFiscalePM hs = pf.getParametrePeriodeFiscalePM(TypeContribuable.HORS_SUISSE);
		if (hs != null) {
			this.delaiImprimeMoisHorsSuisse = hs.getDelaiImprimeMois();
			this.delaiImprimeRepousseFinDeMoisHorsSuisse = hs.isDelaiImprimeRepousseFinDeMois();
			this.toleranceJoursHorsSuisse = hs.getDelaiToleranceJoursEffective();
			this.toleranceRepousseeFinDeMoisHorsSuisse = hs.isDelaiTolereRepousseFinDeMois();
			this.refDelaiHorsSuisse = hs.getReferenceDelaiInitial();
		}

		final ParametrePeriodeFiscalePM vd = pf.getParametrePeriodeFiscalePM(TypeContribuable.VAUDOIS_ORDINAIRE);
		if (vd != null) {
			this.delaiImprimeMoisVaud = vd.getDelaiImprimeMois();
			this.delaiImprimeRepousseFinDeMoisVaud = vd.isDelaiImprimeRepousseFinDeMois();
			this.toleranceJoursVaud = vd.getDelaiToleranceJoursEffective();
			this.toleranceRepousseeFinDeMoisVaud = vd.isDelaiTolereRepousseFinDeMois();
			this.refDelaiVaud = vd.getReferenceDelaiInitial();
		}

		final ParametrePeriodeFiscalePM up = pf.getParametrePeriodeFiscalePM(TypeContribuable.UTILITE_PUBLIQUE);
		if (up != null) {
			this.delaiImprimeMoisUtilitePublique = up.getDelaiImprimeMois();
			this.delaiImprimeRepousseFinDeMoisUtilitePublique = up.isDelaiImprimeRepousseFinDeMois();
			this.toleranceJoursUtilitePublique = up.getDelaiToleranceJoursEffective();
			this.toleranceRepousseeFinDeMoisUtilitePublique = up.isDelaiTolereRepousseFinDeMois();
			this.refDelaiUtilitePublique = up.getReferenceDelaiInitial();
		}
	}

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

	public void saveTo(@NotNull PeriodeFiscale pf) {

		pf.setShowCodeControleSommationDeclarationPM(this.isCodeControleSurSommationDI());

		final ParametrePeriodeFiscalePM[] ppfs = new ParametrePeriodeFiscalePM[] {
				pf.getParametrePeriodeFiscalePM(TypeContribuable.VAUDOIS_ORDINAIRE),
				pf.getParametrePeriodeFiscalePM(TypeContribuable.HORS_CANTON),
				pf.getParametrePeriodeFiscalePM(TypeContribuable.HORS_SUISSE),
				pf.getParametrePeriodeFiscalePM(TypeContribuable.UTILITE_PUBLIQUE),
		};

		final int[][] delais = new int[][] {
				{delaiImprimeMoisVaud, toleranceJoursVaud},
				{delaiImprimeMoisHorsCanton, toleranceJoursHorsCanton},
				{delaiImprimeMoisHorsSuisse, toleranceJoursHorsSuisse},
				{delaiImprimeMoisUtilitePublique, toleranceJoursUtilitePublique}
		};

		final ParametrePeriodeFiscalePM.ReferencePourDelai[] refDelais = new ParametrePeriodeFiscalePM.ReferencePourDelai[] {
				refDelaiVaud,
				refDelaiHorsCanton,
				refDelaiHorsSuisse,
				refDelaiUtilitePublique
		};

		final boolean[][] reportsFinDeMois = new boolean[][] {
				{delaiImprimeRepousseFinDeMoisVaud, toleranceRepousseeFinDeMoisVaud},
				{delaiImprimeRepousseFinDeMoisHorsCanton, toleranceRepousseeFinDeMoisHorsCanton},
				{delaiImprimeRepousseFinDeMoisHorsSuisse, toleranceRepousseeFinDeMoisHorsSuisse},
				{delaiImprimeRepousseFinDeMoisUtilitePublique, toleranceRepousseeFinDeMoisUtilitePublique}
		};

		// On verifie que tous les parametres de periode fiscale ne soient pas null
		for (ParametrePeriodeFiscalePM ppf : ppfs) {
			if (ppf == null) {
				final String msgErr = "Impossible de retrouver tous les paramètres PM pour la période fiscale : " + pf.getAnnee();
				throw new ObjectNotFoundException(msgErr);
			}
		}

		// mise à jour des paramètres
		for (int i = 0 ; i < ppfs.length ; ++ i) {
			ppfs[i].setDelaiImprimeMois(delais[i][0]);
			ppfs[i].setDelaiImprimeRepousseFinDeMois(reportsFinDeMois[i][0]);
			ppfs[i].setDelaiToleranceJoursEffective(delais[i][1]);
			ppfs[i].setDelaiTolereRepousseFinDeMois(reportsFinDeMois[i][1]);
			ppfs[i].setReferenceDelaiInitial(refDelais[i]);
		}
	}
}

package ch.vd.uniregctb.param.view;

import ch.vd.registre.base.date.RegDate;

public class ParametrePeriodeFiscalePPEditView {

	private Long idPeriodeFiscale;
	private Integer anneePeriodeFiscale;
	private boolean codeControleSurSommationDI;

	private boolean emolumentSommationDI;
	private Integer montantEmolumentSommationDI;

	private RegDate sommationReglementaireVaud;
	private RegDate sommationEffectiveVaud;
	private RegDate finEnvoiMasseDIVaud;

	private RegDate sommationReglementaireHorsCanton;
	private RegDate sommationEffectiveHorsCanton;
	private RegDate finEnvoiMasseDIHorsCanton;

	private RegDate sommationReglementaireHorsSuisse;
	private RegDate sommationEffectiveHorsSuisse;
	private RegDate finEnvoiMasseDIHorsSuisse;

	private RegDate sommationReglementaireDepense;
	private RegDate sommationEffectiveDepense;
	private RegDate finEnvoiMasseDIDepense;

	private RegDate sommationReglementaireDiplomate;
	private RegDate sommationEffectiveDiplomate;
	private RegDate finEnvoiMasseDIDiplomate;

	public Integer getAnneePeriodeFiscale() {
		return anneePeriodeFiscale;
	}

	public void setAnneePeriodeFiscale(Integer anneePeriodeFiscale) {
		this.anneePeriodeFiscale = anneePeriodeFiscale;
	}

	public boolean isEmolumentSommationDI() {
		return emolumentSommationDI;
	}

	public void setEmolumentSommationDI(boolean emolumentSommationDI) {
		this.emolumentSommationDI = emolumentSommationDI;
	}

	public Integer getMontantEmolumentSommationDI() {
		return montantEmolumentSommationDI;
	}

	public void setMontantEmolumentSommationDI(Integer montantEmolumentSommationDI) {
		this.montantEmolumentSommationDI = montantEmolumentSommationDI;
	}

	public RegDate getSommationReglementaireVaud() {
		return sommationReglementaireVaud;
	}

	public void setSommationReglementaireVaud(RegDate sommationReglementaireVaud) {
		this.sommationReglementaireVaud = sommationReglementaireVaud;
	}

	public RegDate getSommationEffectiveVaud() {
		return sommationEffectiveVaud;
	}

	public void setSommationEffectiveVaud(RegDate sommationEffectiveVaud) {
		this.sommationEffectiveVaud = sommationEffectiveVaud;
	}

	public RegDate getFinEnvoiMasseDIVaud() {
		return finEnvoiMasseDIVaud;
	}

	public void setFinEnvoiMasseDIVaud(RegDate finEnvoiMasseDIVaud) {
		this.finEnvoiMasseDIVaud = finEnvoiMasseDIVaud;
	}

	public RegDate getSommationReglementaireHorsCanton() {
		return sommationReglementaireHorsCanton;
	}

	public void setSommationReglementaireHorsCanton(RegDate sommationReglementaireHorsCanton) {
		this.sommationReglementaireHorsCanton = sommationReglementaireHorsCanton;
	}

	public RegDate getSommationEffectiveHorsCanton() {
		return sommationEffectiveHorsCanton;
	}

	public void setSommationEffectiveHorsCanton(RegDate sommationEffectiveHorsCanton) {
		this.sommationEffectiveHorsCanton = sommationEffectiveHorsCanton;
	}

	public RegDate getFinEnvoiMasseDIHorsCanton() {
		return finEnvoiMasseDIHorsCanton;
	}

	public void setFinEnvoiMasseDIHorsCanton(RegDate finEnvoiMasseDIHorsCanton) {
		this.finEnvoiMasseDIHorsCanton = finEnvoiMasseDIHorsCanton;
	}

	public RegDate getSommationReglementaireHorsSuisse() {
		return sommationReglementaireHorsSuisse;
	}

	public void setSommationReglementaireHorsSuisse(RegDate sommationReglementaireHorsSuisse) {
		this.sommationReglementaireHorsSuisse = sommationReglementaireHorsSuisse;
	}

	public RegDate getSommationEffectiveHorsSuisse() {
		return sommationEffectiveHorsSuisse;
	}

	public void setSommationEffectiveHorsSuisse(RegDate sommationEffectiveHorsSuisse) {
		this.sommationEffectiveHorsSuisse = sommationEffectiveHorsSuisse;
	}

	public RegDate getFinEnvoiMasseDIHorsSuisse() {
		return finEnvoiMasseDIHorsSuisse;
	}

	public void setFinEnvoiMasseDIHorsSuisse(RegDate finEnvoiMasseDIHorsSuisse) {
		this.finEnvoiMasseDIHorsSuisse = finEnvoiMasseDIHorsSuisse;
	}

	public RegDate getSommationReglementaireDepense() {
		return sommationReglementaireDepense;
	}

	public void setSommationReglementaireDepense(RegDate sommationReglementaireDepense) {
		this.sommationReglementaireDepense = sommationReglementaireDepense;
	}

	public RegDate getSommationEffectiveDepense() {
		return sommationEffectiveDepense;
	}

	public void setSommationEffectiveDepense(RegDate sommationEffectiveDepense) {
		this.sommationEffectiveDepense = sommationEffectiveDepense;
	}

	public RegDate getFinEnvoiMasseDIDepense() {
		return finEnvoiMasseDIDepense;
	}

	public void setFinEnvoiMasseDIDepense(RegDate finEnvoiMasseDIDepense) {
		this.finEnvoiMasseDIDepense = finEnvoiMasseDIDepense;
	}

	public Long getIdPeriodeFiscale() {
		return idPeriodeFiscale;
	}

	public void setIdPeriodeFiscale(Long idPeriodeFiscale) {
		this.idPeriodeFiscale = idPeriodeFiscale;
	}

	public RegDate getSommationReglementaireDiplomate() {
		return sommationReglementaireDiplomate;
	}

	public void setSommationReglementaireDiplomate(RegDate sommationReglementaireDiplomate) {
		this.sommationReglementaireDiplomate = sommationReglementaireDiplomate;
	}

	public RegDate getSommationEffectiveDiplomate() {
		return sommationEffectiveDiplomate;
	}

	public void setSommationEffectiveDiplomate(RegDate sommationEffectiveDiplomate) {
		this.sommationEffectiveDiplomate = sommationEffectiveDiplomate;
	}

	public RegDate getFinEnvoiMasseDIDiplomate() {
		return finEnvoiMasseDIDiplomate;
	}

	public void setFinEnvoiMasseDIDiplomate(RegDate finEnvoiMasseDIDiplomate) {
		this.finEnvoiMasseDIDiplomate = finEnvoiMasseDIDiplomate;
	}

	public boolean isCodeControleSurSommationDI() {
		return codeControleSurSommationDI;
	}

	public void setCodeControleSurSommationDI(boolean codeControleSurSommationDI) {
		this.codeControleSurSommationDI = codeControleSurSommationDI;
	}
}

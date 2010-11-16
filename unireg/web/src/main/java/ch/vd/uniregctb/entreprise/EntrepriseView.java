package ch.vd.uniregctb.entreprise;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.view.EtatPMView;
import ch.vd.uniregctb.tiers.view.RegimeFiscalView;

public class EntrepriseView {

	private String numeroIPMRO;
	private String designationAbregee;
	private String raisonSociale;
	private String raisonSociale1;
	private String raisonSociale2;
	private String raisonSociale3;
	private RegDate dateFinDernierExerciceCommercial;
	private RegDate dateBouclementFuture;

	private List<SiegeView> sieges;
	private List<FormeJuridiqueView> formesJuridiques;
	private List<CapitalView> capitaux;
	private List<RegimeFiscalView> regimesFiscauxVD;
	private List<RegimeFiscalView> regimesFiscauxCH;
	private List<EtatPMView> etats;

	public String getNumeroIPMRO() {
		return numeroIPMRO;
	}

	public void setNumeroIPMRO(String numeroIPMRO) {
		this.numeroIPMRO = numeroIPMRO;
	}

	public String getDesignationAbregee() {
		return designationAbregee;
	}

	public void setDesignationAbregee(String designationAbregee) {
		this.designationAbregee = designationAbregee;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public String getRaisonSociale1() {
		return raisonSociale1;
	}

	public void setRaisonSociale1(String raisonSociale1) {
		this.raisonSociale1 = raisonSociale1;
	}

	public String getRaisonSociale2() {
		return raisonSociale2;
	}

	public void setRaisonSociale2(String raisonSociale2) {
		this.raisonSociale2 = raisonSociale2;
	}

	public String getRaisonSociale3() {
		return raisonSociale3;
	}

	public void setRaisonSociale3(String raisonSociale3) {
		this.raisonSociale3 = raisonSociale3;
	}

	public RegDate getDateFinDernierExerciceCommercial() {
		return dateFinDernierExerciceCommercial;
	}

	public void setDateFinDernierExerciceCommercial(RegDate dateFinDernierExerciceCommercial) {
		this.dateFinDernierExerciceCommercial = dateFinDernierExerciceCommercial;
	}

	public RegDate getDateBouclementFuture() {
		return dateBouclementFuture;
	}

	public void setDateBouclementFuture(RegDate dateBouclementFuture) {
		this.dateBouclementFuture = dateBouclementFuture;
	}

	public List<SiegeView> getSieges() {
		return sieges;
	}

	public void setSieges(List<SiegeView> sieges) {
		this.sieges = sieges;
	}

	public List<FormeJuridiqueView> getFormesJuridiques() {
		return formesJuridiques;
	}

	public void setFormesJuridiques(List<FormeJuridiqueView> formesJuridiques) {
		this.formesJuridiques = formesJuridiques;
	}

	public List<CapitalView> getCapitaux() {
		return capitaux;
	}

	public void setCapitaux(List<CapitalView> capitaux) {
		this.capitaux = capitaux;
	}

	public List<RegimeFiscalView> getRegimesFiscauxVD() {
		return regimesFiscauxVD;
	}

	public void setRegimesFiscauxVD(List<RegimeFiscalView> regimesFiscauxVD) {
		this.regimesFiscauxVD = regimesFiscauxVD;
	}

	public List<RegimeFiscalView> getRegimesFiscauxCH() {
		return regimesFiscauxCH;
	}

	public void setRegimesFiscauxCH(List<RegimeFiscalView> regimesFiscauxCH) {
		this.regimesFiscauxCH = regimesFiscauxCH;
	}

	public List<EtatPMView> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatPMView> etats) {
		this.etats = etats;
	}
}

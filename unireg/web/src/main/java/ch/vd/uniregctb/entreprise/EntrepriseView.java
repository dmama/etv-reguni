package ch.vd.uniregctb.entreprise;

import java.util.List;

import ch.vd.uniregctb.tiers.view.EtatPMView;

public class EntrepriseView {

	private String raisonSociale;
	private List<String> autresRaisonsSociales;

	private List<SiegeView> sieges;
	private List<FormeJuridiqueView> formesJuridiques;
	private List<CapitalView> capitaux;
	private List<EtatPMView> etats;
	private List<String> numerosIDE;

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public List<String> getAutresRaisonsSociales() {
		return autresRaisonsSociales;
	}

	public void setAutresRaisonsSociales(List<String> autresRaisonsSociales) {
		this.autresRaisonsSociales = autresRaisonsSociales;
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

	public List<EtatPMView> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatPMView> etats) {
		this.etats = etats;
	}

	public List<String> getNumerosIDE() {
		return numerosIDE;
	}

	public void setNumerosIDE(List<String> numerosIDE) {
		this.numerosIDE = numerosIDE;
	}
}

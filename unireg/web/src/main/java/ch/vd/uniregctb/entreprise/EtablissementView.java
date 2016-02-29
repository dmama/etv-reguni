package ch.vd.uniregctb.entreprise;

import java.util.List;

import ch.vd.uniregctb.tiers.view.DomicileEtablissementView;

public class EtablissementView {

	private Long id;
	private boolean connueAuCivil = false;

	private String raisonSociale;
	private String enseigne;

	private List<DomicileEtablissementView> domiciles;

	private List<String> numerosIDE;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isConnueAuCivil() {
		return connueAuCivil;
	}

	public void setConnueAuCivil(boolean connueAuCivil) {
		this.connueAuCivil = connueAuCivil;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}

	public List<DomicileEtablissementView> getDomiciles() {
		return domiciles;
	}

	public void setDomiciles(List<DomicileEtablissementView> domiciles) {
		this.domiciles = domiciles;
	}

	public List<String> getNumerosIDE() {
		return numerosIDE;
	}

	public void setNumerosIDE(List<String> numerosIDE) {
		this.numerosIDE = numerosIDE;
	}


}

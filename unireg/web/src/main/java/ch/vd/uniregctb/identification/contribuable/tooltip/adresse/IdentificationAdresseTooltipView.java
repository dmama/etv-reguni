package ch.vd.uniregctb.identification.contribuable.tooltip.adresse;

import ch.vd.uniregctb.adresse.AdresseGenerique;

public class IdentificationAdresseTooltipView {

	private String rue;
	private String complements;
	private String localite;
	private String pays;
	private AdresseGenerique.Source source;
	private String errorMessage;

	public void init(String rue, String complements, String localite, String pays, AdresseGenerique.Source source) {
		this.rue = rue;
		this.complements = complements;
		this.localite = localite;
		this.pays = pays;
		this.source = source;
		this.errorMessage = null;
	}

	public void init(String errorMessage) {
		this.rue = null;
		this.complements = null;
		this.localite = null;
		this.pays = null;
		this.source = null;
		this.errorMessage = errorMessage;
	}
	
	public void reset() {
		this.rue = null;
		this.complements = null;
		this.localite = null;
		this.pays = null;
		this.source = null;
		this.errorMessage = null;
	}

	public String getRue() {
		return rue;
	}

	public String getComplements() {
		return complements;
	}

	public String getLocalite() {
		return localite;
	}

	public String getPays() {
		return pays;
	}

	public AdresseGenerique.Source getSource() {
		return source;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}

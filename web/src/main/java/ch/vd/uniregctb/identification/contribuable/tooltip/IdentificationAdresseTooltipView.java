package ch.vd.uniregctb.identification.contribuable.tooltip;

import ch.vd.uniregctb.adresse.AdresseGenerique;

public class IdentificationAdresseTooltipView {

	private final String rue;
	private final String complements;
	private final String localite;
	private final String pays;
	private final AdresseGenerique.SourceType source;
	private final String errorMessage;

	public IdentificationAdresseTooltipView(String rue, String complements, String localite, String pays, AdresseGenerique.SourceType source) {
		this.rue = rue;
		this.complements = complements;
		this.localite = localite;
		this.pays = pays;
		this.source = source;
		this.errorMessage = null;
	}

	public IdentificationAdresseTooltipView(String errorMessage) {
		this.rue = null;
		this.complements = null;
		this.localite = null;
		this.pays = null;
		this.source = null;
		this.errorMessage = errorMessage;
	}
	
	public IdentificationAdresseTooltipView() {
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

	public AdresseGenerique.SourceType getSource() {
		return source;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}

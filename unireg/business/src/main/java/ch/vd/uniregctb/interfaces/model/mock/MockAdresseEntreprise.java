package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;

public class MockAdresseEntreprise implements AdresseEntreprise {

	private String complement;
	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private String localite;
	private String numeroMaison;
	private String rue;
	private EnumTypeAdresseEntreprise type;

	public String getComplement() {
		return complement;
	}

	public void setComplement(String complement) {
		this.complement = complement;
	}

	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	public String getLocalite() {
		return localite;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	public String getNumeroMaison() {
		return numeroMaison;
	}

	public void setNumeroMaison(String numeroMaison) {
		this.numeroMaison = numeroMaison;
	}

	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	public EnumTypeAdresseEntreprise getType() {
		return type;
	}

	public void setType(EnumTypeAdresseEntreprise type) {
		this.type = type;
	}
}

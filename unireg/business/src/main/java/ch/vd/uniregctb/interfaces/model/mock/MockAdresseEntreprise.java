package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.Pays;

public class MockAdresseEntreprise implements AdresseEntreprise {

	private String complement;
	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private String localite;
	private String numeroMaison;
	private String rue;
	private EnumTypeAdresseEntreprise type;
	private Integer numeroTechniqueRue;
	private Pays pays;
	private int numeroOrdrePostal;
	private String numeroPostal;
	private String numeroPostalComplementaire;

	public void setNumeroTechniqueRue(Integer numeroTechniqueRue) {
		this.numeroTechniqueRue = numeroTechniqueRue;
	}

	public void setPays(Pays pays) {
		this.pays = pays;
	}

	public void setNumeroOrdrePostal(int numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
	}

	public void setNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
	}

	public void setNumeroPostalComplementaire(String numeroPostalComplementaire) {
		this.numeroPostalComplementaire = numeroPostalComplementaire;
	}

	public String getComplement() {
		return complement;
	}

	public Integer getNumeroTechniqueRue() {
		return numeroTechniqueRue;
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

	public String getLocaliteAbregeMinuscule() {
		return localite;
	}

	public String getLocaliteCompletMinuscule() {
		return localite;
	}

	public Pays getPays() {
		return pays;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	public String getNumeroMaison() {
		return numeroMaison;
	}

	public int getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public String getNumeroPostal() {
		return numeroPostal;
	}

	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
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

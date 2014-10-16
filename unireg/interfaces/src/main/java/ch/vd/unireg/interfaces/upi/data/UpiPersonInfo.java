package ch.vd.unireg.interfaces.upi.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.Sexe;

public class UpiPersonInfo implements Serializable {

	private static final long serialVersionUID = 2089884639803618596L;

	private final String noAvs13;
	private Sexe sexe;
	private RegDate dateNaissance;
	private String prenoms;
	private String nom;

	public UpiPersonInfo(String noAvs13) {
		this.noAvs13 = noAvs13;
	}

	public String getNoAvs13() {
		return noAvs13;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public String getPrenoms() {
		return prenoms;
	}

	public void setPrenoms(String prenoms) {
		this.prenoms = prenoms;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}
}

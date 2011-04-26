package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.type.Sexe;

public class MockHistoriqueIndividu implements HistoriqueIndividu {

	private String autresPrenoms;
	private String complementIdentification;
	private RegDate dateDebutValidite;
	private String noAVS;
	private int noSequence;
	private String nom;
	private String nomCourrier1;
	private String nomCourrier2;
	private String nomNaissance;
	private String prenom;
	private String profession;
	private Sexe sexe;

	public MockHistoriqueIndividu() {

	}

	public MockHistoriqueIndividu(RegDate dateDebutValidite, String nom, String prenom) {
		this.dateDebutValidite = dateDebutValidite;
		this.prenom = prenom;
		this.nom = nom;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	public void setAutresPrenoms(String autresPrenoms) {
		this.autresPrenoms = autresPrenoms;
	}

	public String getComplementIdentification() {
		return complementIdentification;
	}

	public void setComplementIdentification(String complementIdentification) {
		this.complementIdentification = complementIdentification;
	}

	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	public String getNoAVS() {
		return noAVS;
	}

	public void setNoAVS(String noAVS) {
		this.noAVS = noAVS;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public void setNoSequence(int noSequence) {
		this.noSequence = noSequence;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getNomCourrier1() {
		return nomCourrier1;
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier1 = nomCourrier1;
	}

	public String getNomCourrier2() {
		return nomCourrier2;
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier2 = nomCourrier2;
	}

	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public String getProfession() {
		return profession;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}
}

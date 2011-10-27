package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.type.Sexe;

public class MockHistoriqueIndividu implements HistoriqueIndividu {

	private String autresPrenoms;
	private RegDate dateDebutValidite;
	private String noAVS;
	private String nom;
	private String nomNaissance;
	private String prenom;
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

	@Override
	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	public void setAutresPrenoms(String autresPrenoms) {
		this.autresPrenoms = autresPrenoms;
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	@Override
	public String getNoAVS() {
		return noAVS;
	}

	public void setNoAVS(String noAVS) {
		this.noAVS = noAVS;
	}

	@Override
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Override
	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	@Override
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}
}

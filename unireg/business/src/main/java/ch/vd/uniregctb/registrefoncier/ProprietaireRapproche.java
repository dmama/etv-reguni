package ch.vd.uniregctb.registrefoncier;

import ch.vd.registre.base.date.RegDate;

public class ProprietaireRapproche extends ProprietaireFoncier {

	private String nom1;
	private String prenom1;
	private RegDate dateNaissance1;
	private long numeroContribuable1;
	private String nom2;
	private String prenom2;
	private RegDate dateNaissance2;
	private long numeroContribuable2;
	private String formulePolitesse;

	public String getFormulePolitesse() {
		return formulePolitesse;
	}

	public void setFormulePolitesse(String formulePolitesse) {
		this.formulePolitesse = formulePolitesse;
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

	private String nomCourrier1;
	private String nomCourrier2;
	private String resultat;

	public ProprietaireRapproche(long numeroRegistreFoncier, String nom, String prenom, RegDate dateNaissance, long numeroContribuable) {
		super(numeroRegistreFoncier, nom, prenom, dateNaissance, numeroContribuable);

	}

	public ProprietaireRapproche(ProprietaireFoncier proprio) {
		super(proprio);
	}

	public String getNom1() {
		return nom1;
	}

	public void setNom1(String nom1) {
		this.nom1 = nom1;
	}

	public String getPrenom1() {
		return prenom1;
	}

	public void setPrenom1(String prenom1) {
		this.prenom1 = prenom1;
	}

	public RegDate getDateNaissance1() {
		return dateNaissance1;
	}

	public void setDateNaissance1(RegDate dateNaissance1) {
		this.dateNaissance1 = dateNaissance1;
	}

	public long getNumeroContribuable1() {
		return numeroContribuable1;
	}

	public void setNumeroContribuable1(long numeroContribuable1) {
		this.numeroContribuable1 = numeroContribuable1;
	}

	public String getNom2() {
		return nom2;
	}

	public void setNom2(String nom2) {
		this.nom2 = nom2;
	}

	public String getPrenom2() {
		return prenom2;
	}

	public void setPrenom2(String prenom2) {
		this.prenom2 = prenom2;
	}

	public RegDate getDateNaissance2() {
		return dateNaissance2;
	}

	public void setDateNaissance2(RegDate dateNaissance2) {
		this.dateNaissance2 = dateNaissance2;
	}

	public long getNumeroContribuable2() {
		return numeroContribuable2;
	}

	public void setNumeroContribuable2(long numeroContribuable2) {
		this.numeroContribuable2 = numeroContribuable2;
	}

	public String getResultat() {
		return resultat;
	}

	public void setResultat(String resultat) {
		this.resultat = resultat;
	}

}

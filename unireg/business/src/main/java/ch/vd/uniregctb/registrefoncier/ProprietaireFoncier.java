package ch.vd.uniregctb.registrefoncier;

import ch.vd.registre.base.date.RegDate;

public class ProprietaireFoncier {

    private long numeroRegistreFoncier;
    private String nom;
    private String prenom;
    private RegDate dateNaissance;
    private Long numeroContribuable;

    public ProprietaireFoncier(long numeroRegistreFoncier, String nom, String prenom, RegDate dateNaissance, Long numeroContribuable) {
        this.numeroRegistreFoncier = numeroRegistreFoncier;
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.numeroContribuable = numeroContribuable;
    }

    protected ProprietaireFoncier(ProprietaireFoncier proprio) {
    	this.dateNaissance = proprio.getDateNaissance();
    	this.nom = proprio.getNom();
    	this.prenom = proprio.getPrenom();
    	this.numeroContribuable = proprio.getNumeroContribuable();
    	this.numeroRegistreFoncier = proprio.getNumeroRegistreFoncier();
    }

    public long getNumeroRegistreFoncier() {
        return numeroRegistreFoncier;
    }

    public void setNumeroRegistreFoncier(long numeroRegistreFoncier) {
        this.numeroRegistreFoncier = numeroRegistreFoncier;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public RegDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(RegDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public Long getNumeroContribuable() {
        return numeroContribuable;
    }

    public void setNumeroContribuable(Long numeroContribuable) {
        this.numeroContribuable = numeroContribuable;
    }

	@Override
	public String toString() {
		return String.valueOf(numeroRegistreFoncier);
	}
}

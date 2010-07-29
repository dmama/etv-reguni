package ch.vd.uniregctb.web.xt.handler;

import ch.vd.securite.model.Operateur;
import org.apache.commons.lang.StringEscapeUtils;

public class WrapperUtilisateur {

	private String visaOperateur;
	private String nom;
	private String prenom;
	private Long individuNoTechnique;

	public WrapperUtilisateur(Operateur utlisateur) {
		this.visaOperateur = utlisateur.getCode();
		this.nom = StringEscapeUtils.escapeXml(utlisateur.getNom());
		this.prenom = StringEscapeUtils.escapeXml(utlisateur.getPrenom());
		this.individuNoTechnique = utlisateur.getIndividuNoTechnique();
	}

	public String getVisaOperateur() {
		return visaOperateur;
	}

	public void setVisaOperateur(String visaOperateur) {
		this.visaOperateur = visaOperateur;
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

	public Long getIndividuNoTechnique() {
		return individuNoTechnique;
	}

	public void setIndividuNoTechnique(Long individuNoTechnique) {
		this.individuNoTechnique = individuNoTechnique;
	}



}
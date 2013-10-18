package ch.vd.uniregctb.general.view;

import java.io.Serializable;

public class UtilisateurView implements Serializable {

	private static final long serialVersionUID = -1181597556095764613L;

	private String visaOperateur;
	private Long numeroIndividu;
	private String prenomNom;
	private String officeImpot;

	public String getVisaOperateur() {
		return visaOperateur;
	}

	public void setVisaOperateur(String visaOperateur) {
		this.visaOperateur = visaOperateur;
	}

	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public void setNumeroIndividu(Long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
	}

	public String getPrenomNom() {
		return prenomNom;
	}

	public void setPrenomNom(String prenomNom) {
		this.prenomNom = prenomNom;
	}

	public String getOfficeImpot() {
		return officeImpot;
	}

	public void setOfficeImpot(String officeImpot) {
		this.officeImpot = officeImpot;
	}
}

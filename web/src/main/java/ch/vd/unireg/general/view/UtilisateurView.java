package ch.vd.unireg.general.view;

import java.io.Serializable;

/**
 * Donnée d'un utilisateur (= opérateur) d'Unireg.
 */
public class UtilisateurView implements Serializable {

	private static final long serialVersionUID = 268658459889280729L;

	private String visaOperateur;
	private String prenomNom;
	private String officeImpot;

	public String getVisaOperateur() {
		return visaOperateur;
	}

	public void setVisaOperateur(String visaOperateur) {
		this.visaOperateur = visaOperateur;
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

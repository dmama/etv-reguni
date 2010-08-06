package ch.vd.uniregctb.acces.parDossier.view;

import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.DroitAcces;

public class DroitAccesView extends DroitAcces implements Annulable{

	private static final long serialVersionUID = -2346891502349854834L;


	private String visaOperateur;

	private String prenomNom;

	private String officeImpot;

	private boolean lectureSeule;

	private Long numero;

	private String utilisateur;

	private Long numeroUtilisateur;

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

	public boolean isLectureSeule() {
		return lectureSeule;
	}

	public void setLectureSeule(boolean lectureSeule) {
		this.lectureSeule = lectureSeule;
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public String getUtilisateur() {
		return utilisateur;
	}

	public void setUtilisateur(String utilisateur) {
		this.utilisateur = utilisateur;
	}

	public Long getNumeroUtilisateur() {
		return numeroUtilisateur;
	}

	public void setNumeroUtilisateur(Long numeroUtilisateur) {
		this.numeroUtilisateur = numeroUtilisateur;
	}

}

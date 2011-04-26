package ch.vd.uniregctb.acces.parDossier.view;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class DroitAccesView implements Serializable, Annulable {

	private static final long serialVersionUID = -2346891502349854834L;

	//
	// partie modification
	//

	private String visaOperateur;

	private String prenomNom;

	private String officeImpot;

	private boolean lectureSeule;

	private Long numero;

	private String utilisateur;

	private Long numeroUtilisateur;

	private boolean ajoutEffectue;

	//
	// partie visualisation
	//

	private TypeDroitAcces type;

	private boolean annule;

	private Long id;
	
	private Niveau niveau;

	private RegDate dateDebut;

	private RegDate dateFin;

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

	public TypeDroitAcces getType() {
		return type;
	}

	public void setType(TypeDroitAcces type) {
		this.type = type;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Niveau getNiveau() {
		return niveau;
	}

	public void setNiveau(Niveau niveau) {
		this.niveau = niveau;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public void resetOperateur() {
		setVisaOperateur(null);
		setUtilisateur(null);
		setNumeroUtilisateur(null);
	}

	public boolean isAjoutEffectue() {
		return ajoutEffectue;
	}

	public void setAjoutEffectue(boolean ajoutEffectue) {
		this.ajoutEffectue = ajoutEffectue;
	}
}

package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;

public class HistoriqueIndividuImpl implements HistoriqueIndividu, Serializable {

	private static final long serialVersionUID = 4030612779028440946L;
	
	private final RegDate dateDebut;
	private final String autresPrenoms;
	private final String complementIdentification;
	private final String noAVS;
	private final int noSequence;
	private final String nom;
	private final String nomCourrier1;
	private final String nomCourrier2;
	private final String nomNaissance;
	private final String prenom;
	private final String profession;

	public static HistoriqueIndividuImpl get(ch.vd.registre.civil.model.HistoriqueIndividu target) {
		if (target == null) {
			return null;
		}
		return new HistoriqueIndividuImpl(target);
	}

	private HistoriqueIndividuImpl(ch.vd.registre.civil.model.HistoriqueIndividu target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.autresPrenoms = target.getAutresPrenoms();
		this.complementIdentification = target.getComplementIdentification();
		this.noAVS = target.getNoAVS();
		this.noSequence = target.getNoSequence();
		this.nom = target.getNom();
		this.nomCourrier1 = target.getNomCourrier1();
		this.nomCourrier2 = target.getNomCourrier2();
		this.nomNaissance = target.getNomNaissance();
		this.prenom = target.getPrenom();
		this.profession = target.getProfession();
	}

	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	public String getComplementIdentification() {
		return complementIdentification;
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public String getNoAVS() {
		return noAVS;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public String getNom() {
		return nom;
	}

	public String getNomCourrier1() {
		return nomCourrier1;
	}

	public String getNomCourrier2() {
		return nomCourrier2;
	}

	public String getNomNaissance() {
		return nomNaissance;
	}

	public String getPrenom() {
		return prenom;
	}

	public String getProfession() {
		return profession;
	}
}

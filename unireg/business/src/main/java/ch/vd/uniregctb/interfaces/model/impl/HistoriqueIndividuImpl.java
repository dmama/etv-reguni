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

	@Override
	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	@Override
	public String getComplementIdentification() {
		return complementIdentification;
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	@Override
	public String getNoAVS() {
		return noAVS;
	}

	@Override
	public int getNoSequence() {
		return noSequence;
	}

	@Override
	public String getNom() {
		return nom;
	}

	@Override
	public String getNomCourrier1() {
		return nomCourrier1;
	}

	@Override
	public String getNomCourrier2() {
		return nomCourrier2;
	}

	@Override
	public String getNomNaissance() {
		return nomNaissance;
	}

	@Override
	public String getPrenom() {
		return prenom;
	}

	@Override
	public String getProfession() {
		return profession;
	}
}

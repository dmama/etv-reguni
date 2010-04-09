package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;

public class HistoriqueIndividuWrapper implements HistoriqueIndividu {

	private final RegDate dateDebut;
	private String autresPrenoms;
	private String complementIdentification;
	private String noAVS;
	private int noSequence;
	private String nom;
	private String nomCourrier1;
	private String nomCourrier2;
	private String nomNaissance;
	private String prenom;
	private String profession;

	public static HistoriqueIndividuWrapper get(ch.vd.registre.civil.model.HistoriqueIndividu target) {
		if (target == null) {
			return null;
		}
		return new HistoriqueIndividuWrapper(target);
	}

	private HistoriqueIndividuWrapper(ch.vd.registre.civil.model.HistoriqueIndividu target) {
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

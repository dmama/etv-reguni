package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.type.TypeTutelle;

public class MockTutelle implements Tutelle {

	private RegDate dateDebut;
	private RegDate dateFin;
	private String libelleMotif;
	private int noSequence;
	private String nomAutoriteTutelaire;
	private Long numeroCollectiviteAutoriteTutelaire;
	private Individu tuteur;
	private TuteurGeneral tuteurGeneral;
	private TypeTutelle typeTutelle;

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

	public String getLibelleMotif() {
		return libelleMotif;
	}

	public void setLibelleMotif(String libelleMotif) {
		this.libelleMotif = libelleMotif;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public void setNoSequence(int noSequence) {
		this.noSequence = noSequence;
	}

	public String getNomAutoriteTutelaire() {
		return nomAutoriteTutelaire;
	}

	public void setNomAutoriteTutelaire(String nomAutoriteTutelaire) {
		this.nomAutoriteTutelaire = nomAutoriteTutelaire;
	}

	public Individu getTuteur() {
		return tuteur;
	}

	public void setTuteur(Individu tuteur) {
		this.tuteur = tuteur;
	}

	public TuteurGeneral getTuteurGeneral() {
		return tuteurGeneral;
	}

	public void setTuteurGeneral(TuteurGeneral tuteurGeneral) {
		this.tuteurGeneral = tuteurGeneral;
	}

	public TypeTutelle getTypeTutelle() {
		return typeTutelle;
	}

	public void setTypeTutelle(TypeTutelle typeTutelle) {
		this.typeTutelle = typeTutelle;
	}

	public Long getNumeroCollectiviteAutoriteTutelaire() {
		return numeroCollectiviteAutoriteTutelaire;
	}

	public void setNumeroCollectiviteAutoriteTutelaire(Long numeroCollectiviteAutoriteTutelaire) {
		this.numeroCollectiviteAutoriteTutelaire = numeroCollectiviteAutoriteTutelaire;
	}
}

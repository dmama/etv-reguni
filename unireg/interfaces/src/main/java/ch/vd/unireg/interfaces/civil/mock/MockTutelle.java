package ch.vd.unireg.interfaces.civil.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Tutelle;
import ch.vd.unireg.interfaces.civil.data.TuteurGeneral;
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

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public String getLibelleMotif() {
		return libelleMotif;
	}

	public void setLibelleMotif(String libelleMotif) {
		this.libelleMotif = libelleMotif;
	}

	@Override
	public int getNoSequence() {
		return noSequence;
	}

	public void setNoSequence(int noSequence) {
		this.noSequence = noSequence;
	}

	@Override
	public String getNomAutoriteTutelaire() {
		return nomAutoriteTutelaire;
	}

	public void setNomAutoriteTutelaire(String nomAutoriteTutelaire) {
		this.nomAutoriteTutelaire = nomAutoriteTutelaire;
	}

	@Override
	public Individu getTuteur() {
		return tuteur;
	}

	public void setTuteur(Individu tuteur) {
		this.tuteur = tuteur;
	}

	@Override
	public TuteurGeneral getTuteurGeneral() {
		return tuteurGeneral;
	}

	public void setTuteurGeneral(TuteurGeneral tuteurGeneral) {
		this.tuteurGeneral = tuteurGeneral;
	}

	@Override
	public TypeTutelle getTypeTutelle() {
		return typeTutelle;
	}

	public void setTypeTutelle(TypeTutelle typeTutelle) {
		this.typeTutelle = typeTutelle;
	}

	@Override
	public Long getNumeroCollectiviteAutoriteTutelaire() {
		return numeroCollectiviteAutoriteTutelaire;
	}

	public void setNumeroCollectiviteAutoriteTutelaire(Long numeroCollectiviteAutoriteTutelaire) {
		this.numeroCollectiviteAutoriteTutelaire = numeroCollectiviteAutoriteTutelaire;
	}
}

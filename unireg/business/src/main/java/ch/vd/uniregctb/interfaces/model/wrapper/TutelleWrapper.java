package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeTutelle;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;

public class TutelleWrapper implements Tutelle, Serializable {

	private static final long serialVersionUID = -687108599649537399L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private Individu tuteur;
	private ch.vd.registre.civil.model.Individu targetTuteur;
	private TuteurGeneral tuteurGeneral;
	private ch.vd.registre.civil.model.TuteurGeneral targetTuteurGeneral;
	private String libelleMotif;
	private int noSequence;
	private String nomAutoriteTutelaire;
	private Long numeroCollectiviteAutoriteTutelaire;
	private EnumTypeTutelle typeTutelle;

	public static TutelleWrapper get(ch.vd.registre.civil.model.Tutelle target) {
		if (target == null) {
			return null;
		}
		return new TutelleWrapper(target);
	}

	private TutelleWrapper(ch.vd.registre.civil.model.Tutelle target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.libelleMotif = target.getLibelleMotif();
		this.noSequence = target.getNoSequence();
		this.nomAutoriteTutelaire = target.getNomAutoriteTutelaire();
		this.numeroCollectiviteAutoriteTutelaire = target.getNumeroCollectiviteAutoriteTutellaire();
		this.targetTuteur = target.getTuteur();
		this.targetTuteurGeneral = target.getTuteurGeneral();
		this.typeTutelle = target.getTypeTutelle();
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public String getLibelleMotif() {
		return libelleMotif;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public String getNomAutoriteTutelaire() {
		return nomAutoriteTutelaire;
	}

	public Long getNumeroCollectiviteAutoriteTutelaire() {
		return numeroCollectiviteAutoriteTutelaire;
	}

	public Individu getTuteur() {
		if (tuteur == null && targetTuteur != null) {
			tuteur = IndividuWrapper.get(targetTuteur);
			targetTuteur = null;
		}
		return tuteur;
	}

	public TuteurGeneral getTuteurGeneral() {
		if (tuteurGeneral == null && targetTuteurGeneral != null) {
			tuteurGeneral = TuteurGeneralWrapper.get(targetTuteurGeneral);
			targetTuteurGeneral = null;
		}
		return tuteurGeneral;
	}

	public EnumTypeTutelle getTypeTutelle() {
		return typeTutelle;
	}

}

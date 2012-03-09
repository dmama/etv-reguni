package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.type.TypeTutelle;

public class TutelleImpl implements Tutelle, Serializable {

	private static final long serialVersionUID = -687108599649537399L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Individu tuteur;
	private final TuteurGeneral tuteurGeneral;
	private final String libelleMotif;
	private final int noSequence;
	private final String nomAutoriteTutelaire;
	private final Long numeroCollectiviteAutoriteTutelaire;
	private final TypeTutelle typeTutelle;

	public static TutelleImpl get(ch.vd.registre.civil.model.Tutelle target, RegDate upTo) {
		if (target == null) {
			return null;
		}
		if (upTo != null && target.getDateDebut() != null && target.getDateDebut().after(upTo.asJavaDate())) {
			return null;
		}
		return new TutelleImpl(target, upTo);
	}

	private TutelleImpl(ch.vd.registre.civil.model.Tutelle target, RegDate upTo) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.libelleMotif = target.getLibelleMotif();
		this.noSequence = target.getNoSequence();
		this.nomAutoriteTutelaire = target.getNomAutoriteTutelaire();
		this.numeroCollectiviteAutoriteTutelaire = target.getNumeroCollectiviteAutoriteTutellaire();
		this.tuteur = IndividuImpl.get(target.getTuteur(), upTo);
		this.tuteurGeneral = TuteurGeneralImpl.get(target.getTuteurGeneral());
		this.typeTutelle = TypeTutelle.get(target.getTypeTutelle());
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String getLibelleMotif() {
		return libelleMotif;
	}

	@Override
	public int getNoSequence() {
		return noSequence;
	}

	@Override
	public String getNomAutoriteTutelaire() {
		return nomAutoriteTutelaire;
	}

	@Override
	public Long getNumeroCollectiviteAutoriteTutelaire() {
		return numeroCollectiviteAutoriteTutelaire;
	}

	@Override
	public Individu getTuteur() {
		return tuteur;
	}

	@Override
	public TuteurGeneral getTuteurGeneral() {
		return tuteurGeneral;
	}

	@Override
	public TypeTutelle getTypeTutelle() {
		return typeTutelle;
	}

}

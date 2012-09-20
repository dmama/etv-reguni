package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeTutelle;
import ch.vd.uniregctb.type.TypeTutelle;

public class TutelleImpl implements Tutelle, Serializable {

	private static final long serialVersionUID = -1258171130548686359L;

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
		this.typeTutelle = initTypeTutelle(target.getTypeTutelle());
	}

	private static TypeTutelle initTypeTutelle(EnumTypeTutelle type) {
		if (type == null) {
			return null;
		}
		else if (type == EnumTypeTutelle.TUTELLE) {
			return TypeTutelle.TUTELLE;
		}
		else if (type == EnumTypeTutelle.CURATELLE) {
			return TypeTutelle.CURATELLE;
		}
		else if (type == EnumTypeTutelle.CONSEIL_LEGAL) {
			return TypeTutelle.CONSEIL_LEGAL;
		}
		else {
			throw new IllegalArgumentException("Type de tutelle inconnu = [" + type.getName() + ']');
		}
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

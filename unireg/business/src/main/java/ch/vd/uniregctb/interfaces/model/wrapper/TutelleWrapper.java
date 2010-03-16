package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeTutelle;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;

public class TutelleWrapper implements Tutelle {

	private final ch.vd.registre.civil.model.Tutelle target;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private Individu tuteur;
	private TuteurGeneral tuteurGeneral;

	public static TutelleWrapper get(ch.vd.registre.civil.model.Tutelle target) {
		if (target == null) {
			return null;
		}
		return new TutelleWrapper(target);
	}

	private TutelleWrapper(ch.vd.registre.civil.model.Tutelle target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public String getLibelleMotif() {
		return target.getLibelleMotif();
	}

	public int getNoSequence() {
		return target.getNoSequence();
	}

	public String getNomAutoriteTutelaire() {
		return target.getNomAutoriteTutelaire();
	}

	public Individu getTuteur() {
		if (tuteur == null) {
			tuteur = IndividuWrapper.get(target.getTuteur());
		}
		return tuteur;
	}

	public TuteurGeneral getTuteurGeneral() {
		if (tuteurGeneral == null) {
			tuteurGeneral = TuteurGeneralWrapper.get(target.getTuteurGeneral());
		}
		return tuteurGeneral;
	}

	public EnumTypeTutelle getTypeTutelle() {
		return target.getTypeTutelle();
	}

}

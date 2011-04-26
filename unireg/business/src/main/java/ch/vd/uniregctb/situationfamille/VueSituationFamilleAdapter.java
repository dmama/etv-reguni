package ch.vd.uniregctb.situationfamille;

import java.util.Date;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.EtatCivil;

/**
 * Classe de base permettant d'adapter une situation de famille en fonction de nouvelles dates de début/fin.
 */
public abstract class VueSituationFamilleAdapter<T extends VueSituationFamille> implements VueSituationFamille {

	private final T target;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public VueSituationFamilleAdapter(T target, RegDate dateDebut, RegDate dateFin) {
		this.target = target;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	protected T getTarget() {
		return target;
	}

	public RegDate getDateDebut() {
		if (dateDebut == null) {
			return target.getDateDebut();
		}
		else {
			return dateDebut;
		}
	}

	public RegDate getDateFin() {
		if (dateFin == null) {
			return target.getDateFin();
		}
		else {
			return dateFin;
		}
	}

	public Long getId() {
		return target.getId();
	}

	public Integer getNombreEnfants() {
		return target.getNombreEnfants();
	}

	public Source getSource() {
		return target.getSource();
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	public boolean isAnnule() {
		return target.isAnnule();
	}

	public Date getAnnulationDate() {
		return target.getAnnulationDate();
	}

	public EtatCivil getEtatCivil() {
		return target.getEtatCivil();
	}
}

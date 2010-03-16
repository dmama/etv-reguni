package ch.vd.uniregctb.situationfamille;

import java.util.Date;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;

/**
 * Classe de base permettant d'adapter une situation de famille en provenance du fiscal.
 */
public abstract class VueSituationFamilleFiscalAdapter implements VueSituationFamille {

	private final SituationFamille situation;

	private final Source source;

	public VueSituationFamilleFiscalAdapter(SituationFamille situation, Source source) {
		this.situation = situation;
		this.source = source;
	}

	public Long getId() {
		return situation.getId();
	}

	public RegDate getDateDebut() {
		return situation.getDateDebut();
	}

	public RegDate getDateFin() {
		return situation.getDateFin();
	}

	public Integer getNombreEnfants() {
		return situation.getNombreEnfants();
	}

	public Source getSource() {
		return source;
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	public boolean isAnnule() {
		return situation.isAnnule();
	}

	public Date getAnnulationDate() {
		return situation.getAnnulationDate();
	}

	public EtatCivil getEtatCivil() {
		return situation.getEtatCivil();
	}
}

package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue("Semestriel")
public class Semestriel extends Periodicite {

	private static final long serialVersionUID = 3641798749348427971L;
	@Override
	public RegDate getDebutPeriode(RegDate reference) {
		final int semestre = ((reference.month() - 1) / 6); // janvier-juin = 0, juillet-decembre = 1
		return RegDate.get(reference.year(), (semestre * 6) + 1, 1);
	}

	@Override
	public RegDate getDebutPeriodeSuivante(RegDate reference) {
		return getDebutPeriode(reference).addMonths(6);
	}

	@Transient
	@Override
	public String getTypePeriodicite() {
		return SEMESTRIEL;
	}
}

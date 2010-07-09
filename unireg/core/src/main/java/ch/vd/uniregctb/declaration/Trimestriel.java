package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;



@Entity
@DiscriminatorValue("Trimestriel")
public class Trimestriel extends Periodicite {

	private static final long serialVersionUID = 3641798749395427983L;
	@Override
	public RegDate getDebutPeriode(RegDate reference) {
		final int trimestre = ((reference.month() - 1) / 3); // janvier-mars = 0, avril-juin = 1, ...
		return RegDate.get(reference.year(), (trimestre * 3) + 1, 1);
	}

	@Override
	public RegDate getDebutPeriodeSuivante(RegDate reference) {
		return getDebutPeriode(reference).addMonths(3);
	}

	@Transient
	@Override
	public String getTypePeriodicite() {
		return TRIMESTRIEL;
	}
}

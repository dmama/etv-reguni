package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;


@Entity
@DiscriminatorValue("Annuel")
public class Annuel extends Periodicite {

	private static final long serialVersionUID = 3641798749348427852L;
	@Override
	public RegDate getDebutPeriode(RegDate reference) {
		return RegDate.get(reference.year(), 1, 1);
	}

	@Override
	public RegDate getDebutPeriodeSuivante(RegDate reference) {
		return RegDate.get(reference.year() + 1, 1, 1);
	}

	@Transient
	@Override
	public String getTypePeriodicite() {
		return ANNUEL; 
	}
}

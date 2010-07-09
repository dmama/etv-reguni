package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue("Mensuel")
public class Mensuel extends Periodicite {


	private static final long serialVersionUID = 3641798749349377983L;
	@Override
	public RegDate getDebutPeriode(RegDate reference) {
		return RegDate.get(reference.year(), reference.month(), 1);
	}

	@Override
	public RegDate getDebutPeriodeSuivante(RegDate reference) {
		return RegDate.get(reference.year(), reference.month(), 1).addMonths(1);
	}

	@Transient
	@Override
	public String getTypePeriodicite() {
		return MENSUEL;
	}
}

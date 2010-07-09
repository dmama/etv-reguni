package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;

@Entity
@DiscriminatorValue("Unique")
public class Unique extends Periodicite {

	private static final long serialVersionUID = 3641798749348427943L;
	@Override
	public RegDate getDebutPeriode(RegDate reference) {
		//TODO (FDE a implementer)
		throw new NotImplementedException();
	}

	@Override
	public RegDate getDebutPeriodeSuivante(RegDate reference) {
		throw new NotImplementedException();
	}

	@Transient
	@Override
	public String getTypePeriodicite() {
		return UNIQUE;  //To change body of implemented methods use File | Settings | File Templates.
	}
}

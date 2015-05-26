package ch.vd.uniregctb.migration.pm.rcent.model.wrappers;

import java.math.BigInteger;
import java.util.List;

import ch.vd.evd0022.v1.Identification;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedWrapper;

public class RCEntIdentification  extends RCEntRangedWrapper<Identification> {

	public RCEntIdentification(RegDate beginDate, RegDate endDateDate, Identification element) {
		super(beginDate, endDateDate, element);
	}

	public List<String> getOtherName() {
		return getElement().getOtherName();
	}

	public BigInteger getCantonalId() {
		return getElement().getCantonalId();
	}

	public List<Identifier> getIdentifier() {
		return getElement().getIdentifier();
	}

	public String getName() {
		return getElement().getName();
	}
}

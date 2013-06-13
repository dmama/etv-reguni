package ch.vd.uniregctb.evenement.party.control;

import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;

public interface TaxliabilityControlRule {

	public TaxliabilityControlResult check() throws ControlRuleException;

}

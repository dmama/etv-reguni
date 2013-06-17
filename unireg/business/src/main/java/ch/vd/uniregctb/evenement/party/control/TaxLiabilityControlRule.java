package ch.vd.uniregctb.evenement.party.control;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.Tiers;

public interface TaxLiabilityControlRule {

	public TaxLiabilityControlResult check(@NotNull Tiers tiers) throws ControlRuleException;

}

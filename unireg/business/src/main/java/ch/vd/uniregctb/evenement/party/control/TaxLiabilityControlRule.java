package ch.vd.uniregctb.evenement.party.control;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.Tiers;

/**
 * @param <T> type de valeurs collectées ({@link ch.vd.uniregctb.type.ModeImposition} ou {@link ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement})
 */
public interface TaxLiabilityControlRule<T extends Enum<T>> {

	TaxLiabilityControlResult<T> check(@NotNull Tiers tiers, Set<T> aRejeter) throws ControlRuleException;
}

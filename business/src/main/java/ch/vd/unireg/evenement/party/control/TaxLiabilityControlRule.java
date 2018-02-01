package ch.vd.unireg.evenement.party.control;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.tiers.Contribuable;

/**
 * @param <T> type de valeurs collectées ({@link ch.vd.unireg.type.ModeImposition} ou {@link ch.vd.unireg.metier.assujettissement.TypeAssujettissement})
 */
public interface TaxLiabilityControlRule<T extends Enum<T>> {

	TaxLiabilityControlResult<T> check(@NotNull Contribuable ctb, Set<T> aRejeter) throws ControlRuleException;
}

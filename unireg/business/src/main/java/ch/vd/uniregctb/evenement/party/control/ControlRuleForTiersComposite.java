package ch.vd.uniregctb.evenement.party.control;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe abstraire representant les règles de contrôle sur les tiers composés c'est à dire ménage ou Parent en ménage
 * Elle a besoin d'une règle de contrôle tierce(règle Tiers pour l'instant) pour calculer ses propres règles de contrôle
 *
 */
public abstract class ControlRuleForTiersComposite<T extends Enum<T>> extends AbstractControlRule<T> {

	protected AbstractControlRule<T> controlRule;

	protected ControlRuleForTiersComposite(TiersService tiersService, @NotNull AbstractControlRule<T> controlRule) {
		super(tiersService);
		this.controlRule = controlRule;
	}

	public final boolean isAssujetti(@NotNull Tiers tiers) throws ControlRuleException {
		return controlRule.isAssujetti(tiers);
	}

	@Override
	public final Set<T> getSourceAssujettissement(@NotNull Tiers tiers) throws ControlRuleException {
		return controlRule.getSourceAssujettissement(tiers);
	}
}

package ch.vd.unireg.evenement.party.control;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;

/**
 * Classe abstraire representant les règles de contrôle sur les tiers composés c'est à dire ménage ou Parent en ménage
 * Elle a besoin d'une règle de contrôle tierce(règle Tiers pour l'instant) pour calculer ses propres règles de contrôle
 *
 */
public abstract class ControlRuleForTiersComposite<T extends Enum<T>> extends AbstractControlRule<T> {

	protected final AbstractControlRule<T> controlRule;

	protected ControlRuleForTiersComposite(TiersService tiersService, @NotNull AbstractControlRule<T> controlRule) {
		super(tiersService);
		this.controlRule = controlRule;
	}

	public final AssujettissementStatut checkAssujettissement(@NotNull Contribuable ctb, Set<T> aRejeter) throws ControlRuleException {
		return controlRule.checkAssujettissement(ctb, aRejeter);
	}

	@Override
	public final Set<T> getSourceAssujettissement(@NotNull Contribuable ctb) throws ControlRuleException {
		return controlRule.getSourceAssujettissement(ctb);
	}
}

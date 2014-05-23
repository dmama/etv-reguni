package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe abstraire representant les règles de contrôle sur les tiers composés c'est à dire ménage ou Parent en ménage
 * Elle a besoin d'une règle de contrôle tierce(règle Tiers pour l'instant) pour calculer ses propres règles de contrôle
 *
 */
public abstract class ControleRuleForTiersComposite extends AbstractControlRule {

	protected AbstractControlRule controlRule;

	protected ControleRuleForTiersComposite(TiersService tiersService,AbstractControlRule controlRule) {
		super(tiersService);
		this.controlRule = controlRule;
	}

	public boolean isAssujetti(@NotNull Tiers tiers) throws ControlRuleException{
		return controlRule.isAssujetti(tiers);
	};

	@Override
	public <T> List<T> getSourceAssujettissement(@NotNull Tiers tiers) throws ControlRuleException {
		return controlRule.getSourceAssujettissement(tiers);
	}

	public  boolean isAssujettissementNonConforme(@NotNull Tiers tiers) throws ControlRuleException{
		return controlRule.isAssujettissementNonConforme(tiers);
	};
}

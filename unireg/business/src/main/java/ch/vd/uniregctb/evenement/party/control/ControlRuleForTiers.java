package ch.vd.uniregctb.evenement.party.control;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Régle A1.3:Déclenchée si demande porte sur date déterminante.
 * Vérification du for fiscal principal Unireg en vigueur * à la date déterminante sur le numéro tiers fourni
 */
public abstract class ControlRuleForTiers extends AbstractControlRule {

	protected ControlRuleForTiers(TiersService tiersService) {
		super(tiersService);
	}

	@Override
	public TaxLiabilityControlResult check(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult result = new TaxLiabilityControlResult();
		//S'il y a un assujettissement sur tout ou partie de la PF (au moins 1 jour) -> CTRL OK
		if (isAssujetti(tiers)) {
			result.setIdTiersAssujetti(tiers.getId());
		}
		//	Dans le cas contraire (pas un seul jour d'assujettissement)-> CTRL KO
		else {
			setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, null, null, null);
		}
		return result;
	}

	public abstract boolean isMineur(PersonnePhysique personne);

}

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
		//S'il y a un assujettissement sur tout ou partie de la PF (au moins 1 jour) -> CTRL OK et que l'assujetissement est conforme à la demande
		final boolean assujettissementNonConforme = isAssujettissementNonConforme(tiers);
		if (isAssujetti(tiers) && !assujettissementNonConforme) {
			result.setIdTiersAssujetti(tiers.getId());
			result.setOrigine(TaxLiabilityControlResult.Origine.INITIAL);
			result.setSourceAssujettissements(getSourceAssujettissement(tiers));
		}
		//	Dans le cas contraire (pas un seul jour d'assujettissement)-> CTRL KO
		else {
			setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, null, null, null,assujettissementNonConforme);
		}
		return result;
	}

	public abstract boolean isMineur(PersonnePhysique personne);

}

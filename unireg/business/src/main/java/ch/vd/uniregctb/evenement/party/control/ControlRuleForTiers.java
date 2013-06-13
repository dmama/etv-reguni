package ch.vd.uniregctb.evenement.party.control;

import ch.vd.uniregctb.evenement.party.TaxliabilityControlEchecType;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;

/**
 * Régle A1.3:Déclenchée si demande porte sur date déterminante.
 * Vérification du for fiscal principal Unireg en vigueur * à la date déterminante sur le numéro tiers fourni
 */
public abstract class ControlRuleForTiers extends AbstractControlRule {


	public ControlRuleForTiers(Context contex, Long tiersId) {
		super(contex,tiersId);
	}

	@Override
	public TaxliabilityControlResult check() throws ControlRuleException {
		TaxliabilityControlResult result = new TaxliabilityControlResult();
		Tiers tiers =  context.tiersDAO.get(tiersId);
		//S'il y a un assujettissement sur tout ou partie de la PF (au moins 1 jour) -> CTRL OK
		if (isAssujetti(tiersId)) {
			result.setIdTiersAssujetti(tiersId);
		}
		//	Dans le cas contraire (pas un seul jour d'assujettissement)-> CTRL KO
		else {
			setErreur(result, TaxliabilityControlEchecType.CONTROLE_NUMERO_KO, null, null, null);
		}

		return result;
	}


	public  boolean isMineur(long tiersId){
		PersonnePhysique personne = (PersonnePhysique)context.tiersService.getTiers(tiersId);
		return isMineur(personne);
	}

	public abstract boolean isMineur(PersonnePhysique personne);

}

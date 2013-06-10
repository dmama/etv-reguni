package ch.vd.uniregctb.evenement.party.control;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.xml.Context;

/**
 * Régle A1.3:Déclenchée si demande porte sur date déterminante.
 * Vérification du for fiscal principal Unireg en vigueur * à la date déterminante sur le numéro tiers fourni
 */
public class ControlRuleForTiersDate extends ControlRuleForTiers {


	private RegDate date;


	public ControlRuleForTiersDate(Context contex, Long tiersId, RegDate date) {
		super(contex, tiersId);
		this.date = date;

	}

	@Override
	public boolean isAssujetti(Long tiersId) throws ControlRuleException {
		return hasForFiscalPrincipalAt(tiersId, date);
	}

	/**
	 *
	 * @param tiersId
	 * @param date
	 * @return
	 * @throws ControlRuleException
	 */
	protected boolean hasForFiscalPrincipalAt(Long tiersId,RegDate date) throws ControlRuleException {
		Tiers tiers = context.tiersService.getTiers(tiersId);
		if (tiers == null) {
			final String message = String.format("Le tiers %d n'existe pas",tiersId);
			throw new ControlRuleException(message);

		}
		ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(date);

		if (forFiscalPrincipal!=null) {
			//For fiscal principal "vaudois" en vigueur (assujettissement illimité) >> CTRL OK
			if (forFiscalPrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				//Vérification des date du for
				final boolean isForEnVigueur = RegDateHelper.isBetween(date,forFiscalPrincipal.getDateDebut(),forFiscalPrincipal.getDateFin(), NullDateBehavior.LATEST);
				if (isForEnVigueur) {
					return true;
				}
				else{
					//Le for ne respecte pas la notion en vigueur
					return false;
				}

			}
			//For fiscal principal "HC" ou "HS" en vigueur (assujettissement limité) >> CTRL KO
			else{
				return false;
			}

		}
		//Pas de for fiscal principal en vigueur à la date >> CTRL KO
		else{
			return false;
		}
	}
	@Override
	public boolean isMineur(PersonnePhysique personne ) {
		return context.tiersService.isMineur(personne,date);
	}

}

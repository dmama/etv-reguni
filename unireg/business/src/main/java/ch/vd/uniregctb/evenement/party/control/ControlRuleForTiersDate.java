package ch.vd.uniregctb.evenement.party.control;

import ch.vd.registre.base.date.RegDate;
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

	private final RegDate date;

	public ControlRuleForTiersDate(Context context, long tiersId, RegDate date) {
		super(context, tiersId);
		this.date = date;
	}

	@Override
	public boolean isAssujetti(long tiersId) throws ControlRuleException {
		return hasForFiscalPrincipalAt(tiersId);
	}

	/**
	 * @param tiersId
	 * @return
	 * @throws ControlRuleException
	 */
	protected boolean hasForFiscalPrincipalAt(long tiersId) throws ControlRuleException {
		final Tiers tiers = context.tiersService.getTiers(tiersId);
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(date);
		return forFiscalPrincipal != null && forFiscalPrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Override
	public boolean isMineur(PersonnePhysique personne ) {
		return context.tiersService.isMineur(personne,date);
	}

}

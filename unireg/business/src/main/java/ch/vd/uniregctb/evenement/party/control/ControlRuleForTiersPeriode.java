package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.xml.Context;

/**
 * Régle A1.1: Utilisation de l'algorithme Unireg de détermination des assujettissements d'un numéro de tiers sur la PF
 */
public class ControlRuleForTiersPeriode extends ControlRuleForTiers {

	private final int periode;

	public ControlRuleForTiersPeriode(Context context, long tiersId, int periode) {
		super(context, tiersId);
		this.periode = periode;
	}

	@Override
	public boolean isAssujetti(long tiersId) throws ControlRuleException {
		return isAssujettiSurPeriode(tiersId);
	}

	private boolean isAssujettiSurPeriode(long tiersId) throws ControlRuleException {
		final Contribuable contribuable= context.tiersDAO.getContribuableByNumero(tiersId);
		List<Assujettissement> assujetissements=null;
		try {
			assujetissements= context.assujettissementService.determine(contribuable,periode);
		}
		catch (AssujettissementException e) {
			throw  new ControlRuleException(e.getMessage());
		}

		//return vrai si le contribuable est assutti sur la periode
		return (assujetissements!=null && !assujetissements.isEmpty());
	}

	@Override
	public boolean isMineur(PersonnePhysique personne ) {
		return context.tiersService.isMineur(personne, RegDate.get(periode, 12, 31));
	}

}

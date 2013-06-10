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


	private Integer periode;


	public ControlRuleForTiersPeriode(Context contex, Long tiersId, Integer periode) {
		super(contex, tiersId);
		this.periode = periode;

	}

	@Override
	public boolean isAssujetti(Long tiersId) {
		return isAssujettiSurPeriode(tiersId, periode);
	}


	protected boolean isAssujettiSurPeriode(Long tiersId,Integer periode) {
		final Contribuable contribuable= context.tiersDAO.getContribuableByNumero(tiersId);
		//TODO lever une exception si le contribuable n'existe pas ou faire la verification avant
		List<Assujettissement> assujetissements=null;
		try {
			assujetissements= context.assujettissementService.determine(contribuable,periode);
		}
		catch (AssujettissementException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}



		//return vrai si le contribuable est assutti sur la periode
		return (assujetissements!=null && !assujetissements.isEmpty());
	}

	@Override
	public boolean isMineur(PersonnePhysique personne ) {
		return context.tiersService.isMineur(personne, RegDate.get(periode, 12, 31));
	}

}

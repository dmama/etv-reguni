package ch.vd.uniregctb.evenement.party.control;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Régle A1.1: Utilisation de l'algorithme Unireg de détermination des assujettissements d'un numéro de tiers sur la PF
 */
public class ControlRuleForTiersPeriode extends ControlRuleForTiers<TypeAssujettissement> {

	private final int periode;
	private final AssujettissementService assService;

	public ControlRuleForTiersPeriode(int periode, TiersService tiersService, AssujettissementService assService) {
		super(tiersService);
		this.periode = periode;
		this.assService = assService;
	}

	@Override
	public boolean isAssujetti(@NotNull Tiers tiers) throws ControlRuleException {
		return isAssujettiSurPeriode(tiers);
	}

	@Override
	public  Set<TypeAssujettissement> getSourceAssujettissement(@NotNull Tiers tiers) throws ControlRuleException {
		final Set<TypeAssujettissement> types = EnumSet.noneOf(TypeAssujettissement.class);
		final List<Assujettissement> assujetissements = getAssujettissements(tiers);
		if (assujetissements != null) {
			for (Assujettissement a : assujetissements) {
				types.add(a.getType());
			}
		}
		return types;
	}

	private boolean isAssujettiSurPeriode(@NotNull Tiers tiers) throws ControlRuleException {
		final List<Assujettissement> assujetissements = getAssujettissements(tiers);

		//return vrai si le contribuable est assutti sur la periode
		return (assujetissements != null && !assujetissements.isEmpty());
	}

	private List<Assujettissement> getAssujettissements(Tiers tiers) throws ControlRuleException {
		try {
			return tiers instanceof Contribuable ? assService.determine((Contribuable) tiers, periode) : null;
		}
		catch (AssujettissementException e) {
			final String message = String.format("Exception lors du calcul d'assujetissement pour le tiers %d", tiers.getId());
			throw new ControlRuleException(message, e);
		}
	}

	@Override
	public boolean isMineur(PersonnePhysique personne ) {
		return tiersService.isMineur(personne, RegDate.get(periode, 12, 31));
	}
}

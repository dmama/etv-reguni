package ch.vd.uniregctb.evenement.party.control;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Régle A1.3:Déclenchée si demande porte sur date déterminante.
 * Vérification du for fiscal principal Unireg en vigueur à la date déterminante sur le numéro tiers fourni
 */
public class ControlRuleForTiersDate extends ControlRuleForTiers<ModeImposition> {

	private final RegDate date;

	public ControlRuleForTiersDate(RegDate date, TiersService tiersService) {
		super(tiersService);
		this.date = date;
	}

	@Override
	public AssujettissementStatut checkAssujettissement(@NotNull Tiers tiers, Set<ModeImposition> aRejeter) throws ControlRuleException {
		return hasForPrincipalVaudois(tiers,aRejeter);
	}

	private AssujettissementStatut hasForPrincipalVaudois(@NotNull Tiers tiers, Set<ModeImposition> aRejeter) throws ControlRuleException {
		//On se situe dans le cadre d'un contrôle assujetissement sur PP
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(date);
		final boolean isAssujetti;
		final boolean modeImpositionNonConforme;
		if (forFiscalPrincipal instanceof ForFiscalPrincipalPP) {
			modeImpositionNonConforme = isModeImpositionNonConforme(aRejeter, (ForFiscalPrincipalPP) forFiscalPrincipal);
			isAssujetti = forFiscalPrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && !modeImpositionNonConforme;
		}
		else {
			// les entreprise ne sont pas encore supportées par ce service...!
			isAssujetti = false;
			modeImpositionNonConforme = false;
		}
		return  new AssujettissementStatut(isAssujetti, modeImpositionNonConforme);
	}

	private boolean isModeImpositionNonConforme(Set<ModeImposition> aRejeter, ForFiscalPrincipalPP forFiscalPrincipalPP) {

		if (aRejeter == null || aRejeter.isEmpty() || forFiscalPrincipalPP==null) {
			return false;
		}

		return aRejeter.contains(forFiscalPrincipalPP.getModeImposition());
	}

	@Override
	public boolean isMineur(PersonnePhysique personne) {
		return tiersService.isMineur(personne, date);
	}

	@Override
	public Set<ModeImposition> getSourceAssujettissement(@NotNull Tiers tiers) {
		final Set<ModeImposition> modeImpositions = EnumSet.noneOf(ModeImposition.class);
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(date);
		if (forFiscalPrincipal instanceof ForFiscalPrincipalPP) {
			modeImpositions.add(((ForFiscalPrincipalPP) forFiscalPrincipal).getModeImposition());
		}
		return modeImpositions;
	}
}

package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Régle A1.3:Déclenchée si demande porte sur date déterminante.
 * Vérification du for fiscal principal Unireg en vigueur * à la date déterminante sur le numéro tiers fourni
 */
public class ControlRuleForTiersDate extends ControlRuleForTiers {

	private final RegDate date;
	private Set<ModeImposition> modeImpositionARejeter;

	public ControlRuleForTiersDate(RegDate date, TiersService tiersService, Set<ModeImposition> listeMode) {
		super(tiersService);
		this.date = date;
		this.modeImpositionARejeter = listeMode;
	}

	@Override
	public boolean isAssujetti(@NotNull Tiers tiers) throws ControlRuleException {
		return hasForPrincipalVaudois(tiers);
	}

	@Override
	public boolean isAssujettissementNonConforme(@NotNull Tiers tiers) throws ControlRuleException {
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(date);
		return modeImpositionARejeter!= null && forFiscalPrincipal!=null && modeImpositionARejeter.contains(forFiscalPrincipal.getModeImposition());
	}

	private boolean hasForPrincipalVaudois(@NotNull Tiers tiers) throws ControlRuleException {
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(date);
		return forFiscalPrincipal != null && forFiscalPrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Override
	public boolean isMineur(PersonnePhysique personne) {
		return tiersService.isMineur(personne, date);
	}

	@Override
	public List<ModeImposition> getSourceAssujettissement(@NotNull Tiers tiers) {
		final List<ModeImposition> modeImpositions = new ArrayList<>();
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(date);
		if (forFiscalPrincipal != null) {
			modeImpositions.add(forFiscalPrincipal.getModeImposition());
		}
		return modeImpositions;
	}
}

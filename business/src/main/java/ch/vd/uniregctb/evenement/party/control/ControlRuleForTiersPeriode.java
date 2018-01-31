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
	public AssujettissementStatut checkAssujettissement(@NotNull Contribuable ctb, Set<TypeAssujettissement> aRejeter) throws ControlRuleException {
		return assujettissementSurPeriode(ctb, aRejeter);
	}

	@Override
	public  Set<TypeAssujettissement> getSourceAssujettissement(@NotNull Contribuable tiers) throws ControlRuleException {
		final Set<TypeAssujettissement> types = EnumSet.noneOf(TypeAssujettissement.class);
		final List<Assujettissement> assujetissements = getAssujettissements(tiers);
		if (assujetissements != null) {
			for (Assujettissement a : assujetissements) {
				types.add(a.getType());
			}
		}
		return types;
	}

	private AssujettissementStatut assujettissementSurPeriode(@NotNull Contribuable tiers, Set<TypeAssujettissement> aRejeter) throws ControlRuleException {

		final Set<TypeAssujettissement> typeAssujettissementsTrouves = getSourceAssujettissement(tiers);
		AssujettissementStatut statut = null;
		final boolean aucunAssujettissement = typeAssujettissementsTrouves == null || typeAssujettissementsTrouves.isEmpty();
		final boolean pasDeTypeAssujettissementARejeter = aRejeter == null || aRejeter.isEmpty();
		if (aucunAssujettissement) {
			return new AssujettissementStatut(false,false);
		}
		//assujetissement trouvé, aucun type à rejeter

		if (pasDeTypeAssujettissementARejeter) {
			return new AssujettissementStatut(true,false);
		}

		//A ce stade on a des assujetissements et des types d'assujettissement à rejeter
		typeAssujettissementsTrouves.removeAll(aRejeter);

		final boolean isAssujetti = !typeAssujettissementsTrouves.isEmpty();
		final boolean assujettissementNonConforme = typeAssujettissementsTrouves.isEmpty();
		return new AssujettissementStatut(isAssujetti, assujettissementNonConforme);

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
	private boolean assujetissementConforme(List<Assujettissement>trouves, Set<TypeAssujettissement> aRejeter){
		if (trouves == null || trouves.isEmpty()) {
			return true;
		}
		if (aRejeter == null || aRejeter.isEmpty()) {
			return true;
		}
		for (Assujettissement trouve : trouves) {
			if (aRejeter.contains(trouve.getType())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isMineur(PersonnePhysique personne ) {
		return tiersService.isMineur(personne, RegDate.get(periode, 12, 31));
	}
}

package ch.vd.uniregctb.evenement.party.control;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Régle A1.3:Déclenchée si demande porte sur date déterminante.
 * Vérification du for fiscal principal Unireg en vigueur à la date déterminante sur le numéro tiers fourni
 * @param <T> type de valeurs collectées ({@link ch.vd.uniregctb.type.ModeImposition} ou {@link ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement})
 */
public abstract class ControlRuleForTiers<T extends Enum<T>> extends AbstractControlRule<T> {

	protected ControlRuleForTiers(TiersService tiersService) {
		super(tiersService);
	}

	@Override
	public TaxLiabilityControlResult<T> check(@NotNull Tiers tiers, Set<T> aRejeter) throws ControlRuleException {
		//S'il y a un assujettissement sur tout ou partie de la PF (au moins 1 jour) -> CTRL OK
		final TaxLiabilityControlResult<T> result;
		final AssujettissementStatut assujettissementStatut = checkAssujettissement(tiers, aRejeter);
		if (assujettissementStatut.isAssujetti) {
			final Set<T> sourceAssujettissement = getSourceAssujettissement(tiers);
			result = new TaxLiabilityControlResult<>(TaxLiabilityControlResult.Origine.INITIAL, tiers.getId(), sourceAssujettissement);
		}
		//	Dans le cas contraire (pas un seul jour d'assujettissement)-> CTRL KO
		else {
			result = new TaxLiabilityControlResult<>(createEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, null, null, null));
			result.getEchec().setAssujetissementNonConforme(assujettissementStatut.assujettissementNonConforme);
		}
		return result;
	}

	public abstract boolean isMineur(PersonnePhysique personne);

}

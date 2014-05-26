package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @param <T> type de valeurs collectées ({@link ch.vd.uniregctb.type.ModeImposition} ou {@link ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement})
 */
public abstract class ControlRuleForMenage<T extends Enum<T>> extends ControlRuleForTiersComposite<T> {

	protected ControlRuleForMenage(TiersService tiersService, ControlRuleForTiers<T> ruleForTiers) {
		super(tiersService, ruleForTiers);
	}

	@Override
	public TaxLiabilityControlResult<T> check(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult<T> result;
		final List<EnsembleTiersCouple> listeCouples = tiers instanceof PersonnePhysique ? getEnsembleTiersCouple((PersonnePhysique) tiers) : null;
		if (listeCouples != null && !listeCouples.isEmpty()) {
			//recherche des menages communs assujettis sur la période
			final List<Long> menagesCommunsAssujettis = new ArrayList<Long>();
			final List<Long> menagesCommunsNonAssujettis = new ArrayList<Long>();
			for (EnsembleTiersCouple couple : listeCouples) {
				final MenageCommun menageCommun = couple.getMenage();
				if (isAssujetti(menageCommun)) {
					menagesCommunsAssujettis.add(menageCommun.getId());
				}
				else {
					menagesCommunsNonAssujettis.add(menageCommun.getId());
				}

			}
			//Si un seul numéro de couple est assujetti >> CTRL OK (num CTB assujetti renvoyé = num CTB couple)
			if (menagesCommunsAssujettis.size() == 1) {
				final Long idTiersAssujetti = menagesCommunsAssujettis.get(0);
				final Tiers tiersCandidat = tiersService.getTiers(idTiersAssujetti);
				final Set<T> sourceAssujettissement = getSourceAssujettissement(tiersCandidat);
				result = new TaxLiabilityControlResult<>(TaxLiabilityControlResult.Origine.MENAGE_COMMUN, idTiersAssujetti, sourceAssujettissement);
			}
			//Si plusieurs numéros de couples sont assujettis >> CTRL KO
			else if (menagesCommunsAssujettis.size() > 1) {
				final TaxLiabilityControlEchec echec = createEchec(TaxLiabilityControlEchec.EchecType.PLUSIEURS_MC_ASSUJETTI_TROUVES, menagesCommunsAssujettis, null, null);
				result = new TaxLiabilityControlResult<>(echec);
			}
			//Si aucun numéro de couple est assujetti >> CTRL KO
			else {
				//Dans le cas de ménage trouvé non assujetti, on doit en renvoyer la liste
				final TaxLiabilityControlEchec echec;
				if (!menagesCommunsNonAssujettis.isEmpty()) {
					echec = createEchec(TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES, menagesCommunsNonAssujettis, null, null);
				}
				else {
					echec = createEchec(TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE, null, null, null);
				}
				result = new TaxLiabilityControlResult<>(echec);
			}
		}
		else {
			result = new TaxLiabilityControlResult<>(createEchec(TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE, null, null, null));
		}

		return result;
	}

	public abstract List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique pp);
}

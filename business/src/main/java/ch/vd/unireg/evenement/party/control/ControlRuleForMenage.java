package ch.vd.unireg.evenement.party.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

/**
 * @param <T> type de valeurs collectées ({@link ch.vd.unireg.type.ModeImposition} ou {@link ch.vd.unireg.metier.assujettissement.TypeAssujettissement})
 */
public abstract class ControlRuleForMenage<T extends Enum<T>> extends ControlRuleForTiersComposite<T> {

	protected ControlRuleForMenage(TiersService tiersService, ControlRuleForTiers<T> ruleForTiers) {
		super(tiersService, ruleForTiers);
	}

	@Override
	public TaxLiabilityControlResult<T> check(@NotNull Contribuable ctb, Set<T> aRejeter) throws ControlRuleException {
		final TaxLiabilityControlResult<T> result;
		final List<EnsembleTiersCouple> listeCouples = ctb instanceof PersonnePhysique ? getEnsembleTiersCouple((PersonnePhysique) ctb) : null;
		if (listeCouples != null && !listeCouples.isEmpty()) {
			//recherche des menages communs assujettis sur la période
			final List<Long> menagesCommunsAssujettis = new ArrayList<>();
			final List<Long> menagesCommunsNonAssujettis = new ArrayList<>();
			boolean assujettissementNonConformeTrouve = false;
			for (EnsembleTiersCouple couple : listeCouples) {
				final MenageCommun menageCommun = couple.getMenage();
				final AssujettissementStatut assujettissementStatut = checkAssujettissement(menageCommun, aRejeter);
				if (assujettissementStatut.isAssujetti) {
					menagesCommunsAssujettis.add(menageCommun.getId());
				}
				else {
					menagesCommunsNonAssujettis.add(menageCommun.getId());
					if (assujettissementStatut.assujettissementNonConforme) {
						assujettissementNonConformeTrouve = true;
					}
				}

			}
			//Si un seul numéro de couple est assujetti >> CTRL OK (num CTB assujetti renvoyé = num CTB couple)
			if (menagesCommunsAssujettis.size() == 1) {
				final Long idTiersAssujetti = menagesCommunsAssujettis.get(0);
				final MenageCommun tiersCandidat = (MenageCommun) tiersService.getTiers(idTiersAssujetti);
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
					echec.setAssujetissementNonConforme(assujettissementNonConformeTrouve);
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

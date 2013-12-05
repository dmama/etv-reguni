package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ControlRuleForMenage extends AbstractControlRule {

	protected ControlRuleForMenage(TiersService tiersService) {
		super(tiersService);
	}

	@Override
	public TaxLiabilityControlResult check(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult result = new TaxLiabilityControlResult();
		final List<EnsembleTiersCouple> listeCouples = tiers instanceof PersonnePhysique ? getEnsembleTiersCouple((PersonnePhysique) tiers) : null;
		if (listeCouples != null && !listeCouples.isEmpty()) {
			//recherche des menages communs assujettis sur la période
			final List<Long> menageCommunsAssujettis = new ArrayList<Long>();

			final List<Long> menageCommunsNonAssujettis = new ArrayList<Long>();
			for (EnsembleTiersCouple couple : listeCouples) {
				final MenageCommun menageCommun = couple.getMenage();
				if (isAssujetti(menageCommun)) {
					menageCommunsAssujettis.add(menageCommun.getId());
				}
				else {
					menageCommunsNonAssujettis.add(menageCommun.getId());
				}

			}
			//Si un seul numéro de couple est assujetti >> CTRL OK (num CTB assujetti renvoyé = num CTB couple)
			if (menageCommunsAssujettis.size() == 1) {
				result.setIdTiersAssujetti(menageCommunsAssujettis.get(0));
			}
			//Si plusieurs numéros de couples sont assujettis >> CTRL KO
			else if (menageCommunsAssujettis.size() > 1) {
				final TaxLiabilityControlEchec echec = new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.PLUSIEURS_MC_ASSUJETTI_TROUVES);
				echec.setMenageCommunIds(menageCommunsAssujettis);
				result.setEchec(echec);

			}
			//Si aucun numéro de couple est assujetti >> CTRL KO
			else if (menageCommunsAssujettis.isEmpty()) {
				//Dans le cas de ménage trouvé non assujetti, on doit en renvoyer la liste
				if (!menageCommunsNonAssujettis.isEmpty()) {
					setErreur(result, TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES,menageCommunsNonAssujettis,null,null);
				}
				else {
					setErreur(result, TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE, null, null, null);
				}
			}
		}
		else {
			setErreur(result, TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE, null, null, null);
		}

		return result;
	}

	public abstract List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique pp);
}

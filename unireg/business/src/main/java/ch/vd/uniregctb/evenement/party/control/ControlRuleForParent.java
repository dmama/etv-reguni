package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @param <T> type de valeurs collectées ({@link ch.vd.uniregctb.type.ModeImposition} ou {@link ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement})
 */
public abstract class ControlRuleForParent<T extends Enum<T>> extends ControlRuleForTiersComposite<T> {

	protected final ControlRuleForMenage<T> ruleForMenage;

	protected ControlRuleForParent(TiersService tiersService, ControlRuleForTiers<T> ruleForTiers, ControlRuleForMenage<T> ruleForMenage) {
		super(tiersService, ruleForTiers);
		this.ruleForMenage = ruleForMenage;
	}

	@Override
	public TaxLiabilityControlResult<T> check(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult<T> result;

		//Recherche des parents:
		final List<Parente> parentes = tiers instanceof PersonnePhysique ? extractParents(tiersService.getParents((PersonnePhysique) tiers, false)) : Collections.<Parente>emptyList();
		if (parentes.isEmpty()) {
			result = new TaxLiabilityControlResult<>(createEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null));
		}
		else if (parentes.size() == 1) {
			final Parente parente = parentes.get(0);
			final Long parentId = parente.getObjetId();
			final PersonnePhysique parent = (PersonnePhysique) tiersService.getTiers(parentId);
			if (parent != null) {
				// le parent est assujetti tout va bien
				if (isAssujetti(parent)) {
					final Set<T> sourceAssujettissement = getSourceAssujettissement(parent);
					result = new TaxLiabilityControlResult<>(TaxLiabilityControlResult.Origine.PARENT, parentId, sourceAssujettissement);
				}
				else {
					final TaxLiabilityControlResult<T> resultMenagesParent = rechercheAssujettisementSurMenage(parent);
					final Long idTiersAssujetti = resultMenagesParent.getIdTiersAssujetti();
					if (idTiersAssujetti != null) {
						result = new TaxLiabilityControlResult<>(TaxLiabilityControlResult.Origine.MENAGE_COMMUN_PARENT, resultMenagesParent);
					}
					else {
						//on est dans un echec du controle d'assujetissement sur le ménage du parent
						final List<Long> ParentmenageCommunIds = resultMenagesParent.getEchec().getMenageCommunIds();
						result = new TaxLiabilityControlResult<>(createEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, ParentmenageCommunIds, Arrays.asList(parentId)));
					}
				}
			}
			else {
				result = new TaxLiabilityControlResult<>(createEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null));
			}
		}
		else if (parentes.size() == 2) {
			result = checkAppartenanceMenageAssujetti(parentes);
		}
		else {
			final List<Long> parentsIds = new ArrayList<>(parentes.size());
			for (Parente parenteParent : parentes) {
				parentsIds.add(parenteParent.getObjetId());
			}
			result = new TaxLiabilityControlResult<>(createEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, parentsIds));
		}
		return result;
	}

	public TaxLiabilityControlResult<T> checkAppartenanceMenageAssujetti(List<Parente> parenteParents) throws ControlRuleException {
		final Parente rapportParent1 = parenteParents.get(0);
		final PersonnePhysique parent1 = (PersonnePhysique) tiersService.getTiers(rapportParent1.getObjetId());

		final Parente rapportParent2 = parenteParents.get(1);
		final PersonnePhysique parent2 = (PersonnePhysique) tiersService.getTiers(rapportParent2.getObjetId());

		final Long parent1Id = parent1.getId();
		final TaxLiabilityControlResult<T> resultMenageParent1 = rechercheAssujettisementSurMenage(parent1);

		final Long parent2Id = parent2.getId();
		final TaxLiabilityControlResult<T> resultMenageParent2 = rechercheAssujettisementSurMenage(parent2);

		final Long idMenageAssujettiParent1 = resultMenageParent1.getIdTiersAssujetti();
		final Long idMenageAssujettiParent2 = resultMenageParent2.getIdTiersAssujetti();

		final TaxLiabilityControlResult<T> result;
		if (idMenageAssujettiParent1 != null && idMenageAssujettiParent2 != null && idMenageAssujettiParent1.longValue() == idMenageAssujettiParent2.longValue()) {
			final Tiers menage = tiersService.getTiers(idMenageAssujettiParent1);
			final Set<T> sourceAssujettissement = getSourceAssujettissement(menage);
			result = new TaxLiabilityControlResult<>(TaxLiabilityControlResult.Origine.MENAGE_COMMUN_PARENT, idMenageAssujettiParent1, sourceAssujettissement);
		}
		else {
			//Liste des parents
			final List<Long> parentsIds = new ArrayList<>();
			parentsIds.add(parent1Id);
			parentsIds.add(parent2Id);

			//Liste des ménages communs des parents
			final List<Long> listeMenage = new ArrayList<>();
			if (idMenageAssujettiParent1 != null) {
				listeMenage.add(idMenageAssujettiParent1);
			}

			if (idMenageAssujettiParent2 != null) {
				listeMenage.add(idMenageAssujettiParent2);
			}

			//On recupere les menages communs trouvés Pour les parents en cas d'erreur
			final TaxLiabilityControlEchec echecParent1 = resultMenageParent1.getEchec();
			if (echecParent1 != null && echecParent1.getMenageCommunIds() != null) {
				listeMenage.addAll(echecParent1.getMenageCommunIds());
			}

			final TaxLiabilityControlEchec echecParent2 = resultMenageParent2.getEchec();
			if (echecParent2 != null &&  echecParent2.getMenageCommunIds() != null) {
				listeMenage.addAll(echecParent2.getMenageCommunIds());
			}

			final List<Long> menageCommunParentsIds = listeMenage.isEmpty() ? null : listeMenage;
			result = new TaxLiabilityControlResult<>(createEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, menageCommunParentsIds, parentsIds));
		}

		return result;
	}

	protected abstract List<Parente> extractParents(List<Parente> parentes);

	private TaxLiabilityControlResult<T> rechercheAssujettisementSurMenage(@NotNull PersonnePhysique parent) throws ControlRuleException{
		return ruleForMenage.check(parent);
	}
}

package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ControlRuleForParent extends AbstractControlRule {

	protected ControlRuleForParent(TiersService tiersService) {
		super(tiersService);
	}

	@Override
	public TaxLiabilityControlResult check(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult result = new TaxLiabilityControlResult();

		//Recherche des parents:
		final List<RapportFiliation> filiations = tiers instanceof PersonnePhysique ? tiersService.getRapportsFiliation((PersonnePhysique) tiers) : Collections.<RapportFiliation>emptyList();
		final List<RapportFiliation> filiationParents = extractParents(filiations);
		if (filiationParents.isEmpty()) {
			setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null);
		}
		else if (filiationParents.size() == 1) {
			final RapportFiliation rapportParent = filiationParents.get(0);
			final PersonnePhysique parent = rapportParent.getAutrePersonnePhysique();
			if (parent != null) {

				final Long parentId = parent.getId();

				//le parent est assujetti tout va bien
				if (isAssujetti(parent)) {
					result.setIdTiersAssujetti(parentId);
				}
				else {
					final TaxLiabilityControlResult result1ForMenageParent = rechercheAssujettisementSurMenage(parent);
					if (result1ForMenageParent.getIdTiersAssujetti() != null) {
						result.setIdTiersAssujetti(result1ForMenageParent.getIdTiersAssujetti());
					}
					else{
						//on est dans un echec du controle d'assujetissement sur le ménage du parent
						final List<Long> ParentmenageCommunIds = result1ForMenageParent.getEchec().getMenageCommunIds();
						setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, ParentmenageCommunIds, Arrays.asList(parentId));
					}
				}
			}
			else {
				setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null);
			}
		}
		else if (filiationParents.size() == 2) {
			checkAppartenanceMenageAssujetti(filiationParents, result);
		}
		else if (filiationParents.size() > 2) {
			final List<Long> parentsIds = new ArrayList<Long>(filiationParents.size());
			for (RapportFiliation filiationParent : filiationParents) {
				parentsIds.add(filiationParent.getAutrePersonnePhysique().getId());
			}
			setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, parentsIds);
		}
		return result;
	}

	public void checkAppartenanceMenageAssujetti(List<RapportFiliation> filiationParents, TaxLiabilityControlResult result) throws ControlRuleException {
		final RapportFiliation rapportParent1 = filiationParents.get(0);
		final PersonnePhysique parent1 = rapportParent1.getAutrePersonnePhysique();

		final RapportFiliation rapportParent2 = filiationParents.get(1);
		final PersonnePhysique parent2 = rapportParent2.getAutrePersonnePhysique();

		final Long parent1Id = parent1.getId();
		final TaxLiabilityControlResult resultMenageParent1 = rechercheAssujettisementSurMenage(parent1);

		final Long parent2Id = parent2.getId();
		final TaxLiabilityControlResult resultMenageParent2 = rechercheAssujettisementSurMenage(parent2);

		final Long idMenageAssujettiParent1 = resultMenageParent1.getIdTiersAssujetti();
		final Long idMenageAssujettiParent2 = resultMenageParent2.getIdTiersAssujetti();

		if (idMenageAssujettiParent1 != null && idMenageAssujettiParent2 != null && idMenageAssujettiParent1.longValue() == idMenageAssujettiParent2.longValue()) {
			result.setIdTiersAssujetti(idMenageAssujettiParent1);
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
			if (resultMenageParent1.getEchec() != null && resultMenageParent1.getEchec().getMenageCommunIds() != null) {
				listeMenage.addAll(resultMenageParent1.getEchec().getMenageCommunIds());
			}

			if (resultMenageParent2.getEchec() != null &&  resultMenageParent2.getEchec().getMenageCommunIds() != null) {
				listeMenage.addAll(resultMenageParent2.getEchec().getMenageCommunIds());
			}

			final List<Long> menageCommunParentsIds = listeMenage.isEmpty() ? null : listeMenage;
			setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, menageCommunParentsIds, parentsIds);
		}
	}

	protected abstract List<RapportFiliation> extractParents(List<RapportFiliation> filiations);

	protected abstract TaxLiabilityControlResult rechercheAssujettisementSurMenage(@NotNull PersonnePhysique parent) throws ControlRuleException;

}

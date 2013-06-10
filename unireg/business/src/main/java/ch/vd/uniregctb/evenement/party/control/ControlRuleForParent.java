package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.evenement.party.TaxliabilityControlEchecType;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.xml.Context;

public abstract class ControlRuleForParent extends AbstractControlRule {
	public ControlRuleForParent(Context contex, Long tiersId) {
		super(contex, tiersId);
	}

	@Override
	public TaxliabilityControlResult check() throws ControlRuleException {
		TaxliabilityControlResult result = new TaxliabilityControlResult();

		//Recherche des parents:
		PersonnePhysique personne = (PersonnePhysique) context.tiersDAO.get(tiersId);
		if (personne == null) {
			final String message = String.format("Le tiers %d n'existe pas", tiersId);
			throw new ControlRuleException(message);
		}
		List<RapportFiliation> filiations = context.tiersService.getRapportsFiliation(personne);
		List<RapportFiliation> filiationParents = extractParents(filiations);
		if (filiationParents.isEmpty()) {
			setErreur(result, TaxliabilityControlEchecType.CONTROLE_SUR_PARENTS_KO, null, null, null);
		}
		else if (filiationParents.size() == 1) {
			final RapportFiliation rapportParent = filiationParents.get(0);
			PersonnePhysique parent = rapportParent.getAutrePersonnePhysique();
			if (parent != null) {

				final List<Long> parentsIds = new ArrayList<Long>();
				parentsIds.add(parent.getId());

				//le parent est assujetti tout va bien
				if (isAssujetti(parent.getId())) {
					result.setIdTiersAssujetti(parent.getId());
				}
				else {

					TaxliabilityControlResult result1ForMenageParent = rechercheAssujettisementSurMenage(parent.getId());
					if (result1ForMenageParent.getIdTiersAssujetti() != null) {
						result.setIdTiersAssujetti(result1ForMenageParent.getIdTiersAssujetti());
					}
					else{
						//on est dans un echec du controle d'assujetissement sur le ménage du parent
						final List<Long> ParentmenageCommunIds = result1ForMenageParent.getEchec().getMenageCommunIds();

						setErreur(result,TaxliabilityControlEchecType.CONTROLE_SUR_PARENTS_KO,null, ParentmenageCommunIds,parentsIds);
					}

				}

			}
			else {
				setErreur(result, TaxliabilityControlEchecType.CONTROLE_SUR_PARENTS_KO, null, null, null);
			}

		}
		else if (filiationParents.size() == 2) {

			checkAppartenanceMenageAssujetti(filiationParents, result);

		}
		else if (filiationParents.size() > 2) {
			final List<Long> parentsIds = new ArrayList<Long>();
			for (RapportFiliation filiationParent : filiationParents) {
				parentsIds.add(filiationParent.getAutrePersonnePhysique().getId());
			}
			setErreur(result, TaxliabilityControlEchecType.CONTROLE_SUR_PARENTS_KO, null, null, parentsIds);

		}
		return result;
	}


	public void checkAppartenanceMenageAssujetti(List<RapportFiliation> filiationParents, TaxliabilityControlResult result) throws ControlRuleException {
		final RapportFiliation rapportParent1 = filiationParents.get(0);
		PersonnePhysique parent1 = rapportParent1.getAutrePersonnePhysique();

		final RapportFiliation rapportParent2 = filiationParents.get(1);
		PersonnePhysique parent2 = rapportParent2.getAutrePersonnePhysique();

		final Long parent1Id = parent1.getId();
		TaxliabilityControlResult resultMenageParent1 = rechercheAssujettisementSurMenage(parent1Id);

		final Long parent2Id = parent2.getId();
		TaxliabilityControlResult resultMenageParent2 = rechercheAssujettisementSurMenage(parent2Id);

		final Long idMenageAssujettiParent1 = resultMenageParent1.getIdTiersAssujetti();
		final Long idMenageAssujettiParent2 = resultMenageParent2.getIdTiersAssujetti();


		final boolean menagesParentExiste = (idMenageAssujettiParent1!=null && idMenageAssujettiParent2!=null);
		final boolean memeMenageParents = (idMenageAssujettiParent1 == idMenageAssujettiParent2);
		if (menagesParentExiste && memeMenageParents) {
			result.setIdTiersAssujetti(idMenageAssujettiParent1);
		}
		else {
			//Liste des parents
			final List<Long> parentsIds = new ArrayList<Long>();
			parentsIds.add(parent1Id);
			parentsIds.add(parent2Id);

			//Liste des ménages communs des parents
			 List<Long> menageCommunParentsIds =null;
			List<Long> listeMenage =  new ArrayList<Long>();
			if (idMenageAssujettiParent1 != null) {
				listeMenage.add(idMenageAssujettiParent1);
			}

			if (idMenageAssujettiParent2 != null) {
				listeMenage.add(idMenageAssujettiParent2);
			}

			//On recupere les menages communs trouvés Pour les parents en cas d'erreur
			if (resultMenageParent1.getEchec() != null && resultMenageParent1.getEchec().getMenageCommunIds()!=null) {
				listeMenage.addAll(resultMenageParent1.getEchec().getMenageCommunIds());
			}

			if (resultMenageParent2.getEchec() != null &&  resultMenageParent2.getEchec().getMenageCommunIds()!=null) {
				listeMenage.addAll(resultMenageParent2.getEchec().getMenageCommunIds());
			}

			if (!listeMenage.isEmpty()) {
				menageCommunParentsIds = listeMenage;
			}


			setErreur(result, TaxliabilityControlEchecType.CONTROLE_SUR_PARENTS_KO, null, menageCommunParentsIds, parentsIds);
		}
	}

	public abstract List<RapportFiliation> extractParents(List<RapportFiliation> filiations);

	public abstract TaxliabilityControlResult rechercheAssujettisementSurMenageParent(Long tiersId) throws ControlRuleException;

	public abstract TaxliabilityControlResult rechercheAssujettisementSurMenage(Long parentId) throws ControlRuleException;

	protected abstract boolean isEnMenage(PersonnePhysique parent);


}

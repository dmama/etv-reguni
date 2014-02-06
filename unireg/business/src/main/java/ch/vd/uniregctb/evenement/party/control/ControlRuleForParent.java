package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ControlRuleForParent extends ControleRuleForTiersComposite {

	protected final ControlRuleForMenage ruleForMenage;

	protected ControlRuleForParent(TiersService tiersService,ControlRuleForTiers ruleForTiers,ControlRuleForMenage ruleForMenage) {
		super(tiersService, ruleForTiers);
		this.ruleForMenage = ruleForMenage;

	}


	@Override
	public TaxLiabilityControlResult check(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult result = new TaxLiabilityControlResult();

		//Recherche des parents:
		final List<Parente> parentes = tiers instanceof PersonnePhysique ? extractParents(tiersService.getParents((PersonnePhysique) tiers, false)) : Collections.<Parente>emptyList();
		if (parentes.isEmpty()) {
			setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null,false);
		}
		else if (parentes.size() == 1) {
			final Parente parente = parentes.get(0);
			final Long parentId = parente.getObjetId();
			final PersonnePhysique parent = (PersonnePhysique) tiersService.getTiers(parentId);
			if (parent != null) {
				// le parent est assujetti tout va bien
				if (isAssujetti(parent)) {
					final boolean nonConforme = isAssujettissementNonConforme(parent);
					if (nonConforme) {
						setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null,nonConforme);
					}else{
						result.setIdTiersAssujetti(parentId);
					}

				}
				else {
					final TaxLiabilityControlResult result1ForMenageParent = rechercheAssujettisementSurMenage(parent);
					final Long idTiersAssujetti = result1ForMenageParent.getIdTiersAssujetti();
					if (idTiersAssujetti != null) {
						Tiers menage = tiersService.getTiers(idTiersAssujetti);
						final boolean nonConforme = isAssujettissementNonConforme(menage);
						if (nonConforme) {
							setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null,nonConforme);
						}else{
							result.setIdTiersAssujetti(idTiersAssujetti);
						}

					}
					else{
						//Si on a un echec à cause d'un assujetissement non conforme, on renvoie l'erreur
						if (result1ForMenageParent.getEchec().isAssujetissementNonConforme()) {
							setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, Arrays.asList(parentId),true);
						}
						else{
							//on est dans un echec du controle d'assujetissement sur le ménage du parent
							final List<Long> ParentmenageCommunIds = result1ForMenageParent.getEchec().getMenageCommunIds();
							setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, ParentmenageCommunIds, Arrays.asList(parentId),false);
						}

					}
				}
			}
			else {
				setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null,false);
			}
		}
		else if (parentes.size() == 2) {
			checkAppartenanceMenageAssujetti(parentes, result);
		}
		else if (parentes.size() > 2) {
			final List<Long> parentsIds = new ArrayList<>(parentes.size());
			for (Parente parenteParent : parentes) {
				parentsIds.add(parenteParent.getObjetId());
			}
			setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, parentsIds,false);
		}
		return result;
	}

	public void checkAppartenanceMenageAssujetti(List<Parente> parenteParents, TaxLiabilityControlResult result) throws ControlRuleException {
		final Parente rapportParent1 = parenteParents.get(0);
		final PersonnePhysique parent1 = (PersonnePhysique) tiersService.getTiers(rapportParent1.getObjetId());

		final Parente rapportParent2 = parenteParents.get(1);
		final PersonnePhysique parent2 = (PersonnePhysique) tiersService.getTiers(rapportParent2.getObjetId());

		final Long parent1Id = parent1.getId();
		final TaxLiabilityControlResult resultMenageParent1 = rechercheAssujettisementSurMenage(parent1);

		final Long parent2Id = parent2.getId();
		final TaxLiabilityControlResult resultMenageParent2 = rechercheAssujettisementSurMenage(parent2);

		final Long idMenageAssujettiParent1 = resultMenageParent1.getIdTiersAssujetti();
		final Long idMenageAssujettiParent2 = resultMenageParent2.getIdTiersAssujetti();

		if (idMenageAssujettiParent1 != null && idMenageAssujettiParent2 != null && idMenageAssujettiParent1.longValue() == idMenageAssujettiParent2.longValue()) {
			Tiers menage = tiersService.getTiers(idMenageAssujettiParent1);
			final boolean nonConforme = isAssujettissementNonConforme(menage);
			if (nonConforme) {
				setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, null, null,nonConforme);
			}else{
				result.setIdTiersAssujetti(idMenageAssujettiParent1);
			}
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
			final boolean assujetissementNonConforme1 = echecParent1 != null && echecParent1.isAssujetissementNonConforme();
			final boolean assujetissementNonConforme2 = echecParent2 !=null && echecParent2.isAssujetissementNonConforme();
			final boolean nonConforme = assujetissementNonConforme1 ||	assujetissementNonConforme2;
			setErreur(result, TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, null, menageCommunParentsIds, parentsIds,nonConforme);
		}
	}

	protected abstract List<Parente> extractParents(List<Parente> parentes);

	private TaxLiabilityControlResult rechercheAssujettisementSurMenage(@NotNull PersonnePhysique parent) throws ControlRuleException{
		return ruleForMenage.check(parent);
	};

}

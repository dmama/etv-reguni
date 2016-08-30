package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;

public class TypeEtatDecisionTaxationUserType extends EnumIntegerMappingUserType<RegpmTypeEtatDecisionTaxation> {

	private static final Map<Integer, RegpmTypeEtatDecisionTaxation> MAPPING = buildMapping();

	private static Map<Integer, RegpmTypeEtatDecisionTaxation> buildMapping() {
		final Map<Integer, RegpmTypeEtatDecisionTaxation> map = new HashMap<>();
		map.put(1, RegpmTypeEtatDecisionTaxation.NON_NOTIFIEE);
		map.put(2, RegpmTypeEtatDecisionTaxation.NOTIFIEE);
		map.put(3, RegpmTypeEtatDecisionTaxation.ENTREE_EN_FORCE);
		map.put(4, RegpmTypeEtatDecisionTaxation.A_REVISER);
		map.put(5, RegpmTypeEtatDecisionTaxation.EN_RECLAMATION);
		map.put(6, RegpmTypeEtatDecisionTaxation.ERREUR_DE_CALCUL);
		map.put(7, RegpmTypeEtatDecisionTaxation.ERREUR_DE_TRANSCRIPTION);
		map.put(8, RegpmTypeEtatDecisionTaxation.ARTICLE_98_120);
		map.put(9, RegpmTypeEtatDecisionTaxation.ANNULEE);
		map.put(10, RegpmTypeEtatDecisionTaxation.EN_SOUSTRACTION);
		return map;
	}

	public TypeEtatDecisionTaxationUserType() {
		super(RegpmTypeEtatDecisionTaxation.class, MAPPING);
	}

}

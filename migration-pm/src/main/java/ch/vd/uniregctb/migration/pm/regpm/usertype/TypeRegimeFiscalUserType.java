package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;

public class TypeRegimeFiscalUserType extends EnumCharMappingUserType<RegpmTypeRegimeFiscal> {

	private static final Map<String, RegpmTypeRegimeFiscal> MAPPING = buildMapping();

	private static Map<String, RegpmTypeRegimeFiscal> buildMapping() {
		final Map<String, RegpmTypeRegimeFiscal> map = new HashMap<>();
		map.put("01", RegpmTypeRegimeFiscal._01_ORDINAIRE);
		map.put("109", RegpmTypeRegimeFiscal._109_PM_AVEC_EXONERATION_ART_90G);
		map.put("11", RegpmTypeRegimeFiscal._11_PARTICIPATIONS_HOLDING);
		map.put("12", RegpmTypeRegimeFiscal._12_PARTICIPATIONS_PART_IMPOSABLE);
		map.put("190", RegpmTypeRegimeFiscal._190_PM_AVEC_EXONERATION_ART_90CEFH);
		map.put("20", RegpmTypeRegimeFiscal._20_SOCIETE_DE_SERVICES);
		map.put("31", RegpmTypeRegimeFiscal._31_SOCIETE_ORDINAIRE);
		map.put("32", RegpmTypeRegimeFiscal._32_SOCIETE_ORDINAIRE_SUBVENTION);
		map.put("33", RegpmTypeRegimeFiscal._33_SOCIETE_ORDINAIRE_CARACTERE_SOCIAL);
		map.put("35", RegpmTypeRegimeFiscal._35_SOCIETE_ORDINAIRE_SIAL);
		map.put("40", RegpmTypeRegimeFiscal._40_SOCIETE_DE_BASE);
		map.put("41", RegpmTypeRegimeFiscal._41_ORDINAIRE_ICC_BASE_MIXTE);
		map.put("41C", RegpmTypeRegimeFiscal._41C_SOCIETE_DE_BASE_MIXTE);
		map.put("42", RegpmTypeRegimeFiscal._42_ORDINAIRE_ICC_BASE_DOMICILE);
		map.put("42C", RegpmTypeRegimeFiscal._42C_SOCIETE_DE_DOMICILE);
		map.put("50", RegpmTypeRegimeFiscal._50_PLACEMENT_COLLECTIF_IMMEUBLE);
		map.put("60", RegpmTypeRegimeFiscal._60_TRANSPORTS_CONCESSIONNES);
		map.put("609", RegpmTypeRegimeFiscal._609_TRANSPORTS_CONCESSIONNES_EXONERES);
		map.put("70", RegpmTypeRegimeFiscal._70_ORDINAIRE_ASSOCIATION_FONDATION);
		map.put("701", RegpmTypeRegimeFiscal._701_APM_IMPORTANTES);
		map.put("7020", RegpmTypeRegimeFiscal._7020_SERVICES_ASSOCIATION_FONDATION);
		map.put("7032", RegpmTypeRegimeFiscal._7032_APM_SI_SUBVENTIONNEE);
		map.put("709", RegpmTypeRegimeFiscal._709_PURE_UTILITE_PUBLIQUE);
		map.put("71", RegpmTypeRegimeFiscal._71_FONDATION_ECCLESIASTIQUE);
		map.put("715", RegpmTypeRegimeFiscal._715_FONDATION_ECCLESIASTIQUE_ART_90D);
		map.put("719", RegpmTypeRegimeFiscal._719_BUTS_CULTUELS_ART_90H);
		map.put("72", RegpmTypeRegimeFiscal._72_FONDATION_PREVOYANCE);
		map.put("729", RegpmTypeRegimeFiscal._729_INSTITUTIONS_DE_PREVOYANCE_ART_90I);
		map.put("739", RegpmTypeRegimeFiscal._739_CAISSES_ASSURANCES_SOCIALES_ART_90F);
		map.put("749", RegpmTypeRegimeFiscal._749_CONFEDERATION_ETAT_ETRANGER_ART_90AI);
		map.put("759", RegpmTypeRegimeFiscal._759_CANTON_ETABLISSEMENT_ART_90B);
		map.put("769", RegpmTypeRegimeFiscal._769_COMMUNE_ETABLISSEMENT_ART_90C);
		map.put("779", RegpmTypeRegimeFiscal._779_PLACEMENT_COLLECTIF_EXONERE_ART_90J);
		return map;
	}

	public TypeRegimeFiscalUserType() {
		super(RegpmTypeRegimeFiscal.class, MAPPING);
	}
}

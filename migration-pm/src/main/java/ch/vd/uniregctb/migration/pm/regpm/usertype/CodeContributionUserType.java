package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeContribution;

public class CodeContributionUserType extends EnumCharMappingUserType<RegpmCodeContribution> {

	private static final Map<String, RegpmCodeContribution> MAPPING = buildMapping();

	private static Map<String, RegpmCodeContribution> buildMapping() {
		final Map<String, RegpmCodeContribution> map = new HashMap<>();
		map.put("BENCH", RegpmCodeContribution.BENEFICE);
		map.put("BENVD", RegpmCodeContribution.BENEFICE);
		map.put("BENCO", RegpmCodeContribution.BENEFICE);
		map.put("CAPCH", RegpmCodeContribution.CAPITAL);
		map.put("CAPVD", RegpmCodeContribution.CAPITAL);
		map.put("CAPCO", RegpmCodeContribution.CAPITAL);
		map.put("REVCH", RegpmCodeContribution.REVENU_PRENUMERANDO);
		map.put("REVVD", RegpmCodeContribution.REVENU_PRENUMERANDO);
		map.put("REVCO", RegpmCodeContribution.REVENU_PRENUMERANDO);
		map.put("BNXCH", RegpmCodeContribution.BENEFICE_EXTRAORDINAIRE);
		map.put("BNXVD", RegpmCodeContribution.BENEFICE_EXTRAORDINAIRE);
		map.put("BNXCO", RegpmCodeContribution.BENEFICE_EXTRAORDINAIRE);
		map.put("IBCCH", RegpmCodeContribution.IMPOT_BENEFICE_CAPITAL);
		map.put("IBCVD", RegpmCodeContribution.IMPOT_BENEFICE_CAPITAL);
		map.put("IBCCO", RegpmCodeContribution.IMPOT_BENEFICE_CAPITAL);
		map.put("IMRVD", RegpmCodeContribution.IMPOT_MINIMUM_RECETTES_BRUTES);
		map.put("IMRCO", RegpmCodeContribution.IMPOT_MINIMUM_RECETTES_BRUTES);
		map.put("IMCVD", RegpmCodeContribution.IMPOT_MINIMUM_CAPITAUX_INVESTIS);
		map.put("IMCCO", RegpmCodeContribution.IMPOT_MINIMUM_CAPITAUX_INVESTIS);
		map.put("ICIVD", RegpmCodeContribution.IMPOT_COMPLEMENTAIRE_IMMEUBLES);
		map.put("ICICO", RegpmCodeContribution.IMPOT_COMPLEMENTAIRE_IMMEUBLES);
		map.put("ADDCH", RegpmCodeContribution.AMENDE_DEFAUT_DOSSIER);
		map.put("ADDVD", RegpmCodeContribution.AMENDE_DEFAUT_DOSSIER);
		map.put("ADPCH", RegpmCodeContribution.AMENDE_DEFAUT_PIECE);
		map.put("ADPVD", RegpmCodeContribution.AMENDE_DEFAUT_PIECE);
		map.put("ASSCH", RegpmCodeContribution.AMENDE_SOUSTRACTION);
		map.put("ASSVD", RegpmCodeContribution.AMENDE_SOUSTRACTION);
		map.put("ASSCO", RegpmCodeContribution.AMENDE_SOUSTRACTION);
		map.put("FORCH", RegpmCodeContribution.IMPOT_FORTUNE);
		map.put("FORVD", RegpmCodeContribution.IMPOT_FORTUNE);
		map.put("FORCO", RegpmCodeContribution.IMPOT_FORTUNE);
		map.put("DECOM", RegpmCodeContribution.DROIT_ENTIER);
		map.put("DECAN", RegpmCodeContribution.DROIT_ENTIER);
		map.put("DDCOM", RegpmCodeContribution.DEMI_DROIT);
		map.put("DDCAN", RegpmCodeContribution.DEMI_DROIT);
		map.put("CCFEU", RegpmCodeContribution.INCENDIE_BATIMENT);
		map.put("CCEPU", RegpmCodeContribution.TAXE_EPURATION_EAUX);
		map.put("CCEGU", RegpmCodeContribution.TAXE_EGOUT);
		map.put("CCORD", RegpmCodeContribution.TAXE_ORDURES);
		map.put("CCFON", RegpmCodeContribution.IMPOT_FONCIER_SOL_AUTRUI);
		map.put("CCEAU", RegpmCodeContribution.FOURNITURE_EAU);
		map.put("IFONC", RegpmCodeContribution.IMPOT_FONCIER);
		map.put("GICAN", RegpmCodeContribution.GAINS_IMMOBILIERS);
		map.put("GICOM", RegpmCodeContribution.GAINS_IMMOBILIERS);
		map.put("PADCH", RegpmCodeContribution.PERCU_APRES_DEFALCATION);
		map.put("PADVD", RegpmCodeContribution.PERCU_APRES_DEFALCATION);
		map.put("PADCO", RegpmCodeContribution.PERCU_APRES_DEFALCATION);
		map.put("APSCH", RegpmCodeContribution.AMENDE_PARTICIPATION_SOUSTRACTION);
		map.put("APSVD", RegpmCodeContribution.AMENDE_PARTICIPATION_SOUSTRACTION);
		map.put("APSCO", RegpmCodeContribution.AMENDE_PARTICIPATION_SOUSTRACTION);
		map.put("RBIVD", RegpmCodeContribution.REMBOURSEMENT_IMPOT_COMPLEMENTAIRE_IMMEUBLES);
		map.put("RBICO", RegpmCodeContribution.REMBOURSEMENT_IMPOT_COMPLEMENTAIRE_IMMEUBLES);
		return map;
	}

	public CodeContributionUserType() {
		super(RegpmCodeContribution.class, MAPPING);
	}
}

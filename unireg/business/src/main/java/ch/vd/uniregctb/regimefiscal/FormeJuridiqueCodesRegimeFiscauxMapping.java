package ch.vd.uniregctb.regimefiscal;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * TODO: Implémenter cette table dans la configuration d'Unireg
 * @author Raphaël Marmier, 2017-01-25, <raphael.marmier@vd.ch>
 */
class FormeJuridiqueCodesRegimeFiscauxMapping {

	@NotNull
	static String getDefaultCodePourFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {

		if (formeJuridique == null) {
			return "00";
		}

		switch (formeJuridique) {

		case EI:                return "00";

		case SNC:               //     "80"
		case SC:                return "80";

		case SCA:               //     "01"
		case SA:                //     "01"
		case SARL:              //     "01"
		case SCOOP:             return "01";

		case ASSOCIATION:       //     "70"
		case FONDATION:         return "70";

		case FILIALE_HS_RC:     //     "00"
		case PARTICULIER:       //     "00"
		case SCPC:              //     "00"
		case SICAV:             //     "00"
		case SICAF:             //     "00"
		case IDP:               //     "00"
		case PNC:               //     "00"
		case INDIVISION:        return "00";

		case FILIALE_CH_RC:     return "01";

		case ADM_CH:            //     "00"
		case ADM_CT:            //     "00"
		case ADM_DI:            //     "00"
		case ADM_CO:            //     "00"
		case CORP_DP_ADM:       //     "00"
		case ENT_CH:            //     "00"
		case ENT_CT:            //     "00"
		case ENT_DI:            //     "00"
		case ENT_CO:            //     "00"
		case CORP_DP_ENT:       //     "00"
		case SS:                return "00";

		case FILIALE_HS_NIRC:   return "01";

		case ENT_PUBLIQUE_HS:   //     "00"
		case ADM_PUBLIQUE_HS:   //     "00"
		case ORG_INTERNAT:      //     "00"
		case ENT_HS:            return "00";

		default:                return "00";
		}
	}
}

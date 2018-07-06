package ch.vd.unireg.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Liste des rôles IFOSec utilisés dans Unireg
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public enum Role {

	VISU_LIMITE("ROLE_VISU_LIMITE", "UR000001"),
	VISU_ALL("ROLE_VISU_ALL", "UR000002"),
	CREATE_NONHAB("ROLE_CREATE_NONHAB", "UR000010"),
	CREATE_AC("ROLE_CREATE_AC", "UR000011"),
	CREATE_DPI("ROLE_CREATE_DPI", "UR000012"),
	CREATE_PM("ROLE_CREATE_PM", "UR000013"),
	CREATE_CA("ROLE_CREATE_CA", "UR000014"),
	MODIF_VD_ORD("ROLE_MODIF_VD_ORD", "UR000020"),
	MODIF_VD_SOURC("ROLE_MODIF_VD_SOURC", "UR000021"),
	MODIF_HC_HS("ROLE_MODIF_HC_HS", "UR000022"),
	/**
	 * Habitant non-assujetti
	 */
	MODIF_HAB_DEBPUR("ROLE_MODIF_HAB_DEBPUR", "UR000023"),
	/**
	 * Non-habitant non-assujetti
	 */
	MODIF_NONHAB_DEBPUR("ROLE_MODIF_NONHAB_DEBPUR", "UR000024"),
	MODIF_NONHAB_INACTIF("ROLE_MODIF_NONHAB_INACTIF", "UR000025"),
	MODIF_AC("ROLE_MODIF_AC", "UR000026"),
	MODIF_PM("ROLE_MODIF_PM", "UR000027"),
	MODIF_CA("ROLE_MODIF_CA", "UR000028"),
	ADR_PP_D("ROLE_ADR_PP_D", "UR000030"),
	ADR_PP_C("ROLE_ADR_PP_C", "UR000031"),
	ADR_PP_B("ROLE_ADR_PP_B", "UR000032"),
	ADR_P("ROLE_ADR_P", "UR000033"),
	ADR_PP_C_DCD("ROLE_ADR_PP_C_DCD", "UR000034"),
	ADR_PM_D("ROLE_ADR_PM_D", "UR000035"),
	ADR_PM_C("ROLE_ADR_PM_C", "UR000036"),
	ADR_PM_B("ROLE_ADR_PM_B", "UR000037"),
	FOR_PRINC_ORDDEP_HAB("ROLE_FOR_PRINC_ORDDEP_HAB", "UR000040"),
	FOR_PRINC_ORDDEP_HCHS("ROLE_FOR_PRINC_ORDDEP_HCHS", "UR000041"),
	FOR_PRINC_ORDDEP_GRIS("ROLE_FOR_PRINC_ORDDEP_GRIS", "UR000042"),
	FOR_PRINC_SOURC_HAB("ROLE_FOR_PRINC_SOURC_HAB", "UR000043"),
	FOR_PRINC_SOURC_HCHS("ROLE_FOR_PRINC_SOURC_HCHS", "UR000044"),
	FOR_PRINC_SOURC_GRIS("ROLE_FOR_PRINC_SOURC_GRIS", "UR000045"),
	FOR_PRINC_PM("ROLE_FOR_PRINC_PM", "UR000046"),
	FOR_SECOND_PP("ROLE_FOR_SECOND_PP", "UR000047"),
	FOR_SECOND_PM("ROLE_FOR_SECOND_PM", "UR000048"),
	FOR_AUTRE("ROLE_FOR_AUTRE", "UR000049"),
	DI_EMIS_PP("ROLE_DI_EMIS_PP", "UR000050"),
	DI_EMIS_PM("ROLE_DI_EMIS_PM", "UR000051"),
	DI_SOM_PP("ROLE_DI_SOM_PP", "UR000052"),
	DI_SOM_PM("ROLE_DI_SOM_PM", "UR000053"),
	DI_DUPLIC_PP("ROLE_DI_DUPLIC_PP", "UR000054"),
	DI_DUPLIC_PM("ROLE_DI_DUPLIC_PM", "UR000055"),
	DI_DELAI_PP("ROLE_DI_DELAI_PP", "UR000056"),
	DI_DELAI_PM("ROLE_DI_DELAI_PM", "UR000057"),
	DI_QUIT_PP("ROLE_DI_QUIT_PP", "UR000058"),
	DI_QUIT_PM("ROLE_DI_QUIT_PM", "UR000059"),
	SIT_FAM("ROLE_SIT_FAM", "UR000060"),
	RT("ROLE_RT", "UR000061"),
	COOR_FIN("ROLE_COOR_FIN", "UR000062"),
	ANNUL_TIERS("ROLE_ANNUL_TIERS", "UR000063"),
	LR("ROLE_LR", "UR000070"),
	EVEN("ROLE_EVEN", "UR000071"),
	FORM_OUV_DOSS("ROLE_FORM_OUV_DOSS", "UR000072"),
	SUIVI_DOSS("ROLE_SUIVI_DOSS", "UR000073"),
	EVEN_PM("ROLE_EVEN_PM", "UR000074"),
	ADMIN("ROLE_ADMIN", "UR000080"),
	PARAM_APP("ROLE_PARAM_APP", "UR000081"),
	PARAM_PERIODE("ROLE_PARAM_PERIODE", "UR000082"),
	LECTURE_DOSSIER_PROTEGE("ROLE_LECTURE_DOSSIER_PROTEGE", "IZPOUDP"),
	ECRITURE_DOSSIER_PROTEGE("ROLE_ECRITURE_DOSSIER_PROTEGE", "IZPOUDM"),
	SEC_DOS_LEC("ROLE_SEC_DOS_LEC", "UR000090"),
	SEC_DOS_ECR("ROLE_SEC_DOS_ECR", "UR000091"),
	MVT_DOSSIER_MASSE("ROLE_MVT_DOSSIER_MASSE", "UR000092"),
	VISU_FORS("ROLE_VISU_FORS", "UR000093"),
	VISU_IMMEUBLES("ROLE_VISU_IMMEUBLES", "UR000094"), // SIFISC-2337
	GEST_EFACTURE("ROLE_GEST_EFACTURE", "UR000095"),    // gestion des états e-facture
	GEST_QUIT_EFACTURE("ROLE_GEST_QUIT_EFACTURE", "UR000096"),    // gestion des quittances e-facture (écran séparé)
	COMPLT_COMM("ROLE_COMPLT_COMM", "UR000097"),        // SIFISC-11438

	GEST_DECISION_ACI("ROLE_GEST_DECISION_ACI", "UR000098"),    // gestion des décisions ACI

	REMARQUE_TIERS("ROLE_REMARQUE_TIERS", "UR000099"),  // SIFISC-13146

	MW_IDENT_CTB_VISU("ROLE_MW_IDENT_CTB_VISU", "UR000100"),
	MW_IDENT_CTB_CELLULE_BO("ROLE_MW_IDENT_CTB_CELLULE_BO", "UR000101"),
	MW_IDENT_CTB_GEST_BO("ROLE_MW_IDENT_CTB_GEST_BO", "UR000102"),
	MW_IDENT_CTB_ADMIN("ROLE_MW_IDENT_CTB_ADMIN", "UR000103"),

	NCS_IDENT_CTB_CELLULE_BO("ROLE_NCS_IDENT_CTB_CELLULE_BO", "UR000104"),
	LISTE_IS_IDENT_CTB_CELLULE_BO("ROLE_LISTE_IS_IDENT_CTB_CELLULE_BO", "UR000105"),

	MODIF_FISCAL_DPI("ROLE_MODIF_FISCAL_DPI", "UR000106"),  // SIFISC-13603

	RAPPROCHEMENT_RF_IDENTIFICATION_CTB("ROLE_RAPPROCHEMENT_RF_IDENTIFICATION_CTB", "UR000107"),    // SIFISC-20374

	QSNC_EMISSION("ROLE_QSNC_EMISSION", "UR000130"),
	QSNC_RAPPEL("ROLE_QSNC_RAPPEL", "UR000131"),
	QSNC_DUPLICATA("ROLE_QSNC_DUPLICATA", "UR000132"),
	QSNC_QUITTANCEMENT("ROLE_QSNC_QUITTANCEMENT", "UR000133"),
	QSNC_LIBERATION("ROLE_QSNC_LIBERATION","UR000134"),
	QSNC_DELAI("ROLE_QSNC_DELAI","UR000135"),

	GEST_SNC("ROLE_GEST_SNC", "UR000136"),

	DI_DESANNUL_PP("ROLE_DI_DESANNUL_PP", "UR000140"), // SIFISC-5517
	DI_DESANNUL_PM("ROLE_DI_DESANNUL_PM", "UR000141"),

	DI_SUSPENDRE_PM("ROLE_DI_SUPENDRE_PM", "UR000142"),
	DI_DESUSPENDRE_PM("ROLE_DI_DESUPENDRE_PM", "UR000143"),
	BOUCLEMENTS_PM("ROLE_BOUCLEMENTS_PM", "UR000144"),

	ETAT_PM("ROLE_ETAT_PM", "UR000145"),

	REGIMES_FISCAUX("ROLE_REGIMES_FISCAUX", "UR000146"),
	ALLEGEMENTS_FISCAUX("ROLE_ALLEGEMENTS_FISCAUX", "UR000147"),
	FLAGS_PM("ROLE_FLAGS_PM", "UR000148"),

	ETABLISSEMENTS("ROLE_ETABLISSEMENTS", "UR000149"),
	CREATE_ENTREPRISE("ROLE_CREATE_ENTREPRISE", "UR000150"),

	GEST_QUIT_LETTRE_BIENVENUE("ROLE_GEST_QUIT_LETTRE_BIENVENUE", "UR000151"),

	FAILLITE_ENTREPRISE("ROLE_FAILLITE_ENTREPRISE", "UR000152"),
	DEMENAGEMENT_SIEGE_ENTREPRISE("ROLE_DEMENAGEMENT_SIEGE_ENTREPRISE", "UR000153"),
	FIN_ACTIVITE_ENTREPRISE("ROLE_FIN_ACTIVITE_ENTREPRISE", "UR000154"),
	FUSION_ENTREPRISES("ROLE_FUSION_ENTREPRISES", "UR000155"),
	SCISSION_ENTREPRISE("ROLE_SCISSION_ENTREPRISE", "UR000156"),
	TRANSFERT_PATRIMOINE_ENTREPRISE("ROLE_TRANSFERT_PATRIMOINE_ENTREPRISE", "UR000157"),
	REINSCRIPTION_RC_ENTREPRISE("ROLE_REINSCRIPTION_RC_ENTREPRISE", "UR000158"),

	DI_LIBERER_PM("ROLE_DI_LIBERER_PM", "UR000159"),        // SIFISC-19756
	DI_LIBERER_PP("ROLE_DI_LIBERER_PP", "UR000160"),        // SIFISC-19756

	MODIF_MANDAT_GENERAL("ROLE_MODIF_MANDAT_GENERAL", "UR000161"),  // SIFISC-17049
	MODIF_MANDAT_SPECIAL("ROLE_MODIF_MANDAT_SPECIAL", "UR000162"),  // SIFISC-17049
	MODIF_MANDAT_TIERS("ROLE_MODIF_MANDAT_TIERS", "UR000163"),  // SIFISC-17049

	ENVOI_AUTORISATION_RADIATION("ROLE_ENVOI_AUTORISATION_RADIATION", "UR000164"),  // SIFISC-18446
	ENVOI_DEMANDE_BILAN_FINAL("ROLE_ENVOI_DEMANDE_BILAN_FINAL", "UR000165"),  // SIFISC-18446
	ENVOI_LETTRE_TYPE_INFO_LIQUIDATION("ROLE_ENVOI_LETTRE_TYPE_INFO_LIQUIDATION", "UR000166"),  // SIFISC-18446

	REQUISITION_RADIATION_RC("ROLE_REQUISITION_RADIATION_RC", "UR000167"),      // SIFISC-18446
	SUIVI_ANNONCES_IDE("ROLE_SUIVI_ANNONCES_IDE", "UR000168"),      // SIFISC-19660

	GEST_ETIQUETTES("ROLE_GEST_ETIQUETTES", "UR000169"),            // SIFISC-20149

	SUIVI_IMPORT_RF("ROLE_SUIVI_IMPORT_RF", "UR000170"),      // SIFISC-20372

	GEST_FOURRE_NEUTRE("ROLE_GEST_FOURRE_NEUTRE","UR000171"), // SIFISC-22442

	DEMANDES_DEGREVEMENT_ICI("ROLE_DEMANDES_DEGREVEMENT_ICI", "UR000172"),      // SIFISC-21763
	DEGREVEMENTS_ICI("ROLE_DEGREVEMENTS_ICI", "UR000173"),                      // SIFISC-21763
	EXONERATIONS_IFONC("ROLE_EXONERATIONS_IFONC", "UR000174"),                  // SIFISC-21763

	GEST_FRACTIONS_COMMUNE_RF("ROLE_GEST_FRACTIONS_COMMUNE_RF", "UR000175"),                // SIFISC-24367
	ELECTION_PRINCIPAL_COMMUNAUTE_RF("ROLE_ELECTION_PRINCIPAL_COMMUNAUTE_RF", "UR000176"),  // SIFISC-24595

	SUPERGRA("ROLE_SUPERGRA", "UR000777"),

	TESTER("ROLE_TESTER", "");

	private static HashMap<String, Role> ifoSec2Role;
	private final String code;
	private final String ifosecCode;

	Role(String code, String ifosecCode) {
		this.code = code;
		this.ifosecCode = ifosecCode;
	}

	public String getCode() {
		return code;
	}

	public String getIfosecCode() {
		return ifosecCode;
	}

	public static Role fromIfoSec(String code) {
		return getIfoSec2Role().get(code);
	}

	private static Map<String, Role> getIfoSec2Role() {
		if (ifoSec2Role == null) {
			// lazy-init pour laisser le temps au UniregModeHelper de s'initializer.
			initIfoSec2Role();
		}
		return ifoSec2Role;
	}

	private static synchronized void initIfoSec2Role() {
		if (ifoSec2Role == null) {
			ifoSec2Role = new HashMap<>();

			for (Role role : Role.values()) {
				if (ifoSec2Role.containsKey(role.getIfosecCode())) {
					throw new IllegalArgumentException("Le code IfoSec " + role.getIfosecCode() + " est défini deux fois !");
				}
				ifoSec2Role.put(role.getIfosecCode(), role);
			}
		}
	}
}

package ch.vd.uniregctb.security;

import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.utils.BridageISHelper;

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
	MODIF_HAB_DEBPUR("ROLE_MODIF_HAB_DEBPUR", "UR000023"),
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
	//ANNUL_TIERS("ROLE_ANNUL_TIERS", "UR000063"),
	LR("ROLE_LR", "UR000070"),
	EVEN("ROLE_EVEN", "UR000071"),
	FORM_OUV_DOSS("ROLE_FORM_OUV_DOSS", "UR000072"),
	SUIVI_DOSS("ROLE_SUIVI_DOSS", "UR000073"),
	ADMIN("ROLE_ADMIN", "UR000080"),
	PARAM_APP("ROLE_PARAM_APP", "UR000081"),
	PARAM_PERIODE("ROLE_PARAM_PERIODE", "UR000082"),
	LECTURE_DOSSIER_PROTEGE("ROLE_LECTURE_DOSSIER_PROTEGE", "IZPOUDP"),
	ECRITURE_DOSSIER_PROTEGE("ROLE_ECRITURE_DOSSIER_PROTEGE", "IZPOUDM"),
	SEC_DOS_LEC("ROLE_SEC_DOS_LEC", "UR000090"),
	SEC_DOS_ECR("ROLE_SEC_DOS_ECR", "UR000091"),
	MVT_DOSSIER_MASSE("ROLE_MVT_DOSSIER_MASSE", "UR000092"),

	MW_IDENT_CTB_VISU("ROLE_MW_IDENT_CTB_VISU", "UR000100"),
	MW_IDENT_CTB_CELLULE_BO("ROLE_MW_IDENT_CTB_CELLULE_BO", "UR000101"),
	MW_IDENT_CTB_GEST_BO("ROLE_MW_IDENT_CTB_GEST_BO", "UR000102"),
	MW_IDENT_CTB_ADMIN("ROLE_MW_IDENT_CTB_ADMIN", "UR000103"),

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
			// lazy-init pour laisser le temps au BridageISHelper de s'initializer.
			initIfoSec2Role();
		}
		return ifoSec2Role;
	}

	private static synchronized void initIfoSec2Role() {
		if (ifoSec2Role != null) {
			return;
		}

		ifoSec2Role = new HashMap<String, Role>() {

			private static final long serialVersionUID = -2656272349946676385L;

			{
				put(Role.VISU_LIMITE);
				put(Role.VISU_ALL);
				put(Role.CREATE_NONHAB);

				if (!BridageISHelper.isBridageIS()) {
					put(Role.CREATE_AC);
					put(Role.CREATE_DPI);
					put(Role.CREATE_PM);
				}

				put(Role.CREATE_CA);
				put(Role.MODIF_VD_ORD);
				put(Role.MODIF_VD_SOURC);
				put(Role.MODIF_HC_HS);
				put(Role.MODIF_HAB_DEBPUR);
				put(Role.MODIF_NONHAB_DEBPUR);
				put(Role.MODIF_NONHAB_INACTIF);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.MODIF_AC);
					put(Role.MODIF_PM);
				}
				put(Role.MODIF_CA);
				put(Role.ADR_PP_D);
				put(Role.ADR_PP_C);
				put(Role.ADR_PP_B);
				put(Role.ADR_P);
				put(Role.ADR_PP_C_DCD);
				put(Role.ADR_PM_D);
				put(Role.ADR_PM_C);
				put(Role.ADR_PM_B);
				put(Role.FOR_PRINC_ORDDEP_HAB);
				put(Role.FOR_PRINC_ORDDEP_HCHS);
				put(Role.FOR_PRINC_ORDDEP_GRIS);
				put(Role.FOR_PRINC_SOURC_HAB);
				put(Role.FOR_PRINC_SOURC_HCHS);
				put(Role.FOR_PRINC_SOURC_GRIS);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.FOR_PRINC_PM);
				}
				put(Role.FOR_SECOND_PP);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.FOR_SECOND_PM);
				}

				put(Role.FOR_AUTRE);
				put(Role.DI_EMIS_PP);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.DI_EMIS_PM);
				}
				put(Role.DI_SOM_PP);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.DI_SOM_PM);
				}
				put(Role.DI_DUPLIC_PP);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.DI_DUPLIC_PM);
				}
				put(Role.DI_DELAI_PP);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.DI_DELAI_PM);
				}
				put(Role.DI_QUIT_PP);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.DI_QUIT_PM);
				}
				put(Role.SIT_FAM);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.RT);
				}
				put(Role.COOR_FIN);
				//put(Role.ANNUL_TIERS);
				if (!BridageISHelper.isBridageIS()) {
					put(Role.LR);
				}
				put(Role.EVEN);
				put(Role.FORM_OUV_DOSS);
				put(Role.SUIVI_DOSS);
				put(Role.ADMIN);
				put(Role.PARAM_APP);
				put(Role.PARAM_PERIODE);

				put(Role.LECTURE_DOSSIER_PROTEGE);
				put(Role.ECRITURE_DOSSIER_PROTEGE);
				put(Role.SEC_DOS_LEC);
				put(Role.SEC_DOS_ECR);

				put(Role.MVT_DOSSIER_MASSE);

				put(Role.MW_IDENT_CTB_VISU);
				put(Role.MW_IDENT_CTB_CELLULE_BO);
				put(Role.MW_IDENT_CTB_GEST_BO);
				put(Role.MW_IDENT_CTB_ADMIN);
			};

			void put(Role role) {
				if (containsKey(role.getIfosecCode())) {
					Assert.fail("Le code IfoSec " + role.getIfosecCode() + " est défini deux fois !");
				}
				put(role.getIfosecCode(), role);
			}
		};

	}
}

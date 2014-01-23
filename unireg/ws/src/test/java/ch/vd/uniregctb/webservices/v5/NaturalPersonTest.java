package ch.vd.uniregctb.webservices.v5;

import org.junit.Test;

import ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.TypePermis;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;


public class NaturalPersonTest extends EnumTest {

	@Test
	public void testCategorieFromValue() {
		assertNull(EnumHelper.coreToWeb((CategorieEtranger) null));
		// la catégorie suisse n'existe pas dans core. assertEquals(NaturalPersonCategoryType.SUISSE,
		// EnumHelper.coreToXMLv3(ch.vd.uniregctb.type.CategorieEtranger.SUISSE));

		assertEquals(NaturalPersonCategoryType.C_02_B_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._02_PERMIS_SEJOUR_B));
		assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._03_ETABLI_C));
		assertEquals(NaturalPersonCategoryType.C_04_CI_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._04_CONJOINT_DIPLOMATE_CI));
		assertEquals(NaturalPersonCategoryType.C_05_F_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._05_ETRANGER_ADMIS_PROVISOIREMENT_F));
		assertEquals(NaturalPersonCategoryType.C_06_G_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._06_FRONTALIER_G));
		assertEquals(NaturalPersonCategoryType.C_07_L_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._07_PERMIS_SEJOUR_COURTE_DUREE_L));
		assertEquals(NaturalPersonCategoryType.C_08_N_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._08_REQUERANT_ASILE_N));
		assertEquals(NaturalPersonCategoryType.C_09_S_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._09_A_PROTEGER_S));
		assertEquals(NaturalPersonCategoryType.C_10_OBLIGED_TO_ANNOUNCE, EnumHelper.coreToWeb(CategorieEtranger._10_TENUE_DE_S_ANNONCER));
		assertEquals(NaturalPersonCategoryType.C_11_DIPLOMAT, EnumHelper.coreToWeb(CategorieEtranger._11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE));
		assertEquals(NaturalPersonCategoryType.C_12_INTERNATIONAL_CIVIL_SERVANT, EnumHelper.coreToWeb(CategorieEtranger._12_FONCT_INTER_SANS_IMMUNITE));
		assertEquals(NaturalPersonCategoryType.C_13_NOT_ASSIGNED, EnumHelper.coreToWeb(CategorieEtranger._13_NON_ATTRIBUEE));
	}

	@Test
	public void testCategorieFromTypePermis() {
		assertNull(EnumHelper.coreToWeb((TypePermis) null));

		assertEquals(NaturalPersonCategoryType.C_02_B_PERMIT, EnumHelper.coreToWeb(TypePermis.SEJOUR));
		assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, EnumHelper.coreToWeb(TypePermis.ETABLISSEMENT));
		assertEquals(NaturalPersonCategoryType.C_04_CI_PERMIT, EnumHelper.coreToWeb(TypePermis.CONJOINT_DIPLOMATE));
		assertEquals(NaturalPersonCategoryType.C_05_F_PERMIT, EnumHelper.coreToWeb(TypePermis.ETRANGER_ADMIS_PROVISOIREMENT));
		assertEquals(NaturalPersonCategoryType.C_06_G_PERMIT, EnumHelper.coreToWeb(TypePermis.FRONTALIER));
		assertEquals(NaturalPersonCategoryType.C_07_L_PERMIT, EnumHelper.coreToWeb(TypePermis.COURTE_DUREE));
		assertEquals(NaturalPersonCategoryType.C_08_N_PERMIT, EnumHelper.coreToWeb(TypePermis.REQUERANT_ASILE));
		assertEquals(NaturalPersonCategoryType.C_09_S_PERMIT, EnumHelper.coreToWeb(TypePermis.PERSONNE_A_PROTEGER));
		assertEquals(NaturalPersonCategoryType.C_10_OBLIGED_TO_ANNOUNCE, EnumHelper.coreToWeb(TypePermis.PERSONNE_TENUE_DE_S_ANNONCER));
		assertEquals(NaturalPersonCategoryType.C_11_DIPLOMAT, EnumHelper.coreToWeb(TypePermis.DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE));
		assertEquals(NaturalPersonCategoryType.C_12_INTERNATIONAL_CIVIL_SERVANT, EnumHelper.coreToWeb(TypePermis.FONCT_INTER_SANS_IMMUNITE));
		assertEquals(NaturalPersonCategoryType.C_13_NOT_ASSIGNED, EnumHelper.coreToWeb(TypePermis.PAS_ATTRIBUE));
		// Pas d'équivalent : TypePermis.PROVISOIRE
		// Pas d'équivalent : TypePermis.SUISSE_SOURCIER;
	}
}

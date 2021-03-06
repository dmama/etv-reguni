package ch.vd.unireg.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.webservices.party3.EnumTest;
import ch.vd.unireg.webservices.party3.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class NaturalPersonTest extends EnumTest {

	@Test
	public void testCategorieFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.CategorieEtranger) null));
		// la catégorie suisse n'existe pas dans core. assertEquals(NaturalPersonCategory.SUISSE,
		// EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger.SUISSE));

		assertEquals(NaturalPersonCategory.C_02_B_PERMIT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._02_PERMIS_SEJOUR_B));
		assertEquals(NaturalPersonCategory.C_03_C_PERMIT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._03_ETABLI_C));
		assertEquals(NaturalPersonCategory.C_04_CI_PERMIT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._04_CONJOINT_DIPLOMATE_CI));
		assertEquals(NaturalPersonCategory.C_05_F_PERMIT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._05_ETRANGER_ADMIS_PROVISOIREMENT_F));
		assertEquals(NaturalPersonCategory.C_06_G_PERMIT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._06_FRONTALIER_G));
		assertEquals(NaturalPersonCategory.C_07_L_PERMIT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._07_PERMIS_SEJOUR_COURTE_DUREE_L));
		assertEquals(NaturalPersonCategory.C_08_N_PERMIT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._08_REQUERANT_ASILE_N));
		assertEquals(NaturalPersonCategory.C_09_S_PERMIT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._09_A_PROTEGER_S));
		assertEquals(NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._10_TENUE_DE_S_ANNONCER));
		assertEquals(NaturalPersonCategory.C_11_DIPLOMAT, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE));
		assertEquals(NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT, EnumHelper.coreToWeb(CategorieEtranger._12_FONCT_INTER_SANS_IMMUNITE));
		assertEquals(NaturalPersonCategory.C_13_NOT_ASSIGNED, EnumHelper.coreToWeb(ch.vd.unireg.type.CategorieEtranger._13_NON_ATTRIBUEE));
	}

	@Test
	public void testCategorieFromTypePermis() {
		assertNull(EnumHelper.coreToWeb((TypePermis) null));

		assertEquals(NaturalPersonCategory.C_02_B_PERMIT, EnumHelper.coreToWeb(TypePermis.SEJOUR));
		assertEquals(NaturalPersonCategory.C_03_C_PERMIT, EnumHelper.coreToWeb(TypePermis.ETABLISSEMENT));
		assertEquals(NaturalPersonCategory.C_04_CI_PERMIT, EnumHelper.coreToWeb(TypePermis.CONJOINT_DIPLOMATE));
		assertEquals(NaturalPersonCategory.C_05_F_PERMIT, EnumHelper.coreToWeb(TypePermis.ETRANGER_ADMIS_PROVISOIREMENT));
		assertEquals(NaturalPersonCategory.C_06_G_PERMIT, EnumHelper.coreToWeb(TypePermis.FRONTALIER));
		assertEquals(NaturalPersonCategory.C_07_L_PERMIT, EnumHelper.coreToWeb(TypePermis.COURTE_DUREE));
		assertEquals(NaturalPersonCategory.C_08_N_PERMIT, EnumHelper.coreToWeb(TypePermis.REQUERANT_ASILE));
		assertEquals(NaturalPersonCategory.C_09_S_PERMIT, EnumHelper.coreToWeb(TypePermis.PERSONNE_A_PROTEGER));
		assertEquals(NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE, EnumHelper.coreToWeb(TypePermis.PERSONNE_TENUE_DE_S_ANNONCER));
		assertEquals(NaturalPersonCategory.C_11_DIPLOMAT, EnumHelper.coreToWeb(TypePermis.DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE));
		assertEquals(NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT, EnumHelper.coreToWeb(TypePermis.FONCT_INTER_SANS_IMMUNITE));
		assertEquals(NaturalPersonCategory.C_13_NOT_ASSIGNED, EnumHelper.coreToWeb(TypePermis.PAS_ATTRIBUE));
		// Pas d'équivalent : TypePermis.PROVISOIRE
		// Pas d'équivalent : TypePermis.SUISSE_SOURCIER;
	}
}

package ch.vd.uniregctb.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;


public class NaturalPersonTest extends EnumTest {

	@Test
	public void testCategorieFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.CategorieEtranger) null));
		// la catégorie suisse n'existe pas dans core. assertEquals(NaturalPersonCategory.SUISSE,
		// EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger.SUISSE));

		assertEquals(NaturalPersonCategory.C_02_B_PERMIT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._02_PERMIS_SEJOUR_B));
		assertEquals(NaturalPersonCategory.C_03_C_PERMIT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._03_ETABLI_C));
		assertEquals(NaturalPersonCategory.C_04_CI_PERMIT, EnumHelper.coreToWeb(CategorieEtranger._04_CONJOINT_DIPLOMATE_OU_FONCT_INT_CI));
		assertEquals(NaturalPersonCategory.C_05_F_PERMIT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._05_ETRANGER_ADMIS_PROVISOIREMENT_F));
		assertEquals(NaturalPersonCategory.C_06_G_PERMIT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._06_FRONTALIER_G));
		assertEquals(NaturalPersonCategory.C_07_L_PERMIT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._07_PERMIS_SEJOUR_COURTE_DUREE_L));
		assertEquals(NaturalPersonCategory.C_08_N_PERMIT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._08_REQUERANT_ASILE_N));
		assertEquals(NaturalPersonCategory.C_09_S_PERMIT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._09_A_PROTEGER_S));
		assertEquals(NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._10_TENUE_DE_S_ANNONCER));
		assertEquals(NaturalPersonCategory.C_11_DIPLOMAT, EnumHelper.coreToWeb(CategorieEtranger._11_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL));
		// TODO SIFISC 5349 - les catégories 11 et 12 sont fusionnés dans la 11 ( on modifie l'enum web aussi ?)
		// assertEquals(NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT, EnumHelper.coreToWeb(CategorieEtranger._11_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL));
		assertEquals(NaturalPersonCategory.C_13_NOT_ASSIGNED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._13_NON_ATTRIBUEE));
	}

	@Test
	public void testCategorieFromTypePermis() {
		assertNull(EnumHelper.coreToWeb((TypePermis) null));

		assertEquals(NaturalPersonCategory.C_02_B_PERMIT, EnumHelper.coreToWeb(TypePermis.ANNUEL));
		assertEquals(NaturalPersonCategory.C_07_L_PERMIT, EnumHelper.coreToWeb(TypePermis.COURTE_DUREE));
		assertEquals(NaturalPersonCategory.C_11_DIPLOMAT, EnumHelper.coreToWeb(TypePermis.DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL));
		assertEquals(NaturalPersonCategory.C_03_C_PERMIT, EnumHelper.coreToWeb(TypePermis.ETABLISSEMENT));
		assertEquals(NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT, EnumHelper.coreToWeb(TypePermis.CONJOINT_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL));
		assertEquals(NaturalPersonCategory.C_06_G_PERMIT, EnumHelper.coreToWeb(TypePermis.FRONTALIER));
		assertEquals(NaturalPersonCategory.C_09_S_PERMIT, EnumHelper.coreToWeb(TypePermis.PERSONNE_A_PROTEGER));
		// Pas d'équivalent : TypePermis.PROVISOIRE
		assertEquals(NaturalPersonCategory.C_08_N_PERMIT, EnumHelper.coreToWeb(TypePermis.REQUERANT_ASILE));
		assertEquals(NaturalPersonCategory.C_05_F_PERMIT, EnumHelper.coreToWeb(TypePermis.ETRANGER_ADMIS_PROVISOIREMENT));
		// Pas d'équivalent : TypePermis.SUISSE_SOURCIER;
	}
}

package ch.vd.uniregctb.webservices.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;



public class PersonnePhysiqueTest extends EnumTest {

	@Test
	public void testCategorieFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.CategorieEtranger) null));
		// la catégorie suisse n'existe pas dans core. assertEquals(PersonnePhysique.Categorie.SUISSE,
		// EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger.SUISSE));

		assertEquals(PersonnePhysique.Categorie._02_PERMIS_SEJOUR_B, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._02_PERMIS_SEJOUR_B));
		assertEquals(PersonnePhysique.Categorie._03_ETABLI_C, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._03_ETABLI_C));
		assertEquals(PersonnePhysique.Categorie._04_CONJOINT_DIPLOMATE_CI, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._04_CONJOINT_DIPLOMATE_CI));
		assertEquals(PersonnePhysique.Categorie._05_ETRANGER_ADMIS_PROVISOIREMENT_F, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._05_ETRANGER_ADMIS_PROVISOIREMENT_F));
		assertEquals(PersonnePhysique.Categorie._06_FRONTALIER_G, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._06_FRONTALIER_G));
		assertEquals(PersonnePhysique.Categorie._07_PERMIS_SEJOUR_COURTE_DUREE_L, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._07_PERMIS_SEJOUR_COURTE_DUREE_L));
		assertEquals(PersonnePhysique.Categorie._08_REQUERANT_ASILE_N, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._08_REQUERANT_ASILE_N));
		assertEquals(PersonnePhysique.Categorie._09_A_PROTEGER_S, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._09_A_PROTEGER_S));
		assertEquals(PersonnePhysique.Categorie._10_TENUE_DE_S_ANNONCER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._10_TENUE_DE_S_ANNONCER));
		assertEquals(PersonnePhysique.Categorie._11_DIPLOMATE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._11_DIPLOMATE));
		assertEquals(PersonnePhysique.Categorie._12_FONCTIONNAIRE_INTERNATIONAL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._12_FONCTIONNAIRE_INTERNATIONAL));
		assertEquals(PersonnePhysique.Categorie._13_NON_ATTRIBUEE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._13_NON_ATTRIBUEE));
	}

	@Test
	public void testCategorieFromTypePermis() {
		assertNull(EnumHelper.coreToWeb((ch.vd.registre.civil.model.EnumTypePermis) null));

		assertEquals(PersonnePhysique.Categorie._02_PERMIS_SEJOUR_B, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.ANNUEL));
		assertEquals(PersonnePhysique.Categorie._07_PERMIS_SEJOUR_COURTE_DUREE_L, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.COURTE_DUREE));
		assertEquals(PersonnePhysique.Categorie._11_DIPLOMATE, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.DIPLOMATE));
		assertEquals(PersonnePhysique.Categorie._03_ETABLI_C, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.ETABLLISSEMENT));
		assertEquals(PersonnePhysique.Categorie._12_FONCTIONNAIRE_INTERNATIONAL, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.FONCTIONNAIRE_INTERNATIONAL));
		assertEquals(PersonnePhysique.Categorie._06_FRONTALIER_G, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.FRONTALIER));
		assertEquals(PersonnePhysique.Categorie._09_A_PROTEGER_S, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.PERSONNE_A_PROTEGER));
		// Pas d'équivalent : EnumTypePermis.PROVISOIRE
		assertEquals(PersonnePhysique.Categorie._08_REQUERANT_ASILE_N, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.REQUERANT_ASILE_AVANT_DECISION));
		assertEquals(PersonnePhysique.Categorie._05_ETRANGER_ADMIS_PROVISOIREMENT_F, EnumHelper.coreToWeb(ch.vd.registre.civil.model.EnumTypePermis.REQUERANT_ASILE_REFUSE));
		// Pas d'équivalent : EnumTypePermis.SUISSE_SOURCIER;
	}
}

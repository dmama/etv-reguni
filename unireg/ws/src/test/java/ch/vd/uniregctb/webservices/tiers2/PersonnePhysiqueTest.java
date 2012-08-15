package ch.vd.uniregctb.webservices.tiers2;

import org.junit.Test;

import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;



public class PersonnePhysiqueTest extends EnumTest {

	@Test
	public void testCategorieFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.CategorieEtranger) null));
		// la catégorie suisse n'existe pas dans core. assertEquals(PersonnePhysique.Categorie.SUISSE,
		// EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger.SUISSE));

		assertEquals(PersonnePhysique.Categorie._02_PERMIS_SEJOUR_B, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._02_PERMIS_SEJOUR_B));
		assertEquals(PersonnePhysique.Categorie._03_ETABLI_C, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._03_ETABLI_C));
		// TODO SIFISC 5349 - a discuter ...
		//assertEquals(PersonnePhysique.Categorie._04_CONJOINT_DIPLOMATE_CI, EnumHelper.coreToWeb(CategorieEtranger._04_CONJOINT_DIPLOMATE_OU_FONCT_INT_CI));
		assertEquals(PersonnePhysique.Categorie._05_ETRANGER_ADMIS_PROVISOIREMENT_F, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._05_ETRANGER_ADMIS_PROVISOIREMENT_F));
		assertEquals(PersonnePhysique.Categorie._06_FRONTALIER_G, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._06_FRONTALIER_G));
		assertEquals(PersonnePhysique.Categorie._07_PERMIS_SEJOUR_COURTE_DUREE_L, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._07_PERMIS_SEJOUR_COURTE_DUREE_L));
		assertEquals(PersonnePhysique.Categorie._08_REQUERANT_ASILE_N, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._08_REQUERANT_ASILE_N));
		assertEquals(PersonnePhysique.Categorie._09_A_PROTEGER_S, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._09_A_PROTEGER_S));
		assertEquals(PersonnePhysique.Categorie._10_TENUE_DE_S_ANNONCER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._10_TENUE_DE_S_ANNONCER));
		// TODO SIFISC 5349 - les catégories 11 et 12 sont fusionnés dans la 11 ( on modifie l'enum web aussi ?)
		// assertEquals(PersonnePhysique.Categorie._11_DIPLOMATE, EnumHelper.coreToWeb(CategorieEtranger._11_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL));
		// assertEquals(PersonnePhysique.Categorie._12_FONCTIONNAIRE_INTERNATIONAL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._12_FONCTIONNAIRE_INTERNATIONAL));
		assertEquals(PersonnePhysique.Categorie._13_NON_ATTRIBUEE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.CategorieEtranger._13_NON_ATTRIBUEE));
	}

	@Test
	public void testCategorieFromTypePermis() {
		assertNull(EnumHelper.coreToWeb((TypePermis) null));

		assertEquals(PersonnePhysique.Categorie._02_PERMIS_SEJOUR_B, EnumHelper.coreToWeb(TypePermis.ANNUEL));
		assertEquals(PersonnePhysique.Categorie._07_PERMIS_SEJOUR_COURTE_DUREE_L, EnumHelper.coreToWeb(TypePermis.COURTE_DUREE));
		assertEquals(PersonnePhysique.Categorie._11_DIPLOMATE, EnumHelper.coreToWeb(TypePermis.DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL));
		assertEquals(PersonnePhysique.Categorie._03_ETABLI_C, EnumHelper.coreToWeb(TypePermis.ETABLISSEMENT));
		// TODO SIFISC 5349 - a discuter ...
		//assertEquals(PersonnePhysique.Categorie._12_FONCTIONNAIRE_INTERNATIONAL, EnumHelper.coreToWeb(TypePermis.DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL));
		assertEquals(PersonnePhysique.Categorie._06_FRONTALIER_G, EnumHelper.coreToWeb(TypePermis.FRONTALIER));
		assertEquals(PersonnePhysique.Categorie._09_A_PROTEGER_S, EnumHelper.coreToWeb(TypePermis.PERSONNE_A_PROTEGER));
		// Pas d'équivalent : TypePermis.PROVISOIRE
		assertEquals(PersonnePhysique.Categorie._08_REQUERANT_ASILE_N, EnumHelper.coreToWeb(TypePermis.REQUERANT_ASILE));
		assertEquals(PersonnePhysique.Categorie._05_ETRANGER_ADMIS_PROVISOIREMENT_F, EnumHelper.coreToWeb(TypePermis.ETRANGER_ADMIS_PROVISOIREMENT));
		// Pas d'équivalent : TypePermis.SUISSE_SOURCIER;
	}
}

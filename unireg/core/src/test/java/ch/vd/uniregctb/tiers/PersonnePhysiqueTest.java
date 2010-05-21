package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PersonnePhysiqueTest extends WithoutSpringTest {

	@Test
	public void testValidateNomVideNonHabitant() {

		PersonnePhysique pp = new PersonnePhysique(false);

		pp.setNom(null);
		assertTrue(pp.validate().hasErrors());

		pp.setNom("");
		assertTrue(pp.validate().hasErrors());

		pp.setNom("  ");
		assertTrue(pp.validate().hasErrors());

		pp.setNom("Bob");
		assertFalse(pp.validate().hasErrors());
	}

	@Test
	public void testValidateNomCaracteresSpeciauxNonHabitant() {

		final PersonnePhysique pp = new PersonnePhysique(false);

		pp.setNom("1");
		assertTrue(pp.validate().hasErrors());

		pp.setNom(".K");
		assertTrue(pp.validate().hasErrors());

		pp.setNom(" Kulti");
		assertTrue(pp.validate().hasErrors());

		pp.setNom("ŠǿůžŷķæœŒŭĠĥſ");
		assertTrue(pp.validate().hasErrors());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(pp.validate().hasErrors());

		pp.setNom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(pp.validate().hasErrors());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");
		assertFalse(pp.validate().hasErrors());

		pp.setNom("''AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");   // double guillemet en tête
		assertTrue(pp.validate().hasErrors());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž -  O'Hara.//"); // double espace
		assertTrue(pp.validate().hasErrors());

		pp.setNom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O''Hara.// "); // espace final
		assertTrue(pp.validate().hasErrors());
	}

	@Test
	public void testValidatePrenomCaracteresSpeciauxNonHabitant() {

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNom("Bob");

		pp.setPrenom("1");
		assertTrue(pp.validate().hasErrors());

		pp.setPrenom(".K");
		assertTrue(pp.validate().hasErrors());

		pp.setPrenom(" Kulti");
		assertTrue(pp.validate().hasErrors());

		pp.setPrenom("ŠǿůžŷķæœŒŭĠĥſ");
		assertTrue(pp.validate().hasErrors());

		pp.setPrenom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(pp.validate().hasErrors());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(pp.validate().hasErrors());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");
		assertFalse(pp.validate().hasErrors());

		pp.setPrenom("''AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");  // double guillemet en tête
		assertTrue(pp.validate().hasErrors());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž -  O'Hara.//");  // double espace
		assertTrue(pp.validate().hasErrors());

		pp.setPrenom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O''Hara.// ");  // espace final
		assertTrue(pp.validate().hasErrors());

		pp.setPrenom(null);
		assertFalse(pp.validate().hasErrors());
	}
}

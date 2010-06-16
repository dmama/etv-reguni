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
		assertFalse(pp.validate().hasWarnings());

		pp.setNom("");
		assertTrue(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());

		pp.setNom("  ");
		assertTrue(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());

		pp.setNom("Bob");
		assertFalse(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());
	}

	@Test
	public void testValidateNomCaracteresSpeciauxNonHabitant() {

		final PersonnePhysique pp = new PersonnePhysique(false);

		pp.setNom("1");
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setNom(".K");
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setNom(" Kulti");
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setNom("ŠǿůžŷķæœŒŭĠĥſ");
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());

		pp.setNom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");
		assertFalse(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());

		pp.setNom("''AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");   // double guillemet en tête
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž -  O'Hara.//"); // double espace
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setNom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O''Hara.// "); // espace final
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());
	}

	@Test
	public void testValidatePrenomCaracteresSpeciauxNonHabitant() {

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNom("Bob");

		pp.setPrenom("1");
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setPrenom(".K");
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setPrenom(" Kulti");
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setPrenom("ŠǿůžŷķæœŒŭĠĥſ");
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setPrenom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");
		assertFalse(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());

		pp.setPrenom("''AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");  // double guillemet en tête
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž -  O'Hara.//");  // double espace
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setPrenom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O''Hara.// ");  // espace final
		assertFalse(pp.validate().hasErrors());
		assertTrue(pp.validate().hasWarnings());

		pp.setPrenom(null);
		assertFalse(pp.validate().hasErrors());
		assertFalse(pp.validate().hasWarnings());
	}
}

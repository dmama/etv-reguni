package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.SurfaceAuSolRF;
import ch.vd.unireg.registrefoncier.key.SurfaceAuSolRFKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SurfaceAuSolRFHelperTest {

	@Test
	public void testDataEqualsListNullity() throws Exception {

		assertTrue(SurfaceAuSolRFHelper.dataEquals((Set<SurfaceAuSolRF>) null, null));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(Collections.emptySet(), null));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(null, Collections.emptyList()));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(Collections.emptySet(), Collections.emptyList()));

		assertFalse(SurfaceAuSolRFHelper.dataEquals(null, Collections.singletonList(new Bodenbedeckung())));
		assertFalse(SurfaceAuSolRFHelper.dataEquals(Collections.singleton(new SurfaceAuSolRF()), null));
	}

	@Test
	public void testDataEqualsListDifferentSizes() throws Exception {
		assertFalse(SurfaceAuSolRFHelper.dataEquals(Collections.singleton(new SurfaceAuSolRF()),
		                                            Arrays.asList(new Bodenbedeckung(), new Bodenbedeckung())));
	}

	@Test
	public void testDataEqualsList() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("3737628");

		final SurfaceAuSolRF surface1 = new SurfaceAuSolRF();
		surface1.setImmeuble(immeuble1);
		surface1.setSurface(10);
		surface1.setType("Forêt");

		final SurfaceAuSolRF surface2 = new SurfaceAuSolRF();
		surface2.setImmeuble(immeuble1);
		surface2.setSurface(47373);
		surface2.setType("Paturage");

		final SurfaceAuSolRF surface3 = new SurfaceAuSolRF();
		surface3.setImmeuble(immeuble1);
		surface3.setSurface(234);
		surface3.setType("Eau");

		final Bodenbedeckung bodenbedeckung1 = new Bodenbedeckung();
		bodenbedeckung1.setGrundstueckIDREF("3737628");
		bodenbedeckung1.setFlaeche(10);
		bodenbedeckung1.setArt(new CapiCode(null, "Forêt"));

		final Bodenbedeckung bodenbedeckung2 = new Bodenbedeckung();
		bodenbedeckung2.setGrundstueckIDREF("3737628");
		bodenbedeckung2.setFlaeche(47373);
		bodenbedeckung2.setArt(new CapiCode(null, "Paturage"));

		final Bodenbedeckung bodenbedeckung3 = new Bodenbedeckung();
		bodenbedeckung3.setGrundstueckIDREF("3737628");
		bodenbedeckung3.setFlaeche(234);
		bodenbedeckung3.setArt(new CapiCode(null, "Eau"));

		assertTrue(SurfaceAuSolRFHelper.dataEquals(newSet(surface1, surface2, surface3), newList(bodenbedeckung1, bodenbedeckung2, bodenbedeckung3)));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(newSet(surface1, surface2, surface3), newList(bodenbedeckung3, bodenbedeckung2, bodenbedeckung1)));
		assertFalse(SurfaceAuSolRFHelper.dataEquals(newSet(surface1, surface2), newList(bodenbedeckung2, bodenbedeckung3)));
		assertFalse(SurfaceAuSolRFHelper.dataEquals(newSet(surface1, surface3), newList(bodenbedeckung2, bodenbedeckung1)));
	}

	@Test
	public void testDataEquals() throws Exception {

		final SurfaceAuSolRF surface1 = new SurfaceAuSolRF();
		surface1.setSurface(10);
		surface1.setType("Forêt");

		final SurfaceAuSolRF surface2 = new SurfaceAuSolRF();
		surface2.setSurface(47373);
		surface2.setType("Paturage");

		final SurfaceAuSolRF surface3 = new SurfaceAuSolRF();
		surface3.setSurface(234);
		surface3.setType("Eau");

		assertFalse(SurfaceAuSolRFHelper.dataEquals(surface1, surface2));
		assertFalse(SurfaceAuSolRFHelper.dataEquals(surface2, surface3));
		assertFalse(SurfaceAuSolRFHelper.dataEquals(surface1, surface3));

		assertTrue(SurfaceAuSolRFHelper.dataEquals(surface1, surface1));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(surface2, surface2));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(surface3, surface3));
	}

	/**
	 * [SIFISC-22504] Ce test vérifie que la création d'une surface au sol à partir d'une donnée RF normale fonctionne bien.
	 */
	@Test
	public void testNewSurfaceAuSol() throws Exception {

		final Bodenbedeckung bodendeckung = new Bodenbedeckung();
		bodendeckung.setGrundstueckIDREF("1f109152381046080138104a9c595368");
		bodendeckung.setFlaeche(49);
		bodendeckung.setArt(new CapiCode("", "Pisicine"));

		final SurfaceAuSolRF surfaceAuSolRF = SurfaceAuSolRFHelper.newSurfaceAuSolRF(bodendeckung);
		assertNotNull(surfaceAuSolRF);
		assertEquals(49, surfaceAuSolRF.getSurface());
		assertEquals("Pisicine", surfaceAuSolRF.getType());
	}

	/**
	 * [SIFISC-22504] Ce test vérifie que la création d'une surface au sol à partir d'une donnée RF sans type ne crashe pas et que le type utilisé est 'Indéterminé'.
	 */
	@Test
	public void testNewSurfaceAuSolSansType() throws Exception {

		final Bodenbedeckung bodendeckung = new Bodenbedeckung();
		bodendeckung.setGrundstueckIDREF("1f109152381046080138104a9c595368");
		bodendeckung.setFlaeche(49);

		final SurfaceAuSolRF surfaceAuSolRF = SurfaceAuSolRFHelper.newSurfaceAuSolRF(bodendeckung);
		assertNotNull(surfaceAuSolRF);
		assertEquals(49, surfaceAuSolRF.getSurface());
		assertEquals("Indéterminé", surfaceAuSolRF.getType());
	}

	/**
	 * [SIFISC-23055] Ce test vérifie que la création d'une clé de surface au sol à partir d'une donnée RF sans type ne crashe pas et que le type utilisé est 'Indéterminé'.
	 */
	@Test
	public void testNewKeySansType() throws Exception {

		final Bodenbedeckung bodendeckung = new Bodenbedeckung();
		bodendeckung.setGrundstueckIDREF("1f109152381046080138104a9c595368");
		bodendeckung.setFlaeche(49);

		final SurfaceAuSolRFKey key = SurfaceAuSolRFHelper.newKey(bodendeckung);
		assertNotNull(key);
		assertEquals("1f109152381046080138104a9c595368", key.getIdRF());
		assertEquals(49, key.getSurface());
		assertEquals("Indéterminé", key.getType());
	}

	@NotNull
	private static List<Bodenbedeckung> newList(Bodenbedeckung... bodenbedeckungs) {
		return Arrays.asList(bodenbedeckungs);
	}

	@NotNull
	private static HashSet<SurfaceAuSolRF> newSet(SurfaceAuSolRF... surfaces) {
		return new HashSet<>(Arrays.asList(surfaces));
	}
}
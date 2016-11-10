package ch.vd.uniregctb.registrefoncier.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SurfaceAuSolRFHelperTest {

	@Test
	public void testDataEqualsNullity() throws Exception {

		assertTrue(SurfaceAuSolRFHelper.dataEquals(null, null));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(Collections.emptySet(), null));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(null, Collections.emptyList()));
		assertTrue(SurfaceAuSolRFHelper.dataEquals(Collections.emptySet(), Collections.emptyList()));

		assertFalse(SurfaceAuSolRFHelper.dataEquals(null, Collections.singletonList(new Bodenbedeckung())));
		assertFalse(SurfaceAuSolRFHelper.dataEquals(Collections.singleton(new SurfaceAuSolRF()), null));
	}

	@Test
	public void testDataEqualsDifferentSizes() throws Exception {
		assertFalse(SurfaceAuSolRFHelper.dataEquals(Collections.singleton(new SurfaceAuSolRF()), Arrays.asList(new Bodenbedeckung(), new Bodenbedeckung())));
	}

	@Test
	public void testDataEquals() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondRF();
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

	@NotNull
	private static List<Bodenbedeckung> newList(Bodenbedeckung... bodenbedeckungs) {
		return Arrays.asList(bodenbedeckungs);
	}

	@NotNull
	private static HashSet<SurfaceAuSolRF> newSet(SurfaceAuSolRF... surfaces) {
		return new HashSet<>(Arrays.asList(surfaces));
	}
}
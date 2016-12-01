package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import org.junit.Test;
import org.junit.runner.RunWith;

import ch.vd.capitastra.grundstueck.GrundstueckFlaeche;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(UniregJUnit4Runner.class)
public class SurfaceTotaleRFHelperTest {

	/**
	 * Ce test vérifie que deux surfaces identiques sont bien considérées égales.
	 */
	@Test
	public void testDataEquals() throws Exception {

		final SurfaceTotaleRF surface = new SurfaceTotaleRF();
		surface.setSurface(322);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(322);

		assertTrue(SurfaceTotaleRFHelper.dataEquals(surface, flaeche));
	}

	/**
	 * Ce test vérifie les cas de nullités entre surfaces.
	 */
	@Test
	public void testDataEqualsNullNotNull() throws Exception {
		assertTrue(SurfaceTotaleRFHelper.dataEquals(null, (SurfaceTotaleRF) null));
		assertFalse(SurfaceTotaleRFHelper.dataEquals(null, new SurfaceTotaleRF()));
		assertFalse(SurfaceTotaleRFHelper.dataEquals(new SurfaceTotaleRF(), (SurfaceTotaleRF) null));
	}

	/**
	 * Ce test vérifie que deux surfaces qui diffèrent sur le montant sont bien considérées inégales.
	 */
	@Test
	public void testDataEqualsMontantsDifferents() throws Exception {

		final SurfaceTotaleRF surface = new SurfaceTotaleRF();
		surface.setSurface(322);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(201);

		assertFalse(SurfaceTotaleRFHelper.dataEquals(surface, flaeche));
	}

	@Test
	public void testNewSurfaceTotaleRF() throws Exception {
		final SurfaceTotaleRF surface = SurfaceTotaleRFHelper.newSurfaceTotaleRF(201);
		assertEquals(201, surface.getSurface());
	}

}
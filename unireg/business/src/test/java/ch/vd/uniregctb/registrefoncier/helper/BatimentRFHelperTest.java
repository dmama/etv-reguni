package ch.vd.uniregctb.registrefoncier.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.GebaeudeArt;
import ch.vd.capitastra.grundstueck.GrundstueckZuGebaeude;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceBatimentRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BatimentRFHelperTest {

	/**
	 * Ce test vérifie qu'une exception est bien levée sur on compare deux bâtiments avec des idRF différents.
	 */
	@Test
	public void testCurrentDataEqualsDifferentIdRF() throws Exception {

		final BatimentRF batiment = new BatimentRF();
		batiment.setMasterIdRF("7837829e9a9a");

		final Gebaeude gebaeude = new Gebaeude();
		gebaeude.setMasterID("a8e88838e45");

		try {
			BatimentRFHelper.currentDataEquals(batiment, gebaeude);
			fail();
		}
		catch (ProgrammingException e) {
			assertNull(e.getMessage());
		}
	}

	/**
	 * Ce test vérifie qu'une exception est bien levée sur on compare deux bâtiments avec des types différents.
	 */
	@Test
	public void testCurrentDataEqualsDifferentTypes() throws Exception {

		final BatimentRF batiment = new BatimentRF();
		batiment.setMasterIdRF("7837829e9a9a");
		batiment.setType("Garage");

		final Gebaeude gebaeude = new Gebaeude();
		gebaeude.setMasterID("7837829e9a9a");
		gebaeude.getGebaeudeArten().add(new GebaeudeArt(new CapiCode("", "Grange"), null));

		try {
			BatimentRFHelper.currentDataEquals(batiment, gebaeude);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Le type du bâtiment masterIdRF=[7837829e9a9a] a changé.", e.getMessage());
		}
	}

	/**
	 * Ce test vérifie que la surface est bien prise en compte dans le calcul de l'égalité entre deux bâtiments.
	 */
	@Test
	public void testCurrentDataEqualsOnSurface() throws Exception {

		// même surface
		{
			final BatimentRF batiment = new BatimentRF();
			batiment.setMasterIdRF("7837829e9a9a");
			batiment.setType("Garage");
			batiment.addSurface(new SurfaceBatimentRF(58, null, RegDate.get(2014, 12, 31)));
			batiment.addSurface(new SurfaceBatimentRF(60, RegDate.get(2015, 1, 1), null));
			batiment.setImplantations(new HashSet<>());

			final Gebaeude gebaeude = new Gebaeude();
			gebaeude.setMasterID("7837829e9a9a");
			gebaeude.getGebaeudeArten().add(new GebaeudeArt(new CapiCode("", "Garage"), null));
			gebaeude.setFlaeche(60);

			assertTrue(BatimentRFHelper.currentDataEquals(batiment, gebaeude));
		}

		// surface différente
		{
			final BatimentRF batiment = new BatimentRF();
			batiment.setMasterIdRF("7837829e9a9a");
			batiment.setType("Garage");
			batiment.addSurface(new SurfaceBatimentRF(58, null, RegDate.get(2014, 12, 31)));
			batiment.addSurface(new SurfaceBatimentRF(60, RegDate.get(2015, 1, 1), null));
			batiment.setImplantations(new HashSet<>());

			final Gebaeude gebaeude = new Gebaeude();
			gebaeude.setMasterID("7837829e9a9a");
			gebaeude.getGebaeudeArten().add(new GebaeudeArt(new CapiCode("", "Garage"), null));
			gebaeude.setFlaeche(62);

			assertFalse(BatimentRFHelper.currentDataEquals(batiment, gebaeude));
		}

		// surface inexistante
		{
			final BatimentRF batiment = new BatimentRF();
			batiment.setMasterIdRF("7837829e9a9a");
			batiment.setType("Garage");
			batiment.setSurfaces(new HashSet<>());
			batiment.setImplantations(new HashSet<>());

			final Gebaeude gebaeude = new Gebaeude();
			gebaeude.setMasterID("7837829e9a9a");
			gebaeude.getGebaeudeArten().add(new GebaeudeArt(new CapiCode("", "Garage"), null));
			gebaeude.setFlaeche(60);

			assertFalse(BatimentRFHelper.currentDataEquals(batiment, gebaeude));
		}
	}

	/**
	 * Ce test vérifie que les implantations sont bien prises en compte dans le calcul de l'égalité entre deux bâtiments.
	 */
	@Test
	public void testCurrentDataEqualsOnImplantation() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("78238e8323");

		final ImmeubleRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("48e89c9a9");

		final ImmeubleRF immeuble3 = new BienFondRF();
		immeuble3.setIdRF("02389349aaa");

		// même implantation
		{
			final BatimentRF batiment = new BatimentRF();
			batiment.setMasterIdRF("7837829e9a9a");
			batiment.setType("Garage");
			batiment.setSurfaces(new HashSet<>());
			batiment.addImplantation(new ImplantationRF(23, immeuble1));
			batiment.addImplantation(new ImplantationRF(12234, immeuble2));
			batiment.addImplantation(new ImplantationRF(208, immeuble3));

			final Gebaeude gebaeude = new Gebaeude();
			gebaeude.setMasterID("7837829e9a9a");
			gebaeude.getGebaeudeArten().add(new GebaeudeArt(new CapiCode("", "Garage"), null));
			gebaeude.getGrundstueckZuGebaeude().add(new GrundstueckZuGebaeude("78238e8323", 23));
			gebaeude.getGrundstueckZuGebaeude().add(new GrundstueckZuGebaeude("48e89c9a9", 12234));
			gebaeude.getGrundstueckZuGebaeude().add(new GrundstueckZuGebaeude("02389349aaa", 208));

			assertTrue(BatimentRFHelper.currentDataEquals(batiment, gebaeude));
		}

		// implantation différente
		{
			final BatimentRF batiment = new BatimentRF();
			batiment.setMasterIdRF("7837829e9a9a");
			batiment.setType("Garage");
			batiment.setSurfaces(new HashSet<>());
			batiment.addImplantation(new ImplantationRF(23, immeuble1));
			batiment.addImplantation(new ImplantationRF(12234, immeuble2));
			batiment.addImplantation(new ImplantationRF(208, immeuble3));

			final Gebaeude gebaeude = new Gebaeude();
			gebaeude.setMasterID("7837829e9a9a");
			gebaeude.getGebaeudeArten().add(new GebaeudeArt(new CapiCode("", "Garage"), null));
			gebaeude.getGrundstueckZuGebaeude().add(new GrundstueckZuGebaeude("78238e8323", 23));
			gebaeude.getGrundstueckZuGebaeude().add(new GrundstueckZuGebaeude("48e89c9a9", 12234));

			assertFalse(BatimentRFHelper.currentDataEquals(batiment, gebaeude));
		}
	}

	@Test
	public void testNewBatimentRF() throws Exception {

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("3738728228");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("a8280ec000");

		final Gebaeude gebaeude = new Gebaeude();
		gebaeude.setMasterID("7837829e9a9a");
		gebaeude.getGebaeudeArten().add(new GebaeudeArt(new CapiCode("", "Garage"), null));
		gebaeude.setFlaeche(360);
		gebaeude.getGrundstueckZuGebaeude().add(new GrundstueckZuGebaeude("3738728228", 80));
		gebaeude.getGrundstueckZuGebaeude().add(new GrundstueckZuGebaeude("a8280ec000", 280));

		final BatimentRF batiment = BatimentRFHelper.newBatimentRF(gebaeude, idRF -> {
			if (idRF.equals(immeuble1.getIdRF())) {
				return immeuble1;
			}
			else if (idRF.equals(immeuble2.getIdRF())) {
				return immeuble2;
			}
			else {
				return null;
			}
		});
		assertNotNull(batiment);
		assertEquals("7837829e9a9a", batiment.getMasterIdRF());
		assertEquals("Garage", batiment.getType());

		final Set<SurfaceBatimentRF> surfaces = batiment.getSurfaces();
		assertEquals(1, surfaces.size());
		final SurfaceBatimentRF surface0 = surfaces.iterator().next();
		assertEquals(360, surface0.getSurface());

		final List<ImplantationRF> implantations = new ArrayList<>(batiment.getImplantations());
		assertEquals(2, implantations.size());
		Collections.sort(implantations, (l, r) -> l.getImmeuble().getIdRF().compareTo(r.getImmeuble().getIdRF()));

		final ImplantationRF implantation0 = implantations.get(0);
		assertEquals(Integer.valueOf(80), implantation0.getSurface());
		assertEquals("3738728228", implantation0.getImmeuble().getIdRF());

		final ImplantationRF implantation1 = implantations.get(1);
		assertEquals(Integer.valueOf(280), implantation1.getSurface());
		assertEquals("a8280ec000", implantation1.getImmeuble().getIdRF());
	}
}